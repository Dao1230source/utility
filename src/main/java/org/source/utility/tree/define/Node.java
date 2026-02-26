package org.source.utility.tree.define;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface Node<I extends Comparable<I>, E extends Element<I>, N extends Node<I, E, N>> {
    @Nullable
    E getElement();

    void setElement(E element);

    /**
     * getParent
     *
     * @return parent
     */
    @Nullable
    N getParent();

    /**
     * getChildren
     *
     * @return children
     */
    List<N> getChildren();

    @JsonIgnore
    default @Nullable I getId() {
        return Node.getProperty(this, Element::getId);
    }

    @JsonIgnore
    default @Nullable I getParentId() {
        return Node.getProperty(this, Element::getParentId);
    }

    @JsonIgnore
    default boolean hasElement() {
        return Objects.nonNull(this.getElement());
    }

    @JsonIgnore
    default boolean hasChildren() {
        return !CollectionUtils.isEmpty(this.getChildren());
    }

    default void removeFromParent() {
        N parent = this.getParent();
        // 从父节点移除
        if (Objects.isNull(parent)) {
            return;
        }
        parent.removeChild(this);
    }

    void removeChild(@Nullable Node<I, E, N> child);

    /**
     * getProperty
     */
    @Nullable
    static <I extends Comparable<I>, E extends Element<I>, N extends Node<I, E, N>, V> V getProperty(
            @Nullable Node<I, E, N> n, @Nullable Function<E, V> getter) {
        if (Objects.isNull(n) || Objects.isNull(n.getElement()) || Objects.isNull(getter)) {
            return null;
        }
        return getter.apply(n.getElement());
    }

    static <I extends Comparable<I>, E extends Element<I>, N extends Node<I, E, N>, V> void setProperty(
            @Nullable Node<I, E, N> n, @Nullable BiConsumer<E, V> setter, @Nullable V value) {
        if (Objects.isNull(n) || Objects.isNull(n.getElement()) || Objects.isNull(setter)) {
            return;
        }
        setter.accept(n.getElement(), value);
    }

    /**
     * superiorNodes
     *
     * @param node          node
     * @param includeItself includeItself
     * @param <I>           I
     * @param <E>           E
     * @param <N>           Node
     * @return list
     */
    static <I extends Comparable<I>, E extends Element<I>, N extends Node<I, E, N>> List<N> superiorNodes(@Nullable N node, boolean includeItself) {
        List<N> list = new ArrayList<>();
        N current = node;
        if (Objects.isNull(current)) {
            return list;
        }
        if (includeItself) {
            list.add(current);
        }
        while (Objects.nonNull(current) && Objects.nonNull(current.getParent())) {
            current = current.getParent();
            list.add(current);
        }
        return list;
    }


    /**
     * 递归查询所有子节点
     *
     * @param node node
     * @param <I>  I
     * @param <E>  E
     * @param <N>  Node
     */
    static <I extends Comparable<I>, E extends Element<I>, N extends Node<I, E, N>> List<N> recursiveChildren(@Nullable N node, boolean includeItself) {
        List<N> result = new ArrayList<>();
        if (Objects.isNull(node)) {
            return result;
        }
        if (includeItself) {
            result.add(node);
        }
        // 使用迭代代替递归
        Deque<N> queue = new LinkedList<>(node.getChildren());
        while (!queue.isEmpty()) {
            N current = queue.removeFirst();
            result.add(current);
            List<N> children = current.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                queue.addAll(children);
            }
        }
        return result;
    }
}