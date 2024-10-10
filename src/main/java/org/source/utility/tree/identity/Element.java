package org.source.utility.tree.identity;


import lombok.NonNull;

import java.io.Serializable;

/**
 * @author zengfugen
 */
public interface Element<I> extends Comparable<Element<I>>, Serializable {
    /**
     * get id
     *
     * @return id
     */
    @NonNull
    I getId();

    /**
     * get parentId
     *
     * @return parentId
     */
    I getParentId();

}