# Jena依赖冲突解决方案

## 问题分析

**错误信息**:
```
java.lang.IllegalAccessError: failed to access class org.apache.jena.assembler.ConstAssembler 
from class org.apache.jena.tdb2.assembler.VocabTDB2
```

**根本原因**:
- `jena-tdb2` 模块依赖 `jena-assembler`
- 早期版本的pom.xml中只包含了 `jena-core` 和 `jena-arq`
- 缺少 `jena-tdb2`、`jena-assembler` 和 `jena-base` 导致模块初始化失败
- Jena会在运行时尝试初始化所有模块，缺少部分模块会导致类加载顺序错误

## 解决方案

### 修改pom.xml

在 `<dependencies>` 部分添加以下依赖（确保版本一致）：

```xml
<!-- Apache Commons Lang3 - 修复依赖冲突 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>

<!-- Jena Core - 基础RDF库 -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-core</artifactId>
    <version>4.8.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Jena ARQ - SPARQL查询引擎 -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-arq</artifactId>
    <version>4.8.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Jena TDB2 - 三元组数据库 -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-tdb2</artifactId>
    <version>4.8.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Jena Assembler - 组件装配 -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-assembler</artifactId>
    <version>4.8.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Jena Base - Jena基础模块 -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-base</artifactId>
    <version>4.8.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 关键要点

1. **版本一致性**: 所有Jena模块使用 `4.8.0` 版本
2. **排除冲突**: 从每个Jena模块排除 `commons-lang3`，统一使用 `3.14.0` 版本
3. **模块完整性**: 确保包含所有必要的Jena模块：
   - `jena-core` - 基础RDF功能
   - `jena-arq` - SPARQL查询支持
   - `jena-tdb2` - 三元组存储
   - `jena-assembler` - 配置装配
   - `jena-base` - 基础设施

## 编译步骤

```bash
# 清除Maven缓存
rm -rf ~/.m2/repository/org/apache/jena

# 重新编译
cd /workspaces/SWRLAPI
mvn clean package -DskipTests
```

## 验证

编译成功后应该看到：
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

## 后续使用

### 使用Jena进行SPARQL查询

```java
try (QueryExecution qexec = QueryExecution.create()
        .query(jenaQuery)
        .dataset(dataset)
        .build()) {
    ResultSet results = qexec.execSelect();
    while (results.hasNext()) {
        QuerySolution soln = results.nextSolution();
        // 处理结果
    }
}
```

### Jena初始化

当应用启动时，Jena会自动初始化所有模块。现在已经包含了所有必要的模块，初始化应该能够成功完成。

## 相关文件

- 修改文件: [pom.xml](pom.xml)
- 影响范围: Maven依赖管理
- 编译选项: `-DskipTests`

## 参考链接

- [Apache Jena官方文档](https://jena.apache.org/)
- [Jena模块依赖关系](https://jena.apache.org/documentation/index.html)
- [SWRLAPI与OWL API集成](https://github.com/protegeproject/swrlapi)
