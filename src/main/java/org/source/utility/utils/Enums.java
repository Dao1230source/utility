package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.source.utility.constant.Constants;
import org.source.utility.function.SFunction;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 枚举工具类
 * <p>
 * 提供枚举与 Map 之间的转换功能，支持通过方法引用获取枚举值作为 Key 或 Value。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>将枚举转换为 Map，支持自定义 Key 和 Value 获取器</li>
 *   <li>根据 Key 获取对应的枚举实例</li>
 *   <li>缓存转换结果，提高性能</li>
 * </ul>
 * </p>
 *
 * @author zengfugen
 */
@UtilityClass
@Slf4j
public class Enums {

    /**
     * 枚举 Map 缓存
     * <p>
     * Key：枚举类名与字段名的组合
     * Value：Key 到枚举实例的映射
     * </p>
     */
    private static final Map<String, Map<Object, Enum<?>>> ENUM_MAP = new ConcurrentHashMap<>(32);

    /**
     * 将枚举类转换为 Map，以指定字段作为 Key
     * <p>
     * 转换结果会被缓存，相同参数的后续调用将直接返回缓存结果。
     * </p>
     *
     * @param <E>      枚举类型
     * @param <K>      Key 类型
     * @param enumClass 枚举类
     * @param keyGetter Key 获取器，通过方法引用指定作为 Map Key 的字段
     * @return Key 到枚举实例的 Map
     */
    public static <E extends Enum<E>, K> Map<Object, Enum<?>> enumToMap(Class<E> enumClass, SFunction<E, K> keyGetter) {
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

    /**
     * 将枚举类转换为 Map，以指定字段作为 Key，枚举实例作为 Value
     *
     * @param <E>       枚举类型
     * @param <K>       Key 类型
     * @param enumClass  枚举类
     * @param keyGetter Key 获取器，通过方法引用指定作为 Map Key 的字段
     * @return Key 到枚举实例的 Map
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K> Map<K, E> toMap(Class<E> enumClass, SFunction<E, K> keyGetter) {
        Map<Object, Enum<?>> map = enumToMap(enumClass, keyGetter);
        Map<K, E> genericMap = new ConcurrentHashMap<>(map.size());
        map.forEach((k, v) -> genericMap.put((K) k, (E) v));
        return genericMap;
    }

    /**
     * 将枚举类转换为 Map，以指定字段作为 Key 和 Value
     *
     * @param <E>         枚举类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @param enumClass   枚举类
     * @param keyGetter   Key 获取器
     * @param valueGetter Value 获取器
     * @return Key 到 Value 的 Map
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K, V> Map<K, V> toMap(Class<E> enumClass, SFunction<E, K> keyGetter,
                                                            SFunction<E, V> valueGetter) {
        Map<Object, Enum<?>> map = enumToMap(enumClass, keyGetter);
        Map<K, V> genericMap = new ConcurrentHashMap<>(map.size());
        map.forEach((k, v) -> genericMap.put((K) k, valueGetter.apply((E) v)));
        return genericMap;
    }

    /**
     * 根据 Key 获取对应的枚举实例
     *
     * @param <E>       枚举类型
     * @param <K>       Key 类型
     * @param enumClass 枚举类
     * @param keyGetter Key 获取器
     * @param k         要查找的 Key
     * @return 对应的枚举实例，未找到返回 null
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, K> @Nullable E getEnum(Class<E> enumClass,
                                                             SFunction<E, K> keyGetter,
                                                             @Nullable K k) {
        if (Objects.isNull(k)) {
            return null;
        }
        return (E) enumToMap(enumClass, keyGetter).get(k);
    }

    /**
     * 根据 Key 获取枚举实例，并返回指定字段的值
     *
     * @param <E>         枚举类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @param enumClass   枚举类
     * @param keyGetter   Key 获取器
     * @param k           要查找的 Key
     * @param valueGetter Value 获取器
     * @return 对应枚举实例的指定字段值，未找到返回 null
     */
    public static <E extends Enum<E>, K, V> @Nullable V getValue(Class<E> enumClass,
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