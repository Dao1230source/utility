package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.constant.Constants;
import org.source.utility.function.SFunction;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zengfugen
 */
@UtilityClass
@Slf4j
public class Enums {

    private static final Map<String, Map<Object, Enum<?>>> ENUM_MAP = new ConcurrentHashMap<>(32);

    public static <E extends Enum<E>, K> Map<Object, Enum<?>> enumToMap(Class<E> enumClass, SFunction<E, K> keyGetter) {
        if (Objects.isNull(enumClass) || Objects.isNull(keyGetter)) {
            return Map.of();
        }
        if (!enumClass.isEnum()) {
            log.warn("class不是一个枚举类: {}", enumClass.getName());
            return Map.of();
        }
        String enumName = enumClass.getSimpleName();
        String fieldName = "";
        try {
            fieldName = Lambdas.getFieldName(keyGetter);
        } catch (Exception e) {
            log.warn("get field name error", e);
        }
        String key = String.join(Constants.UNDERSCORE, enumName, fieldName);
        return ENUM_MAP.computeIfAbsent(key, v -> {
            E[] enums = enumClass.getEnumConstants();
            return Arrays.stream(enums).collect(Collectors.toConcurrentMap(keyGetter, e -> e));
        });
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K> Map<K, E> toMap(Class<E> enumClass, SFunction<E, K> keyGetter) {
        Map<Object, Enum<?>> map = enumToMap(enumClass, keyGetter);
        Map<K, E> genericMap = new ConcurrentHashMap<>(map.size());
        map.forEach((k, v) -> genericMap.put((K) k, (E) v));
        return genericMap;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K, V> Map<K, V> toMap(Class<E> enumClass, SFunction<E, K> keyGetter,
                                                            SFunction<E, V> valueGetter) {
        if (Objects.isNull(valueGetter)) {
            return Map.of();
        }
        Map<Object, Enum<?>> map = enumToMap(enumClass, keyGetter);
        Map<K, V> genericMap = new ConcurrentHashMap<>(map.size());
        map.forEach((k, v) -> genericMap.put((K) k, valueGetter.apply((E) v)));
        return genericMap;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K> E getEnum(Class<E> enumClass,
                                                   SFunction<E, K> keyGetter,
                                                   K k) {
        if (Objects.isNull(k)) {
            return null;
        }
        return (E) enumToMap(enumClass, keyGetter).get(k);
    }

    public static <E extends Enum<E>, K, V> V getValue(Class<E> enumClass,
                                                       SFunction<E, K> keyGetter,
                                                       K k,
                                                       SFunction<E, V> valueGetter) {
        if (Objects.isNull(valueGetter)) {
            return null;
        }
        E e = getEnum(enumClass, keyGetter, k);
        if (Objects.nonNull(e)) {
            return valueGetter.apply(e);
        }
        return null;
    }

}