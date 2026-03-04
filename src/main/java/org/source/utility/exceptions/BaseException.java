package org.source.utility.exceptions;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Strings;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zengfugen
 */
@JsonIgnoreProperties({"stackTrace", "suppressed", "localizeMessage"})
@EqualsAndHashCode(callSuper = false)
@Data
public class BaseException extends RuntimeException {
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends EnumProcessor>, ExceptionConstructor<?>> CONSTRUCTOR_MAP = HashMap.newHashMap(16);

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

    /**
     * 获取异常的构造器
     *
     * @return 构造器
     */
    @SuppressWarnings("unchecked")
    private static <E1 extends BaseException, E2 extends EnumProcessor<E1>> ExceptionConstructor<E1>
    exceptionConstructor(Class<E2> exceptionClass) {
        return (ExceptionConstructor<E1>) CONSTRUCTOR_MAP.computeIfAbsent(exceptionClass, k -> {
            ParameterizedType parameterizedType = Reflects.getParameterizedType(exceptionClass, EnumProcessor.class.getName());
            assert parameterizedType != null;
            Class<? extends BaseException> cls = (Class<? extends BaseException>) parameterizedType.getActualTypeArguments()[0];
            try {
                return ExceptionConstructor.<E1>builder()
                        .base((Constructor<E1>) cls.getConstructor(EnumProcessor.class))
                        .baseAndExtra((Constructor<E1>) cls.getConstructor(EnumProcessor.class, String.class, Object[].class))
                        .baseAndEx((Constructor<E1>) cls.getConstructor(EnumProcessor.class, Throwable.class))
                        .baseAndExAndExtra((Constructor<E1>) cls.getConstructor(EnumProcessor.class, Throwable.class, String.class, Object[].class))
                        .build();
            } catch (NoSuchMethodException e) {
                throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, e);
            }
        });
    }

    /**
     * except
     *
     * @return BaseException的实现类，具体的业务异常类型
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(E2 e2) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBase().newInstance(e2);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }

    /**
     * new BaseException
     *
     * @param extraMessage 额外消息
     * @param objects      消息中占位符的具体值
     * @return BaseException
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(
            E2 e2, String extraMessage, Object... objects) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBaseAndExtra().newInstance(e2, extraMessage, objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, e);
        }
    }

    /**
     * new BaseException
     *
     * @param e e
     * @return BaseException
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(
            E2 e2, Throwable e) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBaseAndEx().newInstance(e2, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }

    /**
     * new BaseException
     *
     * @param e            e
     * @param extraMessage 额外消息
     * @param objects      消息中占位符的具体值
     * @return BaseException
     */
    @SuppressWarnings("unchecked")
    static <E1 extends BaseException, E2 extends EnumProcessor<E1>> E1 newException(
            E2 e2, Throwable e, String extraMessage, Object... objects) {
        try {
            ExceptionConstructor<E1> exceptionConstructor = exceptionConstructor(e2.getClass());
            return exceptionConstructor.getBaseAndExAndExtra().newInstance(e2, e, extraMessage, objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new BaseException(BaseExceptionEnum.REFLECT_EXCEPTION, ex);
        }
    }
}
