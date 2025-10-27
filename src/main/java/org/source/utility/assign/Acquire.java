package org.source.utility.assign;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class Acquire<E, K, T> {
    private static final Map<String, Cache<Object, Object>> CACHE_MAP = new ConcurrentHashMap<>(16);
    private final BiConsumer<E, Throwable> defaultExceptionHandler = (e, ex) -> {
        throw BaseExceptionEnum.ASSIGN_ACQUIRE_RUN_EXCEPTION.except(ex);
    };
    private final Assign<E> assign;
    private final List<Action<E, K, T>> actions;
    private final Function<Collection<K>, Map<K, T>> batchFetcher;
    // 有些接口不支持批量查询
    private final Function<K, T> fetcher;

    private Map<K, T> ktMap;
    private Supplier<Cache<K, T>> cacherSupplier;
    private String name;
    private BiConsumer<E, Map<K, T>> afterProcessor;
    private BiConsumer<E, Throwable> exceptionHandler;
    private Throwable throwable;
    private boolean throwException = false;
    private Integer batchSize;

    public Acquire(Assign<E> assign,
                   @Nullable Function<Collection<K>, Map<K, T>> batchFetcher,
                   @Nullable Function<K, T> fetcher) {
        if (Objects.isNull(batchFetcher) && Objects.isNull(fetcher)) {
            throw BaseExceptionEnum.NOT_NULL.except("batchFetcher和fetcher至少有一个不为空");
        }
        this.assign = assign;
        this.batchFetcher = batchFetcher;
        this.fetcher = fetcher;
        this.actions = new ArrayList<>();
    }

    public static <K, T> Cache<K, T> defaultCache() {
        return Caffeine.newBuilder().expireAfterAccess(600, TimeUnit.SECONDS).build();
    }

    public Acquire<E, K, T> cache(Supplier<Cache<K, T>> cacherSupplier) {
        log.debug("cached by local cache, acquire name must not be null");
        this.cacherSupplier = cacherSupplier;
        return this;
    }

    public Acquire<E, K, T> cache() {
        this.cacherSupplier = Acquire::defaultCache;
        return this;
    }

    public Acquire<E, K, T> name(String name) {
        this.name = name;
        return this;
    }

    public Acquire<E, K, T> afterProcessor(BiConsumer<E, Map<K, T>> afterProcessor) {
        this.afterProcessor = afterProcessor;
        return this;
    }

    public Acquire<E, K, T> exceptionHandler(BiConsumer<E, Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public Acquire<E, K, T> throwException() {
        this.throwException = true;
        return this;
    }

    public Acquire<E, K, T> batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public Action<E, K, T> addAction(Function<E, K> keyGetter) {
        Action<E, K, T> action = new Action<>(this, keyGetter);
        this.actions.add(action);
        return action;
    }

    public Assign<E> backAssign() {
        return this.assign;
    }

    Map<K, T> fetch(Collection<E> mainData) {
        log.info("Acquire name:{}", name);
        if (Objects.nonNull(this.ktMap)) {
            return this.ktMap;
        }
        if (CollectionUtils.isEmpty(this.actions)) {
            this.ktMap = Map.of();
            return this.ktMap;
        }
        Set<K> ks = mainData.stream().map(k -> this.actions.stream().map(a -> a.getKeyGetter().apply(k)).toList())
                .flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(ks)) {
            this.ktMap = Map.of();
            return this.ktMap;
        }
        List<List<K>> partitions;
        // 分批请求
        if (Objects.nonNull(this.batchSize)) {
            partitions = Streams.partition(new ArrayList<>(ks), this.batchSize);
        } else {
            partitions = List.of(new ArrayList<>(ks));
        }
        Assign.<List<K>, Void>parallelExecute(partitions, k -> {
            Map<K, T> fromCache = this.getFromCache(new HashSet<>(ks));
            this.ktMap.putAll(fromCache);
            return null;
        }, this.assign.getExecutor(), null, "Acquire.fetch parallel execute by batchSize exception");
        if (log.isDebugEnabled()) {
            log.debug("fetch result: {}", Jsons.str(this.ktMap));
        }
        return this.ktMap;
    }

    private Map<K, T> getFromCache(Set<K> ks) {
        try {
            if (StringUtils.hasText(this.name) && Objects.nonNull(this.cacherSupplier)) {
                @SuppressWarnings("unchecked")
                Cache<K, T> cache = (Cache<K, T>) CACHE_MAP.computeIfAbsent(this.name,
                        k -> (Cache<Object, Object>) this.cacherSupplier.get());
                @SuppressWarnings("unchecked")
                Map<K, T> cachedMap = cache.getAll(ks, keys -> this.get((Set<K>) keys));
                log.info("fetch data from cache");
                return cachedMap;
            } else {
                return this.get(ks);
            }
        } catch (Exception e) {
            log.error("Assign.Acquire getFromCache except", e);
            this.throwable = e;
            return Map.of();
        }
    }

    private Map<K, T> get(Set<K> ks) {
        if (CollectionUtils.isEmpty(ks)) {
            return Map.of();
        }
        if (Objects.nonNull(this.batchFetcher)) {
            return this.batchFetcher.apply(ks);
        } else if (Objects.nonNull(this.fetcher)) {
            Map<K, T> result = HashMap.newHashMap(ks.size());
            if (Objects.nonNull(this.assign) && Objects.nonNull(this.assign.getExecutor())) {
                Assign.parallelExecute(ks, this.fetcher,
                        this.assign.getExecutor(), null, "Acquire parallel execute fetcher exception");
            } else {
                ks.forEach(k -> result.put(k, this.fetcher.apply(k)));
            }
        }
        return Map.of();
    }

    void invoke(E e) {
        if (this.isSuccess()) {
            this.actions.forEach(k -> k.invoke(e, this.ktMap));
        }
        this.after(e);
    }

    private void after(E e) {
        this.handleException(e);
        this.handleAfter(e);
    }

    private void handleException(E e) {
        if (Objects.nonNull(this.throwable)) {
            BiConsumer<E, Throwable> handler = (ele, ex) -> {
            };
            if (throwException) {
                handler = handler.andThen(defaultExceptionHandler);
            }
            if (Objects.nonNull(this.exceptionHandler)) {
                handler = this.exceptionHandler.andThen(handler);
            }
            handler.accept(e, this.throwable);
        }
    }

    private void handleAfter(E e) {
        if (Objects.nonNull(this.afterProcessor)) {
            this.afterProcessor.accept(e, this.ktMap);
        }
    }

    public boolean isSuccess() {
        return Objects.isNull(this.throwable);
    }
}