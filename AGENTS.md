# AGENTS.md - Agent Coding Guidelines

## 项目概述

这是一个 **Java 通用工具库**，提供各类业务开发中常用的工具类和函数式编程辅助。

- **语言**: Java 21
- **构建工具**: Maven
- **包基础路径**: `org.source.utility`

---

## 构建命令

### 常用命令

```bash
# 构建项目
./mvnw clean install -DskipTests

# 运行测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ClassNameTest

# 运行单个测试方法
./mvnw test -Dtest=ClassNameTest#methodName

# 跳过测试
./mvnw clean install -DskipTests

# 详细输出
./mvnw clean install -DskipTests -X

# 仅打包
./mvnw package -DskipTests
```

### 代码生成

```bash
# 生成 JavaDoc
./mvnw javadoc:javadoc

# 生成 sources jar
./mvnw source:jar
```

---

## 代码规范

### 命名约定

- **类名**: PascalCase (如 `UserService`, `I18nTemplate`)
- **接口名**: PascalCase，函数式接口建议以 `-er` 结尾 (如 `Processor`, `Handler`)
- **方法名**: camelCase (如 `getUserById`, `processData`)
- **常量**: UPPER_SNAKE_CASE (如 `MAX_RETRY_COUNT`)
- **枚举**: PascalCase，值用 UPPER_SNAKE_CASE (如 `LogScopeEnum.BUSINESS`)
- **包名**: 小写，用单词或下划线分隔 (如 `org.source.utility.utils`)

### 包结构

```
org.source.utility
├── assign         # 赋值/组装工具（核心模块）
├── tree           # 树形结构处理
├── flow           # 流程编排
├── utils          # 工具类
│   ├── Jsons      # JSON 处理
│   ├── Maps       # Map 工具
│   ├── Streams   # Stream 工具
│   ├── Strings   # 字符串工具
│   ├── Dates     # 日期工具
│   ├── Enums     # 枚举工具
│   ├── Reflects  # 反射工具
│   └── Asserts   # 断言工具
├── mapstruct      # MapStruct 映射
├── exceptions     # 异常处理
├── function       # 自定义函数接口
├── constant       # 常量定义
└── enums          # 枚举定义
```

### 导入顺序 (IDE 标准)

1. `java.*` 和 `javax.*`
2. 第三方库 (org.*, com.*)
3. 项目内部包 (`org.source.utility.*`)
4. 静态导入

### 类型使用

- **参数类型**: 优先使用接口而非实现类 (如用 `List<T>` 而非 `ArrayList<T>`)
- **基本类型**: 使用基本类型而非包装类
- **可空返回值**: 使用 `Optional`
- **集合类型**: 优先使用 `List`, `Set`, `Map` 接口

### 注解使用

- **Lombok**: 使用 `@Slf4j`, `@Getter`, `@Setter`, `@Builder`, `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`
- **Order**: 注解单独成行，按字母顺序排列

### 异常处理

- 使用自定义异常继承 `RuntimeException`
- **业务异常**: 使用 `BaseException`
- **日志记录**: 始终记录异常，并使用适当的日志级别
- **禁止吞异常**: 不允许在未记录日志的情况下吞掉异常

### 线程安全

- 使用线程安全集合 (`ConcurrentHashMap`, `CopyOnWriteArrayList`)
- 使用 `ThreadLocal` 配合 `TransmittableThreadLocal` 进行异步上下文传递
- 优先使用不可变对象

### 文档

- 公共 API 需要 Javadoc
- 每个包需要有 `package-info.java` 说明用途
- 保持代码自文档化，使用有意义的命名

---

## 核心模块说明

### assign 模块

提供数据赋值和组装能力，支持：
- 批量数据获取和赋值
- 多线程并行处理
- 分支处理
- 缓存支持

### tree 模块

树形结构处理，支持：
- 树构建
- 树扁平化
- 树遍历
- 节点增强

### flow 模块

流程编排，支持：
- 条件分支
- 值处理
- 选择器
- 节点树

---

## 关键依赖

- Java 21
- Lombok
- Jackson (JSON)
- Caffeine (缓存)
- TransmittableThreadLocal
- MapStruct
- Spring Context

---

## 模块参考 (Skills Reference)

开发时请参考以下模块文档，了解 API 用法与最佳实践：

| 模块 | 文档 | 说明 |
|------|------|------|
| assign | [Assign.md](Assign.md) | 批量赋值工具库，支持并行处理、分批请求、缓存、分支与子任务编排 |

**使用 Assign 模块时务必先阅读 Assign.md**，其中包含：
- 完整 API 参考（Assign/Acquire/Action/Assemble）
- 14 个使用示例
- 最佳实践与 FAQ

---

## Agent 注意事项

1. **Spring 集成**: 该库支持 Spring 自动装配
2. **虚拟线程**: 项目使用 JDK 21 虚拟线程 (`Executors.newVirtualThreadPerTaskExecutor()`)
3. **无现有测试**: 项目目前没有测试覆盖，修改代码时需要添加测试
4. **IDE 集成**: 项目支持 IntelliJ IDEA，使用 Maven 导入
5. **Assign 模块**: 开发 assign 相关功能前，请先阅读 [Assign.md](Assign.md) 了解 API 设计与用法
