package org.source.utility.tree;

import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;

/**
* 默认节点类
* <p>
* 最简单的树节点实现，继承 AbstractNode 的所有功能。
* 无额外特性，适用于基础的树结构实现。
* </p>
* <p>
* 使用场景：
* <ul>
*   <li>简单的树形结构</li>
*   <li>标准的父子关系</li>
*   <li>无特殊深度计算需求</li>
* </ul>
* </p>
*
* @param <I> ID 类型
* @param <E> 元素类型
*
* @author utility
* @since 1.0
*/
public class DefaultNode<I extends Comparable<I>, E extends Element<I>> extends AbstractNode<I, E, DefaultNode<I, E>> {
    /**
     * 创建空节点
     *
     * @return 新的 DefaultNode 实例
     */
    @Override
    public DefaultNode<I, E> emptyNode() {
        return new DefaultNode<>();
    }
}