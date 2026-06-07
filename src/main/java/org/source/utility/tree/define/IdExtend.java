package org.source.utility.tree.define;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * ID 扩展配置记录类
 * <p>
 * 用于配置树节点中 ID 和父节点 ID 的获取方式。
 * 支持自定义 ID 字段名称和获取逻辑，使树结构可以基于不同的字段来建立父子关系。
 * </p>
 *
 * @param <I>   ID 类型，必须实现 Comparable 接口
 * @param <E>   元素类型，必须实现 Element 接口
 * @param <N>   节点类型，必须继承 AbstractNode
 * @param name  ID 字段名称，用于标识不同的 ID 配置
 * @param idGetter ID 获取函数，从节点中提取 ID
 * @param parentIdGetter 父节点 ID 获取函数，从节点中提取父节点 ID
 * @author zengfugen
 */
public record IdExtend<I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>>(String name,
                                                                                                       Function<N, @Nullable I> idGetter,
                                                                                                       Function<N, @Nullable I> parentIdGetter) {
    /**
     * 默认 ID 配置名称
     */
    public static final String ID_NAME = "id";

    /**
     * 判断是否为默认 ID 配置
     *
     * @return 如果 name 为 "id" 返回 true，否则返回 false
     */
    public boolean isDefault() {
        return ID_NAME.equals(name);
    }

    /**
     * 创建默认的 ID 扩展配置
     * <p>
     * 使用 Node::getId 和 Node::getParentId 作为默认的获取函数。
     * </p>
     *
     * @param <I> ID 类型
     * @param <E> 元素类型
     * @param <N> 节点类型
     * @return 默认的 IdExtend 实例
     */
    public static <I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> IdExtend<I, E, N> defaultIdExtend() {
        Function<N, @Nullable I> idGetter = N::getId;
        Function<N, @Nullable I> parentIdGetter = N::getParentId;
        return new IdExtend<>(IdExtend.ID_NAME, idGetter, parentIdGetter);
    }
}
