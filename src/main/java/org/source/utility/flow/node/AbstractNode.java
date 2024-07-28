package org.source.utility.flow.node;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 流程节点
 * 考虑到一个流程会有不同类型key的节点，Node应该是无属性的，Point是基础的有属性的节点
 *
 * @author zengfugen
 */
@Data
public abstract class AbstractNode {
    protected String name;
    protected AbstractNode next;

    /**
     * 执行业务逻辑
     *
     * @return 是否继续执行
     */
    public abstract boolean execute();

    private final List<Condition> conditionList = new ArrayList<>();

    /**
     * <pre>
     * 是否执行下一个节点：
     * 1、节点条件列表是否全为true
     * 2、节点执行逻辑是否返回true
     * 3、是否有下一个节点
     * </pre>
     */
    public void invoke() {
        if (this.condition() && this.execute() && null != this.getNext()) {
            this.getNext().invoke();
        }
    }

    public boolean condition() {
        if (CollectionUtils.isNotEmpty(conditionList)) {
            return conditionList.stream().map(Condition::test).reduce(Boolean.TRUE, Boolean::logicalAnd);
        }
        return true;
    }

    public final AbstractNode addConditions(Condition... conditions) {
        if (null != conditions) {
            this.conditionList.addAll(Arrays.asList(conditions));
        }
        return this;
    }

    public void add(AbstractNode abstractNode) {
        last().setNext(abstractNode);
    }


    public AbstractNode last() {
        AbstractNode abstractNode = this;
        while (null != abstractNode.getNext()) {
            abstractNode = abstractNode.getNext();
        }
        return abstractNode;
    }
}
