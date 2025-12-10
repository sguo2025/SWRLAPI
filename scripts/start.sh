#!/bin/bash

###############################################################################
# BSS4.0 客户过户受理本体服务 - 快速启动脚本
###############################################################################

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
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

# 检查Java版本
check_java() {
    print_header "检查Java环境"
    
    if ! command -v java &> /dev/null; then
        print_error "Java未安装，请先安装JDK 21"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java版本过低，当前版本: $JAVA_VERSION，需要版本: 21+"
        exit 1
    fi
    
    print_success "Java版本检查通过: $(java -version 2>&1 | head -n 1)"
}

# 检查Maven
check_maven() {
    print_header "检查Maven环境"
    
    if ! command -v mvn &> /dev/null; then
        print_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    print_success "Maven检查通过: $(mvn -version | head -n 1)"
}

# 编译项目
build_project() {
    print_header "编译项目"
    print_info "执行: mvn clean package -DskipTests"
    
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        print_success "项目编译成功"
    else
        print_error "项目编译失败"
        exit 1
    fi
}

# 启动服务
start_service() {
    print_header "启动服务"
    
    JAR_FILE="target/transfer-order-ontology-1.0.0.jar"
    
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR文件不存在: $JAR_FILE"
        print_info "请先执行编译: ./start.sh build"
        exit 1
    fi
    
    print_info "启动Spring Boot应用..."
    echo ""
    echo -e "${YELLOW}服务启动中，请等待...${NC}"
    echo -e "${YELLOW}访问地址: http://localhost:8080/ontology/api/health${NC}"
    echo -e "${YELLOW}按 Ctrl+C 停止服务${NC}"
    echo ""
    
    java -jar "$JAR_FILE"
}

# 运行开发模式
dev_mode() {
    print_header "开发模式启动"
    print_info "执行: mvn spring-boot:run"
    
    mvn spring-boot:run
}

# 显示帮助
show_help() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║   BSS4.0 客户过户受理本体服务 - 启动脚本                 ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo "用法: ./start.sh [command]"
    echo ""
    echo "命令:"
    echo "  check     - 检查环境（Java、Maven）"
    echo "  build     - 编译项目"
    echo "  start     - 启动服务（需先编译）"
    echo "  dev       - 开发模式启动（Maven方式）"
    echo "  all       - 完整流程：检查+编译+启动"
    echo "  help      - 显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  ./start.sh all      # 一键启动（推荐）"
    echo "  ./start.sh build    # 仅编译"
    echo "  ./start.sh dev      # 开发模式"
    echo ""
}

# 完整流程
run_all() {
    check_java
    check_maven
    build_project
    start_service
}

# 主程序
main() {
    case "${1:-all}" in
        check)
            check_java
            check_maven
            ;;
        build)
            check_java
            check_maven
            build_project
            ;;
        start)
            start_service
            ;;
        dev)
            check_java
            check_maven
            dev_mode
            ;;
        all)
            run_all
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知命令: $1"
            show_help
            exit 1
            ;;
    esac
}

# 切换到项目目录
cd "$(dirname "$0")"

# 执行主程序
main "$@"
