package org.source.utility.flow.point;

import org.source.utility.flow.processor.StringProcessor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author zengfugen
 */
public class StringPoint<K> extends Point<String, K> {

    @Override
    public void setKey(String key) {
        super.setKey(this.getName());
    }

    public static <PK, T> Point<PK, String> ofSimple(Consumer<T> consumer, T t) {
        return of(StringProcessor.ofSimple(consumer, t));
    }

    public static <PK> Point<PK, String> ofSimple(BooleanSupplier supplier) {
        return of(StringProcessor.ofSimple(supplier));
    }
}
