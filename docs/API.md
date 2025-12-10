# API接口清单

## 基础URL

```
http://localhost:8080/ontology/api
```

## 接口列表

### 1. 系统接口

#### 1.1 健康检查
- **方法**: GET
- **路径**: `/health`
- **描述**: 检查服务运行状态
- **响应示例**:
```json
{
  "status": "UP",
  "service": "Transfer Order Ontology Service",
  "version": "1.0.0"
}
```

#### 1.2 获取本体信息
- **方法**: GET
- **路径**: `/ontology/info`
- **描述**: 获取OWL本体的基本统计信息
- **响应示例**:
```json
{
  "iri": "https://iwhalecloud.com/ontology/transfer#TransferOrderOntology",
  "classCount": 30,
  "objectPropertyCount": 15,
  "dataPropertyCount": 45,
  "individualCount": 12,
  "axiomCount": 450,
  "namespace": "https://iwhalecloud.com/ontology/transfer#"
}
```

---

### 2. 类（Class）接口

#### 2.1 获取所有类
- **方法**: GET
- **路径**: `/classes`
- **描述**: 获取本体中定义的所有类
- **响应示例**:
```json
[
  {
    "name": "SourceCustomer",
    "iri": "https://iwhalecloud.com/ontology/transfer#SourceCustomer",
    "label": "源客户"
  },
  {
    "name": "TargetCustomer",
    "iri": "https://iwhalecloud.com/ontology/transfer#TargetCustomer",
    "label": "目标客户"
  }
]
```

---

### 3. 个体（Individual）接口

#### 3.1 获取所有个体
- **方法**: GET
- **路径**: `/individuals`
- **描述**: 获取本体中的所有个体实例
- **响应示例**:
```json
[
  {
    "name": "PartyManagementComponent",
    "iri": "https://iwhalecloud.com/ontology/transfer#PartyManagementComponent",
    "types": ["ODAComponent"]
  }
]
```

#### 3.2 根据类名获取个体
- **方法**: GET
- **路径**: `/individuals/class/{className}`
- **描述**: 获取指定类的所有实例
- **参数**: 
  - `className` (路径参数): 类名，如 `ODAComponent`
- **示例**: `/individuals/class/ODAComponent`
- **响应示例**:
```json
[
  {
    "name": "PartyManagementComponent",
    "iri": "https://iwhalecloud.com/ontology/transfer#PartyManagementComponent",
    "types": ["ODAComponent"]
  }
]
```

#### 3.3 获取个体属性
- **方法**: GET
- **路径**: `/individuals/{individualName}/properties`
- **描述**: 获取指定个体的所有属性（数据属性和对象属性）
- **参数**:
  - `individualName` (路径参数): 个体名称
- **示例**: `/individuals/PartyManagementComponent/properties`
- **响应示例**:
```json
{
  "individual": "PartyManagementComponent",
  "iri": "https://iwhalecloud.com/ontology/transfer#PartyManagementComponent",
  "dataProperties": {
    "componentCode": "PartyManagementComponent",
    "componentName": "Party Management Component",
    "tmfComponent": "ODA.PartyManagement"
  },
  "objectProperties": {}
}
```

#### 3.4 添加新个体
- **方法**: POST
- **路径**: `/individuals`
- **描述**: 创建一个新的个体实例
- **请求体**:
```json
{
  "className": "SourceCustomer",
  "individualName": "TestCustomer001"
}
```
- **响应示例**:
```json
{
  "individual": "TestCustomer001",
  "class": "SourceCustomer",
  "iri": "https://iwhalecloud.com/ontology/transfer#TestCustomer001"
}
```

---

### 4. 属性（Property）接口

#### 4.1 添加数据属性
- **方法**: POST
- **路径**: `/individuals/{individualName}/data-property`
- **描述**: 为指定个体添加数据属性
- **参数**:
  - `individualName` (路径参数): 个体名称
- **请求体**:
```json
{
  "propertyName": "custName",
  "value": "张三",
  "datatype": "string"
}
```
- **数据类型**: `string`, `integer`, `boolean`, `decimal`
- **响应示例**:
```json
{
  "status": "success",
  "message": "数据属性添加成功"
}
```

#### 4.2 添加对象属性
- **方法**: POST
- **路径**: `/individuals/{sourceIndividual}/object-property`
- **描述**: 为指定个体添加对象属性（建立个体间关系）
- **参数**:
  - `sourceIndividual` (路径参数): 源个体名称
- **请求体**:
```json
{
  "propertyName": "hasSourceCustomer",
  "targetIndividual": "TestCustomer001"
}
```
- **响应示例**:
```json
{
  "status": "success",
  "message": "对象属性添加成功"
}
```

---

### 5. 推理（Reasoning）接口

#### 5.1 执行SWRL推理
- **方法**: POST
- **路径**: `/reasoning/swrl`
- **描述**: 执行SWRL规则推理引擎
- **响应示例**:
```json
{
  "status": "success",
  "message": "SWRL推理执行成功",
  "ruleCount": 5
}
```

---

### 6. 业务接口

#### 6.1 创建过户订单示例
- **方法**: POST
- **路径**: `/transfer/create-order-example`
- **描述**: 创建完整的过户订单示例，包括源客户、目标客户、订单和订阅
- **请求体**:
```json
{
  "orderId": "ORDER_20251209001",
  "sourceCustomerId": "CUST_SOURCE_001",
  "targetCustomerId": "CUST_TARGET_001"
}
```
- **响应示例**:
```json
{
  "status": "success",
  "message": "过户订单示例创建成功",
  "orderId": "ORDER_20251209001",
  "sourceCustomerId": "CUST_SOURCE_001",
  "targetCustomerId": "CUST_TARGET_001",
  "subscriptionId": "SUB_1733756400000"
}
```

#### 6.2 检查客户状态
- **方法**: POST
- **路径**: `/transfer/check-customer-status`
- **描述**: 创建客户并检查其状态（涉诈、欠费等），执行SWRL规则推理
- **请求体**:
```json
{
  "customerId": "CUST001",
  "custStatus": "NORMAL",
  "arrearsStatus": "NO_ARREARS"
}
```
- **客户状态值**:
  - `custStatus`: `NORMAL` (正常), `FRAUD` (涉诈), `SUSPENDED` (暂停)
  - `arrearsStatus`: `NO_ARREARS` (无欠费), `ARREARS` (欠费)
- **响应示例**:
```json
{
  "status": "success",
  "customerId": "CUST001",
  "properties": {
    "individual": "CUST001",
    "iri": "https://iwhalecloud.com/ontology/transfer#CUST001",
    "dataProperties": {
      "custId": "CUST001",
      "custStatus": "NORMAL",
      "arrearsStatus": "NO_ARREARS"
    },
    "objectProperties": {}
  },
  "reasoningResult": {
    "status": "success",
    "message": "SWRL推理执行成功",
    "ruleCount": 5
  }
}
```

---

## 错误响应

所有接口在发生错误时返回类似格式：

```json
{
  "status": "error",
  "message": "错误描述信息"
}
```

HTTP状态码：
- `200 OK`: 请求成功
- `500 Internal Server Error`: 服务器内部错误

---

## 完整示例

### 创建完整的过户流程

```bash
# 1. 创建源客户
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{"className":"SourceCustomer","individualName":"Customer_Zhang"}'

# 2. 添加客户属性
curl -X POST http://localhost:8080/ontology/api/individuals/Customer_Zhang/data-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"custName","value":"张三","datatype":"string"}'

curl -X POST http://localhost:8080/ontology/api/individuals/Customer_Zhang/data-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"custId","value":"CUST001","datatype":"string"}'

curl -X POST http://localhost:8080/ontology/api/individuals/Customer_Zhang/data-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"custStatus","value":"NORMAL","datatype":"string"}'

# 3. 创建目标客户
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{"className":"TargetCustomer","individualName":"Customer_Li"}'

# 4. 创建过户订单
curl -X POST http://localhost:8080/ontology/api/individuals \
  -H "Content-Type: application/json" \
  -d '{"className":"TransferOrder","individualName":"Order_001"}'

# 5. 建立订单与客户的关系
curl -X POST http://localhost:8080/ontology/api/individuals/Order_001/object-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"hasSourceCustomer","targetIndividual":"Customer_Zhang"}'

curl -X POST http://localhost:8080/ontology/api/individuals/Order_001/object-property \
  -H "Content-Type: application/json" \
  -d '{"propertyName":"hasTargetCustomer","targetIndividual":"Customer_Li"}'

# 6. 执行SWRL推理
curl -X POST http://localhost:8080/ontology/api/reasoning/swrl

# 7. 查询订单属性
curl http://localhost:8080/ontology/api/individuals/Order_001/properties
```

---

## 测试建议

使用提供的测试脚本快速测试所有接口：

```bash
# 完整测试
./test-api.sh

# 单个测试
./test-api.sh health
./test-api.sh info
./test-api.sh classes
./test-api.sh individuals
./test-api.sh order
./test-api.sh reasoning
```

---

## 注意事项

1. 所有数据属性和对象属性名称需要在OWL本体中预先定义
2. 个体名称在本体中必须唯一
3. SWRL推理只在调用推理接口时执行，不会自动触发
4. 推理结果会修改本体内容，但不会持久化到文件（重启服务后恢复）

---

**更新时间**: 2025-12-09  
**版本**: 1.0.0
