package org.source.utility.utils;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@UtilityClass
public class Maps {
    public static <K, V, P> P getPropertyOrDefault(Map<K, V> map, K k, Function<V, P> propertyGetter, P defaultProperty) {
        V v = map.get(k);
        if (Objects.nonNull(v)) {
            return propertyGetter.apply(v);
        }
        return defaultProperty;
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return Objects.isNull(map) || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
}
