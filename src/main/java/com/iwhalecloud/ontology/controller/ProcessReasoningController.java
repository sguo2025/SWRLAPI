package com.iwhalecloud.ontology.controller;

import com.iwhalecloud.ontology.model.ProcessStepInfo;
import com.iwhalecloud.ontology.model.TransferOrderProcess;
import com.iwhalecloud.ontology.service.ProcessReasoningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程推理REST控制器
 * 提供8步过户流程的推理和控制接口
 */
@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
@Slf4j
public class ProcessReasoningController {

    private final ProcessReasoningService processReasoningService;

    /**
     * 获取所有流程步骤定义
     */
    @GetMapping("/steps")
    public ResponseEntity<Map<String, Object>> getAllSteps() {
        log.info("获取所有流程步骤定义");
        
        List<ProcessStepInfo> steps = processReasoningService.getAllProcessSteps();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalSteps", 8);
        response.put("steps", steps);
        response.put("description", "BSS4.0客户过户受理8步流程");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取订单的流程状态
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<TransferOrderProcess> getProcessStatus(@PathVariable String orderId) {
        log.info("获取订单流程状态: {}", orderId);
        
        TransferOrderProcess process = processReasoningService.getProcessStatus(orderId);
        return ResponseEntity.ok(process);
    }

    /**
     * 推理下一步骤
     * 
     * 基于当前步骤和业务规则，推理出下一步应该执行的步骤
     */
    @PostMapping("/reason-next-step")
    public ResponseEntity<TransferOrderProcess> reasonNextStep(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Integer currentStepNumber = (Integer) request.get("currentStepNumber");
        
        log.info("推理下一步骤: 订单={}, 当前步骤={}", orderId, currentStepNumber);
        
        TransferOrderProcess process = processReasoningService.reasonNextStep(orderId, currentStepNumber);
        return ResponseEntity.ok(process);
    }

    /**
     * 执行步骤推进
     * 
     * 将订单推进到下一步，包含业务规则验证
     */
    @PostMapping("/proceed")
    public ResponseEntity<Map<String, Object>> proceedToNextStep(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Integer currentStepNumber = (Integer) request.get("currentStepNumber");
        
        log.info("执行步骤推进: 订单={}, 当前步骤={}", orderId, currentStepNumber);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 推理下一步
            TransferOrderProcess process = processReasoningService.reasonNextStep(orderId, currentStepNumber);
            
            if (!process.getCanProceed()) {
                response.put("status", "blocked");
                response.put("message", "无法推进到下一步");
                response.put("reason", process.getBlockReason());
                response.put("process", process);
                return ResponseEntity.ok(response);
            }
            
            // 更新订单的当前步骤（实际应该在这里更新本体）
            // ontologyService.addDataProperty(orderId, "currentStepNumber", 
            //     process.getNextStepNumber().toString(), "integer");
            
            response.put("status", "success");
            response.put("message", "成功推进到步骤" + process.getNextStepNumber());
            response.put("process", process);
            
        } catch (Exception e) {
            log.error("步骤推进失败", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 执行步骤回退
     * 
     * 支持从步骤3回退到步骤1
     */
    @PostMapping("/rollback")
    public ResponseEntity<Map<String, Object>> rollbackStep(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Integer fromStep = (Integer) request.get("fromStep");
        Integer toStep = (Integer) request.get("toStep");
        
        log.info("执行步骤回退: 订单={}, 从步骤{}回退到步骤{}", orderId, fromStep, toStep);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TransferOrderProcess process = processReasoningService.rollbackStep(orderId, fromStep, toStep);
            
            if (!process.getCanProceed()) {
                response.put("status", "error");
                response.put("message", process.getBlockReason());
                response.put("process", process);
                return ResponseEntity.ok(response);
            }
            
            response.put("status", "success");
            response.put("message", "成功回退到步骤" + toStep);
            response.put("process", process);
            
        } catch (Exception e) {
            log.error("步骤回退失败", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 执行完整的流程推理
     * 
     * 基于SWRL规则和当前状态，执行完整的推理分析
     */
    @PostMapping("/full-reasoning/{orderId}")
    public ResponseEntity<Map<String, Object>> executeFullReasoning(@PathVariable String orderId) {
        log.info("执行完整流程推理: 订单={}", orderId);
        
        Map<String, Object> result = processReasoningService.executeReasoningAndUpdateProcess(orderId);
        return ResponseEntity.ok(result);
    }

    /**
     * 验证步骤前置条件
     * 
     * 检查指定步骤的前置条件是否满足
     */
    @PostMapping("/validate-prerequisites")
    public ResponseEntity<Map<String, Object>> validatePrerequisites(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Integer stepNumber = (Integer) request.get("stepNumber");
        
        log.info("验证步骤前置条件: 订单={}, 步骤={}", orderId, stepNumber);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前流程状态
            TransferOrderProcess process = processReasoningService.getProcessStatus(orderId);
            
            // 检查是否按顺序执行
            if (stepNumber <= process.getCurrentStepNumber()) {
                response.put("valid", true);
                response.put("message", "该步骤已完成或正在执行");
            } else if (stepNumber == process.getCurrentStepNumber() + 1) {
                // 检查当前步骤是否完成
                TransferOrderProcess nextProcess = processReasoningService.reasonNextStep(
                    orderId, process.getCurrentStepNumber());
                
                response.put("valid", nextProcess.getCanProceed());
                response.put("message", nextProcess.getCanProceed() ? 
                    "前置条件满足，可以执行" : nextProcess.getBlockReason());
                response.put("ruleCheckResults", nextProcess.getRuleCheckResults());
            } else {
                response.put("valid", false);
                response.put("message", "必须按顺序执行步骤，当前应执行步骤" + 
                    (process.getCurrentStepNumber() + 1));
            }
            
            response.put("currentStepNumber", process.getCurrentStepNumber());
            response.put("requestedStepNumber", stepNumber);
            
        } catch (Exception e) {
            log.error("验证前置条件失败", e);
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 执行SWRL推理
     * 
     * 基于SWRLAPI执行规则推理，验证业务规则
     */
    @PostMapping("/execute-swrl-reasoning/{orderId}")
    public ResponseEntity<Map<String, Object>> executeSWRLReasoning(@PathVariable String orderId) {
        log.info("执行SWRL推理: 订单={}", orderId);
        
        Map<String, Object> result = processReasoningService.executeReasoningAndUpdateProcess(orderId);
        return ResponseEntity.ok(result);
    }

    /**
     * 初始化SWRL规则
     * 
     * 初始化所有业务规则到本体
     */
    @PostMapping("/init-swrl-rules")
    public ResponseEntity<Map<String, Object>> initializeSWRLRules() {
        log.info("初始化SWRL规则");
        
        Map<String, Object> result = processReasoningService.initializeSWRLRules();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取适用的业务规则
     * 
     * 获取指定步骤的适用业务规则列表
     */
    @GetMapping("/applicable-rules/{stepNumber}")
    public ResponseEntity<Map<String, Object>> getApplicableRules(@PathVariable Integer stepNumber) {
        log.info("获取步骤{}的适用规则", stepNumber);
        
        Map<String, Object> result = processReasoningService.getApplicableBusinessRules(stepNumber);
        return ResponseEntity.ok(result);
    }

    /**
     * 模拟完整流程执行
     * 
     * 从步骤1到步骤8模拟执行并返回每步的推理结果
     */
    @PostMapping("/simulate-full-process")
    public ResponseEntity<Map<String, Object>> simulateFullProcess(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        
        log.info("模拟完整流程执行: 订单={}", orderId);
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> stepResults = new java.util.ArrayList<>();
        
        try {
            for (int i = 1; i <= 8; i++) {
                TransferOrderProcess process = processReasoningService.reasonNextStep(orderId, i);
                
                Map<String, Object> stepResult = new HashMap<>();
                stepResult.put("stepNumber", i);
                stepResult.put("stepName", process.getCurrentStep().getStepName());
                stepResult.put("canProceed", process.getCanProceed());
                stepResult.put("status", process.getCurrentStep().getStatus());
                stepResult.put("recommendation", process.getRecommendation());
                
                if (!process.getCanProceed()) {
                    stepResult.put("blockReason", process.getBlockReason());
                    stepResults.add(stepResult);
                    break;
                }
                
                stepResults.add(stepResult);
            }
            
            response.put("status", "success");
            response.put("orderId", orderId);
            response.put("totalSteps", 8);
            response.put("executedSteps", stepResults.size());
            response.put("stepResults", stepResults);
            
        } catch (Exception e) {
            log.error("模拟流程执行失败", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有已加载的业务规则
     * 返回从OWL本体中动态加载的所有规则定义
     */
    @GetMapping("/loaded-rules")
    public ResponseEntity<Map<String, Object>> getLoadedRules() {
        log.info("获取所有已加载的业务规则");
        Map<String, Object> result = processReasoningService.getLoadedRules();
        return ResponseEntity.ok(result);
    }

    /**
     * 根据规则代码获取规则详情
     * @param ruleCode 规则代码，如 FraudCustomerCheckRule
     */
    @GetMapping("/rule/{ruleCode}")
    public ResponseEntity<Map<String, Object>> getRuleByCode(@PathVariable String ruleCode) {
        log.info("获取规则详情: {}", ruleCode);
        Map<String, Object> result = processReasoningService.getRuleByCode(ruleCode);
        return ResponseEntity.ok(result);
    }

    /**
     * 动态加载业务规则从OWL本体
     * 强制重新加载所有规则
     */
    @PostMapping("/reload-rules")
    public ResponseEntity<Map<String, Object>> reloadRules() {
        log.info("重新加载业务规则");
        Map<String, Object> result = processReasoningService.initializeSWRLRules();
        return ResponseEntity.ok(result);
    }
    /**
     * 根据规则代码执行logicExpression推理
     * @param ruleCode 规则代码
     * @param requestBody 推理上下文数据
     */
    @PostMapping("/reasoning/execute-rule/{ruleCode}")
    public ResponseEntity<Map<String, Object>> executeRuleByCode(
            @PathVariable String ruleCode,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("执行规则推理: ruleCode={}", ruleCode);
        
        Map<String, Object> context = requestBody != null ? requestBody : new HashMap<>();
        Map<String, Object> result = processReasoningService.executeRuleByCode(ruleCode, context);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 执行所有业务规则的推理
     * @param orderId 订单ID
     * @param requestBody 推理上下文数据
     */
    @PostMapping("/reasoning/execute-all/{orderId}")
    public ResponseEntity<Map<String, Object>> executeAllRulesReasoning(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("执行所有业务规则推理: orderId={}", orderId);
        
        Map<String, Object> context = requestBody != null ? requestBody : new HashMap<>();
        Map<String, Object> result = processReasoningService.executeAllRulesReasoning(orderId, context);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取规则的logicExpression
     * @param ruleCode 规则代码
     */
    @GetMapping("/rule/{ruleCode}/expression")
    public ResponseEntity<Map<String, Object>> getRuleExpression(@PathVariable String ruleCode) {
        log.info("获取规则表达式: {}", ruleCode);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> rule = processReasoningService.getRuleByCode(ruleCode);
            
            if (rule != null) {
                result.put("status", "success");
                result.put("ruleCode", ruleCode);
                result.put("expression", rule.get("ruleBody"));
                result.put("type", rule.get("ruleType"));
                result.put("priority", rule.get("priority"));
                result.put("description", rule.get("description"));
            } else {
                result.put("status", "not_found");
                result.put("message", "规则不存在");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}