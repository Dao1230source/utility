package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

@UtilityClass
public class Asserts {

    public static <E extends RuntimeException> void isTrue(boolean expression, Supplier<E> exceptionSupplier) {
        if (!expression) {
            throw exceptionSupplier.get();
        }
    }

    public static <E extends RuntimeException> void isFalse(boolean expression, Supplier<E> exceptionSupplier) {
        isTrue(!expression, exceptionSupplier);
    }

    public static <T, E extends RuntimeException> void isNull(T t, Supplier<E> exceptionSupplier) {
        isTrue(Objects.isNull(t), exceptionSupplier);
    }

    public static <T, E extends RuntimeException> void nonNull(T t, Supplier<E> exceptionSupplier) {
        isTrue(Objects.nonNull(t), exceptionSupplier);
    }

    public static <E extends RuntimeException> void notEmpty(String str, Supplier<E> exceptionSupplier) {
        isTrue(StringUtils.hasText(str), exceptionSupplier);
    }

    public static <E extends RuntimeException> void isEmpty(String str, Supplier<E> exceptionSupplier) {
        isFalse(StringUtils.hasText(str), exceptionSupplier);
    }

    public static <T, E extends RuntimeException> void notEmpty(Collection<T> ts, Supplier<E> exceptionSupplier) {
        isFalse(CollectionUtils.isEmpty(ts), exceptionSupplier);
    }

    public static <T, E extends RuntimeException> void isEmpty(Collection<T> ts, Supplier<E> exceptionSupplier) {
        isTrue(CollectionUtils.isEmpty(ts), exceptionSupplier);
    }

    public static <T, E extends RuntimeException> void notEmpty(T[] ts, Supplier<E> exceptionSupplier) {
        isTrue(null != ts && ts.length != 0, exceptionSupplier);
    }

    public static <T, E extends RuntimeException> void isEmpty(T[] ts, Supplier<E> exceptionSupplier) {
        isFalse(null != ts && ts.length != 0, exceptionSupplier);
    }

}
