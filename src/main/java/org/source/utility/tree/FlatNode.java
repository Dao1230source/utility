package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;

import java.util.LinkedHashMap;
import java.util.Objects;

@JsonIgnoreProperties({"element", "properties"})
@EqualsAndHashCode(callSuper = true)
@Data
public class FlatNode<I, E extends Element<I>> extends AbstractNode<I, E, FlatNode<I, E>> {

    @JsonAnyGetter
    private transient LinkedHashMap<String, Objects> properties;
}
