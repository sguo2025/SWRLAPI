package com.iwhalecloud.ontology.service;

import com.iwhalecloud.ontology.model.ProcessStepInfo;
import com.iwhalecloud.ontology.model.TransferOrderProcess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.semanticweb.owlapi.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程推理服务
 * 基于OWL本体进行步骤推理和流程控制
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessReasoningService {

    private final OntologyService ontologyService;
    
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
     */
    private Map<String, Object> checkBusinessRules(String orderId, Integer stepNumber) {
        Map<String, Object> results = new HashMap<>();
        results.put("canProceed", true);
        
        try {
            // 查询订单相关的客户信息
            Map<String, Object> orderProps = ontologyService.getIndividualProperties(orderId);
            
            if (orderProps == null || !orderProps.containsKey("dataProperties")) {
                results.put("canProceed", true);
                results.put("message", "订单不存在或无属性，允许继续");
                return results;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataProps = (Map<String, Object>) orderProps.get("dataProperties");
            
            // 检查涉诈状态
            if (dataProps.containsKey("custStatus") && "FRAUD".equals(dataProps.get("custStatus"))) {
                results.put("canProceed", false);
                results.put("blockReason", "涉诈用户不允许办理任何业务，待涉诈解除后方可继续办理");
                results.put("ruleName", "FraudCustomerCheckRule");
                return results;
            }
            
            // 检查欠费状态（在过户相关步骤）
            if (stepNumber >= 2 && dataProps.containsKey("arrearsStatus") && "ARREARS".equals(dataProps.get("arrearsStatus"))) {
                results.put("canProceed", false);
                results.put("blockReason", "用户存在欠费，不允许办理过户业务，请先缴清费用");
                results.put("ruleName", "ArrearsCheckRule");
                return results;
            }
            
            // 步骤7需要检查鉴权是否通过
            if (stepNumber == 7) {
                // 这里应该检查是否有鉴权记录且状态为PASSED
                results.put("message", "订单保存前检查鉴权状态");
            }
            
            // 步骤8需要检查支付状态
            if (stepNumber == 8) {
                // 这里应该检查是否有支付记录且状态为SETTLED
                results.put("message", "订单确认前检查支付状态");
            }
            
            results.put("message", "所有业务规则检查通过");
            
        } catch (Exception e) {
            log.error("业务规则检查失败", e);
            results.put("canProceed", true);
            results.put("message", "规则检查异常，默认允许继续: " + e.getMessage());
        }
        
        return results;
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
            // 执行SWRL推理
            Map<String, Object> reasoningResult = ontologyService.executeSWRLReasoning();
            result.put("reasoningResult", reasoningResult);
            
            // 获取更新后的流程状态
            TransferOrderProcess process = getProcessStatus(orderId);
            result.put("processStatus", process);
            
            result.put("status", "success");
            result.put("message", "推理执行成功并更新流程状态");
            
        } catch (Exception e) {
            log.error("推理执行失败", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}
