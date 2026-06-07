package org.source.utility.tree;

import org.source.utility.tree.define.EnhanceElement;

/**
 * 默认增强节点类
 * <p>
 * 继承自 {@link EnhanceNode}，提供了最简单的多父节点树节点实现。
 * </p>
 * <p>
 * 特性：
 * <ul>
 *   <li>支持多个父节点</li>
 *   <li>子节点有序排序</li>
 *   <li>元素实现 Comparable 接口</li>
 * </ul>
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>简单的 DAG 结构</li>
 *   <li>需要保持子节点顺序的树结构</li>
 *   <li>标准父子关系，但支持多父节点</li>
 * </ul>
 * </p>
 *
 * @param <I> ID 类型
 * @param <E> 元素类型，必须实现 EnhanceElement 接口
 * @author zengfugen
 */
public class DefaultEnhanceNode<I extends Comparable<I>, E extends EnhanceElement<I>> extends EnhanceNode<I, E, DefaultEnhanceNode<I, E>> {
    /**
     * 创建空节点
     *
     * @return 新的 DefaultEnhanceNode 实例
     */
    @Override
    public DefaultEnhanceNode<I, E> emptyNode() {
        return new DefaultEnhanceNode<>();
    }
}
