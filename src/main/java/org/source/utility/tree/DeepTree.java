package org.source.utility.tree;

import org.source.utility.tree.identity.Element;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DeepTree<I, E extends Element<I>> extends Tree<I, E, DeepNode<I, E>> {
    private final boolean upward;

    public DeepTree(Supplier<DeepNode<I, E>> newInstance, BiConsumer<DeepNode<I, E>, E> elementHandler, boolean upward) {
        super(newInstance, elementHandler);
        this.upward = upward;
    }

    @Override
    public DeepNode<I, E> add(Collection<? extends E> es) {
        DeepNode<I, E> root = super.add(es);
        calculateDepth(this, root, upward);
        return root;
    }

    public static <I, E extends Element<I>> Tree<I, E, DeepNode<I, E>> buildDeep(boolean upward) {
        BiConsumer<DeepNode<I, E>, E> nElementBiConsumer = NodeEnum.DEEP.elementHandlerDefault();
        return new DeepTree<>(NodeEnum.DEEP::newInstance, nElementBiConsumer, upward);
    }

    public static <I, E extends Element<I>> void calculateDepth(Tree<I, E, DeepNode<I, E>> tree,
                                                                DeepNode<I, E> deepNode,
                                                                boolean upward) {
        if (upward) {
            deepNode.upwardDepth();
        } else {
            tree.find(n -> CollectionUtils.isEmpty(n.getChildren())).forEach(DeepNode::downwardDepth);
        }
    }
}
