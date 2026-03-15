package org.source.utility.exception;

import org.source.utility.utils.Asserts;

import java.util.Collection;

public interface EnumProcessor<E extends BaseException> {
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
        return BaseException.newException(this);
    }

    default E newException(String extraMessage, Object... objects) {
        return BaseException.newException(this, extraMessage, objects);
    }

    default E newException(Throwable e) {
        return BaseException.newException(this, e);
    }

    default E newException(Throwable e, String extraMessage, Object... objects) {
        return BaseException.newException(this, e, extraMessage, objects);
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

}
