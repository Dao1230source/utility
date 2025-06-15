package org.source.utility.tree.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.Tree;
import org.source.utility.utils.Jsons;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Data
public abstract class AbstractNode<I, E extends Element<I>, N extends AbstractNode<I, E, N>> implements Node<I, E, N> {
    private E element;
    private N parent;
    @JsonIgnore
    private transient List<N> children;

    public void addChild(N child) {
        this.addChild(child, true);
    }

    public void addChild(N child, boolean keepOldIndex) {
        if (Objects.isNull(this.children)) {
            this.children = new ArrayList<>(16);
        }
        if (!keepOldIndex) {
            this.children.add(child);
            return;
        }
        int i = this.children.indexOf(child);
        if (i < 0) {
            this.children.add(child);
            return;
        }
        this.children.remove(i);
        this.children.add(i, child);
    }

    public I getId() {
        return Node.getProperty(this, Element::getId);
    }

    public I getParentId() {
        return Node.getProperty(this, Element::getParentId);
    }

    public abstract <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O emptyNode();

    public abstract <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> emptyTree();

    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O cast(Function<E, F> mapper) {
        O newNode = this.emptyNode();
        E ele = this.getElement();
        if (Objects.nonNull(ele)) {
            F newEle = mapper.apply(this.getElement());
            newNode.setElement(newEle);
        }
        return newNode;
    }

    /**
     * 转换为另一个node
     *
     * @param node           source
     * @param mapper         convert
     * @param parentIdSetter set parentId
     * @param targetIdMap    目标node的{@literal Map<id,node>}
     * @param <I>            I
     * @param <E>            E
     * @param <N>            N
     * @param <J>            J对应I
     * @param <F>            F对应E
     * @param <O>            O对应N
     * @return O
     */
    public static <I, E extends Element<I>, N extends AbstractNode<I, E, N>,
            J, F extends Element<J>, O extends AbstractNode<J, F, O>> O cast(N node,
                                                                             Function<E, F> mapper,
                                                                             BiConsumer<F, J> parentIdSetter,
                                                                             @Nullable Map<J, O> targetIdMap,
                                                                             @Nullable BiConsumer<O, N> afterCreateHandler) {
        O newNode = node.cast(mapper);
        if (Objects.nonNull(afterCreateHandler)) {
            afterCreateHandler.accept(newNode, node);
        }
        if (Objects.nonNull(targetIdMap) && Objects.nonNull(newNode.getId())) {
            targetIdMap.put(newNode.getId(), newNode);
        }
        if (!CollectionUtils.isEmpty(node.getChildren())) {
            newNode.setChildren(node.getChildren().stream().map(n -> {
                O newChildNode = AbstractNode.cast(n, mapper, parentIdSetter, targetIdMap, afterCreateHandler);
                newChildNode.setParent(newNode);
                if (Objects.nonNull(newChildNode.getElement())) {
                    parentIdSetter.accept(newChildNode.getElement(), newNode.getId());
                }
                return newChildNode;
            }).toList());
        }
        return newNode;
    }

    public static <I, E extends Element<I>, N extends AbstractNode<I, E, N>,
            J, F extends Element<J>, O extends AbstractNode<J, F, O>> O cast(N node,
                                                                             Function<E, F> mapper,
                                                                             BiConsumer<F, J> parentIdSetter) {
        return cast(node, mapper, parentIdSetter, null, null);
    }

    /**
     * 只比较element
     *
     * @param o other
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractNode<?, ?, ?> that = (AbstractNode<?, ?, ?>) o;
        return Objects.equals(getElement(), that.getElement());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getElement());
    }

    @Override
    public int compareTo(@NotNull N n) {
        return this.getElement().compareTo(n.getElement());
    }

    @Override
    public String toString() {
        return Jsons.str(this);
    }
}
