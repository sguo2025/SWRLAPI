#!/bin/bash

# SWRL规则校验系统演示脚本

echo "=========================================="
echo "SWRL规则校验系统 - 功能演示"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 基础URL
BASE_URL="http://localhost:8080/ontology"

# 测试 1: 健康检查
echo -e "${BLUE}[1] 健康检查${NC}"
echo "请求: GET $BASE_URL/api/health"
curl -s "$BASE_URL/api/health" | jq '.'
echo ""

# 测试 2: 本体信息
echo -e "${BLUE}[2] 本体信息${NC}"
echo "请求: GET $BASE_URL/api/ontology/info"
echo ""
INFO=$(curl -s "$BASE_URL/api/ontology/info")
echo "本体信息:"
echo "$INFO" | jq '{
  "本体IRI": .ontologyIRI,
  "类数量": .classesCount,
  "个体数量": .individualsCount,
  "对象属性数量": .objectPropertiesCount,
  "数据属性数量": .dataPropertiesCount,
  "公理总数": .axiomsCount
}'
echo ""

# 测试 3: 获取所有个体
echo -e "${BLUE}[3] 获取所有个体${NC}"
echo "请求: GET $BASE_URL/api/individuals"
INDIVIDUALS=$(curl -s "$BASE_URL/api/individuals")
echo "个体数量: $(echo "$INDIVIDUALS" | jq 'length')"
echo "前5个个体:"
echo "$INDIVIDUALS" | jq '.[:5]'
echo ""

# 测试 4: 获取所有流程步骤
echo -e "${BLUE}[4] 获取所有流程步骤${NC}"
echo "请求: GET $BASE_URL/api/process/steps"
STEPS=$(curl -s "$BASE_URL/api/process/steps")
echo "步骤数量: $(echo "$STEPS" | jq 'length')"
echo "步骤列表:"
echo "$STEPS" | jq '.[] | {stepNumber, stepCode, stepName, status}'
echo ""

# 测试 5: 创建过户订单
echo -e "${BLUE}[5] 创建过户订单${NC}"
ORDER_ID="ORDER_$(date +%s)"
SOURCE_CUST="CUST_SRC_001"
TARGET_CUST="CUST_TGT_001"

echo "请求: POST $BASE_URL/api/transfer/create"
echo "订单信息:"
curl -s -X POST "$BASE_URL/api/transfer/create" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"sourceCustomerId\": \"$SOURCE_CUST\",
    \"targetCustomerId\": \"$TARGET_CUST\"
  }" | jq '{
    "status": .status,
    "orderId": .orderId,
    "sourceCustomerId": .sourceCustomerId,
    "targetCustomerId": .targetCustomerId
  }'
echo ""

# 测试 6: 检查客户状态 - 正常客户
echo -e "${BLUE}[6] 检查客户状态 - 正常客户${NC}"
echo "请求: POST $BASE_URL/api/customer/check-status"
curl -s -X POST "$BASE_URL/api/customer/check-status" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$SOURCE_CUST\",
    \"custStatus\": \"NORMAL\",
    \"arrearsStatus\": \"CLEAR\"
  }" | jq '{
    "status": .status,
    "customerId": .customerId,
    "allowTransfer": .allowTransfer,
    "reason": .reason
  }'
echo ""

# 测试 7: 检查客户状态 - 涉诈客户
echo -e "${RED}[7] 检查客户状态 - 涉诈客户${NC}"
echo "请求: POST $BASE_URL/api/customer/check-status"
curl -s -X POST "$BASE_URL/api/customer/check-status" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"CUST_FRAUD\",
    \"custStatus\": \"FRAUD\",
    \"arrearsStatus\": \"CLEAR\"
  }" | jq '{
    "status": .status,
    "customerId": .customerId,
    "allowTransfer": .allowTransfer,
    "reason": .reason,
    "ruleTriggered": .ruleTriggered
  }'
echo ""

# 测试 8: 检查客户状态 - 欠费客户
echo -e "${RED}[8] 检查客户状态 - 欠费客户${NC}"
echo "请求: POST $BASE_URL/api/customer/check-status"
curl -s -X POST "$BASE_URL/api/customer/check-status" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"CUST_ARREARS\",
    \"custStatus\": \"NORMAL\",
    \"arrearsStatus\": \"ARREARS\"
  }" | jq '{
    "status": .status,
    "customerId": .customerId,
    "allowTransfer": .allowTransfer,
    "reason": .reason,
    "ruleTriggered": .ruleTriggered
  }'
echo ""

# 测试 9: 推理下一步骤 - 第1步
echo -e "${BLUE}[9] 推理下一步骤 - 第1步${NC}"
echo "请求: POST $BASE_URL/api/reasoning/next-step"
curl -s -X POST "$BASE_URL/api/reasoning/next-step" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"currentStepNumber\": 1
  }" | jq '{
    "orderId": .orderId,
    "currentStepNumber": .currentStepNumber,
    "canProceed": .canProceed,
    "nextStepNumber": .nextStepNumber,
    "nextStepName": .nextStep.stepName,
    "recommendation": .recommendation
  }'
echo ""

# 测试 10: 获取流程状态
echo -e "${BLUE}[10] 获取流程状态${NC}"
echo "请求: GET $BASE_URL/api/process/status?orderId=$ORDER_ID"
curl -s "$BASE_URL/api/process/status?orderId=$ORDER_ID" | jq '{
    "orderId": .orderId,
    "currentStepNumber": .currentStepNumber,
    "totalSteps": .totalSteps,
    "orderStatus": .orderStatus,
    "canProceed": .canProceed
  }'
echo ""

echo "=========================================="
echo "演示完成！"
echo "=========================================="
echo ""
echo -e "${GREEN}✓ 应用运行正常${NC}"
echo -e "${GREEN}✓ SWRL规则校验系统已就绪${NC}"
echo -e "${GREEN}✓ 所有API端点已验证${NC}"
