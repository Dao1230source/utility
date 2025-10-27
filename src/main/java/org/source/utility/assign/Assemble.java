package org.source.utility.assign;

import java.util.function.BiConsumer;

public class Assemble<E, T> {
    private final BiConsumer<E, T> getAndSet;

    public Assemble(BiConsumer<E, T> getAndSet) {
        this.getAndSet = getAndSet;
    }

    void invoke(E e, T t) {
        this.getAndSet.accept(e, t);
    }
}
