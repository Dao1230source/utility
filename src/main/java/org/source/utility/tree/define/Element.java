package org.source.utility.tree.define;


import org.springframework.lang.Nullable;

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
    I getId();

    /**
     * get parentId
     *
     * @return parentId
     */
    @Nullable
    I getParentId();

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

    static <I extends Comparable<I>, E extends EnhanceElement<I>, S extends Comparable<S>> int comparator(
            @Nullable E first, @Nullable E second, @Nullable Function<E, S> sortedGetter) {
        return nullLast(first, second, (f, s) -> {
            int res = 0;
            if (Objects.nonNull(sortedGetter)) {
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