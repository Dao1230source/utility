package org.source.utility.tree.identity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Data
public abstract class AbstractNode<I, E extends Element<I>, N extends AbstractNode<I, E, N>> implements Node<I, E, N> {
    private E element;
    private N parent;
    private List<N> children;

    public void addChild(N child) {
        if (Objects.isNull(this.children)) {
            this.children = new ArrayList<>(16);
        }
        this.children.add(child);
    }

    public I getId() {
        return Node.getProperty(this.element, Element::getId);
    }

    public I getParentId() {
        return Node.getProperty(this.element, Element::getParentId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractNode<?, ?, ?> that = (AbstractNode<?, ?, ?>) o;
        return Objects.equals(getElement(), that.getElement()) && Objects.equals(getChildren(), that.getChildren());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getElement(), getChildren());
    }

    @Override
    public int compareTo(@NotNull N n) {
        return this.getElement().compareTo(n.getElement());
    }
}
