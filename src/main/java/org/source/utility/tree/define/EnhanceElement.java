package org.source.utility.tree.define;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 增强树元素抽象类
 * <p>
 * 继承自 Element 接口，实现了 Comparable 接口，使元素可比较和可排序。
 * 提供基于 ID 的默认比较、equals 和 hashCode 实现。
 * </p>
 * <p>
 * 所有比较方法都基于 ID 进行：
 * <ul>
 *   <li>compareTo：按 ID 比较，null 值排最后</li>
 *   <li>equals：ID 相等即为相等</li>
 *   <li>hashCode：基于 ID 计算哈希值</li>
 * </ul>
 * </p>
 *
 * @param <I> ID 类型，必须实现 Comparable 接口
 * @author zengfugen
 */
public abstract class EnhanceElement<I extends Comparable<I>> implements Element<I>, Comparable<EnhanceElement<I>> {

    /**
     * 按默认方式比较元素（使用 ID）
     * <p>
     * null 值排最后，非 null 值按 ID 排序。
     * </p>
     *
     * @param other 要比较的元素
     * @return 比较结果
     */
    @Override
    public int compareTo(@Nullable EnhanceElement<I> other) {
        // 按ID 默认比较
        if (other == null) {
            return 1;
        }
        return this.getId().compareTo(other.getId());
    }

    /**
     * 基于 ID 计算哈希值
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    /**
     * 判断两个元素是否相等
     * <p>
     * ID 相等即为相等。
     * </p>
     *
     * @param obj 要比较的对象
     * @return 如果 ID 相等返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EnhanceElement<?> other) {
            return Objects.equals(this.getId(), other.getId());
        }
        return false;
    }
}