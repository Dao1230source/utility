package org.source.utility.assign;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class Assemble<E, T> {
    private final BiConsumer<E, T> getAndSet;

    public Assemble(BiConsumer<E, T> getAndSet) {
        this.getAndSet = getAndSet;
    }

    public void invoke(@NotNull E e, @NotNull T t) {
        this.getAndSet.accept(e, t);
    }
}
