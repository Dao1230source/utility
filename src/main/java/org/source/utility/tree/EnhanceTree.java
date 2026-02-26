package org.source.utility.tree;

import lombok.EqualsAndHashCode;
import org.source.utility.tree.define.EnhanceElement;

@EqualsAndHashCode(callSuper = true)
public class EnhanceTree<I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> extends Tree<I, E, N> {
    protected EnhanceTree(N root) {
        super(root);
    }

    public static <I extends Comparable<I>, E extends EnhanceElement<I>, N extends EnhanceNode<I, E, N>> EnhanceTree<I, E, N> of(N root) {
        return new EnhanceTree<>(root);
    }

}