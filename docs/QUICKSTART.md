# 快速开始指南

本指南将帮助您快速启动和测试BSS4.0客户过户受理本体服务。

## 前置要求

- JDK 21+
- Maven 3.6+
- curl命令行工具
- jq（JSON格式化工具，可选）

## 一键启动

### 方式1: 使用启动脚本（推荐）

```bash
# 一键启动服务（检查环境 + 编译 + 启动）
./start.sh all

# 或分步执行
./start.sh check    # 检查环境
./start.sh build    # 编译项目
./start.sh start    # 启动服务
./start.sh dev      # 开发模式（Maven方式）
```

### 方式2: 手动启动

```bash
# 1. 编译项目
mvn clean package -DskipTests

# 2. 启动服务
java -jar target/transfer-order-ontology-1.0.0.jar

# 或者使用Maven插件启动
mvn spring-boot:run
```

## 验证服务

服务启动后，在新终端执行：

```bash
# 健康检查
curl http://localhost:8080/ontology/api/health

# 应该返回：
# {"status":"UP","service":"Transfer Order Ontology Service","version":"1.0.0"}
```

## 快速测试

### 1. 获取本体信息

```bash
curl http://localhost:8080/ontology/api/ontology/info | jq
```

### 2. 查看所有类

```bash
curl http://localhost:8080/ontology/api/classes | jq
```

### 3. 查看ODA组件

```bash
curl http://localhost:8080/ontology/api/individuals/class/ODAComponent | jq
```

### 4. 创建过户订单示例

```bash
curl -X POST http://localhost:8080/ontology/api/transfer/create-order-example \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_TEST_001",
    "sourceCustomerId": "CUST_SOURCE_001",
    "targetCustomerId": "CUST_TARGET_001"
  }' | jq
```

### 5. 测试涉诈客户检查

```bash
curl -X POST http://localhost:8080/ontology/api/transfer/check-customer-status \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "FRAUD_CUST_001",
    "custStatus": "FRAUD",
    "arrearsStatus": "NO_ARREARS"
  }' | jq
```

### 6. 测试欠费客户检查

```bash
curl -X POST http://localhost:8080/ontology/api/transfer/check-customer-status \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "ARREARS_CUST_001",
    "custStatus": "NORMAL",
    "arrearsStatus": "ARREARS"
  }' | jq
```

## 自动化测试

运行完整的API测试套件：

```bash
# 确保服务已启动，然后在另一个终端执行：
./test-api.sh

# 或运行单个测试
./test-api.sh health      # 健康检查
./test-api.sh info        # 本体信息
./test-api.sh order       # 创建订单
./test-api.sh reasoning   # SWRL推理
```

## 典型使用场景

### 场景1: 创建完整的过户流程

```bash
# 1. 创建源客户
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{"className":"SourceCustomer","individualName":"Customer_Zhang"}'

# 2. 为客户添加属性
curl -X POST http://localhost:8080/ontology/api/individuals/Customer_Zhang/data-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"custName","value":"张三","datatype":"string"}'

# 3. 创建目标客户
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{"className":"TargetCustomer","individualName":"Customer_Li"}'

# 4. 创建过户订单并建立关系
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{"className":"TransferOrder","individualName":"Order_20251209"}'

# 5. 建立源客户关系
curl -X POST http://localhost:8080/ontology/api/individuals/Order_20251209/object-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"hasSourceCustomer","targetIndividual":"Customer_Zhang"}'

# 6. 执行SWRL推理
curl -X POST http://localhost:8080/ontology/api/reasoning/swrl
```

### 场景2: 查询和分析

```bash
# 查询特定类的所有实例
curl http://localhost:8080/ontology/api/individuals/class/SourceCustomer | jq

# 查询个体的所有属性
curl http://localhost:8080/ontology/api/individuals/Customer_Zhang/properties | jq

# 查询ODA组件
curl http://localhost:8080/ontology/api/individuals/class/ODAComponent | jq

# 查询业务规则
curl http://localhost:8080/ontology/api/individuals/class/BusinessLogic | jq
```

## 常见问题

### Q1: 端口被占用怎么办？

修改 `src/main/resources/application.yml` 中的端口：

```yaml
server:
  port: 8081  # 改为其他端口
```

### Q2: 如何查看日志？

服务启动后会在控制台输出日志。如需调整日志级别，修改 `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.iwhalecloud: DEBUG  # 改为INFO减少日志输出
```

### Q3: Maven编译失败？

```bash
# 清理缓存重新编译
mvn clean install -U

# 或跳过测试
mvn clean package -DskipTests
```

### Q4: 如何重新加载OWL文件？

重启服务即可，服务启动时会自动加载 `src/main/resources/owl/transfer_order_ontology.owl`

## 下一步

- 阅读完整的 [README.md](README.md) 了解详细信息
- 查看 [transfer_order_ontology.owl](src/main/resources/owl/transfer_order_ontology.owl) 了解本体模型
- 查看 [OntologyController.java](src/main/java/com/iwhalecloud/ontology/controller/OntologyController.java) 了解API实现
- 修改和扩展SWRL规则

## 停止服务

在服务运行的终端按 `Ctrl + C` 即可停止服务。

## 技术支持


