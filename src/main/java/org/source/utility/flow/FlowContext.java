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
 * 流程上下文类
 * <p>
 * 管理 Flow 的执行上下文，包括流程注册、存储、查询和清理。
 * 使用 ThreadLocal 实现线程隔离，确保多线程环境下流程上下文的独立性。
 * </p>
 *
 * @author zengfugen
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Data
public class FlowContext {
    /**
     * 处理器到 Flow 的映射
     * <p>
     * Key：ProcessorRef（包含处理器名称和所属流程名称）
     * Value：对应的 Flow 对象
     * </p>
     */
    private ThreadLocal<Map<ProcessorRef, Flow>> processorMap = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * 处理器引用列表
     * <p>
     * 用于快速查询和清理，保存所有已注册的处理器的引用。
     * </p>
     */
    private ThreadLocal<List<ProcessorRef>> processorRefList = ThreadLocal.withInitial(() -> Collections.synchronizedList(new ArrayList<>()));

    /**
     * 根节点名称常量
     */
    private static final String NAME_ROOT = "root";

    /**
     * 名称分隔符常量
     */
    private static final String NAME_SEPARATOR = "_";

    /**
     * 单例实例
     */
    private static final FlowContext INSTANCE = new FlowContext();

    /**
     * 获取单例实例
     *
     * @return FlowContext 实例
     */
    public static FlowContext getInstance() {
        return INSTANCE;
    }

    /**
     * 注册流程
     * <p>
     * 将流程及其包含的所有处理器注册到上下文中。
     * </p>
     *
     * @param flow 要注册的流程
     * @throws NullPointerException 如果流程名称为 null
     */
    public void put(Flow flow) {
        Objects.requireNonNull(flow.getName(), "流程名称（name）必填");
        if (null != flow.getParent()) {
            ProcessorInfo.refreshSerial();
        }
        this.saveToFlowMap(flow);
    }

    /**
     * 保存流程到映射表
     * <p>
     * 递归保存流程中所有处理器到 processorMap。
     * </p>
     *
     * @param flow 要保存的流程
     */
    public void saveToFlowMap(@NonNull Flow flow) {
        ProcessorInfo processorInfo = ProcessorInfo.of(flow);
        Map<ProcessorRef, Flow> processorInfoMap = new ConcurrentHashMap<>(16);
        this.save(processorInfoMap, processorInfo);
        this.processorMap.get().putAll(processorInfoMap);
    }

    /**
     * 移除流程
     * <p>
     * 从上下文中移除指定流程及其所有处理器。
     * </p>
     *
     * @param flow 要移除的流程
     */
    public void remove(@NonNull Flow flow) {
        List<ProcessorRef> refList = Streams.retain(this.processorRefList.get(), ProcessorRef::getFlowName,
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

    /**
     * 递归保存处理器信息
     *
     * @param processorInfoMap 处理器映射表
     * @param processorInfo   处理器信息
     */
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

    /**
     * 根据处理器获取对应的 Flow
     *
     * @param <I>       输入类型
     * @param <O>       输出类型
     * @param processor 处理器
     * @return 对应的 Flow 对象
     */
    public <I, O> Flow<I, O> getFlow(Processor processor) {
        List<ProcessorRef> refList = Streams.retain(this.processorRefList.get(), ProcessorRef::getName,
                processor.getName()).toList();
        ProcessorRef processorRef = refList.get(refList.size() - 1);
        return this.processorMap.get().get(processorRef);
    }

    /**
     * 从处理器信息中获取 Flow 对象
     *
     * @param processorInfo 处理器信息
     * @return Flow 对象，未找到返回 null
     */
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

    /**
     * 处理器信息类
     * <p>
     * 继承自 NodeTree，用于构建流程的树形结构。
     * </p>
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ProcessorInfo extends NodeTree<Processor> {
        private static final AtomicInteger FLOW_SERIAL = new AtomicInteger(0);

        /**
         * Branch 序列号计数器
         */
        private static final AtomicInteger BRANCH_SERIAL = new AtomicInteger(0);

        /**
         * Point 序列号计数器
         */
        private static final AtomicInteger POINT_SERIAL = new AtomicInteger(0);

        /**
         * 处理器类型
         */
        private String processorType;

        /**
         * 构造函数
         *
         * @param processor 处理器
         */
        public ProcessorInfo(Processor processor) {
            this.element = processor;
            setType(this, processor);
        }

        /**
         * 设置父子关系
         *
         * @param root 根处理器
         * @param child 子处理器
         */
        public static void set(ProcessorInfo root, ProcessorInfo child) {
            if (null == root) {
                return;
            }
            if (null != child) {
                root.addChild(child);
                child.setParent(root);
            }
        }

        /**
         * 创建处理器信息
         *
         * @param processor 处理器
         * @return 处理器信息树
         */
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

        /**
         * 设置处理器类型
         *
         * @param processorInfo 处理器信息
         * @param processor     处理器
         */
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

        /**
         * 设置默认名称
         * <p>
         * 如果节点使用默认名称，自动生成带序列号的名称。
         * </p>
         *
         * @param node 节点
         */
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

        /**
         * 重置序列号
         */
        public static void refreshSerial() {
            FLOW_SERIAL.set(0);
            BRANCH_SERIAL.set(0);
            POINT_SERIAL.set(0);
        }
    }

    /**
     * 处理器类型枚举
     */
    public enum ProcessorTypeEnum {
        /**
         * 处理器
         */
        PROCESSOR,

        /**
         * 单节点
         */
        POINT,

        /**
         * 分支节点
         */
        BRANCH,

        /**
         * 流程
         */
        FLOW,
    }

    /**
     * 处理器引用类
     * <p>
     * 用于在 processorMap 中作为 Key，包含处理器名称和所属流程名称。
     * </p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessorRef {
        /**
         * 处理器名称
         */
        private String name;

        /**
         * 所属流程名称
         */
        private String flowName;
    }

}
