#!/bin/bash
# JMeter 压力测试批量执行脚本
# 律师事务所管理系统
# 作者: junyuzhan @ 2026-01-10

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 默认配置
SERVER_HOST="${SERVER_HOST:-localhost}"
SERVER_PORT="${SERVER_PORT:-8080}"
SERVER_PROTOCOL="${SERVER_PROTOCOL:-http}"
TEST_USERNAME="${TEST_USERNAME:-admin}"
TEST_PASSWORD="${TEST_PASSWORD:-admin123}"

# 打印帮助信息
print_help() {
    echo -e "${BLUE}律师事务所管理系统 - JMeter 压力测试脚本${NC}"
    echo ""
    echo "用法: $0 [选项] [测试类型]"
    echo ""
    echo "测试类型:"
    echo "  login     - 登录压力测试 (100并发, 5分钟)"
    echo "  matter    - 项目管理压力测试 (50并发, 5分钟)"
    echo "  client    - 客户管理压力测试 (50并发, 5分钟)"
    echo "  upload    - 文件上传压力测试 (10并发, 5分钟)"
    echo "  full      - 综合压力测试 (100并发, 10分钟)"
    echo "  all       - 运行所有测试"
    echo ""
    echo "选项:"
    echo "  -h, --host HOST       服务器地址 (默认: localhost)"
    echo "  -p, --port PORT       服务器端口 (默认: 8080)"
    echo "  -u, --user USERNAME   测试用户名 (默认: admin)"
    echo "  -P, --password PASS   测试密码 (默认: admin123)"
    echo "  -g, --gui             使用 GUI 模式运行"
    echo "  --help                显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 login                          # 运行登录测试"
    echo "  $0 -h 192.168.1.100 -p 8080 all   # 指定服务器运行所有测试"
    echo "  $0 -g matter                      # GUI 模式运行项目测试"
}

# 检查 JMeter 是否安装
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        echo -e "${RED}错误: JMeter 未安装或不在 PATH 中${NC}"
        echo "请安装 JMeter: brew install jmeter"
        exit 1
    fi
    echo -e "${GREEN}✓ JMeter 已安装${NC}"
}

# 创建结果目录
create_results_dir() {
    mkdir -p "${RESULTS_DIR}/${TIMESTAMP}"
    echo -e "${GREEN}✓ 结果目录: ${RESULTS_DIR}/${TIMESTAMP}${NC}"
}

# 运行单个测试
run_test() {
    local test_name=$1
    local test_file=$2
    local gui_mode=$3
    
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}运行测试: ${test_name}${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    local result_file="${RESULTS_DIR}/${TIMESTAMP}/${test_name}-result.jtl"
    local report_dir="${RESULTS_DIR}/${TIMESTAMP}/${test_name}-report"
    
    if [ "$gui_mode" = "true" ]; then
        echo -e "${YELLOW}以 GUI 模式启动...${NC}"
        jmeter -t "${SCRIPT_DIR}/${test_file}" \
            -Jserver.host="${SERVER_HOST}" \
            -Jserver.port="${SERVER_PORT}" \
            -Jserver.protocol="${SERVER_PROTOCOL}" \
            -Jtest.username="${TEST_USERNAME}" \
            -Jtest.password="${TEST_PASSWORD}"
    else
        echo -e "${YELLOW}以命令行模式运行...${NC}"
        echo "服务器: ${SERVER_PROTOCOL}://${SERVER_HOST}:${SERVER_PORT}"
        echo "用户: ${TEST_USERNAME}"
        echo ""
        
        jmeter -n -t "${SCRIPT_DIR}/${test_file}" \
            -l "${result_file}" \
            -e -o "${report_dir}" \
            -Jserver.host="${SERVER_HOST}" \
            -Jserver.port="${SERVER_PORT}" \
            -Jserver.protocol="${SERVER_PROTOCOL}" \
            -Jtest.username="${TEST_USERNAME}" \
            -Jtest.password="${TEST_PASSWORD}"
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 测试完成: ${test_name}${NC}"
            echo -e "${GREEN}  结果文件: ${result_file}${NC}"
            echo -e "${GREEN}  HTML报告: ${report_dir}/index.html${NC}"
        else
            echo -e "${RED}✗ 测试失败: ${test_name}${NC}"
            return 1
        fi
    fi
}

# 打印测试摘要
print_summary() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}测试完成摘要${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "结果目录: ${RESULTS_DIR}/${TIMESTAMP}"
    echo ""
    echo "查看 HTML 报告:"
    for report in "${RESULTS_DIR}/${TIMESTAMP}"/*-report; do
        if [ -d "$report" ]; then
            echo "  - $(basename $report): file://${report}/index.html"
        fi
    done
}

# 主函数
main() {
    local gui_mode="false"
    local test_type=""
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                SERVER_HOST="$2"
                shift 2
                ;;
            -p|--port)
                SERVER_PORT="$2"
                shift 2
                ;;
            -u|--user)
                TEST_USERNAME="$2"
                shift 2
                ;;
            -P|--password)
                TEST_PASSWORD="$2"
                shift 2
                ;;
            -g|--gui)
                gui_mode="true"
                shift
                ;;
            --help)
                print_help
                exit 0
                ;;
            login|matter|client|upload|full|all)
                test_type="$1"
                shift
                ;;
            *)
                echo -e "${RED}未知参数: $1${NC}"
                print_help
                exit 1
                ;;
        esac
    done
    
    # 检查测试类型
    if [ -z "$test_type" ]; then
        print_help
        exit 1
    fi
    
    # 检查环境
    check_jmeter
    create_results_dir
    
    echo ""
    echo -e "${BLUE}测试配置:${NC}"
    echo "  服务器: ${SERVER_PROTOCOL}://${SERVER_HOST}:${SERVER_PORT}"
    echo "  用户: ${TEST_USERNAME}"
    echo "  GUI模式: ${gui_mode}"
    echo ""
    
    # 运行测试
    case $test_type in
        login)
            run_test "login" "login-stress-test.jmx" "$gui_mode"
            ;;
        matter)
            run_test "matter" "matter-stress-test.jmx" "$gui_mode"
            ;;
        client)
            run_test "client" "client-stress-test.jmx" "$gui_mode"
            ;;
        upload)
            run_test "upload" "file-upload-stress-test.jmx" "$gui_mode"
            ;;
        full)
            run_test "full" "full-stress-test.jmx" "$gui_mode"
            ;;
        all)
            if [ "$gui_mode" = "true" ]; then
                echo -e "${RED}错误: GUI 模式不支持运行所有测试${NC}"
                exit 1
            fi
            run_test "login" "login-stress-test.jmx" "$gui_mode"
            run_test "matter" "matter-stress-test.jmx" "$gui_mode"
            run_test "client" "client-stress-test.jmx" "$gui_mode"
            run_test "full" "full-stress-test.jmx" "$gui_mode"
            print_summary
            ;;
    esac
    
    if [ "$gui_mode" != "true" ] && [ "$test_type" != "all" ]; then
        print_summary
    fi
}

main "$@"
