package org.source.utility.flow.processor;

import org.source.utility.flow.Flow;
import org.source.utility.flow.node.Key;
import org.source.utility.flow.node.Value;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 适用于本业务的处理器，
 * 业务处理结束后是否继续执行流程的标记为true/false
 *
 * @param <K>
 * @author zengfugen
 */
public interface Processor<K> extends Key<K>, Value<Boolean> {
    String DEFAULT_NAME = "Processor_";

    /**
     * 定义处理器的名称
     *
     * @return 名称
     */
    default String getName() {
        return DEFAULT_NAME + this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * 简易处理器
     *
     * @param key      key值
     * @param consumer 消费者
     * @param t        参数
     * @param <K>      泛型
     * @param <T>      泛型
     * @return Processor<K>
     */
    static <K, T> Processor<K> ofSimple(K key, Consumer<T> consumer, T t) {
        return new Processor<>() {
            @Override
            public K getKey() {
                return key;
            }

            @Override
            public Boolean value() {
                consumer.accept(t);
                return true;
            }
        };
    }

    /**
     * 简易处理器
     *
     * @param key      key值
     * @param supplier supplier
     * @param <K>      泛型
     * @return Processor<K>
     */
    static <K> Processor<K> ofSimple(K key, BooleanSupplier supplier) {
        return new Processor<>() {

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public Boolean value() {
                return supplier.getAsBoolean();
            }
        };
    }

    /**
     * 简易处理器
     *
     * @param key     key值
     * @param subFlow 子流程
     * @param <K>     泛型
     * @return Processor<K>
     */
    static <K> Processor<K> ofSimple(K key, Flow<?, ?> subFlow) {
        return ofSimple(key, Flow::invoke, subFlow);
    }

    /**
     * 空Boolean 处理器
     * 默认key=false
     *
     * @param key key
     * @return Processor
     */
    static <K> Processor<K> ofBlank(K key) {
        return ofSimple(key, () -> true);
    }
}
