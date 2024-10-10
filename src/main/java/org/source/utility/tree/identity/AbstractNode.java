package org.source.utility.tree.identity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.Tree;
import org.source.utility.utils.Jsons;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

    public abstract <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O emptyNode();

    public abstract <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> emptyTree();

    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O cast(Function<E, F> mapper) {
        O newNode = this.emptyNode();
        E ele = this.getElement();
        if (Objects.nonNull(ele)) {
            F newEle = mapper.apply(this.getElement());
            newNode.setElement(newEle);
        }
        return newNode;
    }

    public static <I, E extends Element<I>, N extends AbstractNode<I, E, N>,
            J, F extends Element<J>, O extends AbstractNode<J, F, O>> O cast(N node, Function<E, F> mapper,
                                                                             Map<J, O> targetIdMap) {
        O newNode = node.cast(mapper);
        if (Objects.nonNull(newNode.getId())) {
            targetIdMap.put(newNode.getId(), newNode);
        }
        if (!CollectionUtils.isEmpty(node.getChildren())) {
            newNode.setChildren(node.getChildren().stream().map(n -> {
                O newChildNode = AbstractNode.cast(n, mapper, targetIdMap);
                newChildNode.setParent(newNode);
                return newChildNode;
            }).toList());
        }
        return newNode;
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

    @Override
    public String toString() {
        return Jsons.str(this);
    }
}
