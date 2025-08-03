package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.utility.function.SFunction;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;
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
    private LinkedHashMap<String, Object> properties;

    public FlatNode(List<SFunction<E, Object>> propertyGetters) {
        this.propertyGetters = propertyGetters;
    }

    @Override
    public FlatNode<I, E> emptyNode() {
        return new FlatNode<>(this.propertyGetters);
    }

    @Override
    public void nodeHandler() {
        this.setProperties(LinkedHashMap.newLinkedHashMap(this.propertyGetters.size()));
        Streams.of(this.propertyGetters).forEach(k -> this.getProperties().put(Lambdas.getFieldName(k), k.apply(this.getElement())));
    }
}