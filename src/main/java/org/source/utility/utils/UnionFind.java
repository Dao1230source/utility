package org.source.utility.utils;

import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 并查集（Union-Find/Disjoint Set Union）数据结构
 * <p>
 * 用于高效地解决动态连通性问题，特别适用于：
 * <ul>
 *   <li>检测图中的循环</li>
 *   <li>判断两个节点是否连通</li>
 *   <li>动态连通性管理</li>
 * </ul>
 * </p>
 * <p>
 * 算法特性：
 * <ul>
 *   <li><strong>路径压缩（Path Compression）</strong>：find 操作时优化树结构</li>
 *   <li><strong>按秩合并（Union by Rank）</strong>：总是把较小的树挂在较大的树下</li>
 *   <li><strong>时间复杂度</strong>：接近 O(1)，更精确地是 O(α(n))，其中 α 是 Ackermann 函数的反函数</li>
 *   <li><strong>空间复杂度</strong>：O(n)</li>
 * </ul>
 * </p>
 * <p>
 * 在树组件中的应用：
 * 用于在 add() 操作时检测是否会形成循环引用，确保树的结构有效性。
 * </p>
 *
 * @param <I> 元素类型，必须实现 equals 和 hashCode 方法
 * @author utility
 * @since 1.0
 */
public class UnionFind<I> {
    /**
     * 父节点映射表
     * key：当前元素
     * value：父节点（对于根节点，自己是自己的父节点）
     */
    final Map<I, I> parent = new HashMap<>();

    /**
     * 秩映射表
     * 用于记录以每个根节点为根的树的秩（高度的上界）
     * 秩越小，树越扁平，查询越快
     */
    final Map<I, Integer> rank = new HashMap<>();

    /**
     * 创建一个新的集合，包含单个元素
     * <p>
     * 幂等操作：多次调用同一个元素不会有副作用
     * </p>
     *
     * @param x 要添加的元素，如果已存在则无操作
     */
    public void makeSet(@Nullable I x) {
        if (Objects.isNull(x)) {
            return;
        }
        parent.computeIfAbsent(x, k -> {
            rank.put(x, 0);
            return x;
        });
    }

    /**
     * 查找元素所在集合的代表元素
     * <p>
     * 实现了路径压缩（Path Compression）优化：
     * 在查找过程中，将访问过的节点直接连接到根节点，减少树的高度。
     * 这使得后续的查找操作更快。
     * </p>
     *
     * @param x 要查找的元素，如果不存在会自动创建
     * @return 所在集合的根节点（代表元素）
     */
    public I find(I x) {
        if (!parent.containsKey(x)) {
            makeSet(x);
        }
        I p = parent.get(x);
        if (!Objects.equals(x, p)) {
            // 路径压缩：将 x 的父节点直接指向根节点
            parent.put(x, find(p));
        }
        return parent.get(x);
    }

    /**
     * 合并两个集合
     * <p>
     * 实现了按秩合并（Union by Rank）优化：
     * 总是把秩较小的树的根作为秩较大的树根的子节点，
     * 这保证了树的高度最小，从而保证了接近 O(1) 的查询时间。
     * </p>
     * <p>
     * 注意：如果 x 和 y 已在同一个集合中，此方法无操作。
     * </p>
     *
     * @param x 第一个集合中的元素
     * @param y 第二个集合中的元素，秩较大的节点，即父节点
     */
    public void union(I x, I y) {
        I rootX = find(x);
        I rootY = find(y);

        if (rootX.equals(rootY)) {
            return;
        }

        // 按秩合并
        // 未找到秩默认0
        int rankX = rank.getOrDefault(rootX, 0);
        int rankY = rank.getOrDefault(rootY, 0);

        if (rankX < rankY) {
            parent.put(rootX, rootY);
        } else if (rankX > rankY) {
            parent.put(rootY, rootX);
        } else {
            parent.put(rootX, rootY);
            rank.put(rootX, rankX + 1);
        }
    }

    /**
     * 清空并查集
     * <p>
     * 删除所有元素和它们的关系，将并查集恢复到初始状态。
     * </p>
     */
    public void clear() {
        parent.clear();
        rank.clear();
    }

    /**
     * 将另一个并查集的内容重建到当前并查集
     * <p>
     * 用于替换当前的并查集状态，主要应用于树结构变化后的重建。
     * 会清空当前的所有数据，然后复制另一个并查集的完整状态。
     * </p>
     *
     * @param other 源并查集，其内容将被复制到当前并查集
     */
    public void rebuild(UnionFind<I> other) {
        this.parent.clear();
        this.rank.clear();
        this.parent.putAll(other.parent);
        this.rank.putAll(other.rank);
    }
}