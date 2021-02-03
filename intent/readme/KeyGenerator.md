## 返回插入的主键

Q: MyBatis是如何返回插入的主键的呢？比如下面的代码：

```java
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    @Insert("insert into user (name, password) values(#{name}, #{password})")
    int insertUser(UserEntity userEntity);
}
```

加上@Options注解后就能自动设置主键到UserEntity的id属性上，这是怎么实现的呢？（这里必须给`keyProperty`属性值，值是UserEntity的id名称）

A:
在MyBatis初始化的时候，会扫描所有的Mapper，并且根据Mapper里面的方法（注解或XML）生成对应的Statement，保存在`org.apache.ibatis.session.Configuration#mappedStatements`
里面，生成的具体过程看：

```java
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.scripting.LanguageDriver;

/**
 * {@link org.apache.ibatis.builder.MapperBuilderAssistant#addMappedStatement(String, SqlSource, StatementType, SqlCommandType, Integer, Integer, String, Class, String, Class, ResultSetType, boolean, boolean, boolean, KeyGenerator, String, String, String, LanguageDriver, String)}
 */
public class Test {
}
```

生成完毕后重点关心`MappedStatement`的`keyProperties`和`keyGenerator`属性，`keyProperties`后面填充属性值的时候必须要用到这个属性，而`keyGenerator`
属性是生成的对应处理key方式，生成策略在`MapperAnnotationBuilder#parseStatement`搜索keyGenerator。

然后是调用Mapper的插入方法的时候，通过一系列的代理，最后来到了`SimpleExecutor#doUpdate`（也可能是其他的Executor）的方法，在`doUpdate`方法里面首先获取了`StatementHandler`，

是通过`Configuration#newStatementHandler`方法创建一个`RoutingStatementHandler`，`RoutingStatementHandler`
再根据`MappedStatement#statementType`（不指定一般都为`StatementType#PREPARED`，可以通过`@Options`和`SelectKey`指定类型,`@Options`
优先级比较高，参考`MapperAnnotationBuilder#parseStatement`或`XMLStatementBuilder#parseStatementNode`搜索statementType）

创建对应的`StatementHandler`，这里使用默认的`StatementType#PREPARED`所以创建的`StatementHandler`为`PreparedStatementHandler`
，然后执行`PreparedStatementHandler#update`方法，`PreparedStatementHandler#update`方法中获取了之前生成的`KeyGenerator`，因为这里注解`@Options`
的`useGeneratedKeys`为`true`，所以生成的`KeyGenerator`为`Jdbc3KeyGenerator`

执行了`Jdbc3KeyGenerator#processAfter`方法，`processAfter`方法执行`processBatch`方法，在`processBatch`中获取了`MappedStatement`
的`keyProperties`属性，并且调用了`java.sql.Statement#getGeneratedKeys`方法获取返回的Id，这个是`java.sql`
包里面的方法，注释可以看到返回的是`java.sql.ResultSet`，如果没有生成主键（比如查询、更新、删除语句）可能返回空的ResultSet。

如果`keyProperties`属性不为null并且sql执行返回的`ResultSetMetaData`条数大于等于`keyProperties`
的长度，则进行属性的填充。最后走到`Jdbc3KeyGenerator#populateKeys`方法填充插入返回的`id`属性。