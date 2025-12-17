package com.iwhalecloud.ontology.service;

import com.iwhalecloud.ontology.model.ProcessStepInfo;
import com.iwhalecloud.ontology.model.TransferOrderProcess;
import com.iwhalecloud.ontology.model.BusinessRuleDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.semanticweb.owlapi.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程推理服务
 * 基于OWL本体和SWRL规则进行步骤推理和流程控制
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessReasoningService {

    private final OntologyService ontologyService;
    private final SWRLRuleEngine swrlRuleEngine;
    
    // 8个步骤的定义
    private static final Map<Integer, String> STEP_CODES = Map.of(
        1, "Step1_LocateSourceCustomer",
        2, "Step2_SelectTransferNumber",
        3, "Step3_CreateCustomerOrder",
        4, "Step4_InitTransferBusiness",
        5, "Step5_InitCommonAttributes",
        6, "Step6_ConfirmTargetCustomer",
        7, "Step7_SaveOrder",
        8, "Step8_ConfirmOrder"
    );
    
    private static final Map<Integer, String> STEP_NAMES = Map.of(
        1, "定位源客户",
        2, "过户号码选择",
        3, "创建客户订单",
        4, "过户业务初始化",
        5, "公共属性初始化",
        6, "目标客户确认",
        7, "订单保存",
        8, "订单确认"
    );

    /**
     * 获取所有流程步骤信息
     */
    public List<ProcessStepInfo> getAllProcessSteps() {
        log.info("获取所有流程步骤信息");
        
        List<ProcessStepInfo> steps = new ArrayList<>();
        
        for (int i = 1; i <= 8; i++) {
            ProcessStepInfo step = ProcessStepInfo.builder()
                .stepNumber(i)
                .stepCode(STEP_CODES.get(i))
                .stepName(STEP_NAMES.get(i))
                .status(ProcessStepInfo.StepStatus.PENDING)
                .canRollback(i == 3) // 步骤3可以回退到步骤1
                .rollbackToStep(i == 3 ? 1 : null)
                .build();
            
            // 从本体中查询步骤的详细信息
            enrichStepFromOntology(step);
            
            steps.add(step);
        }
        
        return steps;
    }

    /**
     * 从本体中丰富步骤信息
     */
    private void enrichStepFromOntology(ProcessStepInfo step) {
        try {
            String className = STEP_CODES.get(step.getStepNumber());
            
            // 查询步骤类的个体（实际场景中可能需要创建步骤实例）
            // 这里我们从类定义中获取信息
            Map<String, Object> stepInfo = getStepClassInfo(className);
            
            if (stepInfo != null) {
                step.setDescription((String) stepInfo.get("comment"));
                step.setRequiresEntities((List<String>) stepInfo.getOrDefault("requiresEntities", new ArrayList<>()));
                step.setProducesEntities((List<String>) stepInfo.getOrDefault("producesEntities", new ArrayList<>()));
                step.setOdaComponent((String) stepInfo.get("odaComponent"));
                step.setApiEndpoint((String) stepInfo.get("apiEndpoint"));
                step.setBusinessRules((List<String>) stepInfo.getOrDefault("businessRules", new ArrayList<>()));
            }
            
        } catch (Exception e) {
            log.warn("从本体获取步骤信息失败: {}", step.getStepCode(), e);
        }
    }

    /**
     * 获取步骤类的信息
     */
    private Map<String, Object> getStepClassInfo(String className) {
        // 这是一个简化的实现，实际应该从OWL本体中解析
        // 可以通过OntologyService来实现更复杂的查询
        return Map.of(
            "comment", STEP_NAMES.get(getStepNumberFromCode(className))
        );
    }
    
    private int getStepNumberFromCode(String stepCode) {
        return STEP_CODES.entrySet().stream()
            .filter(e -> e.getValue().equals(stepCode))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(0);
    }

    /**
     * 推理下一步骤
     */
    public TransferOrderProcess reasonNextStep(String orderId, Integer currentStepNumber) {
        log.info("推理订单 {} 的下一步骤，当前步骤: {}", orderId, currentStepNumber);
        
        TransferOrderProcess process = TransferOrderProcess.builder()
            .orderId(orderId)
            .currentStepNumber(currentStepNumber)
            .totalSteps(8)
            .steps(getAllProcessSteps())
            .build();
        
        // 检查当前步骤
        if (currentStepNumber < 1 || currentStepNumber > 8) {
            process.setCanProceed(false);
            process.setBlockReason("当前步骤编号无效");
            return process;
        }
        
        // 设置当前步骤
        ProcessStepInfo currentStep = process.getSteps().get(currentStepNumber - 1);
        currentStep.setStatus(ProcessStepInfo.StepStatus.IN_PROGRESS);
        process.setCurrentStep(currentStep);
        
        // 执行业务规则检查
        Map<String, Object> ruleResults = checkBusinessRules(orderId, currentStepNumber);
        process.setRuleCheckResults(ruleResults);
        
        // 判断是否可以推进
        boolean canProceed = (boolean) ruleResults.getOrDefault("canProceed", true);
        process.setCanProceed(canProceed);
        
        if (!canProceed) {
            process.setBlockReason((String) ruleResults.get("blockReason"));
            currentStep.setStatus(ProcessStepInfo.StepStatus.FAILED);
            currentStep.setErrorMessage(process.getBlockReason());
            return process;
        }
        
        // 推理下一步
        Integer nextStepNumber = currentStepNumber < 8 ? currentStepNumber + 1 : null;
        process.setNextStepNumber(nextStepNumber);
        
        if (nextStepNumber != null) {
            ProcessStepInfo nextStep = process.getSteps().get(nextStepNumber - 1);
            process.setNextStep(nextStep);
            process.setRecommendation("当前步骤检查通过，可以推进到步骤" + nextStepNumber + ": " + nextStep.getStepName());
            
            // 标记当前步骤完成
            currentStep.setStatus(ProcessStepInfo.StepStatus.COMPLETED);
            currentStep.setEndTime(LocalDateTime.now());
        } else {
            process.setRecommendation("已到达最后一步，流程即将完成");
            currentStep.setStatus(ProcessStepInfo.StepStatus.COMPLETED);
            process.setOrderStatus("COMPLETED");
        }
        
        return process;
    }

    /**
     * 检查业务规则
     * 基于SWRL规则引擎执行规则校验
     */
    private Map<String, Object> checkBusinessRules(String orderId, Integer stepNumber) {
        Map<String, Object> results = new HashMap<>();
        results.put("canProceed", true);
        
        try {
            log.info("执行SWRL规则检查: orderId={}, stepNumber={}", orderId, stepNumber);
            
            // 获取本体
            OWLOntology ontology = ontologyService.getOntology();
            
            // 查询订单相关的客户信息
            Map<String, Object> orderProps = ontologyService.getIndividualProperties(orderId);
            
            if (orderProps == null || !orderProps.containsKey("dataProperties")) {
                results.put("canProceed", true);
                results.put("message", "订单不存在或无属性，允许继续");
                results.put("appliedRules", new ArrayList<>());
                return results;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataProps = (Map<String, Object>) orderProps.get("dataProperties");
            
            // 收集应用的规则
            List<String> appliedRules = new ArrayList<>();
            List<Map<String, Object>> violatedRules = new ArrayList<>();
            
            // 获取适用于当前步骤的所有业务规则
            List<BusinessRuleDefinition> applicableRules = BusinessRuleDefinition.getDefaultBusinessRules()
                .stream()
                .filter(rule -> rule.getApplicableSteps().contains(stepNumber) && rule.getEnabled())
                .sorted(Comparator.comparingInt(BusinessRuleDefinition::getPriority).reversed())
                .collect(Collectors.toList());
            
            log.info("当前步骤 {} 适用的规则数: {}", stepNumber, applicableRules.size());
            
            // 对每个规则进行检查
            for (BusinessRuleDefinition ruleDef : applicableRules) {
                log.debug("检查规则: {}", ruleDef.getRuleName());
                
                boolean ruleViolated = false;
                String violationReason = null;
                
                // 根据规则类别执行相应的检查
                switch (ruleDef.getCategory()) {
                    case CUSTOMER_STATUS:
                        // 检查客户状态相关规则
                        Map<String, Object> customerCheckResult = checkCustomerStatusRules(dataProps, ruleDef);
                        ruleViolated = (boolean) customerCheckResult.getOrDefault("violated", false);
                        violationReason = (String) customerCheckResult.get("reason");
                        break;
                        
                    case AUTHENTICATION:
                        // 检查鉴权规则
                        Map<String, Object> authCheckResult = checkAuthenticationRules(dataProps, ruleDef);
                        ruleViolated = (boolean) authCheckResult.getOrDefault("violated", false);
                        violationReason = (String) authCheckResult.get("reason");
                        break;
                        
                    case PAYMENT:
                        // 检查支付规则
                        Map<String, Object> paymentCheckResult = checkPaymentRules(dataProps, ruleDef);
                        ruleViolated = (boolean) paymentCheckResult.getOrDefault("violated", false);
                        violationReason = (String) paymentCheckResult.get("reason");
                        break;
                        
                    case DATA_VALIDATION:
                        // 检查数据验证规则
                        Map<String, Object> dataCheckResult = checkDataValidationRules(dataProps, ruleDef);
                        ruleViolated = (boolean) dataCheckResult.getOrDefault("violated", false);
                        violationReason = (String) dataCheckResult.get("reason");
                        break;
                        
                    case STEP_PROGRESSION:
                        // 步骤流转规则暂不阻断，仅记录
                        appliedRules.add(ruleDef.getRuleName());
                        continue;
                        
                    default:
                        appliedRules.add(ruleDef.getRuleName());
                        continue;
                }
                
                appliedRules.add(ruleDef.getRuleName());
                
                if (ruleViolated) {
                    log.warn("规则 {} 被触发，原因: {}", ruleDef.getRuleName(), violationReason);
                    Map<String, Object> violation = new HashMap<>();
                    violation.put("ruleName", ruleDef.getRuleName());
                    violation.put("ruleId", ruleDef.getRuleId());
                    violation.put("message", ruleDef.getViolationMessage());
                    violation.put("reason", violationReason);
                    violation.put("priority", ruleDef.getPriority());
                    violatedRules.add(violation);
                    
                    // 如果是高优先级规则（优先级>=8），立即停止流程
                    if (ruleDef.getPriority() >= 8) {
                        results.put("canProceed", false);
                        results.put("blockReason", ruleDef.getViolationMessage());
                        results.put("violatedRules", violatedRules);
                        results.put("appliedRules", appliedRules);
                        results.put("blockingRule", ruleDef.getRuleName());
                        log.info("高优先级规则被触发，阻止流程继续: {}", ruleDef.getRuleName());
                        return results;
                    }
                }
            }
            
            // 检查是否有任何违反的规则
            if (!violatedRules.isEmpty()) {
                results.put("canProceed", false);
                results.put("blockReason", "业务规则检查失败，有" + violatedRules.size() + "条规则被触发");
                results.put("violatedRules", violatedRules);
            } else {
                results.put("message", "所有业务规则检查通过");
                results.put("violatedRules", new ArrayList<>());
            }
            
            results.put("appliedRules", appliedRules);
            results.put("ruleCheckCount", appliedRules.size());
            
        } catch (Exception e) {
            log.error("业务规则检查失败", e);
            results.put("canProceed", true);
            results.put("message", "规则检查异常，默认允许继续: " + e.getMessage());
            results.put("error", e.getClass().getSimpleName());
        }
        
        return results;
    }
    
    /**
     * 检查客户状态相关规则
     */
    private Map<String, Object> checkCustomerStatusRules(Map<String, Object> dataProps, BusinessRuleDefinition ruleDef) {
        Map<String, Object> result = new HashMap<>();
        result.put("violated", false);
        
        try {
            String ruleId = ruleDef.getRuleId();
            
            if ("FRAUD_CHECK".equals(ruleId)) {
                String custStatus = (String) dataProps.get("custStatus");
                if ("FRAUD".equals(custStatus)) {
                    result.put("violated", true);
                    result.put("reason", "客户状态为涉诈");
                    log.warn("规则FRAUD_CHECK被触发");
                }
            } else if ("ARREARS_CHECK".equals(ruleId)) {
                String arrearsStatus = (String) dataProps.get("arrearsStatus");
                if ("ARREARS".equals(arrearsStatus)) {
                    result.put("violated", true);
                    result.put("reason", "客户存在欠费");
                    log.warn("规则ARREARS_CHECK被触发");
                }
            } else if ("BLACKLIST_CHECK".equals(ruleId)) {
                String blacklistStatus = (String) dataProps.get("blacklistStatus");
                if ("IN_BLACKLIST".equals(blacklistStatus)) {
                    result.put("violated", true);
                    result.put("reason", "客户在黑名单中");
                    log.warn("规则BLACKLIST_CHECK被触发");
                }
            }
        } catch (Exception e) {
            log.error("客户状态规则检查异常", e);
        }
        
        return result;
    }
    
    /**
     * 检查鉴权相关规则
     */
    private Map<String, Object> checkAuthenticationRules(Map<String, Object> dataProps, BusinessRuleDefinition ruleDef) {
        Map<String, Object> result = new HashMap<>();
        result.put("violated", false);
        
        try {
            String authStatus = (String) dataProps.get("authStatus");
            
            // 如果未进行过鉴权或鉴权失败，则规则被违反
            if (authStatus == null || !authStatus.equals("PASSED")) {
                result.put("violated", true);
                result.put("reason", "鉴权状态为: " + (authStatus == null ? "未进行" : authStatus));
                log.warn("规则AuthenticationCheckRule被触发");
            }
        } catch (Exception e) {
            log.error("鉴权规则检查异常", e);
        }
        
        return result;
    }
    
    /**
     * 检查支付相关规则
     */
    private Map<String, Object> checkPaymentRules(Map<String, Object> dataProps, BusinessRuleDefinition ruleDef) {
        Map<String, Object> result = new HashMap<>();
        result.put("violated", false);
        
        try {
            String paymentStatus = (String) dataProps.get("paymentStatus");
            
            // 如果未完成支付，则规则被违反
            if (paymentStatus == null || !paymentStatus.equals("SETTLED")) {
                result.put("violated", true);
                result.put("reason", "支付状态为: " + (paymentStatus == null ? "未进行" : paymentStatus));
                log.warn("规则PaymentCheckRule被触发");
            }
        } catch (Exception e) {
            log.error("支付规则检查异常", e);
        }
        
        return result;
    }
    
    /**
     * 检查数据验证相关规则
     */
    private Map<String, Object> checkDataValidationRules(Map<String, Object> dataProps, BusinessRuleDefinition ruleDef) {
        Map<String, Object> result = new HashMap<>();
        result.put("violated", false);
        
        try {
            String ruleId = ruleDef.getRuleId();
            
            if ("CUST_INFO_CHECK".equals(ruleId)) {
                // 检查必要的客户信息是否存在
                List<String> requiredFields = Arrays.asList("custId", "custName", "contactInfo");
                for (String field : requiredFields) {
                    if (dataProps.get(field) == null || dataProps.get(field).toString().isEmpty()) {
                        result.put("violated", true);
                        result.put("reason", "缺少必要字段: " + field);
                        log.warn("规则CustomerInfoCompletenessRule被触发: 缺少{}", field);
                        return result;
                    }
                }
            } else if ("TRANSFER_NUM_CHECK".equals(ruleId)) {
                // 检查过户号码是否有效
                String numberStatus = (String) dataProps.get("numberStatus");
                if (numberStatus == null || !numberStatus.equals("AVAILABLE")) {
                    result.put("violated", true);
                    result.put("reason", "过户号码不可用");
                    log.warn("规则TransferNumberValidityRule被触发");
                }
            }
        } catch (Exception e) {
            log.error("数据验证规则检查异常", e);
        }
        
        return result;
    }

    /**
     * 执行步骤回退
     */
    public TransferOrderProcess rollbackStep(String orderId, Integer fromStep, Integer toStep) {
        log.info("执行步骤回退: 订单={}, 从步骤{}回退到步骤{}", orderId, fromStep, toStep);
        
        TransferOrderProcess process = TransferOrderProcess.builder()
            .orderId(orderId)
            .currentStepNumber(toStep)
            .totalSteps(8)
            .steps(getAllProcessSteps())
            .build();
        
        // 验证回退规则：只有步骤3可以回退到步骤1
        if (fromStep != 3 || toStep != 1) {
            process.setCanProceed(false);
            process.setBlockReason("只允许从步骤3回退到步骤1");
            return process;
        }
        
        // 标记回退的步骤
        for (int i = toStep; i <= fromStep; i++) {
            ProcessStepInfo step = process.getSteps().get(i - 1);
            step.setStatus(ProcessStepInfo.StepStatus.ROLLED_BACK);
        }
        
        // 设置当前步骤为回退目标
        ProcessStepInfo currentStep = process.getSteps().get(toStep - 1);
        currentStep.setStatus(ProcessStepInfo.StepStatus.PENDING);
        process.setCurrentStep(currentStep);
        
        process.setCanProceed(true);
        process.setRecommendation("已回退到步骤" + toStep + ": " + currentStep.getStepName() + "，请重新执行");
        
        return process;
    }

    /**
     * 获取完整的流程状态
     */
    public TransferOrderProcess getProcessStatus(String orderId) {
        log.info("获取订单 {} 的流程状态", orderId);
        
        try {
            // 从本体中查询订单
            Map<String, Object> orderProps = ontologyService.getIndividualProperties(orderId);
            
            if (orderProps == null) {
                // 订单不存在，返回初始状态
                return TransferOrderProcess.builder()
                    .orderId(orderId)
                    .currentStepNumber(1)
                    .totalSteps(8)
                    .orderStatus("NOT_STARTED")
                    .steps(getAllProcessSteps())
                    .recommendation("订单尚未创建，请从步骤1开始")
                    .build();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataProps = (Map<String, Object>) orderProps.getOrDefault("dataProperties", new HashMap<>());
            
            Integer currentStep = parseInteger(dataProps.get("currentStepNumber"), 1);
            String orderStatus = (String) dataProps.getOrDefault("orderStatus", "IN_PROGRESS");
            
            TransferOrderProcess process = TransferOrderProcess.builder()
                .orderId(orderId)
                .currentStepNumber(currentStep)
                .totalSteps(8)
                .orderStatus(orderStatus)
                .sourceCustomerId((String) dataProps.get("sourceCustomerId"))
                .targetCustomerId((String) dataProps.get("targetCustomerId"))
                .steps(getAllProcessSteps())
                .build();
            
            // 更新步骤状态
            updateStepStatuses(process, currentStep);
            
            // 设置当前步骤
            if (currentStep >= 1 && currentStep <= 8) {
                process.setCurrentStep(process.getSteps().get(currentStep - 1));
            }
            
            return process;
            
        } catch (Exception e) {
            log.error("获取流程状态失败", e);
            throw new RuntimeException("获取流程状态失败: " + e.getMessage());
        }
    }
    
    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private void updateStepStatuses(TransferOrderProcess process, Integer currentStep) {
        List<ProcessStepInfo> steps = process.getSteps();
        
        for (int i = 0; i < steps.size(); i++) {
            ProcessStepInfo step = steps.get(i);
            if (i + 1 < currentStep) {
                step.setStatus(ProcessStepInfo.StepStatus.COMPLETED);
            } else if (i + 1 == currentStep) {
                step.setStatus(ProcessStepInfo.StepStatus.IN_PROGRESS);
            } else {
                step.setStatus(ProcessStepInfo.StepStatus.PENDING);
            }
        }
    }

    /**
     * 执行SWRL推理并更新流程状态
     */
    public Map<String, Object> executeReasoningAndUpdateProcess(String orderId) {
        log.info("执行SWRL推理并更新订单 {} 的流程状态", orderId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取本体
            OWLOntology ontology = ontologyService.getOntology();
            
            // 首先注册业务规则到本体
            log.info("注册业务规则到本体...");
            Map<String, Object> ruleRegistrationResult = swrlRuleEngine.registerBusinessRules(ontology);
            result.put("ruleRegistration", ruleRegistrationResult);
            
            // 执行SWRL推理
            log.info("执行SWRL推理...");
            Map<String, Object> reasoningResult = swrlRuleEngine.executeSWRLReasoning(ontology);
            result.put("reasoningResult", reasoningResult);
            
            // 获取更新后的流程状态
            TransferOrderProcess process = getProcessStatus(orderId);
            result.put("processStatus", process);
            
            result.put("status", "success");
            result.put("message", "推理执行成功并更新流程状态");
            
            log.info("SWRL推理和流程更新完成");
            
        } catch (Exception e) {
            log.error("推理执行失败", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 初始化本体中的SWRL规则
     */
    public Map<String, Object> initializeSWRLRules() {
        log.info("初始化SWRL规则...");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            OWLOntology ontology = ontologyService.getOntology();
            Map<String, Object> ruleResult = swrlRuleEngine.registerBusinessRules(ontology);
            
            result.put("status", ruleResult.get("status"));
            result.put("message", ruleResult.get("message"));
            result.put("ruleDetails", ruleResult);
            
            log.info("SWRL规则初始化完成: {}", ruleResult.get("message"));
            
        } catch (Exception e) {
            log.error("SWRL规则初始化失败", e);
            result.put("status", "error");
            result.put("message", "规则初始化失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取所有适用的业务规则
     */
    public Map<String, Object> getApplicableBusinessRules(Integer stepNumber) {
        log.info("获取步骤 {} 的适用业务规则", stepNumber);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<BusinessRuleDefinition> applicableRules = BusinessRuleDefinition.getDefaultBusinessRules()
                .stream()
                .filter(rule -> rule.getApplicableSteps().contains(stepNumber) && rule.getEnabled())
                .sorted(Comparator.comparingInt(BusinessRuleDefinition::getPriority).reversed())
                .collect(Collectors.toList());
            
            List<Map<String, Object>> ruleInfoList = new ArrayList<>();
            for (BusinessRuleDefinition rule : applicableRules) {
                Map<String, Object> ruleInfo = new HashMap<>();
                ruleInfo.put("ruleId", rule.getRuleId());
                ruleInfo.put("ruleName", rule.getRuleName());
                ruleInfo.put("description", rule.getDescription());
                ruleInfo.put("category", rule.getCategory().name());
                ruleInfo.put("priority", rule.getPriority());
                ruleInfo.put("violationMessage", rule.getViolationMessage());
                ruleInfo.put("checkAttributes", rule.getCheckAttributes());
                ruleInfoList.add(ruleInfo);
            }
            
            result.put("status", "success");
            result.put("stepNumber", stepNumber);
            result.put("ruleCount", ruleInfoList.size());
            result.put("rules", ruleInfoList);
            
        } catch (Exception e) {
            log.error("获取适用规则失败", e);
            result.put("status", "error");
            result.put("message", "获取规则失败: " + e.getMessage());
        }
        
        return result;
    }
}
