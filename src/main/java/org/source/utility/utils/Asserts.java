package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 断言工具类
 * <p>
 * 提供一系列断言方法，当断言条件不满足时抛出指定的异常。
 * 相比 Spring 的 Assert 类，本工具类支持自定义异常类型。
 * </p>
 *
 * @author zengfugen
 */
@UtilityClass
public class Asserts {

    /**
     * 断言条件为 true，否则抛出异常
     *
     * @param <E>               异常类型
     * @param expression        布尔表达式
     * @param exceptionSupplier 异常提供器
     * @throws E 当条件为 false 时抛出
     */
    public static <E extends RuntimeException> void isTrue(boolean expression, Supplier<E> exceptionSupplier) {
        if (!expression) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言条件为 false，否则抛出异常
     *
     * @param <E>               异常类型
     * @param expression        布尔表达式
     * @param exceptionSupplier 异常提供器
     * @throws E 当条件为 true 时抛出
     */
    public static <E extends RuntimeException> void isFalse(boolean expression, Supplier<E> exceptionSupplier) {
        isTrue(!expression, exceptionSupplier);
    }

    /**
     * 断言对象为 null，否则抛出异常
     *
     * @param <T>               对象类型
     * @param <E>               异常类型
     * @param t                 待断言的对象
     * @param exceptionSupplier 异常提供器
     * @throws E 当对象不为 null 时抛出
     */
    public static <T, E extends RuntimeException> void isNull(@Nullable T t, Supplier<E> exceptionSupplier) {
        isTrue(Objects.isNull(t), exceptionSupplier);
    }

    /**
     * 断言对象不为 null，否则抛出异常
     *
     * @param <T>               对象类型
     * @param <E>               异常类型
     * @param t                 待断言的对象
     * @param exceptionSupplier 异常提供器
     * @throws E 当对象为 null 时抛出
     */
    public static <T, E extends RuntimeException> void nonNull(@Nullable T t, Supplier<E> exceptionSupplier) {
        isTrue(Objects.nonNull(t), exceptionSupplier);
    }

    /**
     * 断言字符串非空（非 null 且非空字符串），否则抛出异常
     *
     * @param <E>               异常类型
     * @param str               待断言的字符串
     * @param exceptionSupplier 异常提供器
     * @throws E 当字符串为空时抛出
     */
    public static <E extends RuntimeException> void notEmpty(String str, Supplier<E> exceptionSupplier) {
        isTrue(StringUtils.isNoneBlank(str), exceptionSupplier);
    }

    /**
     * 断言字符串为空（null 或空字符串），否则抛出异常
     *
     * @param <E>               异常类型
     * @param str               待断言的字符串
     * @param exceptionSupplier 异常提供器
     * @throws E 当字符串非空时抛出
     */
    public static <E extends RuntimeException> void isEmpty(String str, Supplier<E> exceptionSupplier) {
        isFalse(StringUtils.isNoneBlank(str), exceptionSupplier);
    }

    /**
     * 断言集合非空，否则抛出异常
     *
     * @param <T>               集合元素类型
     * @param <E>               异常类型
     * @param ts                待断言的集合
     * @param exceptionSupplier 异常提供器
     * @throws E 当集合为空时抛出
     */
    public static <T, E extends RuntimeException> void notEmpty(Collection<T> ts, Supplier<E> exceptionSupplier) {
        isFalse(CollectionUtils.isEmpty(ts), exceptionSupplier);
    }

    /**
     * 断言集合为空，否则抛出异常
     *
     * @param <T>               集合元素类型
     * @param <E>               异常类型
     * @param ts                待断言的集合
     * @param exceptionSupplier 异常提供器
     * @throws E 当集合非空时抛出
     */
    public static <T, E extends RuntimeException> void isEmpty(Collection<T> ts, Supplier<E> exceptionSupplier) {
        isTrue(CollectionUtils.isEmpty(ts), exceptionSupplier);
    }

    /**
     * 断言数组非空（非 null 且长度不为 0），否则抛出异常
     *
     * @param <T>               数组元素类型
     * @param <E>               异常类型
     * @param ts                待断言的数组
     * @param exceptionSupplier 异常提供器
     * @throws E 当数组为空时抛出
     */
    public static <T, E extends RuntimeException> void notEmpty(@Nullable T[] ts, Supplier<E> exceptionSupplier) {
        isTrue(ts.length != 0, exceptionSupplier);
    }

    /**
     * 断言数组为空（null 或长度为 0），否则抛出异常
     *
     * @param <T>               数组元素类型
     * @param <E>               异常类型
     * @param ts                待断言的数组
     * @param exceptionSupplier 异常提供器
     * @throws E 当数组非空时抛出
     */
    public static <T, E extends RuntimeException> void isEmpty(@Nullable T[] ts, Supplier<E> exceptionSupplier) {
        isFalse(Objects.isNull(ts) || ts.length == 0, exceptionSupplier);
    }

}
