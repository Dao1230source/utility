package org.source.utility.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.aop.support.AopUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Slf4j
public class Reflects {
    private Reflects() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取对象实现的指定名称的接口具体泛型
     *
     * @param o    对象
     * @param name 泛型接口名称
     * @return 实现类的泛型组
     */
    public static Type[] getInterfaceGenerics(Object o, String name) {
        ParameterizedType parameterizedType = getGenericInterface(o, name);
        if (null == parameterizedType) {
            return new Type[0];
        }
        return getGenericsOfInterface(parameterizedType);
    }

    /**
     * 类可能实现多个接口，这里只获取指定名称的泛型接口
     *
     * @param o    对象实例
     * @param name 接口名称
     * @return 指定名称的泛型接口
     */
    public static ParameterizedType getGenericInterface(Object o, String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        Class<?> clazz;
        // 如果是代理类，取父类
        if (AopUtils.isAopProxy(o)) {
            clazz = o.getClass().getSuperclass();
        } else {
            clazz = o.getClass();
        }
        return getParameterizedType(clazz, name);
    }

    /**
     * 获取Type的具体类型信息，包括其泛型
     * 返回参数化类型
     *
     * @param type type
     * @param name 接口名称
     * @param <T>  t
     * @return 参数化类型
     */
    public static <T extends Type> ParameterizedType getParameterizedType(T type, String name) {
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType().getTypeName().equals(name)) {
                return parameterizedType;
            }
        } else if (type instanceof Class) {
            Type[] types = ((Class<?>) type).getGenericInterfaces();
            for (Type superType : types) {
                ParameterizedType superParameterizedType = getParameterizedType(superType, name);
                if (null != superParameterizedType) {
                    return superParameterizedType;
                }
            }
        }
        return null;
    }

    /**
     * @param parameterizedType 泛型类型
     * @return 获取接口实现类的泛型信息
     */
    public static Type[] getGenericsOfInterface(ParameterizedType parameterizedType) {
        return parameterizedType.getActualTypeArguments();
    }

    /**
     * 将对象中的BigDecimal设置精度
     *
     * @param e   e
     * @param <E> E
     */
    public static <E> void setBigDecimalScale(E e, int scale) throws IntrospectionException,
            InvocationTargetException, IllegalAccessException {
        Field[] fields = e.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(BigDecimal.class)) {
                BigDecimal f = (BigDecimal) getFieldValueThrow(e, field);
                if (null != f) {
                    f = f.setScale(scale, RoundingMode.HALF_UP);
                    setFieldValueThrow(e, field, f);
                }
            }
        }
    }

    public static <E, T> Field[] getFieldsFromObject(E e, BiPredicate<Field, T> predicate, T t) {
        Field[] fields = e.getClass().getDeclaredFields();
        return Arrays.stream(fields).filter(f -> predicate.test(f, t)).toArray(Field[]::new);
    }

    public static <E> Field getFieldByName(E e, String fieldName) {
        try {
            return getFieldByNameThrow(e, fieldName);
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }

    public static <E> Field getFieldByNameThrow(E e, String fieldName) throws NoSuchFieldException {
        if (e instanceof Class<?> cls) {
            return cls.getDeclaredField(fieldName);
        }
        return e.getClass().getDeclaredField(fieldName);
    }

    public static <E> void setFieldValueThrow(E e, Field field, Object value)
            throws IllegalAccessException, IntrospectionException, InvocationTargetException {
        PropertyDescriptor descriptor = getDescriptor(e, field);
        Method writeMethod = descriptor.getWriteMethod();
        writeMethod.invoke(e, value);
    }

    public static <E> void setFieldValueThrow(E e, String fieldName, Object value)
            throws IllegalAccessException, IntrospectionException, InvocationTargetException {
        Field field = getFieldByName(e, fieldName);
        if (Objects.isNull(field)) {
            return;
        }
        setFieldValueThrow(e, field, value);
    }

    public static <T> void setFieldValue(T t, Field field, Object value) {
        try {
            setFieldValueThrow(t, field, value);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw BaseExceptionEnum.REFLECT_EXCEPTION.except(
                    Strings.format("setFieldValue({},{},{})", t.getClass().getName(), field.getName(), value), e);
        }
    }

    public static <T> void setFieldValue(T t, String fieldName, Object value) {
        try {
            setFieldValueThrow(t, fieldName, value);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw BaseExceptionEnum.REFLECT_EXCEPTION.except(
                    Strings.format("setFieldValue({},{},{})", t.getClass().getName(), fieldName, value), e);
        }
    }

    public static <E> Object getFieldValueThrow(E e, Field field) throws InvocationTargetException,
            IllegalAccessException, IntrospectionException {
        PropertyDescriptor descriptor = getDescriptor(e, field);
        Method readMethod = descriptor.getReadMethod();
        return readMethod.invoke(e);
    }

    public static <E> Object getFieldValueThrow(E e, String fieldName) throws InvocationTargetException,
            IllegalAccessException, IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(e.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        Optional<Method> first = Arrays.stream(propertyDescriptors).filter(p -> p.getDisplayName().equals(fieldName))
                .map(PropertyDescriptor::getReadMethod).findFirst();
        if (first.isPresent()) {
            return first.get().invoke(e);
        }
        return null;
    }


    public static <T> Object getFieldValue(T t, Field field) {
        try {
            return getFieldValueThrow(t, field);
        } catch (InvocationTargetException | IllegalAccessException |
                 IntrospectionException e) {
            throw BaseExceptionEnum.REFLECT_EXCEPTION.except(
                    Strings.format("getFieldValue({},{})", t.getClass().getName(), field.getName()), e);
        }
    }

    public static <T> Object getFieldValue(T t, String fieldName) {
        try {
            return getFieldValueThrow(t, fieldName);
        } catch (InvocationTargetException | IllegalAccessException |
                 IntrospectionException e) {
            throw BaseExceptionEnum.REFLECT_EXCEPTION.except(
                    Strings.format("getFieldValue({},{})", t.getClass().getName(), fieldName), e);
        }
    }

    public static <T> Object[] getFieldValues(@NotNull T t, @NotNull Field[] fields) {
        return Streams.of(fields).map(f -> getFieldValue(t, f)).toArray(Object[]::new);
    }

    public static <T> Map<String, Object> getFieldValueAsMap(@NotNull T t, @NotNull Field[] fields) {
        Map<String, Object> map = HashMap.newHashMap(fields.length);
        for (Field field : fields) {
            map.put(field.getName(), getFieldValue(t, field));
        }
        return map;
    }

    public static Field[] getFields(Class<?> clazz, Class<? extends Annotation> annotationType) {
        Field[] fields = clazz.getDeclaredFields();
        return Arrays.stream(fields).filter(k -> Objects.nonNull(k.getAnnotation(annotationType))).toArray(Field[]::new);
    }

    public static Field getField(Class<?> clazz, Class<? extends Annotation> annotationType) {
        Field[] fields = getFields(clazz, annotationType);
        if (fields.length > 0) {
            return fields[0];
        }
        return null;
    }

    public static PropertyDescriptor getDescriptor(Object obj, Field field) throws IntrospectionException {
        return new PropertyDescriptor(field.getName(), obj.getClass());
    }

    public static PropertyDescriptor getDescriptor(Object obj, String fieldName) throws IntrospectionException {
        return new PropertyDescriptor(fieldName, obj.getClass());
    }

    public static Class<?> classForName(String className) {
        try {
            return classForNameThrow(className);
        } catch (ClassNotFoundException e) {
            throw BaseExceptionEnum.REFLECT_EXCEPTION.except(Strings.format("no class for name:{}", className), e);
        }
    }

    public static Class<?> classForNameThrow(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    public static Class<?> classForNameOrDefault(String className, Class<?> defaultClass) {
        try {
            return classForNameThrow(className);
        } catch (Exception ignore) {
            return defaultClass;
        }
    }

    public static <A extends Annotation> Field[] getFieldsByAnnotation(Class<?> clazz, Class<A> annotationClass) {
        Field[] declaredFields = clazz.getDeclaredFields();
        return Streams.of(declaredFields).filter(f -> Objects.nonNull(f.getAnnotation(annotationClass))).toArray(Field[]::new);
    }

    public static <A extends Annotation> Field[] getFieldsByAnnotation(Class<?> clazz, Class<A> annotationClass,
                                                                       Predicate<A> predicate) {
        Field[] declaredFields = clazz.getDeclaredFields();
        return Streams.of(declaredFields).filter(f -> {
            A a = f.getAnnotation(annotationClass);
            return Objects.nonNull(a) && predicate.test(a);
        }).toArray(Field[]::new);
    }

    public static <T> T newInstance(Class<T> tClass) {
        try {
            return tClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw BaseExceptionEnum.REFLECT_EXCEPTION.except(Strings.format("newInstance:{}", tClass.getName()), e);
        }
    }

}
