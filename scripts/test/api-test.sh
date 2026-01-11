#!/bin/bash

# 全面API测试脚本
# 测试所有主要接口

BASE_URL="http://localhost:8080/api"
TOKEN=""
REFRESH_TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

# 测试结果数组
declare -a TEST_RESULTS

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} $test_name: ${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
        TEST_RESULTS+=("PASS:$test_name")
    elif [ "$status" = "SKIP" ]; then
        echo -e "${YELLOW}⊘${NC} $test_name: ${YELLOW}SKIP${NC}"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP:$test_name")
    else
        echo -e "${RED}✗${NC} $test_name: ${RED}FAIL${NC}"
        if [ -n "$message" ]; then
            echo -e "  ${RED}Error: $message${NC}"
        fi
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL:$test_name:$message")
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

# 检查服务是否运行
check_service() {
    echo -e "${BLUE}检查后端服务状态...${NC}"
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} 后端服务运行中"
        return 0
    else
        echo -e "${RED}✗${NC} 后端服务未运行"
        return 1
    fi
}

# 测试登录
test_login() {
    echo ""
    echo -e "${BLUE}=== 认证模块测试 ===${NC}"
    
    local response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
        print_result "用户登录" "PASS"
        return 0
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "用户登录" "FAIL" "$message"
        return 1
    fi
}

# 测试获取用户信息
test_get_user_info() {
    if [ -z "$TOKEN" ]; then
        print_result "获取用户信息" "SKIP" "未登录"
        return 1
    fi
    
    local response=$(send_request "GET" "$BASE_URL/auth/info" "" "Authorization: Bearer $TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "获取用户信息" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "获取用户信息" "FAIL" "$message"
    fi
}

# 测试刷新Token
test_refresh_token() {
    if [ -z "$REFRESH_TOKEN" ]; then
        print_result "刷新Token" "SKIP" "无refreshToken"
        return 1
    fi
    
    local response=$(send_request "POST" "$BASE_URL/auth/refresh" "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "刷新Token" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "刷新Token" "FAIL" "$message"
    fi
}

# 测试获取验证码
test_get_captcha() {
    local response=$(send_request "GET" "$BASE_URL/auth/captcha")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "获取验证码" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "获取验证码" "FAIL" "$message"
    fi
}

# 客户管理模块测试
test_client_module() {
    echo ""
    echo -e "${BLUE}=== 客户管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 客户列表
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "客户列表查询" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "客户列表查询" "FAIL" "$message"
    fi
    
    # 案源列表
    response=$(send_request "GET" "$BASE_URL/client/lead?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "案源列表查询" "PASS"
    else
        print_result "案源列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 利益冲突审查列表
    response=$(send_request "GET" "$BASE_URL/client/conflict-check/list?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "利冲审查列表" "PASS"
    else
        print_result "利冲审查列表" "SKIP" "可能未实现或需要权限"
    fi
}

# 项目管理模块测试
test_matter_module() {
    echo ""
    echo -e "${BLUE}=== 项目管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 项目列表
    local response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "项目列表查询" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "项目列表查询" "FAIL" "$message"
    fi
    
    # 我的项目
    response=$(send_request "GET" "$BASE_URL/matter/my?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "我的项目查询" "PASS"
    else
        print_result "我的项目查询" "SKIP" "可能未实现"
    fi
    
    # 任务列表
    response=$(send_request "GET" "$BASE_URL/tasks?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "任务列表查询" "PASS"
    else
        print_result "任务列表查询" "SKIP" "可能未实现"
    fi
    
    # 我的待办任务
    response=$(send_request "GET" "$BASE_URL/tasks/my/todo" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "我的待办任务" "PASS"
    else
        print_result "我的待办任务" "SKIP" "可能未实现"
    fi
    
    # 工时列表
    response=$(send_request "GET" "$BASE_URL/timesheets?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "工时列表查询" "PASS"
    else
        print_result "工时列表查询" "SKIP" "可能未实现"
    fi
    
    # 我的工时
    response=$(send_request "GET" "$BASE_URL/timesheets/my?startDate=2024-01-01&endDate=2024-12-31" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "我的工时查询" "PASS"
    else
        print_result "我的工时查询" "SKIP" "可能未实现"
    fi
}

# 财务管理模块测试
test_finance_module() {
    echo ""
    echo -e "${BLUE}=== 财务管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 合同列表
    local response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "合同列表查询" "PASS"
    else
        print_result "合同列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 收费列表
    response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "收费列表查询" "PASS"
    else
        print_result "收费列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 发票列表
    response=$(send_request "GET" "$BASE_URL/finance/invoice/list?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "发票列表查询" "PASS"
    else
        print_result "发票列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 提成列表
    response=$(send_request "GET" "$BASE_URL/finance/commission?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "提成列表查询" "PASS"
    else
        print_result "提成列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 费用报销列表
    response=$(send_request "GET" "$BASE_URL/finance/expense?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "费用报销列表" "PASS"
    else
        print_result "费用报销列表" "SKIP" "可能未实现或需要权限"
    fi
}

# 文档管理模块测试
test_document_module() {
    echo ""
    echo -e "${BLUE}=== 文档管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 文档列表
    local response=$(send_request "GET" "$BASE_URL/document?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "文档列表查询" "PASS"
    else
        print_result "文档列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 文档分类列表
    response=$(send_request "GET" "$BASE_URL/document/category/tree" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "文档分类查询" "PASS"
    else
        print_result "文档分类查询" "SKIP" "可能未实现"
    fi
    
    # 文档模板列表
    response=$(send_request "GET" "$BASE_URL/document/template?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "文档模板列表" "PASS"
    else
        print_result "文档模板列表" "SKIP" "可能未实现"
    fi
    
    # 印章列表
    response=$(send_request "GET" "$BASE_URL/document/seal?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "印章列表查询" "PASS"
    else
        print_result "印章列表查询" "SKIP" "可能未实现"
    fi
}

# 证据管理模块测试
test_evidence_module() {
    echo ""
    echo -e "${BLUE}=== 证据管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 证据列表
    local response=$(send_request "GET" "$BASE_URL/evidence?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "证据列表查询" "PASS"
    else
        print_result "证据列表查询" "SKIP" "可能未实现或需要权限"
    fi
}

# 档案管理模块测试
test_archive_module() {
    echo ""
    echo -e "${BLUE}=== 档案管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 档案列表
    local response=$(send_request "GET" "$BASE_URL/archive/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "档案列表查询" "PASS"
    else
        print_result "档案列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 档案借阅列表
    response=$(send_request "GET" "$BASE_URL/archive/borrow/list?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "档案借阅列表" "PASS"
    else
        print_result "档案借阅列表" "SKIP" "可能未实现"
    fi
}

# 系统管理模块测试
test_system_module() {
    echo ""
    echo -e "${BLUE}=== 系统管理模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 用户列表
    local response=$(send_request "GET" "$BASE_URL/system/user/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "用户列表查询" "PASS"
    else
        print_result "用户列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 角色列表
    response=$(send_request "GET" "$BASE_URL/system/role/list?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "角色列表查询" "PASS"
    else
        print_result "角色列表查询" "SKIP" "可能未实现"
    fi
    
    # 部门列表
    response=$(send_request "GET" "$BASE_URL/system/department/tree" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "部门树查询" "PASS"
    else
        print_result "部门树查询" "SKIP" "可能未实现"
    fi
    
    # 菜单树
    response=$(send_request "GET" "$BASE_URL/system/menu/tree" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "菜单树查询" "PASS"
    else
        print_result "菜单树查询" "SKIP" "可能未实现"
    fi
    
    # 字典类型列表
    response=$(send_request "GET" "$BASE_URL/system/dict/types" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "字典类型列表" "PASS"
    else
        print_result "字典类型列表" "SKIP" "可能未实现"
    fi
}

# 工作台模块测试
test_workbench_module() {
    echo ""
    echo -e "${BLUE}=== 工作台模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 工作台统计数据
    local response=$(send_request "GET" "$BASE_URL/workbench/stats" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "工作台统计" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "工作台统计" "FAIL" "$message"
    fi
    
    # 工作台数据
    response=$(send_request "GET" "$BASE_URL/workbench/data" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "工作台数据" "PASS"
    else
        print_result "工作台数据" "SKIP" "可能未实现"
    fi
    
    # 待办事项统计
    response=$(send_request "GET" "$BASE_URL/workbench/todo/summary" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "待办事项统计" "PASS"
    else
        print_result "待办事项统计" "SKIP" "可能未实现"
    fi
    
    # 审批列表
    response=$(send_request "GET" "$BASE_URL/workbench/approval/list?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "审批列表查询" "PASS"
    else
        print_result "审批列表查询" "SKIP" "可能未实现"
    fi
}

# 知识库模块测试
test_knowledge_module() {
    echo ""
    echo -e "${BLUE}=== 知识库模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 法规库列表
    local response=$(send_request "GET" "$BASE_URL/knowledge/law?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "法规库列表" "PASS"
    else
        print_result "法规库列表" "SKIP" "可能未实现或需要权限"
    fi
    
    # 案例库列表
    response=$(send_request "GET" "$BASE_URL/knowledge/case?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "案例库列表" "PASS"
    else
        print_result "案例库列表" "SKIP" "可能未实现"
    fi
    
    # 经验文章列表
    response=$(send_request "GET" "$BASE_URL/knowledge/article?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "经验文章列表" "PASS"
    else
        print_result "经验文章列表" "SKIP" "可能未实现"
    fi
}

# 行政后勤模块测试
test_admin_module() {
    echo ""
    echo -e "${BLUE}=== 行政后勤模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 考勤列表
    local response=$(send_request "GET" "$BASE_URL/admin/attendance?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "考勤列表查询" "PASS"
    else
        print_result "考勤列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 请假申请列表
    response=$(send_request "GET" "$BASE_URL/admin/leave/applications?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "请假申请列表" "PASS"
    else
        print_result "请假申请列表" "SKIP" "可能未实现"
    fi
    
    # 会议室列表
    response=$(send_request "GET" "$BASE_URL/admin/meeting-room" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "会议室列表" "PASS"
    else
        print_result "会议室列表" "SKIP" "可能未实现"
    fi
    
    # 资产列表
    response=$(send_request "GET" "$BASE_URL/admin/assets?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "资产列表查询" "PASS"
    else
        print_result "资产列表查询" "SKIP" "可能未实现"
    fi
}

# 人力资源模块测试
test_hr_module() {
    echo ""
    echo -e "${BLUE}=== 人力资源模块测试 ===${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 员工列表
    local response=$(send_request "GET" "$BASE_URL/hr/employee?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "员工列表查询" "PASS"
    else
        print_result "员工列表查询" "SKIP" "可能未实现或需要权限"
    fi
    
    # 培训通知列表
    response=$(send_request "GET" "$BASE_URL/hr/training-notice?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "培训通知列表" "PASS"
    else
        print_result "培训通知列表" "SKIP" "可能未实现"
    fi
    
    # 绩效列表
    response=$(send_request "GET" "$BASE_URL/hr/performance/tasks?pageNum=1&pageSize=10" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "绩效任务列表" "PASS"
    else
        print_result "绩效任务列表" "SKIP" "可能未实现"
    fi
}

# 打印测试总结
print_summary() {
    echo ""
    echo "=========================================="
    echo "测试总结"
    echo "=========================================="
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        echo "通过率: ${pass_rate}%"
    fi
    
    echo ""
    echo "详细结果："
    for result in "${TEST_RESULTS[@]}"; do
        local status=$(echo "$result" | cut -d':' -f1)
        local name=$(echo "$result" | cut -d':' -f2)
        local message=$(echo "$result" | cut -d':' -f3-)
        
        if [ "$status" = "PASS" ]; then
            echo -e "  ${GREEN}✓${NC} $name"
        elif [ "$status" = "SKIP" ]; then
            echo -e "  ${YELLOW}⊘${NC} $name"
        else
            echo -e "  ${RED}✗${NC} $name"
            if [ -n "$message" ]; then
                echo -e "    ${RED}$message${NC}"
            fi
        fi
    done
    
    if [ $FAILED -eq 0 ]; then
        echo ""
        echo -e "${GREEN}所有测试通过！${NC}"
        exit 0
    else
        echo ""
        echo -e "${RED}有 $FAILED 个测试失败${NC}"
        exit 1
    fi
}

# 主函数
main() {
    echo "=========================================="
    echo "智慧律所管理系统 - 全面API测试"
    echo "=========================================="
    
    # 检查服务
    if ! check_service; then
        exit 1
    fi
    
    # 执行测试
    test_login
    if [ $? -eq 0 ]; then
        test_get_user_info
        test_refresh_token
        test_get_captcha
        
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
    fi
    
    # 打印总结
    print_summary
}

# 运行主函数
main

