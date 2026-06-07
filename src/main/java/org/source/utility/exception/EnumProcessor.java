package org.source.utility.exception;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Asserts;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface EnumProcessor<E extends BaseException> {

    Map<Class<? extends EnumProcessor<?>>, ExceptionConstructor<?>> CONSTRUCTOR_MAP = HashMap.newHashMap(16);

    /**
     * 枚举的 name() 方法
     *
     * @return 枚举的名称
     */
    String name();

    /**
     * enum code
     *
     * @return code
     */
    default String getCode() {
        return name();
    }

    /**
     * enum message
     *
     * @return msg
     */
    String getMessage();

    default E newException() {
        return newException(this);
    }

    default E newException(String extraMessage, Object... objects) {
        return newException(this, extraMessage, objects);
    }

    default E newException(Throwable e) {
        return newException(this, e);
    }

    default E newException(Throwable e, String extraMessage, Object... objects) {
        return newException(this, e, extraMessage, objects);
    }

    default void throwException() {
        throw newException();
    }

    default void throwException(String extraMessage, Object... objects) {
        throw newException(extraMessage, objects);
    }

    default void throwException(Throwable e) {
        throw newException(e);
    }

    default void throwException(Throwable e, String extraMessage, Object... objects) {
        throw newException(e, extraMessage, objects);
    }

    /**
     * isTrue
     *
     * @param expression expression
     */
    default void isTrue(boolean expression) {
        Asserts.isTrue(expression, this::newException);
    }

    default void isFalse(boolean expression) {
        Asserts.isFalse(expression, this::newException);
    }

    /**
     * isTrue
     *
     * @param expression   expression
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void isTrue(boolean expression, String extraMessage, Object... objects) {
        Asserts.isTrue(expression, () -> this.newException(extraMessage, objects));
    }

    default void isFalse(boolean expression, String extraMessage, Object... objects) {
        Asserts.isFalse(expression, () -> this.newException(extraMessage, objects));
    }

    /**
     * nonNull
     *
     * @param obj obj
     */
    default void isNull(Object obj) {
        Asserts.isNull(obj, this::newException);
    }

    /**
     * nonNull
     *
     * @param obj          obj
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void isNull(Object obj, String extraMessage, Object... objects) {
        Asserts.isNull(obj, () -> this.newException(extraMessage, objects));
    }

    /**
     * nonNull
     *
     * @param obj obj
     */
    default void nonNull(Object obj) {
        Asserts.nonNull(obj, this::newException);
    }

    /**
     * nonNull
     *
     * @param obj          obj
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void nonNull(Object obj, String extraMessage, Object... objects) {
        Asserts.nonNull(obj, () -> this.newException(extraMessage, objects));
    }

    /**
     * notEmpty
     *
     * @param collection collection
     */
    default void notEmpty(Collection<?> collection) {
        Asserts.notEmpty(collection, this::newException);
    }

    default void isEmpty(Collection<?> collection) {
        Asserts.isEmpty(collection, this::newException);
    }

    /**
     * notEmpty
     *
     * @param collection   collection
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void notEmpty(Collection<?> collection, String extraMessage, Object... objects) {
        Asserts.notEmpty(collection, () -> this.newException(extraMessage, objects));
    }

    default void isEmpty(Collection<?> collection, String extraMessage, Object... objects) {
        Asserts.isEmpty(collection, () -> this.newException(extraMessage, objects));
    }

    default void notEmpty(String str) {
        Asserts.notEmpty(str, this::newException);
    }

    default void isEmpty(String str) {
        Asserts.isEmpty(str, this::newException);
    }

    /**
     * notEmpty
     *
     * @param str          str
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void notEmpty(String str, String extraMessage, Object... objects) {
        Asserts.notEmpty(str, () -> this.newException(extraMessage, objects));
    }

    default void isEmpty(String str, String extraMessage, Object... objects) {
        Asserts.isEmpty(str, () -> this.newException(extraMessage, objects));
    }


    /**
     * notEmpty
     *
     * @param objects objects
     */
    default void notEmpty(Object[] objects) {
        Asserts.notEmpty(objects, this::newException);
    }

    default void isEmpty(Object[] objects) {
        Asserts.isEmpty(objects, this::newException);
    }

    default void notEmpty(Object[] object, String extraMessage, Object... objects) {
        Asserts.notEmpty(object, () -> this.newException(extraMessage, objects));
    }

    default void isEmpty(Object[] object, String extraMessage, Object... objects) {
        Asserts.isEmpty(object, () -> this.newException(extraMessage, objects));
    }


    /**
     * 获取异常的构造器
     *
     * @return 构造器
     */
    @SuppressWarnings("unchecked")
    private static <E1 extends BaseException, E2 extends EnumProcessor<E1>> ExceptionConstructor<E1>
    exceptionConstructor(Class<E2> exceptionClass) {
        return (ExceptionConstructor<E1>) CONSTRUCTOR_MAP.computeIfAbsent(exceptionClass, k -> {
            TypeFactory typeFactory = TypeFactory.defaultInstance();
            JavaType type = typeFactory.constructType(exceptionClass);
            JavaType superType = type.findSuperType(EnumProcessor.class);
            JavaType javaType = superType.containedType(0);
            Class<? extends BaseException> cls = (Class<? extends BaseException>) javaType.getRawClass();
            BaseExceptionEnum.ENUM_PROCESSOR_GENERIC_MUST_INSTANCEOF_BASE_EXCEPTION.nonNull(cls);
            try {
                return ExceptionConstructor.<E1>builder()
                        .base((Constructor<E1>) cls.getConstructor(EnumProcessor.class))
                        .baseAndExtra((Constructor<E1>) cls.getConstructor(EnumProcessor.class, String.class, Object[].class))
                        .baseAndEx((Constructor<E1>) cls.getConstructor(EnumProcessor.class, Throwable.class))
                        .baseAndExAndExtra((Constructor<E1>) cls.getConstructor(EnumProcessor.class, Throwable.class, String.class, Object[].class))
                        .build();
            } catch (NoSuchMethodException e) {
                throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, e);
            }
        });
    }

    /**
     * except
     *
     * @return BaseException的实现类，具体的业务异常类型
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(E2 e2) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBase().newInstance(e2);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }

    /**
     * new BaseException
     *
     * @param extraMessage 额外消息
     * @param objects      消息中占位符的具体值
     * @return BaseException
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(
            E2 e2, String extraMessage, Object... objects) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBaseAndExtra().newInstance(e2, extraMessage, objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, e);
        }
    }

    /**
     * new BaseException
     *
     * @param e e
     * @return BaseException
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(
            E2 e2, Throwable e) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBaseAndEx().newInstance(e2, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }

    /**
     * new BaseException
     *
     * @param e            e
     * @param extraMessage 额外消息
     * @param objects      消息中占位符的具体值
     * @return BaseException
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(
            E2 e2, Throwable e, String extraMessage, Object... objects) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBaseAndExAndExtra().newInstance(e2, e, extraMessage, objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }
}
