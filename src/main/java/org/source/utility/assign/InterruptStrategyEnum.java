package org.source.utility.assign;

/**
 * 中断策略枚举
 * <p>
 * 定义在 Assign 执行过程中，当 Acquire 失败时是否中断后续操作的策略。
 * </p>
 *
 * @author zengfugen
 */
public enum InterruptStrategyEnum {
    /**
     * 不中断
     * <p>
     * 无论 Acquire 是否成功，都继续执行后续操作。
     * </p>
     */
    NO,

    /**
     * 任意失败即中断
     * <p>
     * 当任一 Acquire 失败（全部失败或部分失败）时，中断后续操作。
     * </p>
     */
    ANY {
        @Override
        public boolean interrupt(InvokeStatusEnum invokeStatusEnum) {
            return InvokeStatusEnum.PARTIAL_FAIL.equals(invokeStatusEnum)
                    || InvokeStatusEnum.ALL_FAIL.equals(invokeStatusEnum);
        }
    },

    /**
     * 全部失败才中断
     * <p>
     * 只有当所有 Acquire 都失败时，才中断后续操作。
     * </p>
     */
    ALL {
        @Override
        public boolean interrupt(InvokeStatusEnum invokeStatusEnum) {
            return InvokeStatusEnum.ALL_FAIL.equals(invokeStatusEnum);
        }
    };

    /**
     * 判断是否中断 assign 链路的执行
     *
     * @param invokeStatusEnum 执行状态
     * @return true 表示需要中断，false 表示继续执行
     */
    public boolean interrupt(InvokeStatusEnum invokeStatusEnum) {
        return false;
    }
}
