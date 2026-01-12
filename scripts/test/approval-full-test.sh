#!/bin/bash

# 审批业务逻辑完整测试脚本
# 包含测试数据创建和完整审批流程验证

BASE_URL="http://localhost:8080/api"
TOKEN=""
ADMIN_USER_ID=""

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

# 测试数据存储
TEST_CONTRACT_ID=""
TEST_APPROVAL_ID=""
TEST_CLIENT_ID=""
TEST_MATTER_ID=""

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
        echo -e "${YELLOW}⊘${NC} $test_name: ${YELLOW}SKIP${NC} - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP:$test_name:$message")
    else
        echo -e "${RED}✗${NC} $test_name: ${RED}FAIL${NC}"
        if [ -n "$message" ]; then
            echo -e "  ${RED}错误: $message${NC}"
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
    local token=$4
    
    local headers=""
    if [ -n "$token" ]; then
        headers="Authorization: Bearer $token"
    fi
    
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
        echo -e "${RED}✗${NC} 后端服务未运行，请先启动后端服务"
        return 1
    fi
}

# 登录
login() {
    echo -e "${BLUE}登录管理员账号...${NC}"
    local response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓${NC} 登录成功"
        
        # 获取用户信息获取ID
        local user_response=$(send_request "GET" "$BASE_URL/auth/info" "" "$TOKEN")
        local user_body=$(echo "$user_response" | sed '$d')
        ADMIN_USER_ID=$(echo "$user_body" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
        echo "  当前用户ID: $ADMIN_USER_ID"
        return 0
    else
        echo -e "${RED}✗${NC} 登录失败"
        return 1
    fi
}

# 获取现有客户
get_existing_client() {
    echo -e "${BLUE}获取现有客户...${NC}"
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=1" "" "$TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_CLIENT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$TEST_CLIENT_ID" ]; then
            echo -e "${GREEN}✓${NC} 找到客户ID: $TEST_CLIENT_ID"
            return 0
        fi
    fi
    echo -e "${YELLOW}⊘${NC} 未找到现有客户"
    return 1
}

# 获取现有项目
get_existing_matter() {
    echo -e "${BLUE}获取现有项目...${NC}"
    local response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=1" "" "$TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_MATTER_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$TEST_MATTER_ID" ]; then
            echo -e "${GREEN}✓${NC} 找到项目ID: $TEST_MATTER_ID"
            return 0
        fi
    fi
    echo -e "${YELLOW}⊘${NC} 未找到现有项目"
    return 1
}

# ==================== 测试1: 创建合同并提交审批 ====================

test_create_contract_and_submit() {
    echo ""
    echo -e "${CYAN}=== 测试1: 创建合同并提交审批 ===${NC}"
    
    if [ -z "$TEST_CLIENT_ID" ]; then
        print_result "创建合同" "SKIP" "没有可用的客户"
        return
    fi
    
    # 创建合同
    local create_data="{
        \"name\":\"审批测试合同-$(date +%s)\",
        \"contractType\":\"SERVICE\",
        \"clientId\":$TEST_CLIENT_ID,
        \"feeType\":\"FIXED\",
        \"totalAmount\":50000,
        \"currency\":\"CNY\",
        \"signDate\":\"$(date +%Y-%m-%d)\"
    }"
    
    if [ -n "$TEST_MATTER_ID" ]; then
        create_data=$(echo "$create_data" | sed "s/}$/,\"matterId\":$TEST_MATTER_ID}/")
    fi
    
    local response=$(send_request "POST" "$BASE_URL/matter/contract" "$create_data" "$TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_CONTRACT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        print_result "创建合同" "PASS"
        echo "  合同ID: $TEST_CONTRACT_ID"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建合同" "FAIL" "$message"
        return
    fi
    
    # 提交审批（指定审批人为管理员自己）
    local submit_response=$(send_request "POST" "$BASE_URL/matter/contract/$TEST_CONTRACT_ID/submit?approverId=$ADMIN_USER_ID" "" "$TOKEN")
    local submit_code=$(echo "$submit_response" | tail -1)
    local submit_body=$(echo "$submit_response" | sed '$d')
    local submit_success=$(echo "$submit_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$submit_success" = "true" ]; then
        print_result "提交合同审批" "PASS"
    else
        local message=$(echo "$submit_body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "提交合同审批" "FAIL" "$message"
    fi
}

# ==================== 测试2: 获取待审批记录 ====================

test_get_pending_approvals() {
    echo ""
    echo -e "${CYAN}=== 测试2: 获取待审批记录 ===${NC}"
    
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/pending" "" "$TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TEST_APPROVAL_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        local count=$(echo "$body" | grep -o '"id":[0-9]*' | wc -l)
        print_result "获取待审批列表" "PASS"
        echo "  待审批数量: $count"
        if [ -n "$TEST_APPROVAL_ID" ]; then
            echo "  测试审批ID: $TEST_APPROVAL_ID"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "获取待审批列表" "FAIL" "$message"
    fi
}

# ==================== 测试3: 拒绝审批必须填写原因 ====================

test_rejection_requires_comment() {
    echo ""
    echo -e "${CYAN}=== 测试3: 拒绝审批必须填写原因 ===${NC}"
    
    if [ -z "$TEST_APPROVAL_ID" ]; then
        print_result "拒绝必须填写原因" "SKIP" "没有待审批记录"
        return
    fi
    
    # 尝试不填写原因直接拒绝
    local response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
        "{\"approvalId\":$TEST_APPROVAL_ID,\"result\":\"REJECTED\",\"comment\":\"\"}" "$TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
    
    if [ "$success" = "false" ]; then
        if echo "$message" | grep -q -E "拒绝|事由|原因"; then
            print_result "拒绝必须填写原因验证" "PASS"
        else
            print_result "拒绝必须填写原因验证" "PASS" "返回错误: $message"
        fi
    else
        print_result "拒绝必须填写原因验证" "FAIL" "未填写原因也可以拒绝"
    fi
}

# ==================== 测试4: 审批通过 ====================

test_approve() {
    echo ""
    echo -e "${CYAN}=== 测试4: 审批通过 ===${NC}"
    
    if [ -z "$TEST_APPROVAL_ID" ]; then
        print_result "审批通过" "SKIP" "没有待审批记录"
        return
    fi
    
    # 审批通过
    local response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
        "{\"approvalId\":$TEST_APPROVAL_ID,\"result\":\"APPROVED\",\"comment\":\"测试通过\"}" "$TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "审批通过操作" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "审批通过操作" "FAIL" "$message"
    fi
}

# ==================== 测试5: 重复审批验证 ====================

test_duplicate_approval() {
    echo ""
    echo -e "${CYAN}=== 测试5: 重复审批验证 ===${NC}"
    
    if [ -z "$TEST_APPROVAL_ID" ]; then
        print_result "重复审批验证" "SKIP" "没有审批记录"
        return
    fi
    
    # 尝试再次审批已处理的记录
    local response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
        "{\"approvalId\":$TEST_APPROVAL_ID,\"result\":\"APPROVED\",\"comment\":\"重复审批测试\"}" "$TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
    
    if [ "$success" = "false" ]; then
        if echo "$message" | grep -q -E "已处理|无法重复|已审批"; then
            print_result "重复审批被拒绝" "PASS"
        else
            print_result "重复审批被拒绝" "PASS" "返回错误: $message"
        fi
    else
        print_result "重复审批被拒绝" "FAIL" "已处理的审批竟然可以再次审批"
    fi
}

# ==================== 测试6: 合同状态变更验证 ====================

test_contract_status_change() {
    echo ""
    echo -e "${CYAN}=== 测试6: 合同状态变更验证 ===${NC}"
    
    if [ -z "$TEST_CONTRACT_ID" ]; then
        print_result "合同状态变更" "SKIP" "没有测试合同"
        return
    fi
    
    # 查询合同状态
    local response=$(send_request "GET" "$BASE_URL/matter/contract/$TEST_CONTRACT_ID" "" "$TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        # 只获取第一个status字段（合同状态）
        local status=$(echo "$body" | grep -o '"status":"[^"]*' | head -1 | cut -d'"' -f4)
        echo "  合同当前状态: $status"
        
        if [ "$status" = "ACTIVE" ]; then
            print_result "合同状态变为ACTIVE" "PASS"
        else
            print_result "合同状态变为ACTIVE" "FAIL" "状态为 $status，期望 ACTIVE"
        fi
    else
        print_result "合同状态变更验证" "SKIP" "无法获取合同状态"
    fi
}

# ==================== 测试7: 审批历史记录 ====================

test_approval_history() {
    echo ""
    echo -e "${CYAN}=== 测试7: 审批历史记录 ===${NC}"
    
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/my-history" "" "$TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        local count=$(echo "$body" | grep -o '"id":[0-9]*' | wc -l)
        print_result "审批历史记录" "PASS"
        echo "  历史记录数量: $count"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "审批历史记录" "FAIL" "$message"
    fi
}

# ==================== 测试8: 我发起的审批 ====================

test_my_initiated() {
    echo ""
    echo -e "${CYAN}=== 测试8: 我发起的审批 ===${NC}"
    
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/my-initiated" "" "$TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        local count=$(echo "$body" | grep -o '"id":[0-9]*' | wc -l)
        print_result "我发起的审批" "PASS"
        echo "  发起数量: $count"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "我发起的审批" "FAIL" "$message"
    fi
}

# ==================== 测试9: 审批详情 ====================

test_approval_detail() {
    echo ""
    echo -e "${CYAN}=== 测试9: 审批详情 ===${NC}"
    
    if [ -z "$TEST_APPROVAL_ID" ]; then
        print_result "审批详情" "SKIP" "没有审批记录"
        return
    fi
    
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/$TEST_APPROVAL_ID" "" "$TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        local businessType=$(echo "$body" | grep -o '"businessType":"[^"]*' | cut -d'"' -f4)
        print_result "审批详情查询" "PASS"
        echo "  业务类型: $businessType"
        echo "  审批状态: $status"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "审批详情查询" "FAIL" "$message"
    fi
}

# ==================== 测试10: 创建第二个合同测试拒绝流程 ====================

test_rejection_flow() {
    echo ""
    echo -e "${CYAN}=== 测试10: 拒绝审批流程 ===${NC}"
    
    if [ -z "$TEST_CLIENT_ID" ]; then
        print_result "拒绝流程测试" "SKIP" "没有可用的客户"
        return
    fi
    
    # 创建新合同
    local create_data="{
        \"name\":\"拒绝测试合同-$(date +%s)\",
        \"contractType\":\"SERVICE\",
        \"clientId\":$TEST_CLIENT_ID,
        \"feeType\":\"FIXED\",
        \"totalAmount\":30000,
        \"currency\":\"CNY\",
        \"signDate\":\"$(date +%Y-%m-%d)\"
    }"
    
    local response=$(send_request "POST" "$BASE_URL/matter/contract" "$create_data" "$TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "创建拒绝测试合同" "FAIL" "无法创建合同"
        return
    fi
    
    local contract_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  创建合同ID: $contract_id"
    
    # 提交审批
    local submit_response=$(send_request "POST" "$BASE_URL/matter/contract/$contract_id/submit?approverId=$ADMIN_USER_ID" "" "$TOKEN")
    local submit_body=$(echo "$submit_response" | sed '$d')
    local submit_success=$(echo "$submit_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$submit_success" != "true" ]; then
        print_result "提交拒绝测试合同" "FAIL" "无法提交审批"
        return
    fi
    
    # 获取新的待审批记录
    sleep 1
    local pending_response=$(send_request "GET" "$BASE_URL/workbench/approval/pending" "" "$TOKEN")
    local pending_body=$(echo "$pending_response" | sed '$d')
    local new_approval_id=$(echo "$pending_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -z "$new_approval_id" ]; then
        print_result "获取新待审批记录" "FAIL" "未找到待审批记录"
        return
    fi
    
    echo "  新审批ID: $new_approval_id"
    
    # 拒绝审批（填写原因）
    local reject_response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
        "{\"approvalId\":$new_approval_id,\"result\":\"REJECTED\",\"comment\":\"测试拒绝，合同金额需要调整\"}" "$TOKEN")
    local reject_body=$(echo "$reject_response" | sed '$d')
    local reject_success=$(echo "$reject_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$reject_success" = "true" ]; then
        print_result "拒绝审批操作" "PASS"
        
        # 检查合同状态是否变为REJECTED
        sleep 1
        local contract_response=$(send_request "GET" "$BASE_URL/matter/contract/$contract_id" "" "$TOKEN")
        local contract_body=$(echo "$contract_response" | sed '$d')
        # 只获取第一个status字段（合同状态）
        local contract_status=$(echo "$contract_body" | grep -o '"status":"[^"]*' | head -1 | cut -d'"' -f4)
        
        if [ "$contract_status" = "REJECTED" ]; then
            print_result "合同状态变为REJECTED" "PASS"
        else
            print_result "合同状态变为REJECTED" "FAIL" "状态为 $contract_status"
        fi
    else
        local message=$(echo "$reject_body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "拒绝审批操作" "FAIL" "$message"
    fi
}

# ==================== 打印测试总结 ====================

print_summary() {
    echo ""
    echo "=========================================="
    echo -e "${BLUE}审批业务逻辑完整测试报告${NC}"
    echo "=========================================="
    echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "测试统计:"
    echo "  总测试数: $TOTAL"
    echo -e "  ${GREEN}通过: $PASSED${NC}"
    echo -e "  ${RED}失败: $FAILED${NC}"
    echo -e "  ${YELLOW}跳过: $SKIPPED${NC}"
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        echo "  通过率: ${pass_rate}%"
    fi
    
    echo ""
    echo "详细结果:"
    for result in "${TEST_RESULTS[@]}"; do
        local status=$(echo "$result" | cut -d':' -f1)
        local name=$(echo "$result" | cut -d':' -f2)
        local message=$(echo "$result" | cut -d':' -f3-)
        
        if [ "$status" = "PASS" ]; then
            echo -e "  ${GREEN}✓${NC} $name"
        elif [ "$status" = "SKIP" ]; then
            echo -e "  ${YELLOW}⊘${NC} $name ${YELLOW}($message)${NC}"
        else
            echo -e "  ${RED}✗${NC} $name"
            if [ -n "$message" ]; then
                echo -e "    ${RED}$message${NC}"
            fi
        fi
    done
    
    echo ""
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ 所有审批逻辑测试通过！${NC}"
        return 0
    else
        echo -e "${RED}✗ 有 $FAILED 个测试失败${NC}"
        return 1
    fi
}

# ==================== 主函数 ====================

main() {
    echo "=========================================="
    echo -e "${BLUE}智慧律所管理系统 - 审批业务逻辑完整测试${NC}"
    echo "=========================================="
    echo ""
    
    # 检查服务
    if ! check_service; then
        exit 1
    fi
    
    # 登录
    if ! login; then
        exit 1
    fi
    
    echo ""
    echo -e "${BLUE}准备测试数据...${NC}"
    
    # 获取测试所需的现有数据
    get_existing_client
    get_existing_matter
    
    # 执行测试
    test_create_contract_and_submit
    test_get_pending_approvals
    test_rejection_requires_comment
    test_approve
    test_duplicate_approval
    test_contract_status_change
    test_approval_history
    test_my_initiated
    test_approval_detail
    test_rejection_flow
    
    # 打印总结
    print_summary
}

# 运行主函数
main
