package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jspecify.annotations.Nullable;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lambda 表达式工具类
 * <p>
 * 提供从 Lambda 表达式中提取字段名、方法名和类信息的能力。
 * 主要用于 {@link org.source.utility.function.SFunction} 等函数式接口，
 * 支持通过方法引用获取字段名，避免硬编码字符串。
 * </p>
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 *   String fieldName = Lambdas.getFieldName(User::getName); // 返回 "name"
 *   String methodName = Lambdas.getMethodName(User::getName); // 返回 "getName"
 *   Class<User> clazz = Lambdas.getClass(User::getName); // 返回 User.class
 * </pre>
 *
 * @author zengfugen
 */
@UtilityClass
public class Lambdas {

    /**
     * Lambda 缓存映射
     * Key：Lambda 类名
     * Value：序列化后的 Lambda 对象
     */
    private static final Map<String, SerializedLambda> LAMBDA_MAP = new ConcurrentHashMap<>(32);

    /**
     * 从 Lambda 表达式中提取字段名
     *
     * @param serializable Lambda 表达式
     * @return 字段名，提取失败返回 null
     */
    public static @Nullable String getFieldName(Serializable serializable) {
        return PropertyUtil.toFieldName(getMethodName(serializable));
    }

    /**
     * 从 Lambda 表达式中提取方法名
     *
     * @param serializable Lambda 表达式
     * @return 方法名
     */
    public static String getMethodName(Serializable serializable) {
        return resolve(serializable).getImplMethodName();
    }

    /**
     * 从 Lambda 表达式中提取类信息
     *
     * @param <T>           目标类型
     * @param serializable  Lambda 表达式
     * @return 类对象
     * @throws ClassNotFoundException 类未找到时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(Serializable serializable) throws ClassNotFoundException {
        String className = resolve(serializable).getImplClass();
        className = className.replace(Constants.DIAGONAL, Constants.DOT);
        return (Class<T>) Class.forName(className);
    }

    /**
     * 解析 Lambda 表达式为 SerializedLambda 对象
     * <p>
     * 解析结果会被缓存，相同类型的 Lambda 后续调用直接返回缓存结果。
     * </p>
     *
     * @param serializable Lambda 表达式
     * @return SerializedLambda 对象
     * @throws BaseExceptionEnum.RESOLVE_S_FUNCTION_EXCEPTION 解析失败时抛出
     */
    public static SerializedLambda resolve(Serializable serializable) {
        return LAMBDA_MAP.computeIfAbsent(serializable.getClass().getName(), k -> {
            try {
                // Method writeReplace = serializable.getClass().getDeclaredMethod("writeReplace");
                // ReflectionUtils.makeAccessible(writeReplace);
                // return (SerializedLambda) writeReplace.invoke(serializable);
                return (SerializedLambda) MethodUtils.invokeMethod(serializable, true,"writeReplace");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw BaseExceptionEnum.RESOLVE_S_FUNCTION_EXCEPTION.newException(e);
            }
        });
    }
}
