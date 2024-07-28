package org.source.utility.flow;

import lombok.extern.slf4j.Slf4j;
import org.source.utility.flow.point.Branch;
import org.source.utility.flow.point.Container;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.source.utility.flow.point.Point;
import org.source.utility.flow.processor.Processor;
import org.source.utility.flow.node.*;

import java.util.Arrays;

/**
 * 流程
 * <pre>
 * 该package的设计是为了将需要多段多情况处理的任务模块化分隔，自由组装流程
 * 基本概念：
 * 1、节点{@link AbstractNode} 共有两个实现，单节点{@link Point}、分支节点{@link Branch}
 * 2、处理器{@link Processor} 在接口的实现类中处理业务逻辑
 * 3、容器{@link Container} 容器通常包含一个或多个选择，可以是单节点或处理器
 * 4、条件{@link Condition} 一系列的条件决定节点是否执行
 * 5、容器类唯一标识{@link Key}
 * 6、容器类对象{@link Value}
 * 7、选择器{@link Selector} 选择容器中的key值，目前只支持单选
 * 其他说明：
 * 1、分支节点包含的是单节点
 * 2、单节点中包含处理器
 * 3、处理器返回的结果可决定流程是否往下执行
 * </pre>
 * @author zengfugen
 */
@EqualsAndHashCode(callSuper = false)
@Slf4j
@Data
public class Flow<I, O> extends Point<String, String> {
    public static final String DEFAULT_NAME = "Flow";
    private Flow<?, ?> parent;

    private I input;
    private O output;

    @Override
    public String getName() {
        if (StringUtils.isBlank(super.getName()) || Point.DEFAULT_NAME.equals(super.getName())) {
            this.setName(DEFAULT_NAME);
        }
        return super.getName();
    }

    private Flow() {
    }

    public static <I, O> Flow<I, O> builder() {
        return new Flow<>();
    }

    public Flow<I, O> input(I i) {
        this.setInput(i);
        return this;
    }

    public Flow<I, O> output(O o) {
        this.setOutput(o);
        return this;
    }

    @SuppressWarnings({"rawtypes"})
    public Flow root() {
        Flow<?, ?> root = this;
        while (null != root.getParent()) {
            root = root.getParent();
        }
        return root;
    }

    public Flow<I, O> name(String name) {
        this.setName(name);
        return this;
    }

    public Flow<I, O> parent(Flow<?, ?> parent) {
        this.setParent(parent);
        return this;
    }

    private final AbstractNode rootNode = new AbstractNode() {
        @Override
        public boolean execute() {
            return true;
        }
    };

    @Override
    public void add(AbstractNode abstractNode) {
        this.rootNode.add(abstractNode);
    }

    /**
     * 真正执行流程逻辑处理
     */
    @Override
    public void invoke() {
        log.debug("Flow.name:{}", name);
        FlowUtil.put(this);
        this.rootNode.invoke();
        FlowUtil.remove(this);
    }

    @SuppressWarnings("rawtypes")
    public Flow<I, O> assemble(Point... point) {
        Arrays.stream(point).forEach(this::add);
        return this;
    }

}
