package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;
import org.source.utility.tree.define.EnhanceElement;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;


/**
 * 增强节点类
 * <p>
 * 支持多父节点的树结构实现（DAG - Directed Acyclic Graph）。
 * 相比 AbstractNode，此节点支持：
 * <ul>
 *   <li>多个父节点：一个节点可以有多个父节点</li>
 *   <li>有序子节点：使用 TreeSet 按元素自然顺序排序子节点</li>
 *   <li>JSON 序列化：支持将多父节点信息序列化</li>
 * </ul>
 * </p>
 * <p>
 * 适用于表示具有复杂关系的数据结构，如：
 * <ul>
 *   <li>文件系统（支持符号链接）</li>
 *   <li>知识图谱</li>
 *   <li>任务依赖关系</li>
 * </ul>
 * </p>
 *
 * @param <I> ID 类型
 * @param <E> 元素类型，必须实现 EnhanceElement 接口
 * @param <N> 节点类型，必须继承 EnhanceNode
 * @author utility
 * @since 1.0
 */
@JsonIgnoreProperties(value = {"comparator"})
public class EnhanceNode<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends AbstractNode<I, E, N> {
    /**
     * 多个父节点集合
     * 支持一个节点有多个父节点
     * 使用 LinkedHashSet 保持插入顺序
     */
    private final LinkedHashSet<N> parents = new LinkedHashSet<>();

    /**
     * 有序子节点集合
     * 使用 TreeSet 按元素自然顺序排序
     */
    private final TreeSet<N> children = new TreeSet<>(this.comparator);

    /**
     * 子节点比较器
     * 用于 TreeSet 排序子节点
     * 排序规则：按元素的 compareTo 方法，null 元素放在最后
     */
    private final Comparator<N> comparator = (o1, o2) -> Element.nullLast(o1.getElement(), o2.getElement(), EnhanceElement::compareTo);

    /**
     * 创建空节点
     *
     * @return 新的 EnhanceNode 实例
     */
    @SuppressWarnings("unchecked")
    @Override
    public N emptyNode() {
        return (N) new EnhanceNode<>();
    }

    /**
     * 添加子节点
     * <p>
     * 将子节点添加到有序的 TreeSet 中。
     * 如果是第一次添加子节点，会创建新的 TreeSet。
     * </p>
     *
     * @param child 要添加的子节点
     */
    @Override
    public N addChild(N child) {
        this.children.add(child);
        return child;
    }

    /**
     * 添加到父节点
     * <p>
     * 将此节点添加到给定的父节点。
     * 支持多个父节点，可以多次调用此方法添加多个父节点。
     * </p>
     *
     * @param parent 父节点，可以为 null
     */
    @Override
    public void appendToParent(N parent) {
        this.parents.add(parent);
    }

    /**
     * 获取子节点列表
     * <p>
     * 返回按元素自然顺序排列的有序子节点列表。
     * 返回的是副本，修改返回列表不会影响原节点。
     * </p>
     *
     * @return 有序的子节点列表，如果没有子节点则返回空列表
     */
    @Override
    public List<N> getChildren() {
        return List.copyOf(this.children);
    }

    /**
     * 获取多父节点信息用于 JSON 序列化
     * <p>
     * 当节点有多个父节点时，以列表形式返回（排除单个父节点的情况）。
     * 用于 JSON 序列化，标记为只读属性。
     * </p>
     *
     * @return 父节点对应的元素列表，如果父节点数 &lt;= 1 则返回空列表
     */
    @JsonProperty(value = "parents", access = JsonProperty.Access.READ_ONLY)
    public List<I> parentsToJson() {
        // 只有一个父级时无需展示
        if (CollectionUtils.isEmpty(this.parents) || this.parents.size() == 1) {
            return List.of();
        }
        return Streams.of(parents).map(N::getId).filter(Objects::nonNull).toList();
    }

    /**
     * 获取所有父节点
     * <p>
     * 返回此节点的所有父节点的集合。
     * 返回的集合为 null 表示没有父节点。
     * </p>
     *
     * @return 父节点集合，可以为 null
     */
    @Override
    public List<N> findParents() {
        if (CollectionUtils.isEmpty(this.parents)) {
            return List.of();
        }
        return Streams.retain(this.parents, Objects::nonNull).toList();
    }

    /**
     * 从所有父节点移除此节点
     * <p>
     * 将此节点从其所有父节点的子节点列表中移除。
     * 用于节点删除操作。
     * </p>
     */
    @Override
    public void removeFromParent() {
        // 从父节点移除
        List<N> parents1 = this.findParents();
        if (CollectionUtils.isEmpty(parents1)) {
            return;
        }
        parents1.forEach(p -> p.removeChild(this));
    }
}