package org.source.utility.assign;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 赋值操作类
 * <p>
 * 定义如何将获取到的外部数据赋值给目标对象的某个字段。
 * 支持多个赋值操作（Assemble），可以同时设置多个字段。
 * </p>
 *
 * @param <E> 主数据类型
 * @param <K> Key 类型
 * @param <T> 关联数据类型
 * @author zengfugen
 */
@Slf4j
@JsonIncludeProperties({"name", "assembles"})
@JsonPropertyOrder({"name", "assembles"})
public class Action<E, K, T> {
    /**
     * 操作名称
     */
    @Getter
    private String name;

    /**
     * 从主数据中提取 Key 的函数
     */
    @Getter
    private final Function<E, @Nullable K> keyGetter;

    /**
     * 所属的 Acquire 对象
     */
    @JsonBackReference
    private final Acquire<E, K, T> acquire;

    /**
     * 赋值操作列表
     */
    @JsonManagedReference
    @Getter
    private final List<Assemble<E, T>> assembles;

    /**
     * 对外部数据进行过滤
     * <p>
     * 如筛选有效数据、未删除数据等，只有通过过滤的数据才会被赋值。
     * </p>
     */
    private @Nullable Predicate<T> filter;

    /**
     * 构造函数
     *
     * @param acquire   所属的 Acquire 对象
     * @param keyGetter Key 提取函数
     */
    public Action(Acquire<E, K, T> acquire, Function<E, @Nullable K> keyGetter) {
        this.acquire = acquire;
        this.keyGetter = keyGetter;
        this.assembles = new ArrayList<>();
        this.name = "Action_" + this.hashCode();
    }

    /**
     * 添加赋值操作
     * <p>
     * 指定从关联数据中提取的值，并设置到主数据的某个字段。
     * </p>
     *
     * @param <P>      值类型
     * @param tGetter  从关联数据中提取值的函数
     * @param eSetter  设置主数据字段的消费者
     * @return this，支持链式调用
     */
    public <P> Action<E, K, T> addAssemble(Function<T, P> tGetter, BiConsumer<E, P> eSetter) {
        return addAssemble((e, t) -> eSetter.accept(e, tGetter.apply(t)));
    }

    /**
     * 添加赋值操作
     * <p>
     * 自定义的赋值逻辑，通过 BiConsumer 实现。
     * </p>
     *
     * @param getAndSet 赋值操作，接收主数据和关联数据
     * @return this，支持链式调用
     */
    public Action<E, K, T> addAssemble(BiConsumer<E, T> getAndSet) {
        Assemble<E, T> assemble = new Assemble<>(getAndSet);
        this.assembles.add(assemble);
        return this;
    }

    /**
     * 设置过滤条件
     * <p>
     * 只有通过过滤的关联数据才会被用于赋值。
     * </p>
     *
     * @param test 过滤条件
     * @return this，支持链式调用
     */
    public Action<E, K, T> filter(Predicate<T> test) {
        this.filter = test;
        return this;
    }

    /**
     * 设置操作名称
     *
     * @param name 操作名称
     * @return this，支持链式调用
     */
    public Action<E, K, T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 返回所属的 Acquire 对象
     *
     * @return Acquire 对象
     */
    public Acquire<E, K, T> backAcquire() {
        return this.acquire;
    }

    /**
     * 执行赋值操作
     *
     * @param e     主数据对象
     * @param ktMap Key 到关联数据的映射
     */
    void invoke(E e, Map<K, T> ktMap) {
        if (ktMap.isEmpty() || this.assembles.isEmpty()) {
            return;
        }
        K key = this.keyGetter.apply(e);
        if (Objects.isNull(key)) {
            log.debug("key为null，跳过赋值");
            return;
        }
        T t = ktMap.get(key);
        if (Objects.isNull(t)) {
            log.debug("未找到key={}的关联数据，跳过赋值", key);
            return;
        }
        // 通过过滤检查后才执行赋值
        if (Objects.isNull(this.filter) || this.filter.test(t)) {
            this.assembles.forEach(k -> k.invoke(e, t));
        }
    }
}