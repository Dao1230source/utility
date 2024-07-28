package org.source.utility.tree;

import lombok.Getter;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Tree<I, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    private final Map<I, N> idMap = HashMap.newHashMap(32);
    private final Supplier<N> newInstance;
    private final BiConsumer<N, E> elementHandler;
    @Getter
    protected final N root;

    public Tree(Supplier<N> newInstance, BiConsumer<N, E> elementHandler) {
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
            this.elementHandler.accept(n, e);
            return n;
        }).toList();
        List<N> absentNodes = nodes.stream().filter(n -> !this.idMap.containsKey(n.getId())).toList();
        this.idMap.putAll(Streams.toMap(absentNodes, AbstractNode::getId, Function.identity()));
        nodes.forEach(k -> {
            I parentId = k.getParentId();
            N parent;
            if (Objects.isNull(parentId)) {
                parent = root;
            } else {
                parent = this.idMap.get(parentId);
                if (Objects.isNull(parent)) {
                    parent = root;
                }
            }
            parent.addChild(k);
            k.setParent(parent);
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

}
