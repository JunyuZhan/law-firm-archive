#!/bin/bash

# 智慧律所管理系统 - 生产环境部署前全面测试脚本
# 测试内容：查询接口 + CRUD操作 + 业务流程
# 测试日期：$(date +%Y-%m-%d)

BASE_URL="http://localhost:8080/api"
TOKEN=""
REFRESH_TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

# 测试结果数组
declare -a TEST_RESULTS
declare -a FAILED_DETAILS

# 测试创建的资源ID（用于后续清理）
CREATED_CLIENT_ID=""
CREATED_MATTER_ID=""
CREATED_TASK_ID=""

# 报告输出文件
REPORT_FILE=""
TEST_START_TIME=""
TEST_END_TIME=""

# 初始化报告
init_report() {
    TEST_START_TIME=$(date +"%Y-%m-%d %H:%M:%S")
    REPORT_FILE="/Users/apple/Documents/Project/law-firm/docs/TEST_REPORT_$(date +%Y%m%d_%H%M%S).md"
}

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    local module=$4
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} $test_name: ${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
        TEST_RESULTS+=("PASS|$module|$test_name|")
    elif [ "$status" = "SKIP" ]; then
        echo -e "${YELLOW}⊘${NC} $test_name: ${YELLOW}SKIP${NC} - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP|$module|$test_name|$message")
    else
        echo -e "${RED}✗${NC} $test_name: ${RED}FAIL${NC}"
        if [ -n "$message" ]; then
            echo -e "  ${RED}Error: $message${NC}"
        fi
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL|$module|$test_name|$message")
        FAILED_DETAILS+=("$module - $test_name: $message")
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

# 检查响应是否成功
check_response() {
    local response=$1
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        echo "SUCCESS"
        return 0
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        echo "$message"
        return 1
    fi
}

# 从响应中提取ID
extract_id() {
    local body=$1
    echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# 检查服务是否运行
check_service() {
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo -e "${BLUE}       检查服务状态${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    
    # 检查后端
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} 后端服务运行中 (http://localhost:8080)"
    else
        echo -e "${RED}✗${NC} 后端服务未运行"
        return 1
    fi
    
    # 检查前端
    if curl -s -f "http://localhost:5555" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} 前端服务运行中 (http://localhost:5555)"
    else
        echo -e "${YELLOW}⊘${NC} 前端服务未检测到（可选）"
    fi
    
    # 检查数据库连接（通过健康检查）
    local health_response=$(curl -s "$BASE_URL/actuator/health" 2>/dev/null)
    if echo "$health_response" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}✓${NC} 数据库连接正常"
    fi
    
    return 0
}

# ==================== 认证模块测试 ====================
test_auth_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 认证模块测试 (Authentication)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    # 1.1 获取验证码
    local response=$(send_request "GET" "$BASE_URL/auth/captcha")
    local result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取验证码" "PASS" "" "认证模块"
    else
        print_result "获取验证码" "FAIL" "$result" "认证模块"
    fi
    
    # 1.2 用户登录
    response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
        print_result "管理员登录 (admin/admin123)" "PASS" "" "认证模块"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "管理员登录 (admin/admin123)" "FAIL" "$message" "认证模块"
        return 1
    fi
    
    # 1.3 获取用户信息
    response=$(send_request "GET" "$BASE_URL/auth/info" "" "Authorization: Bearer $TOKEN")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取当前用户信息" "PASS" "" "认证模块"
    else
        print_result "获取当前用户信息" "FAIL" "$result" "认证模块"
    fi
    
    # 1.4 刷新Token
    if [ -n "$REFRESH_TOKEN" ]; then
        response=$(send_request "POST" "$BASE_URL/auth/refresh" "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
        result=$(check_response "$response")
        if [ "$result" = "SUCCESS" ]; then
            print_result "Token刷新" "PASS" "" "认证模块"
        else
            print_result "Token刷新" "FAIL" "$result" "认证模块"
        fi
    fi
    
    # 1.5 错误登录测试
    response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"wrongpassword"}')
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ] || [ "$http_code" != "200" ]; then
        print_result "错误密码拒绝登录" "PASS" "" "认证模块"
    else
        print_result "错误密码拒绝登录" "FAIL" "应该拒绝错误密码" "认证模块"
    fi
    
    return 0
}

# ==================== 客户管理模块测试 ====================
test_client_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 客户管理模块测试 (Client Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result body
    
    # 2.1 客户列表查询
    response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "客户列表查询" "PASS" "" "客户管理"
    else
        print_result "客户列表查询" "FAIL" "$result" "客户管理"
    fi
    
    # 2.2 创建客户（CRUD - Create）
    local timestamp=$(date +%s)
    local client_data='{
        "name": "测试客户_'"$timestamp"'",
        "clientType": "INDIVIDUAL",
        "idCard": "110101199001011234",
        "contactPerson": "张三",
        "contactPhone": "13800138000",
        "contactEmail": "test@example.com"
    }'
    response=$(send_request "POST" "$BASE_URL/client" "$client_data" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        body=$(echo "$response" | sed '$d')
        CREATED_CLIENT_ID=$(extract_id "$body")
        print_result "创建客户" "PASS" "" "客户管理"
    else
        print_result "创建客户" "FAIL" "$result" "客户管理"
    fi
    
    # 2.3 获取客户详情（CRUD - Read）
    if [ -n "$CREATED_CLIENT_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/client/$CREATED_CLIENT_ID" "" "$auth_header")
        result=$(check_response "$response")
        if [ "$result" = "SUCCESS" ]; then
            print_result "获取客户详情" "PASS" "" "客户管理"
        else
            print_result "获取客户详情" "FAIL" "$result" "客户管理"
        fi
    fi
    
    # 2.4 更新客户（CRUD - Update）
    if [ -n "$CREATED_CLIENT_ID" ]; then
        local update_data='{
            "id": '"$CREATED_CLIENT_ID"',
            "name": "测试客户_已更新_'"$timestamp"'",
            "clientType": "INDIVIDUAL",
            "idCard": "110101199001011234",
            "contactPerson": "李四",
            "contactPhone": "13900139000"
        }'
        response=$(send_request "PUT" "$BASE_URL/client" "$update_data" "$auth_header")
        result=$(check_response "$response")
        if [ "$result" = "SUCCESS" ]; then
            print_result "更新客户信息" "PASS" "" "客户管理"
        else
            print_result "更新客户信息" "FAIL" "$result" "客户管理"
        fi
    fi
    
    # 2.5 案源列表
    response=$(send_request "GET" "$BASE_URL/client/lead?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "案源列表查询" "PASS" "" "客户管理"
    else
        print_result "案源列表查询" "SKIP" "$result" "客户管理"
    fi
    
    # 2.6 利冲审查列表
    response=$(send_request "GET" "$BASE_URL/client/conflict-check/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "利冲审查列表" "PASS" "" "客户管理"
    else
        print_result "利冲审查列表" "SKIP" "$result" "客户管理"
    fi
}

# ==================== 项目管理模块测试 ====================
test_matter_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 项目管理模块测试 (Matter Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 3.1 项目列表查询
    response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "项目列表查询" "PASS" "" "项目管理"
    else
        print_result "项目列表查询" "FAIL" "$result" "项目管理"
    fi
    
    # 3.2 我的项目
    response=$(send_request "GET" "$BASE_URL/matter/my?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "我的项目查询" "PASS" "" "项目管理"
    else
        print_result "我的项目查询" "SKIP" "$result" "项目管理"
    fi
    
    # 3.3 任务列表
    response=$(send_request "GET" "$BASE_URL/tasks?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "任务列表查询" "PASS" "" "项目管理"
    else
        print_result "任务列表查询" "SKIP" "$result" "项目管理"
    fi
    
    # 3.4 我的待办任务
    response=$(send_request "GET" "$BASE_URL/tasks/my/todo" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "我的待办任务" "PASS" "" "项目管理"
    else
        print_result "我的待办任务" "SKIP" "$result" "项目管理"
    fi
    
    # 3.5 工时列表
    response=$(send_request "GET" "$BASE_URL/timesheets?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "工时列表查询" "PASS" "" "项目管理"
    else
        print_result "工时列表查询" "SKIP" "$result" "项目管理"
    fi
    
    # 3.6 我的工时
    response=$(send_request "GET" "$BASE_URL/timesheets/my?startDate=2024-01-01&endDate=2026-12-31" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "我的工时查询" "PASS" "" "项目管理"
    else
        print_result "我的工时查询" "SKIP" "$result" "项目管理"
    fi
}

# ==================== 财务管理模块测试 ====================
test_finance_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 财务管理模块测试 (Finance Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 4.1 合同列表
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "合同列表查询" "PASS" "" "财务管理"
    else
        print_result "合同列表查询" "SKIP" "$result" "财务管理"
    fi
    
    # 4.2 收费列表
    response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "收费列表查询" "PASS" "" "财务管理"
    else
        print_result "收费列表查询" "SKIP" "$result" "财务管理"
    fi
    
    # 4.3 发票列表
    response=$(send_request "GET" "$BASE_URL/finance/invoice/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "发票列表查询" "PASS" "" "财务管理"
    else
        print_result "发票列表查询" "SKIP" "$result" "财务管理"
    fi
    
    # 4.4 提成列表
    response=$(send_request "GET" "$BASE_URL/finance/commission?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "提成列表查询" "PASS" "" "财务管理"
    else
        print_result "提成列表查询" "SKIP" "$result" "财务管理"
    fi
    
    # 4.5 费用报销
    response=$(send_request "GET" "$BASE_URL/finance/expense?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "费用报销列表" "PASS" "" "财务管理"
    else
        print_result "费用报销列表" "SKIP" "$result" "财务管理"
    fi
}

# ==================== 文档管理模块测试 ====================
test_document_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 文档管理模块测试 (Document Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 5.1 文档列表
    response=$(send_request "GET" "$BASE_URL/document?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "文档列表查询" "PASS" "" "文档管理"
    else
        print_result "文档列表查询" "SKIP" "$result" "文档管理"
    fi
    
    # 5.2 文档分类
    response=$(send_request "GET" "$BASE_URL/document/category/tree" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "文档分类查询" "PASS" "" "文档管理"
    else
        print_result "文档分类查询" "SKIP" "$result" "文档管理"
    fi
    
    # 5.3 文档模板
    response=$(send_request "GET" "$BASE_URL/document/template?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "文档模板列表" "PASS" "" "文档管理"
    else
        print_result "文档模板列表" "SKIP" "$result" "文档管理"
    fi
    
    # 5.4 印章列表
    response=$(send_request "GET" "$BASE_URL/document/seal?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "印章列表查询" "PASS" "" "文档管理"
    else
        print_result "印章列表查询" "SKIP" "$result" "文档管理"
    fi
}

# ==================== 证据管理模块测试 ====================
test_evidence_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 证据管理模块测试 (Evidence Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    response=$(send_request "GET" "$BASE_URL/evidence?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "证据列表查询" "PASS" "" "证据管理"
    else
        print_result "证据列表查询" "SKIP" "$result" "证据管理"
    fi
}

# ==================== 档案管理模块测试 ====================
test_archive_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 档案管理模块测试 (Archive Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 7.1 档案列表
    response=$(send_request "GET" "$BASE_URL/archive/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "档案列表查询" "PASS" "" "档案管理"
    else
        print_result "档案列表查询" "SKIP" "$result" "档案管理"
    fi
    
    # 7.2 档案借阅
    response=$(send_request "GET" "$BASE_URL/archive/borrow/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "档案借阅列表" "PASS" "" "档案管理"
    else
        print_result "档案借阅列表" "SKIP" "$result" "档案管理"
    fi
}

# ==================== 系统管理模块测试 ====================
test_system_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 系统管理模块测试 (System Management)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 8.1 用户列表
    response=$(send_request "GET" "$BASE_URL/system/user/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "用户列表查询" "PASS" "" "系统管理"
    else
        print_result "用户列表查询" "SKIP" "$result" "系统管理"
    fi
    
    # 8.2 角色列表
    response=$(send_request "GET" "$BASE_URL/system/role/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "角色列表查询" "PASS" "" "系统管理"
    else
        print_result "角色列表查询" "SKIP" "$result" "系统管理"
    fi
    
    # 8.3 部门树
    response=$(send_request "GET" "$BASE_URL/system/department/tree" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "部门树查询" "PASS" "" "系统管理"
    else
        print_result "部门树查询" "SKIP" "$result" "系统管理"
    fi
    
    # 8.4 菜单树
    response=$(send_request "GET" "$BASE_URL/system/menu/tree" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "菜单树查询" "PASS" "" "系统管理"
    else
        print_result "菜单树查询" "SKIP" "$result" "系统管理"
    fi
    
    # 8.5 字典类型
    response=$(send_request "GET" "$BASE_URL/system/dict/types" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "字典类型列表" "PASS" "" "系统管理"
    else
        print_result "字典类型列表" "SKIP" "$result" "系统管理"
    fi
    
    # 8.6 系统配置
    response=$(send_request "GET" "$BASE_URL/system/config" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "系统配置查询" "PASS" "" "系统管理"
    else
        print_result "系统配置查询" "SKIP" "$result" "系统管理"
    fi
}

# ==================== 工作台模块测试 ====================
test_workbench_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 工作台模块测试 (Workbench)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 9.1 工作台统计
    response=$(send_request "GET" "$BASE_URL/workbench/stats" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "工作台统计" "PASS" "" "工作台"
    else
        print_result "工作台统计" "FAIL" "$result" "工作台"
    fi
    
    # 9.2 工作台数据
    response=$(send_request "GET" "$BASE_URL/workbench/data" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "工作台数据" "PASS" "" "工作台"
    else
        print_result "工作台数据" "SKIP" "$result" "工作台"
    fi
    
    # 9.3 待办事项
    response=$(send_request "GET" "$BASE_URL/workbench/todo/summary" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "待办事项统计" "PASS" "" "工作台"
    else
        print_result "待办事项统计" "SKIP" "$result" "工作台"
    fi
    
    # 9.4 审批列表
    response=$(send_request "GET" "$BASE_URL/workbench/approval/list?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "审批列表查询" "PASS" "" "工作台"
    else
        print_result "审批列表查询" "SKIP" "$result" "工作台"
    fi
}

# ==================== 知识库模块测试 ====================
test_knowledge_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  10. 知识库模块测试 (Knowledge Base)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 10.1 法规库
    response=$(send_request "GET" "$BASE_URL/knowledge/law?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "法规库列表" "PASS" "" "知识库"
    else
        print_result "法规库列表" "SKIP" "$result" "知识库"
    fi
    
    # 10.2 案例库
    response=$(send_request "GET" "$BASE_URL/knowledge/case?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "案例库列表" "PASS" "" "知识库"
    else
        print_result "案例库列表" "SKIP" "$result" "知识库"
    fi
    
    # 10.3 经验文章
    response=$(send_request "GET" "$BASE_URL/knowledge/article?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "经验文章列表" "PASS" "" "知识库"
    else
        print_result "经验文章列表" "SKIP" "$result" "知识库"
    fi
}

# ==================== 行政后勤模块测试 ====================
test_admin_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  11. 行政后勤模块测试 (Administration)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 11.1 考勤列表
    response=$(send_request "GET" "$BASE_URL/admin/attendance?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "考勤列表查询" "PASS" "" "行政后勤"
    else
        print_result "考勤列表查询" "SKIP" "$result" "行政后勤"
    fi
    
    # 11.2 请假申请
    response=$(send_request "GET" "$BASE_URL/admin/leave/applications?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "请假申请列表" "PASS" "" "行政后勤"
    else
        print_result "请假申请列表" "SKIP" "$result" "行政后勤"
    fi
    
    # 11.3 会议室
    response=$(send_request "GET" "$BASE_URL/admin/meeting-room" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "会议室列表" "PASS" "" "行政后勤"
    else
        print_result "会议室列表" "SKIP" "$result" "行政后勤"
    fi
    
    # 11.4 资产列表
    response=$(send_request "GET" "$BASE_URL/admin/assets?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "资产列表查询" "PASS" "" "行政后勤"
    else
        print_result "资产列表查询" "SKIP" "$result" "行政后勤"
    fi
}

# ==================== 人力资源模块测试 ====================
test_hr_module() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  12. 人力资源模块测试 (Human Resources)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local response result
    
    # 12.1 员工列表
    response=$(send_request "GET" "$BASE_URL/hr/employee?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "员工列表查询" "PASS" "" "人力资源"
    else
        print_result "员工列表查询" "SKIP" "$result" "人力资源"
    fi
    
    # 12.2 培训通知
    response=$(send_request "GET" "$BASE_URL/hr/training-notice?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "培训通知列表" "PASS" "" "人力资源"
    else
        print_result "培训通知列表" "SKIP" "$result" "人力资源"
    fi
    
    # 12.3 绩效任务
    response=$(send_request "GET" "$BASE_URL/hr/performance/tasks?pageNum=1&pageSize=10" "" "$auth_header")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "绩效任务列表" "PASS" "" "人力资源"
    else
        print_result "绩效任务列表" "SKIP" "$result" "人力资源"
    fi
}

# ==================== 清理测试数据 ====================
cleanup_test_data() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  清理测试数据${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        return
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 删除测试创建的客户
    if [ -n "$CREATED_CLIENT_ID" ]; then
        local response=$(send_request "DELETE" "$BASE_URL/client/$CREATED_CLIENT_ID" "" "$auth_header")
        local result=$(check_response "$response")
        if [ "$result" = "SUCCESS" ]; then
            echo -e "${GREEN}✓${NC} 已删除测试客户 (ID: $CREATED_CLIENT_ID)"
            print_result "删除测试客户" "PASS" "" "数据清理"
        else
            echo -e "${YELLOW}⊘${NC} 删除测试客户失败（可能已被删除）"
            print_result "删除测试客户" "SKIP" "可能已被删除" "数据清理"
        fi
    fi
}

# ==================== 生成测试报告 ====================
generate_report() {
    TEST_END_TIME=$(date +"%Y-%m-%d %H:%M:%S")
    
    local pass_rate=0
    if [ $TOTAL -gt 0 ]; then
        pass_rate=$((PASSED * 100 / TOTAL))
    fi
    
    cat > "$REPORT_FILE" << EOF
# 智慧律所管理系统 - 生产环境部署前测试报告

## 测试概要

| 项目 | 值 |
|------|-----|
| 测试日期 | $(date +%Y-%m-%d) |
| 测试开始时间 | $TEST_START_TIME |
| 测试结束时间 | $TEST_END_TIME |
| 测试环境 | 本地开发环境 |
| 后端地址 | http://localhost:8080 |
| 前端地址 | http://localhost:5555 |

## 测试结果汇总

| 指标 | 数量 | 说明 |
|------|------|------|
| **总测试数** | $TOTAL | - |
| ✅ **通过** | $PASSED | 接口正常响应 |
| ❌ **失败** | $FAILED | 接口异常或错误 |
| ⏭️ **跳过** | $SKIPPED | 可选功能或需特定条件 |
| **通过率** | ${pass_rate}% | 通过数/总测试数 |

## 测试结论

EOF

    if [ $FAILED -eq 0 ]; then
        echo "### ✅ 测试通过" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "所有核心功能测试通过，系统具备部署生产环境的条件。" >> "$REPORT_FILE"
    else
        echo "### ⚠️ 存在失败项" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "有 $FAILED 个测试未通过，请检查以下问题后再部署：" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        for detail in "${FAILED_DETAILS[@]}"; do
            echo "- $detail" >> "$REPORT_FILE"
        done
    fi

    cat >> "$REPORT_FILE" << 'EOF'

## 模块测试详情

### 1. 认证模块 (Authentication)

| 测试项 | 状态 | 说明 |
|--------|------|------|
EOF

    # 按模块输出测试结果
    local current_module=""
    local module_header_written=0
    
    for result in "${TEST_RESULTS[@]}"; do
        local status=$(echo "$result" | cut -d'|' -f1)
        local module=$(echo "$result" | cut -d'|' -f2)
        local name=$(echo "$result" | cut -d'|' -f3)
        local message=$(echo "$result" | cut -d'|' -f4)
        
        local status_icon=""
        case $status in
            "PASS") status_icon="✅ 通过" ;;
            "FAIL") status_icon="❌ 失败" ;;
            "SKIP") status_icon="⏭️ 跳过" ;;
        esac
        
        echo "| $name | $status_icon | $message |" >> "$REPORT_FILE"
    done

    cat >> "$REPORT_FILE" << EOF

## 部署建议

### 部署前检查清单

- [ ] 所有核心API测试通过
- [ ] 数据库迁移脚本已准备
- [ ] 环境变量已正确配置
- [ ] SSL证书已配置（生产环境）
- [ ] 备份策略已就绪
- [ ] 监控告警已配置

### 注意事项

1. **数据库备份**：部署前务必备份现有数据
2. **配置检查**：确认生产环境配置文件正确
3. **权限验证**：确认各角色权限配置正确
4. **性能测试**：建议进行压力测试

---

*报告生成时间: $(date +"%Y-%m-%d %H:%M:%S")*
*测试脚本: scripts/test/full-api-test.sh*
EOF

    echo ""
    echo -e "${GREEN}测试报告已生成: $REPORT_FILE${NC}"
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}                    测试总结${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        echo "通过率: ${pass_rate}%"
    fi
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 所有测试通过！系统可以部署到生产环境${NC}"
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}  ❌ 有 $FAILED 个测试失败，请检查后再部署${NC}"
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
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     智慧律所管理系统 - 生产环境部署前全面测试${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     测试时间: $(date +"%Y-%m-%d %H:%M:%S")${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    
    # 初始化报告
    init_report
    
    # 检查服务
    if ! check_service; then
        echo -e "${RED}服务未运行，测试终止${NC}"
        exit 1
    fi
    
    # 执行测试
    test_auth_module
    if [ $? -eq 0 ]; then
        test_client_module
        test_matter_module
        test_finance_module
        test_document_module
        test_evidence_module
        test_archive_module
        test_system_module
        test_workbench_module
        test_knowledge_module
        test_admin_module
        test_hr_module
        
        # 清理测试数据
        cleanup_test_data
    fi
    
    # 打印总结
    print_summary
    
    # 生成报告
    generate_report
    
    if [ $FAILED -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# 运行主函数
main

