package org.source.utility.assign;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Unmodifiable;
import org.source.utility.utils.Streams;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

@Slf4j
public class Assign<E> {
    private static final Assign<?> EMPTY = Assign.build(List.of());
    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService DEFAULT_EXECUTE = Objects.requireNonNull(TtlExecutors.getTtlExecutorService(
            new ThreadPoolExecutor(1, PROCESSORS * 10, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new CustomizableThreadFactory("assign-pool-"), new ThreadPoolExecutor.CallerRunsPolicy())
    ));
    /**
     * 集合不可修改，只可以更新集合对象的值
     */
    @Unmodifiable
    private final Collection<E> mainData;
    private final int depth;
    private final List<Acquire<E, ?, ?>> acquires;
    private final List<Consumer<E>> assignValues;
    private final Assign<E> superAssign;
    /**
     * 分支：分支和主体有关联关系，可以通过 backSuper() 等方法返回
     */
    private final List<Assign<E>> branches;

    /**
     * 子赋值程序，和主体是不相关的，对子体的mainData的引用修改不会影响主体，但子体中对 E 值的修改会影响主体
     */
    private final List<Consumer<Collection<E>>> subs;

    private final List<ClassifyInvoker<E, ?>> classifyInvokers;

    private String name;
    /**
     * 多线程执行器
     */
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

    public Assign(Collection<E> mainData, int depth, @Nullable Assign<E> superAssign) {
        this.mainData = Collections.unmodifiableCollection(mainData);
        this.depth = depth;
        this.superAssign = superAssign;
        this.acquires = new ArrayList<>();
        this.assignValues = new ArrayList<>();
        this.name = String.valueOf(this.hashCode());
        this.interruptStrategy = InterruptStrategyEnum.ANY;
        this.status = InvokeStatusEnum.CREATED;
        this.branches = new ArrayList<>();
        if (Objects.nonNull(this.superAssign)) {
            this.superAssign.branches.add(this);
        }
        this.subs = new ArrayList<>();
        this.classifyInvokers = new ArrayList<>();
    }

    public Assign(Collection<E> mainData) {
        this(mainData, 1, null);
    }

    public Assign(Assign<E> superAssign) {
        this(superAssign.mainData, superAssign.depth + 1, superAssign);
    }

    public <K, T> Acquire<E, K, T> addAcquire(Function<Collection<K>, Map<K, T>> fetcher) {
        Acquire<E, K, T> acquire = new Acquire<>(this, fetcher);
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
        Acquire<E, K, T> acquire = new Acquire<>(this, mapFetcher);
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

    public Assign<E> addBranch(Predicate<E> retain) {
        List<E> list = Streams.retain(this.mainData, retain).toList();
        return new Assign<>(list, this.depth + 1, this);
    }

    public Assign<E> addBranch() {
        return new Assign<>(this);
    }

    public Assign<E> addSub(Consumer<Collection<E>> sub) {
        this.subs.add(sub);
        return this;
    }

    @SafeVarargs
    public final <K> Assign<E> addClassifyInvoker(Function<E, K> classifier, ClassifyInvoker.KeyAssigner<E, K>... keyAssigner) {
        this.classifyInvokers.add(ClassifyInvoker.<E, K>builder().classifier(classifier).keyAssigners(List.of(keyAssigner)).build());
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
        if (depth < 1 || depth >= this.depth) {
            return (Assign<E>) EMPTY;
        }
        Assign<E> assign = this.backSuper();
        while (assign.depth > depth) {
            assign = assign.backSuper();
        }
        return assign;
    }

    public Assign<E> backSuperlative() {
        return backSuperTo(1);
    }

    public List<E> getMainData2List() {
        return new ArrayList<>(this.mainData);
    }

    public Assign<E> invoke() {
        this.parseClassifyInvokers();
        if (Objects.nonNull(this.superAssign)) {
            if (InvokeStatusEnum.CREATED.equals(this.superAssign.status)) {
                this.superAssign.invoke();
            }
            if (!InvokeStatusEnum.CREATED.equals(this.status)
                    || this.superAssign.interruptStrategy.interrupt(this.superAssign.status)) {
                return this;
            }
        }
        if (CollectionUtils.isEmpty(this.mainData)) {
            this.status = InvokeStatusEnum.ALL_SUCCESS;
            return this;
        }
        log.debug("invoke main, name:{}", this.name);
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
        this.branches.forEach(b -> {
            if (InvokeStatusEnum.CREATED.equals(b.status)) {
                b.invoke();
            }
        });
        this.subs.forEach(sub -> sub.accept(this.mainData));
        return this;
    }

    private void invokeMain() {
        this.mainData.forEach(e -> this.assignValues.forEach(a -> a.accept(e)));
        if (Objects.nonNull(this.executor)) {
            try {
                List<? extends CompletableFuture<? extends Map<?, ?>>> completableFutureList = this.acquires.stream()
                        .map(a -> CompletableFuture.supplyAsync(() -> a.fetch(this.mainData), this.executor))
                        .toList();
                CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
            } catch (Exception e) {
                log.error("Assign parallel fetch data exception", e);
            }
        } else {
            this.acquires.forEach(a -> a.fetch(this.mainData));
        }
        this.mainData.forEach(e -> this.acquires.forEach(a -> a.invoke(e)));
    }

    private void parseClassifyInvokers() {
        if (CollectionUtils.isEmpty(this.classifyInvokers)) {
            return;
        }
        this.classifyInvokers.forEach(c -> {
            Map<?, List<E>> keyMap = Streams.groupBy(this.mainData, c.getClassifier());
            Map<?, Function<Collection<E>, Assign<E>>> map = Streams.toMap(c.getKeyAssigners(),
                    ClassifyInvoker.KeyAssigner::getKey, ClassifyInvoker.KeyAssigner::getAssign);
            map.forEach((k, v) -> {
                List<E> list = keyMap.get(k);
                if (CollectionUtils.isEmpty(list)) {
                    return;
                }
                this.addSub(es -> v.apply(list));
            });
        });
    }

    public static <E> Assign<E> build(Collection<E> mainData) {
        return new Assign<>(mainData);
    }
}