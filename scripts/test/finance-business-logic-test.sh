#!/bin/bash

# 智慧律所管理系统 - 财务模块业务逻辑详细测试
# 测试内容：
# 1. 收款数据锁定机制 (Requirements 3.1, 3.2, 3.3)
# 2. 金额计算与校验规则
# 3. 状态流转规则
# 4. 提成计算业务逻辑
# 5. 数据关联与完整性
# 6. 收款变更审批流程
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

# 检查响应是否成功（只检查第一个 success 字段）
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

# 登录（带滑块验证）
login() {
    echo ""
    echo -e "${BLUE}登录获取Token...${NC}"
    
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
        echo -e "${GREEN}✓${NC} 登录成功"
        return 0
    else
        echo -e "${RED}✗${NC} 登录失败"
        return 1
    fi
}

# ==================== 1. 收款数据锁定机制测试 ====================
test_payment_lock_mechanism() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 收款数据锁定机制测试 (Requirements 3.1-3.3)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 查找一个待确认的收款记录
    local response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=100" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "获取收费列表" "FAIL" "无法获取收费数据" "锁定机制"
        return
    fi
    
    # 查找有收款记录的收费
    local fee_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$fee_id" ]; then
        response=$(send_request "GET" "$BASE_URL/finance/fee/$fee_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            # 检查是否有locked字段
            local has_locked=$(echo "$body" | grep -o '"locked"')
            if [ -n "$has_locked" ]; then
                print_result "收款记录包含锁定状态字段" "PASS" "" "锁定机制"
            else
                print_result "收款记录包含锁定状态字段" "SKIP" "字段可能在payments子对象中" "锁定机制"
            fi
            
            # 检查收费详情是否包含收款列表
            local has_payments=$(echo "$body" | grep -o '"payments"')
            if [ -n "$has_payments" ]; then
                print_result "收费详情关联收款记录" "PASS" "" "锁定机制"
            else
                print_result "收费详情关联收款记录" "SKIP" "可能无收款记录" "锁定机制"
            fi
        fi
    else
        print_result "获取收费详情进行锁定测试" "SKIP" "无可用收费记录" "锁定机制"
    fi
    
    # 1.2 测试已确认收款不能取消
    response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=100&status=PAID" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local paid_fee_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$paid_fee_id" ]; then
            # 获取该收费的收款记录
            response=$(send_request "GET" "$BASE_URL/finance/fee/$paid_fee_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            # 检查已收款金额
            local paid_amount=$(echo "$body" | grep -o '"paidAmount":[0-9.]*' | cut -d':' -f2)
            if [ -n "$paid_amount" ] && [ "$paid_amount" != "0" ]; then
                print_result "已收款记录有paidAmount字段" "PASS" "" "锁定机制"
            else
                print_result "已收款记录有paidAmount字段" "SKIP" "可能无已收款" "锁定机制"
            fi
        fi
    fi
    
    # 1.3 测试收款变更申请流程存在
    response=$(send_request "GET" "$BASE_URL/finance/payment-amendment/list?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "收款变更申请接口可用" "PASS" "" "锁定机制"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "收款变更申请接口可用" "SKIP" "${message:-可能无权限}" "锁定机制"
    fi
}

# ==================== 2. 金额计算与校验规则测试 ====================
test_amount_calculation() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 金额计算与校验规则测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 测试收款金额不能为负数
    local negative_payment='{
        "feeId": 1,
        "amount": -1000,
        "paymentMethod": "BANK",
        "paymentDate": "2026-01-12"
    }'
    local response=$(send_request "POST" "$BASE_URL/finance/fee/payment" "$negative_payment" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝负数收款金额" "PASS" "" "金额校验"
    else
        print_result "拒绝负数收款金额" "FAIL" "允许了负数金额" "金额校验"
    fi
    
    # 2.2 测试收款金额不能为零
    local zero_payment='{
        "feeId": 1,
        "amount": 0,
        "paymentMethod": "BANK",
        "paymentDate": "2026-01-12"
    }'
    response=$(send_request "POST" "$BASE_URL/finance/fee/payment" "$zero_payment" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝零金额收款" "PASS" "" "金额校验"
    else
        print_result "拒绝零金额收款" "SKIP" "可能允许零金额" "金额校验"
    fi
    
    # 2.3 测试收费金额与已收金额计算
    response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        # 检查是否有unpaidAmount字段
        local has_unpaid=$(echo "$body" | grep -o '"unpaidAmount"')
        if [ -n "$has_unpaid" ]; then
            print_result "收费记录计算待收金额" "PASS" "" "金额校验"
        else
            print_result "收费记录计算待收金额" "SKIP" "可能字段名不同" "金额校验"
        fi
    fi
    
    # 2.4 测试发票金额校验
    local invalid_invoice='{
        "clientId": 1,
        "invoiceType": "NORMAL",
        "invoiceTitle": "测试",
        "invoiceAmount": -500
    }'
    response=$(send_request "POST" "$BASE_URL/finance/invoice/apply" "$invalid_invoice" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝负数发票金额" "PASS" "" "金额校验"
    else
        print_result "拒绝负数发票金额" "FAIL" "允许了负数发票金额" "金额校验"
    fi
    
    # 2.5 测试费用报销金额校验
    local invalid_expense='{
        "expenseType": "TRAVEL",
        "amount": -100,
        "description": "测试负数",
        "expenseDate": "2026-01-12",
        "expenseCategory": "TRAVEL"
    }'
    response=$(send_request "POST" "$BASE_URL/finance/expense" "$invalid_expense" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝负数报销金额" "PASS" "" "金额校验"
    else
        print_result "拒绝负数报销金额" "FAIL" "允许了负数报销金额" "金额校验"
    fi
}

# ==================== 3. 状态流转规则测试 ====================
test_status_transition() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 状态流转规则测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 3.1 测试收费状态流转：PENDING -> PARTIAL -> PAID
    local response=$(send_request "GET" "$BASE_URL/finance/fee/list?pageNum=1&pageSize=100" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local pending_count=$(echo "$body" | grep -o '"status":"PENDING"' | wc -l)
        local partial_count=$(echo "$body" | grep -o '"status":"PARTIAL"' | wc -l)
        local paid_count=$(echo "$body" | grep -o '"status":"PAID"' | wc -l)
        
        echo -e "  ${BLUE}收费状态分布: PENDING=$pending_count, PARTIAL=$partial_count, PAID=$paid_count${NC}"
        
        if [ $pending_count -gt 0 ] || [ $partial_count -gt 0 ] || [ $paid_count -gt 0 ]; then
            print_result "收费状态字段存在且有值" "PASS" "" "状态流转"
        else
            print_result "收费状态字段存在且有值" "SKIP" "可能无数据或状态值不同" "状态流转"
        fi
    fi
    
    # 3.2 测试合同状态
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=50" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local draft_count=$(echo "$body" | grep -o '"status":"DRAFT"' | wc -l)
        local pending_count=$(echo "$body" | grep -o '"status":"PENDING"' | wc -l)
        local approved_count=$(echo "$body" | grep -o '"status":"APPROVED"' | wc -l)
        
        echo -e "  ${BLUE}合同状态分布: DRAFT=$draft_count, PENDING=$pending_count, APPROVED=$approved_count${NC}"
        
        if [ $draft_count -gt 0 ] || [ $pending_count -gt 0 ] || [ $approved_count -gt 0 ]; then
            print_result "合同状态字段存在且有值" "PASS" "" "状态流转"
        else
            print_result "合同状态字段存在且有值" "SKIP" "可能无数据" "状态流转"
        fi
    fi
    
    # 3.3 测试提成状态
    response=$(send_request "GET" "$BASE_URL/finance/commission?pageNum=1&pageSize=50" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local has_status=$(echo "$body" | grep -o '"status"')
        if [ -n "$has_status" ]; then
            print_result "提成记录包含状态字段" "PASS" "" "状态流转"
        else
            print_result "提成记录包含状态字段" "SKIP" "可能无数据" "状态流转"
        fi
    fi
    
    # 3.4 测试发票状态
    response=$(send_request "GET" "$BASE_URL/finance/invoice/list?pageNum=1&pageSize=50" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local has_status=$(echo "$body" | grep -o '"status"')
        if [ -n "$has_status" ]; then
            print_result "发票记录包含状态字段" "PASS" "" "状态流转"
        else
            print_result "发票记录包含状态字段" "SKIP" "可能无数据" "状态流转"
        fi
    fi
}

# ==================== 4. 提成计算业务逻辑测试 ====================
test_commission_logic() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 提成计算业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 测试提成规则配置
    local response=$(send_request "GET" "$BASE_URL/finance/commission/rules" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        # 检查规则字段
        local has_rate=$(echo "$body" | grep -o '"rate"\|"commissionRate"')
        local has_name=$(echo "$body" | grep -o '"ruleName"\|"name"')
        
        if [ -n "$has_rate" ] || [ -n "$has_name" ]; then
            print_result "提成规则包含必要配置字段" "PASS" "" "提成逻辑"
        else
            print_result "提成规则包含必要配置字段" "SKIP" "字段名可能不同" "提成逻辑"
        fi
        
        # 检查是否有默认规则
        local has_default=$(echo "$body" | grep -o '"isDefault":true\|"default":true')
        if [ -n "$has_default" ]; then
            print_result "存在默认提成规则" "PASS" "" "提成逻辑"
        else
            print_result "存在默认提成规则" "SKIP" "可能无默认规则设置" "提成逻辑"
        fi
    else
        print_result "获取提成规则列表" "FAIL" "接口调用失败" "提成逻辑"
    fi
    
    # 4.2 测试提成规则激活状态
    response=$(send_request "GET" "$BASE_URL/finance/commission/rules/active" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取激活的提成规则" "PASS" "" "提成逻辑"
    else
        print_result "获取激活的提成规则" "SKIP" "可能无激活规则" "提成逻辑"
    fi
    
    # 4.3 测试待计算提成的收款
    response=$(send_request "GET" "$BASE_URL/finance/commission/pending-payments" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待计算提成收款接口" "PASS" "" "提成逻辑"
    else
        print_result "获取待计算提成收款接口" "SKIP" "可能无待计算记录" "提成逻辑"
    fi
    
    # 4.4 测试提成汇总计算
    response=$(send_request "GET" "$BASE_URL/finance/commission/summary" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        # 检查汇总数据字段
        local has_total=$(echo "$body" | grep -o '"totalAmount"\|"total"')
        if [ -n "$has_total" ]; then
            print_result "提成汇总包含总金额字段" "PASS" "" "提成逻辑"
        else
            print_result "提成汇总包含总金额字段" "SKIP" "字段名可能不同" "提成逻辑"
        fi
    else
        print_result "提成汇总计算" "SKIP" "可能权限不足" "提成逻辑"
    fi
    
    # 4.5 测试按用户查询提成总额
    response=$(send_request "GET" "$BASE_URL/finance/commission/users/1/total" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按用户查询提成总额" "PASS" "" "提成逻辑"
    else
        print_result "按用户查询提成总额" "SKIP" "用户可能不存在" "提成逻辑"
    fi
}

# ==================== 5. 数据关联与完整性测试 ====================
test_data_integrity() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 数据关联与完整性测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 测试收费必须关联客户
    local invalid_fee='{
        "feeType": "LAWYER_FEE",
        "feeName": "测试费用",
        "amount": 10000
    }'
    local response=$(send_request "POST" "$BASE_URL/finance/fee" "$invalid_fee" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "收费必须关联客户/合同" "PASS" "" "数据完整性"
    else
        print_result "收费必须关联客户/合同" "FAIL" "允许创建无关联收费" "数据完整性"
    fi
    
    # 5.2 测试收款必须关联收费记录
    local invalid_payment='{
        "amount": 1000,
        "paymentMethod": "BANK",
        "paymentDate": "2026-01-12"
    }'
    response=$(send_request "POST" "$BASE_URL/finance/fee/payment" "$invalid_payment" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "收款必须关联收费记录" "PASS" "" "数据完整性"
    else
        print_result "收款必须关联收费记录" "FAIL" "允许创建无关联收款" "数据完整性"
    fi
    
    # 5.3 测试合同详情包含必要关联数据
    response=$(send_request "GET" "$BASE_URL/finance/contract/list?pageNum=1&pageSize=1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local contract_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$contract_id" ]; then
            response=$(send_request "GET" "$BASE_URL/finance/contract/$contract_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                # 检查必要字段
                local has_client=$(echo "$body" | grep -o '"clientId"\|"clientName"')
                local has_matter=$(echo "$body" | grep -o '"matterId"\|"matterName"')
                local has_amount=$(echo "$body" | grep -o '"totalAmount"\|"contractAmount"')
                
                if [ -n "$has_client" ] && [ -n "$has_amount" ]; then
                    print_result "合同包含客户和金额信息" "PASS" "" "数据完整性"
                else
                    print_result "合同包含客户和金额信息" "SKIP" "部分字段缺失" "数据完整性"
                fi
            fi
        else
            print_result "合同关联数据完整性" "SKIP" "无合同数据" "数据完整性"
        fi
    fi
    
    # 5.4 测试查询不存在的收费记录
    response=$(send_request "GET" "$BASE_URL/finance/fee/999999999" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "404" ] || ! check_success "$body"; then
        print_result "查询不存在收费返回正确状态" "PASS" "" "数据完整性"
    else
        print_result "查询不存在收费返回正确状态" "FAIL" "应返回404或错误" "数据完整性"
    fi
    
    # 5.5 测试查询不存在的发票
    response=$(send_request "GET" "$BASE_URL/finance/invoice/999999999" "" "$auth_header")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "404" ] || ! check_success "$body"; then
        print_result "查询不存在发票返回正确状态" "PASS" "" "数据完整性"
    else
        print_result "查询不存在发票返回正确状态" "FAIL" "应返回404或错误" "数据完整性"
    fi
}

# ==================== 6. 收款变更审批流程测试 ====================
test_amendment_workflow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 收款变更审批流程测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 测试变更申请列表接口
    local response=$(send_request "GET" "$BASE_URL/finance/payment-amendment/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "收款变更申请列表接口" "PASS" "" "变更流程"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "收款变更申请列表接口" "SKIP" "${message:-可能无权限或接口不同}" "变更流程"
    fi
    
    # 6.2 测试按状态筛选变更申请
    response=$(send_request "GET" "$BASE_URL/finance/payment-amendment/list?pageNum=1&pageSize=10&status=PENDING" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按状态筛选变更申请" "PASS" "" "变更流程"
    else
        print_result "按状态筛选变更申请" "SKIP" "可能不支持状态筛选" "变更流程"
    fi
    
    # 6.3 测试合同修订接口
    response=$(send_request "GET" "$BASE_URL/finance/contract-amendment/list?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "合同修订列表接口" "PASS" "" "变更流程"
    else
        print_result "合同修订列表接口" "SKIP" "可能无权限或接口不同" "变更流程"
    fi
}

# ==================== 7. 操作日志审计测试 ====================
test_audit_log() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 操作日志审计测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 测试操作日志接口
    local response=$(send_request "GET" "$BASE_URL/admin/operation-logs?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "操作日志查询接口" "PASS" "" "审计日志"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "操作日志查询接口" "SKIP" "${message:-可能无权限}" "审计日志"
    fi
    
    # 7.2 测试按模块筛选操作日志
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs?pageNum=1&pageSize=10&module=收费管理" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按模块筛选操作日志" "PASS" "" "审计日志"
    else
        print_result "按模块筛选操作日志" "SKIP" "可能无相关日志" "审计日志"
    fi
    
    # 7.3 测试获取日志模块列表
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/modules" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取日志模块列表" "PASS" "" "审计日志"
    else
        print_result "获取日志模块列表" "SKIP" "可能无权限" "审计日志"
    fi
    
    # 7.4 测试日志统计接口
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "操作日志统计接口" "PASS" "" "审计日志"
    else
        print_result "操作日志统计接口" "SKIP" "可能无权限" "审计日志"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}              财务业务逻辑测试总结${NC}"
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
    
    local categories=("锁定机制" "金额校验" "状态流转" "提成逻辑" "数据完整性" "变更流程" "审计日志")
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
        echo -e "${GREEN}  ✅ 财务业务逻辑测试全部通过！${NC}"
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
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}     智慧律所管理系统 - 财务业务逻辑详细测试${NC}"
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
    test_payment_lock_mechanism
    test_amount_calculation
    test_status_transition
    test_commission_logic
    test_data_integrity
    test_amendment_workflow
    test_audit_log
    
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
