package org.source.utility.utils;

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
@Slf4j
public class Enums {
    private Enums() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<String, Map<Object, Enum<?>>> ENUM_MAP = new ConcurrentHashMap<>(32);

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K> E getEnum(Class<E> enumClass,
                                                   SFunction<E, K> keyGetter,
                                                   K k) {
        if (Objects.isNull(k) || Objects.isNull(enumClass) || Objects.isNull(keyGetter)) {
            return null;
        }
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("class不是一个枚举类: " + enumClass.getName());
        }
        String enumName = enumClass.getSimpleName();
        String fieldName = "";
        try {
            fieldName = Lambdas.getFieldName(keyGetter);
        } catch (Exception e) {
            log.debug("get field name error", e);
        }
        String key = String.join(Constants.UNDERSCORE, enumName, fieldName);
        ENUM_MAP.computeIfAbsent(key, v -> {
            E[] enums = enumClass.getEnumConstants();
            return Arrays.stream(enums).collect(Collectors.toConcurrentMap(keyGetter, e -> e));
        });
        return (E) ENUM_MAP.get(key).get(k);
    }

    public static <E extends Enum<E>, K, V> V getValue(Class<E> enumClass,
                                                       SFunction<E, K> keyGetter,
                                                       K k,
                                                       SFunction<E, V> valueGetter) {
        E e = getEnum(enumClass, keyGetter, k);
        if (Objects.nonNull(e)) {
            return valueGetter.apply(e);
        }
        return null;
    }

}
