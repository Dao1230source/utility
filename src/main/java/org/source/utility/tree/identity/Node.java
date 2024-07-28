package org.source.utility.tree.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public interface Node<I, E extends Element<I>, N extends Node<I, E, N>> extends Comparable<N>, Serializable {
    /**
     * getParent
     *
     * @return parent
     */
    @JsonIgnore
    N getParent();

    /**
     * getChildren
     *
     * @return children
     */
    List<N> getChildren();

    /**
     * getProperty
     *
     * @param e      e
     * @param getter getter
     * @param <I>    I
     * @param <E>    E
     * @param <V>    V
     * @return V
     */
    static <I, E extends Element<I>, V> V getProperty(E e, Function<E, V> getter) {
        if (Objects.isNull(e) || Objects.isNull(getter)) {
            return null;
        }
        return getter.apply(e);
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
}
