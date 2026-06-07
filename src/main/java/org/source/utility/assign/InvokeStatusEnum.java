package org.source.utility.assign;

/**
 * 执行状态枚举
 * <p>
 * 定义 Assign 执行过程中 Acquire 的执行状态。
 * </p>
 *
 * @author zengfugen
 */
public enum InvokeStatusEnum {
    /**
     * 已创建
     * <p>
     * Assign 对象刚创建，尚未执行 invoke() 方法。
     * </p>
     */
    CREATED {
        @Override
        public boolean invoked() {
            return false;
        }
    },

    /**
     * 全部成功
     * <p>
     * 所有 Acquire 都执行成功。
     * </p>
     */
    ALL_SUCCESS,

    /**
     * 部分失败
     * <p>
     * 部分 Acquire 执行失败，但至少有一个成功。
     * </p>
     */
    PARTIAL_FAIL,

    /**
     * 全部失败
     * <p>
     * 所有 Acquire 都执行失败。
     * </p>
     */
    ALL_FAIL;

    public boolean invoked() {
        return true;
    }
}
