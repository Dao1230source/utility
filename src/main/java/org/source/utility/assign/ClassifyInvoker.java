package org.source.utility.assign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ClassifyInvoker<E, K> {
    private Function<E, K> classifier;
    private List<KeyAssigner<E, K>> keyAssigners;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class KeyAssigner<E, K> {
        private K key;
        private Function<Collection<E>, Assign<E>> assign;
    }
}