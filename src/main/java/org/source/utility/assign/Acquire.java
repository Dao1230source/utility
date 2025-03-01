package org.source.utility.assign;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class Acquire<E, K, T> {
    private static final Map<String, Cache<Object, Object>> CACHE_MAP = new ConcurrentHashMap<>(16);
    private final Function<Collection<K>, Map<K, T>> fetcher;
    private final Assign<E> assign;
    private final List<Action<E, K, T>> actions;

    private Map<K, T> ktMap;
    private Supplier<Cache<K, T>> cacherSupplier;
    private String name;
    private BiConsumer<E, Map<K, T>> afterProcessor;
    private BiConsumer<E, Throwable> exceptionHandler;
    private Throwable throwable;
    private Integer batchSize;


    public Acquire(Assign<E> assign, Function<Collection<K>, Map<K, T>> fetcher) {
        this.assign = assign;
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
        log.info("fetching main data, name:{}", name);
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
        // 分批请求
        if (Objects.nonNull(this.batchSize)) {
            Map<K, T> result = new ConcurrentHashMap<>(ks.size());
            List<? extends CompletableFuture<Void>> completableFutureList = Streams.partition(new ArrayList<>(ks), this.batchSize).stream().map(k ->
                            CompletableFuture.runAsync(() -> this.getFromCacheAsync(k, result), this.assign.getExecutor()))
                    .toList();
            CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
            this.ktMap = result;
        } else {
            this.ktMap = this.getFromCache(ks);
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch result: {}", Jsons.str(this.ktMap));
        }
        return this.ktMap;
    }

    private void getFromCacheAsync(Collection<K> ks, Map<K, T> result) {
        Map<K, T> fromCache = this.getFromCache(new HashSet<>(ks));
        result.putAll(fromCache);
    }

    private Map<K, T> getFromCache(Set<K> ks) {
        try {
            if (StringUtils.hasText(this.name) && Objects.nonNull(this.cacherSupplier)) {
                @SuppressWarnings("unchecked")
                Cache<K, T> cache = (Cache<K, T>) CACHE_MAP.computeIfAbsent(this.name,
                        k -> (Cache<Object, Object>) this.cacherSupplier.get());
                Map<K, T> cachedMap = cache.getAll(ks, keys -> this.fetcher.apply(ks));
                log.info("fetch data from cache");
                return cachedMap;
            } else {
                return this.fetcher.apply(ks);
            }
        } catch (Exception e) {
            log.error("Assign.Acquire getFromCache except", e);
            this.throwable = e;
            return Map.of();
        }
    }

    void invoke(E e) {
        if (this.isSuccess()) {
            this.actions.forEach(k -> k.invoke(e, this.ktMap));
            this.after(e);
        }
    }

    private void after(E e) {
        if (Objects.nonNull(this.throwable)) {
            if (Objects.nonNull(this.exceptionHandler)) {
                this.exceptionHandler.accept(e, this.throwable);
            }
        } else {
            if (Objects.nonNull(this.afterProcessor)) {
                this.afterProcessor.accept(e, this.ktMap);
            }
        }
    }

    public boolean isSuccess() {
        return Objects.isNull(this.ktMap) || Objects.isNull(this.throwable);
    }
}
