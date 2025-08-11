package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;
import org.source.utility.tree.define.EnhanceElement;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;

@JsonIgnoreProperties(value = {"comparator"})
public class EnhanceNode<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends AbstractNode<I, E, N> {
    private LinkedHashSet<N> parents;
    private TreeSet<N> children;
    private final Comparator<N> comparator = (o1, o2) -> Element.nullLast(o1.getElement(), o2.getElement(), EnhanceElement::compareTo);

    @SuppressWarnings("unchecked")
    @Override
    public N emptyNode() {
        return (N) new EnhanceNode<>();
    }

    @Override
    public void addChild(N child) {
        if (CollectionUtils.isEmpty(this.children)) {
            this.children = new TreeSet<>(this.comparator);
        }
        this.children.add(child);
    }

    @Override
    public void appendToParent(N parent) {
        if (CollectionUtils.isEmpty(this.parents)) {
            this.parents = new LinkedHashSet<>();
        }
        this.parents.add(parent);
    }

    @Override
    public List<N> getChildren() {
        if (Objects.isNull(this.children)) {
            return List.of();
        }
        return new ArrayList<>(this.children);
    }

    @Override
    public void setChildren(List<N> children) {
        if (Objects.isNull(this.children)) {
            this.children = new TreeSet<>(this.comparator);
        }
        this.children.addAll(children);
    }

    @JsonProperty(value = "parents", access = JsonProperty.Access.READ_ONLY)
    public List<E> parentsToJson() {
        // 只有一个父级时无需展示
        if (CollectionUtils.isEmpty(this.parents) || this.parents.size() == 1) {
            return List.of();
        }
        return Streams.of(parents).map(N::getElement).filter(Objects::nonNull).toList();
    }

    @JsonIgnore
    public Set<N> getParents() {
        return parents;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}