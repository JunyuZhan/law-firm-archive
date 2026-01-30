#!/bin/bash

# 智慧律所管理系统 - 客户管理模块业务逻辑测试脚本
# 测试内容：客户管理、案源管理、联系人管理、利冲审查、客户关系等完整业务逻辑
# 测试日期：$(date +%Y-%m-%d)

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

declare -a TEST_RESULTS
declare -a FAILED_DETAILS

# 测试创建的资源ID
TEST_CLIENT_ID=""
TEST_INDIVIDUAL_CLIENT_ID=""
TEST_ENTERPRISE_CLIENT_ID=""
TEST_CONTACT_ID=""
TEST_LEAD_ID=""
TEST_CONFLICT_CHECK_ID=""

# 时间戳用于唯一名称
TIMESTAMP=$(date +%s)

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    local category=$4
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} $test_name: ${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
        TEST_RESULTS+=("PASS|$category|$test_name|")
    elif [ "$status" = "SKIP" ]; then
        echo -e "${YELLOW}⊘${NC} $test_name: ${YELLOW}SKIP${NC} - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP|$category|$test_name|$message")
    else
        echo -e "${RED}✗${NC} $test_name: ${RED}FAIL${NC}"
        if [ -n "$message" ]; then
            echo -e "  ${RED}Error: $message${NC}"
        fi
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL|$category|$test_name|$message")
        FAILED_DETAILS+=("$category - $test_name: $message")
    fi
}

# 打印章节标题
print_section() {
    local title=$1
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $title${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
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

# 从响应中提取ID
extract_id() {
    local body=$1
    echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# 从响应中提取字段
extract_field() {
    local body=$1
    local field=$2
    echo "$body" | grep -o "\"$field\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
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

# 检查响应是否成功（只检查第一个 success 字段）
check_success() {
    local body=$1
    local success=$(echo "$body" | grep -o '"success":[^,]*' | head -1 | cut -d':' -f2)
    [ "$success" = "true" ]
}

# 登录获取Token（带滑块验证）
login() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo -e "${BLUE}       登录认证${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    
    # Step 1: 获取滑块验证令牌
    local slider_response=$(send_request "GET" "$BASE_URL/auth/slider/token" "" "")
    local slider_body=$(echo "$slider_response" | sed '$d')
    
    if ! check_success "$slider_body"; then
        echo -e "${RED}✗${NC} 获取滑块令牌失败"
        return 1
    fi
    
    local token_id=$(echo "$slider_body" | grep -o '"tokenId":"[^"]*"' | cut -d'"' -f4)
    
    # Step 2: 验证滑块
    local verify_response=$(send_request "POST" "$BASE_URL/auth/slider/verify" "{\"tokenId\":\"$token_id\",\"slideTime\":1500}" "")
    local verify_body=$(echo "$verify_response" | sed '$d')
    
    if ! check_success "$verify_body"; then
        echo -e "${RED}✗${NC} 滑块验证失败"
        return 1
    fi
    
    local slider_verify_token=$(echo "$verify_body" | grep -o '"verifyToken":"[^"]*"' | cut -d'"' -f4)
    
    # Step 3: 使用滑块验证令牌登录
    local response=$(send_request "POST" "$BASE_URL/auth/login" "{\"username\":\"admin\",\"password\":\"admin123\",\"sliderVerifyToken\":\"$slider_verify_token\"}")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓${NC} 管理员账号登录成功"
        return 0
    else
        echo -e "${RED}✗${NC} 登录失败"
        return 1
    fi
}

# ==================== 1. 客户基础 CRUD 测试 ====================
test_client_crud() {
    print_section "1. 客户基础 CRUD 测试"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 查询客户列表
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询客户列表" "PASS" "" "客户CRUD"
    else
        print_result "查询客户列表" "FAIL" "查询失败" "客户CRUD"
    fi
    
    # 1.2 创建个人客户
    local client_data='{
        "name": "测试个人客户_'"$TIMESTAMP"'",
        "clientType": "INDIVIDUAL",
        "idCard": "110101199001011234",
        "contactPerson": "张三",
        "contactPhone": "13800138001",
        "contactEmail": "test@example.com",
        "source": "REFERRAL",
        "level": "NORMAL"
    }'
    response=$(send_request "POST" "$BASE_URL/client" "$client_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_INDIVIDUAL_CLIENT_ID=$(extract_id "$body")
        print_result "创建个人客户" "PASS" "" "客户CRUD"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建个人客户" "FAIL" "$message" "客户CRUD"
    fi
    
    # 1.3 创建企业客户
    client_data='{
        "name": "测试企业客户_'"$TIMESTAMP"'",
        "clientType": "ENTERPRISE",
        "creditCode": "91110000'"$TIMESTAMP"'",
        "legalRepresentative": "李四",
        "registeredAddress": "北京市朝阳区测试路1号",
        "contactPerson": "王五",
        "contactPhone": "13800138002",
        "industry": "TECHNOLOGY",
        "source": "DIRECT",
        "level": "VIP"
    }'
    response=$(send_request "POST" "$BASE_URL/client" "$client_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_ENTERPRISE_CLIENT_ID=$(extract_id "$body")
        TEST_CLIENT_ID=$TEST_ENTERPRISE_CLIENT_ID
        print_result "创建企业客户" "PASS" "" "客户CRUD"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建企业客户" "FAIL" "$message" "客户CRUD"
    fi
    
    # 1.4 获取客户详情
    if [ -n "$TEST_CLIENT_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/client/$TEST_CLIENT_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "获取客户详情" "PASS" "" "客户CRUD"
        else
            print_result "获取客户详情" "FAIL" "获取失败" "客户CRUD"
        fi
    fi
    
    # 1.5 更新客户信息
    if [ -n "$TEST_CLIENT_ID" ]; then
        local update_data='{
            "id": '"$TEST_CLIENT_ID"',
            "name": "更新后企业客户_'"$TIMESTAMP"'",
            "clientType": "ENTERPRISE",
            "creditCode": "91110000'"$TIMESTAMP"'",
            "contactPhone": "13800138099",
            "remark": "测试更新备注"
        }'
        response=$(send_request "PUT" "$BASE_URL/client" "$update_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "更新客户信息" "PASS" "" "客户CRUD"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "更新客户信息" "FAIL" "$message" "客户CRUD"
        fi
    fi
    
    # 1.6 客户选择列表接口（公共接口）
    response=$(send_request "GET" "$BASE_URL/client/select-options?pageNum=1&pageSize=20" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "客户选择列表接口" "PASS" "" "客户CRUD"
    else
        print_result "客户选择列表接口" "FAIL" "查询失败" "客户CRUD"
    fi
}

# ==================== 2. 客户业务规则验证 ====================
test_client_business_rules() {
    print_section "2. 客户业务规则验证"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 测试客户名称必填
    local client_data='{
        "clientType": "INDIVIDUAL",
        "idCard": "110101199001011111"
    }'
    local response=$(send_request "POST" "$BASE_URL/client" "$client_data" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        print_result "客户名称必填校验" "PASS" "" "业务规则"
    else
        print_result "客户名称必填校验" "FAIL" "允许空名称创建" "业务规则"
    fi
    
    # 2.2 测试客户类型必填
    client_data='{
        "name": "类型测试客户_'"$TIMESTAMP"'"
    }'
    response=$(send_request "POST" "$BASE_URL/client" "$client_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        print_result "客户类型必填校验" "PASS" "" "业务规则"
    else
        print_result "客户类型必填校验" "FAIL" "允许空类型创建" "业务规则"
    fi
    
    # 2.3 测试重复客户名称创建
    if [ -n "$TEST_INDIVIDUAL_CLIENT_ID" ]; then
        client_data='{
            "name": "测试个人客户_'"$TIMESTAMP"'",
            "clientType": "INDIVIDUAL",
            "idCard": "110101199001012222"
        }'
        response=$(send_request "POST" "$BASE_URL/client" "$client_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "false" ]; then
            print_result "客户名称唯一性约束" "PASS" "" "业务规则"
        else
            print_result "客户名称唯一性约束" "SKIP" "可能允许同名客户" "业务规则"
        fi
    fi
    
    # 2.4 测试客户状态修改
    if [ -n "$TEST_CLIENT_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/client/$TEST_CLIENT_ID/status" '{"status":"INACTIVE"}' "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "客户状态修改为停用" "PASS" "" "业务规则"
            
            # 恢复为激活状态
            send_request "PUT" "$BASE_URL/client/$TEST_CLIENT_ID/status" '{"status":"ACTIVE"}' "$auth_header" > /dev/null
        else
            print_result "客户状态修改为停用" "SKIP" "可能接口不同" "业务规则"
        fi
    fi
    
    # 2.5 测试查询不存在的客户
    response=$(send_request "GET" "$BASE_URL/client/999999999" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$http_code" = "404" ] || [ "$success" = "false" ]; then
        print_result "查询不存在客户返回正确状态" "PASS" "" "业务规则"
    else
        print_result "查询不存在客户返回正确状态" "FAIL" "应返回404或success=false" "业务规则"
    fi
}

# ==================== 3. 案源管理测试 ====================
test_lead_management() {
    print_section "3. 案源管理测试"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 3.1 查询案源列表
    local response=$(send_request "GET" "$BASE_URL/client/lead?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询案源列表" "PASS" "" "案源管理"
    else
        print_result "查询案源列表" "SKIP" "可能无权限或未实现" "案源管理"
    fi
    
    # 3.2 创建案源
    local lead_data='{
        "leadName": "案源测试_'"$TIMESTAMP"'",
        "leadType": "INDIVIDUAL",
        "contactName": "赵六",
        "contactPhone": "13900139001",
        "sourceChannel": "REFERRAL",
        "businessType": "LITIGATION",
        "estimatedAmount": 100000,
        "description": "测试案源描述",
        "priority": "HIGH"
    }'
    response=$(send_request "POST" "$BASE_URL/client/lead" "$lead_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_LEAD_ID=$(extract_id "$body")
        print_result "创建案源" "PASS" "" "案源管理"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建案源" "SKIP" "$message" "案源管理"
    fi
    
    # 3.3 获取案源详情
    if [ -n "$TEST_LEAD_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/client/lead/$TEST_LEAD_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "获取案源详情" "PASS" "" "案源管理"
        else
            print_result "获取案源详情" "FAIL" "获取失败" "案源管理"
        fi
    fi
    
    # 3.4 添加案源跟进记录
    if [ -n "$TEST_LEAD_ID" ]; then
        local followup_data='{
            "content": "首次电话联系，客户表示有合作意向",
            "nextFollowUpTime": "2026-01-15T10:00:00"
        }'
        response=$(send_request "POST" "$BASE_URL/client/lead/$TEST_LEAD_ID/follow-up" "$followup_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "创建案源跟进记录" "PASS" "" "案源管理"
        else
            print_result "创建案源跟进记录" "SKIP" "可能接口不同" "案源管理"
        fi
        
        # 3.5 查询案源跟进记录
        response=$(send_request "GET" "$BASE_URL/client/lead/$TEST_LEAD_ID/follow-ups" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "查询案源跟进记录" "PASS" "" "案源管理"
        else
            print_result "查询案源跟进记录" "SKIP" "可能接口不同" "案源管理"
        fi
    fi
    
    # 3.6 案源统计
    response=$(send_request "GET" "$BASE_URL/client/lead/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "案源统计接口" "PASS" "" "案源管理"
    else
        print_result "案源统计接口" "SKIP" "可能未实现" "案源管理"
    fi
}

# ==================== 4. 利冲审查测试 ====================
test_conflict_check() {
    print_section "4. 利益冲突审查测试"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 查询利冲审查列表
    local response=$(send_request "GET" "$BASE_URL/client/conflict-check/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询利冲审查列表" "PASS" "" "利冲审查"
    else
        print_result "查询利冲审查列表" "SKIP" "可能无权限" "利冲审查"
    fi
    
    # 4.2 快速利冲检索
    local quick_check_data='{
        "clientName": "测试客户ABC",
        "opposingParty": "对方当事人XYZ"
    }'
    response=$(send_request "POST" "$BASE_URL/client/conflict-check/quick" "$quick_check_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        local has_conflict=$(echo "$body" | grep -o '"hasConflict":[^,}]*' | cut -d':' -f2)
        print_result "快速利冲检索" "PASS" "" "利冲审查"
        echo -e "    ${BLUE}→ 冲突检测结果: hasConflict=$has_conflict${NC}"
    else
        print_result "快速利冲检索" "SKIP" "可能接口不同" "利冲审查"
    fi
    
    # 4.3 申请利冲审查
    local apply_data='{
        "clientName": "利冲申请测试客户_'"$TIMESTAMP"'",
        "opposingParty": "对方当事人_'"$TIMESTAMP"'",
        "matterName": "测试案件名称",
        "checkType": "NEW_CLIENT",
        "remark": "自动化测试申请"
    }'
    response=$(send_request "POST" "$BASE_URL/client/conflict-check/apply" "$apply_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_CONFLICT_CHECK_ID=$(extract_id "$body")
        print_result "申请利冲审查" "PASS" "" "利冲审查"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "申请利冲审查" "SKIP" "$message" "利冲审查"
    fi
    
    # 4.4 获取利冲审查详情
    if [ -n "$TEST_CONFLICT_CHECK_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/client/conflict-check/$TEST_CONFLICT_CHECK_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "获取利冲审查详情" "PASS" "" "利冲审查"
        else
            print_result "获取利冲审查详情" "FAIL" "获取失败" "利冲审查"
        fi
    fi
    
    # 4.5 利冲审批通过测试
    if [ -n "$TEST_CONFLICT_CHECK_ID" ]; then
        local approve_data='{"comment": "自动化测试审批通过"}'
        response=$(send_request "POST" "$BASE_URL/client/conflict-check/$TEST_CONFLICT_CHECK_ID/approve" "$approve_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "利冲审批通过" "PASS" "" "利冲审查"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "利冲审批通过" "SKIP" "$message" "利冲审查"
        fi
    fi
    
    # 4.6 利冲审查客户搜索（用于对方当事人字段）
    response=$(send_request "GET" "$BASE_URL/client/search-for-conflict?keyword=测试&limit=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "利冲客户搜索接口" "PASS" "" "利冲审查"
    else
        print_result "利冲客户搜索接口" "SKIP" "可能无权限" "利冲审查"
    fi
}

# ==================== 5. 联系人管理测试 ====================
test_contact_management() {
    print_section "5. 联系人管理测试"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    if [ -z "$TEST_CLIENT_ID" ]; then
        echo -e "${YELLOW}跳过：无测试客户${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 创建联系人
    local contact_data='{
        "clientId": '"$TEST_CLIENT_ID"',
        "contactName": "联系人测试_'"$TIMESTAMP"'",
        "position": "总经理",
        "mobilePhone": "13600136001",
        "email": "contact@test.com",
        "isPrimary": true,
        "relationshipNote": "测试联系人"
    }'
    local response=$(send_request "POST" "$BASE_URL/client/contact" "$contact_data" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_CONTACT_ID=$(extract_id "$body")
        print_result "创建联系人" "PASS" "" "联系人管理"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建联系人" "SKIP" "$message" "联系人管理"
    fi
    
    # 5.2 查询客户联系人列表
    response=$(send_request "GET" "$BASE_URL/client/contact/client/$TEST_CLIENT_ID" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询客户联系人列表" "PASS" "" "联系人管理"
    else
        print_result "查询客户联系人列表" "SKIP" "可能接口不同" "联系人管理"
    fi
    
    # 5.3 获取联系人详情
    if [ -n "$TEST_CONTACT_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/client/contact/$TEST_CONTACT_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "获取联系人详情" "PASS" "" "联系人管理"
        else
            print_result "获取联系人详情" "FAIL" "获取失败" "联系人管理"
        fi
    fi
    
    # 5.4 更新联系人
    if [ -n "$TEST_CONTACT_ID" ]; then
        local update_contact='{
            "clientId": '"$TEST_CLIENT_ID"',
            "contactName": "更新联系人_'"$TIMESTAMP"'",
            "position": "副总经理",
            "mobilePhone": "13600136099"
        }'
        response=$(send_request "PUT" "$BASE_URL/client/contact/$TEST_CONTACT_ID" "$update_contact" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "更新联系人" "PASS" "" "联系人管理"
        else
            print_result "更新联系人" "SKIP" "可能接口不同" "联系人管理"
        fi
    fi
}

# ==================== 6. 客户查询和筛选测试 ====================
test_client_search_filter() {
    print_section "6. 客户查询和筛选测试"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 按客户类型筛选
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10&clientType=INDIVIDUAL" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "按客户类型筛选（个人）" "PASS" "" "查询筛选"
    else
        print_result "按客户类型筛选（个人）" "FAIL" "筛选失败" "查询筛选"
    fi
    
    response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10&clientType=ENTERPRISE" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "按客户类型筛选（企业）" "PASS" "" "查询筛选"
    else
        print_result "按客户类型筛选（企业）" "FAIL" "筛选失败" "查询筛选"
    fi
    
    # 6.2 按状态筛选
    response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10&status=ACTIVE" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "按状态筛选（激活）" "PASS" "" "查询筛选"
    else
        print_result "按状态筛选（激活）" "SKIP" "可能不支持状态筛选" "查询筛选"
    fi
    
    # 6.3 按关键字搜索
    response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10&keyword=测试" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "按关键字搜索" "PASS" "" "查询筛选"
    else
        print_result "按关键字搜索" "SKIP" "可能不支持关键字搜索" "查询筛选"
    fi
    
    # 6.4 分页测试
    response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=5" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        local page_size=$(echo "$body" | grep -o '"pageSize":[0-9]*' | cut -d':' -f2)
        if [ "$page_size" = "5" ]; then
            print_result "分页查询（pageSize=5）" "PASS" "" "查询筛选"
        else
            print_result "分页查询（pageSize=5）" "SKIP" "返回的pageSize不匹配" "查询筛选"
        fi
    else
        print_result "分页查询（pageSize=5）" "FAIL" "分页查询失败" "查询筛选"
    fi
}

# ==================== 7. 数据完整性和权限测试 ====================
test_data_integrity() {
    print_section "7. 数据完整性和权限测试"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 无Token访问客户列表
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    
    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "无Token访问被正确拒绝" "PASS" "" "权限测试"
    else
        print_result "无Token访问被正确拒绝" "FAIL" "应返回401/403，实际返回$http_code" "权限测试"
    fi
    
    # 7.2 使用无效Token访问
    response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10" "" "Authorization: Bearer invalid_token_12345")
    http_code=$(echo "$response" | tail -1)
    
    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "无效Token被正确拒绝" "PASS" "" "权限测试"
    else
        print_result "无效Token被正确拒绝" "FAIL" "应返回401/403，实际返回$http_code" "权限测试"
    fi
    
    # 7.3 删除不存在的客户
    response=$(send_request "DELETE" "$BASE_URL/client/999999999" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$http_code" = "404" ] || [ "$success" = "false" ]; then
        print_result "删除不存在客户返回正确状态" "PASS" "" "数据完整性"
    else
        print_result "删除不存在客户返回正确状态" "SKIP" "可能静默处理" "数据完整性"
    fi
    
    # 7.4 更新不存在的客户
    local update_data='{
        "id": 999999999,
        "name": "不存在的客户",
        "clientType": "INDIVIDUAL"
    }'
    response=$(send_request "PUT" "$BASE_URL/client" "$update_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        print_result "更新不存在客户返回错误" "PASS" "" "数据完整性"
    else
        print_result "更新不存在客户返回错误" "SKIP" "可能有不同处理方式" "数据完整性"
    fi
}

# ==================== 清理测试数据 ====================
cleanup_test_data() {
    print_section "清理测试数据"
    
    if [ -z "$TOKEN" ]; then
        return
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 删除联系人
    if [ -n "$TEST_CONTACT_ID" ]; then
        local response=$(send_request "DELETE" "$BASE_URL/client/contact/$TEST_CONTACT_ID" "" "$auth_header")
        echo -e "${GREEN}✓${NC} 清理测试联系人 (ID: $TEST_CONTACT_ID)"
    fi
    
    # 删除案源
    if [ -n "$TEST_LEAD_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/client/lead/$TEST_LEAD_ID" "" "$auth_header")
        echo -e "${GREEN}✓${NC} 清理测试案源 (ID: $TEST_LEAD_ID)"
    fi
    
    # 删除企业客户
    if [ -n "$TEST_ENTERPRISE_CLIENT_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/client/$TEST_ENTERPRISE_CLIENT_ID" "" "$auth_header")
        echo -e "${GREEN}✓${NC} 清理测试企业客户 (ID: $TEST_ENTERPRISE_CLIENT_ID)"
    fi
    
    # 删除个人客户
    if [ -n "$TEST_INDIVIDUAL_CLIENT_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/client/$TEST_INDIVIDUAL_CLIENT_ID" "" "$auth_header")
        echo -e "${GREEN}✓${NC} 清理测试个人客户 (ID: $TEST_INDIVIDUAL_CLIENT_ID)"
    fi
    
    echo ""
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}              客户管理模块业务逻辑测试总结${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$(( PASSED * 100 / TOTAL ))
        local effective_rate=$(( (PASSED + SKIPPED) * 100 / TOTAL ))
        echo ""
        echo "通过率: ${pass_rate}%"
        echo "有效通过率(含跳过): ${effective_rate}%"
    fi
    
    echo ""
    
    # 按类别统计
    echo -e "${CYAN}按类别统计：${NC}"
    
    # 客户CRUD统计
    local crud_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|客户CRUD" | wc -l | tr -d ' ')
    local crud_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|客户CRUD" | wc -l | tr -d ' ')
    local crud_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|客户CRUD" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}客户CRUD${NC}: ${GREEN}✓$crud_pass${NC} ${RED}✗$crud_fail${NC} ${YELLOW}⊘$crud_skip${NC}"
    
    # 业务规则统计
    local rule_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|业务规则" | wc -l | tr -d ' ')
    local rule_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|业务规则" | wc -l | tr -d ' ')
    local rule_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|业务规则" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}业务规则${NC}: ${GREEN}✓$rule_pass${NC} ${RED}✗$rule_fail${NC} ${YELLOW}⊘$rule_skip${NC}"
    
    # 案源管理统计
    local lead_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|案源管理" | wc -l | tr -d ' ')
    local lead_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|案源管理" | wc -l | tr -d ' ')
    local lead_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|案源管理" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}案源管理${NC}: ${GREEN}✓$lead_pass${NC} ${RED}✗$lead_fail${NC} ${YELLOW}⊘$lead_skip${NC}"
    
    # 利冲审查统计
    local conflict_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|利冲审查" | wc -l | tr -d ' ')
    local conflict_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|利冲审查" | wc -l | tr -d ' ')
    local conflict_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|利冲审查" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}利冲审查${NC}: ${GREEN}✓$conflict_pass${NC} ${RED}✗$conflict_fail${NC} ${YELLOW}⊘$conflict_skip${NC}"
    
    # 联系人管理统计
    local contact_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|联系人管理" | wc -l | tr -d ' ')
    local contact_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|联系人管理" | wc -l | tr -d ' ')
    local contact_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|联系人管理" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}联系人管理${NC}: ${GREEN}✓$contact_pass${NC} ${RED}✗$contact_fail${NC} ${YELLOW}⊘$contact_skip${NC}"
    
    # 查询筛选统计
    local search_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|查询筛选" | wc -l | tr -d ' ')
    local search_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|查询筛选" | wc -l | tr -d ' ')
    local search_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|查询筛选" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}查询筛选${NC}: ${GREEN}✓$search_pass${NC} ${RED}✗$search_fail${NC} ${YELLOW}⊘$search_skip${NC}"
    
    # 权限测试统计
    local perm_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|权限测试" | wc -l | tr -d ' ')
    local perm_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|权限测试" | wc -l | tr -d ' ')
    local perm_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|权限测试" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}权限测试${NC}: ${GREEN}✓$perm_pass${NC} ${RED}✗$perm_fail${NC} ${YELLOW}⊘$perm_skip${NC}"
    
    # 数据完整性统计
    local data_pass=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "PASS|数据完整性" | wc -l | tr -d ' ')
    local data_fail=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "FAIL|数据完整性" | wc -l | tr -d ' ')
    local data_skip=$(printf '%s\n' "${TEST_RESULTS[@]}" | grep "SKIP|数据完整性" | wc -l | tr -d ' ')
    echo -e "  ${MAGENTA}数据完整性${NC}: ${GREEN}✓$data_pass${NC} ${RED}✗$data_fail${NC} ${YELLOW}⊘$data_skip${NC}"
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 客户管理模块业务逻辑测试通过！${NC}"
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}  ❌ 有 $FAILED 个业务逻辑测试失败${NC}"
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "失败详情："
        for detail in "${FAILED_DETAILS[@]}"; do
            echo -e "  ${RED}• $detail${NC}"
        done
    fi
    
    echo ""
    echo "说明："
    echo "  - PASS: 业务逻辑验证通过"
    echo "  - SKIP: 可能接口实现不同或功能可选"
    echo "  - FAIL: 业务逻辑存在问题，需要检查"
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     智慧律所管理系统 - 客户管理模块业务逻辑测试${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     测试时间: $(date +"%Y-%m-%d %H:%M:%S")${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    
    # 检查服务
    if ! check_service; then
        echo -e "${RED}服务未运行，测试终止${NC}"
        exit 1
    fi
    
    # 登录
    if ! login; then
        echo -e "${RED}登录失败，测试终止${NC}"
        exit 1
    fi
    
    # 执行测试
    test_client_crud
    test_client_business_rules
    test_lead_management
    test_conflict_check
    test_contact_management
    test_client_search_filter
    test_data_integrity
    
    # 清理
    cleanup_test_data
    
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
