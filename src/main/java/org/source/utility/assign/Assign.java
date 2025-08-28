package org.source.utility.assign;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Streams;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

@Slf4j
public class Assign<E> {
    private static final int ROOT_DEPTH = 1;
    private static final Assign<?> EMPTY = Assign.build(List.of());
    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService DEFAULT_EXECUTE = Objects.requireNonNull(TtlExecutors.getTtlExecutorService(
            new ThreadPoolExecutor(1, PROCESSORS * 10, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new CustomizableThreadFactory("assign-pool-"), new ThreadPoolExecutor.CallerRunsPolicy())
    ));
    /**
     * 集合不可修改，只可以更新集合对象的值
     */
    private Collection<E> mainData;
    private final Predicate<E> filter;
    private final int depth;
    private final List<Acquire<E, ?, ?>> acquires;
    private final List<Consumer<E>> assignValues;
    /**
     * 分支：分支和主体有关联关系，可以通过 backSuper() 等方法返回
     */
    private final List<Assign<E>> branches;
    /**
     * 子赋值程序，和主体是不相关的，对子体的mainData的引用修改不会影响主体，但子体中对 E 值的修改会影响主体
     * 比如将 E 转换成 其他类型并做一些操作等
     */
    private final List<Consumer<Collection<E>>> subs;

    private Assign<E> superAssign;

    private String name;
    /**
     * 多线程执行器
     */
    @Nullable
    @Getter
    private Executor executor;
    /**
     * 执行状态
     */
    private InvokeStatusEnum status;
    /**
     * 中断执行的策略
     */
    private InterruptStrategyEnum interruptStrategy;

    public Assign(Collection<E> mainData, int depth, @Nullable Assign<E> superAssign, @Nullable Predicate<E> filter) {
        this.mainData = List.copyOf(mainData);
        this.depth = depth;
        this.superAssign = superAssign;
        this.acquires = new ArrayList<>();
        this.assignValues = new ArrayList<>();
        this.name = String.valueOf(this.hashCode());
        this.interruptStrategy = InterruptStrategyEnum.ANY;
        this.status = InvokeStatusEnum.CREATED;
        this.executor = null;
        this.branches = new ArrayList<>();
        if (Objects.nonNull(this.superAssign)) {
            this.superAssign.branches.add(this);
        }
        this.filter = filter;
        this.subs = new ArrayList<>();
    }

    public Assign(Collection<E> mainData) {
        this(mainData, ROOT_DEPTH, null, null);
    }

    public Assign(Assign<E> superAssign) {
        this(superAssign.mainData, superAssign.depth + ROOT_DEPTH, superAssign, null);
    }

    public <K, T> Acquire<E, K, T> addAcquire(Function<Collection<K>, Map<K, T>> fetcher) {
        Acquire<E, K, T> acquire = new Acquire<>(this, fetcher, null);
        this.acquires.add(acquire);
        return acquire;
    }

    private static <K, T> Map<K, T> toMap(@Nullable Collection<T> ts,
                                          Function<T, K> keyGetter) {
        if (CollectionUtils.isEmpty(ts)) {
            return Map.of();
        }
        return Streams.toMap(ts, keyGetter);
    }

    public <K, T> Acquire<E, K, T> addAcquire(Function<Collection<K>, Collection<T>> fetcher,
                                              Function<T, K> keyGetter) {
        Function<Collection<K>, Map<K, T>> mapFetcher = ks -> toMap(fetcher.apply(ks), keyGetter);
        Acquire<E, K, T> acquire = new Acquire<>(this, mapFetcher, null);
        this.acquires.add(acquire);
        return acquire;
    }


    public <K, T> Acquire<E, K, T> addAcquire4Single(Function<K, T> fetcher) {
        Acquire<E, K, T> acquire = new Acquire<>(this, null, fetcher);
        this.acquires.add(acquire);
        return acquire;
    }

    public <K, T> Acquire<E, K, T> addAcquireByList(Function<List<K>, Collection<T>> fetcher,
                                                    Function<T, K> keyGetter) {
        return addAcquire(ks -> fetcher.apply(new ArrayList<>(ks)), keyGetter);
    }

    public <K, T> Acquire<E, K, T> addAcquireByMainData(Function<Collection<E>, Collection<T>> fetcher,
                                                        Function<T, K> keyGetter) {
        Function<Collection<K>, Map<K, T>> mapFetcher = ks -> toMap(fetcher.apply(this.mainData), keyGetter);
        return addAcquire(mapFetcher);
    }

    public <K, T> Acquire<E, K, T> addAcquireByExtra(Supplier<Collection<T>> fetcher,
                                                     Function<T, K> keyGetter) {
        Function<Collection<K>, Map<K, T>> mapFetcher = ks -> toMap(fetcher.get(), keyGetter);
        return addAcquire(mapFetcher);
    }

    public <P> Assign<E> addAssignValue(BiConsumer<E, P> eSetter, P value) {
        this.assignValues.add(e -> eSetter.accept(e, value));
        return this;
    }

    public <P> Assign<E> addAssignValueIfAbsent(Function<E, P> eGetter, BiConsumer<E, P> eSetter, P value) {
        this.assignValues.add(e -> {
            if (Objects.isNull(eGetter.apply(e))) {
                eSetter.accept(e, value);
            }
        });
        return this;
    }

    public Assign<E> parallel(Executor executor) {
        log.debug("acquires executed parallel");
        this.executor = executor;
        return this;
    }

    public Assign<E> parallel() {
        this.executor = DEFAULT_EXECUTE;
        return this;
    }

    public Assign<E> interruptStrategy(InterruptStrategyEnum interruptStrategy) {
        this.interruptStrategy = interruptStrategy;
        return this;
    }

    public Assign<E> name(String name) {
        this.name = name;
        return this;
    }

    public Assign<E> addBranch(Predicate<E> filter) {
        return new Assign<>(this.mainData, this.depth + 1, this, filter);
    }

    public Assign<E> addBranch() {
        return new Assign<>(this);
    }

    public <K> Assign<E> addBranches(Function<E, K> keyGetter, Map<K, Function<Collection<E>, Assign<E>>> keyAssigners) {
        Map<K, Consumer<Collection<E>>> keyOperates = HashMap.newHashMap(keyAssigners.size());
        keyAssigners.forEach((k, v) -> keyOperates.put(k, l -> {
            Assign<E> assign = v.apply(l);
            assign.superAssign = this;
            this.branches.add(assign);
        }));
        return addOperates(keyGetter, keyOperates);
    }

    public <K> Assign<E> addOperates(Function<E, K> keyGetter, Map<K, Consumer<Collection<E>>> keyOperates) {
        Map<K, List<E>> keyMap = Streams.groupBy(this.mainData, keyGetter);
        Map<Consumer<Collection<E>>, List<E>> operatorDataMap = HashMap.newHashMap(keyMap.size());
        keyOperates.forEach((k, consumer) -> {
            List<E> es = keyMap.get(k);
            if (CollectionUtils.isEmpty(es)) {
                return;
            }
            operatorDataMap.compute(consumer, (a, l) -> {
                if (CollectionUtils.isEmpty(l)) {
                    l = new ArrayList<>();
                }
                l.addAll(es);
                return l;
            });
        });
        operatorDataMap.forEach(Consumer::accept);
        return this;
    }

    public Assign<E> addSub(Consumer<Collection<E>> sub) {
        this.subs.add(sub);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Assign<E> backSuper() {
        Assign<E> assign = this.superAssign;
        if (Objects.isNull(assign)) {
            return (Assign<E>) EMPTY;
        }
        return assign;
    }

    @SuppressWarnings("unchecked")
    public Assign<E> backSuperTo(int depth) {
        if (depth < ROOT_DEPTH || depth >= this.depth) {
            return (Assign<E>) EMPTY;
        }
        Assign<E> assign = this.backSuper();
        while (assign.depth > depth) {
            assign = assign.backSuper();
        }
        return assign;
    }

    public Assign<E> backSuperlative() {
        return backSuperTo(ROOT_DEPTH);
    }

    public void forEach(Consumer<E> consumer) {
        this.mainData.forEach(consumer);
    }

    public Assign<E> peek(Consumer<E> consumer) {
        this.mainData.forEach(consumer);
        return this;
    }

    public List<E> toList() {
        return new ArrayList<>(this.mainData);
    }

    public <F> Assign<F> cast(Function<E, F> mapping) {
        return new Assign<>(Streams.map(this.mainData, mapping).filter(Objects::nonNull).toList());
    }

    public <F> Assign<F> casts(Function<Collection<E>, Collection<F>> mapping) {
        return new Assign<>(mapping.apply(this.mainData));
    }

    public Assign<E> invoke() {
        if (Objects.nonNull(this.superAssign)) {
            if (InvokeStatusEnum.CREATED.equals(this.superAssign.status)) {
                this.superAssign.invoke();
            }
            if (!InvokeStatusEnum.CREATED.equals(this.status)
                    || this.superAssign.interruptStrategy.interrupt(this.superAssign.status)) {
                return this;
            }
        }
        if (Objects.nonNull(this.filter)) {
            this.mainData = Streams.retain(this.mainData, filter).toList();
        }
        if (CollectionUtils.isEmpty(this.mainData)) {
            this.status = InvokeStatusEnum.ALL_SUCCESS;
            return this;
        }
        log.debug("Assign name:{}", this.name);
        this.invokeMain();
        int sum = this.acquires.stream().map(k -> k.isSuccess() ? 0 : 1).reduce(0, Integer::sum);
        if (sum == 0) {
            this.status = InvokeStatusEnum.ALL_SUCCESS;
        } else if (sum == this.acquires.size()) {
            this.status = InvokeStatusEnum.ALL_FAIL;
        } else {
            this.status = InvokeStatusEnum.PARTIAL_FAIL;
        }
        log.debug("status:{}", this.status);
        if (this.interruptStrategy.interrupt(this.status)) {
            log.debug("assign end, interruptStrategy:{}", this.interruptStrategy);
            return this;
        }
        this.invokeBranches();
        this.invokeSubs();
        return this;
    }

    private void invokeMain() {
        this.mainData.forEach(e -> this.assignValues.forEach(a -> a.accept(e)));
        Assign.<Acquire<E, ?, ?>, Map<?, ?>>parallelExecute(this.executor, this.acquires,
                a -> true, a -> a.fetch(this.mainData), "Assign parallel fetch data exception");
        this.mainData.forEach(e -> this.acquires.forEach(a -> a.invoke(e)));
    }

    private void invokeBranches() {
        Assign.parallelExecute(this.executor, this.branches, a -> InvokeStatusEnum.CREATED.equals(a.status),
                Assign::invoke, "Assign parallel execute branches exception");
    }

    private void invokeSubs() {
        Assign.<Consumer<Collection<E>>, Void>parallelExecute(this.executor, this.subs,
                a -> true, a -> {
                    a.accept(this.mainData);
                    return null;
                }, "Assign parallel execute invokeSubs exception");
    }

    static <T, R> void parallelExecute(Executor executor, List<T> ts,
                                       Predicate<T> filter, Function<T, R> function,
                                       String errorMsg) {
        if (Objects.nonNull(executor)) {
            try {
                List<CompletableFuture<R>> completableFutureList = ts.stream()
                        .filter(filter)
                        .map(t -> CompletableFuture.supplyAsync(() -> function.apply(t), executor))
                        .toList();
                CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
            } catch (Exception e) {
                log.error(errorMsg, e);
                BaseExceptionEnum.ASSIGN_PARALLEL_EXECUTE_EXCEPTION.except(e);
            }
        } else {
            ts.stream().filter(filter).forEach(function::apply);
        }
    }

    public static <E> Assign<E> build(Collection<E> mainData) {
        return new Assign<>(mainData);
    }
}