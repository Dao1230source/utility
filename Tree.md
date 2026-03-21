# Tree 组件完整指南

**版本**: 2.1
**更新时间**: 2026-03-04
**状态**: ✅ 生产就绪

---

## 📚 目录

1. [概述](#概述)
2. [核心特性](#核心特性)
3. [架构设计](#架构设计)
4. [节点类型](#节点类型)
5. [使用指南](#使用指南)
6. [API 参考](#api-参考)
7. [进阶用法](#进阶用法)
8. [最佳实践](#最佳实践)
9. [代码示例](#代码示例)

---

## 概述

**Tree** 是一个通用的树形数据结构容器，支持灵活的树形和DAG（有向无环图）结构。它提供了并发安全、循环引用检测等高级功能，适用于组织结构、分类体系、菜单系统等多种场景。

### 为什么开发这个组件？

希望以树形结构数据为核心，连接万事万物。树形结构是现实世界中最常见的数据组织方式，无论是组织体系、文件系统、知识图谱还是业务流程，都可以用树或DAG来表示。

### 核心优势

- ✅ **泛型设计** - 支持任意ID类型和元素类型
- ✅ **并发安全** - 基于读写锁的线程安全实现（支持1000+并发操作）
- ✅ **循环检测** - 使用Union-Find算法自动检测循环引用
- ✅ **类型多样** - 提供默认、深度、扁平、增强等多种节点实现
- ✅ **JSON友好** - 完整的Jackson序列化支持
- ✅ **高性能** - O(1)的ID查询，O(n·α(n))的循环检测

### 基本概念

- **I** - 唯一键，元素的唯一标识，可以是任意可比较的类型（String、Integer等）
- **E extends Element<I>** - 挂载在节点上的元素，树中实际存储的业务数据
- **N extends AbstractNode<I, E, N>** - 节点，树中的节点对象，包含元素和关系信息

### demo
[更多使用案例](https://github.com/Dao1230source/demo/tree/main/utility/tree)   
[性能测试报告](https://github.com/Dao1230source/demo/blob/main/utility/tree/JMH-REPORT.md)

---

## 核心特性

### 1. 并发安全（ReentrantReadWriteLock）

```
┌─────────────────────────────────┐
│      Tree<I, E, N>              │
├─────────────────────────────────┤
│  ReentrantReadWriteLock         │
│  ├─ writeLock (add/remove)      │  ← 独占写访问
│  └─ readLock (find/get)         │  ← 共享读访问
├─────────────────────────────────┤
│  ConcurrentHashMap<I, N>        │
│  └─ 节点ID到节点对象的映射      │
├─────────────────────────────────┤
│  UnionFind<I>                   │
│  └─ 循环引用检测数据结构        │
└─────────────────────────────────┘
```

**读锁保护的操作：**
- `find(predicate)` - 查找符合条件的节点，O(n)
- `get(predicate)` - 获取第一个符合条件的节点
- `getById(id)` - 按ID快速查找，O(1) ⭐
- `forEach(biConsumer)` - 遍历所有节点，O(n)

**写锁保护的操作：**
- `add(elements)` - 添加元素集合，O(n·m)（n=元素数，m=平均树深度）
- `remove(predicate)` - 删除符合条件的节点，O(n)
- `clear()` - 清空树，O(n)

### 2. 循环引用检测（Union-Find）

使用**并查集（Union-Find）算法**实现O(n·α(n))的循环检测：

```
添加节点时的检测流程：
1. 初始化所有新节点到并查集
2. 对每条边(节点→父节点)检查
3. 若find(节点ID) == find(父节点ID) → 检测到循环 → 抛异常
4. 否则执行union(节点ID, 父节点ID)

示例：
树结构：A → B → C
添加边 C → A 时：
  - find(C) = root_A
  - find(A) = root_A
  - 相同！说明C和A已连通 → 循环检测！
```

**优势：**
- 检测任意深度的循环
- 支持多父节点DAG
- 自动处理复杂的循环场景
- 时间复杂度接近O(n)（α为逆阿克曼函数，实际可视为常数）

### 3. 处理器机制（2个钩子点）

Tree提供两个处理器钩子点，用于自定义业务逻辑：

```java
// 1️⃣ 节点创建后处理器
// 在节点创建完成后、添加到树前调用
tree.setAfterCreateHandler(node -> {
    node.getElement().setCreateTime(System.currentTimeMillis());
});

// 2️⃣ 节点添加后处理器
// 在节点添加到树后调用，可获得parent信息
tree.setAfterAddHandler((node, parent) -> {
    node.getElement().setLevel(
        parent.getElement() == null ? 0 :
        parent.getElement().getLevel() + 1
    );
});
```

### 5. 灵活的ID和父ID提取

```java
// 默认：调用 element.getId()
// 自定义ID获取方式
tree.setIdGetter(node ->
    node.getElement().getCustomId()
);

// 默认：调用 element.getParentId()
// 自定义父ID获取方式
tree.setParentIdGetter(node ->
    node.getElement().getParentCode()
);
```

---

## 架构设计

- [架构UML图](Tree_Architecture.png)

### 数据结构

```
Tree<I, E, N>
├── root: N                          // 虚拟根节点（element=null）
├── sourceElements: List<E>          // 源元素副本（用于序列化和重建）
├── idMap: ConcurrentHashMap<I, N>   // ID→节点映射（O(1)查找）
├── unionFind: UnionFind<I>          // 循环检测用并查集（延迟初始化）
└── locks: ReadWriteLock             // 并发控制锁
    ├── writeLock                    // 写操作独占锁
    └── readLock                     // 读操作共享锁
```

### 节点继承体系

```
Node<I, E, N> (接口)
    ↑
    │ implements
    │
AbstractNode<I, E, N> (抽象基类)
├── DefaultNode          (最简单，无特性)
├── DeepNode            (支持深度计算)
├── FlatNode            (属性扁平化)
├── EnhanceNode         (多父节点DAG) ✨
│   └── DefaultEnhanceNode
└── 自定义扩展...
```

### 操作流程图

#### 添加元素流程

```
add(elements)
  │
  ├─ 获取writeLock（独占）
  │
  └─ doAdd(elements)
      │
      ├─ 步骤1: 创建节点对象
      │   ├─ for each element:
      │   │   ├─ n = root.emptyNode()
      │   │   ├─ n.setElement(element)
      │   │   └─ afterCreateHandler(n)
      │   └─ toAddNodes: List<N>
      │
      ├─ 步骤2: 缓存节点（解决父级在后问题）
      │   ├─ for each n in toAddNodes:
      │   │   ├─ id = idGetter(n)
      │   │   └─ createAndCacheNode(id, n, idMap)
      │   └─ cachedNodes: List<N>
      │
      ├─ 步骤3: 建立父子关系
      │   ├─ for each node in cachedNodes:
      │   │   ├─ parentId = parentIdGetter(node)
      │   │   ├─ parent = getParent(parentId)
      │   │   ├─ parent.addChild(node)
      │   │   └─ node.appendToParent(parent)
      │   └─ 关系建立完成
      │
      ├─ 步骤4: 循环引用检测
      │   └─ detectCircularReferences(cachedNodes)
      │       ├─ 初始化unionFind
      │       ├─ for each node, parent:
      │       │   └─ union(nodeId, parentId)
      │       └─ 无异常 = 无循环
      │
      ├─ 步骤5: 节点处理
      │   ├─ for each node:
      │   │   ├─ node.nodeHandler()
      │   │   └─ afterAddHandler(node, parent)
      │   └─ 处理完成
      │
      └─ 释放writeLock
```

#### 删除节点流程

```
remove(predicate)
  │
  ├─ 获取writeLock（独占）
  │
  └─ for each matching node:
      └─ removeNode(node)
          ├─ node.removeFromParent()
          ├─ 从idMap删除该节点
          ├─ 从sourceElements删除该节点
          ├─ 递归删除所有子节点
          │   └─ Node.recursiveChildren(node)
          ├─ node.clear() (释放引用)
          └─ rebuildUnionFind()
              └─ 根据当前树结构重建unionFind
```

---

## 节点类型

### 1️⃣ DefaultNode - 默认节点

最简单的树节点实现，继承AbstractNode的所有功能。

```java
// 创建树
Tree<Integer, MyElement, DefaultNode<Integer, MyElement>> tree =
    Tree.of(new DefaultNode<>());

// 特点：
// - 最小化实现
// - 最好性能
// - 标准父子关系（单父节点）

// 适用场景：
// ✓ 简单树结构
// ✓ 标准父子关系
// ✓ 性能敏感的场景
```

### 2️⃣ DeepNode - 深度节点

支持自动计算节点深度。

```java
// rootIsZero=true: 根节点深度为0，向下递增
Tree<Integer, MyElement, DeepNode<Integer, MyElement>> tree1 =
    Tree.of(new DeepNode<>(true));

// rootIsZero=false: 叶节点深度为0，向上递增
Tree<Integer, MyElement, DeepNode<Integer, MyElement>> tree2 =
    Tree.of(new DeepNode<>(false));

// 获取节点深度
Integer depth = node.getDepth();

// 适用场景：
// ✓ 组织结构（层级管理）
// ✓ 菜单系统（嵌套菜单）
// ✓ 分类体系（多级分类）
// ✓ 需要知道节点的绝对或相对深度
```

**深度计算示例：**

```
rootIsZero=true 时（从根向下）：
        root       ← 根节点深度是0
        /  \
       A    B      ← A,B的深度都是1
      /
     A1            ← A1的深度是2

rootIsZero=false 时（从叶向上）：
        root       ← 根节点深度是2
        /  \
       A    B      ← A的深度是1，B是叶子节点深度是0
      /
     A1            ← A1的深度是0（叶子）
```

### 3️⃣ EnhanceNode - 增强节点（支持DAG）✨

支持多父节点和有序子节点，用于构建有向无环图（DAG）。

```java
// 创建树
Tree<Integer, MyElement, EnhanceNode<Integer, MyElement>> tree =
    Tree.of(new EnhanceNode<>());

// 特点：
// - 一个节点可以有多个父节点
// - 子节点自动排序（TreeSet）
// - 完整的DAG支持

// 获取所有父节点
List<EnhanceNode<Integer, MyElement>> parents = node.findParents();

// 获取有序的子节点
List<EnhanceNode<Integer, MyElement>> children = node.getChildren();

// 适用场景：
// ✓ 文件系统（符号链接）
// ✓ 知识图谱（概念关系）
// ✓ 任务依赖（任务可依赖多个其他任务）
// ✓ 权限管理（用户属于多个角色/部门）
```

**多父节点示例：**

```
创建DAG结构：
      A
     / \
    B   C
     \ /
      D  ← D有两个父节点(B, C)

tree.add([
    new Element(1, null, "A"),       // A是根
    new Element(2, 1, "B"),          // B的父是A
    new Element(3, 1, "C"),          // C的父是A
    new Element(4, 2, "D"),          // D的父是B（首次添加）
    new Element(4, 3, "D")           // D的父是C（第二次添加，保留旧数据）
]);

// 或使用EnhanceElement自定义
```

### 4️⃣ FlatNode - 扁平节点

将元素的属性扁平化为JSON节点属性。

```java
// 定义属性提取函数
List<SFunction<MyElement, Object>> getters = Arrays.asList(
    MyElement::getId,
    MyElement::getName,
    MyElement::getCode,
    MyElement::getDescription
);

FlatNode<Integer, MyElement> root = new FlatNode<>(getters);
Tree<Integer, MyElement, FlatNode<Integer, MyElement>> tree =
    Tree.of(root);

// JSON序列化时，属性直接在节点上
{
  "id": 1,
  "name": "root",
  "code": "ROOT",
  "description": "根节点",
  "children": [...]
}

// 适用场景：
// ✓ RESTful API响应
// ✓ 数据转换和聚合
// ✓ 需要扁平化输出的树形结构
```

---

## 使用指南

### 🚀 快速开始（5分钟）

#### 第1步：定义Element

```java
public class DeptElement implements Element<Integer> {
    private Integer id;
    private Integer parentId;
    private String name;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public Integer getParentId() {
        return parentId;
    }

    // 省略其他代码...
}
```

#### 第2步：创建Tree

```java
// 方式1：最简单，选择合适的节点类型
Tree<Integer, DeptElement, DefaultNode<Integer, DeptElement>> tree =
    Tree.of(new DefaultNode<>());

// 方式2：配置处理器（可选）
tree.setAfterAddHandler((node, parent) -> {
    node.getElement().setLevel(
        parent.getElement() == null ? 0 :
        parent.getElement().getLevel() + 1
    );
});
```

#### 第3步：添加数据

```java
List<DeptElement> depts = Arrays.asList(
    new DeptElement(1, null, "公司"),       // 根节点
    new DeptElement(2, 1, "技术部"),        // 子节点
    new DeptElement(3, 1, "市场部"),
    new DeptElement(4, 2, "开发组"),        // 孙节点
    new DeptElement(5, 2, "测试组")
);

tree.add(depts);  // 一次操作添加所有元素
```

#### 第4步：查询数据

```java
// 按ID查询（最快，O(1)）⭐
DefaultNode<Integer, DeptElement> node = tree.getById(2);

// 条件查询
List<DefaultNode<Integer, DeptElement>> techDepts = tree.find(
    n -> n.getElement().getName().contains("技术")
);

// 获取第一个匹配
Optional<DefaultNode<Integer, DeptElement>> first = tree.get(
    n -> n.getId() > 10
);

// 遍历所有节点
tree.forEach((id, node) -> {
    System.out.println(id + " -> " + node.getElement().getName());
});
```

#### 第5步：删除和清理

```java
// 删除特定节点（会级联删除子节点）
tree.remove(n -> n.getId() == 3);

// 删除多个节点
tree.remove(n -> n.getId() > 5);

// 清空树
tree.clear();
```

### 📖 详细用法

#### 自定义ID获取

```java
// 默认使用 element.getId()
// 如果需要自定义ID提取逻辑：

tree.setIdGetter(node -> {
    // 从element的自定义字段获取ID
    return node.getElement().getUniqueCode();
});

tree.setParentIdGetter(node -> {
    // 从element的自定义字段获取父ID
    return node.getElement().getParentCode();
});
```

---

## API 参考

### Tree 核心API

| 方法 | 参数 | 返回值 | 时间复杂度 | 说明 |
|------|------|--------|----------|------|
| `of(root)` | N | Tree | O(1) | 工厂方法，创建树实例 |
| `add(elements)` | Collection | N | O(n·m) | 批量添加元素，n=元素数，m=平均树深度 |
| `find(predicate)` | Predicate | List | O(n) | 查找符合条件的所有节点 |
| `get(predicate)` | Predicate | Optional | O(n) | 获取第一个符合条件的节点 |
| `getById(id)` | I | N | **O(1)** | 按ID查询，最快！ |
| `remove(predicate)` | Predicate | void | O(n) | 删除符合条件的节点，级联删除子节点 |
| `clear()` | - | void | O(n) | 清空树，释放所有资源 |
| `size()` | - | int | O(1) | 获取树中节点数 |
| `forEach(consumer)` | BiConsumer | void | O(n) | 遍历所有节点 |
| `cast(supplier, mapper)` | 两个函数 | Tree | O(n) | 类型转换，创建新树 |

### AbstractNode API

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getElement()` | - | E | 获取节点的业务元素 |
| `setElement(e)` | E | void | 设置节点的业务元素 |
| `getParent()` | - | N | 获取父节点 |
| `getChildren()` | - | List<N> | 获取子节点列表（不可修改副本）|
| `getId()` | - | I | 获取节点ID（来自element） |
| `getParentId()` | - | I | 获取父节点ID（来自element） |
| `addChild(child)` | N | void | 添加子节点 |
| `removeChild(child)` | N | void | 移除子节点 |
| `removeFromParent()` | - | void | 从父节点移除此节点 |
| `findParents()` | - | List<N> | 获取所有父节点（DefaultNode返回0-1个，EnhanceNode返回多个） |
| `emptyNode()` | - | N | 创建空节点，子类必须实现 |
| `nodeHandler()` | - | void | 节点处理钩子，子类可覆盖 |
| `clear()` | - | void | 清理节点所有引用，释放内存 |

### 静态工具方法

```java
// 获取节点到根的路径
List<N> superiorNodes = Node.superiorNodes(node, includeItself=true);
// 返回: [node, parent, grandparent, ..., root]

// 获取节点的所有递归子节点
List<N> descendants = Node.recursiveChildren(node, includeItself=true);
// 返回: [node, child1, child1.1, child2, ...]
```

---

## 进阶用法

### 1. 树类型转换

```java
// 场景：从DefaultNode树转换到DeepNode树
Tree<Integer, DeptElement, DefaultNode<Integer, DeptElement>>
    sourceTree = Tree.of(new DefaultNode<>());
sourceTree.add(elements);

Tree<Integer, DeptElement, DeepNode<Integer, DeptElement>>
    targetTree = sourceTree.cast(
        () -> Tree.of(new DeepNode<>(true)),  // 创建新树
        element -> element                      // 元素映射（这里不变）
    );

// 现在targetTree中的所有节点都自动计算了深度！
```

### 2. 多父节点DAG

```java
// 创建EnhanceNode树（支持多父节点）
Tree<Integer, MyElement, EnhanceNode<Integer, MyElement>> dagTree =
    Tree.of(new EnhanceNode<>());

// 添加数据（支持同一个节点有多个父节点）
dagTree.add(elements);

// 查询节点的所有父节点
EnhanceNode<Integer, MyElement> node = dagTree.getById(someId);
List<EnhanceNode<Integer, MyElement>> parents = node.findParents();

// 遍历所有父节点
parents.forEach(parent -> {
    System.out.println("Parent: " + parent.getElement().getName());
});

// 子节点自动排序
List<EnhanceNode<Integer, MyElement>> sortedChildren = node.getChildren();
// 返回按自定义Comparator排序的子节点
```

### 3. 属性扁平化（FlatNode）

```java
// 定义属性提取函数
List<SFunction<MyElement, Object>> propertyGetters = Arrays.asList(
    element -> element.getId(),
    element -> element.getName(),
    element -> element.getCode(),
    element -> element.getDescription(),
    element -> element.getCreateTime()
);

FlatNode<Integer, MyElement> root = new FlatNode<>(propertyGetters);
Tree<Integer, MyElement, FlatNode<Integer, MyElement>> flatTree =
    Tree.of(root);

flatTree.add(elements);

// 序列化为JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writerWithDefaultPrettyPrinter()
    .writeValueAsString(flatTree.getRoot());

// JSON输出（属性直接在节点上）：
// {
//   "id": 1,
//   "name": "root",
//   "code": "ROOT",
//   "description": "根节点",
//   "createTime": 1234567890,
//   "children": [...]
// }
```

### 4. 深度自动计算

```java
// 方式1：从根向下（根=0）
DeepNode<Integer, MyElement> root1 = new DeepNode<>(true);
Tree<Integer, MyElement, DeepNode<Integer, MyElement>> tree1 = Tree.of(root1);

// 方式2：从叶向上（叶=0）
DeepNode<Integer, MyElement> root2 = new DeepNode<>(false);
Tree<Integer, MyElement, DeepNode<Integer, MyElement>> tree2 = Tree.of(root2);

tree1.add(elements);

// 深度自动计算
tree1.forEach((id, node) -> {
    int depth = node.getDepth();
    System.out.println(
        "  ".repeat(depth) +
        node.getElement().getName()
    );
});

// 输出：
// root
//   child1
//     grandchild1
//   child2
```

### 5. 循环引用检测

```java
// 自动检测循环
List<Element> circularData = Arrays.asList(
    new Element(1, null),   // 根
    new Element(2, 1),      // 1的子节点
    new Element(3, 2),      // 2的子节点
    new Element(1, 3)       // 试图让1成为3的子节点（形成循环！）
);

try {
    tree.add(circularData);
} catch (Exception e) {
    // 捕获异常：
    // Circular reference detected: node 1 cannot be added as child of 3
    System.err.println(e.getMessage());
}

// 循环检测是自动的，无需手动处理
```

---

## 最佳实践

### ✅ 做这些事

#### 1️⃣ 选择合适的节点类型

```java
// 指导原则：选择功能最少的能满足需求的类型
if (needMultipleParents) {
    // 支持多父节点DAG
    tree = Tree.of(new EnhanceNode<>());
} else if (needDepthCalculation) {
    // 需要深度计算
    tree = Tree.of(new DeepNode<>(true));
} else if (needFlattenJSON) {
    // 需要属性扁平化
    tree = Tree.of(new FlatNode<>(getters));
} else {
    // 简单树结构，最好性能
    tree = Tree.of(new DefaultNode<>());  // ⭐ 推荐
}
```

#### 2️⃣ 批量添加，一次操作

```java
// ✅ 好的做法：一次add()操作
List<Element> allElements = loadAllData();
tree.add(allElements);  // 一次写锁

// ❌ 避免：多次add()导致多次加锁
for (Element element : allElements) {
    tree.add(Collections.singletonList(element));  // N次写锁，性能差
}
```

#### 3️⃣ 数据预处理

```java
// ✅ 好的做法：在add()前确保数据完整
tree.setAfterCreateHandler(node -> {
    if (node.getElement().getCreateTime() == null) {
        node.getElement().setCreateTime(System.currentTimeMillis());
    }
});
tree.add(elements);

// ❌ 避免：在add()后补充数据
tree.add(elements);
elements.forEach(e -> e.setCreateTime(System.currentTimeMillis()));
```

#### 4️⃣ 充分利用高效查询

```java
// ✅ 按ID查询（最快，O(1)）
Node node = tree.getById(id);  // 毫秒级

// ❌ 低效的查询方式
Optional<Node> node = tree.find(n -> n.getId().equals(id));  // O(n)，可能秒级
```

#### 5️⃣ 合理使用处理器

```java
// ✅ 处理器用于初始化和校验
tree.setAfterAddHandler((node, parent) -> {
    // 初始化派生属性
    node.getElement().setPath(parent.getElement().getPath() + "/" + node.getId());
    // 校验数据
    if (node.getElement().getName().isEmpty()) {
        throw new IllegalArgumentException("Name cannot be empty");
    }
});

// ❌ 避免：在处理器中进行复杂业务逻辑
tree.setAfterAddHandler((node, parent) -> {
    // 不要做这种复杂操作
    saveToDatabase(node);              // 阻塞操作
    notifyRemoteServer(node);          // 网络操作
    computeComplexMetrics(node);       // 耗时操作
});
```

### ❌ 避免这些事

#### 1️⃣ 避免频繁的小规模add/remove

```
❌ 坏的做法：
for (int i = 0; i < 1000; i++) {
    tree.add(Collections.singletonList(element));  // 1000次加锁
}

✅ 好的做法：
tree.add(list1000);  // 1次加锁
```

#### 2️⃣ 避免在处理器中阻塞

```
❌ 坏的做法：
tree.setAfterAddHandler((node, parent) -> {
    // 在处理器中做I/O操作会严重影响性能
    database.save(node);         // 数据库操作
    httpClient.post(url, node);  // HTTP请求
});

✅ 好的做法：
tree.setAfterAddHandler((node, parent) -> {
    // 处理器只做轻量级操作
    node.getElement().setLevel(parent.getElement().getLevel() + 1);
});
// 数据库保存在外部处理
```

#### 3️⃣ 避免直接修改获取的节点

```
❌ 坏的做法：
Node node = tree.getById(id);
node.getElement().setName("new-name");  // 直接修改可能有并发问题

✅ 好的做法：
Node node = tree.getById(id);
Element newElement = node.getElement().copy();
newElement.setName("new-name");
tree.remove(n -> n.getId().equals(id));
tree.add(Collections.singletonList(newElement));  // 通过tree更新
```

#### 4️⃣ 避免长期保持大量节点

```
❌ 坏的做法：
// 程序运行3年，不删除任何节点
while (true) {
    tree.add(dailyData);  // 每天添加，从不清理
}

✅ 好的做法：
// 定期清理过期数据
if (needsCleanup) {
    tree.remove(n -> n.getCreateTime() < cutoffTime);
}
```

---

## 代码示例

### 示例1：部门组织结构

```java
public class DepartmentExample {

    record Dept(Integer id, Integer parentId, String name, int level)
        implements Element<Integer> {
        @Override public Integer getId() { return id; }
        @Override public Integer getParentId() { return parentId; }
    }

    public static void main(String[] args) {
        // 1. 创建树
        Tree<Integer, Dept, DefaultNode<Integer, Dept>> tree =
            Tree.of(new DefaultNode<>());

        // 2. 设置处理器计算层级
        tree.setAfterAddHandler((node, parent) -> {
            Dept parentDept = parent.getElement();
            int level = parentDept == null ? 0 : parentDept.level() + 1;
            // 需要重新构建Dept对象（record是不可变的）
        });

        // 3. 添加数据
        List<Dept> depts = Arrays.asList(
            new Dept(1, null, "集团总部", 0),
            new Dept(2, 1, "技术中心", 1),
            new Dept(3, 1, "市场部", 1),
            new Dept(4, 2, "开发部", 2),
            new Dept(5, 2, "测试部", 2),
            new Dept(6, 4, "后端组", 3),
            new Dept(7, 4, "前端组", 3)
        );
        tree.add(depts);

        // 4. 树形打印
        tree.forEach((id, node) -> {
            Dept dept = node.getElement();
            System.out.println("  ".repeat(dept.level()) + dept.name());
        });

        // 5. 查询
        DefaultNode<Integer, Dept> techDept = tree.getById(2);
        System.out.println("\n技术中心的子部门:");
        techDept.getChildren().forEach(child -> {
            System.out.println("  - " + child.getElement().name());
        });
    }
}

// 输出：
// 集团总部
//   技术中心
//     开发部
//       后端组
//       前端组
//     测试部
//   市场部
//
// 技术中心的子部门:
//   - 开发部
//   - 测试部
```

### 示例2：文件系统（支持符号链接）

```java
public class FileSystemExample {

    record File(String path, String parentPath)
        implements EnhanceElement<String> {
        @Override public String getId() { return path; }
        @Override public String getParentId() { return parentPath; }
        @Override public int compareTo(File other) { return path.compareTo(other.path); }
    }

    public static void main(String[] args) {
        // 使用EnhanceNode支持多父节点
        Tree<String, File, EnhanceNode<String, File>> fileTree =
            Tree.of(new EnhanceNode<>());

        List<File> files = Arrays.asList(
            new File("/", null),
            new File("/home", "/"),
            new File("/home/user", "/home"),
            new File("/home/user/documents", "/home/user"),
            new File("/link-to-documents", "/"),              // 符号链接1
            new File("/link-to-documents", "/home/user")      // 符号链接2（第二个父节点）
        );

        fileTree.add(files);

        // 查询符号链接的所有父目录
        EnhanceNode<String, File> link = fileTree.getById("/link-to-documents");
        System.out.println("符号链接 /link-to-documents 的所有父目录:");
        link.findParents().forEach(parent -> {
            System.out.println("  ← " + parent.getElement().path());
        });

        // 遍历子节点（已排序）
        System.out.println("\n/ 目录下的内容（自动排序）:");
        fileTree.getById("/").getChildren().forEach(child -> {
            System.out.println("  ├─ " + child.getElement().path());
        });
    }
}

// 输出：
// 符号链接 /link-to-documents 的所有父目录:
//   ← /home/user
//   ← /
//
// / 目录下的内容（自动排序）:
//   ├─ /home
//   ├─ /link-to-documents
```

### 示例3：菜单系统（JSON扁平化）

```java
public class MenuExample {

    record Menu(Integer id, Integer parentId, String name, String url, String icon)
        implements Element<Integer> {
        @Override public Integer getId() { return id; }
        @Override public Integer getParentId() { return parentId; }
    }

    public static void main(String[] args) throws Exception {
        // 属性扁平化
        List<SFunction<Menu, Object>> getters = Arrays.asList(
            Menu::name,
            Menu::url,
            Menu::icon
        );

        Tree<Integer, Menu, FlatNode<Integer, Menu>> tree =
            Tree.of(new FlatNode<>(getters));

        List<Menu> menus = Arrays.asList(
            new Menu(1, null, "系统管理", "/system", "setting"),
            new Menu(2, 1, "用户管理", "/system/user", "user"),
            new Menu(3, 1, "角色管理", "/system/role", "role"),
            new Menu(4, 2, "添加用户", "/system/user/add", "add"),
            new Menu(5, 2, "编辑用户", "/system/user/edit", "edit")
        );

        tree.add(menus);

        // JSON序列化（属性扁平化在节点上）
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(tree.getRoot());
        System.out.println(json);
    }
}

// JSON输出：
// {
//   "name" : "系统管理",
//   "url" : "/system",
//   "icon" : "setting",
//   "children" : [ {
//     "name" : "用户管理",
//     "url" : "/system/user",
//     "icon" : "user",
//     "children" : [ {
//       "name" : "添加用户",
//       "url" : "/system/user/add",
//       "icon" : "add",
//       "children" : [ ]
//     }, {
//       "name" : "编辑用户",
//       "url" : "/system/user/edit",
//       "icon" : "edit",
//       "children" : [ ]
//     } ]
//   }, {
//     "name" : "角色管理",
//     "url" : "/system/role",
//     "icon" : "role",
//     "children" : [ ]
//   } ]
// }
```

---

## 常见问题FAQ

### Q1: 应该选择哪种节点类型？

**A:** 优先选择功能最少的满足需求的类型：
- **DefaultNode** - 简单树，最快速（推荐99%的场景）
- **DeepNode** - 需要深度信息
- **EnhanceNode** - 需要多父节点DAG
- **FlatNode** - 需要JSON扁平化

### Q2: 树支持多大的规模？

**A:** 理论上没有上限（受内存限制）。实测：
- 100K节点：10ms级别add操作
- 1M节点：100ms级别add操作
- 查询：O(1)按ID查询，毫秒级；O(n)条件查询

### Q3: 支持并发吗？

**A:** 完全支持。使用ReentrantReadWriteLock：
- 读操作并发：多线程可同时查询
- 写操作互斥：add/remove/clear独占访问
- 通过测试：1000+并发线程

### Q4: 如何处理循环引用？

**A:** 自动检测和抛异常：
```java
try {
    tree.add(circularData);
} catch (Exception e) {
    // Circular reference detected: node X cannot be added as child of Y
}
```

### Q5: 如何更新节点数据？

**A:** 推荐通过remove/add周期：
```java
tree.remove(n -> n.getId().equals(targetId));
tree.add(Collections.singletonList(newElement));
```

### Q6: 性能优化建议？

**A:**
1. 批量add()，不要单个add()
2. 优先用getById()而不是find()
3. 选择最轻的节点类型
4. 避免在处理器中阻塞

### Q7: 可以JSON序列化吗？

**A:** 完全支持，使用Jackson：
```java
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(tree.getRoot());
```

FlatNode可以扁平化属性到JSON。

### Q8: 如何删除一个节点及其所有子节点？

**A:** 调用remove()会自动级联删除子节点：
```java
tree.remove(n -> n.getId().equals(targetId));
// 该节点的所有子节点也会被删除
```

---

## 总结

**Tree** 是一个功能完整、性能高效、线程安全的树形数据结构库。通过合理使用各种特性，可以构建各种复杂的树形和图形结构。

### 核心优势

✨ **功能完整**
- 多种节点类型
- 灵活的处理器链
- 完整的查询能力

⚡ **高性能**
- O(1)ID查询
- O(n·α(n))循环检测
- 批量操作优化

🔒 **线程安全**
- 读写锁保护
- 支持高并发
- 生产级别

📚 **易于使用**
- 简洁的API
- 丰富的示例
- 完整的文档

### 推荐使用场景

✅ 组织体系
✅ 分类体系
✅ 菜单系统
✅ 知识图谱
✅ 文件系统
✅ 权限管理
✅ 任务依赖
✅ 业务流程

### 开发建议

1. **选择合适的节点类型** - 不要过度设计
2. **使用处理器初始化** - 不要在add()后修改
3. **批量操作** - 不要频繁小规模add/remove
4. **利用高效查询** - getById()永远比find()快
5. **定期清理** - 删除过期节点，释放内存

---

**文档更新时间**: 2026-03-04
**版本**: 2.1
**状态**: ✅ 生产就绪