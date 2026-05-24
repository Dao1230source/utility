package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.tree.define.*;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.source.utility.utils.UnionFind;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * @param <I> ID 类型，必须实现 Comparable 接口，推荐I使用String类型，各种特殊类型都能满足
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

    /**
     * 读写锁，用于保护树的并发操作
     * 所有写操作（add、remove、clear）使用写锁，所有读操作（find、get、getById）使用读锁
     */
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
     * {@literal <node, parentNode>}
     */
    private @Nullable BiConsumer<N, N> afterAddHandler;

    /**
     * ID扩展，元素对象除了ID之外的唯一键
     */
    private IdExtend<I, E, N> idExtend = IdExtend.defaultIdExtend();

    /**
     * 额外扩展的属性缓存，提升forEach等性能
     * {@literal <元素字段值,Node>}
     */
    private Map<String, Map<I, N>> extendCachMap = new ConcurrentHashMap<>();
    /**
     * ID扩展配置列表，用于定义额外的缓存索引
     * 每个IdExtend定义了一个属性名称和对应的ID获取函数
     * 通过{@link #setExtendCacheIdExtends(List)}方法设置
     * 设置后会自动初始化{@link #extendCachMap}中对应的缓存Map
     */
    private List<IdExtend<I, E, N>> extendCacheIdExtends = List.of();

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
        // 转换元素为节点
        List<N> toAddNodes = Streams.map(es, e -> {
            N n = this.getRoot().emptyNode();
            n.setElement(e);
            if (Objects.nonNull(this.getAfterCreateHandler())) {
                this.getAfterCreateHandler().accept(n);
            }
            return n;
        }).toList();
        List<N> addedNodes = new ArrayList<>(toAddNodes.size());
        // 与旧节点合并
        List<N> mergedNodes = Streams.map(toAddNodes, n -> {
            MergeNodeResult<I, E, N> mergedResult = AbstractNode.mergeNode(
                    n,
                    this.getIdExtend().getIdGetter(),
                    i -> this.obtainNodeFromCache(i, this.getIdExtend()),
                    this.getMergeHandler());
            N resultNode = mergedResult.getResultNode();
            if (MergeResultTypeEnum.ADD_NEW.equals(mergedResult.getResultType())) {
                addedNodes.add(resultNode);
            }
            // 先将所有元素缓存，避免有可能父级数据在后，当前元素加入时找不到父级的情况
            this.cacheNode(resultNode);
            return resultNode;
        }).filter(Objects::nonNull).toList();
        // 添加节点
        mergedNodes.forEach(node -> {
            N parent = this.getParent(node, this.getIdExtend());
            N addedChild = parent.addChild(node);
            this.checkIdExtendsUnmodified(node, addedChild);
            addedChild.appendToParent(parent);
            // 如果节点的引用变了，重新缓存
            if (node != addedChild) {
                this.cacheNode(addedChild);
            }
        });
        // 使用并查集检测循环引用
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

    /**
     * 获取ID扩展配置
     * <p>
     * 如果idExtend为null，则返回默认的ID扩展配置（使用节点ID作为唯一键）
     * </p>
     *
     * @return ID扩展配置，不会返回null
     */
    public IdExtend<I, E, N> getIdExtend() {
        if (Objects.isNull(this.idExtend)) {
            this.idExtend = IdExtend.defaultIdExtend();
        }
        return this.idExtend;
    }

    /**
     * 设置扩展缓存配置列表
     * <p>
     * 配置额外的属性缓存索引，用于通过非ID属性快速查找节点
     * 设置后会自动初始化{@link #extendCachMap}中对应的缓存Map
     * </p>
     * <p>
     * 注意：应该在调用{@link #add(Collection)}之前设置，否则缓存可能不完整
     * </p>
     *
     * @param extendsList 扩展配置列表，每个IdExtend定义一个缓存索引
     */
    public void setExtendCacheIdExtends(List<IdExtend<I, E, N>> extendsList) {
        this.extendCacheIdExtends = List.copyOf(extendsList);
        // 初始化对应的缓存 Map
        for (IdExtend<I, E, N> ext : extendsList) {
            this.extendCachMap.putIfAbsent(ext.getName(), new ConcurrentHashMap<>());
        }
    }

    /**
     * 检测循环引用
     * <p>
     * 使用并查集（Union-Find）算法检测新添加的节点是否会形成循环引用
     * 对于每条边（节点 -> 父节点），检查它们是否已在同一集合中
     * </p>
     *
     * @param cachedNodes 新添加的节点列表
     * @throws BaseException 如果检测到循环引用
     */
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

    /**
     * 检测单个节点与父节点之间是否存在循环引用
     * <p>
     * 如果节点和父节点已在同一集合中，说明存在循环，抛出异常
     * 否则将它们加入同一集合
     * </p>
     *
     * @param node   当前节点
     * @param parent 父节点
     * @throws BaseException 如果检测到循环引用
     */
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
     * 获取所有ID扩展配置
     * <p>
     * 合并{@link #idExtend}和{@link #extendCacheIdExtends}，返回完整的ID扩展列表
     * 使用{@link #getIdExtend()}确保idExtend不为null
     * </p>
     *
     * @return 所有ID扩展配置列表，不会包含null元素
     */
    private List<IdExtend<I, E, N>> obtainAllIdExtends() {
        return Stream.concat(Streams.of(this.getIdExtend()), Streams.of(this.extendCacheIdExtends)).toList();
    }

    /**
     * 缓存节点到idMap和扩展缓存中
     * <p>
     * 将节点添加到主ID缓存{@link #idMap}中
     * 同时根据{@link #extendCacheIdExtends}配置，将节点添加到对应的扩展缓存中
     * </p>
     *
     * @param node 要缓存的节点
     */
    protected void cacheNode(N node) {
        this.idMap.put(node.getId(), node);
        Streams.of(this.obtainAllIdExtends())
                .filter(k -> !IdExtend.ID_NAME.equals(k.getName()))
                .forEach(k -> this.extendCachMap.computeIfAbsent(k.getName(), name -> new ConcurrentHashMap<>())
                        .put(k.getIdGetter().apply(node), node));
    }

    /**
     * 从扩展缓存中移除节点
     * <p>
     * 根据{@link #extendCacheIdExtends}配置，从{@link #extendCachMap}中移除节点
     * 只移除节点本身，不包括其子节点
     * </p>
     *
     * @param node 要移除的节点
     */
    private void removeCache(N node) {
        this.idMap.remove(node.getId());
        Streams.of(this.obtainAllIdExtends()).filter(k -> !IdExtend.ID_NAME.equals(k.getName()))
                .forEach(k -> {
                    Map<I, N> inMap = this.extendCachMap.get(k.getName());
                    if (Objects.nonNull(inMap)) {
                        inMap.remove(k.getIdGetter().apply(node));
                    }
                });
    }


    /**
     * 检查合并节点的ID扩展属性未被修改
     * <p>
     * 在节点合并时，验证新节点和旧节点的ID及扩展ID属性是否相同
     * 如果不同则抛出异常，确保合并不会破坏缓存索引的一致性
     * </p>
     *
     * @param o 合并后的节点（旧节点）
     * @param n 新节点
     * @throws BaseException 如果ID或扩展ID属性不一致
     */
    protected void checkIdExtendsUnmodified(N o, N n) {
        BaseExceptionEnum.TREE_MERGED_NODE_ID_MUST_EQUAL.isTrue(Objects.equals(n.getId(), o.getId()),
                "new id:{}, old id:{}", n.getId(), o.getId());
        Streams.of(this.obtainAllIdExtends()).filter(k -> !IdExtend.ID_NAME.equals(k.getName())).forEach(k -> {
            I nValue = k.getIdGetter().apply(n);
            I oValue = k.getIdGetter().apply(o);
            BaseExceptionEnum.TREE_MERGED_NODE_ID_MUST_EQUAL.isTrue(Objects.equals(nValue, oValue),
                    "new key:{}, old key:{}", nValue, oValue);
        });
    }

    /**
     * 获取节点的父节点
     * <p>
     * 根据节点ID扩展配置，从缓存中查找对应的父节点
     * 如果找不到父节点或父节点ID为null，则返回根节点
     * </p>
     *
     * @param n        当前节点
     * @param idExtend ID扩展配置，用于获取父节点ID
     * @return 对应的父节点，如果找不到则返回根节点
     */
    private N getParent(N n, IdExtend<I, E, N> idExtend) {
        I id = idExtend.getParentIdGetter().apply(n);
        N parent = this.obtainNodeFromCache(id, idExtend);
        if (Objects.nonNull(parent)) {
            return parent;
        }
        parent = root;
        return parent;
    }

    /**
     * 从缓存中获取节点
     * <p>
     * 根据ID和ID扩展配置，从对应的缓存中查找节点
     * 如果idExtend是默认配置（使用ID），则从{@link #idMap}中查找
     * 否则从{@link #extendCachMap}中对应的扩展缓存中查找
     * </p>
     *
     * @param id       节点ID或扩展属性值
     * @param idExtend ID扩展配置
     * @return 找到的节点，如果未找到则返回null
     */
    public @Nullable N obtainNodeFromCache(@Nullable I id, IdExtend<I, E, N> idExtend) {
        N old = null;
        if (Objects.isNull(id)) {
            return old;
        }
        if (idExtend.isDefault()) {
            old = this.idMap.get(id);
        } else {
            Map<I, N> inMap = this.extendCachMap.get(idExtend.getName());
            if (Objects.nonNull(inMap)) {
                old = inMap.get(id);
            }
        }
        if (Objects.isNull(old)) {
            log.info("node:{} not found", id);
        }
        return old;
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
     * 根据扩展属性名称和键值获取节点
     * <p>
     * 从{@link #extendCachMap}中根据属性名称查找对应的缓存Map，
     * 再根据键值获取节点
     * 此方法是线程安全的，通过读锁保护
     * </p>
     *
     * @param fieldName 扩展属性名称，对应{@link IdExtend#getName()}
     * @param key       属性值
     * @return Optional包装的节点，如果未找到则为空Optional
     */
    public Optional<N> getByKey(String fieldName, I key) {
        readLock.lock();
        try {
            return Optional.ofNullable(this.extendCachMap.get(fieldName)).map(k -> k.get(key));
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
     *   <li>从缓存中删除节点本身及所有子节点</li>
     *   <li>从sourceElements中删除</li>
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
        // 获取所有子节点
        List<N> allDescendants = Node.recursiveChildren(node, true);
        // 从缓存中删除节点本身及所有子节点
        allDescendants.forEach(this::removeCache);
        // 从源元素中删除
        Set<I> sourceElementToRemoveIds = Streams.map(allDescendants, Node::getId).collect(Collectors.toSet());
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

    /**
     * 批量更新节点
     * <p>
     * 对多个节点执行更新操作，返回成功更新的节点数量
     * 此方法是线程安全的，通过写锁保护
     * </p>
     * <p>
     * 注意：updater不能修改节点的ID值，否则会抛出异常
     * </p>
     *
     * @param ids     要更新的节点ID集合
     * @param updater 更新函数，接收节点并执行更新操作
     * @return 成功更新的节点数量
     */
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
            this.extendCachMap.clear();
            this.extendCacheIdExtends = List.of();
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