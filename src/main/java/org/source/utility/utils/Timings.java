package org.source.utility.utils;

import java.time.Duration;

/**
 * 时间统计工具类，支持纳秒精度和自动单位切换
 */
public final class Timings {

    private Timings() {
    }

    /**
     * 创建一个新的计时器
     */
    public static Timing start() {
        return new Timing();
    }

    /**
     * 格式化Duration，自动选择合适的单位
     * 规则：
     * - 小于1微秒：显示纳秒 "xxx ns"
     * - 小于1毫秒：显示微秒 "xxx μs"
     * - 小于1秒：显示毫秒 "xxx ms"
     * - 小于1分钟：显示秒 "xxx.xxx s"
     * - 大于1分钟：显示分钟和秒 "xxx min xxx.xxx s"
     */
    public static String formatDuration(Duration duration) {
        if (duration.isZero()) {
            return "0 ns";
        }
        long nanos = duration.toNanos();

        if (nanos < 1000) {
            // 小于1微秒，显示纳秒
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            // 小于1毫秒，显示微秒
            double micros = nanos / 1000.0;
            return String.format("%.2f μs", micros);
        } else if (nanos < 1_000_000_000) {
            // 小于1秒，显示毫秒
            double millis = nanos / 1_000_000.0;
            return String.format("%.2f ms", millis);
        } else if (nanos < 60_000_000_000L) {
            // 小于1分钟，显示秒
            double seconds = nanos / 1_000_000_000.0;
            return String.format("%.3f s", seconds);
        } else {
            // 大于1分钟，显示分钟和秒
            long minutes = duration.toMinutes();
            double remainingSeconds = duration.minusMinutes(minutes).toNanos() / 1_000_000_000.0;
            return String.format("%d min %.3f s", minutes, remainingSeconds);
        }
    }
}