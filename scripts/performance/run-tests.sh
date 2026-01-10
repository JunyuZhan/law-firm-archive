#!/bin/bash
# =====================================================
# 律师事务所系统 - 压力测试执行脚本
# =====================================================
# 版本: 1.0.0
# 日期: 2026-01-10
# 作者: Kiro-1
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认配置
BASE_URL="${BASE_URL:-localhost}"
PORT="${PORT:-8080}"
JMETER_HOME="${JMETER_HOME:-/usr/local/bin}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JMX_DIR="${SCRIPT_DIR}/jmx"
REPORT_DIR="${SCRIPT_DIR}/reports/$(date +%Y%m%d_%H%M%S)"

# 打印帮助
print_help() {
    echo "用法: $0 [选项] <测试类型>"
    echo ""
    echo "测试类型:"
    echo "  login          登录接口压测"
    echo "  matter-list    项目列表压测"
    echo "  matter-create  创建项目压测"
    echo "  file-upload    文件上传压测"
    echo "  all            执行所有测试"
    echo ""
    echo "选项:"
    echo "  -t, --threads NUM    并发线程数 (默认: 根据测试类型)"
    echo "  -l, --loops NUM      循环次数 (默认: 根据测试类型)"
    echo "  -r, --rampup NUM     Ramp-Up 时间/秒 (默认: 根据测试类型)"
    echo "  -h, --help           显示此帮助"
    echo ""
    echo "示例:"
    echo "  $0 login                        # 使用默认参数执行登录测试"
    echo "  $0 -t 50 -l 100 login          # 50并发，100次循环"
    echo "  $0 all                          # 执行所有测试"
}

# 检查 JMeter 是否安装
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        echo -e "${RED}错误: JMeter 未安装${NC}"
        echo "请安装 JMeter: brew install jmeter"
        exit 1
    fi
    echo -e "${GREEN}✓ JMeter 已安装${NC}"
}

# 检查服务是否运行
check_service() {
    echo "检查服务状态: http://${BASE_URL}:${PORT}/actuator/health"
    if curl -s "http://${BASE_URL}:${PORT}/actuator/health" | grep -q "UP"; then
        echo -e "${GREEN}✓ 服务运行正常${NC}"
    else
        echo -e "${YELLOW}⚠ 服务可能未启动，请确认服务地址: http://${BASE_URL}:${PORT}${NC}"
        read -p "是否继续? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 执行测试
run_test() {
    local test_name=$1
    local jmx_file=$2
    local threads=${3:-100}
    local loops=${4:-100}
    local rampup=${5:-30}
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}执行测试: ${test_name}${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "配置: 线程=${threads}, 循环=${loops}, Ramp-Up=${rampup}s"
    echo ""
    
    # 创建报告目录
    local test_report_dir="${REPORT_DIR}/${test_name}"
    mkdir -p "${test_report_dir}"
    
    # 执行 JMeter 测试
    jmeter -n -t "${jmx_file}" \
        -l "${test_report_dir}/results.jtl" \
        -Jthreads=${threads} \
        -Jloops=${loops} \
        -Jrampup=${rampup} \
        -JBASE_URL=${BASE_URL} \
        -JPORT=${PORT} \
        -e -o "${test_report_dir}/html"
    
    echo ""
    echo -e "${GREEN}测试完成: ${test_name}${NC}"
    echo "报告位置: ${test_report_dir}/html/index.html"
}

# 主函数
main() {
    local threads=""
    local loops=""
    local rampup=""
    local test_type=""
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--threads)
                threads="$2"
                shift 2
                ;;
            -l|--loops)
                loops="$2"
                shift 2
                ;;
            -r|--rampup)
                rampup="$2"
                shift 2
                ;;
            -h|--help)
                print_help
                exit 0
                ;;
            *)
                test_type="$1"
                shift
                ;;
        esac
    done
    
    # 检查测试类型
    if [ -z "$test_type" ]; then
        print_help
        exit 1
    fi
    
    # 前置检查
    check_jmeter
    check_service
    
    # 创建报告目录
    mkdir -p "${REPORT_DIR}"
    echo "报告目录: ${REPORT_DIR}"
    
    # 执行测试
    case $test_type in
        login)
            run_test "login" "${JMX_DIR}/login-test.jmx" \
                "${threads:-100}" "${loops:-100}" "${rampup:-30}"
            ;;
        matter-list)
            run_test "matter-list" "${JMX_DIR}/matter-list-test.jmx" \
                "${threads:-50}" "${loops:-200}" "${rampup:-20}"
            ;;
        matter-create)
            echo -e "${YELLOW}创建项目测试脚本待完善${NC}"
            ;;
        file-upload)
            echo -e "${YELLOW}文件上传测试脚本待完善${NC}"
            ;;
        all)
            run_test "login" "${JMX_DIR}/login-test.jmx" \
                "${threads:-100}" "${loops:-100}" "${rampup:-30}"
            run_test "matter-list" "${JMX_DIR}/matter-list-test.jmx" \
                "${threads:-50}" "${loops:-200}" "${rampup:-20}"
            ;;
        *)
            echo -e "${RED}未知测试类型: ${test_type}${NC}"
            print_help
            exit 1
            ;;
    esac
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}所有测试执行完成${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "完整报告: ${REPORT_DIR}"
}

main "$@"

