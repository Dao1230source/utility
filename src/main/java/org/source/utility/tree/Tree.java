package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.source.utility.utils.Streams;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

@Data
public class Tree<I, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    @JsonIgnore
    private final Map<I, N> idMap = new ConcurrentHashMap<>(32);
    @JsonIgnore
    private final Supplier<N> newInstance;
    @JsonIgnore
    private final Consumer<N> elementHandler;
    private final N root;

    private boolean keepOldIndex = false;
    private @Nullable Consumer<N> afterCreateHandler;
    private @Nullable BinaryOperator<N> mergeHandler;
    private @Nullable BiConsumer<N, N> finallyHandler;
    private Function<N, I> idGetter = N::getId;
    private Function<N, I> parentIdGetter = N::getParentId;

    public Tree(Supplier<N> newInstance, Consumer<N> elementHandler) {
        this.newInstance = newInstance;
        this.elementHandler = elementHandler;
        this.root = newInstance.get();
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
        es.stream().map(e -> {
            N n = this.newInstance.get();
            n.setElement(e);
            if (Objects.nonNull(afterCreateHandler)) {
                afterCreateHandler.accept(n);
            }
            return n;
        }).forEach(n -> this.idMap.compute(idGetter.apply(n), (k, old) -> {
            // 默认使用新的
            N result = n;
            // 如有更新处理器
            if (Objects.nonNull(mergeHandler)) {
                result = mergeHandler.apply(n, old);
            }
            N parent = this.addChild(result, keepOldIndex);
            if (Objects.nonNull(finallyHandler)) {
                finallyHandler.accept(result, parent);
            }
            return result;
        }));
        return root;
    }

    private N addChild(N n, boolean keepOldIndex) {
        I parentId = parentIdGetter.apply(n);
        N parent;
        if (Objects.isNull(parentId)) {
            parent = root;
        } else {
            parent = this.idMap.get(parentId);
            if (Objects.isNull(parent)) {
                parent = root;
            }
        }
        parent.addChild(n, keepOldIndex);
        n.setParent(parent);
        this.elementHandler.accept(n);
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

    /**
     * 新建一个tree，保留和源tree一样的结构，并将节点负载的元素从E类型的转换为F类型
     *
     * @param mapper         转换函数
     * @param parentIdSetter 父ID设置函数
     * @param <J>            J对应I
     * @param <F>            F对应E
     * @param <O>            O对应N
     * @return {@literal Tree<J, F, O>}
     */
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> cast(Function<E, F> mapper,
                                                                                         BiConsumer<F, J> parentIdSetter,
                                                                                         @Nullable BiConsumer<O, N> afterCreateHandler) {
        Tree<J, F, O> newTree = this.root.emptyTree();
        Map<J, O> targetIdMap = new ConcurrentHashMap<>(idMap.size());
        O newRoot = AbstractNode.cast(this.root, mapper, parentIdSetter, targetIdMap, afterCreateHandler);
        newTree.idMap.putAll(targetIdMap);
        newTree.root.setElement(newRoot.getElement());
        newTree.root.setChildren(newRoot.getChildren());
        return newTree;
    }
}