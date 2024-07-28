package org.source.utility.flow.node;

/**
 * 容器保存的对象
 * @author zengfugen
 * @param <V>
 */
public interface Value<V> {

    /**
     * 处理逻辑
     *
     * @return 是否继续处理流程
     */
    V value();
}
