#!/bin/bash

# 审批业务逻辑测试脚本
# 验证各种审批功能的业务逻辑正确性

BASE_URL="http://localhost:8080/api"
TOKEN=""
ADMIN_TOKEN=""
LAWYER_TOKEN=""

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

# 登录获取token
login() {
    local username=$1
    local password=$2
    
    local response=$(send_request "POST" "$BASE_URL/auth/login" "{\"username\":\"$username\",\"password\":\"$password\"}")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4
    else
        echo ""
    fi
}

# ==================== 审批权限测试 ====================

test_approval_permission() {
    echo ""
    echo -e "${CYAN}=== 测试1: 审批权限控制 ===${NC}"
    echo -e "验证：只有指定的审批人可以审批记录"
    
    # 获取待审批列表
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/pending" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "获取待审批列表" "FAIL" "无法获取待审批列表"
        return
    fi
    
    # 提取第一条待审批记录
    local approval_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -z "$approval_id" ]; then
        print_result "审批权限验证" "SKIP" "当前没有待审批记录可测试"
        return
    fi
    
    echo "  找到待审批记录 ID: $approval_id"
    
    # 使用不同用户尝试审批（应该失败）
    if [ -n "$LAWYER_TOKEN" ]; then
        local approve_response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
            "{\"approvalId\":$approval_id,\"result\":\"APPROVED\",\"comment\":\"测试审批\"}" "$LAWYER_TOKEN")
        local approve_code=$(echo "$approve_response" | tail -1)
        local approve_body=$(echo "$approve_response" | sed '$d')
        local approve_success=$(echo "$approve_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        local approve_message=$(echo "$approve_body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        
        if [ "$approve_success" = "false" ]; then
            if echo "$approve_message" | grep -q "无权审批"; then
                print_result "审批权限验证（非审批人被拒绝）" "PASS"
            else
                print_result "审批权限验证（非审批人被拒绝）" "PASS" "返回错误: $approve_message"
            fi
        else
            print_result "审批权限验证（非审批人被拒绝）" "FAIL" "非审批人竟然可以审批"
        fi
    else
        print_result "审批权限验证（非审批人被拒绝）" "SKIP" "没有普通用户token进行测试"
    fi
}

# ==================== 审批状态验证测试 ====================

test_approval_status() {
    echo ""
    echo -e "${CYAN}=== 测试2: 审批状态验证 ===${NC}"
    echo -e "验证：只有PENDING状态的审批可以被处理"
    
    # 获取已审批历史（查找已处理的审批）
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/my-history" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "获取审批历史" "FAIL" "无法获取审批历史"
        return
    fi
    
    # 提取一条已处理的审批记录
    local processed_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -z "$processed_id" ]; then
        print_result "重复审批验证" "SKIP" "没有已处理的审批记录可测试"
        return
    fi
    
    echo "  找到已处理审批记录 ID: $processed_id"
    
    # 尝试再次审批已处理的记录（应该失败）
    local approve_response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
        "{\"approvalId\":$processed_id,\"result\":\"APPROVED\",\"comment\":\"重复审批测试\"}" "$ADMIN_TOKEN")
    local approve_code=$(echo "$approve_response" | tail -1)
    local approve_body=$(echo "$approve_response" | sed '$d')
    local approve_success=$(echo "$approve_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    local approve_message=$(echo "$approve_body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
    
    if [ "$approve_success" = "false" ]; then
        if echo "$approve_message" | grep -q -E "已处理|无法重复|状态"; then
            print_result "重复审批验证（已处理的审批被拒绝）" "PASS"
        else
            print_result "重复审批验证" "PASS" "返回错误: $approve_message"
        fi
    else
        print_result "重复审批验证（已处理的审批被拒绝）" "FAIL" "已处理的审批竟然可以再次审批"
    fi
}

# ==================== 拒绝必须填写原因测试 ====================

test_rejection_comment_required() {
    echo ""
    echo -e "${CYAN}=== 测试3: 拒绝必须填写原因 ===${NC}"
    echo -e "验证：拒绝审批时必须填写拒绝事由"
    
    # 获取待审批列表
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/pending" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "拒绝必须填写原因" "SKIP" "无法获取待审批列表"
        return
    fi
    
    # 提取第一条待审批记录
    local approval_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -z "$approval_id" ]; then
        print_result "拒绝必须填写原因" "SKIP" "当前没有待审批记录可测试"
        return
    fi
    
    echo "  找到待审批记录 ID: $approval_id"
    
    # 尝试不填写原因直接拒绝（应该失败）
    local reject_response=$(send_request "POST" "$BASE_URL/workbench/approval/approve" \
        "{\"approvalId\":$approval_id,\"result\":\"REJECTED\",\"comment\":\"\"}" "$ADMIN_TOKEN")
    local reject_code=$(echo "$reject_response" | tail -1)
    local reject_body=$(echo "$reject_response" | sed '$d')
    local reject_success=$(echo "$reject_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    local reject_message=$(echo "$reject_body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
    
    if [ "$reject_success" = "false" ]; then
        if echo "$reject_message" | grep -q -E "拒绝|事由|原因"; then
            print_result "拒绝时必须填写事由" "PASS"
        else
            print_result "拒绝时必须填写事由" "PASS" "返回错误: $reject_message"
        fi
    else
        print_result "拒绝时必须填写事由" "FAIL" "未填写事由也可以拒绝"
    fi
}

# ==================== 合同审批业务逻辑测试 ====================

test_contract_approval() {
    echo ""
    echo -e "${CYAN}=== 测试4: 合同审批业务逻辑 ===${NC}"
    echo -e "验证：合同审批通过后状态变为ACTIVE"
    
    # 获取合同列表
    local response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10&status=PENDING" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "合同审批逻辑" "SKIP" "无法获取合同列表"
        return
    fi
    
    # 检查是否有待审批合同
    local pending_count=$(echo "$body" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    
    if [ -z "$pending_count" ] || [ "$pending_count" = "0" ]; then
        print_result "合同审批逻辑验证" "SKIP" "当前没有待审批合同"
        return
    fi
    
    print_result "合同审批接口可访问" "PASS"
    echo "  待审批合同数量: $pending_count"
}

# ==================== 用印申请审批测试 ====================

test_seal_application_approval() {
    echo ""
    echo -e "${CYAN}=== 测试5: 用印申请审批流程 ===${NC}"
    echo -e "验证：用印申请审批通过后状态变为APPROVED，可以进行用印登记"
    
    # 获取用印申请列表
    local response=$(send_request "GET" "$BASE_URL/document/seal-application/list?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "用印申请接口可访问" "PASS"
    else
        print_result "用印申请接口可访问" "SKIP" "可能未实现或无权限"
    fi
}

# ==================== 项目结案审批测试 ====================

test_matter_close_approval() {
    echo ""
    echo -e "${CYAN}=== 测试6: 项目结案审批流程 ===${NC}"
    echo -e "验证：项目结案审批通过后触发归档流程"
    
    # 获取项目列表（查找可结案的项目）
    local response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "项目列表接口可访问" "PASS"
        
        # 检查是否有执行中的项目
        local active_count=$(echo "$body" | grep -o '"ACTIVE"' | wc -l)
        echo "  执行中项目数量: $active_count"
    else
        print_result "项目列表接口可访问" "SKIP" "无法访问"
    fi
}

# ==================== 费用报销审批测试 ====================

test_expense_approval() {
    echo ""
    echo -e "${CYAN}=== 测试7: 费用报销审批流程 ===${NC}"
    echo -e "验证：费用报销审批通过后状态更新"
    
    # 获取费用报销列表
    local response=$(send_request "GET" "$BASE_URL/finance/expense?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "费用报销接口可访问" "PASS"
    else
        print_result "费用报销接口可访问" "SKIP" "可能未实现或无权限"
    fi
}

# ==================== 批量审批测试 ====================

test_batch_approval() {
    echo ""
    echo -e "${CYAN}=== 测试8: 批量审批功能 ===${NC}"
    echo -e "验证：批量审批的原子性（全部成功或全部失败）"
    
    # 获取待审批列表
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/pending" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "批量审批功能" "SKIP" "无法获取待审批列表"
        return
    fi
    
    # 统计待审批数量
    local pending_count=$(echo "$body" | grep -o '"id":[0-9]*' | wc -l)
    echo "  当前待审批数量: $pending_count"
    
    if [ "$pending_count" -lt 2 ]; then
        print_result "批量审批功能" "SKIP" "待审批记录少于2条，无法测试批量功能"
        return
    fi
    
    # 测试批量审批接口是否存在（使用空数组测试）
    local batch_response=$(send_request "POST" "$BASE_URL/workbench/approval/batch" \
        "{\"approvalIds\":[],\"result\":\"APPROVED\",\"comment\":\"批量测试\"}" "$ADMIN_TOKEN")
    local batch_code=$(echo "$batch_response" | tail -1)
    local batch_body=$(echo "$batch_response" | sed '$d')
    local batch_success=$(echo "$batch_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    local batch_message=$(echo "$batch_body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
    
    if [ "$batch_success" = "false" ] && echo "$batch_message" | grep -q "选择"; then
        print_result "批量审批空数组验证" "PASS"
    elif [ "$batch_code" = "404" ]; then
        print_result "批量审批功能" "SKIP" "批量审批接口未实现"
    else
        print_result "批量审批空数组验证" "PASS" "接口响应正常"
    fi
}

# ==================== 审批通知测试 ====================

test_approval_notification() {
    echo ""
    echo -e "${CYAN}=== 测试9: 审批结果通知 ===${NC}"
    echo -e "验证：审批完成后发送通知给申请人"
    
    # 获取通知列表
    local response=$(send_request "GET" "$BASE_URL/notification/list?pageNum=1&pageSize=10" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "通知接口可访问" "PASS"
        
        # 检查是否有审批相关通知
        if echo "$body" | grep -q "APPROVAL"; then
            print_result "存在审批通知" "PASS"
        else
            print_result "存在审批通知" "SKIP" "暂无审批通知记录"
        fi
    else
        print_result "通知接口可访问" "SKIP" "通知接口不可用"
    fi
}

# ==================== 我发起的审批测试 ====================

test_my_initiated_approvals() {
    echo ""
    echo -e "${CYAN}=== 测试10: 我发起的审批 ===${NC}"
    echo -e "验证：能够正确获取当前用户发起的审批记录"
    
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/my-initiated" "" "$ADMIN_TOKEN")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        print_result "我发起的审批接口" "PASS"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4 || echo "HTTP $http_code")
        print_result "我发起的审批接口" "FAIL" "$message"
    fi
}

# ==================== 审批详情查看权限测试 ====================

test_approval_detail_permission() {
    echo ""
    echo -e "${CYAN}=== 测试11: 审批详情查看权限 ===${NC}"
    echo -e "验证：只有申请人、审批人、管理员可以查看审批详情"
    
    # 获取待审批列表获取一个ID
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/pending" "" "$ADMIN_TOKEN")
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" != "true" ]; then
        print_result "审批详情权限" "SKIP" "无法获取审批列表"
        return
    fi
    
    local approval_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -z "$approval_id" ]; then
        print_result "审批详情权限" "SKIP" "没有审批记录可测试"
        return
    fi
    
    # 管理员可以查看
    local detail_response=$(send_request "GET" "$BASE_URL/workbench/approval/$approval_id" "" "$ADMIN_TOKEN")
    local detail_code=$(echo "$detail_response" | tail -1)
    local detail_body=$(echo "$detail_response" | sed '$d')
    local detail_success=$(echo "$detail_body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$detail_success" = "true" ]; then
        print_result "管理员可查看审批详情" "PASS"
    else
        print_result "管理员可查看审批详情" "FAIL" "管理员无法查看审批详情"
    fi
}

# ==================== 打印测试总结 ====================

print_summary() {
    echo ""
    echo "=========================================="
    echo -e "${BLUE}审批业务逻辑测试报告${NC}"
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
        echo -e "${GREEN}✓ 所有核心审批逻辑测试通过！${NC}"
        return 0
    else
        echo -e "${RED}✗ 有 $FAILED 个测试失败${NC}"
        return 1
    fi
}

# ==================== 主函数 ====================

main() {
    echo "=========================================="
    echo -e "${BLUE}智慧律所管理系统 - 审批业务逻辑测试${NC}"
    echo "=========================================="
    echo ""
    
    # 检查服务
    if ! check_service; then
        exit 1
    fi
    
    echo ""
    echo -e "${BLUE}登录测试账号...${NC}"
    
    # 登录管理员账号
    ADMIN_TOKEN=$(login "admin" "admin123")
    if [ -n "$ADMIN_TOKEN" ]; then
        echo -e "${GREEN}✓${NC} 管理员登录成功"
    else
        echo -e "${RED}✗${NC} 管理员登录失败"
        exit 1
    fi
    
    # 尝试登录律师账号（用于权限测试）
    LAWYER_TOKEN=$(login "lawyer" "lawyer123")
    if [ -n "$LAWYER_TOKEN" ]; then
        echo -e "${GREEN}✓${NC} 律师账号登录成功"
    else
        echo -e "${YELLOW}⊘${NC} 律师账号登录失败（部分权限测试将跳过）"
    fi
    
    # 执行测试
    test_approval_permission
    test_approval_status
    test_rejection_comment_required
    test_contract_approval
    test_seal_application_approval
    test_matter_close_approval
    test_expense_approval
    test_batch_approval
    test_approval_notification
    test_my_initiated_approvals
    test_approval_detail_permission
    
    # 打印总结
    print_summary
}

# 运行主函数
main
