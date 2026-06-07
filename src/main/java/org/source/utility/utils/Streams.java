package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jspecify.annotations.Nullable;

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
 * Stream 流处理工具类
 * <p>
 * 提供丰富的集合流处理功能，包括过滤、分组、映射、转换等操作。
 * 支持自动并行流切换，当集合大小超过阈值（默认 1000）时自动使用并行流。
 * </p>
 * <p>
 * 关于副作用和并行流的注意事项：
 * </p>
 * <pre>
 * A method or an expression has a side effect if, in addition to returning or producing a value,
 * it also modifies the state of the computer. Examples include mutable reductions
 * (operations that use the collect operation) as well as invoking the System.out.println
 * method for debugging.
 *
 * Operations like forEach and peek are designed for side effects; you should use them with care.
 * If you use one of these operations with a parallel stream, the Java runtime may invoke the
 * lambda expression concurrently from multiple threads.
 * In addition, never pass as parameters lambda expressions that have side effects in operations
 * such as filter and map, as this can return inconsistent or unpredictable results.
 * </pre>
 *
 * @see <a href="https://docs.oracle.com/javase/tutorial/collections/streams/parallelism.html">Parallelism</a>
 * @author zengfugen
 */
@UtilityClass
@Slf4j
public class Streams {

    /**
     * 流并行处理的阈值
     * 当集合大小超过此值时，自动使用并行流
     */
    private static final int STREAM_PARALLEL_THRESHOLD = 1000;

    /**
     * 获取流并行处理的阈值
     *
     * @return 并行流阈值
     */
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
                                          Predicate<E> predicate) {
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
                                             BiPredicate<E, T> biPredicate,
                                             Collection<T>... collections) {
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
                                                Function<E, K> ekGetter,
                                                Function<T, K> tkGetter,
                                                Collection<T>... collections) {
        Set<K> tkSet = flat(collections).map(tkGetter).collect(Collectors.toSet());
        return of(es).filter(k -> !tkSet.contains(ekGetter.apply(k)));
    }

    @SafeVarargs
    public static <E, K> Stream<E> notRetain(Collection<E> es,
                                             Function<E, K> ekGetter,
                                             Collection<E>... collections) {
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
    public static <E, T> List<E> retain(List<E> eList, BiPredicate<E, T> biPredicate, List<T>... tLists) {
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
    public static <E> Stream<E> retain(Collection<E> eList, Predicate<E> predicate) {
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
    public static <E, T, K> Stream<E> retain(Collection<E> es,
                                             Function<E, K> ekGetter,
                                             Function<T, K> tkGetter,
                                             Collection<T>... collections) {
        Set<K> tkSet = flat(collections).map(tkGetter).collect(Collectors.toSet());
        return of(es).filter(k -> tkSet.contains(ekGetter.apply(k)));
    }

    @SafeVarargs
    public static <E, T, K> Stream<E> retain(Collection<E> es,
                                             Function<E, K> ekGetter,
                                             Function<T, K> tkGetter,
                                             Predicate<K> ekPredicate,
                                             Collection<T>... collections) {
        Set<K> tkSet = flat(collections).map(tkGetter).collect(Collectors.toSet());
        return of(es).filter(k -> ekPredicate.test(ekGetter.apply(k)) && tkSet.contains(ekGetter.apply(k)));
    }

    @SafeVarargs
    public static <E, K> Stream<E> retain(Collection<E> es,
                                          Function<E, K> ekGetter,
                                          Collection<E>... collections) {
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
    public static <E, K> Stream<E> retain(Collection<E> eList, Function<E, K> keyGetter, K... values) {
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
    public static <E, K> boolean exists(Collection<E> es, Function<E, K> ekGetter, K key) {
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
    public static <E, K> boolean notExists(Collection<E> es, Function<E, K> ekGetter, K key) {
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
    public static <K, E> Map<K, List<E>> groupBy(Collection<E> eCollection, Function<E, @Nullable K> groupBy) {
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
    public static <E, K, V> Map<K, V> toMap(Collection<E> es, Function<E, K> mapKey,
                                            Function<E, V> mapValue) {
        return of(es).collect(Collectors.toMap(mapKey, mapValue, (v1, v2) -> v1));
    }

    /**
     * 将列表转为 Map，以指定字段作为 Key，对象本身作为 Value
     * Key 重复时，保留第一个
     *
     * @param <E>    列表元素类型
     * @param <K>    Map Key 类型
     * @param es     源列表
     * @param mapKey Key 获取器
     * @return Map
     */
    public static <E, K> Map<K, E> toMap(Collection<E> es, Function<E, @Nullable K> mapKey) {
        return toMap(es, mapKey, v -> v);
    }

    /**
     * 将数组转为 Map，以指定字段作为 Key
     * Key 重复时，保留第一个
     *
     * @param <E>      数组元素类型
     * @param <K>      Map Key 类型
     * @param <V>      Map Value 类型
     * @param es       源数组
     * @param mapKey   Key 获取器
     * @param mapValue Value 获取器
     * @return Map
     */
    public static <E, K, V> Map<K, V> toMap(E[] es, Function<E, K> mapKey,
                                            Function<E, V> mapValue) {
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
    public static <E> BigDecimal sum(List<E> eList, Function<E, BigDecimal> mapper) {
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
    public static <E, K> Optional<E> findFirst(Collection<E> collection, Function<E, K> keyGetter, K value) {
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
    public static <E> Optional<E> findFirst(Collection<E> collection, Predicate<E> predicate) {
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
    public static <E, T> Stream<T> map(@Nullable Collection<E> collection, Function<E, @Nullable T> map) {
        return of(collection).map(map).filter(Objects::nonNull);
    }

    /**
     * 对列表自身做一些操作
     *
     * @param eList    列表
     * @param consumer 操作
     * @param <E>      泛型
     * @apiNote 注意：此操作可能会改变应用对象本身的数据
     */
    public static <E> void forEach(Collection<E> eList, Consumer<E> consumer) {
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
    public static <E> List<E> distinct(List<E> eList, Comparator<? super E> comparator,
                                       Predicate<E> filter) {
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
        if (es.length == 0) {
            return Stream.empty();
        }
        Stream<Collection<E>> collStream = Arrays.stream(es).filter(CollectionUtils::isNotEmpty);
        return flatMap(collStream, Collection::stream);
    }

    /**
     * 将集合中的每个元素映射为 Stream 并合并
     *
     * @param <T>         集合元素类型
     * @param <R>         Stream 元素类型
     * @param tCollection 源集合
     * @param mapper      映射函数
     * @return 合并后的 Stream
     */
    public static <T, R> Stream<R> flatMap(Collection<T> tCollection, Function<T, Stream<R>> mapper) {
        return flatMap(of(tCollection), mapper);
    }

    /**
     * 将 Stream 中的每个元素映射为 Stream 并合并
     *
     * @param <T>     Stream 元素类型
     * @param <R>     目标 Stream 元素类型
     * @param eStream 源 Stream
     * @param mapper  映射函数
     * @return 合并后的 Stream
     */
    public static <T, R> Stream<R> flatMap(Stream<T> eStream, Function<T, Stream<R>> mapper) {
        return eStream.flatMap(mapper);
    }

    /**
     * 将列表分批处理
     *
     * @param <T>       列表元素类型
     * @param list      源列表
     * @param batchSize 每批大小
     * @return 分批后的列表
     */
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

    /**
     * 将数组转换为 Stream
     * 当数组长度超过阈值时自动使用并行流
     *
     * @param <E> 数组元素类型
     * @param es  数组
     * @return Stream
     */
    @SafeVarargs
    public static <E> Stream<E> of(E... es) {
        if (es.length == 0) {
            return Stream.empty();
        }
        if (es.length > getStreamParallelThreshold()) {
            return Arrays.stream(es).parallel();
        }
        return Arrays.stream(es);
    }

    /**
     * 将集合转换为 Stream
     * 当集合大小超过阈值时自动使用并行流
     *
     * @param <E> 集合元素类型
     * @param es  集合
     * @return Stream
     */
    public static <E> Stream<E> of(@Nullable Collection<E> es) {
        if (CollectionUtils.isEmpty(es)) {
            return Stream.empty();
        }
        if (es.size() >= getStreamParallelThreshold()) {
            return Collections.synchronizedCollection(es).parallelStream();
        }
        return es.stream();
    }

}
