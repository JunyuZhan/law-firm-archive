#!/bin/bash

# 模块功能测试脚本 v5 - 修正所有API路径
# 测试各个功能模块的API

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3

    TOTAL=$((TOTAL + 1))

    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}✗${NC} $test_name"
        if [ -n "$message" ]; then
            echo -e "  ${RED}$message${NC}"
        fi
        FAILED=$((FAILED + 1))
    fi
}

# 登录获取token
login() {
    echo -e "${BLUE}=== 登录 ===${NC}"
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')

    local success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2)
    if [ "$success" = "true" ]; then
        TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        print_result "管理员登录" "PASS"
        return 0
    else
        print_result "管理员登录" "FAIL"
        return 1
    fi
}

# 检查API响应
check_api() {
    local test_name=$1
    local url=$2

    local response=$(curl -s "$url" -H "Authorization: Bearer $TOKEN")
    local has_success=$(echo "$response" | grep -o '"success":true')

    if [ -n "$has_success" ]; then
        print_result "$test_name" "PASS"
        return 0
    else
        local error_msg=$(echo "$response" | grep -o '"message":"[^"]*"' | cut -d'"' -f4 | head -1)
        [ -z "$error_msg" ] && error_msg=$(echo "$response" | head -c 100)
        print_result "$test_name" "FAIL" "$error_msg"
        return 1
    fi
}

# 主测试流程
main() {
    echo -e "${BLUE}======================================"
    echo "律师事务所管理系统 - 功能测试 v5"
    echo -e "======================================${NC}\n"

    # 登录
    if ! login; then
        echo -e "${RED}登录失败，无法继续测试${NC}"
        exit 1
    fi

    echo ""
    echo -e "${BLUE}=== 系统管理 (9项) ===${NC}"

    check_api "用户列表" "$BASE_URL/system/user/list?pageNum=1&pageSize=5"
    check_api "部门列表" "$BASE_URL/system/department/list"
    check_api "角色列表" "$BASE_URL/system/role/list"
    check_api "菜单树" "$BASE_URL/system/menu/tree"
    check_api "字典类型" "$BASE_URL/system/dict/types"
    check_api "操作日志" "$BASE_URL/admin/operation-logs?pageNum=1&pageSize=5"
    check_api "登录日志" "$BASE_URL/system/sessions/list?pageNum=1&pageSize=5"
    check_api "数据备份" "$BASE_URL/system/backup/list?pageNum=1&pageSize=5"
    check_api "外部集成" "$BASE_URL/system/integration?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 案件管理 (10项) ===${NC}"

    check_api "案件列表" "$BASE_URL/matter/list?pageNum=1&pageSize=5"
    check_api "案件选项" "$BASE_URL/matter/select-options?pageNum=1&pageSize=10"
    check_api "任务列表" "$BASE_URL/tasks?pageNum=1&pageSize=5"
    check_api "我的任务" "$BASE_URL/tasks/my?pageNum=1&pageSize=5"
    check_api "待办任务" "$BASE_URL/tasks/my/todo"
    check_api "期限列表" "$BASE_URL/matter/deadlines/list?pageNum=1&pageSize=5"
    check_api "我的期限" "$BASE_URL/matter/deadlines/my-upcoming?days=7&limit=5"
    check_api "工时列表" "$BASE_URL/timesheets?pageNum=1&pageSize=5"
    check_api "进度列表" "$BASE_URL/schedules?pageNum=1&pageSize=5"
    check_api "项目合同" "$BASE_URL/matter/contract/list"

    echo ""
    echo -e "${BLUE}=== 客户管理 (4项) ===${NC}"

    check_api "客户列表" "$BASE_URL/client/list?pageNum=1&pageSize=5"
    check_api "客户选项" "$BASE_URL/client/select-options"
    check_api "冲突审查" "$BASE_URL/client/conflict-check/list?pageNum=1&pageSize=5"
    check_api "联系人" "$BASE_URL/client/contact/1"

    echo ""
    echo -e "${BLUE}=== 文档管理 (5项) ===${NC}"

    check_api "文档列表" "$BASE_URL/document?pageNum=1&pageSize=5"
    check_api "文档分类树" "$BASE_URL/document/category/tree"
    check_api "文档模板" "$BASE_URL/document/template?pageNum=1&pageSize=5"
    check_api "印章列表" "$BASE_URL/document/seal?pageNum=1&pageSize=5"
    check_api "印章申请" "$BASE_URL/document/seal-application?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 证据管理 (2项) ===${NC}"

    check_api "证据列表" "$BASE_URL/evidence/list?pageNum=1&pageSize=5"
    check_api "证据目录" "$BASE_URL/evidence/list"

    echo ""
    echo -e "${BLUE}=== 财务管理 (7项) ===${NC}"

    check_api "费用列表" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=5"
    check_api "发票列表" "$BASE_URL/finance/invoice/list?pageNum=1&pageSize=5"
    check_api "预付款列表" "$BASE_URL/finance/prepayment/list?pageNum=1&pageSize=5"
    check_api "报销列表" "$BASE_URL/finance/expense?pageNum=1&pageSize=5"
    check_api "佣金列表" "$BASE_URL/finance/commission?pageNum=1&pageSize=5"
    check_api "工资单列表" "$BASE_URL/hr/payroll?pageNum=1&pageSize=5"
    check_api "付款调整" "$BASE_URL/finance/payment-amendment/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 合同管理 (4项) ===${NC}"

    check_api "合同模板" "$BASE_URL/system/contract-template/list"
    check_api "民事案由树" "$BASE_URL/causes/civil/tree"
    check_api "刑事罪名树" "$BASE_URL/causes/criminal/tree"
    check_api "行政案由树" "$BASE_URL/causes/admin/tree"

    echo ""
    echo -e "${BLUE}=== 卷宗管理 (4项) ===${NC}"

    check_api "卷宗模板" "$BASE_URL/dossier/template"
    check_api "归档列表" "$BASE_URL/archive/list?pageNum=1&pageSize=5"
    check_api "借阅列表" "$BASE_URL/archive/borrow/list?pageNum=1&pageSize=5"
    check_api "档案位置" "$BASE_URL/archive/location/list"

    echo ""
    echo -e "${BLUE}=== 知识库 (5项) ===${NC}"

    check_api "法规列表" "$BASE_URL/knowledge/law?pageNum=1&pageSize=5"
    check_api "案例列表" "$BASE_URL/knowledge/case?pageNum=1&pageSize=5"
    check_api "文章列表" "$BASE_URL/knowledge/article?pageNum=1&pageSize=5"
    check_api "质检列表" "$BASE_URL/knowledge/quality-check?pageNum=1&pageSize=5"
    check_api "风险提示" "$BASE_URL/knowledge/risk-warning?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 人事管理 (6项) ===${NC}"

    check_api "员工列表" "$BASE_URL/hr/employee?pageNum=1&pageSize=5"
    check_api "劳动合同" "$BASE_URL/hr/contract?pageNum=1&pageSize=5"
    check_api "请假列表" "$BASE_URL/admin/leave/applications?pageNum=1&pageSize=5"
    check_api "考勤列表" "$BASE_URL/admin/attendance?pageNum=1&pageSize=5"
    check_api "晋升列表" "$BASE_URL/hr/promotion/applications?pageNum=1&pageSize=5"
    check_api "培训列表" "$BASE_URL/hr/training-notice?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 行政管理 (7项) ===${NC}"

    check_api "资产列表" "$BASE_URL/admin/assets?pageNum=1&pageSize=5"
    check_api "会议室" "$BASE_URL/admin/meeting-room"
    check_api "用品列表" "$BASE_URL/admin/suppliers?pageNum=1&pageSize=5"
    check_api "采购列表" "$BASE_URL/admin/purchases?pageNum=1&pageSize=5"
    check_api "请假申请" "$BASE_URL/admin/leave/applications?pageNum=1&pageSize=5"
    check_api "加班申请" "$BASE_URL/admin/overtime/my"
    check_api "外出申请" "$BASE_URL/admin/go-out/my"

    echo ""
    echo -e "${BLUE}=== 工作台 (5项) ===${NC}"

    check_api "待办列表" "$BASE_URL/workbench/approval/list?pageNum=1&pageSize=5"
    check_api "我的待办" "$BASE_URL/workbench/approval/my?pageNum=1&pageSize=5"
    check_api "统计信息" "$BASE_URL/workbench/data"
    check_api "报告列表" "$BASE_URL/workbench/report/list?pageNum=1&pageSize=5"
    check_api "定时报告" "$BASE_URL/workbench/scheduled-report/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== AI功能模块 (8项) ===${NC}"

    check_api "AI使用记录" "$BASE_URL/ai/usage/my"
    check_api "AI使用统计" "$BASE_URL/ai/usage/my/summary"
    check_api "AI模型分布" "$BASE_URL/ai/usage/my/by-model"
    check_api "AI使用趋势" "$BASE_URL/ai/usage/my/trend"
    check_api "AI配额信息" "$BASE_URL/ai/usage/my/quota"
    check_api "全员统计" "$BASE_URL/ai/usage/statistics"
    check_api "部门统计" "$BASE_URL/ai/usage/statistics/department"
    check_api "AI账单" "$BASE_URL/ai/usage/billing"

    # 输出测试结果汇总
    echo ""
    echo -e "${BLUE}======================================"
    echo "测试结果汇总"
    echo -e "======================================${NC}"
    echo -e "总计: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"

    if [ $TOTAL -gt 0 ]; then
        echo -e "通过率: $(( PASSED * 100 / TOTAL ))%"
    fi

    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}✓ 所有测试通过！${NC}"
        exit 0
    else
        echo -e "\n${YELLOW}有 $FAILED 个测试失败${NC}"
        exit 0
    fi
}

main "$@"
