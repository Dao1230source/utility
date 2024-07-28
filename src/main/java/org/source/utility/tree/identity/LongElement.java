package org.source.utility.tree.identity;

import org.jetbrains.annotations.NotNull;

public abstract class LongElement implements Element<Long> {
    @Override
    public int compareTo(@NotNull Element<Long> o) {
        return this.getId().compareTo(o.getId());
    }
}
