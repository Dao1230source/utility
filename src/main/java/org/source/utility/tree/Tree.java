package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;
import org.source.utility.utils.Streams;
import org.springframework.lang.Nullable;
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
        return add(es, true, null, null, null);
    }

    /**
     * 新增元素
     *
     * @param es               es
     * @param updateOldHandler {@literal <new,old>}用新数据更新旧数据，为null时id重复不更新
     * @return node
     */
    public N add(Collection<? extends E> es,
                 boolean keepOldIndex,
                 @Nullable Consumer<N> afterCreateHandler,
                 @Nullable BinaryOperator<N> updateOldHandler,
                 @Nullable Consumer<N> finallyHandler) {
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
        }).forEach(n -> this.idMap.compute(n.getId(), (k, old) -> {
            // 默认使用新的
            N result = n;
            // 如有更新处理器
            if (Objects.nonNull(updateOldHandler)) {
                result = updateOldHandler.apply(n, old);
            }
            this.addChild(result, keepOldIndex);
            if (Objects.nonNull(finallyHandler)) {
                finallyHandler.accept(result);
            }
            return result;
        }));
        return root;
    }

    private void addChild(N n, boolean keepOldIndex) {
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
        parent.addChild(n, keepOldIndex);
        n.setParent(parent);
        this.elementHandler.accept(n);
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
