package org.source.utility.assign;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.utils.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据获取与赋值配置类
 * <p>
 * 定义如何获取外部数据并将数据赋值给主对象。
 * 支持批量获取、单条获取、缓存、异常处理等功能。
 * </p>
 *
 * @param <E> 主数据类型
 * @param <K> Key 类型
 * @param <T> 关联数据类型
 * @author zengfugen
 */
@Slf4j
@JsonIncludeProperties({"name", "executedName", "success", "batchSize", "timeout", "fetchTiming", "invokeTiming", "actions"})
@JsonPropertyOrder({"name", "executedName", "success", "batchSize", "timeout", "fetchTiming", "invokeTiming", "actions"})
public class Acquire<E, K, T> {
    /**
     * 默认异常处理器
     * <p>
     * 同一个 Assign 下的 Acquire 的 name 务必不能重复
     * 建议在接口层实现缓存
     * </p>
     */
    private final BiConsumer<E, Throwable> defaultExceptionHandler = (e, ex) -> {
        BaseExceptionEnum.ASSIGN_ACQUIRE_RUN_EXCEPTION.throwException(ex);
    };

    /**
     * 所属的 Assign 对象
     */
    @JsonBackReference
    private final Assign<E> assign;

    /**
     * 赋值操作列表
     */
    @JsonManagedReference
    @Getter
    private final List<Action<E, K, T>> actions;

    /**
     * 批量获取函数
     * <p>
     * 接收 Key 集合，返回 Key 到数据的映射。
     * </p>
     */
    private final @Nullable Function<Collection<K>, Map<K, T>> batchFetcher;

    /**
     * 单条获取函数
     * <p>
     * 有些接口不支持批量查询，使用此函数逐条获取。
     * </p>
     */
    private final @Nullable Function<K, @Nullable T> fetcher;

    /**
     * Acquire 名称
     */
    @Getter
    private String name;

    /**
     * Key 到关联数据的映射
     * <p>
     * Acquire 未执行时为 null。
     * </p>
     */
    private @Nullable Map<K, T> ktMap;

    /**
     * 缓存处理器
     */
    private @Nullable CacheHandler cacheHandler;

    /**
     * 后置处理器
     * <p>
     * 在所有赋值操作完成后执行。
     * </p>
     */
    private @Nullable BiConsumer<E, Map<K, T>> afterProcessor;

    /**
     * 异常处理器
     */
    private @Nullable BiConsumer<E, Throwable> exceptionHandler;

    /**
     * 执行过程中发生的异常
     */
    private @Nullable Throwable throwable;

    /**
     * 是否抛出异常
     */
    private boolean isThrowException = false;

    /**
     * 批量大小
     * <p>
     * 用于分批获取数据，避免单次请求数据量过大。
     * </p>
     */
    @Getter
    private @Nullable Integer batchSize;

    /**
     * 超时时间（秒）
     */
    @Getter
    private long timeout;

    /**
     * 执行名称
     * <p>
     * Assign.invoke() 执行完毕后如果该字段为 null，表示该 Acquire 未执行。
     * 格式：{assignName}:{acquireName}[{executedSequence}][{threadName}]
     * </p>
     */
    @Getter
    private @Nullable String executedName;

    /**
     * fetch 方法时间统计
     */
    @Getter
    private @Nullable Timing fetchTiming;

    /**
     * invoke 方法累计时间统计
     */
    @Getter
    private @Nullable Timing invokeTiming;

    /**
     * 构造函数
     *
     * @param assign       所属的 Assign 对象
     * @param batchFetcher 批量获取函数
     * @param fetcher      单条获取函数
     * @throws BaseException 如果 batchFetcher 和 fetcher 都为 null
     */
    public Acquire(Assign<E> assign,
                   @Nullable Function<Collection<K>, Map<K, T>> batchFetcher,
                   @Nullable Function<K, @Nullable T> fetcher) {
        if (Objects.isNull(batchFetcher) && Objects.isNull(fetcher)) {
            BaseExceptionEnum.NOT_NULL.throwException("batchFetcher 和 fetcher 至少有一个不为空");
        }
        this.assign = assign;
        this.batchFetcher = batchFetcher;
        this.fetcher = fetcher;
        this.actions = new ArrayList<>();
        this.name = "Acquire_" + this.hashCode();
        this.timeout = assign.getTimeout();
    }

    /**
     * 设置自定义缓存处理器
     *
     * @param cacheHandler 缓存处理器
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> cache(CacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
        return this;
    }

    /**
     * 使用默认缓存处理器
     *
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> cache() {
        this.cacheHandler = new DefaultCacheHandler();
        return this;
    }

    /**
     * 设置名称
     *
     * @param name Acquire 名称
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 设置超时时间
     *
     * @param timeout 超时时间（秒）
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> timeout(long timeout) {
        if (timeout <= 0) {
            log.warn("timeout must be positive, got: {}, ignored", timeout);
            return this;
        }
        this.timeout = timeout;
        return this;
    }

    /**
     * 设置后置处理器
     * <p>
     * 在所有赋值操作完成后执行。
     * </p>
     *
     * @param afterProcessor 后置处理器
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> afterProcessor(BiConsumer<E, Map<K, T>> afterProcessor) {
        this.afterProcessor = afterProcessor;
        return this;
    }

    /**
     * 设置异常处理器
     *
     * @param exceptionHandler 异常处理器
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> exceptionHandler(BiConsumer<E, Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    /**
     * 设置在发生异常时是否抛出异常
     *
     * @return this，支持链式调用
     */
    public Acquire<E, K, T> throwException() {
        this.isThrowException = true;
        return this;
    }

    /**
     * 设置批量大小
     * <p>
     * 用于分批获取数据，避免单次请求数据量过大。
     * </p>
     *
     * @param batchSize 批量大小
     * @return this，支持链式调用
     * @throws BaseException 如果 batchSize 小于等于 0
     */
    public Acquire<E, K, T> batchSize(int batchSize) {
        if (batchSize <= 0) {
            BaseExceptionEnum.BATCHSIZE_MUST_BE_POSITIVE.throwException("batchSize: {}", batchSize);
        }
        this.batchSize = batchSize;
        return this;
    }

    /**
     * 添加赋值操作
     *
     * @param keyGetter 从主数据中提取 Key 的函数
     * @return Action 对象，用于配置具体的赋值操作
     */
    public Action<E, K, T> addAction(Function<E, @Nullable K> keyGetter) {
        Action<E, K, T> action = new Action<>(this, keyGetter);
        this.actions.add(action);
        return action;
    }

    /**
     * 返回所属的 Assign 对象
     *
     * @return Assign 对象
     */
    public Assign<E> backAssign() {
        return this.assign;
    }

    /**
     * 获取数据
     * <p>
     * 根据主数据中的 Key 从缓存或数据源获取关联数据。
     * 支持分批获取和缓存。
     * </p>
     *
     * @param mainData 主数据集合
     * @return Key 到关联数据的映射
     */
    Map<K, T> fetch(Collection<E> mainData) {
        log.debug("Acquire name:{}", name);
        this.fetchTiming = Timings.start();
        if (StringUtils.isBlank(this.executedName)) {
            this.executedName = Strings.format("{}:{}[{}][{}]", this.assign.getName(), this.name,
                    this.assign.acquireCounter.getAndIncrement(), Thread.currentThread().getName());
        }
        if (Objects.nonNull(this.ktMap)) {
            this.fetchTiming.end();
            return this.ktMap;
        }
        if (CollectionUtils.isEmpty(this.actions)) {
            this.ktMap = Map.of();
            this.fetchTiming.end();
            return this.ktMap;
        }
        Set<K> ks = mainData.stream().map(k -> Streams.map(this.actions, a -> a.getKeyGetter().apply(k)).toList())
                .flatMap(Collection::stream).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(ks)) {
            this.ktMap = Map.of();
            this.fetchTiming.end();
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
        this.fetchTiming.end();
        if (log.isDebugEnabled()) {
            log.debug("fetch result: {}", Jsons.str(this.ktMap));
        }
        return this.ktMap;
    }

    /**
     * 从数据源获取数据
     *
     * @param ks Key 集合
     */
    private void fetchData(Collection<K> ks) {
        try {
            // 双重检查锁保证线程安全
            if (Objects.isNull(this.ktMap)) {
                synchronized (this) {
                    if (Objects.isNull(this.ktMap)) {
                        this.ktMap = new ConcurrentHashMap<>(ks.size());
                    }
                }
            }
            // 先尝试从缓存获取
            Map<K, T> kt = this.getFromCache(ks);
            if (Objects.isNull(kt) || kt.isEmpty()) {
                // 缓存未命中，从数据源获取
                kt = this.get(ks);
            }
            this.ktMap.putAll(kt);
        } catch (Exception e) {
            log.error("Assign.Acquire except, keys size={}", ks.size(), e);
            this.throwable = e;
        }
    }

    /**
     * 从缓存获取数据
     *
     * @param ks Key 集合
     * @return Key 到关联数据的映射，缓存未命中返回 null
     */
    private @Nullable Map<K, T> getFromCache(Collection<K> ks) {
        Map<K, T> kt = null;
        String acquireCacheName = this.assign.getName() + Constants.UNDERSCORE + this.name;
        if (Objects.isNull(this.cacheHandler)) {
            return kt;
        }
        kt = this.cacheHandler.get(acquireCacheName, ks);
        if (Objects.nonNull(kt)) {
            this.cacheHandler.put(acquireCacheName, kt);
        }
        return kt;
    }

    /**
     * 从数据源获取数据
     * <p>
     * 优先使用批量获取，其次使用单条获取。
     * </p>
     *
     * @param ks Key 集合
     * @return Key 到关联数据的映射
     */
    private Map<K, T> get(Collection<K> ks) {
        if (CollectionUtils.isEmpty(ks)) {
            return Map.of();
        }
        if (Objects.nonNull(this.batchFetcher)) {
            return this.batchFetcher.apply(ks);
        } else if (Objects.nonNull(this.fetcher)) {
            Map<K, T> result;
            if (Objects.nonNull(this.assign.getExecutor())) {
                result = new ConcurrentHashMap<>(ks.size());
                Assign.parallelExecute(ks, this.assign.functionRunVirtualExecutor(k -> {
                    T value = this.fetcher.apply(k);
                    if (Objects.nonNull(value)) {
                        result.put(k, value);
                    }
                    return value;
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

    /**
     * 执行赋值操作
     *
     * @param e 主数据对象
     */
    void invoke(E e) {
        this.invokeTiming = Timings.start();
        if (this.isSuccess() && Objects.nonNull(this.ktMap)) {
            this.actions.forEach(k -> k.invoke(e, this.ktMap));
        }
        this.after(e);
        this.invokeTiming.end();
    }

    /**
     * 执行后置处理
     *
     * @param e 主数据对象
     */
    private void after(E e) {
        this.handleException(e);
        this.handleAfter(e);
    }

    /**
     * 处理异常
     *
     * @param e 主数据对象
     */
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

    /**
     * 执行后置处理器
     *
     * @param e 主数据对象
     */
    private void handleAfter(E e) {
        if (Objects.nonNull(this.afterProcessor) && Objects.nonNull(this.ktMap)) {
            this.afterProcessor.accept(e, this.ktMap);
        }
    }

    /**
     * 判断是否执行成功
     *
     * @return 如果执行过程中没有异常返回 true，否则返回 false
     */
    public boolean isSuccess() {
        return Objects.isNull(this.throwable);
    }
}