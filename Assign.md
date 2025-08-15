### 为什么开发这个组件

开发过程中，我们经常遇到这样的情况：需要通过集合中对象的某个Key值批量去查询其他的数据再给其他字段赋值。最常见的是通过用户ID查询用户名称。  
一般来说我们是这样做的:

```java
// 以下为伪代码
public void assign() {
    // 假设这是一个班级的所有学生，其中只有id，需要给name赋值
    Collecttion<StudentInfo> studentsOfClass;
    // 获取students中的studentId
    Set<String> studentIds;
    // 从学生表获取学生信息
    Collecttion<Student> studentInfo = fetchStudentInfoFromStudentTable(studentIds);
    // 以studentId为唯一Key转换为Map
    Map<String, Student> studentMap;
    // 循环处理studentsOfClass，给studentName赋值
    for (Student student : studentsOfClass) {
        // 根据studentId从Map中取值
        Student studentFromDb = studentMap.get(student.getId());
        // 赋值
        student.setName(studentFromDb.getName);
    }
}
```

使用Assign组件只要如下编写代码即可实现

```java
public void assign() {
    Collecttion<StudentInfo> studentsOfClass;
    Assign.build(studentsOfClass)
            // 从学生表获取学生信息，并以studentId为唯一Key
            .addAcquire(this::fetchStudentInfoFromStudentTable, Student::getId)
            // 根据获取StudentInfo中的studentId去获取学生信息
            .addAction(StudentInfo::getId)
            // 赋值
            .addAssemble(Student.getName, StudentInfo::setName)
            // 最后执行
            .backAcquire().backAssign().invoke();
}
```

## Assign具有以下优势

- 标准化流程
    - 规范取值赋值的标准写法，提高开发效率
- 践行代码简洁之道
    - 程序员只需关注真正业务相关的事物，无须理会其他细节
- 规避常见易错bug
    - 比如toMap时因为id重复而报错
- 多线程并行
    - 提高性能，避免使用线程导致的各种bug
- 分批处理
    - 避免接口输入输出过大引发的各种内存、超时等问题
- 异常处理机制
    - 可以自行处理一个赋值过程的异常
    - 指定中断策略：全部成功/部分成功/全部失败，可继续流转至下一个节点
- 数据缓存
    - 使用 caffeine 缓存获取的数据
- 节点依赖
    - 一些赋值有依赖关系，比如先通过studentId获取teacherId，再通过teacherId获取teacherName
- 数据分类处理
    - 比如高中生需要查询高考时间，初中生需要查询中考时间，小学生则无须处理

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