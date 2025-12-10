package com.iwhalecloud.ontology.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程步骤信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStepInfo {
    
    /**
     * 步骤编号 (1-8)
     */
    private Integer stepNumber;
    
    /**
     * 步骤代码
     */
    private String stepCode;
    
    /**
     * 步骤名称
     */
    private String stepName;
    
    /**
     * 步骤状态
     */
    private StepStatus status;
    
    /**
     * 步骤描述
     */
    private String description;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 需要的实体
     */
    private List<String> requiresEntities;
    
    /**
     * 产出的实体
     */
    private List<String> producesEntities;
    
    /**
     * 映射的ODA组件
     */
    private String odaComponent;
    
    /**
     * 使用的API
     */
    private String apiEndpoint;
    
    /**
     * 执行的规则
     */
    private List<String> businessRules;
    
    /**
     * 是否可回退
     */
    private Boolean canRollback;
    
    /**
     * 回退到步骤
     */
    private Integer rollbackToStep;
    
    /**
     * 步骤状态枚举
     */
    public enum StepStatus {
        PENDING("待执行"),
        IN_PROGRESS("执行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        ROLLED_BACK("已回退"),
        SKIPPED("已跳过");
        
        private final String description;
        
        StepStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
