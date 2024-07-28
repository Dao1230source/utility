package org.source.utility.flow.node;

/**
 * 从容器中选择执行的处理器
 * @author zengfugen
 * @param <K>
 */
public interface Selector<K> {

    /**
     * 路由选择
     *
     * @return 选择执行的处理器
     */
    K select();
}
