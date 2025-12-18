# logicExpression 推理实现指南

## 一、核心架构

```
┌─────────────────────────────────────────────────────────────┐
│                 OWL本体 + logicExpression                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
        ┌──────────────────────────────────────────┐
        │   REST API 请求推理                      │
        │  /reasoning/execute-rule/{ruleCode}      │
        │  /reasoning/execute-all/{orderId}        │
        └──────────────────────────────────────────┘
                              ↓
        ┌──────────────────────────────────────────────────┐
        │   ProcessReasoningController                     │
        │   (REST 端点处理)                                 │
        └──────────────────────────────────────────────────┘
                              ↓
        ┌──────────────────────────────────────────────────┐
        │   ProcessReasoningService                        │
        │  - executeRuleByCode()                           │
        │  - executeAllRulesReasoning()                    │
        │  - executeDecisionTableRule()                    │
        └──────────────────────────────────────────────────┘
                              ↓
        ┌──────────────────────────────────────────────────┐
        │   SWRLReasoningExecutor                          │
        │  - parseSWRLExpression()        [解析]           │
        │  - performReasoning()           [推理]           │
        │  - applyConsequent()            [应用]           │
        └──────────────────────────────────────────────────┘
```

---

## 二、logicExpression 格式

### 2.1 SWRL规则格式

```prolog
前提条件(Antecedent) -> 结论(Consequent)
```

#### 完整示例：

```prolog
# 涉诈客户检查规则
DomainObject(?c) ^ custStatus(?c, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'FRAUD') 
  ^ (SourceCustomer(?c) | TargetCustomer(?c)) 
  -> blockBusinessOperation(?c, true) ^ businessAction(?c, 'STOP_ALL_OPERATIONS') 
  ^ actionMessage(?c, '涉诈用户不允许办理任何业务，待涉诈解除后方可继续办理')
```

#### 语法说明：

| 元素 | 说明 | 示例 |
|------|------|------|
| `?var` | 变量（以?开头） | `?customer`, `?status` |
| `^` | 逻辑与（AND） | `A ^ B ^ C` |
| `\|` | 逻辑或（OR） | `ClassA(?x) \| ClassB(?x)` |
| `->` | 规则分隔符 | `前提 -> 结论` |
| `swrlb:` | 内置函数 | `swrlb:stringEqualIgnoreCase(?s, 'value')` |
| `(...)` | 分组 | `(A(?x) \| B(?x)) ^ C(?x)` |

#### 支持的内置函数：

```
swrlb:stringEqualIgnoreCase(?x, ?y)     # 字符串不区分大小写比较
swrlb:stringConcat(?result, ?s1, ?s2)  # 字符串连接
swrlb:add(?result, ?x, ?y)              # 数值加法
swrlb:subtract(?result, ?x, ?y)         # 数值减法
swrlb:lessThan(?x, ?y)                  # 小于
swrlb:greaterThan(?x, ?y)               # 大于
swrlb:equal(?x, ?y)                     # 相等
```

### 2.2 决策表格式

```
IF 条件表达式 THEN 动作表达式
```

#### 完整示例：

```
IF subscriptionCount > 1 THEN customerSelectionRequired = true
```

---

## 三、OWL本体中的规则定义

### 3.1 BusinessLogic个体定义

```xml
<owl:NamedIndividual rdf:about="&transfer;BlacklistCheckRule">
    <rdf:type rdf:resource="&transfer;BusinessLogic"/>
    
    <!-- 规则代码 -->
    <transfer:logicCode>BlacklistCheckRule</transfer:logicCode>
    
    <!-- 规则类型 -->
    <transfer:logicType>SWRL</transfer:logicType>
    
    <!-- 规则表达式 -->
    <transfer:logicExpression>
        transfer:Step02_VerifySourceCustomer(?step) ^
        base:requiresEntity(?step, ?customer) ^
        base:SourceCustomer(?customer) ^
        base:isBlacklisted(?customer, true) ^
        base:custName(?customer, ?custName)
        ->
        base:operationResult(?step, "客户处于黑名单，请解除黑名单后再进行过户操作")
    </transfer:logicExpression>
    
    <!-- 规则描述 -->
    <rdfs:label xml:lang="zh">黑名单检查规则</rdfs:label>
    <rdfs:comment>检查源客户是否处于黑名单状态</rdfs:comment>
</owl:NamedIndividual>
```

---

## 四、推理执行流程

### 4.1 单个规则推理

```
1. REST请求
   POST /api/process/reasoning/execute-rule/FraudCustomerCheckRule
   Content-Type: application/json
   {
     "customerId": "CUST001",
     "status": "FRAUD"
   }

2. ProcessReasoningController 接收请求
   → 调用 executeRuleByCode("FraudCustomerCheckRule", context)

3. ProcessReasoningService 处理
   → 获取规则定义
   → 提取 logicExpression
   → 确定规则类型 (SWRL/DecisionTable)

4. SWRLReasoningExecutor 执行推理
   a) 解析 logicExpression
      - 分离前提条件和结论
      - 提取变量和原子公式
      - 验证语法
   
   b) 验证规则元素
      - 检查类/属性在本体中是否存在
      - 收集缺失元素
   
   c) 执行推理
      - 查询满足前提条件的实例
      - 对每个实例应用结论
      - 收集推论结果

5. 返回推理结果
   {
     "status": "success",
     "ruleCode": "FraudCustomerCheckRule",
     "ruleType": "SWRL",
     "inferences": [
       {
         "matchedInstance": {...},
         "consequence": {...}
       }
     ]
   }
```

### 4.2 所有规则推理

```
POST /api/process/reasoning/execute-all/ORDER001
Content-Type: application/json
{
  "sourceCustomerId": "CUST001",
  "targetCustomerId": "CUST002",
  "status": "FRAUD"
}

↓

ProcessReasoningService.executeAllRulesReasoning()

↓

循环执行所有已加载的规则：
  - FraudCustomerCheckRule
  - ArrearsCheckRule
  - BlacklistCheckRule
  - AuthenticationCheckRule
  - PaymentCheckRule
  - ...

↓

聚合所有推理结果
```

---

## 五、关键类详解

### 5.1 SWRLReasoningExecutor

**核心方法：**

```java
// 执行SWRL表达式推理
public Map<String, Object> executeSWRLExpression(
    OWLOntology ontology,
    String swrlExpression,
    Map<String, Object> context)

// 解析SWRL表达式
private SWRLRuleInfo parseSWRLExpression(String expression)

// 执行推理
private Map<String, Object> performReasoning(
    OWLOntology ontology,
    SWRLRuleInfo ruleInfo,
    Map<String, Object> context)

// 应用结论
private Map<String, Object> applyConsequent(
    OWLOntology ontology,
    SWRLRuleInfo ruleInfo,
    Map<String, Object> matchedInstance,
    Map<String, Object> context)
```

**内部数据结构：**

```java
public static class SWRLRuleInfo {
    private boolean valid;                    // 规则是否有效
    private String errorMessage;              // 错误信息
    private String antecedent;                // 前提条件原始文本
    private String consequent;                // 结论原始文本
    private List<String> antecedentAtoms;    // 前提条件原子公式列表
    private List<String> consequentAtoms;    // 结论原子公式列表
    private Set<String> variables;            // 规则中出现的变量
}
```

### 5.2 ProcessReasoningService

**新增方法：**

```java
// 根据规则代码执行推理
public Map<String, Object> executeRuleByCode(String ruleCode, Map<String, Object> context)

// 执行决策表规则
private Map<String, Object> executeDecisionTableRule(String expression, Map<String, Object> context)

// 评估条件
private boolean evaluateCondition(String condition, Map<String, Object> context)

// 执行所有规则推理
public Map<String, Object> executeAllRulesReasoning(String orderId, Map<String, Object> context)
```

---

## 六、REST API 端点

### 6.1 获取规则表达式

```http
GET /api/process/rule/{ruleCode}/expression

例如:
GET /api/process/rule/FraudCustomerCheckRule/expression

响应:
{
  "status": "success",
  "ruleCode": "FraudCustomerCheckRule",
  "expression": "DomainObject(?c) ^ custStatus(?c, ?status) ^ ...",
  "type": "SWRL",
  "priority": 10,
  "description": "客户涉诈检查规则"
}
```

### 6.2 执行单个规则推理

```http
POST /api/process/reasoning/execute-rule/{ruleCode}

例如:
POST /api/process/reasoning/execute-rule/FraudCustomerCheckRule

请求体:
{
  "customerId": "CUST001",
  "status": "FRAUD",
  "sourceCustomerName": "张三"
}

响应:
{
  "status": "success",
  "ruleCode": "FraudCustomerCheckRule",
  "ruleType": "SWRL",
  "rule": { /* 完整规则定义 */ },
  "reasoningResult": {
    "status": "success",
    "inferences": [
      {
        "matchedInstance": { /* 匹配的实例数据 */ },
        "consequence": {
          "status": "applied",
          "conclusions": [
            {
              "predicate": "blockBusinessOperation",
              "arguments": "?c, true",
              "value": true
            }
          ]
        }
      }
    ],
    "totalInferences": 1
  },
  "message": "规则执行完成"
}
```

### 6.3 执行所有规则推理

```http
POST /api/process/reasoning/execute-all/{orderId}

例如:
POST /api/process/reasoning/execute-all/ORDER001

请求体:
{
  "sourceCustomerId": "CUST001",
  "targetCustomerId": "CUST002",
  "status": "FRAUD",
  "arrearsStatus": "CLEAR",
  "authStatus": "PASSED"
}

响应:
{
  "status": "success",
  "orderId": "ORDER001",
  "totalRules": 6,
  "successCount": 6,
  "failCount": 0,
  "ruleResults": [
    {
      "status": "success",
      "ruleCode": "FraudCustomerCheckRule",
      "reasoningResult": { /* 推理结果 */ }
    },
    { /* 其他规则结果 */ }
  ],
  "message": "推理完成: 6成功, 0失败"
}
```

---

## 七、实现示例

### 7.1 简单SWRL规则推理

**OWL中的规则定义：**

```xml
<transfer:FraudCheckRule>
    <logicType>SWRL</logicType>
    <logicExpression>
        Customer(?c) ^ hasCustStatus(?c, ?status) 
        ^ swrlb:stringEqualIgnoreCase(?status, 'FRAUD') 
        -> blockTransfer(?c)
    </logicExpression>
</transfer:FraudCheckRule>
```

**执行推理：**

```bash
curl -X POST http://localhost:8080/ontology/api/process/reasoning/execute-rule/FraudCheckRule \
  -H "Content-Type: application/json" \
  -d '{
    "customerStatus": "FRAUD",
    "customerId": "CUST001"
  }' | jq .inferences
```

### 7.2 决策表推理

**OWL中的规则定义：**

```xml
<transfer:MultiInstanceSelectionRule>
    <logicType>DecisionTable</logicType>
    <logicExpression>
        IF subscriptionCount > 1 THEN customerSelectionRequired = true
    </logicExpression>
</transfer:MultiInstanceSelectionRule>
```

**执行推理：**

```bash
curl -X POST http://localhost:8080/ontology/api/process/reasoning/execute-rule/MultiInstanceSelectionRule \
  -H "Content-Type: application/json" \
  -d '{
    "subscriptionCount": 3
  }' | jq .reasoningResult
```

### 7.3 完整工作流推理

```bash
# 步骤1：初始化规则
curl -X POST http://localhost:8080/ontology/api/process/init-swrl-rules | jq .

# 步骤2：获取所有规则
curl -X GET http://localhost:8080/ontology/api/process/loaded-rules | jq .rules

# 步骤3：执行ORDER001的所有规则推理
curl -X POST http://localhost:8080/ontology/api/process/reasoning/execute-all/ORDER001 \
  -H "Content-Type: application/json" \
  -d '{
    "sourceCustomerId": "CUST001",
    "sourceCustomerStatus": "FRAUD",
    "targetCustomerId": "CUST002",
    "authStatus": "PASSED"
  }' | jq .

# 步骤4：查看详细推理结果
curl -X GET http://localhost:8080/ontology/api/process/rule/FraudCustomerCheckRule/expression | jq .
```

---

## 八、扩展新规则

### 方法1：编辑OWL文件（推荐）

在 `transfer_order_ontology.owl` 中添加新的BusinessLogic个体：

```xml
<owl:NamedIndividual rdf:about="&transfer;YourCustomRule">
    <rdf:type rdf:resource="&transfer;BusinessLogic"/>
    <transfer:logicCode>YourCustomRule</transfer:logicCode>
    <transfer:logicType>SWRL</transfer:logicType>
    <transfer:logicExpression>
        <!-- 您的SWRL表达式 -->
    </transfer:logicExpression>
    <rdfs:label>您的规则描述</rdfs:label>
</owl:NamedIndividual>
```

然后重新加载规则：

```bash
curl -X POST http://localhost:8080/ontology/api/process/reload-rules | jq .
```

### 方法2：编码方式

在 SWRLRuleEngine 中添加新方法并在 `registerBusinessRules()` 中注册。

---

## 九、调试和监控

### 查看规则加载日志

```bash
tail -f app.log | grep -i "SWRL\|reasoning\|推理"
```

### 验证规则格式

```bash
# 获取规则表达式
curl http://localhost:8080/ontology/api/process/rule/FraudCustomerCheckRule/expression | jq .expression

# 检查语法是否正确（应该包含 ->）
# 检查变量是否都以 ? 开头
# 检查括号是否匹配
```

### 测试上下文数据

```bash
# 验证上下文变量名是否正确
curl -X POST http://localhost:8080/ontology/api/process/reasoning/execute-rule/YourRule \
  -H "Content-Type: application/json" \
  -d '{
    "debug": true,
    "customerId": "TEST001"
  }' | jq .
```

---

## 十、常见问题

| 问题 | 解决方案 |
|------|--------|
| 规则无法解析 | 检查 `->` 分隔符是否存在，变量是否以 `?` 开头 |
| 找不到匹配实例 | 检查OWL本体中是否存在相关个体和属性 |
| 推理结果为空 | 验证上下文数据是否与规则变量匹配 |
| 性能较差 | 减少规则前提条件数量，优化OWL本体结构 |
| 推理异常 | 检查应用日志，查看具体错误堆栈跟踪 |

---

**最后更新**: 2025-12-17  
**版本**: 1.0.0
