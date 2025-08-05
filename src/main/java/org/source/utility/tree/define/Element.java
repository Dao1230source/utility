package org.source.utility.tree.define;


import lombok.NonNull;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author zengfugen
 */
public interface Element<I> {
    /**
     * get id
     *
     * @return id
     */
    @NonNull
    I getId();

    /**
     * get parentId
     *
     * @return parentId
     */
    I getParentId();

    static <T extends Comparable<T>> int nullLast(T first, T second, BiFunction<T, T, Integer> comparator) {
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

    static <I extends Comparable<I>, E extends EnhanceElement<I>, S extends Comparable<S>> int comparator(E first, E second,
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

}