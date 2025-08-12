package org.source.utility.tree.define;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.utils.Jsons;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Slf4j
@Data
public abstract class AbstractNode<I, E extends Element<I>, N extends AbstractNode<I, E, N>> implements Node<I, E, N> {
    private E element;
    @JsonBackReference
    private N parent;
    @JsonManagedReference
    private List<N> children;

    public abstract N emptyNode();

    public void nodeHandler() {
    }

    public void addChild(N child) {
        if (Objects.isNull(this.children)) {
            this.children = new ArrayList<>(16);
        }
        this.children.add(child);
    }

    public void appendToParent(N parent) {
        this.parent = parent;
    }

    /**
     * 只比较element
     * <br>
     * 重写 equals 等方法避免自动生成的方法比较children等造成{@literal StackOverflowError}异常
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
    public String toString() {
        return Jsons.str(this.getElement());
    }
}