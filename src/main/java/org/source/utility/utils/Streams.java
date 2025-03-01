package org.source.utility.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * <a href="https://docs.oracle.com/javase/tutorial/collections/streams/parallelism.html">...</a>
 * <pre>
 * A method or an expression has a side effect if, in addition to returning or producing a value,it also modifies the state of the computer.
 * Examples include mutable reductions (operations that use the collect operation;
 * see the section Reduction for more information) as well as invoking the System.out.println method for debugging.
 * The JDK handles certain side effects in pipelines well.
 * In particular, the collect method is designed to perform the most common stream operations that have side effects in a parallel-safe manner.
 * Operations like forEach and peek are designed for side effects;
 * a lambda expression that returns void, such as one that invokes System.out.println, can do nothing but have side effects.
 * Even so, you should use the forEach and peek operations with care;
 * if you use one of these operations with a parallel stream,
 * then the Java runtime may invoke the lambda expression that you specified as its parameter concurrently from multiple threads.
 * In addition, never pass as parameters lambda expressions that have side effects in operations such as filter and map.
 * The following sections discuss interference and stateful lambda expressions,
 * both of which can be sources of side effects and can return inconsistent or unpredictable results, especially in parallel streams.
 * However, the concept of laziness is discussed first, because it has a direct effect on interference.
 * <pre/>
 * @author zengfugen
 */
@UtilityClass
@Slf4j
public class Streams {

    private static final int STREAM_PARALLEL_THRESHOLD = 1000;

    public static int getStreamParallelThreshold() {
        return STREAM_PARALLEL_THRESHOLD;
    }

    /**
     * 根据条件过滤列表
     *
     * @param es        源列表
     * @param predicate 过滤条件
     * @param <E>       源列表泛型
     * @return 过滤后的结果
     */
    public static <E> Stream<E> notRetain(Collection<E> es,
                                          @NonNull Predicate<E> predicate) {
        return of(es).filter(predicate.negate());
    }


    /**
     * 通过biPredicate运算，e集合存在但t集合不存在的部分保留
     *
     * @param es          源集合
     * @param biPredicate 过滤条件
     * @param collections 目标集合(可以多个)
     * @param <E>         源列表泛型
     * @param <T>         条件列表泛型
     * @return 过滤后的结果
     */
    @SafeVarargs
    public static <E, T> Stream<E> notRetain(Collection<E> es,
                                             @NonNull BiPredicate<E, T> biPredicate,
                                             @NonNull Collection<T>... collections) {
        List<T> list = flat(collections).toList();
        return of(es).filter(k -> of(list).noneMatch(e -> biPredicate.test(k, e)));
    }

    /**
     * 通过key的比较，e集合存在但t集合不存在的部分保留
     *
     * @param es          es
     * @param ekGetter    对象e的key
     * @param tkGetter    对象t的key
     * @param collections t集合
     * @param <E>         e
     * @param <T>         t
     * @param <K>         key
     * @return e的stream
     */
    @SafeVarargs
    public static <E, T, K> Stream<E> notRetain(Collection<E> es,
                                                @NonNull Function<E, K> ekGetter,
                                                @NonNull Function<T, K> tkGetter,
                                                @NonNull Collection<T>... collections) {
        Set<K> tkSet = flat(collections).map(tkGetter).collect(Collectors.toSet());
        return of(es).filter(k -> !tkSet.contains(ekGetter.apply(k)));
    }

    @SafeVarargs
    public static <E, K> Stream<E> notRetain(Collection<E> es,
                                             @NonNull Function<E, K> ekGetter,
                                             @NonNull Collection<E>... collections) {
        Set<K> tkSet = flat(collections).map(ekGetter).collect(Collectors.toSet());
        return of(es).filter(k -> !tkSet.contains(ekGetter.apply(k)));
    }

    /**
     * 源列表eList根据条件列表tList的指定条件biPredicate选取结果
     *
     * @param eList       源列表
     * @param tLists      条件列表(可以多个)
     * @param biPredicate 选取条件
     * @param <E>         源列表泛型
     * @param <T>         条件列表泛型
     * @return 选取后的结果
     */
    @SafeVarargs
    public static <E, T> List<E> retain(List<E> eList, @NonNull BiPredicate<E, T> biPredicate, List<T>... tLists) {
        if (tLists.length == 0) {
            return eList;
        }
        List<T> list = flat(tLists).toList();
        return of(eList).filter(k -> of(list).anyMatch(e -> biPredicate.test(k, e))).toList();
    }

    /**
     * 根据条件选取
     *
     * @param eList     源列表
     * @param predicate 过滤条件
     * @param <E>       源列表泛型
     * @return 选取结果
     */
    public static <E> Stream<E> retain(Collection<E> eList, @NonNull Predicate<E> predicate) {
        return of(eList).filter(predicate);
    }

    /**
     * 通过key的比较，e集合存在且t集合也存在的部分保留
     *
     * @param es          es
     * @param ekGetter    对象e的key
     * @param tkGetter    对象t的key
     * @param collections t集合
     * @param <E>         e
     * @param <T>         t
     * @param <K>         key
     * @return e的stream
     */
    @SafeVarargs
    public static <E, T, K> Stream<E> retain(@NonNull Collection<E> es,
                                             @NonNull Function<E, K> ekGetter,
                                             @NonNull Function<T, K> tkGetter,
                                             @NonNull Collection<T>... collections) {
        Set<K> tkSet = flat(collections).map(tkGetter).collect(Collectors.toSet());
        return of(es).filter(k -> tkSet.contains(ekGetter.apply(k)));
    }

    @SafeVarargs
    public static <E, T, K> Stream<E> retain(@NonNull Collection<E> es,
                                             @NonNull Function<E, K> ekGetter,
                                             @NonNull Function<T, K> tkGetter,
                                             @NonNull Predicate<K> ekPredicate,
                                             @NonNull Collection<T>... collections) {
        Set<K> tkSet = flat(collections).map(tkGetter).collect(Collectors.toSet());
        return of(es).filter(k -> ekPredicate.test(ekGetter.apply(k)) && tkSet.contains(ekGetter.apply(k)));
    }

    @SafeVarargs
    public static <E, K> Stream<E> retain(@NonNull Collection<E> es,
                                          @NonNull Function<E, K> ekGetter,
                                          @NonNull Collection<E>... collections) {
        Set<K> tkSet = flat(collections).map(ekGetter).collect(Collectors.toSet());
        return of(es).filter(k -> tkSet.contains(ekGetter.apply(k)));
    }

    /**
     * 源列表eList根据指定字段key的某些值ks选取结果
     *
     * @param eList     源列表
     * @param keyGetter 指定字段
     * @param values    指定字段值
     * @param <E>       源列表泛型
     * @param <K>       指定字段泛型
     * @return 选取结果
     */
    @SafeVarargs
    public static <E, K> Stream<E> retain(Collection<E> eList, @NonNull Function<E, K> keyGetter, K... values) {
        Set<K> kSet = of(values).collect(Collectors.toSet());
        return of(eList).filter(k -> kSet.contains(keyGetter.apply(k)));
    }

    /**
     * 列表中是否存在某个值
     *
     * @param es       e集合
     * @param ekGetter 条件
     * @param key      条件
     * @param <E>      源列表泛型
     * @return 是否存在
     */
    public static <E, K> boolean exists(Collection<E> es, @NonNull Function<E, K> ekGetter, K key) {
        return of(es).anyMatch(k -> key.equals(ekGetter.apply(k)));
    }

    /**
     * 列表中是否不存在某个值
     *
     * @param es       e集合
     * @param ekGetter 条件
     * @param key      条件
     * @param <E>      源列表泛型
     * @return 是否不存在
     */
    public static <E, K> boolean notExists(Collection<E> es, @NonNull Function<E, K> ekGetter, K key) {
        return of(es).noneMatch(k -> key.equals(ekGetter.apply(k)));
    }

    /**
     * 列表按指定字段分组
     *
     * @param eCollection 源列表
     * @param groupBy     分组条件
     * @param <K>         源列表泛型
     * @param <E>         分组条件泛型
     * @return 结果Map
     */
    public static <K, E> Map<K, List<E>> groupBy(@NonNull Collection<E> eCollection, @NonNull Function<E, K> groupBy) {
        Stream<E> stream = of(eCollection);
        if (stream.isParallel()) {
            return stream.collect(Collectors.groupingByConcurrent(groupBy));
        }
        return stream.collect(Collectors.groupingBy(groupBy));
    }

    /**
     * list转为map，指定key、value
     * key 重复时，保留第一个
     *
     * @param es       源列表
     * @param mapKey   分组条件 要求 key 值非空
     * @param mapValue value 要求 value 值非空
     * @param <E>      列表泛型
     * @param <K>      Map key泛型
     * @param <V>      Map value泛型
     * @return Map
     */
    public static <E, K, V> Map<K, V> toMap(@NonNull Collection<E> es, @NonNull Function<E, K> mapKey,
                                            @NonNull Function<E, V> mapValue) {
        return of(es).collect(Collectors.toMap(mapKey, mapValue, (v1, v2) -> v1));
    }

    public static <E, K> Map<K, E> toMap(@NonNull Collection<E> es, @NonNull Function<E, K> mapKey) {
        return toMap(es, mapKey, v -> v);
    }

    public static <E, K, V> Map<K, V> toMap(@NonNull E[] es, @NonNull Function<E, K> mapKey,
                                            @NonNull Function<E, V> mapValue) {
        return of(es).collect(Collectors.toMap(mapKey, mapValue, (v1, v2) -> v1));
    }

    /**
     * amount求和
     *
     * @param eList  列表
     * @param mapper 返回 BigDecimal 的函数
     * @param <E>    泛型
     * @return 求和结果
     */
    public static <E> BigDecimal sum(List<E> eList, @NonNull Function<E, BigDecimal> mapper) {
        if (CollectionUtils.isEmpty(eList)) {
            return BigDecimal.ZERO;
        }
        return of(eList).map(mapper).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 从列表list中根据字段keyGetter的值value获取对象
     *
     * @param collection 列表
     * @param keyGetter  指定字段
     * @param value      指定字段值
     * @param <E>        列表泛型
     * @param <K>        指定字段泛型
     * @return 符合条件的第一个值
     */
    public static <E, K> Optional<E> findFirst(@NonNull Collection<E> collection, @NonNull Function<E, K> keyGetter, @NonNull K value) {
        return of(collection).filter(e -> value.equals(keyGetter.apply(e))).findFirst();
    }

    /**
     * 从列表list中根据字段keyGetter的值value获取对象
     *
     * @param collection 列表
     * @param predicate  指定条件
     * @param <E>        列表泛型
     * @return 符合条件的第一个值
     */
    public static <E> Optional<E> findFirst(@NonNull Collection<E> collection, @NonNull Predicate<E> predicate) {
        return of(collection).filter(predicate).findFirst();
    }

    /**
     * 源列表S转换成目标列表T
     *
     * @param collection 源列表
     * @param map        映射函数
     * @param <E>        源列表泛型
     * @param <T>        目标列表泛型
     * @return 目标列表
     */
    public static <E, T> Stream<T> map(Collection<E> collection, @NonNull Function<E, T> map) {
        return of(collection).map(map);
    }

    /**
     * 对列表自身做一些操作
     *
     * @param eList    列表
     * @param consumer 操作
     * @param <E>      泛型
     * @apiNote 注意：此操作可能会改变应用对象本身的数据
     */
    public static <E> void forEach(Collection<E> eList, @NonNull Consumer<E> consumer) {
        Stream<E> stream = of(eList);
        if (stream.isParallel()) {
            stream.forEachOrdered(consumer);
        } else {
            stream.forEach(consumer);
        }
    }

    /**
     * 列表排序
     *
     * @param eList      列表
     * @param comparator 比较器
     * @param <E>        泛型
     * @return 排序后列表
     */
    public static <E> Stream<E> sort(Collection<E> eList, Comparator<E> comparator) {
        return of(eList).sorted(comparator);
    }

    /**
     * 列表去重
     *
     * @param eList      列表
     * @param comparator 去重的比较器
     * @param filter     过滤器
     * @param <E>        T
     * @return 结果
     */
    public static <E> List<E> distinct(List<E> eList, @NonNull Comparator<? super E> comparator,
                                       @NonNull Predicate<E> filter) {
        return eList.stream()
                .filter(filter)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(comparator)), ArrayList::new));
    }

    /**
     * 将多个集合 降维放平 成一个集合
     *
     * @param es  多个集合
     * @param <E> e
     * @return stream
     */
    @SafeVarargs
    public static <E> Stream<E> flat(Collection<E>... es) {
        if (null == es || es.length == 0) {
            return Stream.empty();
        }
        Stream<Collection<E>> collStream = Arrays.stream(es).filter(CollectionUtils::isNotEmpty);
        return flatMap(collStream, Collection::stream);
    }

    public static <T, R> Stream<R> flatMap(Collection<T> tCollection, Function<T, Stream<R>> mapper) {
        return flatMap(of(tCollection), mapper);
    }

    public static <T, R> Stream<R> flatMap(@NonNull Stream<T> eStream, Function<T, Stream<R>> mapper) {
        return eStream.flatMap(mapper);
    }

    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        if (batchSize <= 0) {
            return List.of(list);
        }
        return IntStream.range(0, (list.size() + batchSize - 1) / batchSize).mapToObj(i ->
                list.subList(i * batchSize, Math.min((i + 1) * batchSize, list.size()))).toList();
    }

    @SafeVarargs
    public static <E> Stream<E> of(E... es) {
        if (Objects.isNull(es) || es.length == 0) {
            return Stream.empty();
        }
        if (es.length > getStreamParallelThreshold()) {
            return Arrays.stream(es).parallel();
        }
        return Arrays.stream(es);
    }

    public static <E> Stream<E> of(Collection<E> es) {
        if (null == es || es.isEmpty()) {
            return Stream.empty();
        }
        if (es.size() >= getStreamParallelThreshold()) {
            return Collections.synchronizedCollection(es).parallelStream();
        }
        return es.stream();
    }

}
