### 本项目发布一些有意思的工具类

#### Assign 批量赋值

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

### 标准化流程
    规范取值赋值的标准写法，提高开发效率
### 践行代码简洁之道
    程序员只需关注真正业务相关的事物，无须理会其他细节
### 规避常见易错bug
    比如toMap时因为id重复而报错
### 多线程并行
    提高性能，避免使用线程导致的各种bug
### 分批处理
    避免接口输入输出过大引发的各种内存、超时等问题
### 异常处理机制
    可以自行处理一个赋值过程的异常
    指定中断策略：全部成功/部分成功/全部失败，可继续流转至下一个节点
### 数据缓存
    使用 caffeine 缓存获取的数据
### 节点依赖
    一些赋值有依赖关系，比如先通过studentId获取teacherId，再通过teacherId获取teacherName
### 数据分类处理
    比如高中生需要查询高考时间，初中生需要查询中考时间，小学生则无须处理

更多详情请见：[Assign](Assign.md)