package org.source.utility.assign;

import lombok.Getter;
import org.source.utility.utils.Maps;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Action<E, K, T> {
    @Getter
    private final Function<E, K> keyGetter;
    private final Acquire<E, K, T> acquire;
    private final List<Assemble<E, T>> assembles;
    /**
     * 对外部数据进行过滤，如筛选有效数据、未删除数据等
     */
    private Predicate<T> test;

    public Action(Acquire<E, K, T> acquire, Function<E, K> keyGetter) {
        this.acquire = acquire;
        this.keyGetter = keyGetter;
        this.assembles = new ArrayList<>();
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
        this.test = test;
        return this;
    }

    public Acquire<E, K, T> backAcquire() {
        return this.acquire;
    }

    public void invoke(E e, Map<K, T> ktMap) {
        if (Maps.isEmpty(ktMap)) {
            return;
        }
        K key = this.keyGetter.apply(e);
        T t = ktMap.get(key);
        if (Objects.nonNull(t) && !CollectionUtils.isEmpty(this.assembles)
                && (Objects.isNull(this.test) || this.test.test(t))) {
            this.assembles.forEach(k -> k.invoke(e, t));
        }
    }
}
