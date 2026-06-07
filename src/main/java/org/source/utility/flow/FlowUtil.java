package org.source.utility.flow;

import org.source.utility.flow.processor.Processor;

import java.util.Objects;

/**
 * 流程工具类
 * <p>
 * 提供流程操作的静态方法，作为 FlowContext 的便捷访问层。
 * </p>
 *
 * @author zengfugen
 */
public class FlowUtil {

    /**
     * 流程上下文实例
     */
    private static final FlowContext FLOW_CONTEXT;

    static {
        FLOW_CONTEXT = FlowContext.getInstance();
    }

    private FlowUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 注册流程
     * <p>
     * 将流程注册到上下文中。
     * </p>
     *
     * @param <I>  输入类型
     * @param <O>  输出类型
     * @param flow 要注册的流程
     */
    public static <I, O> void put(Flow<I, O> flow) {
        FLOW_CONTEXT.put(flow);
    }

    /**
     * 移除流程
     * <p>
     * 从上下文中移除指定流程。
     * </p>
     *
     * @param <I>  输入类型
     * @param <O>  输出类型
     * @param flow 要移除的流程
     */
    public static <I, O> void remove(Flow<I, O> flow) {
        FLOW_CONTEXT.remove(flow);
    }

    /**
     * 根据处理器获取对应的 Flow
     *
     * @param <I>       输入类型
     * @param <O>       输出类型
     * @param processor 处理器
     * @return Flow 对象
     * @throws NullPointerException 如果未找到对应的 Flow
     */
    @SuppressWarnings("rawtypes")
    public static <I, O> Flow<I, O> getFlow(Processor processor) {
        Flow<I, O> flow = FLOW_CONTEXT.getFlow(processor);
        Objects.requireNonNull(flow, String.format("流程对象未获取，processor：%s", processor.getClass().getSimpleName()));
        return flow;
    }

}
