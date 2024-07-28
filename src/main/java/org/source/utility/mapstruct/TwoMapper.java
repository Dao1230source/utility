package org.source.utility.mapstruct;

import org.mapstruct.IterableMapping;
import org.mapstruct.MapperConfig;
import org.mapstruct.Named;

import java.util.List;

@MapperConfig
public interface TwoMapper<X, Y> {
    /**
     * 转换
     *
     * @param x source
     * @return target
     */
    @Named("x2y")
    Y x2y(X x);

    /**
     * 复原
     *
     * @param y target
     * @return source
     */
    @Named("y2x")
    X y2x(Y y);

    /**
     * 转换
     *
     * @param xList sList
     * @return tList
     */
    @IterableMapping(qualifiedByName = "x2y")
    List<Y> x2yList(List<X> xList);

    /**
     * 复原
     *
     * @param yList tList
     * @return sList
     */
    @IterableMapping(qualifiedByName = "y2x")
    List<X> y2xList(List<Y> yList);
}
