package org.source.utility.tree.define;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface Node<I, E extends Element<I>, N extends Node<I, E, N>> {
    E getElement();

    void setElement(E element);

    /**
     * getParent
     *
     * @return parent
     */
    N getParent();

    /**
     * getChildren
     *
     * @return children
     */
    List<N> getChildren();

    @JsonIgnore
    default I getId() {
        return Node.getProperty(this, Element::getId);
    }

    @JsonIgnore
    default I getParentId() {
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

    /**
     * getProperty
     */
    @Nullable
    static <I, E extends Element<I>, N extends Node<I, E, N>, V> V getProperty(Node<I, E, N> n, Function<E, V> getter) {
        if (Objects.isNull(n) || Objects.isNull(n.getElement()) || Objects.isNull(getter)) {
            return null;
        }
        return getter.apply(n.getElement());
    }

    static <I, E extends Element<I>, N extends Node<I, E, N>, V> void setProperty(Node<I, E, N> n, BiConsumer<E, V> setter, V value) {
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
    static <I, E extends Element<I>, N extends Node<I, E, N>> List<N> superiorNodes(N node, boolean includeItself) {
        List<N> list = new ArrayList<>();
        if (Objects.isNull(node)) {
            return list;
        }
        N current = node;
        if (includeItself) {
            list.add(current);
        }
        while (Objects.nonNull(current.getParent())) {
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
    static <I, E extends Element<I>, N extends Node<I, E, N>> List<N> recursiveChildren(N node, boolean includeItself) {
        List<N> list = new ArrayList<>();
        if (Objects.isNull(node)) {
            return list;
        }
        if (includeItself) {
            list.add(node);
        }
        if (CollectionUtils.isEmpty(node.getChildren())) {
            return list;
        }
        list.addAll(node.getChildren());
        node.getChildren().forEach(n -> list.addAll(recursiveChildren(n, includeItself)));
        return list;
    }
}