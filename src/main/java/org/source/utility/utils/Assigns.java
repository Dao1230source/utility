package org.source.utility.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * @author zengfugen
 */
@Slf4j
public class Assigns {
    private Assigns() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 本地缓存, 10分钟过期
     */
    public static final LoadingCache<String, Map<Object, Object>> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).build(k -> new HashMap<>());


    @SafeVarargs
    public static <E, T, K, N> void assignName(
            @NonNull Collection<E> mainData,
            @NonNull Function<Set<K>, Collection<T>> additionalDataGetter,
            @NonNull Function<T, K> tkGetter,
            ValueAssign<E, T, K, N>... valueAssigns
    ) {
        assignName(mainData, additionalDataGetter, tkGetter, null, valueAssigns);
    }

    @SafeVarargs
    public static <E, T, K, N> void assignName(
            @NonNull Collection<E> mainData,
            @NonNull Function<Set<K>, Collection<T>> additionalDataGetter,
            @NonNull Function<T, K> tkGetter,
            @Nullable String additionalDataCacheName,
            ValueAssign<E, T, K, N>... valueAssigns
    ) {
        Function<Set<K>, Map<K, T>> collectionMapFunction = additionalDataGetter.andThen(cs ->
                cs.stream().collect(Collectors.toMap(tkGetter, Function.identity(), (v1, v2) -> v1)));
        assignName(mainData, collectionMapFunction, additionalDataCacheName, valueAssigns);
    }

    @SafeVarargs
    public static <E, T, K, N> void assignName(
            @NonNull Collection<E> mainData,
            @NonNull Function<Set<K>, Map<K, T>> additionalDataGetter,
            ValueAssign<E, T, K, N>... valueAssigns
    ) {
        assignName(mainData, additionalDataGetter, null, valueAssigns);
    }

    /**
     * 根据原数据的一个或多个key的值查询其他数据，并赋值给原数据
     * 原对象引用不变
     *
     * @param mainData                eList
     * @param additionalDataGetter    从数据库查询主数据列表
     * @param additionalDataCacheName cacheName
     * @param valueAssigns            名称赋值
     * @param <E>                     e
     * @param <T>                     主数据类型
     * @param <K>                     key
     * @param <N>                     name
     */
    @SafeVarargs
    public static <E, T, K, N> void assignName(
            @NonNull Collection<E> mainData,
            @NonNull Function<Set<K>, Map<K, T>> additionalDataGetter,
            @Nullable String additionalDataCacheName,
            ValueAssign<E, T, K, N>... valueAssigns
    ) {
        if (CollectionUtils.isEmpty(mainData)) {
            return;
        }
        if (Objects.isNull(valueAssigns) || valueAssigns.length == 0) {
            return;
        }
        Set<K> kSet = mainData.stream()
                .map(k -> Arrays.stream(valueAssigns).map(ValueAssign::getKeyGetter).filter(Objects::nonNull).map(e -> e.apply(k))
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (kSet.isEmpty()) {
            return;
        }
        Map<K, T> ktMap = HashMap.newHashMap(kSet.size());
        if (StringUtils.isEmpty(additionalDataCacheName)) {
            ktMap.putAll(additionalDataGetter.apply(kSet));
        } else {
            try {
                @SuppressWarnings("unchecked")
                Map<K, T> cachedMap = (Map<K, T>) CACHE.get(additionalDataCacheName);
                if (Objects.nonNull(cachedMap) && !cachedMap.isEmpty()) {
                    kSet.forEach(k -> ktMap.put(k, cachedMap.get(k)));
                }
            } catch (Exception e) {
                log.error("get map from cache exception:{}", ExceptionUtils.getStackTrace(e));
            }
            Set<K> notInCacheKeys = kSet.stream().filter(k -> !ktMap.containsKey(k)).collect(Collectors.toSet());
            if (!notInCacheKeys.isEmpty()) {
                Map<K, T> remoteKtMap = additionalDataGetter.apply(notInCacheKeys);
                ktMap.putAll(remoteKtMap);
            }
        }
        mainData.forEach(e -> Arrays.stream(valueAssigns).filter(Objects::nonNull).forEach(valueAssign -> {
            K key = valueAssign.getKeyGetter().apply(e);
            T t = ktMap.get(key);
            if (Objects.nonNull(t)) {
                Arrays.stream(valueAssign.getAndSets).filter(Objects::nonNull).forEach(n -> {
                    N tn = n.getTnGetter().apply(t);
                    n.getNameSetter().accept(e, tn);
                });
            }
        }));
    }

    /**
     * key作为唯一键，获取对应的主数据
     * 从主数据 T 中获取名称 N，赋值给 E
     *
     * @param <E> e 被处理对象
     * @param <K> k 唯一键
     * @param <T> t 主数据
     * @param <N> n 名称
     */
    @Data
    public static class ValueAssign<E, T, K, N> {
        private Function<E, K> keyGetter;
        private GetAndSet<E, T, N>[] getAndSets;

        @SafeVarargs
        public ValueAssign(@NonNull Function<E, K> keyGetter, @NonNull GetAndSet<E, T, N>... getAndSets) {
            this.keyGetter = keyGetter;
            this.getAndSets = getAndSets;
        }
    }

    @Data
    public static class GetAndSet<E, T, N> {
        private Function<T, N> tnGetter;
        private BiConsumer<E, N> nameSetter;

        public GetAndSet(@NonNull Function<T, N> tnGetter, @NonNull BiConsumer<E, N> nameSetter) {
            this.tnGetter = tnGetter;
            this.nameSetter = nameSetter;
        }

    }

    public static class StringGetAndSet<E, T> extends GetAndSet<E, T, String> {
        public StringGetAndSet(Function<T, String> tnGetter, BiConsumer<E, String> nameSetter) {
            super(tnGetter, nameSetter);
        }

        public static <E, T> GetAndSet<E, T, String> stringToString(@NonNull Function<T, String> tnGetter,
                                                                    @NonNull BiConsumer<E, String> nameSetter) {
            return new GetAndSet<>(tnGetter, nameSetter);
        }


        public static <E, T> GetAndSet<E, T, String> longTostring(@NonNull ToLongFunction<T> tnGetter,
                                                                  @NonNull BiConsumer<E, String> nameSetter) {
            return new GetAndSet<>(t -> String.valueOf(tnGetter.applyAsLong(t)), nameSetter);
        }

        public static <E, T> GetAndSet<E, T, String> intToInt(@NonNull ToIntFunction<T> tnGetter,
                                                              @NonNull ObjIntConsumer<E> nameSetter) {
            return new GetAndSet<>(t -> String.valueOf(tnGetter.applyAsInt(t)), (t, n) -> nameSetter.accept(t, Integer.parseInt(n)));
        }

        public static <E, T> GetAndSet<E, T, String> stringToInt(@NonNull Function<T, String> tnGetter,
                                                                 @NonNull ObjIntConsumer<E> nameSetter) {
            return new GetAndSet<>(tnGetter, (t, n) -> nameSetter.accept(t, Integer.parseInt(n)));
        }

        public static <E, T> GetAndSet<E, T, String> stringToDate(@NonNull Function<T, String> tnGetter,
                                                                  @NonNull BiConsumer<E, LocalDateTime> nameSetter) {
            return new GetAndSet<>(tnGetter, (t, n) -> nameSetter.accept(t, Dates.strToLocalDateTime(n, Dates.LOCAL_DATE_TIME)));
        }

        public static <E, T> GetAndSet<E, T, String> bigDecimalToBigDecimal(@NonNull Function<T, BigDecimal> tnGetter,
                                                                            @NonNull BiConsumer<E, BigDecimal> nameSetter) {
            return new GetAndSet<>(t -> tnGetter.apply(t).toString(), (t, n) -> nameSetter.accept(t, new BigDecimal(n)));
        }
    }

}
