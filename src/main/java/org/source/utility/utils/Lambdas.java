package org.source.utility.utils;

import lombok.experimental.UtilityClass;
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

    public static String getFieldName(Serializable serializable) {
        return getFieldName(getMethodName(serializable));
    }

    public static String getMethodName(Serializable serializable) {
        return resolve(serializable).getImplMethodName();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(Serializable serializable) {
        String className = resolve(serializable).getImplClass();
        className = className.replace(Constants.DIAGONAL, Constants.DOT);
        return (Class<T>) Reflects.classForName(className);
    }

    public static SerializedLambda resolve(Serializable serializable) {
        return LAMBDA_MAP.computeIfAbsent(serializable.getClass().getName(), k -> {
            try {
                Method writeReplace = serializable.getClass().getDeclaredMethod("writeReplace");
                ReflectionUtils.makeAccessible(writeReplace);
                return (SerializedLambda) writeReplace.invoke(serializable);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw BaseExceptionEnum.RESOLVE_S_FUNCTION_EXCEPTION.except();
            }
        });
    }

    public static String getFieldName(String methodName) {
        if (null == methodName || methodName.isEmpty()) {
            return methodName;
        } else if (methodName.startsWith(Constants.GET)) {
            return Strings.removePrefixAndLowerFirst(methodName, Constants.GET);
        } else if (methodName.startsWith(Constants.SET)) {
            return Strings.removePrefixAndLowerFirst(methodName, Constants.SET);
        } else if (methodName.startsWith(Constants.IS)) {
            return Strings.removePrefixAndLowerFirst(methodName, Constants.IS);
        } else {
            return methodName;
        }
    }


}
