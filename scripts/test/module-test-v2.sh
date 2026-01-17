#!/bin/bash

# 模块测试脚本 v2 - 修正API路径
# 测试各个功能模块的API

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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
        print_result "管理员登录" "FAIL" "登录失败"
        return 1
    fi
}

# 检查API响应
check_api() {
    local test_name=$1
    local url=$2
    local method=${3:-"GET"}
    local data=${4:-""}

    local response
    if [ "$method" = "POST" ]; then
        response=$(curl -s -X POST "$url" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s "$url" -H "Authorization: Bearer $TOKEN")
    fi

    local has_success=$(echo "$response" | grep -o '"success":true')

    if [ -n "$has_success" ]; then
        print_result "$test_name" "PASS"
        return 0
    else
        # 提取错误信息
        local error_msg=$(echo "$response" | grep -o '"message":"[^"]*"' | cut -d'"' -f4 | head -1)
        print_result "$test_name" "FAIL" "$error_msg"
        return 1
    fi
}

# 主测试流程
main() {
    echo -e "${BLUE}======================================"
    echo "律师事务所管理系统 - 功能测试"
    echo -e "======================================${NC}\n"

    # 登录
    if ! login; then
        echo -e "${RED}登录失败，无法继续测试${NC}"
        exit 1
    fi

    echo ""
    echo -e "${BLUE}=== 核心模块测试 ===${NC}"

    # 系统管理
    check_api "用户列表" "$BASE_URL/system/user/list?pageNum=1&pageSize=5"
    check_api "部门列表" "$BASE_URL/system/department/list"
    check_api "角色列表" "$BASE_URL/system/role/list"
    check_api "菜单树" "$BASE_URL/system/menu/tree"
    check_api "用户菜单" "$BASE_URL/system/menu/user"
    check_api "字典类型列表" "$BASE_URL/system/dict-type/list"
    check_api "操作日志" "$BASE_URL/system/operation-log/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 案件模块测试 ===${NC}"

    check_api "案件列表" "$BASE_URL/matter/list?pageNum=1&pageSize=5"
    check_api "案件选项" "$BASE_URL/matter/select-options?pageNum=1&pageSize=10"
    check_api "任务列表" "$BASE_URL/tasks?pageNum=1&pageSize=5"
    check_api "我的任务" "$BASE_URL/tasks/my?pageNum=1&pageSize=5"
    check_api "待办任务" "$BASE_URL/tasks/my/todo"
    check_api "期限列表" "$BASE_URL/matter/deadlines/list?pageNum=1&pageSize=5"
    check_api "我的期限" "$BASE_URL/matter/deadlines/my-upcoming?days=7&limit=5"
    check_api "工时列表" "$BASE_URL/matter/timesheets/list?pageNum=1&pageSize=5"
    check_api "进度列表" "$BASE_URL/matter/schedules/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 客户模块测试 ===${NC}"

    check_api "客户列表" "$BASE_URL/client/list?pageNum=1&pageSize=5"
    check_api "客户选项" "$BASE_URL/client/select-options"

    echo ""
    echo -e "${BLUE}=== 文档模块测试 ===${NC}"

    check_api "文档列表" "$BASE_URL/document?pageNum=1&pageSize=5"
    check_api "文档分类树" "$BASE_URL/document/category/tree"
    check_api "文档模板列表" "$BASE_URL/document-template/list?pageNum=1&pageSize=5"
    check_api "印章列表" "$BASE_URL/document/seal/list?pageNum=1&pageSize=5"
    check_api "印章申请" "$BASE_URL/document/seal-application/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 证据模块测试 ===${NC}"

    check_api "证据列表" "$BASE_URL/evidence/list?pageNum=1&pageSize=5"
    check_api "证据目录列表" "$BASE_URL/evidence/evidence-lists?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 财务模块测试 ===${NC}"

    check_api "费用列表" "$BASE_URL/finance/fees/list?pageNum=1&pageSize=5"
    check_api "发票列表" "$BASE_URL/finance/invoices/list?pageNum=1&pageSize=5"
    check_api "预付款列表" "$BASE_URL/finance/prepayments/list?pageNum=1&pageSize=5"
    check_api "报销列表" "$BASE_URL/finance/expenses/list?pageNum=1&pageSize=5"
    check_api "佣金列表" "$BASE_URL/finance/commissions/list?pageNum=1&pageSize=5"
    check_api "工资单列表" "$BASE_URL/finance/payrolls/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 合同模块测试 ===${NC}"

    check_api "合同列表" "$BASE_URL/matter/contracts/list?pageNum=1&pageSize=5"
    check_api "合同模板列表" "$BASE_URL/contract-templates/list?pageNum=1&pageSize=5"
    check_api "案由列表" "$BASE_URL/system/cause-of-action/page?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 卷宗模块测试 ===${NC}"

    check_api "卷宗模板" "$BASE_URL/dossier-templates/list"
    check_api "归档列表" "$BASE_URL/archives/list?pageNum=1&pageSize=5"
    check_api "借阅列表" "$BASE_URL/archive/borrows/list?pageNum=1&pageSize=5"
    check_api "档案位置" "$BASE_URL/archive/locations/list"

    echo ""
    echo -e "${BLUE}=== 知识库模块测试 ===${NC}"

    check_api "法规列表" "$BASE_URL/knowledge/laws/list?pageNum=1&pageSize=5"
    check_api "案例列表" "$BASE_URL/knowledge/cases/list?pageNum=1&pageSize=5"
    check_api "文章列表" "$BASE_URL/knowledge/articles/list?pageNum=1&pageSize=5"
    check_api "质检列表" "$BASE_URL/knowledge/quality-checks/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 人事模块测试 ===${NC}"

    check_api "员工列表" "$BASE_URL/hr/employees/page?pageNum=1&pageSize=5"
    check_api "合同列表" "$BASE_URL/hr/contracts/list?pageNum=1&pageSize=5"
    check_api "请假列表" "$BASE_URL/hr/leave/list?pageNum=1&pageSize=5"
    check_api "考勤列表" "$BASE_URL/hr/attendance/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 行政模块测试 ===${NC}"

    check_api "资产列表" "$BASE_URL/admin/assets/list?pageNum=1&pageSize=5"
    check_api "会议室列表" "$BASE_URL/admin/meeting-rooms/list"
    check_api "用品列表" "$BASE_URL/admin/suppliers/list?pageNum=1&pageSize=5"
    check_api "采购列表" "$BASE_URL/admin/purchases/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 工作台模块测试 ===${NC}"

    check_api "待办列表" "$BASE_URL/workbench/approvals/list?pageNum=1&pageSize=5"
    check_api "我的待办" "$BASE_URL/workbench/approvals/my?pageNum=1&pageSize=5"
    check_api "报告列表" "$BASE_URL/workbench/reports/list?pageNum=1&pageSize=5"

    echo ""
    echo -e "${BLUE}=== 通知公告测试 ===${NC}"

    check_api "通知列表(POST)" "$BASE_URL/system/notifications/query" "POST" '{"pageNum":1,"pageSize":5}'
    check_api "公告列表(POST)" "$BASE_URL/system/announcements/query" "POST" '{"pageNum":1,"pageSize":5}'

    echo ""
    echo -e "${BLUE}=== 系统配置测试 ===${NC}"

    check_api "配置列表(POST)" "$BASE_URL/system/configs/query" "POST" '{"pageNum":1,"pageSize":10}'

    echo ""
    echo -e "${BLUE}=== AI模块测试 ===${NC}"

    check_api "AI使用记录" "$BASE_URL/ai/usage/my"
    check_api "AI使用统计" "$BASE_URL/ai/usage/my/summary"
    check_api "AI使用趋势" "$BASE_URL/ai/usage/my/trend"
    check_api "AI配额" "$BASE_URL/ai/usage/my/quota"
    check_api "全员统计" "$BASE_URL/ai/usage/statistics"
    check_api "部门统计" "$BASE_URL/ai/usage/statistics/department"
    check_api "AI账单" "$BASE_URL/ai/usage/billing"
    check_api "编辑支持检查" "$BASE_URL/ai/usage/my" # 占位

    # 输出测试结果汇总
    echo ""
    echo -e "${BLUE}======================================"
    echo "测试结果汇总"
    echo -e "======================================${NC}"
    echo -e "总计: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"

    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}所有测试通过！${NC}"
        exit 0
    else
        echo -e "\n${YELLOW}有 $FAILED 个测试失败${NC}"
        exit 0
    fi
}

main "$@"
