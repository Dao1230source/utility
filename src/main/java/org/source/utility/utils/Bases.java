package org.source.utility.utils;

import java.util.Objects;
import java.util.function.Function;

public class Bases {
    private Bases() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T, R> R getOrDefault(T t, Function<T, R> getter, R defaultValue) {
        return Objects.nonNull(t) ? getter.apply(t) : defaultValue;
    }
}
