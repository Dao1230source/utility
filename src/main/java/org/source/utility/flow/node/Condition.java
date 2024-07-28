package org.source.utility.flow.node;

/**
 * 判断是否执行流程节点的条件
 * @author zengfugen
 */
public interface Condition {

    /**
     * 条件判断
     *
     * @return 结果
     */
    boolean test();

    /**
     * 取反
     *
     * @return 反面
     */
    default Condition negate() {
        return () -> !test();
    }
}
