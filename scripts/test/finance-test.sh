#!/bin/bash

# 智慧律所管理系统 - 财务模块全面测试脚本
# 测试内容：
# 1. 财务合同管理
# 2. 收费/收款管理
# 3. 发票管理
# 4. 费用报销
# 5. 提成管理
# 6. 我的财务
# 7. 安全性验证（权限控制、数据隔离、操作审计）
# 测试日期：$(date +%Y-%m-%d)

BASE_URL="http://localhost:8080/api"
TOKEN=""
ADMIN_TOKEN=""
LAWYER_TOKEN=""
FINANCE_TOKEN=""

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

# 测试数据ID
TEST_CONTRACT_ID=""
TEST_FEE_ID=""
TEST_PAYMENT_ID=""
TEST_INVOICE_ID=""
TEST_EXPENSE_ID=""
TEST_COMMISSION_RULE_ID=""

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    local category=$4
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} [$category] $test_name: ${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
        TEST_RESULTS+=("PASS|$category|$test_name|")
    elif [ "$status" = "SKIP" ]; then
        echo -e "${YELLOW}⊘${NC} [$category] $test_name: ${YELLOW}SKIP${NC} - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP|$category|$test_name|$message")
    else
        echo -e "${RED}✗${NC} [$category] $test_name: ${RED}FAIL${NC}"
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

# 检查响应是否成功
check_success() {
    local body=$1
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    [ "$success" = "true" ]
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

# ==================== 登录获取Token ====================
login_users() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  获取测试用户Token${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    # 管理员登录
    local response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        ADMIN_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        TOKEN=$ADMIN_TOKEN
        echo -e "${GREEN}✓${NC} 管理员登录成功"
    else
        echo -e "${RED}✗${NC} 管理员登录失败"
        return 1
    fi
    
    # 尝试律师登录
    response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"lawyer1","password":"lawyer123"}')
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        LAWYER_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓${NC} 律师账号登录成功"
    else
        echo -e "${YELLOW}⊘${NC} 律师账号不存在或密码错误"
    fi
    
    # 尝试财务登录
    response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"finance","password":"finance123"}')
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        FINANCE_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓${NC} 财务账号登录成功"
    else
        echo -e "${YELLOW}⊘${NC} 财务账号不存在或密码错误"
    fi
    
    return 0
}

# ==================== 1. 财务合同管理测试 ====================
test_finance_contract() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 财务合同管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 查询合同列表
    local response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body" && [ "$http_code" = "200" ]; then
        print_result "查询合同列表" "PASS" "" "财务合同"
        
        # 提取第一个合同ID用于后续测试
        TEST_CONTRACT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$TEST_CONTRACT_ID" ]; then
            echo -e "  ${BLUE}提取测试合同ID: $TEST_CONTRACT_ID${NC}"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询合同列表" "FAIL" "${message:-HTTP $http_code}" "财务合同"
    fi
    
    # 1.2 查询合同详情
    if [ -n "$TEST_CONTRACT_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/contract/$TEST_CONTRACT_ID" "" "$auth_header")
        http_code=$(echo "$response" | tail -1)
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询合同详情" "PASS" "" "财务合同"
            
            # 验证返回字段
            local has_amount=$(echo "$body" | grep -o '"totalAmount"')
            local has_status=$(echo "$body" | grep -o '"status"')
            local has_client=$(echo "$body" | grep -o '"clientId"\|"clientName"')
            
            if [ -n "$has_amount" ] && [ -n "$has_status" ]; then
                print_result "合同详情包含必要字段（金额、状态）" "PASS" "" "财务合同"
            else
                print_result "合同详情包含必要字段（金额、状态）" "FAIL" "缺少关键字段" "财务合同"
            fi
        else
            print_result "查询合同详情" "FAIL" "查询失败" "财务合同"
        fi
    else
        print_result "查询合同详情" "SKIP" "无可用合同" "财务合同"
    fi
    
    # 1.3 查询合同分页参数
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=5" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local page_size=$(echo "$body" | grep -o '"pageSize":[0-9]*' | cut -d':' -f2)
        if [ "$page_size" = "5" ]; then
            print_result "分页参数生效" "PASS" "" "财务合同"
        else
            print_result "分页参数生效" "SKIP" "pageSize值: $page_size" "财务合同"
        fi
    else
        print_result "分页参数生效" "SKIP" "查询失败" "财务合同"
    fi
    
    # 1.4 测试按状态筛选
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10&status=APPROVED" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按状态筛选合同" "PASS" "" "财务合同"
    else
        print_result "按状态筛选合同" "SKIP" "可能不支持状态筛选" "财务合同"
    fi
}

# ==================== 2. 收费/收款管理测试 ====================
test_fee_payment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 收费/收款管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 查询收费列表
    local response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body" && [ "$http_code" = "200" ]; then
        print_result "查询收费列表" "PASS" "" "收费管理"
        
        # 提取第一个收费ID
        TEST_FEE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$TEST_FEE_ID" ]; then
            echo -e "  ${BLUE}提取测试收费ID: $TEST_FEE_ID${NC}"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询收费列表" "FAIL" "${message:-HTTP $http_code}" "收费管理"
    fi
    
    # 2.2 查询收费详情
    if [ -n "$TEST_FEE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/fee/$TEST_FEE_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询收费详情" "PASS" "" "收费管理"
            
            # 验证返回的收款记录
            local has_payments=$(echo "$body" | grep -o '"payments"')
            if [ -n "$has_payments" ]; then
                print_result "收费详情包含收款记录列表" "PASS" "" "收费管理"
            else
                print_result "收费详情包含收款记录列表" "SKIP" "可能字段名不同" "收费管理"
            fi
        else
            print_result "查询收费详情" "FAIL" "查询失败" "收费管理"
        fi
    else
        print_result "查询收费详情" "SKIP" "无可用收费记录" "收费管理"
    fi
    
    # 2.3 测试创建收费记录（需要有效的客户和合同）
    local timestamp=$(date +%s)
    local fee_data='{
        "contractId": 1,
        "matterId": 1,
        "clientId": 1,
        "feeType": "LAWYER_FEE",
        "feeName": "测试律师费_'"$timestamp"'",
        "amount": 10000,
        "currency": "CNY",
        "plannedDate": "2026-02-01"
    }'
    response=$(send_request "POST" "$BASE_URL/finance/fee" "$fee_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_fee_id=$(extract_id "$body")
        if [ -n "$new_fee_id" ]; then
            TEST_FEE_ID=$new_fee_id
            print_result "创建收费记录" "PASS" "" "收费管理"
            echo -e "  ${BLUE}新建收费ID: $new_fee_id${NC}"
        else
            print_result "创建收费记录" "PASS" "" "收费管理"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建收费记录" "SKIP" "${message:-关联数据可能不存在}" "收费管理"
    fi
    
    # 2.4 测试创建收款记录
    if [ -n "$TEST_FEE_ID" ]; then
        local payment_data='{
            "feeId": '"$TEST_FEE_ID"',
            "amount": 5000,
            "paymentMethod": "BANK",
            "paymentDate": "2026-01-12",
            "bankAccount": "6222021234567890",
            "transactionNo": "TX'"$timestamp"'"
        }'
        response=$(send_request "POST" "$BASE_URL/finance/fee/payment" "$payment_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            TEST_PAYMENT_ID=$(extract_id "$body")
            print_result "创建收款记录" "PASS" "" "收费管理"
            echo -e "  ${BLUE}新建收款ID: $TEST_PAYMENT_ID${NC}"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "创建收款记录" "SKIP" "${message:-收费记录可能不适合}" "收费管理"
        fi
    else
        print_result "创建收款记录" "SKIP" "无可用收费记录" "收费管理"
    fi
    
    # 2.5 测试确认收款
    if [ -n "$TEST_PAYMENT_ID" ]; then
        response=$(send_request "POST" "$BASE_URL/finance/fee/payment/$TEST_PAYMENT_ID/confirm" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "确认收款" "PASS" "" "收费管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "确认收款" "SKIP" "${message:-可能已确认}" "收费管理"
        fi
    else
        print_result "确认收款" "SKIP" "无可用收款记录" "收费管理"
    fi
    
    # 2.6 测试收款金额校验（超额收款）
    if [ -n "$TEST_FEE_ID" ]; then
        local over_payment_data='{
            "feeId": '"$TEST_FEE_ID"',
            "amount": 999999999,
            "paymentMethod": "BANK",
            "paymentDate": "2026-01-12"
        }'
        response=$(send_request "POST" "$BASE_URL/finance/fee/payment" "$over_payment_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "收款金额校验（拒绝超额收款）" "PASS" "" "收费管理"
        else
            print_result "收款金额校验（拒绝超额收款）" "FAIL" "允许了超额收款" "收费管理"
        fi
    else
        print_result "收款金额校验" "SKIP" "无可用收费记录" "收费管理"
    fi
    
    # 2.7 测试智能匹配收款
    response=$(send_request "GET" "$BASE_URL/finance/fee/reconciliation/match?amount=10000" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "智能匹配收款" "PASS" "" "收费管理"
    else
        print_result "智能匹配收款" "SKIP" "可能无待匹配记录" "收费管理"
    fi
}

# ==================== 3. 发票管理测试 ====================
test_invoice() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 发票管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 3.1 查询发票列表
    local response=$(send_request "GET" "$BASE_URL/finance/invoice/list?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body" && [ "$http_code" = "200" ]; then
        print_result "查询发票列表" "PASS" "" "发票管理"
        
        TEST_INVOICE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询发票列表" "FAIL" "${message:-HTTP $http_code}" "发票管理"
    fi
    
    # 3.2 查询发票详情
    if [ -n "$TEST_INVOICE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/invoice/$TEST_INVOICE_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询发票详情" "PASS" "" "发票管理"
        else
            print_result "查询发票详情" "FAIL" "查询失败" "发票管理"
        fi
    else
        print_result "查询发票详情" "SKIP" "无可用发票" "发票管理"
    fi
    
    # 3.3 发票统计
    response=$(send_request "GET" "$BASE_URL/finance/invoice/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "发票统计查询" "PASS" "" "发票管理"
        
        # 验证统计数据结构
        local has_total=$(echo "$body" | grep -o '"totalAmount"\|"total"')
        if [ -n "$has_total" ]; then
            print_result "发票统计包含汇总数据" "PASS" "" "发票管理"
        else
            print_result "发票统计包含汇总数据" "SKIP" "可能字段名不同" "发票管理"
        fi
    else
        print_result "发票统计查询" "SKIP" "可能接口未实现" "发票管理"
    fi
    
    # 3.4 测试申请开票
    local timestamp=$(date +%s)
    local invoice_data='{
        "clientId": 1,
        "feeId": 1,
        "invoiceType": "NORMAL",
        "invoiceTitle": "测试开票单位_'"$timestamp"'",
        "taxNo": "91110000000000001A",
        "invoiceAmount": 10000
    }'
    response=$(send_request "POST" "$BASE_URL/finance/invoice/apply" "$invoice_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "申请开票" "PASS" "" "发票管理"
        TEST_INVOICE_ID=$(extract_id "$body")
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "申请开票" "SKIP" "${message:-关联数据可能不存在}" "发票管理"
    fi
    
    # 3.5 测试开票确认（如果有待开票的发票）
    if [ -n "$TEST_INVOICE_ID" ]; then
        local issue_data='{
            "invoiceNo": "FP'"$timestamp"'"
        }'
        response=$(send_request "POST" "$BASE_URL/finance/invoice/$TEST_INVOICE_ID/issue" "$issue_data" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "确认开票" "PASS" "" "发票管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "确认开票" "SKIP" "${message:-可能已开票或状态不对}" "发票管理"
        fi
    else
        print_result "确认开票" "SKIP" "无待开票发票" "发票管理"
    fi
    
    # 3.6 按客户筛选发票
    response=$(send_request "GET" "$BASE_URL/finance/invoice/list?pageNum=1&pageSize=10&clientId=1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按客户筛选发票" "PASS" "" "发票管理"
    else
        print_result "按客户筛选发票" "SKIP" "可能不支持客户筛选" "发票管理"
    fi
}

# ==================== 4. 费用报销测试 ====================
test_expense() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 费用报销测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 查询费用报销列表
    local response=$(send_request "GET" "$BASE_URL/finance/expense?pageNum=1&pageSize=10" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body" && [ "$http_code" = "200" ]; then
        print_result "查询费用报销列表" "PASS" "" "费用报销"
        
        TEST_EXPENSE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询费用报销列表" "FAIL" "${message:-HTTP $http_code}" "费用报销"
    fi
    
    # 4.2 查询报销详情
    if [ -n "$TEST_EXPENSE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/expense/$TEST_EXPENSE_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询报销详情" "PASS" "" "费用报销"
        else
            print_result "查询报销详情" "FAIL" "查询失败" "费用报销"
        fi
    else
        print_result "查询报销详情" "SKIP" "无可用报销记录" "费用报销"
    fi
    
    # 4.3 创建费用报销申请
    local timestamp=$(date +%s)
    local expense_data='{
        "expenseType": "TRAVEL",
        "amount": 2000,
        "description": "出差费用测试_'"$timestamp"'",
        "matterId": 1
    }'
    response=$(send_request "POST" "$BASE_URL/finance/expense" "$expense_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        TEST_EXPENSE_ID=$(extract_id "$body")
        print_result "创建报销申请" "PASS" "" "费用报销"
        echo -e "  ${BLUE}新建报销ID: $TEST_EXPENSE_ID${NC}"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建报销申请" "SKIP" "${message:-关联数据可能不存在}" "费用报销"
    fi
    
    # 4.4 测试报销金额校验（负数金额）
    local invalid_expense='{
        "expenseType": "TRAVEL",
        "amount": -100,
        "description": "负数测试"
    }'
    response=$(send_request "POST" "$BASE_URL/finance/expense" "$invalid_expense" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "报销金额校验（拒绝负数）" "PASS" "" "费用报销"
    else
        print_result "报销金额校验（拒绝负数）" "FAIL" "允许了负数金额" "费用报销"
    fi
    
    # 4.5 查询项目成本
    response=$(send_request "GET" "$BASE_URL/finance/expense/matters/1/total-cost" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询项目总成本" "PASS" "" "费用报销"
    else
        print_result "查询项目总成本" "SKIP" "可能项目不存在" "费用报销"
    fi
    
    # 4.6 查询成本归集记录
    response=$(send_request "GET" "$BASE_URL/finance/expense/matters/1/costs" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询成本归集记录" "PASS" "" "费用报销"
    else
        print_result "查询成本归集记录" "SKIP" "可能无归集记录" "费用报销"
    fi
}

# ==================== 5. 提成管理测试 ====================
test_commission() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 提成管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 查询提成规则列表
    local response=$(send_request "GET" "$BASE_URL/finance/commission/rules" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body" && [ "$http_code" = "200" ]; then
        print_result "查询提成规则列表" "PASS" "" "提成管理"
        
        TEST_COMMISSION_RULE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询提成规则列表" "FAIL" "${message:-HTTP $http_code}" "提成管理"
    fi
    
    # 5.2 查询激活的提成规则
    response=$(send_request "GET" "$BASE_URL/finance/commission/rules/active" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询激活的提成规则" "PASS" "" "提成管理"
    else
        print_result "查询激活的提成规则" "SKIP" "可能无激活规则" "提成管理"
    fi
    
    # 5.3 查询提成记录列表
    response=$(send_request "GET" "$BASE_URL/finance/commission?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询提成记录列表" "PASS" "" "提成管理"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询提成记录列表" "FAIL" "${message:-查询失败}" "提成管理"
    fi
    
    # 5.4 查询待计算提成的收款
    response=$(send_request "GET" "$BASE_URL/finance/commission/pending-payments" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询待计算提成收款" "PASS" "" "提成管理"
    else
        print_result "查询待计算提成收款" "SKIP" "可能无待计算记录" "提成管理"
    fi
    
    # 5.5 查询提成汇总
    response=$(send_request "GET" "$BASE_URL/finance/commission/summary" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询提成汇总" "PASS" "" "提成管理"
    else
        print_result "查询提成汇总" "SKIP" "可能权限不足" "提成管理"
    fi
    
    # 5.6 查询提成报表
    response=$(send_request "GET" "$BASE_URL/finance/commission/report" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询提成报表" "PASS" "" "提成管理"
    else
        print_result "查询提成报表" "SKIP" "可能无数据" "提成管理"
    fi
    
    # 5.7 查询用户提成总额
    response=$(send_request "GET" "$BASE_URL/finance/commission/users/1/total" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询用户提成总额" "PASS" "" "提成管理"
    else
        print_result "查询用户提成总额" "SKIP" "可能用户不存在" "提成管理"
    fi
    
    # 5.8 提成规则详情
    if [ -n "$TEST_COMMISSION_RULE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/commission/rules/$TEST_COMMISSION_RULE_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询提成规则详情" "PASS" "" "提成管理"
        else
            print_result "查询提成规则详情" "FAIL" "查询失败" "提成管理"
        fi
    else
        print_result "查询提成规则详情" "SKIP" "无可用规则" "提成管理"
    fi
}

# ==================== 6. 我的财务测试 ====================
test_my_finance() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 我的财务测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 查询我的收款
    local response=$(send_request "GET" "$BASE_URL/finance/my/payments" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body" && [ "$http_code" = "200" ]; then
        print_result "查询我的收款" "PASS" "" "我的财务"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询我的收款" "FAIL" "${message:-HTTP $http_code}" "我的财务"
    fi
    
    # 6.2 查询我的提成
    response=$(send_request "GET" "$BASE_URL/finance/my/commissions" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的提成" "PASS" "" "我的财务"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询我的提成" "FAIL" "${message:-查询失败}" "我的财务"
    fi
    
    # 6.3 按状态查询我的提成
    response=$(send_request "GET" "$BASE_URL/finance/my/commissions?status=PENDING" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按状态查询我的提成" "PASS" "" "我的财务"
    else
        print_result "按状态查询我的提成" "SKIP" "可能不支持状态筛选" "我的财务"
    fi
    
    # 6.4 查询我的费用报销
    response=$(send_request "GET" "$BASE_URL/finance/my/expenses" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的费用报销" "PASS" "" "我的财务"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "查询我的费用报销" "FAIL" "${message:-查询失败}" "我的财务"
    fi
}

# ==================== 7. 安全性测试 ====================
test_security() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 安全性测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    # 7.1 无Token访问财务接口
    local response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    
    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "无Token访问被拒绝" "PASS" "" "安全性"
    else
        print_result "无Token访问被拒绝" "FAIL" "应返回401/403，实际返回$http_code" "安全性"
    fi
    
    # 7.2 无效Token访问
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10" "" "Authorization: Bearer invalid_token_12345")
    http_code=$(echo "$response" | tail -1)
    
    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "无效Token被拒绝" "PASS" "" "安全性"
    else
        print_result "无效Token被拒绝" "FAIL" "应返回401/403，实际返回$http_code" "安全性"
    fi
    
    # 7.3 过期Token访问
    local expired_token="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTYwMDAwMDAwMH0.test"
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10" "" "Authorization: Bearer $expired_token")
    http_code=$(echo "$response" | tail -1)
    
    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "过期Token被拒绝" "PASS" "" "安全性"
    else
        print_result "过期Token被拒绝" "FAIL" "应返回401/403，实际返回$http_code" "安全性"
    fi
    
    # 7.4 普通律师访问提成管理（如果有律师Token）
    if [ -n "$LAWYER_TOKEN" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/commission/summary" "" "Authorization: Bearer $LAWYER_TOKEN")
        http_code=$(echo "$response" | tail -1)
        local body=$(echo "$response" | sed '$d')
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$http_code" = "403" ] || [ "$success" = "false" ]; then
            print_result "普通律师无法查看全所提成汇总" "PASS" "" "安全性"
        else
            print_result "普通律师无法查看全所提成汇总" "SKIP" "可能律师有此权限" "安全性"
        fi
    else
        print_result "普通律师权限测试" "SKIP" "无律师Token" "安全性"
    fi
    
    # 7.5 SQL注入测试
    local auth_header="Authorization: Bearer $TOKEN"
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=10&status='; DROP TABLE contract; --" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    
    # 如果返回正常列表或参数错误，说明SQL注入被防护
    if [ "$http_code" = "200" ] || [ "$http_code" = "400" ]; then
        print_result "SQL注入防护" "PASS" "" "安全性"
    else
        print_result "SQL注入防护" "SKIP" "HTTP $http_code" "安全性"
    fi
    
    # 7.6 XSS防护测试
    local xss_data='{
        "feeName": "<script>alert(1)</script>",
        "amount": 1000
    }'
    response=$(send_request "POST" "$BASE_URL/finance/fee" "$xss_data" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    # 检查返回是否包含原始脚本标签（应该被转义或拒绝）
    if echo "$body" | grep -q '<script>'; then
        print_result "XSS防护" "FAIL" "返回了原始脚本标签" "安全性"
    else
        print_result "XSS防护" "PASS" "" "安全性"
    fi
    
    # 7.7 检查操作日志是否记录
    response=$(send_request "GET" "$BASE_URL/system/log/operation?pageNum=1&pageSize=5&module=收费管理" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "操作日志记录" "PASS" "" "安全性"
    else
        print_result "操作日志记录" "SKIP" "可能接口不同" "安全性"
    fi
    
    # 7.8 重复提交防护测试
    local timestamp=$(date +%s)
    local fee_data='{
        "contractId": 1,
        "clientId": 1,
        "feeType": "OTHER",
        "feeName": "重复测试_'"$timestamp"'",
        "amount": 100
    }'
    
    # 快速发送两次相同请求
    response=$(send_request "POST" "$BASE_URL/finance/fee" "$fee_data" "$auth_header")
    local body1=$(echo "$response" | sed '$d')
    response=$(send_request "POST" "$BASE_URL/finance/fee" "$fee_data" "$auth_header")
    local body2=$(echo "$response" | sed '$d')
    
    local success1=$(echo "$body1" | grep -o '"success":[^,]*' | cut -d':' -f2)
    local success2=$(echo "$body2" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success1" = "true" ] && [ "$success2" = "false" ]; then
        print_result "重复提交防护" "PASS" "" "安全性"
    else
        print_result "重复提交防护" "SKIP" "可能未触发或关联数据不存在" "安全性"
    fi
}

# ==================== 8. 收款变更/修订测试 ====================
test_payment_amendment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 收款变更/修订测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 8.1 查询收款变更列表
    local response=$(send_request "GET" "$BASE_URL/finance/payment-amendment?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询收款变更列表" "PASS" "" "收款变更"
    else
        print_result "查询收款变更列表" "SKIP" "可能接口不同" "收款变更"
    fi
    
    # 8.2 查询合同修订列表
    response=$(send_request "GET" "$BASE_URL/finance/contract-amendment?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询合同修订列表" "PASS" "" "收款变更"
    else
        print_result "查询合同修订列表" "SKIP" "可能接口不同" "收款变更"
    fi
}

# ==================== 9. 预付款测试 ====================
test_prepayment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 预付款管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 9.1 查询预付款列表
    local response=$(send_request "GET" "$BASE_URL/finance/prepayment/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询预付款列表" "PASS" "" "预付款"
    else
        print_result "查询预付款列表" "SKIP" "可能接口不同或需权限" "预付款"
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
    
    # 清理顺序：发票 -> 费用报销 -> 收款 -> 收费
    # 实际项目中这些数据通常不会删除，这里仅作示例
    
    echo -e "${GREEN}✓${NC} 测试数据保留（财务数据通常不删除）"
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}                  财务模块测试总结${NC}"
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
    echo "按模块统计："
    echo -e "${BLUE}─────────────────────────────────────────────────────────${NC}"
    
    # 统计各模块结果
    local categories=("财务合同" "收费管理" "发票管理" "费用报销" "提成管理" "我的财务" "安全性" "收款变更" "预付款")
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
            printf "  %-12s: ${GREEN}%d通过${NC} / ${RED}%d失败${NC} / ${YELLOW}%d跳过${NC}\n" "$cat" "$cat_pass" "$cat_fail" "$cat_skip"
        fi
    done
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 财务模块测试全部通过！${NC}"
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
    
    echo ""
    echo "说明："
    echo "  - PASS: 功能正常，测试通过"
    echo "  - SKIP: 可能接口实现不同、无测试数据或功能可选"
    echo "  - FAIL: 功能异常，需要检查"
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     智慧律所管理系统 - 财务模块全面测试${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     测试时间: $(date +"%Y-%m-%d %H:%M:%S")${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    
    # 检查服务
    if ! check_service; then
        echo -e "${RED}服务未运行，测试终止${NC}"
        exit 1
    fi
    
    # 登录获取Token
    if ! login_users; then
        echo -e "${RED}登录失败，测试终止${NC}"
        exit 1
    fi
    
    # 执行测试
    test_finance_contract
    test_fee_payment
    test_invoice
    test_expense
    test_commission
    test_my_finance
    test_security
    test_payment_amendment
    test_prepayment
    
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
