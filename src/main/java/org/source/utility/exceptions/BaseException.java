package org.source.utility.exceptions;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.source.utility.utils.Strings;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author zengfugen
 */
@JsonIgnoreProperties({"stackTrace", "suppressed", "localizeMessage"})
@EqualsAndHashCode(callSuper = false)
@Data
public class BaseException extends RuntimeException {
    @NonNull
    private final String code;
    @NonNull
    private final String message;
    @JsonIgnore
    @Nullable
    private final Throwable cause;
    @Nullable
    private final String extraMessage;

    @JsonCreator
    public BaseException(@JsonProperty(value = "code") @NotNull String code,
                         @JsonProperty(value = "message") @NotNull String message,
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
                StringUtils.hasText(extraMessage) ? Strings.format(extraMessage, objects) : null);
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
        if (StringUtils.hasText(this.extraMessage)) {
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

    public static BaseException except(Exception e, Supplier<BaseException> supplier) {
        if (e instanceof BaseException baseException) {
            BaseException initialBaseException = baseException.initialBaseException();
            if (Objects.nonNull(initialBaseException)) {
                return initialBaseException;
            }
        }
        return supplier.get();
    }
}
