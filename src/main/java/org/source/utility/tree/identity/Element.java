package org.source.utility.tree.identity;


import lombok.NonNull;

import java.io.Serializable;

/**
 * @author zengfugen
 */
public interface Element<I> extends Comparable<Element<I>>, Serializable {
    long serialVersionUID = 2405172041950251807L;

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