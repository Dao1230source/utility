package org.source.utility.assign;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认内存缓存处理器实现
 * <p>
 * 使用 ConcurrentHashMap 实现简单的内存缓存。
 * 适用于单机环境，不支持分布式缓存。
 * </p>
 * <p>
 * 注意：
 * <ul>
 *   <li>缓存存储在 JVM 内存中，重启后数据丢失</li>
 *   <li>没有过期机制，数据永久存储</li>
 *   <li>多 JVM 实例之间缓存不共享</li>
 * </ul>
 * </p>
 *
 * @author zengfugen
 */
public class DefaultCacheHandler implements CacheHandler {
    /**
     * 全局缓存映射
     * <p>
     * Key：缓存名称
     * Value：Key 到数据的映射
     * </p>
     */
    private static final Map<String, Map<Object, Object>> CACHED_MAP = new ConcurrentHashMap<>();

    /**
     * 从缓存获取数据
     * <p>
     * 根据缓存名称和 Key 集合从缓存中获取对应的数据。
     * </p>
     *
     * @param <K>        Key 类型
     * @param <T>        数据类型
     * @param cachedName 缓存名称
     * @param keys       Key 集合
     * @return Key 到数据的映射，缓存未命中返回空 Map
     */
    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <K, T> Map<K, T> get(String cachedName, Collection<K> keys) {
        Map<Object, Object> map = CACHED_MAP.get(cachedName);
        if (Objects.isNull(map)) {
            return Map.of();
        }
        Map<K, T> kt = new ConcurrentHashMap<>();
        keys.forEach(k -> kt.put(k, (T) map.get(k)));
        return kt;
    }

    /**
     * 将数据写入缓存
     * <p>
     * 将 Key 到数据的映射写入指定名称的缓存。
     * </p>
     *
     * @param <K>        Key 类型
     * @param <T>        数据类型
     * @param cachedName 缓存名称
     * @param kt         Key 到数据的映射
     */
    @SuppressWarnings("unchecked")
    @Override
    public <K, T> void put(String cachedName, Map<K, T> kt) {
        Map<K, T> cachedMap = (Map<K, T>) CACHED_MAP.computeIfAbsent(cachedName, k -> new ConcurrentHashMap<>());
        cachedMap.putAll(kt);
    }
}
