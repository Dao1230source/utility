package org.source.utility.flow.point;

import org.source.utility.flow.processor.Processor;
import org.source.utility.flow.node.Selector;
import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理器容器类
 * <p>
 * 存储多个处理器，并通过选择器选择要执行的处理器。
 * 支持单一处理器和多处理器场景。
 * </p>
 *
 * @param <K> Key 类型
 * @param <V> 处理器类型
 * @author zengfugen
 */
@Data
public class Container<K, V extends Processor<K>> {

    /**
     * Key 到处理器的映射
     */
    private final Map<K, V> kvMap = new HashMap<>();

    /**
     * 选择器，用于决定执行哪个处理器
     */
    private Selector<K> selector;

    /**
     * 选择要执行的处理器
     *
     * @return 选中的处理器，未选中返回 null
     */
    public Processor<K> select() {
        if (null != selector.select()) {
            return kvMap.get(selector.select());
        }
        return null;
    }

    /**
     * 创建包含单个处理器的容器
     *
     * @param <K>       Key 类型
     * @param <V>       处理器类型
     * @param processor 处理器
     * @return Container 实例
     */
    public static <K, V extends Processor<K>> Container<K, V> of(V processor) {
        return new Container<>(processor);
    }

    /**
     * 创建包含多个处理器的容器，使用选择器决定执行哪个处理器
     *
     * @param <K>        Key 类型
     * @param <V>        处理器类型
     * @param keyGetter  选择器
     * @param processors 处理器数组
     * @return Container 实例
     */
    @SafeVarargs
    public static <K, V extends Processor<K>> Container<K, V> of(Selector<K> keyGetter, V... processors) {
        return new Container<>(keyGetter, processors);
    }

    /**
     * 构造函数（单处理器）
     *
     * @param processor 处理器
     */
    private Container(V processor) {
        this.kvMap.put(processor.getKey(), processor);
        this.selector = processor::getKey;
    }

    /**
     * 构造函数（多处理器）
     *
     * @param selector   选择器
     * @param processors 处理器数组
     */
    @SafeVarargs
    private Container(Selector<K> selector, V... processors) {
        this.selector = selector;
        if (null != processors) {
            Arrays.stream(processors).forEach(k -> this.kvMap.put(k.getKey(), k));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{selector:").append(this.selector.toString()).append(",").append("kvMap:{");
        this.kvMap.forEach((k, v) -> sb.append(k).append(":").append(v.getName()));
        sb.append("}}");
        return sb.toString();
    }
}
