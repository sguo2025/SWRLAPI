package com.iwhalecloud.ontology.service;

import lombok.extern.slf4j.Slf4j;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OWL本体服务
 * 负责加载OWL本体、执行推理（支持SWRLAPI）、查询类和个体
 */
@Service
@Slf4j
public class OntologyService {

    @Value("${ontology.file.path}")
    private Resource ontologyResource;

    @Value("${ontology.namespace}")
    private String namespace;

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLDataFactory dataFactory;
    private OWLReasoner reasoner;

    @PostConstruct
    public void init() throws Exception {
        log.info("初始化OWL本体服务...");
        
        // 创建OWL管理器和数据工厂
        manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
        
        // 加载本体文件
        log.info("加载本体文件: {}", ontologyResource.getFilename());
        ontology = manager.loadOntologyFromOntologyDocument(ontologyResource.getInputStream());
        log.info("本体加载成功，包含 {} 个公理", ontology.getAxiomCount());
        
        // 创建推理器
        log.info("创建OWL推理器...");
        StructuralReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        reasoner = reasonerFactory.createReasoner(ontology);
        
        log.info("OWL本体服务初始化完成！");
        logOntologyStatistics();
    }

    private void logOntologyStatistics() {
        int classCount = ontology.getClassesInSignature().size();
        int individualCount = ontology.getIndividualsInSignature().size();
        int objectPropertyCount = ontology.getObjectPropertiesInSignature().size();
        int dataPropertyCount = ontology.getDataPropertiesInSignature().size();
        
        log.info("========== 本体统计信息 ==========");
        log.info("类数量: {}", classCount);
        log.info("个体数量: {}", individualCount);
        log.info("对象属性数量: {}", objectPropertyCount);
        log.info("数据属性数量: {}", dataPropertyCount);
        log.info("公理总数: {}", ontology.getAxiomCount());
        log.info("==================================");
    }

    public List<String> getAllClasses() {
        return ontology.getClassesInSignature()
                .stream()
                .map(cls -> cls.getIRI().getFragment())
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getAllIndividuals() {
        return ontology.getIndividualsInSignature()
                .stream()
                .map(ind -> ind.asOWLNamedIndividual().getIRI().getFragment())
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getIndividualsByClass(String className) {
        IRI classIRI = IRI.create(namespace + className);
        OWLClass owlClass = dataFactory.getOWLClass(classIRI);
        
        return reasoner.getInstances(owlClass, false)
                .entities()
                .map(ind -> ind.getIRI().getFragment())
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    public void addIndividual(String className, String individualName) throws OWLOntologyStorageException {
        IRI classIRI = IRI.create(namespace + className);
        IRI individualIRI = IRI.create(namespace + individualName);
        
        OWLClass owlClass = dataFactory.getOWLClass(classIRI);
        OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(individualIRI);
        OWLClassAssertionAxiom axiom = dataFactory.getOWLClassAssertionAxiom(owlClass, individual);
        
        manager.addAxiom(ontology, axiom);
        log.info("添加个体: {} 到类: {}", individualName, className);
    }

    public void addDataProperty(String individualName, String propertyName, String value, String dataType) throws OWLOntologyStorageException {
        IRI individualIRI = IRI.create(namespace + individualName);
        IRI propertyIRI = IRI.create(namespace + propertyName);
        
        OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(individualIRI);
        OWLDataProperty property = dataFactory.getOWLDataProperty(propertyIRI);
        OWLLiteral literal;
        
        switch (dataType.toUpperCase()) {
            case "INTEGER":
                literal = dataFactory.getOWLLiteral(value, dataFactory.getIntegerOWLDatatype());
                break;
            case "BOOLEAN":
                literal = dataFactory.getOWLLiteral(Boolean.parseBoolean(value));
                break;
            case "DOUBLE":
                literal = dataFactory.getOWLLiteral(Double.parseDouble(value));
                break;
            default:
                literal = dataFactory.getOWLLiteral(value);
        }
        
        OWLDataPropertyAssertionAxiom axiom = dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, literal);
        manager.addAxiom(ontology, axiom);
        log.info("添加数据属性: {} = {} 到个体: {}", propertyName, value, individualName);
    }

    public void addObjectProperty(String sourceIndividual, String propertyName, String targetIndividual) throws OWLOntologyStorageException {
        IRI sourceIRI = IRI.create(namespace + sourceIndividual);
        IRI propertyIRI = IRI.create(namespace + propertyName);
        IRI targetIRI = IRI.create(namespace + targetIndividual);
        
        OWLNamedIndividual source = dataFactory.getOWLNamedIndividual(sourceIRI);
        OWLObjectProperty property = dataFactory.getOWLObjectProperty(propertyIRI);
        OWLNamedIndividual target = dataFactory.getOWLNamedIndividual(targetIRI);
        
        OWLObjectPropertyAssertionAxiom axiom = dataFactory.getOWLObjectPropertyAssertionAxiom(property, source, target);
        manager.addAxiom(ontology, axiom);
        log.info("添加对象属性: {} -[{}]-> {}", sourceIndividual, propertyName, targetIndividual);
    }

    public Map<String, Object> executeSWRLReasoning() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "推理完成（使用OWL推理器）");
        result.put("reasonerType", "Structural Reasoner");
        result.put("timestamp", System.currentTimeMillis());
        
        reasoner.flush();
        
        result.put("classesCount", ontology.getClassesInSignature().size());
        result.put("individualsCount", ontology.getIndividualsInSignature().size());
        result.put("axiomsCount", ontology.getAxiomCount());
        
        log.info("OWL推理执行完成");
        return result;
    }

    public Map<String, Object> getIndividualProperties(String individualName) {
        IRI individualIRI = IRI.create(namespace + individualName);
        OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(individualIRI);
        
        Map<String, Object> properties = new HashMap<>();
        
        // 获取数据属性
        Map<String, String> dataProperties = new HashMap<>();
        ontology.getDataPropertyAssertionAxioms(individual).forEach(axiom -> {
            String propertyName = axiom.getProperty().asOWLDataProperty().getIRI().getFragment();
            String value = axiom.getObject().getLiteral();
            dataProperties.put(propertyName, value);
        });
        properties.put("dataProperties", dataProperties);
        
        // 获取对象属性
        Map<String, String> objectProperties = new HashMap<>();
        ontology.getObjectPropertyAssertionAxioms(individual).forEach(axiom -> {
            String propertyName = axiom.getProperty().asOWLObjectProperty().getIRI().getFragment();
            String value = axiom.getObject().asOWLNamedIndividual().getIRI().getFragment();
            objectProperties.put(propertyName, value);
        });
        properties.put("objectProperties", objectProperties);
        
        // 获取类型
        List<String> types = ontology.getClassAssertionAxioms(individual)
                .stream()
                .map(axiom -> axiom.getClassExpression().asOWLClass().getIRI().getFragment())
                .collect(Collectors.toList());
        properties.put("types", types);
        
        return properties;
    }

    public Map<String, Object> getOntologyInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("ontologyIRI", ontology.getOntologyID().getOntologyIRI().orElse(IRI.create("unknown")).toString());
        info.put("classesCount", ontology.getClassesInSignature().size());
        info.put("individualsCount", ontology.getIndividualsInSignature().size());
        info.put("objectPropertiesCount", ontology.getObjectPropertiesInSignature().size());
        info.put("dataPropertiesCount", ontology.getDataPropertiesInSignature().size());
        info.put("axiomsCount", ontology.getAxiomCount());
        info.put("namespace", namespace);
        return info;
    }

    public Map<String, Object> createTransferOrderExample(String orderId, String sourceCustomerId, String targetCustomerId) throws OWLOntologyStorageException {
        log.info("创建过户订单示例: orderId={}, sourceCustomerId={}, targetCustomerId={}", orderId, sourceCustomerId, targetCustomerId);
        
        // 创建源客户
        addIndividual("SourceCustomer", sourceCustomerId);
        addDataProperty(sourceCustomerId, "custId", sourceCustomerId, "STRING");
        addDataProperty(sourceCustomerId, "custName", "源客户" + sourceCustomerId, "STRING");
        addDataProperty(sourceCustomerId, "custStatus", "NORMAL", "STRING");
        
        // 创建目标客户
        addIndividual("TargetCustomer", targetCustomerId);
        addDataProperty(targetCustomerId, "custId", targetCustomerId, "STRING");
        addDataProperty(targetCustomerId, "custName", "目标客户" + targetCustomerId, "STRING");
        addDataProperty(targetCustomerId, "custStatus", "NORMAL", "STRING");
        
        // 创建过户订单
        addIndividual("TransferOrder", orderId);
        addDataProperty(orderId, "orderId", orderId, "STRING");
        addDataProperty(orderId, "orderStatus", "CREATED", "STRING");
        addDataProperty(orderId, "createTime", new Date().toString(), "STRING");
        
        // 建立关系
        addObjectProperty(orderId, "hasSourceCustomer", sourceCustomerId);
        addObjectProperty(orderId, "hasTargetCustomer", targetCustomerId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "过户订单创建成功");
        result.put("orderId", orderId);
        result.put("sourceCustomerId", sourceCustomerId);
        result.put("targetCustomerId", targetCustomerId);
        
        log.info("过户订单创建完成: {}", orderId);
        return result;
    }

    public Map<String, Object> checkCustomerStatus(String customerId, String custStatus, String arrearsStatus) throws OWLOntologyStorageException {
        log.info("检查客户状态: customerId={}, custStatus={}, arrearsStatus={}", customerId, custStatus, arrearsStatus);
        
        addIndividual("SourceCustomer", customerId);
        addDataProperty(customerId, "custId", customerId, "STRING");
        addDataProperty(customerId, "custStatus", custStatus, "STRING");
        addDataProperty(customerId, "arrearsStatus", arrearsStatus, "STRING");
        
        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        result.put("custStatus", custStatus);
        result.put("arrearsStatus", arrearsStatus);
        
        // 简单的业务规则检查
        if ("FRAUD".equals(custStatus)) {
            result.put("allowTransfer", false);
            result.put("reason", "涉诈用户不允许办理任何业务");
            result.put("ruleTriggered", "FraudCustomerCheckRule");
        } else if ("ARREARS".equals(arrearsStatus)) {
            result.put("allowTransfer", false);
            result.put("reason", "用户存在欠费，请先缴清费用");
            result.put("ruleTriggered", "ArrearsCheckRule");
        } else {
            result.put("allowTransfer", true);
            result.put("reason", "客户状态正常，允许过户");
        }
        
        result.put("status", "success");
        log.info("客户状态检查完成: {}", result);
        return result;
    }

    /**
     * 获取本体实例（用于SWRL规则引擎）
     */
    public OWLOntology getOntology() {
        return ontology;
    }

    /**
     * 获取OWL数据工厂（用于创建OWL元素）
     */
    public OWLDataFactory getDataFactory() {
        return dataFactory;
    }

    /**
     * 获取OWL本体管理器
     */
    public OWLOntologyManager getManager() {
        return manager;
    }

    /**
     * 获取OWL推理器
     */
    public OWLReasoner getReasoner() {
        return reasoner;
    }
}
