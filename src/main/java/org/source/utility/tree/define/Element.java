package org.source.utility.tree.define;


import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 树元素接口
 * <p>
 * 定义树节点包含的元素必须实现的基本方法。
 * 所有树节点的元素都必须实现此接口，提供 ID 和父节点 ID 的获取能力。
 * </p>
 *
 * @param <I> ID 类型，必须实现 Comparable 接口
 * @author zengfugen
 */
public interface Element<I> {
    /**
     * 获取元素 ID
     *
     * @return 元素 ID
     */
    I getId();

    /**
     * 获取父节点 ID
     *
     * @return 父节点 ID，根节点返回 null
     */
    @Nullable
    I getParentId();

    /**
     * null 值排最后的比较器
     * <p>
     * 在排序时将 null 值排在最后，与正常值比较时使用提供的比较器。
     * </p>
     *
     * @param <T>        可比较类型
     * @param first      第一个值
     * @param second     第二个值
     * @param comparator 比较器
     * @return 比较结果：负数表示 first 小于 second，0 表示相等，正数表示 first 大于 second
     */
    static <T extends Comparable<T>> int nullLast(@Nullable T first, @Nullable T second, BiFunction<T, T, Integer> comparator) {
        // null 排最后
        if (Objects.isNull(first)) {
            return 1;
        }
        if (Objects.isNull(second)) {
            return -1;
        }
        if (first == second) {
            return 0;
        }
        return comparator.apply(first, second);
    }

    /**
     * 增强元素的比较器
     * <p>
     * 支持按指定字段排序，null 值排最后。
     * 当指定字段相同时，按 ID 排序。
     * </p>
     *
     * @param <I>          ID 类型
     * @param <E>          元素类型
     * @param <S>          排序字段类型
     * @param first        第一个元素
     * @param second       第二个元素
     * @param sortedGetter 排序字段获取器，可以为 null
     * @return 比较结果
     */
    static <I extends Comparable<I>, E extends EnhanceElement<I>, S extends Comparable<S>> int comparator(
            @Nullable E first, @Nullable E second, @Nullable Function<E, S> sortedGetter) {
        return nullLast(first, second, (f, s) -> {
            int res = 0;
            if (Objects.nonNull(sortedGetter) && Objects.nonNull(first) && Objects.nonNull(second)) {
                S firstSorted = sortedGetter.apply(first);
                S secondSorted = sortedGetter.apply(second);
                res = nullLast(firstSorted, secondSorted, S::compareTo);
            }
            if (res == 0) {
                res = f.getId().compareTo(s.getId());
            }
            return res;
        });
    }

}