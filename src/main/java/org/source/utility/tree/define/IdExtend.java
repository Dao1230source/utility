package org.source.utility.tree.define;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.Function;

@AllArgsConstructor
@Data
public class IdExtend<I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    public static final String ID_NAME = "id";
    /**
     * 名称
     */
    private String name;
    /**
     * ID 获取函数
     * 从元素中提取 ID，默认调用 element.getId()
     */
    private Function<N, I> idGetter;

    /**
     * 父节点 ID 获取函数
     * 从元素中提取父节点 ID，默认调用 element.getParentId()
     */
    private Function<N, I> parentIdGetter;

    public boolean isDefault() {
        return ID_NAME.equals(name);
    }

    public static <I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> IdExtend<I, E, N> defaultIdExtend() {
        return new IdExtend<>(IdExtend.ID_NAME, N::getId, N::getParentId);
    }
}
