package org.source.utility.assign;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
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
@JsonIncludeProperties({"name", "executedName", "success", "batchSize", "timeout", "actions"})
@JsonPropertyOrder({"name", "executedName", "success", "batchSize", "timeout", "actions"})
public class Acquire<E, K, T> {
    /**
     * 同一个 Assign 下的 Acquire 的 name 务必不能重复
     * 建议在接口层实现缓存
     */
    private static final Cache<String, Cache<Object, Object>> CACHE_MAP = Caffeine.newBuilder()
            .expireAfterAccess(600, TimeUnit.SECONDS).maximumSize(1024).build();
    private final BiConsumer<E, Throwable> defaultExceptionHandler = (e, ex) -> {
        throw BaseExceptionEnum.ASSIGN_ACQUIRE_RUN_EXCEPTION.except(ex);
    };

    @JsonBackReference
    private final Assign<E> assign;
    @JsonManagedReference
    @Getter
    private final List<Action<E, K, T>> actions;
    private final Function<Collection<K>, Map<K, T>> batchFetcher;
    // 有些接口不支持批量查询
    private final Function<K, T> fetcher;

    @Getter
    private String name;
    /**
     * Acquire 未执行时为null
     */
    private @Nullable Map<K, T> ktMap;
    private @Nullable Supplier<Cache<K, T>> cacherSupplier;
    private @Nullable BiConsumer<E, Map<K, T>> afterProcessor;
    private @Nullable BiConsumer<E, Throwable> exceptionHandler;
    private @Nullable Throwable throwable;
    private boolean isThrowException = false;
    @Getter
    private @Nullable Integer batchSize;
    @Getter
    private @Nullable Long timeout;
    /**
     * <pre>
     * 执行名称，
     * Assign.invoke()执行完毕后如果该字段为null，表示该Acquire未执行
     * {assignName}:{acquireName}[{executedSequence}][{threadName}]
     * </pre>
     */
    @Getter
    private @Nullable String executedName;

    public Acquire(Assign<E> assign,
                   @Nullable Function<Collection<K>, Map<K, T>> batchFetcher,
                   @Nullable Function<K, T> fetcher) {
        if (Objects.isNull(batchFetcher) && Objects.isNull(fetcher)) {
            throw BaseExceptionEnum.NOT_NULL.except("batchFetcher 和 fetcher 至少有一个不为空");
        }
        this.assign = assign;
        this.batchFetcher = batchFetcher;
        this.fetcher = fetcher;
        this.actions = new ArrayList<>();
        this.name = "Acquire_" + this.hashCode();
        this.timeout = assign.getTimeout();
    }

    public static <K, T> Cache<K, T> defaultCache() {
        return Caffeine.newBuilder().expireAfterAccess(600, TimeUnit.SECONDS).build();
    }

    public Acquire<E, K, T> cache(Supplier<Cache<K, T>> cacherSupplier) {
        this.cacherSupplier = cacherSupplier;
        return this;
    }

    public Acquire<E, K, T> cache() {
        return cache(Acquire::defaultCache);
    }

    public Acquire<E, K, T> name(String name) {
        this.name = name;
        return this;
    }

    public Acquire<E, K, T> timeout(long timeout) {
        if (timeout <= 0) {
            log.warn("timeout must be positive, got: {}, ignored", timeout);
            return this;
        }
        this.timeout = timeout;
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
        this.isThrowException = true;
        return this;
    }

    public Acquire<E, K, T> batchSize(int batchSize) {
        if (batchSize <= 0) {
            throw BaseExceptionEnum.BATCHSIZE_MUST_BE_POSITIVE.except("batchSize: {}", batchSize);
        }
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
        log.debug("Acquire name:{}", name);
        if (!StringUtils.hasText(this.executedName)) {
            this.executedName = Strings.format("{}:{}[{}][{}]", this.assign.getName(), this.name,
                    this.assign.acquireCounter.getAndIncrement(), Thread.currentThread().getName());
        }
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
            Assign.<List<K>, Void>parallelExecute(partitions, this.assign.functionRunVirtualExecutor(k -> {
                this.fetchData(k);
                return null;
            }), this.assign.getExecutor(), this.timeout, null, "Acquire.fetch parallel execute by batchSize exception");
        } else {
            this.fetchData(ks);
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch result: {}", Jsons.str(this.ktMap));
        }
        return this.ktMap;
    }

    private void fetchData(Collection<K> ks) {
        if (Objects.isNull(this.ktMap)) {
            synchronized (this) {
                if (Objects.isNull(this.ktMap)) {
                    this.ktMap = new ConcurrentHashMap<>(ks.size());
                }
            }
        }
        Map<K, T> fromCache = this.getFromCache(new HashSet<>(ks));
        this.ktMap.putAll(fromCache);
    }

    @SuppressWarnings("unchecked")
    private Map<K, T> getFromCache(Set<K> ks) {
        String acquireCacheName = this.assign.getName() + Constants.UNDERSCORE + this.name;
        try {
            if (Objects.nonNull(this.cacherSupplier)) {
                Cache<K, T> cache = (Cache<K, T>) CACHE_MAP.get(acquireCacheName,
                        k -> (Cache<Object, Object>) this.cacherSupplier.get());
                Map<K, T> cachedMap = cache.getAll(ks, keys -> this.get((Set<K>) keys));
                log.debug("fetch data from cache");
                return cachedMap;
            } else {
                return this.get(ks);
            }
        } catch (Exception e) {
            log.error("Assign.Acquire getFromCache except, cacheName={}, keys size={}", acquireCacheName, ks.size(), e);
            this.throwable = e;
            try {
                return this.get(ks);
            } catch (Exception fallbackE) {
                log.error("Fallback get also failed", fallbackE);
                return Map.of();
            }
        }
    }

    private Map<K, T> get(Set<K> ks) {
        if (CollectionUtils.isEmpty(ks)) {
            return Map.of();
        }
        if (Objects.nonNull(this.batchFetcher)) {
            return this.batchFetcher.apply(ks);
        } else if (Objects.nonNull(this.fetcher)) {
            Map<K, T> result;
            if (Objects.nonNull(this.assign) && Objects.nonNull(this.assign.getExecutor())) {
                result = new ConcurrentHashMap<>(ks.size());
                Assign.parallelExecute(ks, this.assign.functionRunVirtualExecutor(k -> {
                    T apply = this.fetcher.apply(k);
                    result.put(k, apply);
                    return apply;
                }), this.assign.getExecutor(), this.timeout, null, "Acquire parallel execute fetcher exception");
            } else {
                result = HashMap.newHashMap(ks.size());
                ks.forEach(k -> {
                    T value = this.fetcher.apply(k);
                    if (Objects.nonNull(value)) {
                        result.put(k, value);
                    }
                });
            }
            return result;
        }
        return Map.of();
    }

    void invoke(E e) {
        if (this.isSuccess() && Objects.nonNull(this.ktMap)) {
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
            if (isThrowException) {
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