package org.source.utility.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zengfugen
 */
public class Strings {
    private Strings() {
        throw new IllegalStateException("Utility class");
    }

    private static final Pattern PATTERN = Pattern.compile("\\{}");

    /**
     * 格式化字符串
     *
     * @param source        待格式化字符串，以{}为占位符，
     * @param replaceValues 替换值数组
     * @return 格式化后的字符串
     */
    public static String format(String source, Object... replaceValues) {
        Matcher matcher = PATTERN.matcher(source);
        StringBuilder sb = new StringBuilder();
        // 从开始替换占位符
        for (Object v : replaceValues) {
            if (matcher.find() && null != v) {
                // 所有替换值都转为String形式
                matcher.appendReplacement(sb, v.toString());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static Object deserialize(String str, @NotNull Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isAssignableFrom(String.class)) {
                return str;
            } else if (clazz.isAssignableFrom(Integer.class)) {
                return Integer.valueOf(str);
            } else if (clazz.isAssignableFrom(Long.class)) {
                return Long.valueOf(str);
            } else if (clazz.isAssignableFrom(Boolean.class)) {
                return Boolean.valueOf(str);
            } else if (clazz.isAssignableFrom(Float.class)) {
                return Float.valueOf(str);
            } else if (clazz.isAssignableFrom(Double.class)) {
                return Double.valueOf(str);
            } else if (clazz.isAssignableFrom(Character.class)) {
                return str.charAt(0);
            } else if (clazz.isAssignableFrom(Byte.class)) {
                return Byte.valueOf(str);
            } else if (clazz.isAssignableFrom(Short.class)) {
                return Short.valueOf(str);
            } else {
                return Jsons.obj(str, clazz);
            }
        } else {
            return Jsons.obj(str, type);
        }
    }

    public static String removePrefixAndLowerFirst(@NotNull String str, @NotNull String prefix) {
        return String.valueOf(str.charAt(prefix.length())).toLowerCase() + str.substring(prefix.length() + 1);
    }
    /**
     * 驼峰转下划线
     *
     * @param str str
     * @return String
     */
    public static String humpToUnderline(String str) {
        StringBuilder sb = new StringBuilder(str);
        int temp = 0;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isUpperCase(str.charAt(i))) {
                sb.insert(i + temp, "_");
                temp += 1;
            }
        }
        return sb.toString().toLowerCase();
    }
}
