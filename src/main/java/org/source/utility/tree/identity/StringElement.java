package org.source.utility.tree.identity;

import org.jetbrains.annotations.NotNull;

public abstract class StringElement implements Element<String> {
    @Override
    public int compareTo(@NotNull Element<String> o) {
        return this.getId().compareTo(o.getId());
    }
}
