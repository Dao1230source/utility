package org.source.utility.assign;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.Getter;

import java.util.function.BiConsumer;

@JsonIncludeProperties({"name"})
public class Assemble<E, T> {
    @Getter
    private String name;

    private final BiConsumer<E, T> getAndSet;

    public Assemble(BiConsumer<E, T> getAndSet) {
        this.name = "Assemble_" + this.hashCode();
        this.getAndSet = getAndSet;
    }

    public Assemble<E, T> name(String name) {
        this.name = name;
        return this;
    }

    void invoke(E e, T t) {
        this.getAndSet.accept(e, t);
    }
}