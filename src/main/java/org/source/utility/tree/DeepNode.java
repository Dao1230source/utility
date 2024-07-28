package org.source.utility.tree;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeepNode<I, E extends Element<I>> extends AbstractNode<I, E, DeepNode<I, E>> {

    private int depth;

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
        int currentDepth = this.getDepth();
        List<DeepNode<I, E>> children = this.getChildren();
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        children.forEach(k -> {
            k.setDepth(currentDepth + 1);
            k.upwardDepth();
        });
    }
}
