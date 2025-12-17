package com.iwhalecloud.ontology.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 业务规则定义
 * 定义了所有可用的SWRL规则及其执行逻辑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleDefinition {
    
    /**
     * 规则ID
     */
    private String ruleId;
    
    /**
     * 规则名称
     */
    private String ruleName;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * SWRL规则表达式
     */
    private String swrlRule;
    
    /**
     * 规则类别
     */
    private RuleCategory category;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 违反规则时的错误消息
     */
    private String violationMessage;
    
    /**
     * 适用的步骤编号列表
     */
    private List<Integer> applicableSteps;
    
    /**
     * 规则优先级（1-10，10最高）
     */
    private Integer priority;
    
    /**
     * 检查的属性列表
     */
    private List<String> checkAttributes;
    
    /**
     * 规则类别枚举
     */
    public enum RuleCategory {
        CUSTOMER_STATUS("客户状态"),
        PAYMENT("支付"),
        AUTHENTICATION("鉴权"),
        STEP_PROGRESSION("步骤流转"),
        DATA_VALIDATION("数据验证"),
        BUSINESS_LOGIC("业务逻辑");
        
        private final String description;
        
        RuleCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 创建"涉诈客户检查"规则
     */
    public static BusinessRuleDefinition createFraudCustomerCheckRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("FRAUD_CHECK")
            .ruleName("FraudCustomerCheckRule")
            .description("检查客户是否为涉诈用户，涉诈用户不允许办理任何业务")
            .swrlRule("Customer(?c) ^ hasCustStatus(?c, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'FRAUD') " +
                     "-> BlockTransfer(?c)")
            .category(RuleCategory.CUSTOMER_STATUS)
            .enabled(true)
            .violationMessage("涉诈用户不允许办理任何业务，待涉诈解除后方可继续办理")
            .applicableSteps(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8))
            .priority(10)
            .checkAttributes(Arrays.asList("custStatus"))
            .build();
    }
    
    /**
     * 创建"欠费检查"规则
     */
    public static BusinessRuleDefinition createArrearsCheckRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("ARREARS_CHECK")
            .ruleName("ArrearsCheckRule")
            .description("检查客户是否存在欠费，欠费用户不允许办理过户业务")
            .swrlRule("Customer(?c) ^ hasArrearsStatus(?c, ?arrears) ^ swrlb:stringEqualIgnoreCase(?arrears, 'ARREARS') " +
                     "-> BlockTransfer(?c)")
            .category(RuleCategory.CUSTOMER_STATUS)
            .enabled(true)
            .violationMessage("用户存在欠费，不允许办理过户业务，请先缴清费用")
            .applicableSteps(Arrays.asList(2, 3, 4, 5, 6, 7, 8))
            .priority(9)
            .checkAttributes(Arrays.asList("arrearsStatus"))
            .build();
    }
    
    /**
     * 创建"黑名单检查"规则
     */
    public static BusinessRuleDefinition createBlacklistCheckRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("BLACKLIST_CHECK")
            .ruleName("BlacklistCheckRule")
            .description("检查客户是否在黑名单中")
            .swrlRule("Customer(?c) ^ hasBlacklistStatus(?c, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'IN_BLACKLIST') " +
                     "-> BlockTransfer(?c)")
            .category(RuleCategory.CUSTOMER_STATUS)
            .enabled(true)
            .violationMessage("该客户在黑名单中，不允许办理任何业务")
            .applicableSteps(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8))
            .priority(9)
            .checkAttributes(Arrays.asList("blacklistStatus"))
            .build();
    }
    
    /**
     * 创建"鉴权检查"规则
     */
    public static BusinessRuleDefinition createAuthenticationCheckRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("AUTH_CHECK")
            .ruleName("AuthenticationCheckRule")
            .description("检查鉴权是否通过，未通过鉴权不允许保存订单")
            .swrlRule("TransferOrder(?o) ^ hasAuthStatus(?o, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'PASSED') " +
                     "-> AllowProceedToStep8(?o)")
            .category(RuleCategory.AUTHENTICATION)
            .enabled(true)
            .violationMessage("用户鉴权未通过，不允许保存订单，请重新进行鉴权")
            .applicableSteps(Arrays.asList(7, 8))
            .priority(8)
            .checkAttributes(Arrays.asList("authStatus"))
            .build();
    }
    
    /**
     * 创建"支付检查"规则
     */
    public static BusinessRuleDefinition createPaymentCheckRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("PAYMENT_CHECK")
            .ruleName("PaymentCheckRule")
            .description("检查支付是否已结算，未结算不允许确认订单")
            .swrlRule("TransferOrder(?o) ^ hasPaymentStatus(?o, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'SETTLED') " +
                     "-> AllowConfirmOrder(?o)")
            .category(RuleCategory.PAYMENT)
            .enabled(true)
            .violationMessage("订单费用未结算，请先完成支付，然后再确认订单")
            .applicableSteps(Arrays.asList(8))
            .priority(8)
            .checkAttributes(Arrays.asList("paymentStatus"))
            .build();
    }
    
    /**
     * 创建"步骤流转"规则
     */
    public static BusinessRuleDefinition createStepProgressionRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("STEP_PROGRESSION")
            .ruleName("StepProgressionRule")
            .description("定义步骤流转逻辑，当前步骤完成后可以流转到下一步骤")
            .swrlRule("TransferOrder(?o) ^ hasCurrentStep(?o, ?currentStep) ^ swrlb:add(?nextStep, ?currentStep, 1) " +
                     "-> hasNextStep(?o, ?nextStep)")
            .category(RuleCategory.STEP_PROGRESSION)
            .enabled(true)
            .violationMessage("步骤流转失败")
            .applicableSteps(Arrays.asList(1, 2, 3, 4, 5, 6, 7))
            .priority(7)
            .checkAttributes(Arrays.asList("currentStepNumber"))
            .build();
    }
    
    /**
     * 创建"步骤回退"规则
     */
    public static BusinessRuleDefinition createStepRollbackRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("STEP_ROLLBACK")
            .ruleName("StepRollbackRule")
            .description("定义步骤回退规则，只有步骤3可以回退到步骤1")
            .swrlRule("TransferOrder(?o) ^ hasCurrentStep(?o, 3) ^ RequestRollback(?o) " +
                     "-> CanRollbackToStep(?o, 1)")
            .category(RuleCategory.STEP_PROGRESSION)
            .enabled(true)
            .violationMessage("只允许从步骤3回退到步骤1")
            .applicableSteps(Arrays.asList(3))
            .priority(6)
            .checkAttributes(Arrays.asList("currentStepNumber"))
            .build();
    }
    
    /**
     * 创建"客户信息完整性检查"规则
     */
    public static BusinessRuleDefinition createCustomerInfoCompletnessRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("CUST_INFO_CHECK")
            .ruleName("CustomerInfoCompletenessRule")
            .description("检查客户信息是否完整，缺少必要信息不允许进行下一步")
            .swrlRule("Customer(?c) ^ hasId(?c, ?id) ^ hasName(?c, ?name) ^ hasContactInfo(?c, ?contact) " +
                     "-> HasCompleteInfo(?c)")
            .category(RuleCategory.DATA_VALIDATION)
            .enabled(true)
            .violationMessage("客户信息不完整，请填写所有必要字段")
            .applicableSteps(Arrays.asList(1, 6))
            .priority(7)
            .checkAttributes(Arrays.asList("custId", "custName", "contactInfo"))
            .build();
    }
    
    /**
     * 创建"过户号码有效性检查"规则
     */
    public static BusinessRuleDefinition createTransferNumberValidityRule() {
        return BusinessRuleDefinition.builder()
            .ruleId("TRANSFER_NUM_CHECK")
            .ruleName("TransferNumberValidityRule")
            .description("检查过户号码是否有效和可用")
            .swrlRule("TransferNumber(?tn) ^ hasNumber(?tn, ?num) ^ hasStatus(?tn, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'AVAILABLE') " +
                     "-> IsValidTransferNumber(?tn)")
            .category(RuleCategory.DATA_VALIDATION)
            .enabled(true)
            .violationMessage("过户号码无效或不可用")
            .applicableSteps(Arrays.asList(2))
            .priority(8)
            .checkAttributes(Arrays.asList("transferNumber", "numberStatus"))
            .build();
    }
    
    /**
     * 获取所有默认业务规则
     */
    public static List<BusinessRuleDefinition> getDefaultBusinessRules() {
        return Arrays.asList(
            createFraudCustomerCheckRule(),
            createArrearsCheckRule(),
            createBlacklistCheckRule(),
            createAuthenticationCheckRule(),
            createPaymentCheckRule(),
            createStepProgressionRule(),
            createStepRollbackRule(),
            createCustomerInfoCompletnessRule(),
            createTransferNumberValidityRule()
        );
    }
}
