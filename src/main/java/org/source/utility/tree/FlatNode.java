package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.utility.function.SFunction;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.source.utility.utils.Lambdas;
import org.source.utility.utils.Streams;

import java.util.LinkedHashMap;
import java.util.List;

@JsonIgnoreProperties({"element", "properties"})
@EqualsAndHashCode(callSuper = true)
@Data
public class FlatNode<I, E extends Element<I>> extends AbstractNode<I, E, FlatNode<I, E>> {
    @JsonIgnore
    private final List<SFunction<E, Object>> propertyGetters;

    @JsonAnyGetter
    private transient LinkedHashMap<String, Object> properties;

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O emptyNode() {
        return (O) FlatNode.newInstance(this.propertyGetters);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> emptyTree() {
        return (Tree<J, F, O>) FlatNode.buildTree(this.propertyGetters);
    }

    public static <I, E extends Element<I>> FlatNode<I, E> newInstance(List<SFunction<E, Object>> propertyGetters) {
        return new FlatNode<>(propertyGetters);
    }

    public static <I, E extends Element<I>> void nodeHandler(FlatNode<I, E> node) {
        node.setProperties(LinkedHashMap.newLinkedHashMap(node.propertyGetters.size()));
        Streams.of(node.propertyGetters).forEach(k -> node.getProperties().put(Lambdas.getFieldName(k), k.apply(node.getElement())));
    }


    public static <I, E extends Element<I>> Tree<I, E, FlatNode<I, E>> buildTree(List<SFunction<E, Object>> propertyGetters) {
        return new Tree<>(() -> FlatNode.newInstance(propertyGetters), FlatNode::nodeHandler);
    }

}
