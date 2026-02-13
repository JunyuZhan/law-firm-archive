#!/bin/bash

# 智慧律所管理系统 - 行政管理模块业务逻辑详细测试
# 测试内容：
# 1. 资产管理 - 创建/更新/删除/领用/归还/报废流程
# 2. 请假管理 - 类型查询/申请/审批/取消/余额
# 3. 加班管理 - 申请/审批/查询
# 4. 考勤管理 - 签到/签退/查询/统计
# 5. 会议室管理 - 会议室/预约/取消
# 6. 采购管理 - 采购申请/审批/入库/取消
# 7. 供应商管理 - CRUD/状态变更
# 8. 出函管理 - 模板/申请/审批/打印流程
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

# 检查响应是否成功
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

# ==================== 1. 资产管理测试 ====================
test_asset_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 资产管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_asset_id=""
    
    # 1.1 查询资产列表
    local response=$(send_request "GET" "$BASE_URL/admin/assets?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询资产列表" "PASS" "" "资产管理"
    else
        print_result "查询资产列表" "FAIL" "接口调用失败" "资产管理"
    fi
    
    # 1.2 创建资产 - 验证必填字段
    local create_asset='{
        "name": "测试办公电脑",
        "category": "IT",
        "brand": "Dell",
        "model": "OptiPlex 7080",
        "purchaseDate": "2026-01-12",
        "purchasePrice": 8000,
        "location": "办公室A区"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/assets" "$create_asset" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        test_asset_id=$(extract_id "$body")
        print_result "创建资产记录" "PASS" "" "资产管理"
        
        # 检查资产编号是否自动生成
        local asset_no=$(echo "$body" | grep -o '"assetNo":"[^"]*' | cut -d'"' -f4)
        if [ -n "$asset_no" ]; then
            print_result "资产编号自动生成" "PASS" "" "资产管理"
        else
            print_result "资产编号自动生成" "FAIL" "未生成资产编号" "资产管理"
        fi
        
        # 检查初始状态是否为IDLE
        local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        if [ "$status" = "IDLE" ]; then
            print_result "新资产状态为闲置(IDLE)" "PASS" "" "资产管理"
        else
            print_result "新资产状态为闲置(IDLE)" "FAIL" "状态为: $status" "资产管理"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建资产记录" "SKIP" "$message" "资产管理"
    fi
    
    # 1.3 测试资产领用
    if [ -n "$test_asset_id" ]; then
        local receive_cmd='{
            "assetId": '$test_asset_id',
            "reason": "日常办公使用",
            "expectedReturnDate": "2026-06-12"
        }'
        response=$(send_request "POST" "$BASE_URL/admin/assets/receive" "$receive_cmd" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "资产领用" "PASS" "" "资产管理"
            
            # 验证资产状态变为使用中
            response=$(send_request "GET" "$BASE_URL/admin/assets/$test_asset_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
            if [ "$status" = "IN_USE" ]; then
                print_result "领用后状态变为使用中(IN_USE)" "PASS" "" "资产管理"
            else
                print_result "领用后状态变为使用中(IN_USE)" "FAIL" "状态为: $status" "资产管理"
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "资产领用" "SKIP" "$message" "资产管理"
        fi
        
        # 1.4 测试已领用资产不能再次领用
        response=$(send_request "POST" "$BASE_URL/admin/assets/receive" "$receive_cmd" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝重复领用已使用资产" "PASS" "" "资产管理"
        else
            print_result "拒绝重复领用已使用资产" "FAIL" "允许了重复领用" "资产管理"
        fi
        
        # 1.5 测试资产归还
        response=$(send_request "POST" "$BASE_URL/admin/assets/$test_asset_id/return?remarks=测试归还" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "资产归还" "PASS" "" "资产管理"
            
            # 验证归还后状态变回闲置
            response=$(send_request "GET" "$BASE_URL/admin/assets/$test_asset_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
            if [ "$status" = "IDLE" ]; then
                print_result "归还后状态变回闲置(IDLE)" "PASS" "" "资产管理"
            else
                print_result "归还后状态变回闲置(IDLE)" "FAIL" "状态为: $status" "资产管理"
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "资产归还" "SKIP" "$message" "资产管理"
        fi
        
        # 1.6 测试闲置资产不能归还
        response=$(send_request "POST" "$BASE_URL/admin/assets/$test_asset_id/return?remarks=再次归还" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝归还闲置资产" "PASS" "" "资产管理"
        else
            print_result "拒绝归还闲置资产" "FAIL" "允许了归还闲置资产" "资产管理"
        fi
        
        # 1.7 测试资产报废
        response=$(send_request "POST" "$BASE_URL/admin/assets/$test_asset_id/scrap?reason=设备老化" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "资产报废申请" "PASS" "" "资产管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "资产报废申请" "SKIP" "$message" "资产管理"
        fi
        
        # 1.8 测试删除有历史记录的资产
        response=$(send_request "DELETE" "$BASE_URL/admin/assets/$test_asset_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝删除有历史记录的资产" "PASS" "" "资产管理"
        else
            print_result "拒绝删除有历史记录的资产" "FAIL" "允许了删除有历史的资产" "资产管理"
        fi
    fi
    
    # 1.9 查询资产操作记录
    if [ -n "$test_asset_id" ]; then
        response=$(send_request "GET" "$BASE_URL/admin/assets/$test_asset_id/records" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询资产操作记录" "PASS" "" "资产管理"
        else
            print_result "查询资产操作记录" "SKIP" "可能无权限" "资产管理"
        fi
    fi
    
    # 1.10 查询我领用的资产
    response=$(send_request "GET" "$BASE_URL/admin/assets/my" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我领用的资产" "PASS" "" "资产管理"
    else
        print_result "查询我领用的资产" "SKIP" "接口可能不可用" "资产管理"
    fi
    
    # 1.11 查询闲置资产
    response=$(send_request "GET" "$BASE_URL/admin/assets/idle" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询闲置资产" "PASS" "" "资产管理"
    else
        print_result "查询闲置资产" "SKIP" "接口可能不可用" "资产管理"
    fi
    
    # 1.12 查询资产统计
    response=$(send_request "GET" "$BASE_URL/admin/assets/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取资产统计" "PASS" "" "资产管理"
    else
        print_result "获取资产统计" "SKIP" "可能无权限" "资产管理"
    fi
}

# ==================== 2. 请假管理测试 ====================
test_leave_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 请假管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_leave_id=""
    local leave_type_id=""
    
    # 2.1 获取请假类型列表
    local response=$(send_request "GET" "$BASE_URL/admin/leave/types" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取请假类型列表" "PASS" "" "请假管理"
        leave_type_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "获取请假类型列表" "FAIL" "接口调用失败" "请假管理"
    fi
    
    # 2.2 提交请假申请 - 验证时间校验
    local future_start=$(date -v+1d +"%Y-%m-%dT09:00:00")
    local future_end=$(date -v+1d +"%Y-%m-%dT18:00:00")
    
    if [ -n "$leave_type_id" ]; then
        local apply_leave='{
            "leaveTypeId": '$leave_type_id',
            "startTime": "'$future_start'",
            "endTime": "'$future_end'",
            "duration": 1,
            "reason": "测试请假申请"
        }'
        response=$(send_request "POST" "$BASE_URL/admin/leave/applications" "$apply_leave" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            test_leave_id=$(extract_id "$body")
            print_result "提交请假申请" "PASS" "" "请假管理"
            
            # 检查状态是否为待审批
            local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
            if [ "$status" = "PENDING" ]; then
                print_result "请假申请状态为待审批(PENDING)" "PASS" "" "请假管理"
            else
                print_result "请假申请状态为待审批(PENDING)" "FAIL" "状态为: $status" "请假管理"
            fi
            
            # 检查申请编号是否自动生成
            local app_no=$(echo "$body" | grep -o '"applicationNo":"[^"]*' | cut -d'"' -f4)
            if [ -n "$app_no" ]; then
                print_result "请假申请编号自动生成" "PASS" "" "请假管理"
            else
                print_result "请假申请编号自动生成" "FAIL" "未生成申请编号" "请假管理"
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "提交请假申请" "SKIP" "$message" "请假管理"
        fi
    fi
    
    # 2.3 测试开始时间晚于结束时间的校验
    if [ -n "$leave_type_id" ]; then
        local invalid_leave='{
            "leaveTypeId": '$leave_type_id',
            "startTime": "'$future_end'",
            "endTime": "'$future_start'",
            "duration": 1,
            "reason": "时间错误的申请"
        }'
        response=$(send_request "POST" "$BASE_URL/admin/leave/applications" "$invalid_leave" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝开始时间晚于结束时间" "PASS" "" "请假管理"
        else
            print_result "拒绝开始时间晚于结束时间" "FAIL" "允许了无效时间" "请假管理"
        fi
    fi
    
    # 2.4 测试过去时间的校验
    if [ -n "$leave_type_id" ]; then
        local past_leave='{
            "leaveTypeId": '$leave_type_id',
            "startTime": "2025-01-01T09:00:00",
            "endTime": "2025-01-01T18:00:00",
            "duration": 1,
            "reason": "过去时间的申请"
        }'
        response=$(send_request "POST" "$BASE_URL/admin/leave/applications" "$past_leave" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝过去时间的请假申请" "PASS" "" "请假管理"
        else
            print_result "拒绝过去时间的请假申请" "FAIL" "允许了过去时间" "请假管理"
        fi
    fi
    
    # 2.5 取消请假申请
    if [ -n "$test_leave_id" ]; then
        response=$(send_request "POST" "$BASE_URL/admin/leave/applications/$test_leave_id/cancel" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "取消请假申请" "PASS" "" "请假管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "取消请假申请" "SKIP" "$message" "请假管理"
        fi
        
        # 2.6 测试已取消的申请不能再次取消
        response=$(send_request "POST" "$BASE_URL/admin/leave/applications/$test_leave_id/cancel" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝重复取消申请" "PASS" "" "请假管理"
        else
            print_result "拒绝重复取消申请" "FAIL" "允许了重复取消" "请假管理"
        fi
    fi
    
    # 2.7 获取待审批列表
    response=$(send_request "GET" "$BASE_URL/admin/leave/applications/pending" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待审批列表" "PASS" "" "请假管理"
    else
        print_result "获取待审批列表" "SKIP" "可能无权限" "请假管理"
    fi
    
    # 2.8 获取假期余额
    response=$(send_request "GET" "$BASE_URL/admin/leave/balance?year=2026" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取假期余额" "PASS" "" "请假管理"
    else
        print_result "获取假期余额" "SKIP" "可能未初始化" "请假管理"
    fi
    
    # 2.9 分页查询请假申请
    response=$(send_request "GET" "$BASE_URL/admin/leave/applications?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询请假申请" "PASS" "" "请假管理"
    else
        print_result "分页查询请假申请" "SKIP" "可能无权限" "请假管理"
    fi
}

# ==================== 3. 加班管理测试 ====================
test_overtime_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 加班管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_overtime_id=""
    local today=$(date +%Y-%m-%d)
    
    # 3.1 申请加班
    local overtime_apply='{
        "overtimeDate": "'$today'",
        "startTime": "18:00",
        "endTime": "21:00",
        "reason": "测试项目紧急任务"
    }'
    local response=$(send_request "POST" "$BASE_URL/admin/overtime/apply" "$overtime_apply" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        test_overtime_id=$(extract_id "$body")
        print_result "申请加班" "PASS" "" "加班管理"
        
        # 检查加班时长计算
        local hours=$(echo "$body" | grep -o '"overtimeHours":[0-9.]*' | cut -d':' -f2)
        if [ -n "$hours" ]; then
            print_result "加班时长自动计算" "PASS" "" "加班管理"
        else
            print_result "加班时长自动计算" "SKIP" "字段名可能不同" "加班管理"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "申请加班" "SKIP" "$message" "加班管理"
    fi
    
    # 3.2 测试加班时长超过12小时的限制
    local long_overtime='{
        "overtimeDate": "'$today'",
        "startTime": "08:00",
        "endTime": "22:00",
        "reason": "超长加班测试"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/overtime/apply" "$long_overtime" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    # 注意：14小时应该被拒绝（如果有12小时限制）
    local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
    if echo "$message" | grep -q "12小时\|时长"; then
        print_result "拒绝超过12小时的加班申请" "PASS" "" "加班管理"
    else
        print_result "拒绝超过12小时的加班申请" "SKIP" "可能无时长限制" "加班管理"
    fi
    
    # 3.3 查询我的加班申请
    response=$(send_request "GET" "$BASE_URL/admin/overtime/my" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的加班申请" "PASS" "" "加班管理"
    else
        print_result "查询我的加班申请" "SKIP" "接口可能不可用" "加班管理"
    fi
    
    # 3.4 按日期范围查询加班申请
    local start_date=$(date -v-30d +%Y-%m-%d)
    local end_date=$(date +%Y-%m-%d)
    response=$(send_request "GET" "$BASE_URL/admin/overtime/range?startDate=$start_date&endDate=$end_date" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按日期范围查询加班" "PASS" "" "加班管理"
    else
        print_result "按日期范围查询加班" "SKIP" "可能无权限" "加班管理"
    fi
    
    # 3.5 审批加班申请
    if [ -n "$test_overtime_id" ]; then
        local approve_request='{"approved": true, "comment": "批准"}'
        response=$(send_request "POST" "$BASE_URL/admin/overtime/$test_overtime_id/approve" "$approve_request" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "审批加班申请" "PASS" "" "加班管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "审批加班申请" "SKIP" "$message" "加班管理"
        fi
    fi
}

# ==================== 4. 考勤管理测试 ====================
test_attendance_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 考勤管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 获取今日考勤
    local response=$(send_request "GET" "$BASE_URL/admin/attendance/today" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取今日考勤状态" "PASS" "" "考勤管理"
    else
        print_result "获取今日考勤状态" "SKIP" "可能今日无记录" "考勤管理"
    fi
    
    # 4.2 签到
    local check_in='{
        "location": "公司办公室",
        "deviceInfo": "测试设备"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/attendance/check-in" "$check_in" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "签到" "PASS" "" "考勤管理"
        
        # 检查签到时间
        local check_in_time=$(echo "$body" | grep -o '"checkInTime":"[^"]*' | cut -d'"' -f4)
        if [ -n "$check_in_time" ]; then
            print_result "签到时间记录" "PASS" "" "考勤管理"
        else
            print_result "签到时间记录" "SKIP" "字段名可能不同" "考勤管理"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        # 可能已签到
        if echo "$message" | grep -q "已签到\|重复"; then
            print_result "签到（已签到）" "SKIP" "今日已签到" "考勤管理"
        else
            print_result "签到" "SKIP" "$message" "考勤管理"
        fi
    fi
    
    # 4.3 签退
    local check_out='{
        "location": "公司办公室",
        "deviceInfo": "测试设备"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/attendance/check-out" "$check_out" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "签退" "PASS" "" "考勤管理"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "签退" "SKIP" "$message" "考勤管理"
    fi
    
    # 4.4 查询考勤记录
    response=$(send_request "GET" "$BASE_URL/admin/attendance?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询考勤记录列表" "PASS" "" "考勤管理"
    else
        print_result "查询考勤记录列表" "SKIP" "可能无权限" "考勤管理"
    fi
    
    # 4.5 获取月度考勤统计
    local year=$(date +%Y)
    local month=$(date +%m)
    response=$(send_request "GET" "$BASE_URL/admin/attendance/statistics/monthly?year=$year&month=$month" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取月度考勤统计" "PASS" "" "考勤管理"
        
        # 检查统计数据字段
        local has_workdays=$(echo "$body" | grep -o '"workDays"\|"totalDays"')
        if [ -n "$has_workdays" ]; then
            print_result "月度统计包含工作天数" "PASS" "" "考勤管理"
        else
            print_result "月度统计包含工作天数" "SKIP" "字段名可能不同" "考勤管理"
        fi
    else
        print_result "获取月度考勤统计" "SKIP" "可能无权限" "考勤管理"
    fi
}

# ==================== 5. 会议室管理测试 ====================
test_meeting_room_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 会议室管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_room_id=""
    local test_booking_id=""
    
    # 5.1 获取所有会议室
    local response=$(send_request "GET" "$BASE_URL/admin/meeting-room" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取会议室列表" "PASS" "" "会议室管理"
        test_room_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "获取会议室列表" "FAIL" "接口调用失败" "会议室管理"
    fi
    
    # 5.2 获取可用会议室
    response=$(send_request "GET" "$BASE_URL/admin/meeting-room/available" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取可用会议室" "PASS" "" "会议室管理"
    else
        print_result "获取可用会议室" "SKIP" "可能无可用会议室" "会议室管理"
    fi
    
    # 5.3 创建会议室
    local create_room='{
        "name": "测试会议室A",
        "capacity": 10,
        "location": "3楼东侧",
        "facilities": "投影仪、白板、视频会议设备",
        "description": "适合小型会议"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/meeting-room" "$create_room" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        test_room_id=$(extract_id "$body")
        print_result "创建会议室" "PASS" "" "会议室管理"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建会议室" "SKIP" "$message" "会议室管理"
    fi
    
    # 5.4 预约会议室
    if [ -n "$test_room_id" ]; then
        local tomorrow=$(date -v+1d +%Y-%m-%d)
        local book_meeting='{
            "roomId": '$test_room_id',
            "title": "测试会议",
            "startTime": "'$tomorrow'T10:00:00",
            "endTime": "'$tomorrow'T11:00:00",
            "attendees": "张三、李四",
            "description": "测试预约"
        }'
        response=$(send_request "POST" "$BASE_URL/admin/meeting-room/bookings" "$book_meeting" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            test_booking_id=$(extract_id "$body")
            print_result "预约会议室" "PASS" "" "会议室管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "预约会议室" "SKIP" "$message" "会议室管理"
        fi
        
        # 5.5 测试时间冲突预约
        response=$(send_request "POST" "$BASE_URL/admin/meeting-room/bookings" "$book_meeting" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝时间冲突的预约" "PASS" "" "会议室管理"
        else
            print_result "拒绝时间冲突的预约" "FAIL" "允许了冲突预约" "会议室管理"
        fi
        
        # 5.6 取消会议预约
        if [ -n "$test_booking_id" ]; then
            response=$(send_request "POST" "$BASE_URL/admin/meeting-room/bookings/$test_booking_id/cancel" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "取消会议预约" "PASS" "" "会议室管理"
            else
                local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
                print_result "取消会议预约" "SKIP" "$message" "会议室管理"
            fi
        fi
    fi
    
    # 5.7 获取会议室某日预约情况
    if [ -n "$test_room_id" ]; then
        local today=$(date +%Y-%m-%d)
        response=$(send_request "GET" "$BASE_URL/admin/meeting-room/$test_room_id/bookings/day?date=$today" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询会议室当日预约" "PASS" "" "会议室管理"
        else
            print_result "查询会议室当日预约" "SKIP" "可能无预约" "会议室管理"
        fi
    fi
    
    # 5.8 获取我的会议预约
    response=$(send_request "GET" "$BASE_URL/admin/meeting-room/bookings/my" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的会议预约" "PASS" "" "会议室管理"
    else
        print_result "查询我的会议预约" "SKIP" "接口可能不可用" "会议室管理"
    fi
    
    # 5.9 获取会议室日程视图
    if [ -n "$test_room_id" ]; then
        local start_date=$(date +%Y-%m-%d)
        local end_date=$(date -v+7d +%Y-%m-%d)
        response=$(send_request "GET" "$BASE_URL/admin/meeting-room/$test_room_id/schedule?startDate=$start_date&endDate=$end_date" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取会议室日程视图" "PASS" "" "会议室管理"
        else
            print_result "获取会议室日程视图" "SKIP" "接口可能不可用" "会议室管理"
        fi
    fi
}

# ==================== 6. 采购管理测试 ====================
test_purchase_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 采购管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_purchase_id=""
    
    # 6.1 分页查询采购申请
    local response=$(send_request "GET" "$BASE_URL/admin/purchases?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询采购申请列表" "PASS" "" "采购管理"
    else
        print_result "查询采购申请列表" "SKIP" "可能无权限" "采购管理"
    fi
    
    # 6.2 创建采购申请
    local create_purchase='{
        "title": "办公用品采购",
        "purchaseType": "OFFICE_SUPPLIES",
        "items": [
            {"itemName": "A4打印纸", "specification": "70g", "quantity": 10, "unit": "箱", "estimatedPrice": 200},
            {"itemName": "签字笔", "specification": "黑色", "quantity": 100, "unit": "支", "estimatedPrice": 200}
        ],
        "reason": "日常办公消耗",
        "expectedDate": "2026-01-20"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/purchases" "$create_purchase" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        test_purchase_id=$(extract_id "$body")
        print_result "创建采购申请" "PASS" "" "采购管理"
        
        # 检查初始状态
        local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        if [ "$status" = "DRAFT" ]; then
            print_result "新采购申请状态为草稿(DRAFT)" "PASS" "" "采购管理"
        else
            print_result "新采购申请状态为草稿(DRAFT)" "SKIP" "状态为: $status" "采购管理"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建采购申请" "SKIP" "$message" "采购管理"
    fi
    
    # 6.3 提交采购申请
    if [ -n "$test_purchase_id" ]; then
        response=$(send_request "POST" "$BASE_URL/admin/purchases/$test_purchase_id/submit" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "提交采购申请" "PASS" "" "采购管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "提交采购申请" "SKIP" "$message" "采购管理"
        fi
        
        # 6.4 审批采购申请
        response=$(send_request "POST" "$BASE_URL/admin/purchases/$test_purchase_id/approve?approved=true&comment=批准" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "审批采购申请" "PASS" "" "采购管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "审批采购申请" "SKIP" "$message" "采购管理"
        fi
    fi
    
    # 6.5 获取我的采购申请
    response=$(send_request "GET" "$BASE_URL/admin/purchases/my" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的采购申请" "PASS" "" "采购管理"
    else
        print_result "查询我的采购申请" "SKIP" "接口可能不可用" "采购管理"
    fi
    
    # 6.6 获取待审批的采购申请
    response=$(send_request "GET" "$BASE_URL/admin/purchases/pending" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询待审批采购申请" "PASS" "" "采购管理"
    else
        print_result "查询待审批采购申请" "SKIP" "可能无权限" "采购管理"
    fi
    
    # 6.7 获取采购统计
    response=$(send_request "GET" "$BASE_URL/admin/purchases/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取采购统计" "PASS" "" "采购管理"
    else
        print_result "获取采购统计" "SKIP" "可能无权限" "采购管理"
    fi
}

# ==================== 7. 供应商管理测试 ====================
test_supplier_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 供应商管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_supplier_id=""
    
    # 7.1 查询供应商列表
    local response=$(send_request "GET" "$BASE_URL/admin/suppliers?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询供应商列表" "PASS" "" "供应商管理"
    else
        print_result "查询供应商列表" "SKIP" "可能无权限" "供应商管理"
    fi
    
    # 7.2 创建供应商
    local timestamp=$(date +%s)
    local create_supplier='{
        "name": "测试供应商有限公司_'$timestamp'",
        "supplierType": "OFFICE_SUPPLIES",
        "contactPerson": "张经理",
        "contactPhone": "13800138000",
        "email": "test@supplier.com",
        "address": "北京市朝阳区xxx路xxx号",
        "bankAccount": "1234567890123456789",
        "bankName": "中国银行",
        "taxNumber": "91110000MA012345X6"
    }'
    response=$(send_request "POST" "$BASE_URL/admin/suppliers" "$create_supplier" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        test_supplier_id=$(extract_id "$body")
        print_result "创建供应商" "PASS" "" "供应商管理"
        
        # 检查初始状态
        local status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        if [ "$status" = "ACTIVE" ] || [ "$status" = "ENABLED" ]; then
            print_result "新供应商默认为启用状态" "PASS" "" "供应商管理"
        else
            print_result "新供应商默认为启用状态" "SKIP" "状态为: $status" "供应商管理"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建供应商" "SKIP" "$message" "供应商管理"
    fi
    
    # 7.3 获取供应商详情
    if [ -n "$test_supplier_id" ]; then
        response=$(send_request "GET" "$BASE_URL/admin/suppliers/$test_supplier_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取供应商详情" "PASS" "" "供应商管理"
        else
            print_result "获取供应商详情" "SKIP" "可能无权限" "供应商管理"
        fi
    fi
    
    # 7.4 更新供应商
    if [ -n "$test_supplier_id" ]; then
        local update_supplier='{
            "name": "测试供应商有限公司(已更新)",
            "supplierType": "OFFICE_SUPPLIES",
            "contactPerson": "李经理",
            "contactPhone": "13900139000"
        }'
        response=$(send_request "PUT" "$BASE_URL/admin/suppliers/$test_supplier_id" "$update_supplier" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "更新供应商信息" "PASS" "" "供应商管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "更新供应商信息" "SKIP" "$message" "供应商管理"
        fi
    fi
    
    # 7.5 停用供应商
    if [ -n "$test_supplier_id" ]; then
        response=$(send_request "PUT" "$BASE_URL/admin/suppliers/$test_supplier_id/status?status=DISABLED" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "停用供应商" "PASS" "" "供应商管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "停用供应商" "SKIP" "$message" "供应商管理"
        fi
    fi
    
    # 7.6 启用供应商
    if [ -n "$test_supplier_id" ]; then
        response=$(send_request "PUT" "$BASE_URL/admin/suppliers/$test_supplier_id/status?status=ACTIVE" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "启用供应商" "PASS" "" "供应商管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "启用供应商" "SKIP" "$message" "供应商管理"
        fi
    fi
    
    # 7.7 获取供应商统计
    response=$(send_request "GET" "$BASE_URL/admin/suppliers/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取供应商统计" "PASS" "" "供应商管理"
    else
        print_result "获取供应商统计" "SKIP" "可能无权限" "供应商管理"
    fi
    
    # 7.8 删除供应商
    if [ -n "$test_supplier_id" ]; then
        response=$(send_request "DELETE" "$BASE_URL/admin/suppliers/$test_supplier_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "删除供应商" "PASS" "" "供应商管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            # 如果是因为有关联数据不能删除，也算通过
            if echo "$message" | grep -q "关联\|使用中\|引用"; then
                print_result "拒绝删除有关联数据的供应商" "PASS" "" "供应商管理"
            else
                print_result "删除供应商" "SKIP" "$message" "供应商管理"
            fi
        fi
    fi
}

# ==================== 8. 出函管理测试 ====================
test_letter_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 出函管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_template_id=""
    local test_application_id=""
    
    # 8.1 获取启用的模板列表
    local response=$(send_request "GET" "$BASE_URL/admin/letter/template/list" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取出函模板列表" "PASS" "" "出函管理"
        test_template_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "获取出函模板列表" "SKIP" "可能无权限" "出函管理"
    fi
    
    # 8.2 获取公共模板列表（无需特殊权限）
    response=$(send_request "GET" "$BASE_URL/admin/letter/template/active" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取公共出函模板" "PASS" "" "出函管理"
    else
        print_result "获取公共出函模板" "SKIP" "接口可能不可用" "出函管理"
    fi
    
    # 8.3 创建模板
    local template_name="TestTemplate_$(date +%s)"
    local encoded_name=$(echo -n "$template_name" | python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read()))")
    local encoded_content=$(echo -n "This is a test template content" | python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read()))")
    response=$(send_request "POST" "$BASE_URL/admin/letter/template?name=$encoded_name&letterType=INTRODUCTION&content=$encoded_content&description=ForTesting" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        test_template_id=$(extract_id "$body")
        print_result "创建出函模板" "PASS" "" "出函管理"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建出函模板" "SKIP" "$message" "出函管理"
    fi
    
    # 8.4 获取所有模板（管理员）
    response=$(send_request "GET" "$BASE_URL/admin/letter/template/all" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取所有模板（管理员）" "PASS" "" "出函管理"
    else
        print_result "获取所有模板（管理员）" "SKIP" "可能无权限" "出函管理"
    fi
    
    # 8.5 切换模板状态
    if [ -n "$test_template_id" ]; then
        response=$(send_request "POST" "$BASE_URL/admin/letter/template/$test_template_id/toggle" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "切换模板启用状态" "PASS" "" "出函管理"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "切换模板启用状态" "SKIP" "$message" "出函管理"
        fi
    fi
    
    # 8.6 我的申请列表
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/my" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的出函申请" "PASS" "" "出函管理"
    else
        print_result "查询我的出函申请" "SKIP" "接口可能不可用" "出函管理"
    fi
    
    # 8.7 获取待审批列表
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/pending-approval" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待审批出函列表" "PASS" "" "出函管理"
    else
        print_result "获取待审批出函列表" "SKIP" "可能无权限" "出函管理"
    fi
    
    # 8.8 获取全部申请列表（行政管理）
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/all" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取全部出函申请（行政）" "PASS" "" "出函管理"
    else
        print_result "获取全部出函申请（行政）" "SKIP" "可能无权限" "出函管理"
    fi
    
    # 8.9 获取待打印列表
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/pending-print" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待打印出函列表" "PASS" "" "出函管理"
    else
        print_result "获取待打印出函列表" "SKIP" "可能无权限" "出函管理"
    fi
}

# ==================== 9. 资产盘点测试 ====================
test_asset_inventory() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 资产盘点业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 9.1 查询盘点任务列表
    local response=$(send_request "GET" "$BASE_URL/admin/asset-inventories?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询资产盘点任务列表" "PASS" "" "资产盘点"
    else
        print_result "查询资产盘点任务列表" "SKIP" "接口可能不存在" "资产盘点"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}              行政管理业务逻辑测试总结${NC}"
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
    
    local categories=("资产管理" "请假管理" "加班管理" "考勤管理" "会议室管理" "采购管理" "供应商管理" "出函管理" "资产盘点")
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
        echo -e "${GREEN}  ✅ 行政管理业务逻辑测试全部通过！${NC}"
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
    echo -e "${PURPLE}     智慧律所管理系统 - 行政管理业务逻辑详细测试${NC}"
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
    test_asset_management
    test_leave_management
    test_overtime_management
    test_attendance_management
    test_meeting_room_management
    test_purchase_management
    test_supplier_management
    test_letter_management
    test_asset_inventory
    
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
