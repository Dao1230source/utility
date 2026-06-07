package org.source.utility.tree.define;

import org.jspecify.annotations.Nullable;

/**
 * 节点合并结果记录类
 * <p>
 * 记录节点合并过程中的详细信息，包括新旧节点、合并后的节点以及合并类型。
 * 用于追踪节点合并的历史和结果。
 * </p>
 *
 * @param <I>  ID 类型
 * @param <E>  元素类型
 * @param <N>  节点类型
 * @param newNode    新节点
 * @param oldNode    旧节点，可能为 null
 * @param resultNode 合并后的结果节点
 * @param resultType 合并结果类型
 * @author zengfugen
 */
public record MergeNodeResult<I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>>(N newNode,
                                                                                                              @Nullable N oldNode,
                                                                                                              N resultNode,
                                                                                                              MergeResultTypeEnum resultType) {
}