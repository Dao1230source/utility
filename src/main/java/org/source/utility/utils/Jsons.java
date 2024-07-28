package org.source.utility.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zengfugen
 */
@Slf4j
public class Jsons {
    private Jsons() {
        throw new IllegalStateException("Utility class");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        config(MAPPER);
    }

    public static ObjectMapper getInstance() {
        return MAPPER;
    }

    public static void config(ObjectMapper mapper) {
        // 非空
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 空bean转换失败：false
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 属性值不存在失败：false
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 默认是timestamp展示，不使用
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 对于Long类型的数据，如果我们在Controller层将结果序列化为json，直接传给前端的话，在Long长度大于17位时会出现精度丢失的问题。
        // 为了避免精度丢失，将Long类型字段统一转成String类型。
        SimpleModule simpleModule = new JavaTimeModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        mapper.registerModule(simpleModule);
    }

    public static String str(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION.except();
        }
    }

    public static byte[] bytes(Object obj) {
        if (Objects.isNull(obj)) {
            return new byte[0];
        }
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION.except();
        }
    }

    public static <T> T obj(String jsonStr, JavaType javaType) {
        try {
            return MAPPER.readValue(jsonStr, javaType);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.except();
        }
    }

    public static <T> @Nullable T obj(@Nullable byte[] bytes, JavaType valueType) {
        if (Objects.isNull(bytes)) {
            return null;
        }
        try {
            return MAPPER.readValue(bytes, valueType);
        } catch (IOException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.except();
        }
    }

    public static <T> T obj(String jsonStr, Class<T> tClass) {
        return obj(jsonStr, MAPPER.constructType(tClass));
    }

    public static <T> T obj(String jsonStr, Type type) {
        return obj(jsonStr, MAPPER.constructType(type));
    }

    public static <T> @NonNull List<T> list(String jsonStr) {
        List<T> tList = obj(jsonStr, MAPPER.constructType(new TypeReference<List<T>>() {
        }));
        if (Objects.isNull(tList)) {
            return List.of();
        }
        return tList;
    }

    public static <T> @NonNull List<T> list(String jsonStr, Class<T> tClass) {
        List<T> tList = obj(jsonStr, getJavaType(List.class, tClass));
        if (Objects.isNull(tList)) {
            return List.of();
        }
        return tList;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable JavaType getJavaType(Class<?>... classes) {
        TypeFactory typeFactory = MAPPER.getTypeFactory();
        JavaType javaType;
        if (classes.length == 0) {
            return null;
        } else if (classes.length == 1) {
            javaType = typeFactory.constructType(classes[0]);
        } else {
            Class<?> parametrized = classes[0];
            if (Map.class.isAssignableFrom(parametrized)) {
                BaseExceptionEnum.SIZE_MIN.isTrue(classes.length >= 3, "3");
                JavaType keyType = getJavaType(classes[1]);
                Class<?>[] valueClasses = Arrays.copyOfRange(classes, 2, classes.length);
                JavaType valueType = getJavaType(valueClasses);
                javaType = typeFactory.constructMapType((Class<? extends Map<?, ?>>) parametrized, keyType, valueType);
            } else {
                Class<?>[] parameterClasses = Arrays.copyOfRange(classes, 1, classes.length);
                javaType = typeFactory.constructParametricType(parametrized, parameterClasses);
            }
        }
        return javaType;
    }

    public static JavaType getJavaType(Type type) {
        return MAPPER.getTypeFactory().constructType(type);
    }
}
