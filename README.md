### 本项目发布一些有意思的工具类
#### Assign 批量赋值
开发过程中，我们经常遇到这样的情况：需要通过集合中对象的某个Key值批量区查询其他的数据再给其他字段赋值。最常见的是通过用户ID查询用户名称。  
一般来说我们是这样做的:
```java
// 以下为伪代码
public void assign() {
    // 假设这是一个班级的所有学生，其中只有id，需要给name赋值
    Collecttion<Student> studentsOfClass;
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
这样简单易实现，可是存在以下一些不完美的地方：
- 项目中存在大量相似的重复代码，且存在不少隐藏bug，比如toMap时因为id重复而报错
- 有时可能不止给一个字段赋值，需要简单重复以上过程，代码写起来十分不简洁
- 如果存在多个赋值过程，导致请求时间过长，影响接口性能，需要使用多线程处理，但是每个人的处理方法不同，代码更是五花八门，而且线程相关的隐藏bug更多
- 如果集合数量过多，需要分批处理
- 如果其中一个赋值过程发生了异常，应该如何处理？
- 如果某些数据需要用到缓存来提高性能该如何实现？
- 一些赋值有依赖关系，比如先通过studentId获取teacherId，再通过teacherId获取teacherName
- 集合数据需要分类处理，比如高中生需要查询高考时间，初中生需要查询中考时间，小学生则无须处理

以上种种问题，[Assign](Assign.md)均可完美实现