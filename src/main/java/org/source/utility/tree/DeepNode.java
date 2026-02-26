package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;

import java.util.Objects;

/**
* 深度节点类
* <p>
* 支持自动计算节点深度的树节点实现。
* 深度计算方式有两种：
* <ul>
*   <li><strong>从根向下</strong> (rootIsZero=true)：根节点深度=0，向下递增</li>
*   <li><strong>从叶向上</strong> (rootIsZero=false)：叶节点深度=0，向上递增</li>
* </ul>
* </p>
* <p>
* 适用于需要追踪节点层级关系的场景，如：
* <ul>
*   <li>组织结构图</li>
*   <li>分类体系</li>
*   <li>菜单结构</li>
* </ul>
* </p>
*
* @param <I> ID 类型
* @param <E> 元素类型
* @author utility
* @since 1.0
*/
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class DeepNode<I extends Comparable<I>, E extends Element<I>> extends AbstractNode<I, E, DeepNode<I, E>> {
    /**
     * 是否以根节点深度为0
     * false：叶节点深度为0（默认）
     * true：根节点深度为0
     */
    @JsonIgnore
    private final boolean rootIsZero;

    /**
     * 节点的深度值
     * 默认值：0
     */
    private int depth = 0;

    /**
     * 构造函数
     *
     * @param rootIsZero 深度计算方式的标志
     */
    public DeepNode(boolean rootIsZero) {
        this.rootIsZero = rootIsZero;
    }

    /**
     * 创建空节点
     *
     * @return 新的 DeepNode 实例，保持相同的 rootIsZero 设置
     */
    @Override
    public DeepNode<I, E> emptyNode() {
        return new DeepNode<>(rootIsZero);
    }

    /**
     * 节点处理钩子
     * <p>
     * 在节点添加到树后，根据 rootIsZero 配置计算节点深度。
     * </p>
     */
    @Override
    public void nodeHandler() {
        if (this.rootIsZero) {
            calculateDepthFromRoot(this);
        } else {
            propagateDepthUpward(this);
        }
    }

    /**
     * 向上传播深度值
     * <p>
     * 当前节点的深度值会影响所有上级节点。
     * 计算方式：叶节点深度=0，根节点深度=最大
     * 使用场景：rootIsZero = false
     * </p>
     *
     * @param node 起始节点
     * @param <I>  ID 类型
     * @param <E>  元素类型
     */
    public static <I extends Comparable<I>, E extends Element<I>> void propagateDepthUpward(DeepNode<I, E> node) {
        DeepNode<I, E> current = node;
        while (Objects.nonNull(current)) {
            DeepNode<I, E> parent = current.getParent();
            if (Objects.nonNull(parent)) {
                int newDepth = current.getDepth() + 1;
                if (parent.getDepth() < newDepth) {
                    parent.setDepth(newDepth);
                }
            }
            current = parent;
        }
    }

    /**
     * 从根节点向下计算深度
     * <p>
     * 根据节点与根的距离计算深度。
     * 计算方式：源节点深度=0
     * 使用场景：rootIsZero = true
     * </p>
     *
     * @param node 要计算深度的节点
     * @param <I>  ID 类型
     * @param <E>  元素类型
     */
    public static <I extends Comparable<I>, E extends Element<I>> void calculateDepthFromRoot(DeepNode<I, E> node) {
        DeepNode<I, E> parent = node.getParent();
        int d = 0;
        if (Objects.nonNull(parent)) {
            d = parent.getDepth() + 1;
        }
        node.setDepth(d);
    }
}