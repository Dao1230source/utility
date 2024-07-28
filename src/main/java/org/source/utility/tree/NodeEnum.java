package org.source.utility.tree;

import org.source.utility.function.SFunction;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.source.utility.utils.Lambdas;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public enum NodeEnum {
    /**
     * default
     */
    DEFAULT {
        @SuppressWarnings("unchecked")
        @Override
        public <I, E extends Element<I>, N extends AbstractNode<I, E, N>> N newInstance() {
            return (N) new DefaultNode<I, E>();
        }
    },
    @SuppressWarnings("unchecked")
    FLAT {
        @Override
        public <I, E extends Element<I>, N extends AbstractNode<I, E, N>> N newInstance() {
            return (N) new FlatNode<I, E>();
        }

        @Override
        public <I, E extends Element<I>, N extends AbstractNode<I, E, N>> BiConsumer<N, E> elementHandler(List<SFunction<E, Objects>> propertyGetters) {
            return (n, e) -> {
                if (n instanceof FlatNode<?, ?> node && !CollectionUtils.isEmpty(propertyGetters)) {
                    node.setProperties(LinkedHashMap.newLinkedHashMap(propertyGetters.size()));
                    Streams.of(propertyGetters).forEach(k -> node.getProperties().put(Lambdas.getFieldName(k), k.apply(e)));
                }
            };
        }
    },
    @SuppressWarnings("unchecked")
    DEEP {
        @Override
        public <I, E extends Element<I>, N extends AbstractNode<I, E, N>> N newInstance() {
            return (N) new DeepNode<I, E>();
        }
    };

    public abstract <I, E extends Element<I>, N extends AbstractNode<I, E, N>> N newInstance();

    /**
     * @param propertyGetters this params maybe used for future compution in overriding classes
     * @param <I>             I
     * @param <E>             E
     * @param <N>             N
     * @return {@literal BiConsumer<N, E>}
     */
    public <I, E extends Element<I>, N extends AbstractNode<I, E, N>> BiConsumer<N, E> elementHandler(List<SFunction<E, Objects>> propertyGetters) {
        return elementHandlerDefault();
    }

    public <I, E extends Element<I>, N extends AbstractNode<I, E, N>> BiConsumer<N, E> elementHandlerDefault() {
        return (n, e) -> {
        };
    }
}
