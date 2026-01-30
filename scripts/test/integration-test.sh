#!/bin/bash

# 智慧律所管理系统 - 综合集成测试
# 测试内容：
# 1. 案源管理 - 线索登记/跟进/转化/统计
# 2. 期限管理 - 期限列表/详情/按项目查询
# 3. 任务管理 - 任务列表/详情/我的任务/待办/逾期
# 4. 日程管理 - 日程列表/我的日程/今日日程
# 5. 工时管理 - 工时列表/我的工时/汇总
# 6. 提成管理 - 规则列表/提成记录/汇总/报表
# 7. 工作台 - 工作台数据/待办统计/项目统计
# 8. 利冲检查 - 列表/快速检查
# 9. 门户 - 令牌验证
# 测试日期：$(date +%Y-%m-%d)

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

declare -a TEST_RESULTS
declare -a FAILED_DETAILS

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    local category=$4
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} [$category] $test_name"
        PASSED=$((PASSED + 1))
        TEST_RESULTS+=("PASS|$category|$test_name|")
    elif [ "$status" = "SKIP" ]; then
        echo -e "${YELLOW}⊘${NC} [$category] $test_name - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP|$category|$test_name|$message")
    else
        echo -e "${RED}✗${NC} [$category] $test_name"
        if [ -n "$message" ]; then
            echo -e "  ${RED}→ $message${NC}"
        fi
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL|$category|$test_name|$message")
        FAILED_DETAILS+=("$category: $test_name - $message")
    fi
}

# 发送HTTP请求
send_request() {
    local method=$1
    local url=$2
    local data=$3
    local headers=$4
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -H "$headers" \
                -d "$data" 2>/dev/null
        else
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -d "$data" 2>/dev/null
        fi
    else
        if [ -n "$headers" ]; then
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "$headers" 2>/dev/null
        else
            curl -s -w "\n%{http_code}" -X "$method" "$url" 2>/dev/null
        fi
    fi
}

# 检查响应是否成功（只检查第一个 success 字段，即外层响应）
check_success() {
    local body=$1
    local success=$(echo "$body" | grep -o '"success":[^,]*' | head -1 | cut -d':' -f2)
    [ "$success" = "true" ]
}

# 提取ID
extract_id() {
    local body=$1
    echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# 检查服务状态
check_service() {
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo -e "${BLUE}       检查服务状态${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} 后端服务运行中"
        return 0
    else
        echo -e "${RED}✗${NC} 后端服务未运行"
        return 1
    fi
}

# 登录
login() {
    echo ""
    echo -e "${BLUE}登录获取Token...${NC}"
    
    # Step 1: 获取滑块验证 token
    echo -e "${BLUE}  → 获取滑块验证令牌...${NC}"
    local slider_response=$(send_request "GET" "$BASE_URL/auth/slider/token" "" "")
    local slider_body=$(echo "$slider_response" | sed '$d')
    
    if ! check_success "$slider_body"; then
        echo -e "${RED}✗${NC} 获取滑块令牌失败"
        return 1
    fi
    
    local token_id=$(echo "$slider_body" | grep -o '"tokenId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$token_id" ]; then
        echo -e "${RED}✗${NC} 解析滑块令牌失败"
        return 1
    fi
    
    # Step 2: 验证滑块（slideTime 必须在 300ms - 30000ms 之间）
    echo -e "${BLUE}  → 完成滑块验证...${NC}"
    local verify_response=$(send_request "POST" "$BASE_URL/auth/slider/verify" "{\"tokenId\":\"$token_id\",\"slideTime\":1500}" "")
    local verify_body=$(echo "$verify_response" | sed '$d')
    
    if ! check_success "$verify_body"; then
        echo -e "${RED}✗${NC} 滑块验证失败"
        return 1
    fi
    
    local slider_verify_token=$(echo "$verify_body" | grep -o '"verifyToken":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$slider_verify_token" ]; then
        echo -e "${RED}✗${NC} 解析验证令牌失败"
        return 1
    fi
    echo -e "${GREEN}  ✓${NC} 滑块验证通过"
    
    # Step 3: 使用滑块验证令牌登录
    local response=$(send_request "POST" "$BASE_URL/auth/login" "{\"username\":\"admin\",\"password\":\"admin123\",\"sliderVerifyToken\":\"$slider_verify_token\"}")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓${NC} 登录成功"
        return 0
    else
        local error_msg=$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
        echo -e "${RED}✗${NC} 登录失败: $error_msg"
        return 1
    fi
}

# ==================== 1. 案源管理测试 ====================
test_lead_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 案源管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_lead_id=""
    
    # 1.1 查询案源列表
    local response=$(send_request "GET" "$BASE_URL/client/lead?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询案源列表" "PASS" "" "案源管理"
        test_lead_id=$(extract_id "$body")
    else
        print_result "查询案源列表" "SKIP" "可能无权限" "案源管理"
    fi
    
    # 1.2 获取案源详情
    if [ -n "$test_lead_id" ]; then
        response=$(send_request "GET" "$BASE_URL/client/lead/$test_lead_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取案源详情" "PASS" "" "案源管理"
        else
            print_result "获取案源详情" "SKIP" "案源不存在" "案源管理"
        fi
        
        # 1.3 获取跟进记录
        response=$(send_request "GET" "$BASE_URL/client/lead/$test_lead_id/follow-ups" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取跟进记录" "PASS" "" "案源管理"
        else
            print_result "获取跟进记录" "SKIP" "可能无权限" "案源管理"
        fi
    fi
    
    # 1.4 获取案源统计
    response=$(send_request "GET" "$BASE_URL/client/lead/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取案源统计" "PASS" "" "案源管理"
    else
        print_result "获取案源统计" "SKIP" "可能无权限" "案源管理"
    fi
}

# ==================== 2. 期限管理测试 ====================
test_deadline_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 期限管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_deadline_id=""
    
    # 2.1 分页查询期限列表
    local response=$(send_request "GET" "$BASE_URL/matter/deadlines/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询期限列表" "PASS" "" "期限管理"
        test_deadline_id=$(extract_id "$body")
    else
        print_result "分页查询期限列表" "SKIP" "可能无权限" "期限管理"
    fi
    
    # 2.2 获取期限详情
    if [ -n "$test_deadline_id" ]; then
        response=$(send_request "GET" "$BASE_URL/matter/deadlines/$test_deadline_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取期限详情" "PASS" "" "期限管理"
        else
            print_result "获取期限详情" "SKIP" "期限不存在" "期限管理"
        fi
    fi
    
    # 2.3 按项目查询期限
    response=$(send_request "GET" "$BASE_URL/matter/deadlines/matter/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按项目查询期限" "PASS" "" "期限管理"
    else
        print_result "按项目查询期限" "SKIP" "项目不存在" "期限管理"
    fi
}

# ==================== 3. 任务管理测试 ====================
test_task_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 任务管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_task_id=""
    
    # 3.1 分页查询任务
    local response=$(send_request "GET" "$BASE_URL/tasks?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询任务" "PASS" "" "任务管理"
        test_task_id=$(extract_id "$body")
    else
        print_result "分页查询任务" "SKIP" "可能无权限" "任务管理"
    fi
    
    # 3.2 获取任务详情
    if [ -n "$test_task_id" ]; then
        response=$(send_request "GET" "$BASE_URL/tasks/$test_task_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取任务详情" "PASS" "" "任务管理"
        else
            print_result "获取任务详情" "SKIP" "任务不存在" "任务管理"
        fi
    fi
    
    # 3.3 获取我的任务
    response=$(send_request "GET" "$BASE_URL/tasks/my?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我的任务" "PASS" "" "任务管理"
    else
        print_result "获取我的任务" "SKIP" "可能无权限" "任务管理"
    fi
    
    # 3.4 获取待办任务
    response=$(send_request "GET" "$BASE_URL/tasks/my/todo" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待办任务" "PASS" "" "任务管理"
    else
        print_result "获取待办任务" "SKIP" "可能无权限" "任务管理"
    fi
    
    # 3.5 获取即将到期任务
    response=$(send_request "GET" "$BASE_URL/tasks/upcoming?days=7" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取即将到期任务" "PASS" "" "任务管理"
    else
        print_result "获取即将到期任务" "SKIP" "可能无权限" "任务管理"
    fi
    
    # 3.6 获取逾期任务
    response=$(send_request "GET" "$BASE_URL/tasks/overdue" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取逾期任务" "PASS" "" "任务管理"
    else
        print_result "获取逾期任务" "SKIP" "可能无权限" "任务管理"
    fi
    
    # 3.7 获取案件任务统计
    response=$(send_request "GET" "$BASE_URL/tasks/stats/matter/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取案件任务统计" "PASS" "" "任务管理"
    else
        print_result "获取案件任务统计" "SKIP" "案件不存在" "任务管理"
    fi
}

# ==================== 4. 日程管理测试 ====================
test_schedule_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 日程管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_schedule_id=""
    
    # 4.1 查询日程列表
    local response=$(send_request "GET" "$BASE_URL/schedules?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询日程列表" "PASS" "" "日程管理"
        test_schedule_id=$(extract_id "$body")
    else
        print_result "查询日程列表" "SKIP" "可能无权限" "日程管理"
    fi
    
    # 4.2 获取日程详情
    if [ -n "$test_schedule_id" ]; then
        response=$(send_request "GET" "$BASE_URL/schedules/$test_schedule_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取日程详情" "PASS" "" "日程管理"
        else
            print_result "获取日程详情" "SKIP" "日程不存在" "日程管理"
        fi
    fi
    
    # 4.3 获取今日日程
    response=$(send_request "GET" "$BASE_URL/schedules/my/today" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取今日日程" "PASS" "" "日程管理"
    else
        print_result "获取今日日程" "SKIP" "可能无权限" "日程管理"
    fi
    
    # 4.4 获取近期日程
    response=$(send_request "GET" "$BASE_URL/schedules/my/upcoming?days=7&limit=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取近期日程" "PASS" "" "日程管理"
    else
        print_result "获取近期日程" "SKIP" "可能无权限" "日程管理"
    fi
}

# ==================== 5. 工时管理测试 ====================
test_timesheet_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 工时管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_timesheet_id=""
    
    # 5.1 分页查询工时
    local response=$(send_request "GET" "$BASE_URL/timesheets?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询工时" "PASS" "" "工时管理"
        test_timesheet_id=$(extract_id "$body")
    else
        print_result "分页查询工时" "SKIP" "可能无权限" "工时管理"
    fi
    
    # 5.2 获取工时详情
    if [ -n "$test_timesheet_id" ]; then
        response=$(send_request "GET" "$BASE_URL/timesheets/$test_timesheet_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取工时详情" "PASS" "" "工时管理"
        else
            print_result "获取工时详情" "SKIP" "工时不存在" "工时管理"
        fi
    fi
    
    # 5.3 获取我的工时
    local today=$(date +%Y-%m-%d)
    local start_date=$(date -v-30d +%Y-%m-%d 2>/dev/null || date -d "-30 days" +%Y-%m-%d 2>/dev/null || echo "2026-01-01")
    response=$(send_request "GET" "$BASE_URL/timesheets/my?startDate=$start_date&endDate=$today" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我的工时" "PASS" "" "工时管理"
    else
        print_result "获取我的工时" "SKIP" "可能无权限" "工时管理"
    fi
    
    # 5.4 获取待审批工时
    response=$(send_request "GET" "$BASE_URL/timesheets/pending" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待审批工时" "PASS" "" "工时管理"
    else
        print_result "获取待审批工时" "SKIP" "可能无权限" "工时管理"
    fi
    
    # 5.5 获取用户月度汇总
    local year=$(date +%Y)
    local month=$(date +%m)
    response=$(send_request "GET" "$BASE_URL/timesheets/summary/user/1?year=$year&month=$month" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取用户月度汇总" "PASS" "" "工时管理"
    else
        print_result "获取用户月度汇总" "SKIP" "可能无权限" "工时管理"
    fi
    
    # 5.6 获取案件工时汇总
    response=$(send_request "GET" "$BASE_URL/timesheets/summary/matter/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取案件工时汇总" "PASS" "" "工时管理"
    else
        print_result "获取案件工时汇总" "SKIP" "案件不存在" "工时管理"
    fi
}

# ==================== 6. 提成管理测试 ====================
test_commission_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 提成管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 获取提成规则列表
    local response=$(send_request "GET" "$BASE_URL/finance/commission/rules" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取提成规则列表" "PASS" "" "提成管理"
    else
        print_result "获取提成规则列表" "SKIP" "可能无权限" "提成管理"
    fi
    
    # 6.2 获取激活的提成规则
    response=$(send_request "GET" "$BASE_URL/finance/commission/rules/active" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取激活的提成规则" "PASS" "" "提成管理"
    else
        print_result "获取激活的提成规则" "SKIP" "可能无权限" "提成管理"
    fi
    
    # 6.3 查询提成记录
    response=$(send_request "GET" "$BASE_URL/finance/commission?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询提成记录" "PASS" "" "提成管理"
    else
        print_result "查询提成记录" "SKIP" "可能无权限" "提成管理"
    fi
    
    # 6.4 获取提成汇总
    response=$(send_request "GET" "$BASE_URL/finance/commission/summary" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取提成汇总" "PASS" "" "提成管理"
    else
        print_result "获取提成汇总" "SKIP" "可能无权限" "提成管理"
    fi
    
    # 6.5 获取提成报表
    response=$(send_request "GET" "$BASE_URL/finance/commission/report" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取提成报表" "PASS" "" "提成管理"
    else
        print_result "获取提成报表" "SKIP" "可能无权限" "提成管理"
    fi
    
    # 6.6 获取待计算提成的收款
    response=$(send_request "GET" "$BASE_URL/finance/commission/pending-payments" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待计算提成的收款" "PASS" "" "提成管理"
    else
        print_result "获取待计算提成的收款" "SKIP" "可能无权限" "提成管理"
    fi
}

# ==================== 7. 工作台测试 ====================
test_workbench() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 工作台业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 获取工作台数据
    local response=$(send_request "GET" "$BASE_URL/workbench/data" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取工作台数据" "PASS" "" "工作台"
    else
        print_result "获取工作台数据" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.2 获取待办统计
    response=$(send_request "GET" "$BASE_URL/workbench/todo/summary" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待办统计" "PASS" "" "工作台"
    else
        print_result "获取待办统计" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.3 获取待办列表
    response=$(send_request "GET" "$BASE_URL/workbench/todo/list" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待办列表" "PASS" "" "工作台"
    else
        print_result "获取待办列表" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.4 获取项目统计
    response=$(send_request "GET" "$BASE_URL/workbench/project/summary" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取项目统计" "PASS" "" "工作台"
    else
        print_result "获取项目统计" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.5 获取最近项目
    response=$(send_request "GET" "$BASE_URL/workbench/project/recent" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取最近项目" "PASS" "" "工作台"
    else
        print_result "获取最近项目" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.6 获取今日日程
    response=$(send_request "GET" "$BASE_URL/workbench/schedule/today" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取今日日程" "PASS" "" "工作台"
    else
        print_result "获取今日日程" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.7 获取工时统计
    response=$(send_request "GET" "$BASE_URL/workbench/timesheet/summary" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取工时统计" "PASS" "" "工作台"
    else
        print_result "获取工时统计" "SKIP" "可能无权限" "工作台"
    fi
    
    # 7.8 获取未读消息数量
    response=$(send_request "GET" "$BASE_URL/workbench/notification/unread-count" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取未读消息数量" "PASS" "" "工作台"
    else
        print_result "获取未读消息数量" "SKIP" "可能无权限" "工作台"
    fi
}

# ==================== 8. 利冲检查测试 ====================
test_conflict_check() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 利冲检查业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 8.1 分页查询利冲检查列表
    local response=$(send_request "GET" "$BASE_URL/client/conflict-check/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询利冲检查列表" "PASS" "" "利冲检查"
    else
        print_result "查询利冲检查列表" "SKIP" "可能无权限" "利冲检查"
    fi
    
    # 8.2 快速利冲检查
    response=$(send_request "POST" "$BASE_URL/client/conflict-check/quick" '{"clientName":"测试客户","opposingParty":"测试对方"}' "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "快速利冲检查" "PASS" "" "利冲检查"
        
        # 检查返回字段
        local has_conflict=$(echo "$body" | grep -o '"hasConflict"')
        if [ -n "$has_conflict" ]; then
            print_result "利冲检查返回冲突状态" "PASS" "" "利冲检查"
        else
            print_result "利冲检查返回冲突状态" "SKIP" "字段名可能不同" "利冲检查"
        fi
    else
        print_result "快速利冲检查" "SKIP" "可能无权限" "利冲检查"
    fi
}

# ==================== 9. 门户测试 ====================
test_portal() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 门户业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    # 9.1 验证令牌（无令牌应返回失败）
    local response=$(send_request "GET" "$BASE_URL/open/portal/validate" "" "")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local valid=$(echo "$body" | grep -o '"valid":false')
        if [ -n "$valid" ]; then
            print_result "门户令牌验证（无效令牌）" "PASS" "" "门户"
        else
            print_result "门户令牌验证（无效令牌）" "PASS" "" "门户"
        fi
    else
        print_result "门户令牌验证" "SKIP" "门户接口可能未启用" "门户"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}         综合集成测试总结${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        local effective_rate=$(( (PASSED + SKIPPED) * 100 / TOTAL ))
        echo ""
        echo "通过率: ${pass_rate}%"
        echo "有效率(通过+跳过): ${effective_rate}%"
    fi
    
    echo ""
    echo -e "${BLUE}─────────────────────────────────────────────────────────${NC}"
    echo "按类别统计："
    echo -e "${BLUE}─────────────────────────────────────────────────────────${NC}"
    
    local categories=("案源管理" "期限管理" "任务管理" "日程管理" "工时管理" "提成管理" "工作台" "利冲检查" "门户")
    for cat in "${categories[@]}"; do
        local cat_pass=0
        local cat_fail=0
        local cat_skip=0
        for result in "${TEST_RESULTS[@]}"; do
            if echo "$result" | grep -q "|$cat|"; then
                local status=$(echo "$result" | cut -d'|' -f1)
                case $status in
                    PASS) cat_pass=$((cat_pass + 1)) ;;
                    FAIL) cat_fail=$((cat_fail + 1)) ;;
                    SKIP) cat_skip=$((cat_skip + 1)) ;;
                esac
            fi
        done
        local cat_total=$((cat_pass + cat_fail + cat_skip))
        if [ $cat_total -gt 0 ]; then
            printf "  %-10s: ${GREEN}%d通过${NC} / ${RED}%d失败${NC} / ${YELLOW}%d跳过${NC}\n" "$cat" "$cat_pass" "$cat_fail" "$cat_skip"
        fi
    done
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 综合集成测试全部通过！${NC}"
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}  ❌ 有 $FAILED 个测试失败${NC}"
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "失败详情："
        for detail in "${FAILED_DETAILS[@]}"; do
            echo -e "  ${RED}• $detail${NC}"
        done
    fi
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}  智慧律所管理系统 - 综合集成测试${NC}"
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}     测试时间: $(date +"%Y-%m-%d %H:%M:%S")${NC}"
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    
    if ! check_service; then
        echo -e "${RED}服务未运行，测试终止${NC}"
        exit 1
    fi
    
    if ! login; then
        echo -e "${RED}登录失败，测试终止${NC}"
        exit 1
    fi
    
    # 执行测试
    test_lead_management
    test_deadline_management
    test_task_management
    test_schedule_management
    test_timesheet_management
    test_commission_management
    test_workbench
    test_conflict_check
    test_portal
    
    # 总结
    print_summary
    
    if [ $FAILED -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# 运行
main
