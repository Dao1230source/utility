package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeepNode<I, E extends Element<I>> extends AbstractNode<I, E, DeepNode<I, E>> {
    @JsonIgnore
    private final boolean upward;
    private int depth = 0;

    public DeepNode(boolean upward) {
        this.upward = upward;
    }

    @Override
    public DeepNode<I, E> emptyNode() {
        return new DeepNode<>(upward);
    }

    @Override
    public void nodeHandler() {
        if (this.upward) {
            upwardDepth(this);
        } else {
            downwardDepth(this);
        }
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
}