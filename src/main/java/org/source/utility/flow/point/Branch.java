package org.source.utility.flow.point;

import org.apache.commons.lang3.StringUtils;
import org.source.utility.flow.node.Selector;

/**
 * 分支，多节点可供选择
 * 目前只支持选择单一节点
 *
 * @param <K>
 * @author zengfugen
 */
public class Branch<BK, PK, K> extends Point<BK, PK> {
    public static final String DEFAULT_NAME = "Branch";

    @Override
    public String getName() {
        if (StringUtils.isBlank(super.getName()) || Point.DEFAULT_NAME.equals(super.getName())) {
            this.setName(DEFAULT_NAME);
        }
        return super.getName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Point<BK, PK> getNext() {
        return (Point<BK, PK>) this.getContainer().select();
    }

    /**
     * 分支节点没有执行逻辑，只选择下一个单节点
     *
     * @return true
     */
    @Override
    public final boolean execute() {
        return true;
    }

    @SafeVarargs
    public static <BK, PK, K> Branch<BK, PK, K> of(Selector<PK> selector, Point<PK, K>... points) {
        return new Branch<>(selector, points);
    }

    @SafeVarargs
    private <P extends Point<PK, K>> Branch(Selector<PK> selector, P... points) {
        this.setContainer(Container.of(selector, points));
    }

}
