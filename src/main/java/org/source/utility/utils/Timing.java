package org.source.utility.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;
import org.source.utility.constant.Constants;

import java.time.Duration;
import java.time.Instant;

/**
 * 记录时间统计信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"startTime", "endTime", "duration"})
public class Timing {
    private final Instant startTime;
    private @Nullable Instant endTime;
    private @Nullable Duration duration;

    Timing() {
        this.startTime = Instant.now();
    }

    /**
     * 结束计时
     */
    public void end() {
        this.endTime = Instant.now();
        this.duration = Duration.between(startTime, endTime);
    }

    @JsonIgnore
    public Instant getStartTime() {
        return startTime;
    }

    @JsonProperty("startTime")
    public String getStartTimeStr() {
        return startTime.toString();
    }

    @JsonIgnore
    public @Nullable Instant getEndTime() {
        return endTime;
    }

    @JsonProperty("endTime")
    public String getEndTimeStr() {
        return endTime == null ? Constants.EMPTY : endTime.toString();
    }

    @JsonIgnore
    public @Nullable Duration getDuration() {
        return duration;
    }

    @JsonProperty("duration")
    public String getFormattedDuration() {
        if (duration == null) {
            return "0 ns";
        }
        return Timings.formatDuration(duration);
    }
}
