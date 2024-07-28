package org.source.utility.tree.identity;

import org.jetbrains.annotations.NotNull;

public abstract class IntegerElement implements Element<Integer> {
    @Override
    public int compareTo(@NotNull Element<Integer> o) {
        return this.getId().compareTo(o.getId());
    }
}
