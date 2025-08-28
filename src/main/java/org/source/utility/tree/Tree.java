package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

@JsonIgnoreProperties(value = {"sourceElements", "idMap", "afterCreateHandler", "mergeHandler",
        "afterAddHandler", "idGetter", "parentIdGetter"})
@Data
public class Tree<I, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    private final List<E> sourceElements = new ArrayList<>(32);
    private final Map<I, N> idMap = new ConcurrentHashMap<>(32);
    @NonNull
    private final N root;

    private @Nullable Consumer<N> afterCreateHandler;
    private @Nullable BinaryOperator<N> mergeHandler;
    private @Nullable BiConsumer<N, N> afterAddHandler;
    private Function<N, I> idGetter = N::getId;
    private Function<N, I> parentIdGetter = N::getParentId;

    protected Tree(@NotNull N root) {
        this.root = root;
    }

    /**
     * new instance with root
     *
     * @param root empty node
     * @param <I>  I
     * @param <E>  E
     * @param <N>  N
     * @return tree
     */
    public static <I, E extends Element<I>, N extends AbstractNode<I, E, N>> Tree<I, E, N> of(N root) {
        return new Tree<>(root);
    }

    /**
     * 新增元素
     *
     * @param es es
     * @return node
     */
    public N add(Collection<? extends E> es) {
        if (CollectionUtils.isEmpty(es)) {
            return root;
        }
        List<N> toAddNodes = Streams.map(es, e -> {
            this.getSourceElements().add(e);
            N n = this.getRoot().emptyNode();
            n.setElement(e);
            if (Objects.nonNull(this.getAfterCreateHandler())) {
                this.getAfterCreateHandler().accept(n);
            }
            return n;
        }).toList();
        // 先将所有元素缓存，避免有可能父级数据在后，当前元素加入时找不到父级的情况
        Map<I, N> cachedNodeMap = Streams.toMap(toAddNodes, AbstractNode::getId, n ->
                this.getIdMap().compute(this.getIdGetter().apply(n), (k, old) -> {
                    // 新旧数据合并处理
                    if (Objects.nonNull(this.getMergeHandler())) {
                        return this.getMergeHandler().apply(n, old);
                    }
                    return n;
                }));
        toAddNodes.forEach(n -> {
            N node = cachedNodeMap.get(n.getId());
            I parentId = this.getParentIdGetter().apply(n);
            N parent = this.getParent(parentId);
            parent.addChild(node);
            node.appendToParent(parent);
            node.nodeHandler();
            if (Objects.nonNull(this.getAfterAddHandler())) {
                this.getAfterAddHandler().accept(node, parent);
            }
        });
        return root;
    }

    private N getParent(I parentId) {
        N parent;
        if (Objects.isNull(parentId)) {
            parent = root;
        } else {
            parent = this.idMap.get(parentId);
            if (Objects.isNull(parent)) {
                parent = root;
            }
        }
        return parent;
    }

    public List<N> find(Predicate<N> predicate) {
        return Streams.of(this.idMap).map(Map::values).flatMap(Collection::stream).filter(predicate).toList();
    }

    public Optional<N> get(Predicate<N> predicate) {
        return Streams.of(this.idMap).map(Map::values).flatMap(Collection::stream).filter(predicate).findFirst();
    }

    public N getById(I id) {
        return this.idMap.get(id);
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

    public <I2, E2 extends Element<I2>, N2 extends AbstractNode<I2, E2, N2>, T2 extends Tree<I2, E2, N2>> T2 cast(
            Supplier<T2> emptyTreeSupplier, Function<E, E2> eleMapper) {
        T2 emptyTree2 = emptyTreeSupplier.get();
        List<E2> list = Streams.of(this.getSourceElements()).map(eleMapper).toList();
        emptyTree2.add(list);
        return emptyTree2;
    }

    public void clear() {
        this.idMap.clear();
        this.sourceElements.clear();
        this.root.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tree<?, ?, ?> tree = (Tree<?, ?, ?>) o;
        return Objects.equals(getRoot(), tree.getRoot());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getRoot());
    }

    @Override
    public String toString() {
        return Jsons.str(this.getRoot());
    }
}