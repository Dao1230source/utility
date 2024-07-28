package org.source.utility.mapstruct;

import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 三个对象转换
 *
 * @param <X> x
 * @param <Y> y
 * @param <Z> z
 */
public interface ThreeMapper<X, Y, Z> extends TwoMapper<X, Y> {

    /**
     * x2z
     *
     * @param x x
     * @return z
     */
    @Named("x2z")
    Z x2z(X x);

    /**
     * z
     *
     * @param y y
     * @return z
     */
    @Named("y2z")
    Z y2z(Y y);

    /**
     * z2x
     *
     * @param z z
     * @return x
     */
    @Named("z2x")
    X z2x(Z z);

    /**
     * z2y
     *
     * @param z z
     * @return y
     */
    @Named("z2y")
    Y z2y(Z z);

    /**
     * x2zList
     *
     * @param xes xes
     * @return zs
     */
    @IterableMapping(qualifiedByName = "x2z")
    List<Z> x2zList(List<X> xes);

    /**
     * toViews
     *
     * @param ys entities
     * @return view
     */
    @IterableMapping(qualifiedByName = "y2z")
    List<Z> y2zList(List<Y> ys);

    /**
     * z2xList
     *
     * @param zs zs
     * @return xes
     */
    @IterableMapping(qualifiedByName = "z2x")
    List<X> z2xList(List<Z> zs);

    /**
     * z2yList
     *
     * @param zs zs
     * @return ys
     */
    @IterableMapping(qualifiedByName = "z2y")
    List<Y> z2yList(List<Z> zs);
}
