package org.source.utility.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Asserts;
import org.source.utility.utils.Reflects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;

@SuppressWarnings("unchecked")
public interface EnumProcessor<T extends BaseException> {
    /**
     * enum code
     *
     * @return code
     */
    String getCode();

    /**
     * enum message
     *
     * @return msg
     */
    String getMessage();

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    class ExceptionConstructor<T extends BaseException> {
        private Constructor<T> base;
        private Constructor<T> baseAndExtra;
        private Constructor<T> baseAndEx;
        private Constructor<T> baseAndExAndExtra;
    }

    /**
     * 获取异常的构造器
     *
     * @return 构造器
     */
    default ExceptionConstructor<T> exceptionConstructor() {
        return (ExceptionConstructor<T>) ExceptionConstructorCache.CONSTRUCTOR_MAP.computeIfAbsent(this.getClass(), k -> {
            ParameterizedType parameterizedType = Reflects.getParameterizedType(this.getClass(), EnumProcessor.class.getName());
            assert parameterizedType != null;
            Class<? extends BaseException> cls = (Class<? extends BaseException>) parameterizedType.getActualTypeArguments()[0];
            try {
                return ExceptionConstructor.<T>builder()
                        .base((Constructor<T>) cls.getConstructor(EnumProcessor.class))
                        .baseAndExtra((Constructor<T>) cls.getConstructor(EnumProcessor.class, String.class, Object[].class))
                        .baseAndEx((Constructor<T>) cls.getConstructor(EnumProcessor.class, Throwable.class))
                        .baseAndExAndExtra((Constructor<T>) cls.getConstructor(EnumProcessor.class, Throwable.class, String.class, Object[].class))
                        .build();
            } catch (NoSuchMethodException e) {
                throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, e);
            }
        });
    }

    /**
     * except
     *
     * @return BaseException
     */
    default T except() {
        try {
            return this.exceptionConstructor().getBase().newInstance(this);
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
    default T except(String extraMessage, Object... objects) {
        try {
            return this.exceptionConstructor().getBaseAndExtra().newInstance(this, extraMessage, objects);
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
    default T except(Throwable e) {
        try {
            return this.exceptionConstructor().getBaseAndEx().newInstance(this, e);
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
    default T except(Throwable e, String extraMessage, Object... objects) {
        try {
            return this.exceptionConstructor().getBaseAndExAndExtra().newInstance(this, e, extraMessage, objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }

    /**
     * isTrue
     *
     * @param expression expression
     */
    default void isTrue(boolean expression) {
        Asserts.isTrue(expression, this::except);
    }

    default void isFalse(boolean expression) {
        Asserts.isFalse(expression, this::except);
    }

    /**
     * isTrue
     *
     * @param expression   expression
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void isTrue(boolean expression, String extraMessage, Object... objects) {
        Asserts.isTrue(expression, () -> this.except(extraMessage, objects));
    }

    default void isFalse(boolean expression, String extraMessage, Object... objects) {
        Asserts.isFalse(expression, () -> this.except(extraMessage, objects));
    }

    /**
     * nonNull
     *
     * @param obj obj
     */
    default void isNull(Object obj) {
        Asserts.isNull(obj, this::except);
    }

    /**
     * nonNull
     *
     * @param obj          obj
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void isNull(Object obj, String extraMessage, Object... objects) {
        Asserts.isNull(obj, () -> this.except(extraMessage, objects));
    }

    /**
     * nonNull
     *
     * @param obj obj
     */
    default void nonNull(Object obj) {
        Asserts.nonNull(obj, this::except);
    }

    /**
     * nonNull
     *
     * @param obj          obj
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void nonNull(Object obj, String extraMessage, Object... objects) {
        Asserts.nonNull(obj, () -> this.except(extraMessage, objects));
    }

    /**
     * notEmpty
     *
     * @param collection collection
     */
    default void notEmpty(Collection<?> collection) {
        Asserts.notEmpty(collection, this::except);
    }

    default void isEmpty(Collection<?> collection) {
        Asserts.isEmpty(collection, this::except);
    }

    /**
     * notEmpty
     *
     * @param collection   collection
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void notEmpty(Collection<?> collection, String extraMessage, Object... objects) {
        Asserts.notEmpty(collection, () -> this.except(extraMessage, objects));
    }

    default void isEmpty(Collection<?> collection, String extraMessage, Object... objects) {
        Asserts.isEmpty(collection, () -> this.except(extraMessage, objects));
    }

    default void notEmpty(String str) {
        Asserts.notEmpty(str, this::except);
    }

    default void isEmpty(String str) {
        Asserts.isEmpty(str, this::except);
    }

    /**
     * notEmpty
     *
     * @param str          str
     * @param extraMessage extraMessage
     * @param objects      objects
     */
    default void notEmpty(String str, String extraMessage, Object... objects) {
        Asserts.notEmpty(str, () -> this.except(extraMessage, objects));
    }

    default void isEmpty(String str, String extraMessage, Object... objects) {
        Asserts.isEmpty(str, () -> this.except(extraMessage, objects));
    }


    /**
     * notEmpty
     *
     * @param objects objects
     */
    default void notEmpty(Object[] objects) {
        Asserts.notEmpty(objects, this::except);
    }

    default void isEmpty(Object[] objects) {
        Asserts.isEmpty(objects, this::except);
    }

    default void notEmpty(Object[] object, String extraMessage, Object... objects) {
        Asserts.notEmpty(object, () -> this.except(extraMessage, objects));
    }

    default void isEmpty(Object[] object, String extraMessage, Object... objects) {
        Asserts.isEmpty(object, () -> this.except(extraMessage, objects));
    }

}
