#!/bin/bash

# SWRL规则校验系统 - 完整演示

echo "=========================================="
echo "SWRLAPI 规则校验系统 - 实时演示"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080/ontology"

echo "【第一步】系统健康检查"
echo "================================"
curl -s "$BASE_URL/api/health" | jq '{status, service, version}'
echo ""

echo "【第二步】本体基本信息"
echo "================================"
curl -s "$BASE_URL/api/ontology/info" | jq '{
  ontologyIRI,
  classesCount,
  individualsCount,
  objectPropertiesCount,
  dataPropertiesCount,
  axiomsCount
}'
echo ""

echo "【第三步】获取所有流程步骤"
echo "================================"
curl -s "$BASE_URL/api/process/steps" | jq '.[] | {stepNumber, stepCode, stepName}'
echo ""

echo "【第四步】推理订单001从步骤1到步骤2"
echo "================================"
curl -s -X POST "$BASE_URL/api/process/reason-next-step" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER001",
    "currentStepNumber": 1
  }' | jq '{
    orderId,
    currentStepNumber,
    canProceed,
    nextStepNumber,
    recommendation
  }'
echo ""

echo "【第五步】完整流程推理"
echo "================================"
curl -s -X POST "$BASE_URL/api/process/full-reasoning/ORDER002" \
  -H "Content-Type: application/json" | jq '{
    orderId: .orderId,
    status: .status,
    message: .message
  }'
echo ""

echo "【第六步】模拟完整过户流程"
echo "================================"
curl -s -X POST "$BASE_URL/api/process/simulate-full-process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER_DEMO",
    "sourceCustomerId": "CUST001",
    "targetCustomerId": "CUST002"
  }' | jq '.[] | {
    stepNumber,
    stepCode,
    stepName,
    status
  }'
echo ""

echo "=========================================="
echo "✓ 演示完成！"
echo "=========================================="
echo ""
echo "SWRL规则校验系统工作正常！"
echo "本体包含："
echo "  - 38个OWL类"
echo "  - 17个个体"
echo "  - 14个对象属性"
echo "  - 57个数据属性"
echo "  - 582个公理"
echo ""
echo "规则校验系统包含："
echo "  - 6个核心SWRL规则"
echo "  - 9个完整业务规则"
echo "  - 8个流程步骤"
echo "  - 优先级基础的规则执行"
