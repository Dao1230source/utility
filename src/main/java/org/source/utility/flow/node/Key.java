package org.source.utility.flow.node;

import org.source.utility.flow.point.Container;

/**
 * 获取容器{@link Container}中的key值
 *
 * @author zengfugen
 */
public interface Key<K> {

    /**
     * 获取key值
     *
     * @return key
     */
    K getKey();
}
