package org.source.utility.tree;

import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;

public class DefaultNode<I, E extends Element<I>> extends AbstractNode<I, E, DefaultNode<I, E>> {
    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O emptyNode() {
        return (O) DefaultNode.newInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> emptyTree() {
        return (Tree<J, F, O>) DefaultNode.buildTree();
    }

    public static <I, E extends Element<I>> DefaultNode<I, E> newInstance() {
        return new DefaultNode<>();
    }

    public static <I, E extends Element<I>> void nodeHandler(DefaultNode<I, E> node) {
        // nothing to do
    }

    public static <I, E extends Element<I>> Tree<I, E, DefaultNode<I, E>> buildTree() {
        return new Tree<>(DefaultNode::newInstance, DefaultNode::nodeHandler);
    }
}
