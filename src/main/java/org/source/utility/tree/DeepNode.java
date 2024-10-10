package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeepNode<I, E extends Element<I>> extends AbstractNode<I, E, DeepNode<I, E>> {
    @JsonIgnore
    private final boolean upward;
    private int depth = 0;

    public void downwardDepth() {
        int currentDepth = this.getDepth();
        DeepNode<I, E> parent = this.getParent();
        if (Objects.nonNull(parent)) {
            int parentDepth = parent.getDepth();
            if (parentDepth < currentDepth + 1) {
                parent.setDepth(currentDepth + 1);
            }
            parent.downwardDepth();
        }
    }

    public void upwardDepth() {
        DeepNode<I, E> parent = this.getParent();
        int d = 0;
        if (Objects.nonNull(parent)) {
            d = parent.getDepth() + 1;
        }
        this.setDepth(d);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O emptyNode() {
        return (O) DeepNode.newInstance(upward);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> emptyTree() {
        return (Tree<J, F, O>) DeepNode.buildTree(this.upward);
    }

    public static <I, E extends Element<I>> DeepNode<I, E> newInstance(boolean upward) {
        return new DeepNode<>(upward);
    }

    public static <I, E extends Element<I>> void nodeHandler(DeepNode<I, E> node) {
        if (node.upward) {
            node.upwardDepth();
        } else {
            node.downwardDepth();
        }
    }

    public static <I, E extends Element<I>> Tree<I, E, DeepNode<I, E>> buildTree(boolean upward) {
        return new Tree<>(() -> DeepNode.newInstance(upward), DeepNode::nodeHandler);
    }

}
