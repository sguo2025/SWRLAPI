# BSS4.0 客户过户受理本体服务

基于Spring Boot 3.5.3、JDK 21、SWRLAPI和OWLAPI的客户过户本体推理服务。

## 项目结构

```
SWRLAPI/
├── pom.xml                                 # Maven项目配置
├── src/
│   └── main/
│       ├── java/com/iwhalecloud/ontology/
│       │   ├── TransferOrderOntologyApplication.java  # 主启动类
│       │   ├── controller/
│       │   │   └── OntologyController.java           # REST控制器
│       │   └── service/
│       │       └── OntologyService.java              # OWL服务
│       └── resources/
│           ├── application.yml                       # 应用配置
│           └── owl/
│               └── transfer_order_ontology.owl       # OWL本体文件
├── test-api.sh                            # API测试脚本
└── README.md                              # 项目说明
```

## 技术栈

- **JDK**: 21
- **Spring Boot**: 3.5.3
- **OWL API**: 5.5.1
- **SWRL API**: 2.1.2
- **Maven**: 构建工具

## 功能特性

- ✅ OWL本体加载和管理
- ✅ SWRL规则推理引擎
- ✅ REST API接口
- ✅ 客户过户业务建模
- ✅ 涉诈和欠费检查规则
- ✅ 8步过户流程管理

## 快速开始

### 1. 编译项目

```bash
cd /workspaces/SWRLAPI
mvn clean package -DskipTests
```

### 2. 启动服务

```bash
java -jar target/transfer-order-ontology-1.0.0.jar
```

或使用Maven运行：

```bash
mvn spring-boot:run
```

### 3. 验证服务

服务启动后，访问健康检查接口：

```bash
curl http://localhost:8080/ontology/api/health
```

## API接口文档

### 基础接口

#### 1. 健康检查
```bash
curl http://localhost:8080/ontology/api/health
```

#### 2. 获取本体信息
```bash
curl http://localhost:8080/ontology/api/ontology/info
```

#### 3. 获取所有类
```bash
curl http://localhost:8080/ontology/api/classes
```

#### 4. 获取所有个体
```bash
curl http://localhost:8080/ontology/api/individuals
```

#### 5. 根据类名获取个体
```bash
curl http://localhost:8080/ontology/api/individuals/class/ODAComponent
```

#### 6. 获取个体属性
```bash
curl http://localhost:8080/ontology/api/individuals/PartyManagementComponent/properties
```

### 高级接口

#### 7. 添加新个体
```bash
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{
    "className": "SourceCustomer",
    "individualName": "TestCustomer001"
  }'
```

#### 8. 添加数据属性
```bash
curl -X POST http://localhost:8080/ontology/api/individuals/TestCustomer001/data-property \
  -H "Content-Type: application/json" \
  -d '{
    "propertyName": "custName",
    "value": "张三",
    "datatype": "string"
  }'
```

#### 9. 添加对象属性
```bash
curl -X POST http://localhost:8080/ontology/api/individuals/ORDER001/object-property \
  -H "Content-Type: application/json" \
  -d '{
    "propertyName": "hasSourceCustomer",
    "targetIndividual": "TestCustomer001"
  }'
```

#### 10. 执行SWRL推理
```bash
curl -X POST http://localhost:8080/ontology/api/reasoning/swrl
```

### 业务接口

#### 11. 创建过户订单示例
```bash
curl -X POST http://localhost:8080/ontology/api/transfer/create-order-example \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_20231209001",
    "sourceCustomerId": "CUST_SOURCE_001",
    "targetCustomerId": "CUST_TARGET_001"
  }'
```

#### 12. 检查客户状态（正常）
```bash
curl -X POST http://localhost:8080/ontology/api/transfer/check-customer-status \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "custStatus": "NORMAL",
    "arrearsStatus": "NO_ARREARS"
  }'
```

#### 13. 检查客户状态（涉诈）
```bash
curl -X POST http://localhost:8080/ontology/api/transfer/check-customer-status \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST002",
    "custStatus": "FRAUD",
    "arrearsStatus": "NO_ARREARS"
  }'
```

#### 14. 检查客户状态（欠费）
```bash
curl -X POST http://localhost:8080/ontology/api/transfer/check-customer-status \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST003",
    "custStatus": "NORMAL",
    "arrearsStatus": "ARREARS"
  }'
```

## 自动化测试

项目包含完整的Shell测试脚本，可以一键测试所有API接口：

```bash
# 添加执行权限
chmod +x test-api.sh

# 运行所有测试
./test-api.sh

# 运行单个测试
./test-api.sh health      # 健康检查
./test-api.sh info        # 本体信息
./test-api.sh classes     # 类列表
./test-api.sh individuals # 个体列表
./test-api.sh order       # 创建订单
./test-api.sh reasoning   # 执行推理
```

## 本体模型说明

### 核心类

- **DomainObject**: 业务域对象基类
- **SourceCustomer**: 源客户（过户方）
- **TargetCustomer**: 目标客户（接收方）
- **TransferOrder**: 过户订单
- **TransferableSubscription**: 可转移订阅
- **AuthorizationRecord**: 鉴权记录
- **PaymentRecord**: 缴费记录
- **ProcessStep**: 流程步骤

### 8步过户流程

1. **Step1_LocateSourceCustomer**: 定位源客户
2. **Step2_SelectTransferNumber**: 过户号码选择
3. **Step3_CreateCustomerOrder**: 创建客户订单
4. **Step4_InitTransferBusiness**: 过户业务初始化
5. **Step5_InitCommonAttributes**: 公共属性初始化
6. **Step6_ConfirmTargetCustomer**: 目标客户确认
7. **Step7_SaveOrder**: 订单保存
8. **Step8_ConfirmOrder**: 订单确认

### SWRL业务规则

1. **FraudCustomerCheckRule**: 涉诈客户检查
   - 如果客户状态为"FRAUD"，阻断所有业务操作
   
2. **ArrearsCheckRule**: 欠费客户检查
   - 如果客户存在欠费，阻断过户业务操作

3. **TransferEligibilityRule**: 过户资格校验
   - 检查源客户鉴权是否通过

4. **PaymentConfirmationRule**: 缴费确认规则
   - 检查支付状态是否完成

## 配置说明

修改 `src/main/resources/application.yml`:

```yaml
server:
  port: 8080                    # 服务端口
  servlet:
    context-path: /ontology     # 上下文路径

ontology:
  file:
    path: classpath:owl/transfer_order_ontology.owl  # OWL文件路径
  namespace: https://iwhalecloud.com/ontology/transfer#  # 命名空间
```

## 开发说明

### 添加新的SWRL规则

在OWL文件中添加新的BusinessLogic个体：

```turtle
transfer:YourNewRule a transfer:BusinessLogic ;
    transfer:logicCode "YourNewRule" ;
    transfer:logicType "SWRL" ;
    transfer:logicExpression "YourSWRLExpression" ;
    rdfs:label "新规则"@zh .
```

### 扩展REST接口

在 `OntologyController.java` 中添加新的接口方法，在 `OntologyService.java` 中实现业务逻辑。

## 故障排查

### 服务无法启动

1. 检查JDK版本：`java -version` 应该是21+
2. 检查端口占用：`lsof -i :8080`
3. 查看日志：检查控制台输出

### OWL文件加载失败

1. 确认文件位置：`src/main/resources/owl/transfer_order_ontology.owl`
2. 检查文件格式：确保是有效的OWL/Turtle格式
3. 查看详细错误信息

### SWRL推理失败

1. 检查SWRL规则语法
2. 确认所有引用的类和属性存在
3. 查看推理引擎日志

## 许可证

Copyright © 2025 sguo