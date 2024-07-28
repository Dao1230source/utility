package org.source.utility.flow;

import org.source.utility.flow.processor.Processor;

import java.util.Objects;

/**
 * @author zengfugen
 */
public class FlowUtil {

    private static final FlowContext FLOW_CONTEXT;

    static {
        FLOW_CONTEXT = FlowContext.getInstance();
    }

    private FlowUtil() {
        throw new UnsupportedOperationException();
    }

    public static <I, O> void put(Flow<I, O> flow) {
        FLOW_CONTEXT.put(flow);
    }

    public static <I, O> void remove(Flow<I, O> flow) {
        FLOW_CONTEXT.remove(flow);
    }

    @SuppressWarnings("rawtypes")
    public static <I, O> Flow<I, O> getFlow(Processor processor) {
        Flow<I, O> flow = FLOW_CONTEXT.getFlow(processor);
        Objects.requireNonNull(flow, String.format("流程对象未获取，processor：%s", processor.getClass().getSimpleName()));
        return flow;
    }

}
