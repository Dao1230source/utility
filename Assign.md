### 组件是什么

处理集合数据，根据集合中对象的可作为unique key的字段的值获取对应数据并给指定字段赋值

### 组件该怎么使用

引入jar

```xml

<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>utility</artifactId>
    <version>latest version</version>
</dependency>
```

简单使用

```java
public void assign() {
    List<StudentData> studentInfoList = assignFacade.getStudentDataList();
    Assign.build(studentInfoList)
            // fetch StudentEntity data by ids
            .addAcquire(assignFacade::getStudentList, StudentEntity::getId)
            // get id from studentInfoList
            .addAction(StudentData::getId)
            // assign name、age
            .addAssemble(StudentEntity::getName, StudentData::setName)
            .addAssemble(StudentEntity::getAge, StudentData::setAge)
            .backAcquire()
            .backAssign()
            // final must invoke
            .invoke();
}
```

#### Assign

组件主类通过调度Acquire获取外部数据，可以构建分支、子流程，使用多线程等

初始化一些值

```java
public void assignValue() {
    // assignValue 先于 acquire 执行
    Assign.addAssignValue(StudentData::setRemark, "student info")
            .addAssignValueIfAbsent(StudentData::getSchool, StudentData::setSchool, "Shenzhen Second School");
}
```

并发执行

```java
public void parallel() {
    // 启用线程池，并行执行 acquire，也可以指定 ExecutorService
    Assign.parallel();
}
```

分支，处理同一批数据

```java
public void branch() {
    // 添加分支，先通过 TeacherEntity 获取 classId，再获取 class 数据，这里只处理 classId = c2 的班级数据
    assign.addBranch(k -> "c2".equals(k.getClassId()))
            .addAcquire(assignFacade::getClassMap)
            .addAction(StudentData::getClassId)
            .addAssemble(ClassEntity::getName, StudentData::setClassName)
            .backAcquire().backAssign().invoke()
            // 返回最高层级
            .backSuperlative();
}
```

子程序，和主体是不相关的，对子体的mainData的引用修改不会影响主体，但子体中对 E 值的修改会影响主体

```java
public void addSub() {
    // 子程序
    assign.addSub(this::subAssign);
}

public void subAssign(Collection<StudentData> studentInfoList) {
    Assign.build(studentInfoList).name("subAssign")
            .addBranch(k -> 2L == k.getId())
            .addAssignValue(StudentData::setRemark, "subAssign invoked")
            .invoke();
}
```

中断策略

```java
public void addSub() {
    // 发生异常时是结束流程
    // InterruptStrategyEnum.NO acquires 不论是否成功都不结束
    // InterruptStrategyEnum.ANY acquires 任一失败都结束
    // InterruptStrategyEnum.ALL acquires 全部失败才结束
    assign.interruptStrategy(InterruptStrategyEnum.ANY);
}
```

#### Acquire

获取数据主要实现，可以分批执行，缓存数据，有后置处理器，异常处理器等  
Acquire的后置处理器和异常处理器

```java
public void test() {
    acquire.afterProcessor((studentInfo, idStudentMap) -> {
                if (!idStudentMap.containsKey(studentInfo.getId())) {
                    log.info("student id:{} 没有获取到对应数据，如果需要可以抛出异常", studentInfo.getId());
                }
            })
            .exceptionHandler((studentInfo, throwable) -> log.info("如果 acquire fetcher 发生异常，在此处处理"));
}
```

缓存、分批获取数据

```java
public void test() {
    acqire
            // 给 acquire 命名，并且启用本地缓存（启用cache必须设置name值），也可以自定义 Cache<K, T>
            .name(StudentEntity.class.getName()).cache()
            // 分批获取数据
            .batchSize(2);
}
```

#### Action

指定集合的唯一值字段，执行赋值的操作，可以添加过滤条件  
过滤条件

```java
public void test() {
    action.filter(student -> student.getAge() == 18);
}

```

### Assemble

从外部数据对象中获取值赋给集合对象的指定字段