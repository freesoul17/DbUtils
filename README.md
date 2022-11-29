## 仓库介绍

由于在学习阶段，不允许使用Apache的dbutils包或者mybatis等技术框架，故自己封装了一个基于原生JDBC的一个工具类。

目前该工具类已实现

| 方法名                                                       | 说明                             |
| ------------------------------------------------------------ | -------------------------------- |
| `Connection getConnection()`                                 | 获取连接                         |
| `void closeResource(Connection connection)`                  | 关闭Connection（有几个重载方法） |
| `int insert(Connection connection, String sql, Object... param)` | 增加一条记录                     |
| `int update(Connection connection, String sql, Object... param)` | 更新一条记录                     |
| `int delete(Connection connection, String sql, Object... param)` | 删除一条记录                     |
| `List<HashMap<String, Object>> queryObjAsList(Connection connection, String sql, Object... param)` | 查询任意字段                     |
| `<T> T queryOneBean(Class<T> clazz, Connection connection, String sql, Object... param)` | 查询并封装一个bean对象           |
| `<T> List<T> queryBeanAsList(Class<T> clazz, Connection connection, String sql, Object... param)` | 查询并封装多个bean               |
| `int batchInsert(Connection connection, String sql, int count, Object[][] param)` | 批量插入(带参数）                |
| `int batchInsert(Connection connection, String sql, int count)` | 批量插入（不带参数）             |
| `int batchDelete(Connection connection, String sql, int count, Object[][] param)` | 批量删除（带参数）               |

很遗憾，我现在还没有头绪怎么简化的封装好事务功能。

另外，即便我提供了CRUD的实现，但是你会发现，我插入删除是返回了更新操作的结果，因为插入删除本质上就是更新。所以你可以使用更新操作来进行删除、修改，也可以使用修改还进行插入删除。但是为了语义化，我提供了这几个方法的实现。

如果您需要使用`druid`或者`C3P0`等连接池，那么你需要自行实例化一个`Connection`

## 使用教程

首先，你需要在你的项目中导入这个工具类，并在类路径下创建一个`jdbc.properties`的配置文件，在配置文件中配置好连接数据库的四要素

```properties
driverClass=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/test
user=root
password=root
```

然后在你要操作的类下面导入该类

```java
import DbUtils的全类名
public class XXXDao {
    Connection connection = DbUtils.getConnection();
}
```

这样做的目的是你每个方法都可以访问到该连接对象，不至于每个方法都去创建一个`Connection`。

在接下来的使用中，你只需要：**① 写SQL语句 ② 调用对应的方法 ③ 关闭连接**（只需要关闭连接，其他的资源我已经帮你关闭了，关闭其他资源的方法我只在内部使用，所以关闭其他资源的方法并没有设置为public，您只需要放心的关闭Connection对象就可以了）

#### 增加一条记录

```java
@Test
public void testInsert() {
    //不带参数
    //① 写SQL语句
    String sql = "insert into student values(null,2020200,'王',21,'1班','A')";
    //② 调用对应的方法
    DbUtils.update(connection, sql);
    //带参数
    ////① 写SQL语句
    String sql1 = "insert into student values(null,?,?,21,'1班','A')";
    //② 调用对应的方法
    DbUtils.update(connection, sql1,202022,"王");
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

#### 删除一条记录

```java
@Test
public void testDelete() {
    //① 写SQL语句
    String sql = "delete from student where id = ?";
    //② 调用对应的方法
    DbUtils.update(connection, sql, 2);
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

#### 修改一条记录

```java
@Test
public void testUpdate() {
	//① 写SQL语句
    String sql = "update student set name = '李' where id = ?";
    //② 调用对应的方法
    int update = DbUtils.update(connection, sql, 2);
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

#### 查询一个javaBean

这里举个特例，数据库中的字段为`student_num`，而实体类中的字段为`studentNum`。本人还没那个实力去搞懂形如`mybatis`等设置驼峰命名法，故**你需要为该字段添加一个和实体类字段一致的别名**，这样便能成功的封装成一个JavaBean

```java
@Test
public void testQueryOneBean() {
    //① 写SQL语句
    String sql = "select id,student_num studentNum,name,age,class_name className ,score " +
            "from student where id = ?";
    //② 调用对应的方法
    Student student = DbUtils.queryOneBean(Student.class, connection, sql, 1);
    System.out.println(student);
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

#### 查询多个JavaBean

同上，若数据库字段和实体类字段不一致，你需要为字段添加一个和实体类字段一致的别名

```java
@Test
public void testQueryBeanAsList() {
    //① 写SQL语句
    String sql = "select id,student_num studentNum,name,age,class_name className ,score " +
            "from student ";
    //② 调用对应的方法
    List student = DbUtils.queryBeanAsList(Student.class, connection, sql);
    System.out.println(student.toString());
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

#### 查询任意字段

```java
@Test
public void testQueryObjAsList() {
    //① 写SQL语句
    String sql = "select count(*) from student where name = ? ";
    //② 调用对应的方法
    List<HashMap<String, Object>> list = DbUtils.queryObjAsList(connection, sql, "王");
    System.out.println(list.toString());
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

#### 批量插入

```java
@Test
public void testBatchInsert() {
    //① 写SQL语句
    String sql = "insert into stu values(?,?) ";
    Object[][] param = new Object[3][2];
    param[0][0] = 10;
    param[0][1] = "王";
    param[1][0] = 11;
    param[1][1] = "吴";
    param[2][0] = 12;
    param[2][1] = "李";
    //② 调用对应的方法
    DbUtils.batchInsert(connection, sql, 3, param);
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

在这里，你可以奇怪这个参数是怎么写的。首先需要一个二维数组，长度为操作的次数和参数。如上所示，操作的次数为3，参数为2，然后将参数设置好便可。

当然，可以不传参数，那意味着一段语句重复执行3次。故你可以使用批量更新的方法不带参数去实现批量执行某条语句。

#### 批量删除

```java
@Test
public void testBatchDelete() {
    //① 写SQL语句
    String sql = "delete from stu where id = ? ";
    Object[][] param = new Object[3][1];
    param[0][0] = 10;
    param[1][0] = 11;
    param[2][0] = 12;
    //② 调用对应的方法
    DbUtils.batchInsert(connection, sql, 3, param);
    //③ 关闭连接
    DbUtils.closeResource(connection);
}
```

由于批量删除是应该携带参数的，不带参数重复去执行一条删除语句是没有意义的，故不提供。

希望大家能给一个star，并强烈欢迎各位提供更多的方法完善此工具