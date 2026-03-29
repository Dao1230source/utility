# Utility - Java Utility Library | Java 通用工具库

<!--
Keywords: Java, Utility, Tools, Batch Assignment, Tree Data Structure, DAG, Virtual Thread, JDK 21, 并发安全, 批量赋值, 树形结构
GitHub: https://github.com/Dao1230source/utility
Maven: io.github.dao1230source:utility
-->

> A collection of useful Java utility classes for enterprise development. Features batch data assignment, tree data structures, and more.
>
> 企业级 Java 通用工具库，提供批量赋值、树形数据结构等实用组件，助力高效开发。

[![Java](https://img.shields.io/badge/Java-21+-orange)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-io.github.dao1230source:utility-blue)](https://central.sonatype.com/artifact/io.github.dao1230source/utility)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/Dao1230source/utility?style=social)](https://github.com/Dao1230source/utility)

---

## 📦 Modules | 模块

### Assign - Batch Assignment Tool | 批量赋值工具

> **Assign** is a Java library for batch data assignment and enrichment. Perfect for scenarios where you need to fetch related data by field values and assign them in bulk. Supports parallel processing, batching, caching, and JDK 21 virtual threads.
>
> **Assign** 是一个 Java 批量赋值工具库，专为"根据字段值批量查询关联数据并赋值"场景设计。支持多线程并行、分批请求、本地缓存、JDK 21 虚拟线程等特性。

**Core Features | 核心特性：**

- 🚀 **Virtual Thread Support** - JDK 21 virtual threads for efficient IO-bound tasks
- 📦 **Batch Processing** - Split large requests into batches automatically
- 💾 **Caffeine Cache** - Built-in caching to avoid duplicate queries
- ⚡ **Parallel Execution** - Multi-threaded data fetching
- 🔀 **Branch & Sub-task** - Conditional branches and independent sub-processes

**Use Cases | 使用场景：**

- Order data enrichment (employee names, department names, etc.)
- Multi-source data aggregation
- Batch API calls with rate limiting
- Database query optimization

**Quick Example | 快速示例：**

```java
Assign.build(orderList)
    .parallelVirtual()  // JDK 21 Virtual Thread
    .addAcquire(this::findEmployeesByEmpCodes, EmployeeDTO::getEmpCode)
    .addAction(OrderDTO::getEmpCode)
    .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
    .backAcquire().backAssign().invoke();
```

📄 **Full Documentation**: [Assign.md](Assign.md) | **Demo**: [GitHub Demo](https://github.com/Dao1230source/demo/tree/main/utility/assign)

---

### Tree - Tree Data Structure Library | 树形数据结构组件

> **Tree** is a generic tree data structure container for Java, supporting flexible tree and DAG (Directed Acyclic Graph) structures. Features thread-safety with read-write locks, automatic circular reference detection using Union-Find algorithm, and multiple node types for various use cases.
>
> **Tree** 是一个通用的 Java 树形数据结构容器，支持灵活的树形和 DAG 结构。提供并发安全（读写锁）、循环引用检测（Union-Find 算法）、多种节点类型等特性，适用于组织结构、分类体系、菜单系统等场景。

**Core Features | 核心特性：**

- 🔒 **Thread-Safe** - ReentrantReadWriteLock for concurrent access
- 🔄 **Circular Detection** - Union-Find algorithm, O(n·α(n)) complexity
- 🎯 **O(1) ID Lookup** - Fast node retrieval by ID
- 🌳 **Multiple Node Types** - DefaultNode, DeepNode, EnhanceNode, FlatNode
- 📊 **DAG Support** - Multi-parent nodes via EnhanceNode
- 📝 **JSON Friendly** - Full Jackson serialization support

**Node Types | 节点类型：**

| Type | Use Case |
|------|----------|
| **DefaultNode** | Simple tree, best performance (recommended for 99% cases) |
| **DeepNode** | Automatic depth calculation |
| **EnhanceNode** | Multi-parent DAG, symbolic links, permissions |
| **FlatNode** | JSON flattening for RESTful APIs |

**Use Cases | 使用场景：**

- Organization charts & department hierarchies
- Menu systems & navigation trees
- Category & classification systems
- File systems with symbolic links
- Knowledge graphs & concept relationships
- Permission management (users with multiple roles)

**Quick Example | 快速示例：**

```java
Tree<Integer, DeptElement, DefaultNode<Integer, DeptElement>> tree =
    Tree.of(new DefaultNode<>());

tree.setAfterAddHandler((node, parent) -> {
    node.getElement().setLevel(
        parent.getElement() == null ? 0 :
        parent.getElement().getLevel() + 1
    );
});

tree.add(departments);  // Batch add all elements
DefaultNode<Integer, DeptElement> node = tree.getById(2);  // O(1) lookup
```

📄 **Full Documentation**: [Tree.md](Tree.md) | **Demo**: [GitHub Demo](https://github.com/Dao1230source/demo/tree/main/utility/tree) | **Performance**: [JMH Report](https://github.com/Dao1230source/demo/blob/main/utility/tree/JMH-REPORT.md)

---

## 🚀 Quick Start | 快速开始

### Maven Dependency | Maven 依赖

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>utility</artifactId>
    <version>latest</version>
</dependency>
```

### Requirements | 环境要求

- **Java 21+** (required for virtual threads)
- **Dependencies**: Caffeine, TransmittableThreadLocal, Jackson (optional for JSON)

---

## 📊 Performance | 性能参考

### Assign Performance (1000 orders, 100/batch)

| Metric | Traditional | Virtual Thread | Improvement |
|--------|-------------|----------------|-------------|
| Response Time | 2-3s | 0.5-1s | **2-4x** |
| Throughput | ~1000 req/s | ~5000 req/s | **5x** |
| Memory | ~200MB | ~50MB | **4x** |

### Tree Performance

| Operation | Complexity |
|-----------|------------|
| `getById(id)` | **O(1)** |
| `add(elements)` | O(n·m) |
| `find(predicate)` | O(n) |
| Circular Detection | O(n·α(n)) ≈ O(n) |

---

## 🏷️ Tags | 标签

**Keywords**: Java, Utility, Batch Assignment, Data Enrichment, Tree Structure, DAG, Virtual Thread, JDK 21, Thread-Safe, Concurrent, Union-Find, Circular Detection, Organization Chart, Menu System, Category Tree

**关键词**: Java工具库, 批量赋值, 数据补充, 树形结构, 有向无环图, 虚拟线程, 并发安全, 循环检测, 组织架构, 菜单系统, 分类树

---

## 📚 Resources | 资源

- **GitHub Repository**: https://github.com/Dao1230source/utility
- **Demo & Examples**: https://github.com/Dao1230source/demo
- **Maven Central**: https://central.sonatype.com/artifact/io.github.dao1230source/utility
- **Issues**: https://github.com/Dao1230source/utility/issues

---

## 📄 License

MIT License - See [LICENSE](LICENSE) for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

- **GitHub Issues**: https://github.com/Dao1230source/utility/issues
- **Pull Requests**: https://github.com/Dao1230source/utility/pulls