package org.source.utility.tree;

import lombok.EqualsAndHashCode;
import org.source.utility.tree.define.EnhanceElement;

/**
 * 增强树容器类
 * <p>
 * 继承自 {@link Tree}，专门用于支持 {@link EnhanceElement} 类型的元素。
 * EnhanceElement 实现了 Comparable 接口，使元素可以进行比较和排序。
 * </p>
 *
 * @param <I> ID 类型，必须实现 Comparable 接口
 * @param <E> 元素类型，必须继承 EnhanceElement
 * @param <N> 节点类型，必须继承 EnhanceNode
 * @author zengfugen
 */
@EqualsAndHashCode(callSuper = true)
public class EnhanceTree<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends Tree<I, E, N> {
    /**
     * 构造函数
     *
     * @param root 根节点
     */
    protected EnhanceTree(N root) {
        super(root);
    }

    /**
     * 创建增强树实例的工厂方法
     *
     * @param <I>  ID 类型
     * @param <E>  元素类型
     * @param <N>  节点类型
     * @param root 根节点
     * @return 新创建的增强树实例
     */
    public static <I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> EnhanceTree<I, E, N> of(N root) {
        return new EnhanceTree<>(root);
    }

}