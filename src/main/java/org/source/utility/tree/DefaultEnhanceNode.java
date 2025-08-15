package org.source.utility.tree;

import org.source.utility.tree.define.EnhanceElement;

public class DefaultEnhanceNode<I extends Comparable<I>, E extends EnhanceElement<I>> extends EnhanceNode<I, E, DefaultEnhanceNode<I, E>> {
    @Override
    public DefaultEnhanceNode<I, E> emptyNode() {
        return new DefaultEnhanceNode<>();
    }
}
