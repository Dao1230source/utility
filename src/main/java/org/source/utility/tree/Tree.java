package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.source.utility.tree.identity.StringElement;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

@Getter
public class Tree<I, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    @JsonIgnore
    private final Map<I, N> idMap = new ConcurrentHashMap<>(32);
    @JsonIgnore
    private final Supplier<N> newInstance;
    @JsonIgnore
    private final Consumer<N> elementHandler;
    private final N root;

    public Tree(Supplier<N> newInstance, Consumer<N> elementHandler) {
        this.newInstance = newInstance;
        this.elementHandler = elementHandler;
        this.root = newInstance.get();
    }

    public N add(Collection<? extends E> es) {
        if (CollectionUtils.isEmpty(es)) {
            return root;
        }
        List<N> nodes = new TreeSet<>(es).stream().map(e -> {
            N n = this.newInstance.get();
            n.setElement(e);
            return n;
        }).toList();
        List<N> absentNodes = nodes.stream().filter(n -> !this.idMap.containsKey(n.getId())).toList();
        this.idMap.putAll(Streams.toMap(absentNodes, AbstractNode::getId, Function.identity()));
        nodes.forEach(n -> {
            I parentId = n.getParentId();
            N parent;
            if (Objects.isNull(parentId)) {
                parent = root;
            } else {
                parent = this.idMap.get(parentId);
                if (Objects.isNull(parent)) {
                    parent = root;
                }
            }
            parent.addChild(n);
            n.setParent(parent);
            this.elementHandler.accept(n);
        });
        return root;
    }

    public List<N> find(Predicate<N> predicate) {
        return Streams.of(this.idMap).map(Map::values).flatMap(Collection::stream).filter(predicate).toList();
    }

    public Optional<N> get(Predicate<N> predicate) {
        return Streams.of(this.idMap).map(Map::values).flatMap(Collection::stream).filter(predicate).findFirst();
    }

    public void remove(Predicate<N> predicate) {
        List<N> ns = find(predicate);
        ns.forEach(n -> {
            N parent = n.getParent();
            if (Objects.nonNull(parent)) {
                parent.getChildren().remove(n);
            }
        });
        ns.forEach(n -> this.idMap.remove(n.getId()));
    }

    public int size() {
        return this.idMap.size();
    }

    public void forEach(BiConsumer<I, N> biConsumer) {
        this.idMap.forEach(biConsumer);
    }

    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> cast(Function<E, F> mapper) {
        Tree<J, F, O> newTree = this.root.emptyTree();
        Map<J, O> targetIdMap = new ConcurrentHashMap<>(idMap.size());
        O newRoot = AbstractNode.cast(this.root, mapper, targetIdMap);
        newTree.idMap.putAll(targetIdMap);
        newTree.root.setElement(newRoot.getElement());
        newTree.root.setChildren(newRoot.getChildren());
        return newTree;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @Data
    static class DemoElement extends StringElement {
        private String id;
        private String parentId;
        private String name;
    }
}
