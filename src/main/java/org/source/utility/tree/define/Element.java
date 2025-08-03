package org.source.utility.tree.define;


import lombok.NonNull;

/**
 * @author zengfugen
 */
public interface Element<I> {
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