package org.source.utility.flow.point;

import org.source.utility.flow.processor.Processor;
import org.source.utility.flow.node.AbstractNode;
import org.source.utility.flow.node.Selector;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 单节点， 多处理器可选择
 * 目前只支持执行一个处理器
 *
 * @author zengfugen
 * @param <PK> point的key值
 * @param <K>  container中的key值
 */
@EqualsAndHashCode(callSuper = false)
@Slf4j
@Data
public class Point<PK, K> extends AbstractNode implements Processor<PK> {
    public static final String DEFAULT_NAME = "Point";
    /**
     * 当单节点应用于分支节点时，需要此变量确定唯一性
     */
    private PK key;

    private Container<K, Processor<K>> container;

    @Override
    public PK getKey() {
        return this.key;
    }

    @Override
    public String getName() {
        if (StringUtils.isBlank(super.getName())) {
            this.setName(DEFAULT_NAME);
        }
        return super.getName();
    }

    @Override
    public Boolean value() {
        Processor<K> processor = container.select();
        if (null == processor) {
            return true;
        }
        log.debug("执行处理器：{}", processor.getName());
        return processor.value();
    }

    @Override
    public boolean execute() {
        log.debug("Point.execute()，节点开始执行，{}", this.getName());
        return this.value();
    }

    public Point() {
    }

    private <P extends Processor<K>> Point(P processor) {
        this.container = Container.of(processor);
    }

    @SafeVarargs
    private <P extends Processor<K>> Point(Selector<K> key, P... processors) {
        this.container = Container.of(key, processors);
    }

    /**
     * 简单节点
     *
     * @param consumer 消费者
     * @param t        参数
     * @param <PK>     point本身的key值泛型
     * @param <T>      参数泛型
     * @return 流程节点
     */
    public static <PK, K, T> Point<PK, K> ofSimple(K key, Consumer<T> consumer, T t) {
        return new Point<>(Processor.ofSimple(key, consumer, t));
    }

    /**
     * 简单节点
     *
     * @param supplier 供应商-布尔值
     * @param <PK>     point本身的key值泛型
     * @return 流程节点
     */
    public static <PK, K> Point<PK, K> ofSimple(K key, BooleanSupplier supplier) {
        return new Point<>(Processor.ofSimple(key, supplier));
    }

    /**
     * 使用单处理器创建流程节点
     *
     * @param processor 处理器
     * @param <PK>      point本身的key值泛型
     * @param <K>       处理器的key值泛型
     * @param <P>       处理器具体实现
     * @return 流程节点
     */
    public static <PK, K, P extends Processor<K>> Point<PK, K> of(P processor) {
        return new Point<>(processor);
    }

    /**
     * 使用多处理器 + 选择器创建流程节点
     *
     * @param key       选择器
     * @param processor 处理器
     * @param <PK>      point本身的key值泛型
     * @param <K>       处理器的key值泛型
     * @param <P>       处理器具体实现
     * @return 流程节点
     */
    @SafeVarargs
    public static <PK, K, P extends Processor<K>> Point<PK, K> of(Selector<K> key, P... processor) {
        return new Point<>(key, processor);
    }

    /**
     * 使用单处理器创建流程节点
     * 并设置point的key值，一般用于{@link Branch}中
     *
     * @param pointKey  point本身的key值
     * @param processor 处理器
     * @param <PK>      point本身的key值泛型
     * @param <K>       处理器的key值泛型
     * @param <P>       处理器具体实现
     * @return 流程节点
     */
    public static <PK, K, P extends Processor<K>> Point<PK, K> ofSelf(PK pointKey, P processor) {
        Point<PK, K> point = Point.of(processor);
        point.setKey(pointKey);
        return point;
    }

    /**
     * 使用多个处理器 + 选择器创建流程节点
     * 并设置point的key值，一般用于{@link Branch}中
     *
     * @param pointKey  point本身的key值
     * @param key       选择器
     * @param processor 处理器
     * @param <PK>      point本身的key值泛型
     * @param <K>       处理器的key值泛型
     * @param <P>       处理器具体实现
     * @return 流程节点
     */
    @SafeVarargs
    public static <PK, K, P extends Processor<K>> Point<PK, K> ofSelf(PK pointKey, Selector<K> key, P... processor) {
        Point<PK, K> point = Point.of(key, processor);
        point.setKey(pointKey);
        return point;
    }
}
