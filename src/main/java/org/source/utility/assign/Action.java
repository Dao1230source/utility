package org.source.utility.assign;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@JsonIncludeProperties({"name", "assembles"})
@JsonPropertyOrder({"name", "assembles"})
public class Action<E, K, T> {
    @Getter
    private String name;
    @Getter
    private final Function<E, K> keyGetter;
    @JsonBackReference
    private final Acquire<E, K, T> acquire;
    @JsonManagedReference
    @Getter
    private final List<Assemble<E, T>> assembles;
    /**
     * 对外部数据进行过滤，如筛选有效数据、未删除数据等
     */
    private Predicate<T> filter;

    public Action(Acquire<E, K, T> acquire, Function<E, K> keyGetter) {
        this.acquire = acquire;
        this.keyGetter = keyGetter;
        this.assembles = new ArrayList<>();
        this.name = "Action_" + this.hashCode();
    }

    public <P> Action<E, K, T> addAssemble(Function<T, P> tGetter, BiConsumer<E, P> eSetter) {
        return addAssemble((e, t) -> eSetter.accept(e, tGetter.apply(t)));
    }

    public Action<E, K, T> addAssemble(BiConsumer<E, T> getAndSet) {
        Assemble<E, T> assemble = new Assemble<>(getAndSet);
        this.assembles.add(assemble);
        return this;
    }

    public Action<E, K, T> filter(Predicate<T> test) {
        this.filter = test;
        return this;
    }

    public Action<E, K, T> name(String name) {
        this.name = name;
        return this;
    }


    public Acquire<E, K, T> backAcquire() {
        return this.acquire;
    }

    void invoke(E e, Map<K, T> ktMap) {
        if (ktMap.isEmpty() || this.assembles.isEmpty()) {
            return;
        }
        K key = this.keyGetter.apply(e);
        if (Objects.isNull(key)) {
            log.debug("元素{}的key为null，跳过赋值", e);
            return;
        }
        T t = ktMap.get(key);
        if (Objects.isNull(t)) {
            log.debug("未找到key={}的关联数据，跳过赋值", key);
            return;
        }
        if (Objects.isNull(this.filter) || this.filter.test(t)) {
            this.assembles.forEach(k -> k.invoke(e, t));
        }
    }
}