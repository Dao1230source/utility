package org.source.utility.tree.define;

/**
 * 节点合并结果类型枚举
 * <p>
 * 定义节点合并时可能产生的结果类型。
 * </p>
 *
 * @author zengfugen
 */
public enum MergeResultTypeEnum {
    /**
     * 添加新节点
     * <p>
     * 添加的节点 ID 在树中不存在，直接创建新节点并添加。
     * </p>
     */
    ADD_NEW,

    /**
     * 合并节点
     * <p>
     * 添加的节点 ID 在树中已存在，使用合并处理器合并新旧节点。
     * </p>
     */
    ADD_MERGED,
}