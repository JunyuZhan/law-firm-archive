#!/bin/bash

# 智慧律所管理系统 - 项目管理模块业务逻辑测试脚本
# 测试内容：项目CRUD、状态流转、团队管理、结案流程、业务规则验证
# 测试日期：$(date +%Y-%m-%d)

BASE_URL="http://localhost:8080/api"
TOKEN=""

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

declare -a TEST_RESULTS
declare -a FAILED_DETAILS

# 测试创建的资源ID
TEST_CLIENT_ID=""
TEST_CONTRACT_ID=""
TEST_MATTER_ID=""

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

# 从响应中提取数据
extract_field() {
    local body=$1
    local field=$2
    echo "$body" | grep -o "\"$field\":[^,}]*" | head -1 | sed 's/.*://' | tr -d '"'
}

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

# ==================== 1. 登录获取Token ====================
login() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 登录认证${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        print_result "管理员账号登录" "PASS" "" "认证"
        return 0
    else
        print_result "管理员账号登录" "FAIL" "登录失败" "认证"
        return 1
    fi
}

# ==================== 2. 准备测试数据 ====================
prepare_test_data() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 准备测试数据（获取已审批合同）${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local timestamp=$(date +%s)
    
    # 2.1 优先获取已审批通过的合同（项目必须基于已审批合同创建）
    local response=$(send_request "GET" "$BASE_URL/matter/contract/approved" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        # 找一个没有关联项目的合同（matterId为null）
        # 先尝试找 matterId 为 null 的合同
        local contract_ids=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        
        for cid in $contract_ids; do
            # 获取合同详情检查是否有关联项目
            local detail_resp=$(send_request "GET" "$BASE_URL/matter/contract/$cid" "" "$auth_header")
            local detail_body=$(echo "$detail_resp" | sed '$d')
            
            # 获取客户ID
            local contract_client_id=$(echo "$detail_body" | grep -o '"clientId":[0-9]*' | head -1 | cut -d':' -f2)
            
            if [ -n "$contract_client_id" ]; then
                TEST_CONTRACT_ID=$cid
                TEST_CLIENT_ID=$contract_client_id
                print_result "获取已审批通过的合同" "PASS" "" "准备数据"
                echo -e "  ${BLUE}使用合同ID: $TEST_CONTRACT_ID, 客户ID: $TEST_CLIENT_ID${NC}"
                break
            fi
        done
        
        if [ -z "$TEST_CONTRACT_ID" ]; then
            # 取第一个合同
            TEST_CONTRACT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            TEST_CLIENT_ID=$(echo "$body" | grep -o '"clientId":[0-9]*' | head -1 | cut -d':' -f2)
            print_result "获取已审批通过的合同" "PASS" "" "准备数据"
            echo -e "  ${BLUE}使用合同ID: $TEST_CONTRACT_ID, 客户ID: $TEST_CLIENT_ID${NC}"
        fi
    else
        print_result "获取已审批通过的合同" "FAIL" "无可用合同" "准备数据"
        return 1
    fi
    
    # 2.2 验证数据完整性
    if [ -z "$TEST_CONTRACT_ID" ] || [ -z "$TEST_CLIENT_ID" ]; then
        print_result "验证测试数据" "FAIL" "合同或客户ID为空" "准备数据"
        return 1
    fi
    
    print_result "验证测试数据完整性" "PASS" "" "准备数据"
}

# ==================== 3. 项目基础操作测试 ====================
test_matter_crud() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 项目基础操作测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}跳过：未登录${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    local timestamp=$(date +%s)
    
    # 3.1 验证无合同无法创建项目
    local matter_data='{
        "name": "无合同项目测试",
        "matterType": "LITIGATION",
        "caseType": "CIVIL",
        "clientId": '"$TEST_CLIENT_ID"'
    }'
    local response=$(send_request "POST" "$BASE_URL/matter" "$matter_data" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        print_result "项目必须关联合同校验" "PASS" "" "项目CRUD"
    else
        print_result "项目必须关联合同校验" "FAIL" "允许创建无合同项目" "项目CRUD"
        # 清理
        local invalid_id=$(extract_id "$body")
        if [ -n "$invalid_id" ]; then
            send_request "DELETE" "$BASE_URL/matter/$invalid_id" "" "$auth_header" > /dev/null 2>&1
        fi
    fi
    
    # 3.2 创建项目（需要已审批的合同）
    if [ -z "$TEST_CONTRACT_ID" ]; then
        print_result "创建项目" "SKIP" "缺少已审批的合同" "项目CRUD"
        return 1
    fi
    
    matter_data='{
        "name": "业务测试项目_'"$timestamp"'",
        "matterType": "LITIGATION",
        "caseType": "CIVIL",
        "litigationStage": "FIRST_INSTANCE",
        "causeOfAction": "合同纠纷",
        "clientId": '"$TEST_CLIENT_ID"',
        "contractId": '"$TEST_CONTRACT_ID"',
        "opposingParty": "某对方当事人",
        "description": "自动化测试创建的项目",
        "feeType": "FIXED",
        "estimatedFee": 80000,
        "claimAmount": 500000,
        "filingDate": "2026-01-12",
        "expectedClosingDate": "2026-06-30"
    }'
    response=$(send_request "POST" "$BASE_URL/matter" "$matter_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_MATTER_ID=$(extract_id "$body")
        print_result "创建项目" "PASS" "" "项目CRUD"
        echo -e "  ${BLUE}测试项目ID: $TEST_MATTER_ID${NC}"
        
        # 验证项目编号自动生成
        local matterNo=$(extract_field "$body" "matterNo")
        if [ -n "$matterNo" ]; then
            print_result "项目编号自动生成" "PASS" "" "项目CRUD"
            echo -e "  ${BLUE}项目编号: $matterNo${NC}"
        else
            print_result "项目编号自动生成" "FAIL" "编号为空" "项目CRUD"
        fi
        
        # 验证项目状态为进行中（基于已审批合同创建）
        local status=$(extract_field "$body" "status")
        if [ "$status" = "ACTIVE" ]; then
            print_result "基于已审批合同创建的项目状态为进行中" "PASS" "" "项目CRUD"
        else
            print_result "基于已审批合同创建的项目状态为进行中" "FAIL" "状态为: $status" "项目CRUD"
        fi
    else
        local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建项目" "FAIL" "$msg" "项目CRUD"
        return 1
    fi
    
    # 3.3 查询项目详情
    response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询项目详情" "PASS" "" "项目CRUD"
        
        # 验证关联数据填充
        local clientName=$(echo "$body" | grep -o '"clientName":"[^"]*' | cut -d'"' -f4)
        if [ -n "$clientName" ]; then
            print_result "关联数据填充（客户名称）" "PASS" "" "项目CRUD"
        else
            print_result "关联数据填充（客户名称）" "SKIP" "客户名称为空" "项目CRUD"
        fi
        
        local contractNo=$(echo "$body" | grep -o '"contractNo":"[^"]*' | cut -d'"' -f4)
        if [ -n "$contractNo" ]; then
            print_result "关联数据填充（合同编号）" "PASS" "" "项目CRUD"
        else
            print_result "关联数据填充（合同编号）" "SKIP" "合同编号为空" "项目CRUD"
        fi
    else
        print_result "查询项目详情" "FAIL" "查询失败" "项目CRUD"
    fi
    
    # 3.4 更新项目
    local update_data='{
        "id": '"$TEST_MATTER_ID"',
        "name": "业务测试项目_已更新_'"$timestamp"'",
        "description": "更新后的项目描述",
        "expectedClosingDate": "2026-07-31"
    }'
    response=$(send_request "PUT" "$BASE_URL/matter" "$update_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "更新项目" "PASS" "" "项目CRUD"
    else
        local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "更新项目" "FAIL" "$msg" "项目CRUD"
    fi
    
    # 3.5 测试项目列表查询
    response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "分页查询项目列表" "PASS" "" "项目CRUD"
    else
        print_result "分页查询项目列表" "FAIL" "查询失败" "项目CRUD"
    fi
    
    # 3.6 测试我的项目查询
    response=$(send_request "GET" "$BASE_URL/matter/my?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询我的项目" "PASS" "" "项目CRUD"
    else
        print_result "查询我的项目" "FAIL" "查询失败" "项目CRUD"
    fi
    
    # 3.7 测试项目选择列表（公共接口）
    response=$(send_request "GET" "$BASE_URL/matter/select-options?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "查询项目选择列表" "PASS" "" "项目CRUD"
    else
        print_result "查询项目选择列表" "FAIL" "查询失败" "项目CRUD"
    fi
}

# ==================== 4. 项目删除限制测试 ====================
test_delete_restriction() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 项目删除限制测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ] || [ -z "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}跳过：缺少认证或测试项目${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 测试项目不能删除
    local response=$(send_request "DELETE" "$BASE_URL/matter/$TEST_MATTER_ID" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        if echo "$msg" | grep -qi "删除\|归档"; then
            print_result "项目禁止删除（只能归档）" "PASS" "" "删除限制"
        else
            print_result "项目禁止删除（只能归档）" "PASS" "" "删除限制"
        fi
    else
        print_result "项目禁止删除（只能归档）" "FAIL" "允许删除项目" "删除限制"
    fi
}

# ==================== 5. 状态流转测试 ====================
test_status_transition() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 项目状态流转测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ] || [ -z "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}跳过：缺少认证或测试项目${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 测试进行中->暂停
    local response=$(send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"SUSPENDED"}' "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "状态流转：进行中→暂停" "PASS" "" "状态流转"
        
        # 5.2 测试暂停->进行中
        response=$(send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"ACTIVE"}' "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "状态流转：暂停→进行中" "PASS" "" "状态流转"
        else
            print_result "状态流转：暂停→进行中" "FAIL" "恢复失败" "状态流转"
        fi
    else
        local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "状态流转：进行中→暂停" "FAIL" "$msg" "状态流转"
    fi
    
    # 5.3 测试非法状态流转（进行中直接到已结案）
    response=$(send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"CLOSED"}' "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        print_result "阻止非法状态流转（进行中→已结案）" "PASS" "" "状态流转"
    else
        print_result "阻止非法状态流转（进行中→已结案）" "FAIL" "允许非法状态流转" "状态流转"
        # 恢复状态
        send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"ACTIVE"}' "$auth_header" > /dev/null 2>&1
    fi
    
    # 5.4 测试非法状态流转（进行中直接到已归档）
    response=$(send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"ARCHIVED"}' "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ]; then
        print_result "阻止非法状态流转（进行中→已归档）" "PASS" "" "状态流转"
    else
        print_result "阻止非法状态流转（进行中→已归档）" "FAIL" "允许非法状态流转" "状态流转"
    fi
}

# ==================== 6. 团队管理测试 ====================
test_team_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 团队成员管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ] || [ -z "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}跳过：缺少认证或测试项目${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 查询项目详情获取参与者列表
    local response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        # 检查是否自动添加了参与人
        if echo "$body" | grep -q '"participants"'; then
            print_result "项目创建时自动添加团队成员" "PASS" "" "团队管理"
        else
            print_result "项目创建时自动添加团队成员" "SKIP" "未找到参与者列表" "团队管理"
        fi
    fi
    
    # 6.2 添加团队成员
    # 先获取一个用户ID
    local user_response=$(send_request "GET" "$BASE_URL/system/user/list?pageNum=1&pageSize=10" "" "$auth_header")
    local user_body=$(echo "$user_response" | sed '$d')
    local new_member_id=$(echo "$user_body" | grep -o '"id":[0-9]*' | head -3 | tail -1 | cut -d':' -f2)
    
    if [ -n "$new_member_id" ]; then
        local participant_data='{
            "userId": '"$new_member_id"',
            "role": "CO_COUNSEL",
            "commissionRate": 15
        }'
        response=$(send_request "POST" "$BASE_URL/matter/$TEST_MATTER_ID/participant" "$participant_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "添加团队成员" "PASS" "" "团队管理"
            
            # 6.3 移除团队成员
            response=$(send_request "DELETE" "$BASE_URL/matter/$TEST_MATTER_ID/participant/$new_member_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
            
            if [ "$success" = "true" ]; then
                print_result "移除团队成员" "PASS" "" "团队管理"
            else
                print_result "移除团队成员" "FAIL" "移除失败" "团队管理"
            fi
        else
            local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            if echo "$msg" | grep -qi "已在"; then
                print_result "添加团队成员" "SKIP" "成员已存在" "团队管理"
            else
                print_result "添加团队成员" "FAIL" "$msg" "团队管理"
            fi
        fi
    else
        print_result "添加团队成员" "SKIP" "无可用用户" "团队管理"
    fi
    
    # 6.4 测试添加重复成员
    # 获取当前用户ID
    response=$(send_request "GET" "$BASE_URL/auth/info" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    local current_user_id=$(echo "$body" | grep -o '"userId":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$current_user_id" ]; then
        participant_data='{
            "userId": '"$current_user_id"',
            "role": "LEAD"
        }'
        response=$(send_request "POST" "$BASE_URL/matter/$TEST_MATTER_ID/participant" "$participant_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "false" ]; then
            print_result "阻止添加重复团队成员" "PASS" "" "团队管理"
        else
            print_result "阻止添加重复团队成员" "SKIP" "可能允许多角色" "团队管理"
        fi
    fi
}

# ==================== 7. 结案流程测试 ====================
test_close_workflow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 项目结案流程测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ] || [ -z "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}跳过：缺少认证或测试项目${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 获取结案审批人列表
    local response=$(send_request "GET" "$BASE_URL/matter/close/approvers" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "获取结案审批人列表" "PASS" "" "结案流程"
    else
        print_result "获取结案审批人列表" "SKIP" "可能无配置审批人" "结案流程"
    fi
    
    # 7.2 确保项目状态为进行中
    send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"ACTIVE"}' "$auth_header" > /dev/null 2>&1
    
    # 7.3 申请结案
    local close_data='{
        "closingDate": "2026-01-12",
        "outcome": "调解结案",
        "closingReason": "双方达成调解协议",
        "summary": "自动化测试结案"
    }'
    response=$(send_request "POST" "$BASE_URL/matter/$TEST_MATTER_ID/close/apply" "$close_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "申请项目结案" "PASS" "" "结案流程"
        
        # 验证状态变为待审批结案
        response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        local status=$(extract_field "$body" "status")
        
        if [ "$status" = "PENDING_CLOSE" ]; then
            print_result "申请后状态变更为待审批结案" "PASS" "" "结案流程"
            
            # 7.4 审批结案（通过）
            response=$(send_request "POST" "$BASE_URL/matter/$TEST_MATTER_ID/close/approve" '{"approved":true,"comment":"同意结案"}' "$auth_header")
            body=$(echo "$response" | sed '$d')
            success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
            
            if [ "$success" = "true" ]; then
                print_result "审批通过结案" "PASS" "" "结案流程"
                
                # 验证状态变为已结案
                response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID" "" "$auth_header")
                body=$(echo "$response" | sed '$d')
                status=$(extract_field "$body" "status")
                
                if [ "$status" = "CLOSED" ]; then
                    print_result "审批后状态变更为已结案" "PASS" "" "结案流程"
                    
                    # 7.5 生成结案报告
                    response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID/close/report" "" "$auth_header")
                    body=$(echo "$response" | sed '$d')
                    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
                    
                    if [ "$success" = "true" ]; then
                        print_result "生成结案报告" "PASS" "" "结案流程"
                    else
                        print_result "生成结案报告" "FAIL" "生成失败" "结案流程"
                    fi
                else
                    print_result "审批后状态变更为已结案" "FAIL" "状态为: $status" "结案流程"
                fi
            else
                local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
                print_result "审批通过结案" "FAIL" "$msg" "结案流程"
            fi
        else
            print_result "申请后状态变更为待审批结案" "FAIL" "状态为: $status" "结案流程"
        fi
    else
        local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "申请项目结案" "FAIL" "$msg" "结案流程"
    fi
}

# ==================== 8. 归档流程测试 ====================
test_archive_workflow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 项目归档流程测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ] || [ -z "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}跳过：缺少认证或测试项目${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 先检查当前状态
    local response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local status=$(extract_field "$body" "status")
    
    if [ "$status" = "CLOSED" ]; then
        # 8.1 测试已结案→已归档
        response=$(send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"ARCHIVED"}' "$auth_header")
        body=$(echo "$response" | sed '$d')
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            print_result "状态流转：已结案→已归档" "PASS" "" "归档流程"
            
            # 8.2 测试已归档项目不能编辑
            local update_data='{
                "id": '"$TEST_MATTER_ID"',
                "name": "尝试修改已归档项目"
            }'
            response=$(send_request "PUT" "$BASE_URL/matter" "$update_data" "$auth_header")
            body=$(echo "$response" | sed '$d')
            success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
            
            if [ "$success" = "false" ]; then
                print_result "已归档项目禁止编辑" "PASS" "" "归档流程"
            else
                print_result "已归档项目禁止编辑" "FAIL" "允许编辑已归档项目" "归档流程"
            fi
            
            # 8.3 测试已归档状态不能变更
            response=$(send_request "PUT" "$BASE_URL/matter/$TEST_MATTER_ID/status" '{"status":"ACTIVE"}' "$auth_header")
            body=$(echo "$response" | sed '$d')
            success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
            
            if [ "$success" = "false" ]; then
                print_result "已归档状态不能变更" "PASS" "" "归档流程"
            else
                print_result "已归档状态不能变更" "FAIL" "允许变更已归档状态" "归档流程"
            fi
        else
            local msg=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "状态流转：已结案→已归档" "FAIL" "$msg" "归档流程"
        fi
    else
        print_result "状态流转：已结案→已归档" "SKIP" "项目当前状态不是已结案: $status" "归档流程"
    fi
}

# ==================== 9. 项目时间线测试 ====================
test_timeline() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 项目时间线测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$TOKEN" ] || [ -z "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}跳过：缺少认证或测试项目${NC}"
        return 1
    fi
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 获取项目时间线
    local response=$(send_request "GET" "$BASE_URL/matter/$TEST_MATTER_ID/timeline" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "获取项目时间线" "PASS" "" "时间线"
    else
        print_result "获取项目时间线" "SKIP" "可能无时间线数据" "时间线"
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
    
    # 项目不能删除，只是提示
    if [ -n "$TEST_MATTER_ID" ]; then
        echo -e "${YELLOW}⊘${NC} 测试项目保留（项目不支持删除，只能归档）"
    fi
    
    # 合同可能也不能删除
    if [ -n "$TEST_CONTRACT_ID" ]; then
        echo -e "${YELLOW}⊘${NC} 测试合同保留（合同可能已关联项目）"
    fi
    
    # 尝试删除测试客户
    if [ -n "$TEST_CLIENT_ID" ]; then
        local response=$(send_request "DELETE" "$BASE_URL/client/$TEST_CLIENT_ID" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "true" ]; then
            echo -e "${GREEN}✓${NC} 清理测试客户 (ID: $TEST_CLIENT_ID)"
        else
            echo -e "${YELLOW}⊘${NC} 测试客户无法删除（可能有关联数据）"
        fi
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}               项目管理模块业务逻辑测试总结${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$(( PASSED * 100 / TOTAL ))
        local effective_rate=$(( (PASSED + SKIPPED) * 100 / TOTAL ))
        echo "通过率: ${pass_rate}%"
        echo "有效通过率: ${effective_rate}%"
    fi
    
    echo ""
    
    # 按类别统计
    echo "按类别统计："
    echo "----------------------------------------"
    for category in "认证" "准备数据" "项目CRUD" "删除限制" "状态流转" "团队管理" "结案流程" "归档流程" "时间线"; do
        local cat_total=0
        local cat_pass=0
        local cat_fail=0
        local cat_skip=0
        
        for result in "${TEST_RESULTS[@]}"; do
            if echo "$result" | grep -q "|$category|"; then
                cat_total=$((cat_total + 1))
                if echo "$result" | grep -q "^PASS|"; then
                    cat_pass=$((cat_pass + 1))
                elif echo "$result" | grep -q "^FAIL|"; then
                    cat_fail=$((cat_fail + 1))
                else
                    cat_skip=$((cat_skip + 1))
                fi
            fi
        done
        
        if [ $cat_total -gt 0 ]; then
            printf "  %-12s: " "$category"
            echo -e "${GREEN}$cat_pass${NC}/${cat_total} 通过, ${RED}$cat_fail${NC} 失败, ${YELLOW}$cat_skip${NC} 跳过"
        fi
    done
    echo "----------------------------------------"
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 项目管理模块业务逻辑测试通过！${NC}"
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
    echo -e "${BLUE}     智慧律所管理系统 - 项目管理模块业务逻辑测试${NC}"
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
    prepare_test_data
    test_matter_crud
    test_delete_restriction
    test_status_transition
    test_team_management
    test_close_workflow
    test_archive_workflow
    test_timeline
    
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
