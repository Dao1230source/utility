# Assign 组件 - 批量赋值工具库

> **IDEA 报错说明**：本文档中的示例代码引用的 `OrderDTO`、`EmployeeDTO`、`NetDTO` 等类为示例 DTO，需用户自行定义。IDEA 可能会因找不到这些类而报错，属于正常现象。示例代码仅用于展示 API 用法，不可直接运行。

## 概述

基于函数式 API 与流式构建，简化在日常开发中常见的"根据字段值批量查询关联数据并赋值"场景。支持多线程并行、分批请求、本地缓存、异常与中断策略、条件分支与子任务编排等特性。

## 核心特性

- 标准化流程：统一的取值与赋值模型，减少样板代码
- 高性能并行：支持线程池与 JDK 21 虚拟线程（parallelVirtual）
- 分批处理：可按 batchSize 分批拉取外部数据
- 本地缓存：集成 Caffeine，避免重复查询
- 灵活异常处理：支持多种中断策略与自定义异常回调
- 条件分支与子任务：按条件分组执行，以及独立子处理流程
- 可扩展：支持直接赋值、基于主数据/额外数据获取、单条查询、类型转换等

## 架构概览

- **Assign**：顶层构建与编排入口，管理主数据、acquires、branches、subs、并行与中断策略
- **Acquire**：表示一次外部数据获取（批量或单条），对应多个 Action，可配置缓存、分批、异常与后置处理
- **Action**：描述从主对象取 key 并执行一组 Assemble（赋值），支持对外部数据的过滤
- **Assemble**：最小赋值单元，BiConsumer<E, T> 语义
- **InterruptStrategyEnum**：中断策略（NO / ANY / ALL）
- **InvokeStatusEnum**：执行状态（CREATED / ALL_SUCCESS / PARTIAL_FAIL / ALL_FAIL）

## 环境要求

- Java 21（使用虚拟线程）
- 依赖：Caffeine、TransmittableThreadLocal 等（按项目依赖引入）

## 快速开始

添加依赖：

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>utility</artifactId>
    <version>latest</version>
</dependency>
```

基础使用示例：

```java
// 基础使用示例
public void assign() {
    // 订单对象中字段：empCode，通常在前端页面展示的时候需要显示用户名称，这里批量给 empName 赋值
    Collection<OrderDTO> orderList = new ArrayList<>();
    // 创建一个Assign
    Assign.build(orderList)
            // Acquire：获取外部数据
            .addAcquire(this::findEmployeesByEmpCodes, EmployeeDTO::getEmpCode)
            // Action：指定字段取值
            .addAction(OrderDTO::getEmpCode)
            // Assemble：赋值
            .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
            // 返回Assign并执行
            .backAcquire().backAssign().invoke();
}
```

## 核心 API 详细说明

### Assign 类 API

#### 静态与构建方法

| 方法 | 说明 |
|------|------|
| `static <E> Assign<E> build(Collection<E> mainData)` | 从主数据集合构建 Assign |
| `Assign(Collection<E> mainData)` | 构造器（等同 build） |

#### 并行与执行控制

| 方法 | 说明 |
|------|------|
| `Assign<E> parallel()` | 使用默认线程池并行 |
| `Assign<E> parallel(Executor executor)` | 使用自定义线程池并行 |
| `Assign<E> parallel(Integer semaphorePermitsMax)` | 使用默认线程池并设置信号量上限 |
| `Assign<E> parallel(@Nullable Executor executor, @Nullable Integer semaphorePermitsMax)` | 指定线程池与信号量上限 |
| `Assign<E> parallelVirtual()` | 使用虚拟线程并行，并采用默认信号量上限（PROCESSORS * 100） |
| `Assign<E> interruptStrategy(InterruptStrategyEnum interruptStrategy)` | 设置中断策略（ANY/ALL/NO） |
| `Assign<E> invoke()` | 执行当前 Assign（包含主流程、branches 与 subs） |

#### 命名与基础信息

| 方法 | 说明 |
|------|------|
| `Assign<E> name(String name)` | 设置 Assign 名称（用于日志与缓存命名） |
| `String getName()` | 获取 Assign 名称 |

#### Acquire 添加方式（数据获取）

| 方法 | 说明 |
|------|------|
| `<K, T> Acquire<E, K, T> addAcquire(Function<Collection<K>, Map<K, T>> fetcher)` | 批量获取并返回 Map |
| `<K, T> Acquire<E, K, T> addAcquire(Function<Collection<K>, Collection<T>> fetcher, Function<T, K> keyGetter)` | 批量获取集合，按 keyGetter 转为 Map |
| `<K, T> Acquire<E, K, T> addAcquire4Single(Function<K, T> fetcher)` | 单条查询获取器（自动聚合为 Map） |
| `<K, T> Acquire<E, K, T> addAcquireByList(Function<List<K>, Collection<T>> fetcher, Function<T, K> keyGetter)` | 入参为 List 的批量查询 |
| `<K, T> Acquire<E, K, T> addAcquireByMainData(Function<Collection<E>, Collection<T>> fetcher, Function<T, K> keyGetter)` | 基于主数据集合获取（忽略请求参数集合） |
| `<K, T> Acquire<E, K, T> addAcquireByExtra(Supplier<Collection<T>> fetcher, Function<T, K> keyGetter)` | 基于额外数据源获取 |

#### 直接赋值与条件赋值

| 方法 | 说明 |
|------|------|
| `<P> Assign<E> addAssignValue(BiConsumer<E, P> eSetter, P value)` | 直接对主对象字段赋固定值 |
| `<P> Assign<E> addAssignValueIfAbsent(Function<E, P> eGetter, BiConsumer<E, P> eSetter, P value)` | 仅在字段为空时赋值 |

#### 分支与子任务

| 方法 | 说明 |
|------|------|
| `Assign<E> addBranch(Predicate<E> filter)` | 创建条件分支（继承当前 mainData，增加 depth） |
| `Assign<E> addBranch()` | 创建普通分支 |
| `<K> Assign<E> addBranches(Function<E, K> keyGetter, Map<K, Function<Collection<E>, Assign<E>>> keyAssigners)` | 按 key 分组并创建多分支 |
| `<K> Assign<E> addOperates(Function<E, K> keyGetter, Map<K, Consumer<Collection<E>>> keyOperates)` | 按 key 分组执行自定义操作 |
| `Assign<E> addSub(Consumer<Collection<E>> sub)` | 添加子任务（独立于主流程） |

#### 返回上级与导航

| 方法 | 说明 |
|------|------|
| `Assign<E> backSuper()` | 返回父 Assign（若无则返回自身） |
| `Assign<E> backSuperTo(int depth)` | 返回至指定 depth |
| `Assign<E> backSuperlative()` | 返回根 Assign（depth = 1） |

#### 工具与转换

| 方法 | 说明 |
|------|------|
| `void forEach(Consumer<E> consumer)` | 遍历当前 mainData |
| `Assign<E> peek(Consumer<E> consumer)` | 遍历并返回自身（流式） |
| `List<E> toList()` | 转为可变 List（拷贝） |
| `<F> Assign<F> cast(Function<E, F> mapping)` | 元素映射转换并构建新 Assign |
| `<F> Assign<F> casts(Function<Collection<E>, Collection<F>> mapping)` | 集合映射转换并构建新 Assign |

### Acquire 类 API

| 方法 | 说明 |
|------|------|
| `Acquire<E, K, T> name(String name)` | 设置 Acquire 名称（与 Assign 名称组成缓存 key） |
| `Acquire<E, K, T> cache()` | 启用默认 Caffeine 缓存配置 |
| `Acquire<E, K, T> cache(Supplier<Cache<K, T>> cacherSupplier)` | 启用自定义缓存 |
| `Acquire<E, K, T> batchSize(int batchSize)` | 设置分批大小 |
| `Acquire<E, K, T> throwException()` | 标记获取失败时抛出异常 |
| `Acquire<E, K, T> exceptionHandler(BiConsumer<E, Throwable> exceptionHandler)` | 自定义异常处理回调 |
| `Acquire<E, K, T> afterProcessor(BiConsumer<E, Map<K, T>> afterProcessor)` | 后置处理回调 |
| `Action<E, K, T> addAction(Function<E, K> keyGetter)` | 添加 Action（定义从主对象取 key） |
| `Assign<E> backAssign()` | 返回所属 Assign |
| `static <K, T> Cache<K, T> defaultCache()` | 默认缓存实例 |

### Action 类 API

| 方法 | 说明 |
|------|------|
| `<P> Action<E, K, T> addAssemble(Function<T, P> tGetter, BiConsumer<E, P> eSetter)` | 从外部对象取值并赋给主对象字段 |
| `Action<E, K, T> addAssemble(BiConsumer<E, T> getAndSet)` | 自定义赋值逻辑 |
| `Action<E, K, T> filter(Predicate<T> test)` | 对外部数据进行过滤（如仅保留有效数据） |
| `Acquire<E, K, T> backAcquire()` | 返回所属 Acquire |
| `Function<E, K> getKeyGetter()` | 获取当前 Action 的 keyGetter |

### 枚举

| 枚举 | 值 | 说明 |
|------|-----|------|
| InterruptStrategyEnum | NO / ANY / ALL | 控制中断行为 |
| InvokeStatusEnum | CREATED / ALL_SUCCESS / PARTIAL_FAIL / ALL_FAIL | 执行状态 |

## 使用示例

### 多字段合并（同 Acquire 多 Action）

```java
// 多字段合并（同 Acquire 多 Action）
public void assignMultiAction() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire()
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
}
```

### 多字段赋值（同 Action 多 Assemble）

```java
// 多字段赋值（同 Action 多 Assemble）
public void assignMultiAssemble() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
}
```

### 分批处理

```java
// 示例：分批处理
public void assignBatch() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .batchSize(100)
      .addAction(OrderDTO::getEmpCode)
      .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
      .backAcquire().backAssign()
      .invoke();
```

### 异常与中断策略

```java
// 示例：异常与中断策略
public void assignWithException() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .interruptStrategy(org.source.utility.assign.InterruptStrategyEnum.ANY)
      .addAcquire(this::findEmployeesByEmpCodes, EmployeeDTO::getEmpCode)
      .name("获取员工信息")
      .throwException()
      .exceptionHandler((order, ex) -> log.error("获取员工异常", ex))
      .addAction(OrderDTO::getEmpCode)
      .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
      .backAcquire().backAssign()
      .invoke();
```

### 本地缓存

```java
// 示例：本地缓存
public void assignWithCache() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .name("empCache")
      .cache()
      .addAction(OrderDTO::getEmpCode)
      .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
      .backAcquire().backAssign()
      .invoke();
```

### 条件分支

```java
// 条件分支
public void assignWithBranch() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addBranch(e -> true)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire().backSuperlative()
          .invoke();
}
```

### 子任务（独立处理）

```java
// 子任务（独立处理）
public void assignWithSub() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addSub(list -> {})
      .addAcquire(this::findEmployeesByEmpCodes, EmployeeDTO::getEmpCode)
      .addAction(OrderDTO::getEmpCode)
      .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
      .backAcquire().backAssign()
      .invoke();
```

### 虚拟线程并行

```java
// 虚拟线程并行
public void assignVirtualThread() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .parallelVirtual()
      .addAcquire(this::findEmployeesByEmpCodes, EmployeeDTO::getEmpCode)
      .addAction(OrderDTO::getEmpCode)
      .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
      .backAcquire().backAssign()
      .invoke();
```

### 单条查询获取

```java
// 单条查询获取
public void assignSingle() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquire4Single(empCode -> null)
      .addAction(OrderDTO::getEmpCode)
      .addAssemble(EmployeeDTO::getEmpName, OrderDTO::setEmpName)
      .backAcquire().backAssign()
      .invoke();
```

### 基于主数据或额外数据获取

```java
// 基于主数据或额外数据获取
public void assignByMainDataOrExtra() {
    Collection<Object> orderList = new ArrayList<>();
    // 基于主数据集合
    Assign.build(orderList)
          .addAcquireByMainData(es -> java.util.Collections.emptyList(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
    // 基于额外数据源  
    Assign.build(orderList)
          .addAcquireByExtra(() -> java.util.Collections.emptyList(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
}
```

### 直接赋值与条件赋值

```java
// 直接赋值与条件赋值
public void assignValue() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAssignValue(e -> {}, 1)
          .addAssignValueIfAbsent(e -> null, e -> {}, "默认区域")
          .invoke();
}
```

### 类型转换

```java
// 类型转换
public void assignCast() {
    Collection<Object> orderList = new ArrayList<>();
    // 元素映射转换
    Assign.build(orderList)
          .cast(e -> null)
          .peek(e -> {})
          .invoke();
    // 集合映射转换
    Assign.build(orderList)
          .casts(list -> java.util.Collections.emptyList())
          .forEach(e -> {});
}
```

### Action 数据过滤

```java
// Action 数据过滤
public void assignWithFilter() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .addAction(e -> null)
          .filter(t -> true)
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
}
```

### 多级依赖（先获取中间属性再级联查询）

```java
// 多级依赖（先获取中间属性再级联查询）
public void assignMultiLevel() {
    Collection<Object> orderList = new ArrayList<>();
    // 第一级
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign().invoke();
    // 第二级
    Assign.build(orderList)
          .addAcquire((keys) -> new java.util.HashMap<>(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
}
```

### 批量查询入参为 List

```java
// 批量查询入参为 List
public void assignByList() {
    Collection<Object> orderList = new ArrayList<>();
    Assign.build(orderList)
          .addAcquireByList(empCodes -> java.util.Collections.emptyList(), e -> null)
          .addAction(e -> null)
          .addAssemble((e, t) -> {})
          .backAcquire().backAssign()
          .invoke();
}
```

### 按类型分组处理多分支

```java
// 按类型分组处理多分支
public void assignBranches() {
    Collection<Object> orderList = new ArrayList<>();
    java.util.Map<Integer, java.util.function.Function<Collection<Object>, Assign<Object>>> typeHandlers = new java.util.HashMap<>();
    typeHandlers.put(1, orders -> Assign.build(orders));
    typeHandlers.put(2, orders -> Assign.build(orders));
    
    Assign.build(orderList)
          .addBranches(e -> null, typeHandlers)
          .invoke();
}
```

## 高级特性

### 执行状态

通过 `InvokeStatusEnum` 判断执行结果：
- **ALL_SUCCESS**：所有 acquires 执行成功
- **PARTIAL_FAIL**：部分 acquires 执行失败
- **ALL_FAIL**：所有 acquires 执行失败

### 中断策略

通过 `InterruptStrategyEnum` 控制中断行为：
- **NO**：无论成功失败都不中断
- **ANY**：任一失败就中断（默认）
- **ALL**：全部失败才中断

### 线程与信号量

- `parallel()` 使用平台线程池
- `parallelVirtual()` 使用虚拟线程，适合 IO 密集型场景
- 设置 `semaphorePermitsMax` 限制最大并发数，保护数据库等关键资源

### 缓存机制

- 缓存 key 格式：`{assignName}_{acquireName}`
- 默认配置：`expireAfterAccess(600s)`，`maximumSize(128)`
- 支持自定义 `Supplier<Cache<K, T>>`

## 最佳实践

1. **命名规范**：为 Assign 和 Acquire 设置有意义的 name，便于日志追踪与缓存识别
2. **批量优先**：优先使用 `addAcquire` 或 `addAcquireByList` 批量接口，减少远程调用次数
3. **分批与限流**：对大批量使用 `batchSize`，对虚拟线程场景设置 `semaphorePermitsMax`
4. **异常处理**：结合 `interruptStrategy` 与 `exceptionHandler`，避免流程中断过严或过松
5. **缓存策略**：对高频查询且结果稳定的 Acquire 开启 `cache` 并设置合理过期与容量
6. **依赖编排**：有依赖关系时先执行 `invoke`，再级联构建后续 Acquire
7. **数据过滤**：使用 `Action.filter` 过滤无效数据，避免脏数据赋值

## FAQ

**Q: parallelVirtual 与 parallel 区别？**

A: `parallelVirtual` 使用虚拟线程并行并自动设置默认信号量上限（PROCESSORS * 100）；`parallel` 可使用默认或自定义线程池，并可按需设置信号量。

**Q: Acquire 缓存 key 如何构成？**

A: 使用 `assign.getName() + "_" + acquire.getName()`，确保同一 Assign 下的 Acquire name 不重复。

**Q: Action.filter 的作用？**

A: 对 fetch 到的外部数据进行过滤，例如仅保留有效或未删除的数据再执行赋值。

**Q: branches 与 subs 区别？**

A: `branches` 继承主数据并与父级关联，可通过 `backSuper` 返回；`subs` 是独立子任务，与主流程无父子关联，通常用于副作用处理（如日志、通知）。

**Q: addAcquireByMainData 与 addAcquire 有何区别？**

A: `addAcquireByMainData` 忽略请求参数，直接基于主数据集合触发查询；`addAcquire` 根据请求参数（key 集合）查询。

**Q: 什么时候使用 addAcquire4Single？**

A: 当外部接口不支持批量查询，只能单条查询时使用。框架会自动并发调用并聚合结果。

## 版本与兼容性

- **最低 JDK**：21（依赖虚拟线程）
- **依赖**：Caffeine、TransmittableThreadLocal 等（见 pom.xml）
- **建议**：结合 Spring 注入线程池、缓存实例等以获得更好治理能力

## 贡献与反馈

欢迎提交 Issue 与 PR，建议附上最小复现示例与期望行为。

## License

遵循项目主仓库的开源协议。
