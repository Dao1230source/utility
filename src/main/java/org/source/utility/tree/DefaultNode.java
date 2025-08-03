package org.source.utility.tree;

import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;

public class DefaultNode<I, E extends Element<I>> extends AbstractNode<I, E, DefaultNode<I, E>> {
    @Override
    public DefaultNode<I, E> emptyNode() {
        return new DefaultNode<>();
    }
}
