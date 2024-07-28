package org.source.utility.flow.processor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author zengfugen
 */
public interface StringProcessor extends Processor<String> {

    String DEFAULT_KEY = "default";

    /**
     * 获取String的key值
     *
     * @return key值
     */
    @Override
    default String getKey() {
        return this.getClass().getSimpleName();
    }

    /**
     * 空String 处理器
     *
     * @param key key
     * @return StringProcessor
     */
    static Processor<String> ofBlank(String key) {
        return Processor.ofBlank(key);
    }

    /**
     * 空String 处理器
     *
     * @return StringProcessor
     */
    static Processor<String> ofBlank() {
        return ofBlank(DEFAULT_KEY);
    }

    /**
     * 简易处理器
     *
     * @param consumer 消费者
     * @param t        参数
     * @param <T>      泛型
     * @return StringProcessor
     */
    static <T> Processor<String> ofSimple(Consumer<T> consumer, T t) {
        return Processor.ofSimple(DEFAULT_KEY, consumer, t);
    }

    /**
     * 简易处理器
     *
     * @param supplier supplier
     * @return StringProcessor
     */
    static Processor<String> ofSimple(BooleanSupplier supplier) {
        return Processor.ofSimple(DEFAULT_KEY, supplier);
    }
}
