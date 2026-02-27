package org.source.utility.tree.define;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * 抽象节点基类
 * <p>
 * 提供树节点的基本功能实现，包括：
 * <ul>
 *   <li>元素存储：包含业务数据</li>
 *   <li>父子关系：维护与父节点和子节点的连接</li>
 *   <li>子节点管理：支持添加、获取子节点</li>
 *   <li>并发安全：使用 ConcurrentHashMap 存储子节点</li>
 *   <li>序列化支持：通过 Jackson 注解实现 JSON 序列化</li>
 * </ul>
 * </p>
 * <p>
 * 子类需要实现 {@link #emptyNode()} 方法以支持节点的创建。
 * </p>
 *
 * @param <I> ID 类型，必须实现 Comparable 接口
 * @param <E> 元素类型，必须实现 Element 接口
 * @param <N> 节点类型，必须继承 AbstractNode
 * @author utility
 * @since 1.0
 */
@JsonIgnoreProperties({"childrenMap", "mergeHandler"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Slf4j
@Data
public abstract class AbstractNode<I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> implements Node<I, E, N> {
    /**
     * 节点包含的业务元素
     * 为 null 表示该节点是虚拟节点（如根节点）
     */
    private @Nullable E element;

    /**
     * 父节点引用
     * 用于维护树的父子关系
     * 根节点的 parent 为 null
     */
    @JsonBackReference
    private @Nullable N parent;

    /**
     * 子节点映射表
     * key：子节点的 ID
     * value：子节点对象
     * 使用 ConcurrentHashMap 保证并发安全性
     */
    private Map<I, N> childrenMap = new ConcurrentHashMap<>();

    /**
     * 新旧数据合并处理器
     * 当添加的节点 ID 已存在时，用此处理器合并新旧节点
     * 默认：保留新节点
     */
    private BinaryOperator<N> mergeHandler = (n, old) -> n;
    /**
     * 当 mergeHandler 返回null时如何处理
     */
    private MergeReturnNullStrategyEnum mergeReturnNullStrategy;

    /**
     * 创建一个空的同类型节点
     * <p>
     * 用于在树中创建新的节点实例。
     * 子类必须实现此方法。
     * </p>
     *
     * @return 新创建的空节点
     */
    public abstract N emptyNode();

    /**
     * 节点处理钩子方法
     * <p>
     * 在节点被添加到树后调用，用于执行节点特定的初始化或处理逻辑。
     * 子类可以覆盖此方法以实现自定义处理。
     * 默认实现：无操作
     * </p>
     */
    public void nodeHandler() {
    }

    /**
     * 获取子节点列表
     * <p>
     * 返回此节点的所有子节点的不可修改列表。
     * 返回的是副本，修改返回列表不会影响原节点。
     * </p>
     *
     * @return 子节点列表，如果没有子节点则返回空列表
     */
    @JsonManagedReference
    @Override
    public List<N> getChildren() {
        return List.copyOf(childrenMap.values());
    }

    /**
     * 添加子节点
     * <p>
     * 将给定的子节点添加到此节点的子节点映射中。
     * 如果子节点 ID 已存在且设置了合并处理器，则使用处理器合并节点。
     * 否则，直接替换或添加节点。
     * </p>
     *
     * @param child 要添加的子节点，不能为 null
     * @return 添加成功的子节点，返回的节点不一定是要添加的child，受 mergeHandler 和 mergeReturnNullStrategy 的影响。可能返回 null
     */
    public @Nullable N addChild(N child) {
        return mergeNode(child, Node::getId, this.getChildrenMap(), this.getMergeHandler(), this.getMergeReturnNullStrategy());
    }

    public static <I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> @Nullable N mergeNode(
            N n,
            Function<N, I> idGetter,
            Map<I, N> idMap,
            @Nullable BinaryOperator<N> mergeHandler,
            @Nullable MergeReturnNullStrategyEnum mergeReturnNullStrategy) {
        I id = idGetter.apply(n);
        if (Objects.isNull(id)) {
            return null;
        }
        N result = n;
        N old = idMap.get(id);
        if (Objects.nonNull(old) && Objects.nonNull(mergeHandler)) {
            result = mergeHandler.apply(n, old);
        }
        if (Objects.nonNull(result)) {
            idMap.put(id, result);
            return result;
        }
        if (MergeReturnNullStrategyEnum.RETAIN_NEW.equals(mergeReturnNullStrategy)) {
            idMap.put(id, n);
            return n;
        } else if (MergeReturnNullStrategyEnum.RETAIN_OLD.equals(mergeReturnNullStrategy)) {
            idMap.put(id, old);
            return old;
        } else if (MergeReturnNullStrategyEnum.REMOVE_OLD.equals(mergeReturnNullStrategy)) {
            // old是已存在的节点数据，getId 一定有值
            idMap.remove(old.getId());
            return null;
        } else if (MergeReturnNullStrategyEnum.THROW_EXCEPTION.equals(mergeReturnNullStrategy)) {
            throw BaseExceptionEnum.TREE_MERGE_EXCEPTION.except("Merge result is null");
        } else {
            // mergeReturnNullStrategy = null 时默认保留新数据
            idMap.put(id, n);
            return n;
        }
    }

    /**
     * 添加到父节点
     * <p>
     * 设置此节点的父节点。
     * 子类可以覆盖此方法以实现额外的逻辑，如循环检测。
     * </p>
     *
     * @param parent 父节点，不可为null
     */
    public void appendToParent(N parent) {
        this.parent = parent;
    }

    /**
     * <p>
     * 返回此节点的父节点集合。
     * 由于树节点是单向的，因此父节点集合中最多只有一个元素。
     * 返回的是副本，修改返回集合不会影响原节点。
     * 如果是EnhanceNode，则返回多个元素
     * </p>
     */
    public List<N> findParents() {
        if (Objects.isNull(this.parent)) {
            return List.of();
        }
        return List.of(this.parent);
    }

    @Override
    public void removeChild(@Nullable Node<I, E, N> child) {
        if (Objects.isNull(child)) {
            return;
        }
        if (CollectionUtils.isEmpty(this.childrenMap)) {
            return;
        }
        I childId = child.getId();
        if (Objects.nonNull(childId)) {
            this.childrenMap.remove(childId);
        }
    }

    /**
     * 清理节点
     * <p>
     * 清除节点中的所有数据和引用，包括：
     * <ul>
     *   <li>元素对象</li>
     *   <li>父节点引用</li>
     *   <li>所有子节点</li>
     * </ul>
     * 用于节点删除后的资源清理。
     * </p>
     */
    public void clear() {
        this.element = null;
        this.parent = null;
        if (Objects.nonNull(this.childrenMap)) {
            this.childrenMap.clear();
        }
    }

    /**
     * 只比较 element
     * <br>
     * 重写 equals 等方法避免自动生成的方法比较children等造成{@literal StackOverflowError}异常
     *
     * @param o other
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractNode<?, ?, ?> that = (AbstractNode<?, ?, ?>) o;
        return Objects.equals(getElement(), that.getElement());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getElement());
    }

    @Override
    public String toString() {
        if (Objects.isNull(this.getElement())) {
            return Constants.EMPTY;
        }
        return Jsons.str(this.getElement());
    }
}