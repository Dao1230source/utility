package org.source.utility.tree;

import org.source.utility.tree.identity.Element;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DefaultTree<I, E extends Element<I>> extends Tree<I, E, DefaultNode<I, E>> {
    public DefaultTree(Supplier<DefaultNode<I, E>> newInstance, BiConsumer<DefaultNode<I, E>, E> elementHandler) {
        super(newInstance, elementHandler);
    }

    public static <I, E extends Element<I>> DefaultTree<I, E> build() {
        BiConsumer<DefaultNode<I, E>, E> nElementBiConsumer = NodeEnum.DEFAULT.elementHandlerDefault();
        return new DefaultTree<>(NodeEnum.DEFAULT::newInstance, nElementBiConsumer);
    }
}
