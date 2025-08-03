package org.source.utility.tree;

import lombok.EqualsAndHashCode;
import org.source.utility.tree.define.EnhanceElement;
import org.source.utility.tree.define.Node;

import java.util.Objects;
import java.util.function.BinaryOperator;

@EqualsAndHashCode(callSuper = true)
public class EnhanceTree<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends Tree<I, E, N> {
    protected EnhanceTree(N root) {
        super(root);
    }

    public static <I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> EnhanceTree<I, E, N> of(N root) {
        EnhanceTree<I, E, N> tree = new EnhanceTree<>(root);
        // ID相同，才会找source node
        BinaryOperator<N> mergeHandler = (n, old) -> {
            if (Objects.isNull(old) || Objects.isNull(n) || !n.getId().equals(old.getId())) {
                return n;
            }
            // ID相同，才会找source node
            I sourceId = Node.getProperty(n, EnhanceElement::getSourceId);
            if (Objects.isNull(sourceId)) {
                return n;
            }
            return tree.getIdMap().getOrDefault(sourceId, n);
        };
        tree.setMergeHandler(mergeHandler);
        return tree;
    }

}
