#!/bin/bash

###############################################################################
# 流程推理API测试脚本
# 测试8步过户流程的推理功能
###############################################################################

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080/ontology/api"

print_header() {
    echo -e "\n${BLUE}======================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}======================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}➤ $1${NC}"
}

# 1. 获取所有流程步骤
test_get_all_steps() {
    print_header "测试1: 获取所有流程步骤定义"
    print_info "GET ${BASE_URL}/process/steps"
    
    curl -s -X GET "${BASE_URL}/process/steps" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "流程步骤定义获取完成"
}

# 2. 创建测试订单
test_create_test_order() {
    print_header "测试2: 创建测试订单"
    print_info "POST ${BASE_URL}/transfer/create-order-example"
    
    TIMESTAMP=$(date +%s)
    ORDER_ID="ORDER_TEST_${TIMESTAMP}"
    
    curl -s -X POST "${BASE_URL}/transfer/create-order-example" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ORDER_ID}\",
            \"sourceCustomerId\": \"CUST_SOURCE_TEST\",
            \"targetCustomerId\": \"CUST_TARGET_TEST\"
        }" | jq '.'
    
    # 设置订单的当前步骤
    curl -s -X POST "${BASE_URL}/individuals/${ORDER_ID}/data-property" \
        -H "Content-Type: application/json" \
        -d '{
            "propertyName": "currentStepNumber",
            "value": "1",
            "datatype": "integer"
        }' > /dev/null
    
    echo "$ORDER_ID" > /tmp/test_order_id.txt
    print_success "测试订单创建完成: ${ORDER_ID}"
}

# 3. 获取订单流程状态
test_get_process_status() {
    print_header "测试3: 获取订单流程状态"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "GET ${BASE_URL}/process/status/${ORDER_ID}"
    
    curl -s -X GET "${BASE_URL}/process/status/${ORDER_ID}" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "订单流程状态获取完成"
}

# 4. 推理步骤1的下一步
test_reason_step1() {
    print_header "测试4: 推理步骤1的下一步"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "POST ${BASE_URL}/process/reason-next-step"
    
    curl -s -X POST "${BASE_URL}/process/reason-next-step" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ORDER_ID}\",
            \"currentStepNumber\": 1
        }" | jq '.'
    
    print_success "步骤1推理完成"
}

# 5. 执行步骤推进
test_proceed_to_next() {
    print_header "测试5: 执行步骤推进"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "POST ${BASE_URL}/process/proceed"
    
    curl -s -X POST "${BASE_URL}/process/proceed" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ORDER_ID}\",
            \"currentStepNumber\": 1
        }" | jq '.'
    
    print_success "步骤推进完成"
}

# 6. 验证步骤前置条件
test_validate_prerequisites() {
    print_header "测试6: 验证步骤前置条件"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "POST ${BASE_URL}/process/validate-prerequisites"
    
    curl -s -X POST "${BASE_URL}/process/validate-prerequisites" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ORDER_ID}\",
            \"stepNumber\": 2
        }" | jq '.'
    
    print_success "前置条件验证完成"
}

# 7. 测试步骤回退
test_rollback() {
    print_header "测试7: 测试步骤回退(从步骤3回退到步骤1)"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "POST ${BASE_URL}/process/rollback"
    
    curl -s -X POST "${BASE_URL}/process/rollback" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ORDER_ID}\",
            \"fromStep\": 3,
            \"toStep\": 1
        }" | jq '.'
    
    print_success "步骤回退测试完成"
}

# 8. 模拟完整流程执行
test_simulate_full_process() {
    print_header "测试8: 模拟完整流程执行"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "POST ${BASE_URL}/process/simulate-full-process"
    
    curl -s -X POST "${BASE_URL}/process/simulate-full-process" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ORDER_ID}\"
        }" | jq '.'
    
    print_success "完整流程模拟完成"
}

# 9. 测试涉诈客户的流程阻断
test_fraud_customer_block() {
    print_header "测试9: 测试涉诈客户的流程阻断"
    
    TIMESTAMP=$(date +%s)
    FRAUD_ORDER="ORDER_FRAUD_${TIMESTAMP}"
    
    # 创建涉诈订单
    curl -s -X POST "${BASE_URL}/individuals" \
        -H "Content-Type: application/json" \
        -d "{
            \"className\": \"TransferOrder\",
            \"individualName\": \"${FRAUD_ORDER}\"
        }" > /dev/null
    
    # 设置涉诈状态
    curl -s -X POST "${BASE_URL}/individuals/${FRAUD_ORDER}/data-property" \
        -H "Content-Type: application/json" \
        -d '{
            "propertyName": "custStatus",
            "value": "FRAUD",
            "datatype": "string"
        }' > /dev/null
    
    # 推理下一步（应该被阻断）
    print_info "推理涉诈订单的下一步"
    curl -s -X POST "${BASE_URL}/process/reason-next-step" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${FRAUD_ORDER}\",
            \"currentStepNumber\": 2
        }" | jq '.'
    
    print_success "涉诈客户阻断测试完成"
}

# 10. 测试欠费客户的流程阻断
test_arrears_customer_block() {
    print_header "测试10: 测试欠费客户的流程阻断"
    
    TIMESTAMP=$(date +%s)
    ARREARS_ORDER="ORDER_ARREARS_${TIMESTAMP}"
    
    # 创建欠费订单
    curl -s -X POST "${BASE_URL}/individuals" \
        -H "Content-Type: application/json" \
        -d "{
            \"className\": \"TransferOrder\",
            \"individualName\": \"${ARREARS_ORDER}\"
        }" > /dev/null
    
    # 设置欠费状态
    curl -s -X POST "${BASE_URL}/individuals/${ARREARS_ORDER}/data-property" \
        -H "Content-Type: application/json" \
        -d '{
            "propertyName": "arrearsStatus",
            "value": "ARREARS",
            "datatype": "string"
        }' > /dev/null
    
    # 推理下一步（应该被阻断）
    print_info "推理欠费订单的下一步"
    curl -s -X POST "${BASE_URL}/process/reason-next-step" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"${ARREARS_ORDER}\",
            \"currentStepNumber\": 3
        }" | jq '.'
    
    print_success "欠费客户阻断测试完成"
}

# 11. 执行完整推理
test_full_reasoning() {
    print_header "测试11: 执行完整SWRL推理"
    
    ORDER_ID=$(cat /tmp/test_order_id.txt 2>/dev/null || echo "ORDER_TEST_001")
    print_info "POST ${BASE_URL}/process/full-reasoning/${ORDER_ID}"
    
    curl -s -X POST "${BASE_URL}/process/full-reasoning/${ORDER_ID}" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "完整推理执行完成"
}

# 12. 测试步骤顺序执行
test_sequential_execution() {
    print_header "测试12: 测试步骤顺序执行"
    
    TIMESTAMP=$(date +%s)
    SEQ_ORDER="ORDER_SEQ_${TIMESTAMP}"
    
    print_info "创建顺序测试订单: ${SEQ_ORDER}"
    
    # 创建订单
    curl -s -X POST "${BASE_URL}/individuals" \
        -H "Content-Type: application/json" \
        -d "{
            \"className\": \"TransferOrder\",
            \"individualName\": \"${SEQ_ORDER}\"
        }" > /dev/null
    
    # 依次推理步骤1到3
    for i in 1 2 3; do
        echo ""
        print_info "推理步骤 ${i}"
        curl -s -X POST "${BASE_URL}/process/reason-next-step" \
            -H "Content-Type: application/json" \
            -d "{
                \"orderId\": \"${SEQ_ORDER}\",
                \"currentStepNumber\": ${i}
            }" | jq '.currentStep, .nextStep, .canProceed, .recommendation'
        sleep 1
    done
    
    print_success "顺序执行测试完成"
}

# 主测试流程
main() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║      流程推理API测试脚本                                  ║"
    echo "║      Process Reasoning API Test Script                   ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    if ! command -v jq &> /dev/null; then
        print_error "jq未安装，请先安装jq"
        exit 1
    fi
    
    # 检查服务
    print_info "检查服务状态..."
    if ! curl -s "${BASE_URL}/health" > /dev/null; then
        print_error "服务未启动，请先启动服务"
        exit 1
    fi
    print_success "服务运行正常"
    
    # 执行测试
    test_get_all_steps
    sleep 1
    
    test_create_test_order
    sleep 1
    
    test_get_process_status
    sleep 1
    
    test_reason_step1
    sleep 1
    
    test_proceed_to_next
    sleep 1
    
    test_validate_prerequisites
    sleep 1
    
    test_rollback
    sleep 1
    
    test_simulate_full_process
    sleep 1
    
    test_fraud_customer_block
    sleep 1
    
    test_arrears_customer_block
    sleep 1
    
    test_full_reasoning
    sleep 1
    
    test_sequential_execution
    
    # 测试完成
    print_header "测试完成"
    print_success "所有流程推理API测试执行完毕！"
    
    if [ -f /tmp/test_order_id.txt ]; then
        ORDER_ID=$(cat /tmp/test_order_id.txt)
        echo ""
        print_info "测试订单ID: ${ORDER_ID}"
        print_info "可以使用以下命令查看订单状态:"
        echo "  curl ${BASE_URL}/process/status/${ORDER_ID} | jq"
    fi
    
    echo ""
}

# 执行主程序
if [ $# -eq 0 ]; then
    main
else
    case "$1" in
        steps)
            test_get_all_steps
            ;;
        status)
            test_get_process_status
            ;;
        reason)
            test_reason_step1
            ;;
        proceed)
            test_proceed_to_next
            ;;
        rollback)
            test_rollback
            ;;
        simulate)
            test_simulate_full_process
            ;;
        fraud)
            test_fraud_customer_block
            ;;
        arrears)
            test_arrears_customer_block
            ;;
        sequential)
            test_sequential_execution
            ;;
        *)
            echo "用法: $0 [steps|status|reason|proceed|rollback|simulate|fraud|arrears|sequential]"
            exit 1
            ;;
    esac
fi
