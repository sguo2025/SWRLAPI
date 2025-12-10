package com.iwhalecloud.ontology.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 过户订单流程模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOrderProcess {
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 源客户ID
     */
    private String sourceCustomerId;
    
    /**
     * 目标客户ID
     */
    private String targetCustomerId;
    
    /**
     * 当前步骤编号
     */
    private Integer currentStepNumber;
    
    /**
     * 总步骤数
     */
    private Integer totalSteps;
    
    /**
     * 下一步骤编号（推理结果）
     */
    private Integer nextStepNumber;
    
    /**
     * 订单状态
     */
    private String orderStatus;
    
    /**
     * 所有步骤信息
     */
    private List<ProcessStepInfo> steps;
    
    /**
     * 当前步骤详情
     */
    private ProcessStepInfo currentStep;
    
    /**
     * 下一步骤详情
     */
    private ProcessStepInfo nextStep;
    
    /**
     * 是否可以推进到下一步
     */
    private Boolean canProceed;
    
    /**
     * 阻塞原因
     */
    private String blockReason;
    
    /**
     * 业务规则检查结果
     */
    private Map<String, Object> ruleCheckResults;
    
    /**
     * 推理建议
     */
    private String recommendation;
}
