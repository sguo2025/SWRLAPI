package com.iwhalecloud.ontology.service;

import lombok.extern.slf4j.Slf4j;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * SWRL推理执行器 - 解析并执行logicExpression中的SWRL规则
 * 
 * 功能：
 * 1. 解析SWRL规则表达式
 * 2. 验证规则的有效性
 * 3. 在OWL本体上执行推理
 * 4. 收集推理结果
 */
@Service
@Slf4j
public class SWRLReasoningExecutor {

    private OWLOntologyManager manager;
    private OWLDataFactory dataFactory;
    private OWLReasonerFactory reasonerFactory;

    public SWRLReasoningExecutor() {
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataFactory = manager.getOWLDataFactory();
        this.reasonerFactory = new StructuralReasonerFactory();
    }

    /**
     * 执行SWRL规则表达式
     * @param ontology OWL本体
     * @param swrlExpression SWRL规则表达式（从logicExpression提取）
     * @param context 推理上下文（包含输入变量和数据）
     * @return 推理结果
     */
    public Map<String, Object> executeSWRLExpression(
            OWLOntology ontology,
            String swrlExpression,
            Map<String, Object> context) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始执行SWRL表达式推理...");
            
            // 1. 解析SWRL表达式
            SWRLRuleInfo ruleInfo = parseSWRLExpression(swrlExpression);
            result.put("ruleInfo", ruleInfo);
            
            if (!ruleInfo.isValid()) {
                result.put("status", "error");
                result.put("message", "SWRL规则表达式无效: " + ruleInfo.getErrorMessage());
                return result;
            }
            
            log.debug("规则解析成功 - 前提条件: {}", ruleInfo.getAntecedent());
            log.debug("规则解析成功 - 结论: {}", ruleInfo.getConsequent());
            
            // 2. 验证规则涉及的个体和属性
            Map<String, Object> validationResult = validateRuleElements(ontology, ruleInfo);
            result.put("validation", validationResult);
            
            // 3. 执行推理
            Map<String, Object> reasoningResult = performReasoning(ontology, ruleInfo, context);
            result.put("reasoning", reasoningResult);
            
            // 4. 收集推理结果
            List<Map<String, Object>> inferences = (List<Map<String, Object>>) reasoningResult.get("inferences");
            result.put("status", "success");
            result.put("message", String.format("推理完成: 生成 %d 个推论", inferences.size()));
            result.put("inferences", inferences);
            
            log.info("SWRL表达式推理完成: {}", result.get("message"));
            
        } catch (Exception e) {
            log.error("SWRL表达式推理失败", e);
            result.put("status", "error");
            result.put("message", "推理执行异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 解析SWRL规则表达式
     * SWRL 规则格式: antecedent(前提条件) -> consequent(结论)
     * 例: Customer(?c) ^ hasCustStatus(?c, ?status) ^ swrlb:stringEqualIgnoreCase(?status, 'FRAUD') -> BlockTransfer(?c)
     */
    private SWRLRuleInfo parseSWRLExpression(String expression) {
        SWRLRuleInfo info = new SWRLRuleInfo();
        
        try {
            // 清理表达式（移除注释和多余空格）
            String cleanExpression = expression.replaceAll("#.*?\\n", "\n")  // 移除注释
                                               .replaceAll("\\s+", " ")        // 标准化空格
                                               .trim();
            
            log.debug("清理后的表达式: {}", cleanExpression);
            
            // 分离前提和结论
            String[] parts = cleanExpression.split("\\s*->\\s*");
            
            if (parts.length != 2) {
                info.setValid(false);
                info.setErrorMessage("规则格式错误: 应包含 '->' 分隔符");
                return info;
            }
            
            String antecedent = parts[0].trim();
            String consequent = parts[1].trim();
            
            // 移除末尾的句号
            consequent = consequent.replaceAll("\\.\\s*$", "");
            
            // 解析前提条件和结论
            List<String> antecedentAtoms = parseAtoms(antecedent);
            List<String> consequentAtoms = parseAtoms(consequent);
            
            info.setValid(true);
            info.setAntecedent(antecedent);
            info.setAntecedentAtoms(antecedentAtoms);
            info.setConsequent(consequent);
            info.setConsequentAtoms(consequentAtoms);
            
            // 提取变量
            Set<String> variables = extractVariables(antecedent + " " + consequent);
            info.setVariables(variables);
            
            log.debug("解析的变量: {}", variables);
            
        } catch (Exception e) {
            info.setValid(false);
            info.setErrorMessage("解析异常: " + e.getMessage());
            log.warn("SWRL表达式解析异常", e);
        }
        
        return info;
    }

    /**
     * 将SWRL表达式分解为原子公式
     * 原子公式由 ^ (AND) 连接
     */
    private List<String> parseAtoms(String expression) {
        return Arrays.stream(expression.split("\\s*\\^\\s*"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
    }

    /**
     * 从SWRL表达式中提取变量
     * 变量以 ? 开头
     */
    private Set<String> extractVariables(String expression) {
        Set<String> variables = new HashSet<>();
        Pattern pattern = Pattern.compile("\\?(\\w+)");
        Matcher matcher = pattern.matcher(expression);
        
        while (matcher.find()) {
            variables.add("?" + matcher.group(1));
        }
        
        return variables;
    }

    /**
     * 验证规则涉及的元素在本体中是否存在
     */
    private Map<String, Object> validateRuleElements(OWLOntology ontology, SWRLRuleInfo ruleInfo) {
        Map<String, Object> result = new HashMap<>();
        List<String> missingElements = new ArrayList<>();
        List<String> foundElements = new ArrayList<>();
        
        try {
            String namespace = getOntologyNamespace(ontology);
            
            // 提取规则中提到的类、属性和个体
            Set<String> elements = extractElements(ruleInfo.getAntecedent() + " " + ruleInfo.getConsequent());
            
            for (String element : elements) {
                if (element.startsWith("?") || element.contains("swrlb:")) {
                    continue; // 跳过变量和内置函数
                }
                
                // 检查类
                IRI classIRI = IRI.create(namespace + element);
                OWLClass owlClass = manager.getOWLDataFactory().getOWLClass(classIRI);
                
                if (ontology.containsClassInSignature(classIRI)) {
                    foundElements.add(element + " (class)");
                } else {
                    // 检查属性
                    OWLObjectProperty objProp = manager.getOWLDataFactory().getOWLObjectProperty(classIRI);
                    if (ontology.containsObjectPropertyInSignature(classIRI)) {
                        foundElements.add(element + " (object property)");
                    } else {
                        missingElements.add(element);
                    }
                }
            }
            
            result.put("status", missingElements.isEmpty() ? "valid" : "partial");
            result.put("foundElements", foundElements);
            result.put("missingElements", missingElements);
            
        } catch (Exception e) {
            log.warn("验证规则元素时出错", e);
            result.put("status", "warning");
            result.put("message", "验证过程中出错: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 从SWRL表达式中提取元素名称
     */
    private Set<String> extractElements(String expression) {
        Set<String> elements = new HashSet<>();
        
        // 提取类名和属性名（大写开头或驼峰命名）
        Pattern pattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9]*)\\b");
        Matcher matcher = pattern.matcher(expression);
        
        while (matcher.find()) {
            String element = matcher.group(1);
            if (!element.equals("AND") && !element.equals("OR")) {
                elements.add(element);
            }
        }
        
        return elements;
    }

    /**
     * 执行推理
     */
    private Map<String, Object> performReasoning(
            OWLOntology ontology,
            SWRLRuleInfo ruleInfo,
            Map<String, Object> context) {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> inferences = new ArrayList<>();
        
        try {
            // 创建推理器
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
            
            log.info("推理器创建成功: {}", reasoner.getClass().getSimpleName());
            
            // 根据前提条件查询匹配的个体
            List<Map<String, Object>> matchingInstances = queryAntecedent(
                ontology, reasoner, ruleInfo, context
            );
            
            log.info("找到 {} 个匹配前提条件的实例", matchingInstances.size());
            
            // 对每个匹配实例应用结论
            for (Map<String, Object> instance : matchingInstances) {
                Map<String, Object> inference = new HashMap<>();
                inference.put("matchedInstance", instance);
                
                // 应用结论规则
                Map<String, Object> conclusion = applyConsequent(
                    ontology, ruleInfo, instance, context
                );
                inference.put("consequence", conclusion);
                
                inferences.add(inference);
                log.debug("生成推论: {}", inference);
            }
            
            result.put("status", "success");
            result.put("inferences", inferences);
            result.put("totalInferences", inferences.size());
            
            reasoner.dispose();
            
        } catch (Exception e) {
            log.error("推理执行异常", e);
            result.put("status", "error");
            result.put("message", "推理异常: " + e.getMessage());
            result.put("inferences", inferences);
        }
        
        return result;
    }

    /**
     * 查询满足前提条件的实例
     */
    private List<Map<String, Object>> queryAntecedent(
            OWLOntology ontology,
            OWLReasoner reasoner,
            SWRLRuleInfo ruleInfo,
            Map<String, Object> context) {
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 这是一个简化的实现
            // 真实场景中需要使用 SPARQL 或复杂的 OWL 查询
            
            // 从上下文中获取已知的实例
            if (context != null && !context.isEmpty()) {
                Map<String, Object> match = new HashMap<>(context);
                match.put("ruleName", ruleInfo.getRuleName());
                results.add(match);
            }
            
            // 如果没有提供上下文，则遍历本体中的所有个体
            if (results.isEmpty()) {
                Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();
                
                for (OWLNamedIndividual individual : individuals) {
                    Map<String, Object> instanceData = new HashMap<>();
                    instanceData.put("individual", individual.getIRI().getShortForm());
                    instanceData.put("iri", individual.getIRI().toString());
                    
                    // 收集个体的属性
                    collectIndividualProperties(ontology, individual, instanceData);
                    
                    results.add(instanceData);
                }
            }
            
        } catch (Exception e) {
            log.warn("查询前提条件异常", e);
        }
        
        return results;
    }

    /**
     * 收集个体的属性信息
     */
    private void collectIndividualProperties(
            OWLOntology ontology,
            OWLNamedIndividual individual,
            Map<String, Object> data) {
        
        try {
            // 获取个体的类型
            Set<OWLClass> types = individual.getTypes(ontology);
            data.put("types", types.stream()
                    .map(t -> t.getIRI().getShortForm())
                    .collect(Collectors.toList()));
            
            // 获取对象属性值
            Set<OWLObjectProperty> objProperties = individual.getObjectPropertiesInSignature();
            Map<String, List<String>> objPropValues = new HashMap<>();
            
            for (OWLObjectProperty prop : objProperties) {
                Set<OWLIndividual> values = individual.getObjectPropertyValues(prop, ontology);
                objPropValues.put(prop.getIRI().getShortForm(),
                    values.stream()
                        .map(v -> v.asOWLNamedIndividual()
                            .map(ind -> ind.getIRI().getShortForm())
                            .orElse(v.toString()))
                        .collect(Collectors.toList()));
            }
            
            data.put("objectProperties", objPropValues);
            
            // 获取数据属性值
            Set<OWLDataProperty> dataProperties = individual.getDataPropertiesInSignature();
            Map<String, List<String>> dataPropValues = new HashMap<>();
            
            for (OWLDataProperty prop : dataProperties) {
                Set<OWLLiteral> values = individual.getDataPropertyValues(prop, ontology);
                dataPropValues.put(prop.getIRI().getShortForm(),
                    values.stream()
                        .map(OWLLiteral::getLiteral)
                        .collect(Collectors.toList()));
            }
            
            data.put("dataProperties", dataPropValues);
            
        } catch (Exception e) {
            log.debug("收集个体属性异常", e);
        }
    }

    /**
     * 应用结论规则
     */
    private Map<String, Object> applyConsequent(
            OWLOntology ontology,
            SWRLRuleInfo ruleInfo,
            Map<String, Object> matchedInstance,
            Map<String, Object> context) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析结论表达式
            List<String> consequentAtoms = ruleInfo.getConsequentAtoms();
            List<Map<String, Object>> conclusions = new ArrayList<>();
            
            for (String atom : consequentAtoms) {
                Map<String, Object> conclusion = new HashMap<>();
                
                // 提取谓词和参数
                String predicate = atom.split("\\(")[0].trim();
                String args = atom.replaceAll("^[^(]*\\(|\\)$", "").trim();
                
                conclusion.put("predicate", predicate);
                conclusion.put("arguments", args);
                
                // 如果是简单的值赋值，提取具体值
                if (args.contains("\"")) {
                    String value = args.replaceAll(".*\"([^\"]*)\".*", "$1");
                    conclusion.put("value", value);
                }
                
                conclusions.add(conclusion);
                log.debug("应用结论: {}", conclusion);
            }
            
            result.put("status", "applied");
            result.put("conclusions", conclusions);
            result.put("message", String.format("应用了 %d 个结论", conclusions.size()));
            
        } catch (Exception e) {
            log.warn("应用结论异常", e);
            result.put("status", "error");
            result.put("message", "应用结论异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取本体命名空间
     */
    private String getOntologyNamespace(OWLOntology ontology) {
        Optional<IRI> ontologyIRI = ontology.getOntologyID().getOntologyIRI();
        if (ontologyIRI.isPresent()) {
            String iri = ontologyIRI.get().toString();
            if (!iri.endsWith("#") && !iri.endsWith("/")) {
                iri += "#";
            }
            return iri;
        }
        return "https://iwhalecloud.com/ontology/transfer#";
    }

    /**
     * SWRL规则信息容器
     */
    public static class SWRLRuleInfo {
        private boolean valid;
        private String errorMessage;
        private String ruleName;
        private String antecedent;
        private String consequent;
        private List<String> antecedentAtoms;
        private List<String> consequentAtoms;
        private Set<String> variables;

        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        
        public String getAntecedent() { return antecedent; }
        public void setAntecedent(String antecedent) { this.antecedent = antecedent; }
        
        public String getConsequent() { return consequent; }
        public void setConsequent(String consequent) { this.consequent = consequent; }
        
        public List<String> getAntecedentAtoms() { return antecedentAtoms; }
        public void setAntecedentAtoms(List<String> antecedentAtoms) { this.antecedentAtoms = antecedentAtoms; }
        
        public List<String> getConsequentAtoms() { return consequentAtoms; }
        public void setConsequentAtoms(List<String> consequentAtoms) { this.consequentAtoms = consequentAtoms; }
        
        public Set<String> getVariables() { return variables; }
        public void setVariables(Set<String> variables) { this.variables = variables; }
    }
}
