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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * JSON 序列化与反序列化工具类
 * <p>
 * 基于 Jackson 提供统一的 JSON 处理能力，配置了以下特性：
 * </p>
 * <ul>
 *   <li>序列化包含所有属性（不忽略 null 值）</li>
 *   <li>忽略未知属性（反序列化时不会因字段不存在而失败）</li>
 *   <li>不使用时间戳格式，使用标准日期格式</li>
 *   <li>Long 类型转 String 避免前端精度丢失</li>
 * </ul>
 *
 * @author zengfugen
 */
@Slf4j
@UtilityClass
public class Jsons {

    /**
     * 全局共享的 ObjectMapper 实例
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        config(MAPPER);
    }

    /**
     * 获取配置好的 ObjectMapper 实例
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getInstance() {
        return MAPPER;
    }

    /**
     * 配置 ObjectMapper 的序列化和反序列化特性
     * <p>
     * 配置包括：
     * </p>
     * <ul>
     *   <li>包含所有属性（不忽略 null 值）</li>
     *   <li>空 Bean 不报错</li>
     *   <li>忽略未知属性</li>
     *   <li>不使用时间戳格式</li>
     *   <li>Long 类型转 String</li>
     * </ul>
     *
     * @param mapper 要配置的 ObjectMapper
     */
    public static void config(ObjectMapper mapper) {
        // 全部展示
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
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

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     * @throws BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION 序列化失败时抛出
     */
    public static String str(Object obj) {
        try {
            return getInstance().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION.newException(e);
        }
    }

    /**
     * 将对象序列化为 JSON 字节数组
     *
     * @param obj 要序列化的对象
     * @return JSON 字节数组
     * @throws BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION 序列化失败时抛出
     */
    public static byte[] bytes(Object obj) {
        try {
            return getInstance().writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION.newException(e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param <T>      目标类型
     * @param jsonStr  JSON 字符串
     * @param javaType 目标类型的 JavaType
     * @return 反序列化后的对象
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> T obj(String jsonStr, JavaType javaType) {
        try {
            return getInstance().readValue(jsonStr, javaType);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.newException(e);
        }
    }

    /**
     * 将 JSON 字节数组反序列化为指定类型的对象
     *
     * @param <T>       目标类型
     * @param bytes     JSON 字节数组
     * @param valueType 目标类型的 JavaType
     * @return 反序列化后的对象
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> T obj(byte[] bytes, JavaType valueType) {
        try {
            return getInstance().readValue(bytes, valueType);
        } catch (IOException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.newException(e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param <T>     目标类型
     * @param jsonStr JSON 字符串
     * @param tClass  目标类型的 Class 对象
     * @return 反序列化后的对象
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> T obj(String jsonStr, Class<T> tClass) {
        return obj(jsonStr, getInstance().constructType(tClass));
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象（支持泛型）
     *
     * @param <T>     目标类型
     * @param jsonStr JSON 字符串
     * @param type    目标类型的 Type 对象
     * @return 反序列化后的对象
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> T obj(String jsonStr, Type type) {
        return obj(jsonStr, getInstance().constructType(type));
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象（使用 TypeReference）
     * <p>
     * 适用于复杂的泛型类型，如 {@code Map<String, List<User>>}
     * </p>
     *
     * @param <T>           目标类型
     * @param jsonStr       JSON 字符串
     * @param typeReference 类型引用
     * @return 反序列化后的对象
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> T obj(String jsonStr, TypeReference<T> typeReference) {
        return obj(jsonStr, typeReference.getType());
    }

    /**
     * 将 JSON 字符串反序列化为 List
     * <p>
     * 注意：由于类型擦除，无法确定 List 元素类型，实际使用建议使用 {@link #list(String, Class)}
     * </p>
     *
     * @param <T>     List 元素类型
     * @param jsonStr JSON 字符串
     * @return 反序列化后的 List
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> List<T> list(String jsonStr) {
        return obj(jsonStr, getInstance().constructType(new TypeReference<List<T>>() {
        }));
    }

    /**
     * 将 JSON 字符串反序列化为指定元素类型的 List
     *
     * @param <T>     List 元素类型
     * @param jsonStr JSON 字符串
     * @param tClass  List 元素类型的 Class 对象
     * @return 反序列化后的 List
     * @throws BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION 反序列化失败时抛出
     */
    public static <T> List<T> list(String jsonStr, Class<T> tClass) {
        return obj(jsonStr, getJavaType(List.class, tClass));
    }

    /**
     * 根据类型数组构建 JavaType
     * <p>
     * 支持构建复杂类型：
     * </p>
     * <ul>
     *   <li>单个类型：getJavaType(User.class) → User</li>
     *   <li>泛型类型：getJavaType(List.class, User.class) → List&lt;User&gt;</li>
     *   <li>Map 类型：getJavaType(Map.class, String.class, User.class) → Map&lt;String, User&gt;</li>
     *   <li>嵌套泛型：getJavaType(Map.class, String.class, List.class, User.class) → Map&lt;String, List&lt;User&gt;&gt;</li>
     * </ul>
     *
     * @param classes 类型数组，第一个为容器类型，其余为泛型参数类型
     * @return 构建的 JavaType
     */
    @SuppressWarnings("unchecked")
    public static JavaType getJavaType(Class<?>... classes) {
        TypeFactory typeFactory = getInstance().getTypeFactory();
        JavaType javaType;
        if (classes.length == 0) {
            return typeFactory.constructArrayType(String.class);
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

    /**
     * 根据类型对象构建 JavaType
     *
     * @param type 类型对象
     * @return 构建的 JavaType
     */
    public static JavaType getJavaType(Type type) {
        return getInstance().getTypeFactory().constructType(type);
    }
}