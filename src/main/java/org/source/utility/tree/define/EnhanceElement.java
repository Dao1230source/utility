package org.source.utility.tree.define;

import org.springframework.lang.Nullable;

import java.util.Objects;

public abstract class EnhanceElement<I extends Comparable<I>> implements Element<I>, Comparable<EnhanceElement<I>> {

    @Override
    public int compareTo(@Nullable EnhanceElement<I> other) {
        // 按ID 默认比较
        if (other == null) {
            return 1;
        }
        return this.getId().compareTo(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EnhanceElement<?> other) {
            return Objects.equals(this.getId(), other.getId());
        }
        return false;
    }
}