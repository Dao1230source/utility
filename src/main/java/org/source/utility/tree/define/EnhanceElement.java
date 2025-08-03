package org.source.utility.tree.define;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @param <I> ID
 */
public abstract class EnhanceElement<I extends Comparable<I>> implements Element<I>, Comparable<EnhanceElement<I>> {

    public abstract I getSourceId();

    public static <I extends Comparable<I>, E extends EnhanceElement<I>, S extends Comparable<S>> int comparator(E first, E second,
                                                                                                                 Function<E, S> sortedGetter) {
        return nullLast(first, second, (f, s) -> {
            int res = 0;
            if (Objects.nonNull(sortedGetter)) {
                S firstSorted = sortedGetter.apply(first);
                S secondSorted = sortedGetter.apply(second);
                res = nullLast(firstSorted, secondSorted, S::compareTo);
            }
            if (res == 0) {
                res = first.getId().compareTo(second.getId());
            }
            return res;
        });
    }

    public static <T extends Comparable<T>> int nullLast(T first, T second, BiFunction<T, T, Integer> comparator) {
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
}