package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.tree.define.*;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.source.utility.utils.UnionFind;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.*;

/**
 * 树容器类
 * <p>
 * 通用的树数据结构实现，支持以下特性：
 * <ul>
 *   <li>泛型设计，支持任意类型的元素和节点</li>
 *   <li>并发安全：通过读写锁保护所有操作</li>
 *   <li>循环检测：使用并查集（Union-Find）检测循环引用</li>
 *   <li>灵活的节点操作：add、remove、find、get 等</li>
 *   <li>可配置的处理器：创建后处理、合并处理、添加后处理</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 *   Tree&lt;Integer, MyElement, DefaultNode&gt; tree = Tree.of(new DefaultNode&lt;&gt;());
 *   tree.add(Arrays.asList(element1, element2, element3));
 *   List&lt;DefaultNode&gt; found = tree.find(node -> node.getId() > 10);
 * </pre>
 * </p>
 *
 * @param <I> ID 类型，必须实现 Comparable 接口
 * @param <E> 元素类型，必须实现 Element 接口
 * @param <N> 节点类型，必须继承 AbstractNode
 * @author utility
 * @since 1.0
 */
@JsonIgnoreProperties(value = {"sourceElements", "idMap", "afterCreateHandler", "mergeHandler",
        "afterAddHandler", "idGetter", "parentIdGetter", "unionFind"})
@Slf4j
@Data
public class Tree<I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    /**
     * 源元素集合
     * 存储所有通过 add() 方法添加的源元素，用于序列化和遍历
     */
    private final List<E> sourceElements = new ArrayList<>(32);

    /**
     * 节点 ID 映射表
     * 用于快速查找节点，key 为节点 ID，value 为对应的节点对象
     * 采用 ConcurrentHashMap 确保并发安全性
     */
    private final Map<I, N> idMap = new ConcurrentHashMap<>(32);

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 写操作锁
     * 保护所有修改操作：add、remove、clear
     */
    private final Lock writeLock = this.lock.writeLock();

    /**
     * 读操作锁
     * 保护所有查询操作：find、get、getById
     */
    private final Lock readLock = this.lock.readLock();

    /**
     * 并查集，用于循环引用检测
     * 维护所有节点的连通关系，用于检测添加节点时是否会形成循环
     * 采用延迟初始化：root 节点在第一次 add() 时才添加到并查集中
     */
    private final UnionFind<I> unionFind = new UnionFind<>();
    /**
     * 树的根节点
     * 所有父节点为空的节点都会以此作为父节点
     */
    private final N root;
    /**
     * 节点合并处理器，和{@see org.source.utility.tree.define.AbstractNode#mergeHandler}是分别控制的
     * <br/>
     * 默认：保留新节点
     */
    private BinaryOperator<N> mergeHandler = (n, old) -> n;
    /**
     * 节点创建后的处理器
     * 在节点创建完成后、添加到树之前调用
     */
    private @Nullable Consumer<N> afterCreateHandler;

    /**
     * 节点添加后的处理器
     * 在节点添加到树之后调用
     */
    private @Nullable BiConsumer<N, N> afterAddHandler;
    /**
     * ID 获取函数
     * 从元素中提取 ID，默认调用 element.getId()
     */
    private Function<N, I> idGetter = N::getId;

    /**
     * 父节点 ID 获取函数
     * 从元素中提取父节点 ID，默认调用 element.getParentId()
     */
    private Function<N, I> parentIdGetter = N::getParentId;

    /**
     * 构造函数
     * <p>
     * 初始化树对象，设置根节点和并发锁。
     * 注意：root 是虚拟节点，代表树的根，一般不包含实际数据。
     * </p>
     *
     * @param root 根节点，不能为 null
     */
    protected Tree(N root) {
        this.root = root;
    }

    /**
     * 创建树实例的工厂方法
     * <p>
     * 通过提供的根节点创建一个新的树对象。
     * </p>
     *
     * @param root 根节点，通常是一个空节点
     * @param <I>  节点 ID 类型
     * @param <E>  元素类型
     * @param <N>  节点类型
     * @return 新创建的树实例
     */
    public static <I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> Tree<I, E, N> of(N root) {
        return new Tree<>(root);
    }

    /**
     * 添加元素集合到树中
     * <p>
     * 此方法是线程安全的，通过写锁保护。
     * 在添加前会检测是否会形成循环引用，如果存在循环则抛出异常。
     * </p>
     *
     * @param es 要添加的元素集合
     * @return 返回树的根节点
     * @throws IllegalArgumentException 如果检测到循环引用
     */
    public N add(Collection<? extends E> es) {
        writeLock.lock();
        try {
            return doAdd(es);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 内部添加方法，执行实际的添加逻辑
     * <p>
     * 执行步骤：
     * <ol>
     *   <li>创建节点对象</li>
     *   <li>缓存所有新节点（避免父级在后的问题）</li>
     *   <li>使用并查集检测循环引用</li>
     *   <li>将节点添加到树中</li>
     *   <li>执行添加后处理器</li>
     * </ol>
     * </p>
     *
     * @param es 要添加的元素集合
     * @return 返回树的根节点
     * @throws IllegalArgumentException 如果检测到循环引用
     */
    protected N doAdd(Collection<? extends E> es) {
        if (CollectionUtils.isEmpty(es)) {
            return root;
        }
        List<N> toAddNodes = Streams.map(es, e -> {
            N n = this.getRoot().emptyNode();
            n.setElement(e);
            if (Objects.nonNull(this.getAfterCreateHandler())) {
                this.getAfterCreateHandler().accept(n);
            }
            return n;
        }).toList();
        // 先将所有元素缓存，避免有可能父级数据在后，当前元素加入时找不到父级的情况
        List<MergeNodeResult<I, E, N>> mergedResult = Streams.map(toAddNodes, n ->
                AbstractNode.mergeNode(n, this.getIdGetter(), this.getIdMap(), this.getMergeHandler())
        ).filter(Objects::nonNull).toList();
        // 添加节点
        List<N> mergedNodes = Streams.of(mergedResult).map(MergeNodeResult::getResultNode).toList();
        mergedNodes.forEach(node -> {
            I parentId = this.getParentIdGetter().apply(node);
            N parent = this.getParent(parentId);
            N addedChild = parent.addChild(node);
            addedChild.appendToParent(parent);
            this.idMap.put(addedChild.getId(), addedChild);
        });
        // 使用并查集检测循环引用
        List<N> addedNodes = Streams.of(mergedResult)
                .filter(k -> MergeResultTypeEnum.ADD_NEW.equals(k.getResultType()))
                .map(MergeNodeResult::getResultNode).toList();
        try {
            this.detectCircularReferences(addedNodes);
        } catch (BaseException e) {
            // 检测到循环引用时，删除已添加的 mergedResult 节点以保持树的一致性
            addedNodes.forEach(this::removeNode);
            // 重新抛出异常
            throw e;
        }
        // 此时节点已加载完毕
        this.doAfter(addedNodes);
        this.getSourceElements().addAll(es);
        return root;
    }

    private void detectCircularReferences(List<N> cachedNodes) {
        I rootId = this.root.getId();
        if (Objects.nonNull(rootId)) {
            this.unionFind.makeSet(rootId);
        }
        // 初始化新节点到并查集中
        cachedNodes.forEach(k -> this.unionFind.makeSet(k.getId()));
        // 检测循环：对于每条边（节点 -> 父节点），检查它们是否已在同一集合中
        for (N node : cachedNodes) {
            List<N> parents = node.findParents();
            for (N parent : parents) {
                this.detectCircularReferences(node, parent);
            }
        }
    }

    private void detectCircularReferences(N node, N parent) {
        I parentNodeId = parent.getId();
        I id = node.getId();
        // 父节点可能是根节点，根节点的 ID 为 null
        if (Objects.isNull(id) || Objects.isNull(parentNodeId)) {
            return;
        }
        // 如果节点和父节点已经在同一个集合中，说明存在循环
        if (this.unionFind.find(id).equals(this.unionFind.find(parentNodeId))) {
            BaseExceptionEnum.CIRCULAR_REFERENCE_EXCEPTION.throwException(
                    "Circular reference detected: node {} cannot be added as child of {}", id, parentNodeId);
        }
        // 将节点和父节点加入同一个集合
        this.unionFind.union(id, parentNodeId);
    }

    /**
     * 批量处理后置事件
     *
     * @param mergedNodes 合并后的节点
     */
    protected void doAfter(List<N> mergedNodes) {
        mergedNodes.forEach(this::doAfter);
    }

    /**
     * 处理后置事件
     *
     * @param node 合并后的节点
     */
    protected void doAfter(N node) {
        node.nodeHandler();
        if (Objects.nonNull(node.getParent()) && Objects.nonNull(this.getAfterAddHandler())) {
            this.getAfterAddHandler().accept(node, node.getParent());
        }
    }

    /**
     * <p>
     * 根据父节点 ID 从树中查找对应的父节点。
     * 如果 parentId 为 null 或在树中找不到对应的节点，则返回根节点。
     * </p>
     *
     * @param parentId 父节点 ID，可以为 null
     * @return 对应的父节点，如果找不到则返回根节点
     */
    private N getParent(@Nullable I parentId) {
        N parent;
        if (Objects.isNull(parentId)) {
            parent = root;
        } else {
            parent = this.idMap.get(parentId);
            if (Objects.isNull(parent)) {
                log.warn("Parent node {} not found, using root node as parent", parentId);
                parent = root;
            }
        }
        return parent;
    }

    /**
     * 查找符合条件的所有节点
     * <p>
     * 此方法是线程安全的，通过读锁保护。
     * 返回所有满足谓词条件的节点列表。
     * </p>
     *
     * @param predicate 谓词条件函数
     * @return 符合条件的节点列表，如果没有符合条件的节点则返回空列表
     */
    public List<N> find(Predicate<N> predicate) {
        readLock.lock();
        try {
            return Streams.of(this.idMap.values()).filter(predicate).toList();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 获取符合条件的第一个节点
     * <p>
     * 此方法是线程安全的，通过读锁保护。
     * 返回第一个满足谓词条件的节点的 Optional 包装。
     * </p>
     *
     * @param predicate 谓词条件函数
     * @return Optional 对象，包含符合条件的第一个节点，或为空
     */
    public Optional<N> get(Predicate<N> predicate) {
        readLock.lock();
        try {
            return Streams.of(this.idMap.values()).filter(predicate).findFirst();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 根据 ID 获取节点
     * <p>
     * 此方法是线程安全的，通过读锁保护。
     * 直接通过 ID 查找节点，时间复杂度为 O(1)。
     * </p>
     *
     * @param id 节点 ID
     * @return 对应的节点，如果找不到则返回 null
     */
    public @Nullable N getById(I id) {
        readLock.lock();
        try {
            return this.idMap.get(id);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 删除符合条件的所有节点
     * <p>
     * 此方法是线程安全的，通过写锁保护。
     * 删除节点时会递归删除其所有子节点，并重建并查集以维护正确的连通关系。
     * </p>
     *
     * @param predicate 谓词条件函数，返回 true 表示删除该节点
     */
    public void remove(Predicate<N> predicate) {
        writeLock.lock();
        try {
            List<N> toRemove = Streams.of(this.idMap.values()).filter(predicate).toList();
            for (N node : toRemove) {
                removeNode(node);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 删除单个节点
     * <p>
     * 内部方法，执行以下步骤：
     * <ol>
     *   <li>从父节点移除</li>
     *   <li>从 idMap 和 sourceElements 中删除</li>
     *   <li>递归删除所有子节点</li>
     *   <li>清理节点引用</li>
     *   <li>重建并查集以维护一致性</li>
     * </ol>
     * </p>
     *
     * @param node 要删除的节点，可以为 null
     */
    private void removeNode(@Nullable N node) {
        if (Objects.isNull(node)) {
            return;
        }
        // 从父节点移除
        node.removeFromParent();
        Set<I> idMapToRemoveIds = HashSet.newHashSet(16);
        Set<I> sourceElementToRemoveIds = HashSet.newHashSet(16);
        // 从缓存中删除该节点
        idMapToRemoveIds.add(node.getId());
        sourceElementToRemoveIds.add(node.getId());
        // 递归删除所有子节点
        List<N> allDescendants = Node.recursiveChildren(node, false);
        for (N descendant : allDescendants) {
            idMapToRemoveIds.add(descendant.getId());
            sourceElementToRemoveIds.add(descendant.getId());
        }
        // 从缓存中删除该节点
        idMapToRemoveIds.forEach(this.idMap::remove);
        this.sourceElements.removeIf(e -> sourceElementToRemoveIds.contains(e.getId()));
        // 清理节点引用
        node.clear();

        // 删除节点后重建并查集，维持连通关系的一致性
        this.rebuildUnionFind();
    }

    /**
     * 重建并查集
     * <p>
     * 内部方法，当节点被删除或树结构改变时调用。
     * 根据当前树的所有节点和它们的父子关系重建并查集。
     * 使用延迟初始化：只在节点实际使用 root 时才添加到并查集中。
     * </p>
     */
    private void rebuildUnionFind() {
        UnionFind<I> newUnionFind = new UnionFind<>();

        // 添加所有现存节点（不提前添加 root，等使用时再添加）
        this.idMap.keySet().forEach(newUnionFind::makeSet);

        // 根据当前树的结构重建连通关系
        // 对每个节点和它的父节点进行 union
        this.idMap.forEach((id, node) -> {
            N parent = node.getParent();
            if (Objects.nonNull(parent)) {
                I parentId = parent.getId();
                if (Objects.nonNull(parentId)) {
                    // 确保父节点也在并查集中
                    newUnionFind.makeSet(parentId);
                    newUnionFind.union(id, parentId);
                }
            }
        });
        this.unionFind.rebuild(newUnionFind);
    }

    /**
     * 更新节点
     *
     * @param id      节点 ID
     * @param updater 更新函数
     * @return 是否成功更新节点
     */
    public boolean update(I id, Consumer<N> updater) {
        this.writeLock.lock();
        try {
            return this.doUpdate(id, updater);
        } finally {
            this.writeLock.unlock();
        }
    }

    public long update(Collection<I> ids, Consumer<N> updater) {
        this.writeLock.lock();
        try {
            return Streams.of(ids).map(i -> this.doUpdate(i, updater)).filter(Boolean.TRUE::equals).count();
        } finally {
            this.writeLock.unlock();
        }
    }

    /**
     * 更新节点
     * updater 不能更新 ID 的值
     *
     * @param id      节点 ID
     * @param updater 更新函数
     * @return 是否成功更新节点
     */
    protected boolean doUpdate(I id, Consumer<N> updater) {
        N n = this.idMap.get(id);
        if (Objects.isNull(n)) {
            return false;
        }
        updater.accept(n);
        // 验证 ID 未被修改
        if (!Objects.equals(id, n.getId())) {
            BaseExceptionEnum.TREE_CAN_NOT_UPDATE_ID.throwException("old id:{}, new id:{}", id, n.getId());
        }
        this.doAfter(n);
        return true;
    }

    /**
     * 获取树中的节点数量
     * <p>
     * 此方法是线程安全的，通过读锁保护。
     * 返回 idMap 中的节点数，不包括根节点。
     * </p>
     *
     * @return 树中的节点数量
     */
    public int size() {
        readLock.lock();
        try {
            return this.idMap.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 遍历树中的所有节点
     * <p>
     * 此方法是线程安全的，通过读锁保护。
     * 对 idMap 中的每个条目执行给定的二元操作。
     * </p>
     *
     * @param biConsumer 二元操作，参数为节点 ID 和节点对象
     */
    public void forEach(BiConsumer<I, N> biConsumer) {
        readLock.lock();
        try {
            this.idMap.forEach(biConsumer);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 树类型转换
     * <p>
     * 将当前树的元素转换为另一种类型的元素，并创建新的树。
     * 这对于需要改变元素类型或节点类型的场景很有用。
     * </p>
     *
     * @param emptyTreeSupplier 供应者函数，用于创建新的空树
     * @param eleMapper         元素映射函数，用于转换元素类型
     * @param <I2>              新树的 ID 类型
     * @param <E2>              新树的元素类型
     * @param <N2>              新树的节点类型
     * @param <T2>              新树的类型
     * @return 转换后的新树
     */
    public <I2 extends Comparable<I2>, E2 extends Element<I2>, N2 extends AbstractNode<I2, E2, N2>, T2 extends Tree<I2, E2, N2>> T2 cast(
            Supplier<T2> emptyTreeSupplier, Function<E, E2> eleMapper) {
        this.writeLock.lock();
        try {
            T2 emptyTree2 = emptyTreeSupplier.get();
            List<E2> list = Streams.of(this.getSourceElements()).map(eleMapper).toList();
            emptyTree2.add(list);
            return emptyTree2;
        } finally {
            this.writeLock.unlock();
        }
    }

    /**
     * 清空树中的所有节点
     * <p>
     * 此方法是线程安全的，通过写锁保护。
     * 删除所有数据，并将树恢复到初始状态。
     * 根节点的引用会保留。
     * </p>
     */
    public void clear() {
        this.writeLock.lock();
        try {
            this.idMap.clear();
            this.sourceElements.clear();
            this.root.clear();
            // 清空并查集（下次 add() 时会重新添加 root）
            this.unionFind.clear();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tree<?, ?, ?> tree = (Tree<?, ?, ?>) o;
        return Objects.equals(getRoot(), tree.getRoot());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getRoot());
    }

    @Override
    public String toString() {
        return Jsons.str(this.getRoot());
    }
}