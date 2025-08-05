package org.source.utility.tree;

import lombok.EqualsAndHashCode;
import org.source.utility.tree.define.EnhanceElement;

import java.util.Objects;
import java.util.function.BinaryOperator;

@EqualsAndHashCode(callSuper = true)
public class EnhanceTree<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends Tree<I, E, N> {
    protected EnhanceTree(N root) {
        super(root);
    }

    public static <I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> EnhanceTree<I, E, N> of(N root) {
        EnhanceTree<I, E, N> tree = new EnhanceTree<>(root);
        BinaryOperator<N> mergeHandler = (n, old) -> {
            // ID相同
            if (Objects.nonNull(old) && Objects.nonNull(n) && n.getId().equals(old.getId())) {
                return old;
            }
            return n;
        };
        tree.setMergeHandler(mergeHandler);
        return tree;
    }

}
