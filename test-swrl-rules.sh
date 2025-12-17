#!/bin/bash

# SWRL 规则校验测试脚本

BASE_URL="http://localhost:8080/api/ontology"

echo "========================================="
echo "SWRL 规则校验功能测试"
echo "========================================="

# 1. 初始化SWRL规则
echo -e "\n[1] 初始化SWRL规则..."
curl -X POST "$BASE_URL/reasoning/init-rules" \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 2. 获取第2步的适用规则
echo -e "\n[2] 获取第2步的适用规则..."
curl -X GET "$BASE_URL/reasoning/applicable-rules/2" \
  -H "Content-Type: application/json" \
  -s | jq '.'

# 3. 创建过户订单示例
echo -e "\n[3] 创建过户订单示例..."
curl -X POST "$BASE_URL/transfer/create" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER001",
    "sourceCustomerId": "CUST001",
    "targetCustomerId": "CUST002"
  }' \
  -s | jq '.'

# 4. 检查客户状态 - 正常客户
echo -e "\n[4] 检查客户状态（正常客户）..."
curl -X POST "$BASE_URL/customer/check-status" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "custStatus": "NORMAL",
    "arrearsStatus": "CLEAR"
  }' \
  -s | jq '.'

# 5. 检查客户状态 - 涉诈客户
echo -e "\n[5] 检查客户状态（涉诈客户）..."
curl -X POST "$BASE_URL/customer/check-status" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST003",
    "custStatus": "FRAUD",
    "arrearsStatus": "CLEAR"
  }' \
  -s | jq '.'

# 6. 检查客户状态 - 欠费客户
echo -e "\n[6] 检查客户状态（欠费客户）..."
curl -X POST "$BASE_URL/customer/check-status" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST004",
    "custStatus": "NORMAL",
    "arrearsStatus": "ARREARS"
  }' \
  -s | jq '.'

# 7. 推理下一步骤 - 第1步
echo -e "\n[7] 推理下一步骤（从第1步）..."
curl -X POST "$BASE_URL/reasoning/next-step" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER001",
    "currentStepNumber": 1
  }' \
  -s | jq '.'

# 8. 推理下一步骤 - 第2步（正常情况）
echo -e "\n[8] 推理下一步骤（从第2步，正常客户）..."
curl -X POST "$BASE_URL/reasoning/next-step" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER001",
    "currentStepNumber": 2
  }' \
  -s | jq '.'

# 9. 执行SWRL推理
echo -e "\n[9] 执行SWRL推理..."
curl -X POST "$BASE_URL/reasoning/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER001"
  }' \
  -s | jq '.'

echo -e "\n========================================="
echo "测试完成"
echo "========================================="
