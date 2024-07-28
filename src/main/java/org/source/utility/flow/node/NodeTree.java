package org.source.utility.flow.node;

import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author zengfugen
 */
@Data
public class NodeTree<E> {

    protected E element;
    protected NodeTree<E> parent;
    protected Set<NodeTree<E>> children;

    public void addChild(NodeTree<E> child) {
        if (null == this.children) {
            this.children = new HashSet<>();
        }
        this.children.add(child);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeTree<?> nodeTree)) {
            return false;
        }
        return element.equals(nodeTree.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element);
    }
}
