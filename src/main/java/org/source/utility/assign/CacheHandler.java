package org.source.utility.assign;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 缓存处理器接口
 * <p>
 * 定义数据获取结果的缓存操作，支持从缓存读取和写入缓存。
 * 可自定义实现以接入 Redis、Caffeine 等缓存框架。
 * </p>
 *
 * @author zengfugen
 */
public interface CacheHandler {

    /**
     * 从缓存获取数据
     *
     * @param <K>        Key 类型
     * @param <T>        数据类型
     * @param cachedName 缓存名称
     * @param keys       Key 集合
     * @return Key 到数据的映射，缓存未命中返回 null
     */
    <K, T> @Nullable Map<K, T> get(String cachedName, Collection<K> keys);

    /**
     * 将数据写入缓存
     *
     * @param <K>        Key 类型
     * @param <T>        数据类型
     * @param cachedName 缓存名称
     * @param kt         Key 到数据的映射
     */
    <K, T> void put(String cachedName, Map<K, T> kt);
}
