package org.source.utility.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class Batchs {
    private Batchs() {
        throw new IllegalStateException("Utility class");
    }

    private static final int ACTION_BATCH_PROCESSING_THRESHOLD = 1000;

    public static int getActionBatchProcessingThreshold() {
        return ACTION_BATCH_PROCESSING_THRESHOLD;
    }

    /**
     * 批量操作
     *
     * @param consumer 批量执行方法
     * @param c        待插入的列表
     * @param <E>      待插入的列表泛型
     */
    public static <E, C extends Collection<E>> void actionBatch(@NotNull C c, @NotNull Consumer<C> consumer,
                                                                @NotNull IntFunction<C> cInstance, @NotNull Integer batchSize) {
        if (CollectionUtils.isEmpty(c)) {
            return;
        }
        if (batchSize <= 0) {
            consumer.accept(c);
            return;
        }
        C subBatch = cInstance.apply(batchSize);
        for (E e : c) {
            subBatch.add(e);
            if (subBatch.size() == batchSize) {
                consumer.accept(subBatch);
                subBatch.clear();
            }
        }
        if (!subBatch.isEmpty()) {
            consumer.accept(subBatch);
        }
    }

    public static <E> void actionBatch(Consumer<List<E>> consumer, List<E> eList, Integer batchSize) {
        actionBatch(eList, consumer, ArrayList::new, batchSize);
    }

    public static <E> void actionBatch(Consumer<List<E>> consumer, List<E> eList) {
        actionBatch(eList, consumer, ArrayList::new, getActionBatchProcessingThreshold());
    }

}
