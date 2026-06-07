package org.source.utility.exception;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.source.utility.utils.Strings;

import java.util.Objects;

/**
 * @author zengfugen
 */
@JsonIgnoreProperties({"stackTrace", "suppressed", "localizeMessage"})
@EqualsAndHashCode(callSuper = false)
@Data
public class BaseException extends RuntimeException {

    private final String code;
    private final String message;
    @JsonIgnore
    @Nullable
    private final Throwable cause;
    @Nullable
    private final String extraMessage;

    @JsonCreator
    public BaseException(@JsonProperty(value = "code") String code,
                         @JsonProperty(value = "message") String message,
                         @JsonProperty(value = "cause") @Nullable Throwable cause,
                         @JsonProperty(value = "extraMessage") @Nullable String extraMessage) {
        this.code = code;
        this.message = message;
        this.cause = cause;
        this.extraMessage = extraMessage;
    }

    public BaseException(EnumProcessor<?> content) {
        this(content.getCode(), content.getMessage(), null, null);
    }

    public BaseException(EnumProcessor<?> content, String extraMessage, Object... objects) {
        this(content.getCode(), content.getMessage(), null, Strings.format(extraMessage, objects));
    }

    public BaseException(EnumProcessor<?> content, Throwable e) {
        this(content, e, e.getMessage());
    }

    public BaseException(EnumProcessor<?> content, Throwable e, String extraMessage, Object... objects) {
        this(content.getCode(), content.getMessage(), e,
                StringUtils.isNotBlank(extraMessage) ? Strings.format(extraMessage, objects) : null);
    }

    @JsonGetter("cause")
    public String causeToString() {
        if (null == cause) {
            return null;
        }
        if (cause instanceof BaseException) {
            return cause.toString();
        } else {
            return cause.getMessage();
        }
    }

    public String toPlainString() {
        if (StringUtils.isNotBlank(this.extraMessage)) {
            return this.code + "(" + this.message + ")" + ":" + this.extraMessage;
        }
        return this.code + "(" + this.message + ")";
    }

    public BaseException initialBaseException() {
        BaseException baseException = this;
        while (Objects.nonNull(baseException.getCause())
                && baseException.getCause() instanceof BaseException causeBaseException) {
            baseException = causeBaseException;
        }
        return baseException;
    }
}
