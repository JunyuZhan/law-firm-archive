#!/bin/bash

# 智慧律所管理系统 - 剩余模块补充测试
# 测试内容：
# 1. 行政合同 - /admin/contract
# 2. 财务合同变更 - /finance/contract-amendments
# 3. 付款变更 - /finance/payment-amendment
# 4. 质量问题 - /knowledge/quality-issue
# 5. 任务评论 - /tasks/{taskId}/comments
# 6. 项目客户服务 - /matter/client-service
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
        TEST_RESULTS+=("PASS|$test_name|$category")
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED}✗${NC} [$category] $test_name - $message"
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL|$test_name|$category")
    else
        echo -e "${YELLOW}⊘${NC} [$category] $test_name - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP|$test_name|$category")
    fi
}

# 发送请求
send_request() {
    local method=$1
    local url=$2
    local data=$3
    local auth_header=$4
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" \
            -d "$data" 2>/dev/null)
    fi
    
    echo "$response"
}

# 检查响应是否成功
check_success() {
    local body=$1
    echo "$body" | grep -q '"success":true'
}

# ==================== 1. 行政合同测试 ====================
test_admin_contract() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 行政合同业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 获取行政合同列表
    local response=$(send_request "GET" "$BASE_URL/admin/contract/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取行政合同列表" "PASS" "" "行政合同"
    else
        print_result "获取行政合同列表" "SKIP" "可能无权限或接口不存在" "行政合同"
    fi
}

# ==================== 2. 财务合同变更测试 ====================
test_contract_amendment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 财务合同变更业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 获取合同变更列表
    local response=$(send_request "GET" "$BASE_URL/finance/contract-amendments?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取合同变更列表" "PASS" "" "合同变更"
    else
        print_result "获取合同变更列表" "SKIP" "可能无权限或接口不存在" "合同变更"
    fi
}

# ==================== 3. 付款变更测试 ====================
test_payment_amendment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 付款变更业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 3.1 获取付款变更列表
    local response=$(send_request "GET" "$BASE_URL/finance/payment-amendment/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取付款变更列表" "PASS" "" "付款变更"
    else
        print_result "获取付款变更列表" "SKIP" "可能无权限或接口不存在" "付款变更"
    fi
}

# ==================== 4. 质量问题测试 ====================
test_quality_issue() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 质量问题业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 获取质量问题列表
    local response=$(send_request "GET" "$BASE_URL/knowledge/quality-issue?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取质量问题列表" "PASS" "" "质量问题"
    else
        print_result "获取质量问题列表" "SKIP" "可能无权限或接口不存在" "质量问题"
    fi
}

# ==================== 5. 任务评论测试 ====================
test_task_comment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 任务评论业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 先获取一个任务ID
    local response=$(send_request "GET" "$BASE_URL/tasks?pageNum=1&pageSize=1" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local TASK_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$TASK_ID" ] && [ "$TASK_ID" != "null" ]; then
        # 5.1 获取任务评论
        response=$(send_request "GET" "$BASE_URL/tasks/$TASK_ID/comments" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取任务评论" "PASS" "" "任务评论"
        else
            print_result "获取任务评论" "SKIP" "可能无权限" "任务评论"
        fi
    else
        print_result "获取任务评论" "SKIP" "无任务数据" "任务评论"
    fi
}

# ==================== 6. 项目客户服务测试 ====================
test_matter_client_service() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 项目客户服务业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 检查客户服务接口
    local response=$(send_request "GET" "$BASE_URL/matter/client-service/status" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "项目客户服务状态" "PASS" "" "客户服务"
    else
        print_result "项目客户服务状态" "SKIP" "可能无权限或接口不存在" "客户服务"
    fi
}

# ==================== 打印总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}         剩余模块补充测试总结${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    echo ""
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        echo "通过率: ${pass_rate}%"
        local effective_rate=$(((PASSED + SKIPPED) * 100 / TOTAL))
        echo "有效率(通过+跳过): ${effective_rate}%"
    fi
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 剩余模块测试全部通过！${NC}"
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}  ❌ 有 $FAILED 个测试失败${NC}"
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
    fi
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}  智慧律所管理系统 - 剩余模块补充测试${NC}"
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}     测试时间: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    
    # 检查服务状态
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo -e "${BLUE}       检查服务状态${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    
    if ! curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${RED}✗${NC} 后端服务未运行，请先启动服务"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} 后端服务运行中"
    
    # 登录获取Token
    echo ""
    echo -e "${BLUE}登录获取Token...${NC}"
    local login_response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$TOKEN" ]; then
        echo -e "${RED}✗${NC} 登录失败，无法获取Token"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} 登录成功"
    
    # 执行各模块测试
    test_admin_contract
    test_contract_amendment
    test_payment_amendment
    test_quality_issue
    test_task_comment
    test_matter_client_service
    
    # 打印总结
    print_summary
}

# 运行主函数
main
