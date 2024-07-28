package org.source.utility.tree;

import org.source.utility.tree.identity.Element;
import org.source.utility.function.SFunction;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class FlatTree<I, E extends Element<I>> extends Tree<I, E, FlatNode<I, E>> {

    public FlatTree(Supplier<FlatNode<I, E>> newInstance, BiConsumer<FlatNode<I, E>, E> elementHandler) {
        super(newInstance, elementHandler);
    }

    public static <I, E extends Element<I>> Tree<I, E, FlatNode<I, E>> buildFlat(List<SFunction<E, Objects>> propertyGetters) {
        BiConsumer<FlatNode<I, E>, E> nElementBiConsumer = NodeEnum.FLAT.elementHandler(propertyGetters);
        return new FlatTree<>(NodeEnum.FLAT::newInstance, nElementBiConsumer);
    }
}
