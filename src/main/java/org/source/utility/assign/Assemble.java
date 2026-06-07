package org.source.utility.assign;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.Getter;

import java.util.function.BiConsumer;

/**
 * 赋值操作类
 * <p>
 * 封装单个赋值操作，将关联数据赋值给主数据的指定字段。
 * </p>
 *
 * @param <E> 主数据类型
 * @param <T> 关联数据类型
 * @author zengfugen
 */
@JsonIncludeProperties({"name"})
public class Assemble<E, T> {
    /**
     * 操作名称
     */
    @Getter
    private String name;

    /**
     * 赋值操作
     * <p>
     * 接收主数据和关联数据，执行赋值操作。
     * </p>
     */
    private final BiConsumer<E, T> getAndSet;

    /**
     * 构造函数
     *
     * @param getAndSet 赋值操作
     */
    public Assemble(BiConsumer<E, T> getAndSet) {
        this.name = "Assemble_" + this.hashCode();
        this.getAndSet = getAndSet;
    }

    /**
     * 设置名称
     *
     * @param name 操作名称
     * @return this，支持链式调用
     */
    public Assemble<E, T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 执行赋值操作
     *
     * @param e 主数据对象
     * @param t 关联数据对象
     */
    void invoke(E e, T t) {
        this.getAndSet.accept(e, t);
    }
}