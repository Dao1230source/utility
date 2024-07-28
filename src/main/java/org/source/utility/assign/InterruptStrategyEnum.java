package org.source.utility.assign;

public enum InterruptStrategyEnum {
    /**
     * acquires 不论是否成功都不结束
     */
    NO,
    /**
     * acquires 任一失败都结束
     */
    ANY {
        @Override
        public boolean interrupt(InvokeStatusEnum invokeStatusEnum) {
            return InvokeStatusEnum.PARTIAL_FAIL.equals(invokeStatusEnum)
                    || InvokeStatusEnum.ALL_FAIL.equals(invokeStatusEnum);
        }
    },
    /**
     * acquires 全部失败才结束
     */
    ALL {
        @Override
        public boolean interrupt(InvokeStatusEnum invokeStatusEnum) {
            return InvokeStatusEnum.ALL_FAIL.equals(invokeStatusEnum);
        }
    };

    /**
     * 是否中断 assign 链路的执行
     * @param invokeStatusEnum This string may be used for further computation in overriding classes
     * @return boolean
     */
    public boolean interrupt(InvokeStatusEnum invokeStatusEnum) {
        return false;
    }
}
