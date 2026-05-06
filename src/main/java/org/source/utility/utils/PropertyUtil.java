package org.source.utility.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.source.utility.constant.Constants;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 属性名称工具类，支持将类的 private 属性名称自动转为 getter/setter 方法名
 *
 * @author zengfugen
 */
@UtilityClass
public class PropertyUtil {

    /**
     * 将属性名转换为 getter 方法名
     * <p>
     * 转换规则遵循 JavaBean 规范：
     * <ul>
     *   <li>name -> getName</li>
     *   <li>userName -> getUserName</li>
     *   <li>URL -> getURL (连续大写字母保持不变)</li>
     *   <li>aName -> getAName (第二个字母大写时，首字母不大写)</li>
     * </ul>
     *
     * @param fieldName 属性名
     * @return getter 方法名
     */
    public static String toGetter(String fieldName) {
        return Constants.GET + capitalize(fieldName);
    }

    /**
     * 将属性名转换为 getter 方法名，根据类型自动选择 get/is 前缀
     * <p>
     * 转换规则遵循 JavaBeans 规范和 Lombok 实现：
     * <ul>
     *   <li>boolean 基本类型使用 is 前缀：active -> isActive</li>
     *   <li>Boolean 包装类型使用 get 前缀：active -> getActive</li>
     *   <li>其他类型使用 get 前缀：name -> getName</li>
     *   <li>连续大写字母保持不变：URL -> getURL/isURL</li>
     * </ul>
     *
     * @param fieldName 属性名
     * @param type      属性类型
     * @return getter 方法名
     */
    public static String toGetter(String fieldName, Class<?> type) {
        if (isBooleanPrimitiveType(type)) {
            // 如果属性名已经以 is 开头，直接返回
            if (StringUtils.isNotBlank(fieldName) && fieldName.startsWith(Constants.IS) && fieldName.length() > Constants.IS.length()) {
                return fieldName;
            }
            return Constants.IS + capitalize(fieldName);
        }
        return toGetter(fieldName);
    }

    /**
     * 将属性名转换为 setter 方法名
     * <p>
     * 转换规则遵循 JavaBean 规范：
     * <ul>
     *   <li>name -> setName</li>
     *   <li>userName -> setUserName</li>
     *   <li>URL -> setURL (连续大写字母保持不变)</li>
     *   <li>aName -> setAName (第二个字母大写时，首字母不大写)</li>
     * </ul>
     *
     * @param fieldName 属性名
     * @return setter 方法名
     */
    public static String toSetter(String fieldName) {
        return Constants.SET + capitalize(fieldName);
    }

    /**
     * 将属性名转换为 setter 方法名，根据类型自动处理 boolean 基本类型属性名
     * <p>
     * 对于 boolean 基本类型，如果属性名以 is 开头，setter 方法名会去掉 is 前缀：
     * <ul>
     *   <li>isActive -> setActive</li>
     *   <li>active -> setActive</li>
     *   <li>name -> setName</li>
     * </ul>
     * 注意：Boolean 包装类型不使用 is 前缀，所以不做特殊处理
     *
     * @param fieldName 属性名
     * @param type      属性类型
     * @return setter 方法名
     */
    public static String toSetter(String fieldName, Class<?> type) {
        if (isBooleanPrimitiveType(type)
                && StringUtils.isNotBlank(fieldName)
                && fieldName.startsWith("is")
                && fieldName.length() > 2) {
            return Constants.SET + capitalize(fieldName.substring(2));
        }
        return toSetter(fieldName);
    }

    /**
     * 判断类型是否为 boolean 基本类型
     * <p>
     * 根据 JavaBeans 规范和 Lombok 实现，只有 boolean 基本类型才使用 is 前缀的 getter，
     * Boolean 包装类型使用 get 前缀
     *
     * @param type 类型
     * @return 是否为 boolean 基本类型
     */
    private static boolean isBooleanPrimitiveType(@Nullable Class<?> type) {
        return type == boolean.class;
    }

    /**
     * 将 getter/setter 方法名转换为属性名
     * <p>
     * 转换规则：
     * <ul>
     *   <li>getName -> name</li>
     *   <li>setUserName -> userName</li>
     *   <li>getURL -> URL (连续大写字母属性)</li>
     *   <li>isActive -> active (boolean 的 is 前缀)</li>
     *   <li>isURL -> URL</li>
     * </ul>
     *
     * @param methodName getter/setter/is 方法名
     * @return 属性名，如果无法解析则返回 null
     */
    @Nullable
    public static String toFieldName(String methodName) {
        if (StringUtils.isBlank(methodName)) {
            return null;
        }
        if (methodName.startsWith(Constants.GET) && methodName.length() > Constants.GET.length()) {
            return decapitalize(methodName.substring(Constants.GET.length()));
        }
        if (methodName.startsWith(Constants.SET) && methodName.length() > Constants.SET.length()) {
            return decapitalize(methodName.substring(Constants.SET.length()));
        }
        if (methodName.startsWith(Constants.IS) && methodName.length() > Constants.IS.length()) {
            return decapitalize(methodName.substring(Constants.IS.length()));
        }
        return null;
    }

    /**
     * 将属性名首字母大写
     * <p>
     * 遵循 JavaBean 规范：
     * 如果第二个字母是大写，则首字母不大写（如 URL -> URL，aName -> AName）
     *
     * @param fieldName 属性名
     * @return 首字母大写后的名称
     */
    public static String capitalize(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return fieldName;
        }
        // 如果第二个字母是大写，首字母不大写
        if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1))) {
            return fieldName;
        }
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * 将方法名中的属性部分首字母小写
     * <p>
     * 遵循 JavaBean 规范：
     * 如果第二个字母是大写，则首字母不小写（如 URL -> URL，而不是 uRL）
     *
     * @param name 方法名中的属性部分
     * @return 首字母小写后的属性名
     */
    public static String decapitalize(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        // 如果第二个字母是大写，首字母不小写
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * 判断方法名是否为 getter 方法
     *
     * @param methodName 方法名
     * @return 是否为 getter 方法
     */
    public static boolean isGetter(String methodName) {
        if (StringUtils.isBlank(methodName)) {
            return false;
        }
        return (methodName.startsWith(Constants.GET) && methodName.length() > Constants.GET.length())
                || (methodName.startsWith(Constants.IS) && methodName.length() > Constants.IS.length());
    }

    /**
     * 判断方法名是否为 setter 方法
     *
     * @param methodName 方法名
     * @return 是否为 setter 方法
     */
    public static boolean isSetter(String methodName) {
        if (StringUtils.isBlank(methodName)) {
            return false;
        }
        return methodName.startsWith(Constants.SET) && methodName.length() > Constants.SET.length();
    }

    /**
     * 将 Field 转换为 getter 方法名，根据字段类型自动选择 get/is 前缀
     * <p>
     * boolean 基本类型使用 is 前缀，Boolean 包装类型及其他类型使用 get 前缀
     *
     * @param field 字段
     * @return getter 方法名
     */
    public static String toGetter(Field field) {
        return toGetter(field.getName(), field.getType());
    }

    /**
     * 将 Field 转换为 setter 方法名，根据字段类型自动处理 boolean 基本类型属性名
     * <p>
     * boolean 基本类型如果属性名以 is 开头，setter 会去掉 is 前缀
     *
     * @param field 字段
     * @return setter 方法名
     */
    public static String toSetter(Field field) {
        return toSetter(field.getName(), field.getType());
    }

    /**
     * 通过 getter 方法获取属性值
     * <p>
     * 使用 PropertyUtil 获取 getter 方法名，通过反射调用
     *
     * @param target    目标对象
     * @param fieldName 属性名
     * @return 属性值
     */
    public static @Nullable Object getProperty(Object target, String fieldName) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        return getProperty(target, field);
    }

    public static @Nullable Object getProperty(Object target, @Nullable Field field) {
        if (Objects.isNull(field)) {
            return null;
        }
        String getterName = PropertyUtil.toGetter(field);
        Method getter = ReflectionUtils.findMethod(target.getClass(), getterName);
        if (Objects.isNull(getter)) {
            return null;
        }
        return ReflectionUtils.invokeMethod(getter, target);
    }

    /**
     * 通过 setter 方法设置属性值
     * <p>
     * 使用 PropertyUtil 获取 setter 方法名，通过反射调用
     *
     * @param target    目标对象
     * @param fieldName 属性名
     * @param value     属性值
     */
    public static void setProperty(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        setProperty(target, field, value);
    }

    /**
     * 通过 setter 方法设置属性值
     * <p>
     * 使用 PropertyUtil 获取 setter 方法名，通过反射调用
     *
     * @param target 目标对象
     * @param field  属性
     * @param value  属性值
     */
    public static void setProperty(Object target, @Nullable Field field, @Nullable Object value) {
        if (Objects.isNull(field)) {
            return;
        }
        String setterName = PropertyUtil.toSetter(field);
        Method setter = ReflectionUtils.findMethod(target.getClass(), setterName, field.getType());
        if (Objects.isNull(setter)) {
            return;
        }
        ReflectionUtils.invokeMethod(setter, target, value);
    }
}