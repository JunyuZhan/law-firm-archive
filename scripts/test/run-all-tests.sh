#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
# 智慧律所管理系统 - 一键测试脚本
# ═══════════════════════════════════════════════════════════════════════════════
# 功能：整合运行所有测试脚本，生成统一报告
# 用法：
#   ./run-all-tests.sh              # 运行全部测试
#   ./run-all-tests.sh quick        # 仅运行快速测试（API + 集成）
#   ./run-all-tests.sh business     # 仅运行业务逻辑测试
#   ./run-all-tests.sh module       # 仅运行模块测试
# ═══════════════════════════════════════════════════════════════════════════════

set -e

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 测试结果统计
TOTAL_SCRIPTS=0
PASSED_SCRIPTS=0
FAILED_SCRIPTS=0
SKIPPED_SCRIPTS=0

# 失败的脚本列表
declare -a FAILED_SCRIPT_LIST

# 报告文件
REPORT_DIR="$PROJECT_ROOT/docs"
REPORT_FILE="$REPORT_DIR/TEST_REPORT_$(date +%Y%m%d_%H%M%S).md"
TEST_START_TIME=$(date +"%Y-%m-%d %H:%M:%S")

# ═══════════════════════════════════════════════════════════════════════════════
# 工具函数
# ═══════════════════════════════════════════════════════════════════════════════

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
}

print_section() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# 检查服务状态
check_services() {
    print_header "检查服务状态"
    
    # 检查后端服务
    echo -n "检查后端服务 (http://localhost:8080)... "
    if curl -s -f "http://localhost:8080/api/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 运行中${NC}"
    else
        echo -e "${RED}✗ 未运行${NC}"
        echo -e "${RED}请先启动后端服务: cd backend && mvn spring-boot:run${NC}"
        exit 1
    fi
    
    # 检查前端服务（可选）
    echo -n "检查前端服务 (http://localhost:5555)... "
    if curl -s -f "http://localhost:5555" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 运行中${NC}"
    else
        echo -e "${YELLOW}⊘ 未运行（可选）${NC}"
    fi
    
    echo ""
}

# 运行单个测试脚本
run_test_script() {
    local script_name=$1
    local script_path="$SCRIPT_DIR/$script_name"
    
    TOTAL_SCRIPTS=$((TOTAL_SCRIPTS + 1))
    
    if [ ! -f "$script_path" ]; then
        echo -e "${YELLOW}⊘${NC} $script_name: ${YELLOW}跳过（文件不存在）${NC}"
        SKIPPED_SCRIPTS=$((SKIPPED_SCRIPTS + 1))
        return 0
    fi
    
    chmod +x "$script_path"
    
    echo -e "${BLUE}▶${NC} 运行: $script_name"
    
    # 运行脚本并捕获退出码
    set +e
    "$script_path" > /tmp/test_output_$$.log 2>&1
    local exit_code=$?
    set -e
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $script_name: ${GREEN}通过${NC}"
        PASSED_SCRIPTS=$((PASSED_SCRIPTS + 1))
    else
        echo -e "${RED}✗${NC} $script_name: ${RED}失败 (退出码: $exit_code)${NC}"
        FAILED_SCRIPTS=$((FAILED_SCRIPTS + 1))
        FAILED_SCRIPT_LIST+=("$script_name")
        
        # 显示错误日志的最后几行
        echo -e "${YELLOW}  最后输出:${NC}"
        tail -5 /tmp/test_output_$$.log 2>/dev/null | sed 's/^/    /'
    fi
    
    rm -f /tmp/test_output_$$.log
    echo ""
}

# ═══════════════════════════════════════════════════════════════════════════════
# 测试组定义
# ═══════════════════════════════════════════════════════════════════════════════

# 快速测试（API基础测试）
# 推荐用于快速验证系统是否正常
run_quick_tests() {
    print_section "快速测试 - API 基础功能"
    echo -e "${YELLOW}说明: 测试认证、基础API，约2-3分钟${NC}"
    
    run_test_script "api-test.sh"           # 基础API测试（含认证）
    run_test_script "integration-test.sh"   # 综合集成测试
}

# 全面API测试（推荐单独运行）
# 注意：此脚本已包含大部分模块测试，与其他脚本有重复
run_full_api_tests() {
    print_section "全面API测试"
    echo -e "${YELLOW}说明: 完整测试所有模块，约10-15分钟，生成详细报告${NC}"
    echo -e "${YELLOW}注意: 此脚本内容较全面，可替代大部分其他测试${NC}"
    
    run_test_script "full-api-test.sh"
}

# 业务逻辑测试
# 执行顺序：客户 → 项目 → 合同 → 财务 → 证据 → 行政 → 人力
# 原因：后面的模块可能依赖前面模块的基础数据
run_business_tests() {
    print_section "业务逻辑测试"
    echo -e "${YELLOW}说明: 测试各模块业务规则，约15-20分钟${NC}"
    
    # 1. 通用业务逻辑（含基础数据创建）
    run_test_script "business-logic-test.sh"
    
    # 2. 按依赖顺序执行
    run_test_script "client-business-logic-test.sh"      # 客户（基础）
    run_test_script "matter-business-logic-test.sh"      # 项目（依赖客户）
    run_test_script "contract-business-logic-test.sh"    # 合同（依赖客户+项目）
    run_test_script "finance-business-logic-test.sh"     # 财务（依赖合同）
    run_test_script "evidence-business-logic-test.sh"    # 证据（依赖项目）
    run_test_script "admin-business-logic-test.sh"       # 行政（独立）
    run_test_script "hr-business-logic-test.sh"          # 人力（独立）
}

# 模块专项测试
# 执行顺序：权限 → 审批 → 财务 → 其他
run_module_tests() {
    print_section "模块专项测试"
    echo -e "${YELLOW}说明: 测试特定模块深度功能，约10-15分钟${NC}"
    
    # 1. 权限测试（影响其他测试）
    run_test_script "permission-test.sh"
    
    # 2. 审批流程测试
    run_test_script "approval-test.sh"
    run_test_script "approval-full-test.sh"
    
    # 3. 财务专项
    run_test_script "finance-test.sh"
    
    # 4. 其他模块（独立）
    run_test_script "letter-test.sh"
    run_test_script "archive-document-test.sh"
    run_test_script "system-knowledge-test.sh"
    run_test_script "system-report-extended-test.sh"
}

# 辅助模块测试
run_auxiliary_tests() {
    print_section "辅助模块测试"
    echo -e "${YELLOW}说明: 测试边缘功能和补充模块，约5-10分钟${NC}"
    
    run_test_script "auxiliary-module-test.sh"
    run_test_script "edge-module-test.sh"
    run_test_script "remaining-module-test.sh"
}

# ═══════════════════════════════════════════════════════════════════════════════
# 生成测试报告
# ═══════════════════════════════════════════════════════════════════════════════

generate_report() {
    local test_end_time=$(date +"%Y-%m-%d %H:%M:%S")
    local pass_rate=0
    
    if [ $TOTAL_SCRIPTS -gt 0 ]; then
        pass_rate=$((PASSED_SCRIPTS * 100 / TOTAL_SCRIPTS))
    fi
    
    mkdir -p "$REPORT_DIR"
    
    cat > "$REPORT_FILE" << EOF
# 智慧律所管理系统 - 综合测试报告

## 测试概要

| 项目 | 值 |
|------|-----|
| 测试开始时间 | $TEST_START_TIME |
| 测试结束时间 | $test_end_time |
| 测试模式 | ${TEST_MODE:-全部测试} |
| 测试环境 | 本地开发环境 |

## 测试脚本执行结果

| 指标 | 数量 | 说明 |
|------|------|------|
| **总脚本数** | $TOTAL_SCRIPTS | - |
| ✅ **通过** | $PASSED_SCRIPTS | 脚本执行成功 |
| ❌ **失败** | $FAILED_SCRIPTS | 脚本执行失败 |
| ⏭️ **跳过** | $SKIPPED_SCRIPTS | 文件不存在或跳过 |
| **通过率** | ${pass_rate}% | - |

EOF

    if [ ${#FAILED_SCRIPT_LIST[@]} -gt 0 ]; then
        cat >> "$REPORT_FILE" << EOF
## 失败的测试脚本

EOF
        for script in "${FAILED_SCRIPT_LIST[@]}"; do
            echo "- \`$script\`" >> "$REPORT_FILE"
        done
    fi

    cat >> "$REPORT_FILE" << EOF

## 测试结论

EOF

    if [ $FAILED_SCRIPTS -eq 0 ]; then
        cat >> "$REPORT_FILE" << EOF
### ✅ 所有测试通过

系统各模块功能正常，可以进行生产部署。
EOF
    else
        cat >> "$REPORT_FILE" << EOF
### ⚠️ 存在失败项

有 $FAILED_SCRIPTS 个测试脚本执行失败，请检查修复后再部署。
EOF
    fi

    cat >> "$REPORT_FILE" << EOF

---

*报告生成时间: $(date +"%Y-%m-%d %H:%M:%S")*
*测试脚本: scripts/test/run-all-tests.sh*
EOF

    echo -e "${GREEN}测试报告已生成: $REPORT_FILE${NC}"
}

# ═══════════════════════════════════════════════════════════════════════════════
# 打印测试总结
# ═══════════════════════════════════════════════════════════════════════════════

print_summary() {
    print_header "测试总结"
    
    echo "执行脚本数: $TOTAL_SCRIPTS"
    echo -e "${GREEN}通过: $PASSED_SCRIPTS${NC}"
    echo -e "${RED}失败: $FAILED_SCRIPTS${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED_SCRIPTS${NC}"
    
    if [ $TOTAL_SCRIPTS -gt 0 ]; then
        local pass_rate=$((PASSED_SCRIPTS * 100 / TOTAL_SCRIPTS))
        echo "通过率: ${pass_rate}%"
    fi
    
    echo ""
    
    if [ $FAILED_SCRIPTS -eq 0 ]; then
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 所有测试通过！${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${RED}═══════════════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}  ❌ ${FAILED_SCRIPTS} 个测试失败${NC}"
        echo -e "${RED}═══════════════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "失败的脚本:"
        for script in "${FAILED_SCRIPT_LIST[@]}"; do
            echo -e "  ${RED}• $script${NC}"
        done
    fi
}

# ═══════════════════════════════════════════════════════════════════════════════
# 显示帮助
# ═══════════════════════════════════════════════════════════════════════════════

show_help() {
    cat << EOF
智慧律所管理系统 - 一键测试脚本

用法: ./run-all-tests.sh [选项]

选项:
  (无)        运行全部测试（约40-50分钟）
  quick       快速测试（约2-3分钟）⭐ 推荐日常使用
  full        全面API测试（约10-15分钟）⭐ 推荐部署前使用
  business    业务逻辑测试（约15-20分钟）
  module      模块专项测试（约10-15分钟）
  auxiliary   辅助模块测试（约5-10分钟）
  help        显示此帮助信息

推荐使用场景:
  ./run-all-tests.sh quick        # 日常开发，快速验证系统
  ./run-all-tests.sh full         # 部署前，完整测试+报告
  ./run-all-tests.sh business     # 业务功能修改后验证
  ./run-all-tests.sh              # 完整回归测试

执行顺序说明:
  1. quick    : api-test → integration-test
  2. business : 按依赖顺序（客户→项目→合同→财务→证据→行政→人力）
  3. module   : 权限→审批→财务→其他
  4. auxiliary: 边缘功能测试

注意事项:
  - 运行前请确保后端服务已启动 (http://localhost:8080)
  - 测试会创建临时数据并在结束后清理
  - 测试报告生成到 docs/TEST_REPORT_*.md
  - full 模式已涵盖大部分测试，通常无需运行 all
EOF
}

# ═══════════════════════════════════════════════════════════════════════════════
# 主函数
# ═══════════════════════════════════════════════════════════════════════════════

main() {
    local mode="${1:-all}"
    TEST_MODE="$mode"
    
    case "$mode" in
        help|-h|--help)
            show_help
            exit 0
            ;;
    esac
    
    print_header "智慧律所管理系统 - 一键测试"
    echo -e "测试时间: $(date +"%Y-%m-%d %H:%M:%S")"
    echo -e "测试模式: ${PURPLE}$mode${NC}"
    
    # 检查服务
    check_services
    
    # 根据模式运行测试
    case "$mode" in
        quick)
            run_quick_tests
            ;;
        full)
            run_full_api_tests
            ;;
        business)
            run_business_tests
            ;;
        module)
            run_module_tests
            ;;
        auxiliary)
            run_auxiliary_tests
            ;;
        all|*)
            run_quick_tests
            run_business_tests
            run_module_tests
            run_auxiliary_tests
            ;;
    esac
    
    # 打印总结
    print_summary
    
    # 生成报告
    generate_report
    
    # 返回退出码
    if [ $FAILED_SCRIPTS -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# 运行主函数
main "$@"
