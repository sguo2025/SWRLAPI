package com.iwhalecloud.ontology.controller;

import com.iwhalecloud.ontology.service.OntologyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OWL本体REST控制器
 * 提供本体查询、修改、推理等接口
 *
 * @author 
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OntologyController {

    private final OntologyService ontologyService;

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Transfer Order Ontology Service");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取本体基本信息
     */
    @GetMapping("/ontology/info")
    public ResponseEntity<Map<String, Object>> getOntologyInfo() {
        log.info("获取本体信息");
        return ResponseEntity.ok(ontologyService.getOntologyInfo());
    }

    /**
     * 获取所有类
     */
    @GetMapping("/classes")
    public ResponseEntity<List<Map<String, String>>> getAllClasses() {
        log.info("获取所有类");
        return ResponseEntity.ok(ontologyService.getAllClasses());
    }

    /**
     * 获取所有个体
     */
    @GetMapping("/individuals")
    public ResponseEntity<List<Map<String, Object>>> getAllIndividuals() {
        log.info("获取所有个体");
        return ResponseEntity.ok(ontologyService.getAllIndividuals());
    }

    /**
     * 根据类名获取个体
     */
    @GetMapping("/individuals/class/{className}")
    public ResponseEntity<List<Map<String, Object>>> getIndividualsByClass(@PathVariable String className) {
        log.info("获取类 {} 的所有个体", className);
        return ResponseEntity.ok(ontologyService.getIndividualsByClass(className));
    }

    /**
     * 获取个体的属性
     */
    @GetMapping("/individuals/{individualName}/properties")
    public ResponseEntity<Map<String, Object>> getIndividualProperties(@PathVariable String individualName) {
        log.info("获取个体 {} 的属性", individualName);
        return ResponseEntity.ok(ontologyService.getIndividualProperties(individualName));
    }

    /**
     * 添加新个体
     */
    @PostMapping("/individuals")
    public ResponseEntity<Map<String, String>> addIndividual(@RequestBody Map<String, String> request) {
        String className = request.get("className");
        String individualName = request.get("individualName");
        
        log.info("添加个体: {} 到类: {}", individualName, className);
        Map<String, String> result = ontologyService.addIndividual(className, individualName);
        return ResponseEntity.ok(result);
    }

    /**
     * 为个体添加数据属性
     */
    @PostMapping("/individuals/{individualName}/data-property")
    public ResponseEntity<Map<String, String>> addDataProperty(
            @PathVariable String individualName,
            @RequestBody Map<String, String> request) {
        
        String propertyName = request.get("propertyName");
        String value = request.get("value");
        String datatype = request.getOrDefault("datatype", "string");
        
        log.info("为个体 {} 添加数据属性: {}={} ({})", individualName, propertyName, value, datatype);
        ontologyService.addDataProperty(individualName, propertyName, value, datatype);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "数据属性添加成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 为个体添加对象属性
     */
    @PostMapping("/individuals/{sourceIndividual}/object-property")
    public ResponseEntity<Map<String, String>> addObjectProperty(
            @PathVariable String sourceIndividual,
            @RequestBody Map<String, String> request) {
        
        String propertyName = request.get("propertyName");
        String targetIndividual = request.get("targetIndividual");
        
        log.info("为个体 {} 添加对象属性: {} -> {}", sourceIndividual, propertyName, targetIndividual);
        ontologyService.addObjectProperty(sourceIndividual, propertyName, targetIndividual);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "对象属性添加成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 执行SWRL推理
     */
    @PostMapping("/reasoning/swrl")
    public ResponseEntity<Map<String, Object>> executeSWRLReasoning() {
        log.info("执行SWRL推理");
        return ResponseEntity.ok(ontologyService.executeSWRLReasoning());
    }

    /**
     * 创建过户订单示例
     * 演示如何创建完整的过户订单及其关联实体
     */
    @PostMapping("/transfer/create-order-example")
    public ResponseEntity<Map<String, Object>> createTransferOrderExample(@RequestBody Map<String, String> request) {
        log.info("创建过户订单示例");
        
        try {
            String orderId = request.getOrDefault("orderId", "ORDER_" + System.currentTimeMillis());
            String sourceCustomerId = request.getOrDefault("sourceCustomerId", "CUST_SOURCE_001");
            String targetCustomerId = request.getOrDefault("targetCustomerId", "CUST_TARGET_001");
            
            // 1. 创建源客户
            ontologyService.addIndividual("SourceCustomer", sourceCustomerId);
            ontologyService.addDataProperty(sourceCustomerId, "custId", sourceCustomerId, "string");
            ontologyService.addDataProperty(sourceCustomerId, "custName", "张三", "string");
            ontologyService.addDataProperty(sourceCustomerId, "custStatus", "NORMAL", "string");
            ontologyService.addDataProperty(sourceCustomerId, "arrearsStatus", "NO_ARREARS", "string");
            
            // 2. 创建目标客户
            ontologyService.addIndividual("TargetCustomer", targetCustomerId);
            ontologyService.addDataProperty(targetCustomerId, "custId", targetCustomerId, "string");
            ontologyService.addDataProperty(targetCustomerId, "custName", "李四", "string");
            ontologyService.addDataProperty(targetCustomerId, "custStatus", "NORMAL", "string");
            
            // 3. 创建过户订单
            ontologyService.addIndividual("TransferOrder", orderId);
            ontologyService.addDataProperty(orderId, "orderId", orderId, "string");
            ontologyService.addDataProperty(orderId, "orderStatus", "CREATED", "string");
            ontologyService.addDataProperty(orderId, "currentStepNumber", "1", "integer");
            ontologyService.addDataProperty(orderId, "totalSteps", "8", "integer");
            
            // 4. 建立关系
            ontologyService.addObjectProperty(orderId, "hasSourceCustomer", sourceCustomerId);
            ontologyService.addObjectProperty(orderId, "hasTargetCustomer", targetCustomerId);
            
            // 5. 创建可转移订阅
            String subscriptionId = "SUB_" + System.currentTimeMillis();
            ontologyService.addIndividual("TransferableSubscription", subscriptionId);
            ontologyService.addDataProperty(subscriptionId, "accNum", "13800138000", "string");
            ontologyService.addDataProperty(subscriptionId, "prodInstId", subscriptionId, "string");
            
            ontologyService.addObjectProperty(sourceCustomerId, "ownsSubscription", subscriptionId);
            ontologyService.addObjectProperty(orderId, "changesSubscription", subscriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "过户订单示例创建成功");
            response.put("orderId", orderId);
            response.put("sourceCustomerId", sourceCustomerId);
            response.put("targetCustomerId", targetCustomerId);
            response.put("subscriptionId", subscriptionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建过户订单示例失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查客户状态
     * 演示SWRL规则推理：检查客户是否涉诈或欠费
     */
    @PostMapping("/transfer/check-customer-status")
    public ResponseEntity<Map<String, Object>> checkCustomerStatus(@RequestBody Map<String, String> request) {
        log.info("检查客户状态");
        
        try {
            String customerId = request.get("customerId");
            String custStatus = request.getOrDefault("custStatus", "NORMAL");
            String arrearsStatus = request.getOrDefault("arrearsStatus", "NO_ARREARS");
            
            // 创建测试客户
            ontologyService.addIndividual("SourceCustomer", customerId);
            ontologyService.addDataProperty(customerId, "custId", customerId, "string");
            ontologyService.addDataProperty(customerId, "custStatus", custStatus, "string");
            ontologyService.addDataProperty(customerId, "arrearsStatus", arrearsStatus, "string");
            
            // 执行推理
            Map<String, Object> reasoningResult = ontologyService.executeSWRLReasoning();
            
            // 查询推理结果
            Map<String, Object> properties = ontologyService.getIndividualProperties(customerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("customerId", customerId);
            response.put("properties", properties);
            response.put("reasoningResult", reasoningResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("检查客户状态失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
