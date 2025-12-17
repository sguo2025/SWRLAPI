# SWRLAPI 规则校验集成说明

## 概述

本项目已完成了基于 SWRLAPI 的规则校验功能集成，实现了从简单的条件判断到完整的 SWRL 规则引擎的升级。

## 主要改进

### 1. **依赖配置** (`pom.xml`)

添加了 SWRLAPI 依赖：
```xml
<dependency>
    <groupId>edu.stanford.swrl</groupId>
    <artifactId>swrlapi</artifactId>
    <version>${swrlapi.version}</version>
</dependency>
```

### 2. **核心模块**

#### A. `SWRLRuleEngine` - SWRL 规则引擎服务
**文件**: `src/main/java/com/iwhalecloud/ontology/service/SWRLRuleEngine.java`

**功能**:
- 添加 SWRL 规则到本体
- 注册业务规则
- 执行规则推理
- 验证个体是否违反规则
- 获取已注册规则列表

**支持的规则**:

| 规则名称 | 规则ID | 描述 | 优先级 |
|---------|--------|------|--------|
| FraudCustomerCheckRule | FRAUD_CHECK | 涉诈客户检查 | 10 |
| ArrearsCheckRule | ARREARS_CHECK | 欠费检查 | 9 |
| BlacklistCheckRule | BLACKLIST_CHECK | 黑名单检查 | 9 |
| AuthenticationCheckRule | AUTH_CHECK | 鉴权检查 | 8 |
| PaymentCheckRule | PAYMENT_CHECK | 支付检查 | 8 |
| StepProgressionRule | STEP_PROGRESSION | 步骤流转 | 7 |
| StepRollbackRule | STEP_ROLLBACK | 步骤回退 | 6 |
| CustomerInfoCompletenessRule | CUST_INFO_CHECK | 客户信息完整性 | 7 |
| TransferNumberValidityRule | TRANSFER_NUM_CHECK | 过户号码有效性 | 8 |

#### B. `BusinessRuleDefinition` - 业务规则定义
**文件**: `src/main/java/com/iwhalecloud/ontology/model/BusinessRuleDefinition.java`

**功能**:
- 定义所有业务规则的元数据
- 规则包括：名称、描述、SWRL表达式、类别、优先级等
- 支持规则的启用/禁用
- 定义规则适用的步骤

**规则类别**:
- `CUSTOMER_STATUS` - 客户状态相关规则
- `PAYMENT` - 支付相关规则
- `AUTHENTICATION` - 鉴权相关规则
- `STEP_PROGRESSION` - 步骤流转规则
- `DATA_VALIDATION` - 数据验证规则
- `BUSINESS_LOGIC` - 业务逻辑规则

#### C. `ProcessReasoningService` - 流程推理服务升级
**文件**: `src/main/java/com/iwhalecloud/ontology/service/ProcessReasoningService.java`

**新增功能**:
- `checkBusinessRules()` - 基于 SWRL 规则执行规则校验
- `initializeSWRLRules()` - 初始化 SWRL 规则
- `getApplicableBusinessRules()` - 获取指定步骤适用的规则
- `executeReasoningAndUpdateProcess()` - 执行 SWRL 推理并更新流程状态
- `checkCustomerStatusRules()` - 检查客户状态规则
- `checkAuthenticationRules()` - 检查鉴权规则
- `checkPaymentRules()` - 检查支付规则
- `checkDataValidationRules()` - 检查数据验证规则

#### D. `OntologyService` - 本体服务增强
**文件**: `src/main/java/com/iwhalecloud/ontology/service/OntologyService.java`

**新增方法**:
- `getOntology()` - 获取本体实例
- `getDataFactory()` - 获取 OWL 数据工厂
- `getManager()` - 获取 OWL 本体管理器
- `getReasoner()` - 获取 OWL 推理器

## SWRL 规则示例

### 1. 涉诈客户检查规则
```swrl
Customer(?c) ^ hasCustStatus(?c, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'FRAUD') 
-> BlockTransfer(?c)
```

### 2. 欠费检查规则
```swrl
Customer(?c) ^ hasArrearsStatus(?c, ?arrears) ^ swrlb:stringEqualIgnoreCase(?arrears, 'ARREARS') 
-> BlockTransfer(?c)
```

### 3. 鉴权检查规则
```swrl
TransferOrder(?o) ^ hasAuthStatus(?o, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'PASSED') 
-> AllowProceedToStep8(?o)
```

### 4. 支付检查规则
```swrl
TransferOrder(?o) ^ hasPaymentStatus(?o, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'SETTLED') 
-> AllowConfirmOrder(?o)
```

### 5. 步骤流转规则
```swrl
TransferOrder(?o) ^ hasCurrentStep(?o, ?currentStep) ^ swrlb:add(?nextStep, ?currentStep, 1) 
-> hasNextStep(?o, ?nextStep)
```

## 规则校验流程

### 流程推进时执行的规则检查

```
1. 获取当前步骤号和订单信息
2. 查询适用于当前步骤的所有规则
3. 按优先级排序（高优先级优先检查）
4. 逐个检查规则：
   - 客户状态规则（优先级9-10）
   - 支付/鉴权规则（优先级8）
   - 数据验证规则（优先级7）
5. 若检查到高优先级（>=8）规则被触发，立即阻止流程推进
6. 返回检查结果和违反规则列表
```

## 使用示例

### 1. 初始化规则
```java
Map<String, Object> result = processReasoningService.initializeSWRLRules();
```

### 2. 检查流程是否可以推进
```java
TransferOrderProcess process = processReasoningService.reasonNextStep(orderId, currentStep);
// 检查 process.isCanProceed() 和 process.getBlockReason()
```

### 3. 获取步骤适用的规则
```java
Map<String, Object> rules = processReasoningService.getApplicableBusinessRules(stepNumber);
```

### 4. 执行 SWRL 推理
```java
Map<String, Object> result = processReasoningService.executeReasoningAndUpdateProcess(orderId);
```

## 规则校验矩阵

| 规则 | Step1 | Step2 | Step3 | Step4 | Step5 | Step6 | Step7 | Step8 |
|-----|-------|-------|-------|-------|-------|-------|-------|-------|
| FRAUD_CHECK | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| ARREARS_CHECK | | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| BLACKLIST_CHECK | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| AUTH_CHECK | | | | | | | ✓ | ✓ |
| PAYMENT_CHECK | | | | | | | | ✓ |
| STEP_PROGRESSION | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | |
| STEP_ROLLBACK | | | ✓ | | | | | |
| CUST_INFO_CHECK | ✓ | | | | | ✓ | | |
| TRANSFER_NUM_CHECK | | ✓ | | | | | | |

## 编译和构建

```bash
# 编译项目
export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.9-ms
mvn clean compile

# 打包项目
mvn clean package

# 运行应用
mvn spring-boot:run
```

## 关键特性

### 1. **优先级机制**
- 优先级10 - 最高优先级，一旦触发立即阻止
- 优先级8-9 - 高优先级，也会立即阻止
- 优先级7及以下 - 普通优先级，记录但继续检查其他规则

### 2. **灵活的规则启用/禁用**
每个规则都可以独立启用或禁用，无需修改代码

### 3. **步骤级别规则配置**
规则可以为不同步骤进行独立配置

### 4. **详细的规则检查报告**
包含：
- 已通过的规则列表
- 违反的规则列表
- 每个规则的原因和违反信息
- 规则应用总数

## 集成点

### 1. 流程推理时的规则检查
```java
private Map<String, Object> checkBusinessRules(String orderId, Integer stepNumber)
```

### 2. 控制器集成
在 `OntologyController` 或 `ProcessReasoningController` 中调用：
```java
@PostMapping("/reasoning/init-rules")
public Map<String, Object> initializeRules() {
    return processReasoningService.initializeSWRLRules();
}
```

## 扩展说明

### 添加新规则步骤

1. 在 `BusinessRuleDefinition` 中创建新规则定义方法
2. 在 `getDefaultBusinessRules()` 中添加新规则
3. 在 `ProcessReasoningService.checkBusinessRules()` 中添加相应的检查逻辑
4. 在 `SWRLRuleEngine` 中定义对应的 SWRL 表达式

### 修改规则优先级

在 `BusinessRuleDefinition` 中修改 `.priority(8)` 的值即可

## 总结

通过本次集成，项目已完成：

✅ SWRLAPI 依赖添加  
✅ SWRL 规则引擎实现  
✅ 业务规则定义和管理  
✅ 流程推理服务与规则引擎集成  
✅ 基于规则的访问控制和流程阻断  
✅ 规则检查报告和日志记录  
✅ 灵活的规则配置机制  

项目现已具备完整的规则校验能力，支持复杂的业务规则定义和执行。
