package org.source.utility.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * <p>
 * 提供日期时间与字符串之间的转换功能。
 * </p>
 *
 * @author zengfugen
 */
public class Dates {
    private Dates() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 默认日期时间格式
     */
    public static final String LOCAL_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 将字符串解析为 LocalDateTime
     *
     * @param str     日期时间字符串
     * @param pattern 日期时间格式
     * @return LocalDateTime 对象
     */
    public static LocalDateTime strToLocalDateTime(String str, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(str, formatter);
    }

    /**
     * 将字符串解析为 LocalDateTime（使用默认格式）
     *
     * @param str 日期时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"
     * @return LocalDateTime 对象
     */
    public static LocalDateTime strToLocalDateTime(String str) {
        return strToLocalDateTime(str, LOCAL_DATE_TIME);
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳
     *
     * @param time LocalDateTime 对象
     * @return 毫秒时间戳
     */
    public static long localDateTimeToMilli(LocalDateTime time) {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(time, ZoneId.systemDefault());
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
