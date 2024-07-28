package org.source.utility.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Dates {
    private Dates() {
        throw new IllegalStateException("Utility class");
    }

    public static final String LOCAL_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    public static LocalDateTime strToLocalDateTime(String str, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(str, formatter);
    }

    public static LocalDateTime strToLocalDateTime(String str) {
        return strToLocalDateTime(str, LOCAL_DATE_TIME);
    }

    public static long localDateTimeToMilli(LocalDateTime time) {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(time, ZoneId.systemDefault());
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
