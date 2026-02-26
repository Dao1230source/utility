package org.source.utility.tree.define;

/**
 * 当节点合同处理器 mergeHandler 返回null时如何处理
 */
public enum MergeReturnNullStrategyEnum {
    REMOVE_OLD,
    RETAIN_OLD,
    RETAIN_NEW,
    THROW_EXCEPTION
}