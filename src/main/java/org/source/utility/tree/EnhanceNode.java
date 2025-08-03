package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.EnhanceElement;
import org.springframework.util.CollectionUtils;

import java.util.*;

@JsonIgnoreProperties(value = {"parent", "parents", "comparator"})
@Data
public class EnhanceNode<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends AbstractNode<I, E, N> {
    private Set<N> parents;
    private TreeSet<N> children;
    private Comparator<N> comparator = EnhanceNode::comparator;

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
            this.parents = new HashSet<>();
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

    public static <I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> int comparator(N first, N second) {
        return EnhanceElement.nullLast(first.getElement(), second.getElement(), EnhanceElement::compareTo);
    }
}