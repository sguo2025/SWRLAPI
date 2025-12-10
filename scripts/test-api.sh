#!/bin/bash

###############################################################################
# BSS4.0 客户过户受理本体服务 - API测试脚本
# 使用curl命令测试所有REST接口
###############################################################################

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 服务基础URL
BASE_URL="http://localhost:8080/ontology/api"

# 打印函数
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

# 检查服务是否启动
check_service() {
    print_header "检查服务健康状态"
    response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/health)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "200" ]; then
        print_success "服务运行正常"
        echo "$body" | jq '.'
    else
        print_error "服务未启动或响应异常 (HTTP $http_code)"
        exit 1
    fi
}

# 1. 获取本体信息
test_ontology_info() {
    print_header "测试1: 获取本体信息"
    print_info "GET ${BASE_URL}/ontology/info"
    
    curl -s -X GET "${BASE_URL}/ontology/info" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "本体信息获取完成"
}

# 2. 获取所有类
test_get_classes() {
    print_header "测试2: 获取所有类"
    print_info "GET ${BASE_URL}/classes"
    
    curl -s -X GET "${BASE_URL}/classes" \
        -H "Content-Type: application/json" | jq '.[0:5]'
    
    print_success "类列表获取完成（显示前5个）"
}

# 3. 获取所有个体
test_get_individuals() {
    print_header "测试3: 获取所有个体"
    print_info "GET ${BASE_URL}/individuals"
    
    curl -s -X GET "${BASE_URL}/individuals" \
        -H "Content-Type: application/json" | jq '.[0:10]'
    
    print_success "个体列表获取完成（显示前10个）"
}

# 4. 根据类名获取个体
test_get_individuals_by_class() {
    print_header "测试4: 获取ODAComponent类的个体"
    print_info "GET ${BASE_URL}/individuals/class/ODAComponent"
    
    curl -s -X GET "${BASE_URL}/individuals/class/ODAComponent" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "ODAComponent类的个体获取完成"
}

# 5. 获取个体属性
test_get_individual_properties() {
    print_header "测试5: 获取个体属性"
    print_info "GET ${BASE_URL}/individuals/PartyManagementComponent/properties"
    
    curl -s -X GET "${BASE_URL}/individuals/PartyManagementComponent/properties" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "个体属性获取完成"
}

# 6. 添加新个体
test_add_individual() {
    print_header "测试6: 添加新个体"
    print_info "POST ${BASE_URL}/individuals"
    
    TIMESTAMP=$(date +%s)
    
    curl -s -X POST "${BASE_URL}/individuals" \
        -H "Content-Type: application/json" \
        -d "{
            \"className\": \"SourceCustomer\",
            \"individualName\": \"TestCustomer_${TIMESTAMP}\"
        }" | jq '.'
    
    print_success "新个体添加完成"
}

# 7. 为个体添加数据属性
test_add_data_property() {
    print_header "测试7: 为个体添加数据属性"
    print_info "POST ${BASE_URL}/individuals/TestCustomer_${TIMESTAMP}/data-property"
    
    TIMESTAMP=$(date +%s)
    INDIVIDUAL="TestCustomer_${TIMESTAMP}"
    
    # 先创建个体
    curl -s -X POST "${BASE_URL}/individuals" \
        -H "Content-Type: application/json" \
        -d "{
            \"className\": \"SourceCustomer\",
            \"individualName\": \"${INDIVIDUAL}\"
        }" > /dev/null
    
    # 添加数据属性
    curl -s -X POST "${BASE_URL}/individuals/${INDIVIDUAL}/data-property" \
        -H "Content-Type: application/json" \
        -d "{
            \"propertyName\": \"custName\",
            \"value\": \"测试客户\",
            \"datatype\": \"string\"
        }" | jq '.'
    
    print_success "数据属性添加完成"
}

# 8. 创建过户订单示例
test_create_transfer_order() {
    print_header "测试8: 创建过户订单示例"
    print_info "POST ${BASE_URL}/transfer/create-order-example"
    
    TIMESTAMP=$(date +%s)
    
    curl -s -X POST "${BASE_URL}/transfer/create-order-example" \
        -H "Content-Type: application/json" \
        -d "{
            \"orderId\": \"ORDER_${TIMESTAMP}\",
            \"sourceCustomerId\": \"CUST_SOURCE_${TIMESTAMP}\",
            \"targetCustomerId\": \"CUST_TARGET_${TIMESTAMP}\"
        }" | jq '.'
    
    print_success "过户订单示例创建完成"
}

# 9. 检查正常客户状态
test_check_normal_customer() {
    print_header "测试9: 检查正常客户状态"
    print_info "POST ${BASE_URL}/transfer/check-customer-status"
    
    TIMESTAMP=$(date +%s)
    
    curl -s -X POST "${BASE_URL}/transfer/check-customer-status" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"NormalCustomer_${TIMESTAMP}\",
            \"custStatus\": \"NORMAL\",
            \"arrearsStatus\": \"NO_ARREARS\"
        }" | jq '.'
    
    print_success "正常客户状态检查完成"
}

# 10. 检查涉诈客户状态
test_check_fraud_customer() {
    print_header "测试10: 检查涉诈客户状态"
    print_info "POST ${BASE_URL}/transfer/check-customer-status"
    
    TIMESTAMP=$(date +%s)
    
    curl -s -X POST "${BASE_URL}/transfer/check-customer-status" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"FraudCustomer_${TIMESTAMP}\",
            \"custStatus\": \"FRAUD\",
            \"arrearsStatus\": \"NO_ARREARS\"
        }" | jq '.'
    
    print_success "涉诈客户状态检查完成"
}

# 11. 检查欠费客户状态
test_check_arrears_customer() {
    print_header "测试11: 检查欠费客户状态"
    print_info "POST ${BASE_URL}/transfer/check-customer-status"
    
    TIMESTAMP=$(date +%s)
    
    curl -s -X POST "${BASE_URL}/transfer/check-customer-status" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"ArrearsCustomer_${TIMESTAMP}\",
            \"custStatus\": \"NORMAL\",
            \"arrearsStatus\": \"ARREARS\"
        }" | jq '.'
    
    print_success "欠费客户状态检查完成"
}

# 12. 执行SWRL推理
test_swrl_reasoning() {
    print_header "测试12: 执行SWRL推理"
    print_info "POST ${BASE_URL}/reasoning/swrl"
    
    curl -s -X POST "${BASE_URL}/reasoning/swrl" \
        -H "Content-Type: application/json" | jq '.'
    
    print_success "SWRL推理执行完成"
}

# 主测试流程
main() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║   BSS4.0 客户过户受理本体服务 - API测试脚本              ║"
    echo "║   Transfer Order Ontology Service Test Script            ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # 检查jq是否安装
    if ! command -v jq &> /dev/null; then
        print_error "jq未安装，请先安装jq用于JSON格式化"
        echo "安装命令: sudo apt-get install jq 或 brew install jq"
        exit 1
    fi
    
    # 检查服务
    check_service
    
    # 执行所有测试
    test_ontology_info
    sleep 1
    
    test_get_classes
    sleep 1
    
    test_get_individuals
    sleep 1
    
    test_get_individuals_by_class
    sleep 1
    
    test_get_individual_properties
    sleep 1
    
    test_add_individual
    sleep 1
    
    test_add_data_property
    sleep 1
    
    test_create_transfer_order
    sleep 1
    
    test_check_normal_customer
    sleep 1
    
    test_check_fraud_customer
    sleep 1
    
    test_check_arrears_customer
    sleep 1
    
    test_swrl_reasoning
    
    # 测试完成
    print_header "测试完成"
    print_success "所有API测试执行完毕！"
    echo ""
}

# 如果有参数，执行指定的测试
if [ $# -eq 0 ]; then
    main
else
    case "$1" in
        health)
            check_service
            ;;
        info)
            test_ontology_info
            ;;
        classes)
            test_get_classes
            ;;
        individuals)
            test_get_individuals
            ;;
        order)
            test_create_transfer_order
            ;;
        reasoning)
            test_swrl_reasoning
            ;;
        *)
            echo "用法: $0 [health|info|classes|individuals|order|reasoning]"
            echo "不带参数运行所有测试"
            exit 1
            ;;
    esac
fi
