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

    public static <I, E extends Element<I>> void downwardDepth(DeepNode<I, E> node) {
        int currentDepth = node.getDepth();
        DeepNode<I, E> parent = node.getParent();
        if (Objects.nonNull(parent)) {
            int parentDepth = parent.getDepth();
            if (parentDepth < currentDepth + 1) {
                parent.setDepth(currentDepth + 1);
            }
            downwardDepth(parent);
        }
    }

    public static <I, E extends Element<I>> void upwardDepth(DeepNode<I, E> node) {
        DeepNode<I, E> parent = node.getParent();
        int d = 0;
        if (Objects.nonNull(parent)) {
            d = parent.getDepth() + 1;
        }
        node.setDepth(d);
    }

    public static <I, E extends Element<I>> void nodeHandler(DeepNode<I, E> node) {
        if (node.upward) {
            upwardDepth(node);
        } else {
            downwardDepth(node);
        }
    }

    public static <I, E extends Element<I>> Tree<I, E, DeepNode<I, E>> buildTree(boolean upward) {
        return new Tree<>(() -> DeepNode.newInstance(upward), DeepNode::nodeHandler);
    }

}
