# 动态规则加载实现指南

## 一、registerBusinessRules 调用位置

`registerBusinessRules` 方法在 **ProcessReasoningService** 中的两个关键位置被调用：

### 1️⃣ 位置一：executeReasoningAndUpdateProcess 方法
**文件**: [ProcessReasoningService.java](src/main/java/com/iwhalecloud/ontology/service/ProcessReasoningService.java#L569)  
**行号**: 569行

```java
public Map<String, Object> executeReasoningAndUpdateProcess(String orderId) {
    // ... 代码 ...
    
    // 首先注册业务规则到本体
    log.info("注册业务规则到本体...");
    Map<String, Object> ruleRegistrationResult = swrlRuleEngine.registerBusinessRules(ontology);
    result.put("ruleRegistration", ruleRegistrationResult);
    
    // 执行SWRL推理
    log.info("执行SWRL推理...");
    Map<String, Object> reasoningResult = swrlRuleEngine.executeSWRLReasoning(ontology);
    // ...
}
```

**调用场景**: 当执行特定订单的SWRL推理时，在推理之前先注册规则

**REST端点**: `POST /api/process/execute-swrl-reasoning/{orderId}`

---

### 2️⃣ 位置二：initializeSWRLRules 方法
**文件**: [ProcessReasoningService.java](src/main/java/com/iwhalecloud/ontology/service/ProcessReasoningService.java#L606)  
**行号**: 606行

```java
public Map<String, Object> initializeSWRLRules() {
    log.info("初始化SWRL规则...");
    
    Map<String, Object> result = new HashMap<>();
    
    try {
        OWLOntology ontology = ontologyService.getOntology();
        // 使用新的动态加载功能
        Map<String, Object> ruleResult = swrlRuleEngine.registerBusinessRules(ontology);
        
        result.put("status", ruleResult.get("status"));
        result.put("message", ruleResult.get("message"));
        result.put("ruleDetails", ruleResult);
        
        // 如果有加载的规则，返回其信息
        Map<String, Object> loadedRules = swrlRuleEngine.getLoadedRules();
        if (!loadedRules.isEmpty()) {
            result.put("loadedRulesInfo", loadedRules);
        }
        
        log.info("SWRL规则初始化完成: {}", ruleResult.get("message"));
    } catch (Exception e) {
        // 错误处理
    }
    
    return result;
}
```

**调用场景**: 初始化或重新加载所有业务规则

**REST端点**: `POST /api/process/init-swrl-rules`

---

## 二、调用流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API 请求                              │
└─────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────┐
        │   ProcessReasoningController        │
        │  (REST 端点处理层)                   │
        └─────────────────────────────────────┘
                      ↓              ↓
        ┌──────────────────┐  ┌──────────────────┐
        │  @PostMapping    │  │  @PostMapping    │
        │  /init-swrl-     │  │  /execute-swrl-  │
        │  rules           │  │  reasoning/{id}  │
        └──────────────────┘  └──────────────────┘
                ↓                     ↓
        ┌──────────────────────────────────────────────┐
        │   ProcessReasoningService                    │
        │  (业务逻辑层)                                 │
        └──────────────────────────────────────────────┘
                ↓                     ↓
        ┌──────────────────┐  ┌──────────────────────┐
        │ initializeSWRL   │  │ executeReasoningAnd  │
        │ Rules()          │  │ UpdateProcess()      │
        └──────────────────┘  └──────────────────────┘
                ↓                     ↓
        ┌──────────────────────────────────────────────┐
        │   SWRLRuleEngine.registerBusinessRules()     │
        │  (规则加载与管理层)                           │
        └──────────────────────────────────────────────┘
                ↓
        ┌──────────────────────────────────────────────┐
        │   loadRulesFromOntology()                    │
        │  (动态规则加载)                              │
        │  1. 扫描OWL本体中的BusinessLogic个体        │
        │  2. 提取规则属性(logicCode, logicExpression)│
        │  3. 存储在loadedRules Map中                 │
        └──────────────────────────────────────────────┘
                ↓
        ┌──────────────────────────────────────────────┐
        │   OWL本体文件                                │
        │  (transfer_order_ontology.owl)              │
        │  包含BusinessLogic个体定义                   │
        └──────────────────────────────────────────────┘
```

---

## 三、动态规则加载工作流程

### 步骤 1: 启动时初始化
```
应用启动 
  → ProcessReasoningService 被 @PostConstruct 初始化
  → ontologyService.loadOntology() 加载OWL文件
  → 规则被自动发现和缓存
```

### 步骤 2: REST请求初始化规则
```
POST /api/process/init-swrl-rules
  → ProcessReasoningController.initializeSWRLRules()
  → ProcessReasoningService.initializeSWRLRules()
  → SWRLRuleEngine.registerBusinessRules()
    → loadRulesFromOntology() 扫描OWL本体
    → 返回加载的所有规则信息
```

### 步骤 3: 执行订单推理
```
POST /api/process/execute-swrl-reasoning/ORDER001
  → ProcessReasoningReasoningController.executeSWRLReasoning(orderId)
  → ProcessReasoningService.executeReasoningAndUpdateProcess(orderId)
  → SWRLRuleEngine.registerBusinessRules()
    → 确保规则已加载
    → 执行SWRL推理
    → 返回推理结果
```

---

## 四、规则加载策略

### 优先级策略 (registerBusinessRules)

1. **首先尝试从OWL本体动态加载**
   ```java
   Map<String, Object> result = loadRulesFromOntology(ontology);
   if (!loadedRules.isEmpty()) {
       // 成功加载，返回动态规则
       return result;
   }
   ```

2. **如果本体中未找到规则，使用硬编码默认规则**
   ```java
   // 使用6条硬编码的SWRL规则作为后备方案
   Map<String, String> businessRules = new LinkedHashMap<>();
   businessRules.put("FraudCustomerCheckRule", getFraudCustomerCheckRule());
   businessRules.put("ArrearsCheckRule", getArrearsCheckRule());
   // ...
   ```

### 缓存策略

已加载的规则存储在内存中：
```java
private Map<String, Map<String, Object>> loadedRules = new LinkedHashMap<>();
```

支持快速查询：
```java
// 获取所有已加载的规则
Map<String, Object> allRules = swrlRuleEngine.getLoadedRules();

// 根据规则代码查询单个规则
Map<String, Object> rule = swrlRuleEngine.getRuleByCode("FraudCustomerCheckRule");
```

---

## 五、OWL本体中的规则定义

在 [transfer_order_ontology.owl](src/main/resources/owl/transfer_order_ontology.owl) 中：

```xml
<!-- 业务规则定义 -->
<owl:NamedIndividual rdf:about="&transfer;FraudCustomerCheckRule">
    <rdf:type rdf:resource="&transfer;BusinessLogic"/>
    <transfer:logicCode>FraudCustomerCheckRule</transfer:logicCode>
    <transfer:logicType>SWRL</transfer:logicType>
    <transfer:logicExpression>Customer(?c) ^ hasCustStatus(?c, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'FRAUD') -> BlockTransfer(?c)</transfer:logicExpression>
    <rdfs:label xml:lang="zh">涉诈客户检查</rdfs:label>
    <rdfs:comment>检查客户是否为涉诈用户，涉诈用户不允许办理任何业务</rdfs:comment>
</owl:NamedIndividual>
```

---

## 六、扩展新规则的方法

### 方法 A: 直接编辑OWL文件（推荐）

在 transfer_order_ontology.owl 中添加新的BusinessLogic个体：

```xml
<owl:NamedIndividual rdf:about="&transfer;YourNewRule">
    <rdf:type rdf:resource="&transfer;BusinessLogic"/>
    <transfer:logicCode>YourNewRule</transfer:logicCode>
    <transfer:logicType>SWRL</transfer:logicType>
    <transfer:logicExpression>您的SWRL规则表达式</transfer:logicExpression>
    <rdfs:label>规则名称</rdfs:label>
    <rdfs:comment>规则说明</rdfs:comment>
</owl:NamedIndividual>
```

然后调用：
```bash
POST /api/process/init-swrl-rules
```

系统会自动加载新规则。

### 方法 B: 在代码中添加硬编码规则

编辑 SWRLRuleEngine.java，添加新方法：

```java
public String getYourNewRule() {
    return "您的SWRL规则表达式";
}
```

然后在 `registerBusinessRules()` 中的硬编码部分添加：

```java
businessRules.put("YourNewRule", getYourNewRule());
```

---

## 七、调试和监控

### 查看已加载的规则

```bash
# 查看规则初始化信息
curl -X POST http://localhost:8080/ontology/api/process/init-swrl-rules | jq .ruleDetails

# 查看加载的规则列表
curl -X POST http://localhost:8080/ontology/api/process/init-swrl-rules | jq .loadedRulesInfo
```

### 查看应用日志

```bash
# 查看规则加载日志
tail -f app.log | grep -i "规则加载\|registerBusinessRules"

# 查看规则初始化日志
tail -f app.log | grep -i "初始化SWRL规则"
```

### 规则加载成功标志

成功的规则加载会产生日志：
```
[INFO] 从OWL本体开始动态加载业务规则...
[INFO] 本体命名空间: https://iwhalecloud.com/ontology/transfer#
[INFO] 找到 X 个业务规则定义
[INFO] 规则加载成功: RuleName
[INFO] 本体规则加载完成: 总数=X, 成功=X, 失败=0
```

---

## 八、关键方法速查表

| 方法 | 功能 | 位置 |
|------|------|------|
| `registerBusinessRules()` | 注册或加载所有业务规则 | SWRLRuleEngine |
| `loadRulesFromOntology()` | 从OWL本体动态加载规则 | SWRLRuleEngine |
| `extractRuleFromIndividual()` | 从OWL个体提取规则信息 | SWRLRuleEngine |
| `getLoadedRules()` | 获取所有已加载的规则 | SWRLRuleEngine |
| `getRuleByCode()` | 根据规则代码查询单个规则 | SWRLRuleEngine |
| `initializeSWRLRules()` | 初始化规则（REST调用） | ProcessReasoningService |
| `executeReasoningAndUpdateProcess()` | 执行推理（REST调用） | ProcessReasoningService |

---

**最后更新**: 2025-12-17  
**实现版本**: 1.0.0
