package org.source.utility.flow.point;

import org.source.utility.flow.processor.Processor;
import org.source.utility.flow.node.Selector;
import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点上的容器
 *
 * @author zengfugen
 * @param <K>
 * @param <V>
 */
@Data
public class Container<K, V extends Processor<K>> {

    private final Map<K, V> kvMap = new HashMap<>();

    private Selector<K> selector;

    public Processor<K> select() {
        if (null != selector.select()) {
            return kvMap.get(selector.select());
        }
        return null;
    }

    public static <K, V extends Processor<K>> Container<K, V> of(V processor) {
        return new Container<>(processor);
    }

    @SafeVarargs
    public static <K, V extends Processor<K>> Container<K, V> of(Selector<K> keyGetter, V... processors) {
        return new Container<>(keyGetter, processors);
    }

    private Container(V processor) {
        this.kvMap.put(processor.getKey(), processor);
        this.selector = processor::getKey;
    }

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
