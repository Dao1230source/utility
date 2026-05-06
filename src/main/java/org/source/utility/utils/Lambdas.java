package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class Lambdas {

    private static final Map<String, SerializedLambda> LAMBDA_MAP = new ConcurrentHashMap<>(32);

    public static @Nullable String getFieldName(Serializable serializable) {
        return PropertyUtil.toFieldName(getMethodName(serializable));
    }

    public static String getMethodName(Serializable serializable) {
        return resolve(serializable).getImplMethodName();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(Serializable serializable) throws ClassNotFoundException {
        String className = resolve(serializable).getImplClass();
        className = className.replace(Constants.DIAGONAL, Constants.DOT);
        return (Class<T>) Class.forName(className);
    }

    public static SerializedLambda resolve(Serializable serializable) {
        return LAMBDA_MAP.computeIfAbsent(serializable.getClass().getName(), k -> {
            try {
                Method writeReplace = serializable.getClass().getDeclaredMethod("writeReplace");
                ReflectionUtils.makeAccessible(writeReplace);
                return (SerializedLambda) writeReplace.invoke(serializable);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw BaseExceptionEnum.RESOLVE_S_FUNCTION_EXCEPTION.newException(e);
            }
        });
    }
}
