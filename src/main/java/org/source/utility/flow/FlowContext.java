package org.source.utility.flow;

import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.source.utility.flow.node.AbstractNode;
import org.source.utility.flow.node.NodeTree;
import org.source.utility.flow.point.Branch;
import org.source.utility.flow.point.Point;
import org.source.utility.flow.processor.Processor;
import org.source.utility.utils.Streams;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zengfugen
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Data
public class FlowContext {
    private ThreadLocal<Map<ProcessorRef, Flow>> processorMap = ThreadLocal.withInitial(ConcurrentHashMap::new);
    private ThreadLocal<List<ProcessorRef>> processorRefList = ThreadLocal.withInitial(() -> Collections.synchronizedList(new ArrayList<>()));

    private static final String NAME_ROOT = "root";
    private static final String NAME_SEPARATOR = "_";

    private static final FlowContext INSTANCE = new FlowContext();

    public static FlowContext getInstance() {
        return INSTANCE;
    }

    public void put(Flow flow) {
        Objects.requireNonNull(flow.getName(), "流程名称（name）必填");
        if (null != flow.getParent()) {
            ProcessorInfo.refreshSerial();
        }
        this.saveToFlowMap(flow);
    }

    public void saveToFlowMap(@NonNull Flow flow) {
        ProcessorInfo processorInfo = ProcessorInfo.of(flow);
        Map<ProcessorRef, Flow> processorInfoMap = new ConcurrentHashMap<>(16);
        this.save(processorInfoMap, processorInfo);
        this.processorMap.get().putAll(processorInfoMap);
    }

    public void remove(@NonNull Flow flow) {
        List<ProcessorRef> refList = Streams.getterByKey(this.processorRefList.get(), ProcessorRef::getFlowName,
                flow.getName()).toList();
        this.processorRefList.get().removeAll(refList);
        refList.forEach(k -> this.processorMap.get().remove(k));
        if (this.processorRefList.get().isEmpty()) {
            this.processorRefList.remove();
        }
        if (this.processorMap.get().isEmpty()) {
            this.processorMap.remove();
        }
    }

    public void save(Map<ProcessorRef, Flow> processorInfoMap, ProcessorInfo processorInfo) {
        if (ProcessorTypeEnum.PROCESSOR.name().equals(processorInfo.getProcessorType())) {
            Flow flow = getFlowFromProcessorInfo(processorInfo);
            ProcessorRef processorRef = new ProcessorRef(processorInfo.getElement().getName(), flow.getName());
            processorInfoMap.put(processorRef, flow);
            this.processorRefList.get().add(processorRef);
            return;
        }
        Set<NodeTree<Processor>> children = processorInfo.getChildren();
        if (CollectionUtils.isEmpty(children)) {
            this.save(processorInfoMap, processorInfo);
        }
        for (NodeTree<Processor> nodeTree : children) {
            this.save(processorInfoMap, (ProcessorInfo) nodeTree);
        }
    }

    public <I, O> Flow<I, O> getFlow(Processor processor) {
        List<ProcessorRef> refList = Streams.getterByKey(this.processorRefList.get(), ProcessorRef::getName,
                processor.getName()).toList();
        ProcessorRef processorRef = refList.get(refList.size() - 1);
        return this.processorMap.get().get(processorRef);
    }

    public Flow getFlowFromProcessorInfo(ProcessorInfo processorInfo) {
        NodeTree<Processor> nodeTree = processorInfo;
        while (null != nodeTree.getParent()) {
            nodeTree = nodeTree.getParent();
            if (ProcessorTypeEnum.FLOW.name().equals(((ProcessorInfo) nodeTree).getProcessorType())) {
                return (Flow) nodeTree.getElement();
            }
        }
        return null;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ProcessorInfo extends NodeTree<Processor> {
        private static final AtomicInteger FLOW_SERIAL = new AtomicInteger(0);
        private static final AtomicInteger BRANCH_SERIAL = new AtomicInteger(0);
        private static final AtomicInteger POINT_SERIAL = new AtomicInteger(0);

        private String processorType;

        public ProcessorInfo(Processor processor) {
            this.element = processor;
            setType(this, processor);
        }

        public static void set(ProcessorInfo root, ProcessorInfo child) {
            if (null == root) {
                return;
            }
            if (null != child) {
                root.addChild(child);
                child.setParent(root);
            }
        }

        public static ProcessorInfo of(Processor processor) {
            if (null == processor) {
                return null;
            }
            ProcessorInfo root = new ProcessorInfo(processor);
            if (processor instanceof Flow flow) {
                AbstractNode node = flow.getRootNode().getNext();
                defaultName(node);
                set(root, of((Point) node));
                while (null != (node = node.getNext())) {
                    defaultName(node);
                    set(root, of((Point) node));
                }
                return root;
            } else if (processor instanceof Point point) {
                Map<?, Processor> map = point.getContainer().getKvMap();
                for (Map.Entry<?, Processor> entry : map.entrySet()) {
                    Processor value = entry.getValue();
                    if (value instanceof Point) {
                        set(root, of(value));
                    } else {
                        set(root, new ProcessorInfo(value));
                    }
                }
            }
            return root;
        }

        public static void setType(ProcessorInfo processorInfo, Processor processor) {
            if (processor instanceof Flow) {
                processorInfo.processorType = ProcessorTypeEnum.FLOW.name();
                return;
            }
            if (processor instanceof Branch) {
                processorInfo.processorType = ProcessorTypeEnum.BRANCH.name();
                return;
            }
            if (processor instanceof Point) {
                processorInfo.processorType = ProcessorTypeEnum.POINT.name();
                return;
            }
            processorInfo.processorType = ProcessorTypeEnum.PROCESSOR.name();
        }

        public static void defaultName(AbstractNode node) {
            if (node instanceof Flow && Flow.DEFAULT_NAME.equals(node.getName())) {
                node.setName(Flow.DEFAULT_NAME + NAME_SEPARATOR + FLOW_SERIAL.incrementAndGet());
                return;
            }
            if (node instanceof Branch && Branch.DEFAULT_NAME.equals(node.getName())) {
                node.setName(Branch.DEFAULT_NAME + NAME_SEPARATOR + BRANCH_SERIAL.incrementAndGet());
                return;
            }
            if (Point.DEFAULT_NAME.equals(node.getName())) {
                node.setName(Point.DEFAULT_NAME + NAME_SEPARATOR + POINT_SERIAL.incrementAndGet());
            }
        }

        public static void refreshSerial() {
            FLOW_SERIAL.set(0);
            BRANCH_SERIAL.set(0);
            POINT_SERIAL.set(0);
        }
    }

    public enum ProcessorTypeEnum {
        /**
         * 处理器
         */
        PROCESSOR,
        POINT,
        BRANCH,
        FLOW,
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessorRef {
        private String name;
        private String flowName;
    }

}
