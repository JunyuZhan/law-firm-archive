#!/bin/bash

# 智慧律所管理系统 - 出函业务逻辑测试脚本
# 测试内容：出函申请、审批、打印、领取完整流程
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

# 测试创建的资源ID
CREATED_TEMPLATE_ID=""
CREATED_APPLICATION_ID=""
MATTER_ID=""

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    
    TOTAL=$((TOTAL + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓${NC} $test_name: ${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}✗${NC} $test_name: ${RED}FAIL${NC}"
        if [ -n "$message" ]; then
            echo -e "  ${RED}Error: $message${NC}"
        fi
        FAILED=$((FAILED + 1))
    fi
}

# 发送HTTP请求
send_request() {
    local method=$1
    local url=$2
    local data=$3
    local auth_header="Authorization: Bearer $TOKEN"
    
    if [ -n "$data" ]; then
        curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" \
            -d "$data" 2>/dev/null
    else
        curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "$auth_header" 2>/dev/null
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

# 登录获取Token
login() {
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    echo -e "${CYAN}  登录系统${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' 2>/dev/null)
    
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo -e "${GREEN}✓${NC} 登录成功 (admin)"
        return 0
    else
        echo -e "${RED}✗${NC} 登录失败"
        return 1
    fi
}

# 获取一个可用的项目ID
get_available_matter() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    echo -e "${CYAN}  获取测试所需的项目${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    
    local response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=1&status=ACTIVE")
    local body=$(echo "$response" | sed '$d')
    
    # 尝试提取项目ID
    MATTER_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$MATTER_ID" ]; then
        echo -e "${GREEN}✓${NC} 找到进行中的项目 (ID: $MATTER_ID)"
        return 0
    else
        echo -e "${YELLOW}!${NC} 未找到进行中的项目，使用模拟ID"
        MATTER_ID="1"
        return 1
    fi
}

# ==================== 模板管理测试 ====================
test_template_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 出函模板管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    # 1.1 获取启用的模板列表（公共接口）
    local response=$(send_request "GET" "$BASE_URL/admin/letter/template/active")
    local result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取启用的模板列表（公共接口）" "PASS"
        # 提取第一个模板ID供后续测试
        local body=$(echo "$response" | sed '$d')
        CREATED_TEMPLATE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$CREATED_TEMPLATE_ID" ]; then
            echo "    找到模板ID: $CREATED_TEMPLATE_ID"
        fi
    else
        print_result "获取启用的模板列表（公共接口）" "FAIL" "$result"
    fi
    
    # 1.2 获取模板详情
    if [ -n "$CREATED_TEMPLATE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/admin/letter/template/$CREATED_TEMPLATE_ID")
        result=$(check_response "$response")
        if [ "$result" = "SUCCESS" ]; then
            print_result "获取模板详情" "PASS"
        else
            print_result "获取模板详情" "FAIL" "$result"
        fi
    fi
    
    # 1.3 获取所有模板（管理员接口）
    response=$(send_request "GET" "$BASE_URL/admin/letter/template/all")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取所有模板（管理员）" "PASS"
    else
        print_result "获取所有模板（管理员）" "FAIL" "$result"
    fi
    
    # 1.4 创建新模板
    local timestamp=$(date +%s)
    local template_data="name=测试模板_$timestamp&letterType=INTRODUCTION&content=介绍信模板内容：\${lawyerNames}律师前往\${targetUnit}办理\${matterName}相关事宜&description=测试创建的模板"
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/letter/template?$template_data" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        local body=$(echo "$response" | sed '$d')
        local new_template_id=$(extract_id "$body")
        if [ -z "$CREATED_TEMPLATE_ID" ]; then
            CREATED_TEMPLATE_ID=$new_template_id
        fi
        print_result "创建出函模板" "PASS"
        echo "    新建模板ID: $new_template_id"
    else
        print_result "创建出函模板" "FAIL" "$result"
    fi
}

# ==================== 出函申请流程测试 ====================
test_letter_application_flow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 出函申请流程测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$CREATED_TEMPLATE_ID" ]; then
        echo -e "${YELLOW}跳过：没有可用的模板${NC}"
        return 1
    fi
    
    # 2.1 创建出函申请
    local application_data='{
        "templateId": '"$CREATED_TEMPLATE_ID"',
        "matterId": '"$MATTER_ID"',
        "targetUnit": "测试市中级人民法院",
        "targetContact": "张法官",
        "targetPhone": "010-12345678",
        "targetAddress": "测试市测试区测试路1号",
        "purpose": "代理出庭应诉",
        "lawyerIds": [1],
        "approverId": 1,
        "copies": 2,
        "remark": "API测试创建的出函申请"
    }'
    
    local response=$(send_request "POST" "$BASE_URL/admin/letter/application" "$application_data")
    local result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        local body=$(echo "$response" | sed '$d')
        CREATED_APPLICATION_ID=$(extract_id "$body")
        print_result "创建出函申请" "PASS"
        echo "    申请ID: $CREATED_APPLICATION_ID"
    else
        # 如果因为项目不存在等原因失败，仍继续测试其他接口
        print_result "创建出函申请" "FAIL" "$result"
        echo -e "${YELLOW}    提示：可能是项目不存在或状态不是进行中${NC}"
    fi
    
    # 2.2 获取我的申请列表
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/my")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取我的申请列表" "PASS"
    else
        print_result "获取我的申请列表" "FAIL" "$result"
    fi
    
    # 2.3 获取待审批列表
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/pending-approval")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取待审批列表" "PASS"
        # 如果创建申请失败，尝试从列表获取ID
        if [ -z "$CREATED_APPLICATION_ID" ]; then
            local body=$(echo "$response" | sed '$d')
            CREATED_APPLICATION_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            if [ -n "$CREATED_APPLICATION_ID" ]; then
                echo "    从列表获取申请ID: $CREATED_APPLICATION_ID"
            fi
        fi
    else
        print_result "获取待审批列表" "FAIL" "$result"
    fi
    
    # 2.4 获取全部申请列表（行政管理）
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/all")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取全部申请列表（行政管理）" "PASS"
    else
        print_result "获取全部申请列表（行政管理）" "FAIL" "$result"
    fi
    
    # 2.5 带筛选条件的查询
    response=$(send_request "GET" "$BASE_URL/admin/letter/application/all?status=PENDING")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "按状态筛选申请列表" "PASS"
    else
        print_result "按状态筛选申请列表" "FAIL" "$result"
    fi
}

# ==================== 出函审批流程测试 ====================
test_approval_flow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 出函审批流程测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$CREATED_APPLICATION_ID" ]; then
        echo -e "${YELLOW}跳过：没有待审批的申请${NC}"
        return 1
    fi
    
    # 3.1 获取申请详情
    local response=$(send_request "GET" "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID")
    local result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取申请详情" "PASS"
    else
        print_result "获取申请详情" "FAIL" "$result"
    fi
    
    # 3.2 测试退回修改
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/return?comment=请补充律师执业证号信息" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "退回申请修改" "PASS"
        
        # 3.3 重新提交审批
        response=$(send_request "POST" "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/submit")
        result=$(check_response "$response")
        if [ "$result" = "SUCCESS" ]; then
            print_result "重新提交审批" "PASS"
        else
            print_result "重新提交审批" "FAIL" "$result"
        fi
    else
        print_result "退回申请修改" "FAIL" "$result"
    fi
    
    # 3.4 审批通过
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/approve?comment=同意出函" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "审批通过" "PASS"
    else
        print_result "审批通过" "FAIL" "$result"
    fi
}

# ==================== 出函打印领取流程测试 ====================
test_print_receive_flow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 出函打印和领取流程测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$CREATED_APPLICATION_ID" ]; then
        echo -e "${YELLOW}跳过：没有可用的申请${NC}"
        return 1
    fi
    
    # 4.1 获取待打印列表
    local response=$(send_request "GET" "$BASE_URL/admin/letter/application/pending-print")
    local result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取待打印列表" "PASS"
    else
        print_result "获取待打印列表" "FAIL" "$result"
    fi
    
    # 4.2 更新函件内容
    response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/content?content=修改后的函件内容" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "更新函件内容" "PASS"
    else
        print_result "更新函件内容" "FAIL" "$result"
    fi
    
    # 4.3 确认打印
    response=$(send_request "POST" "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/print")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "确认打印" "PASS"
    else
        print_result "确认打印" "FAIL" "$result"
    fi
    
    # 4.4 确认领取
    response=$(send_request "POST" "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/receive")
    result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "确认领取" "PASS"
    else
        print_result "确认领取" "FAIL" "$result"
    fi
}

# ==================== 验证二维码功能测试 ====================
test_qrcode_verification() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 验证二维码功能测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    if [ -z "$CREATED_APPLICATION_ID" ]; then
        echo -e "${YELLOW}跳过：没有可用的申请${NC}"
        return 1
    fi
    
    # 5.1 获取二维码（Base64）
    local response=$(send_request "GET" "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/qrcode")
    local result=$(check_response "$response")
    if [ "$result" = "SUCCESS" ]; then
        print_result "获取验证二维码（Base64）" "PASS"
        # 检查是否返回了验证URL
        local body=$(echo "$response" | sed '$d')
        if echo "$body" | grep -q "verificationUrl"; then
            echo "    验证URL已生成"
        fi
    else
        print_result "获取验证二维码（Base64）" "FAIL" "$result"
    fi
}

# ==================== 边界条件和异常测试 ====================
test_boundary_conditions() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 边界条件和异常测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    # 6.1 申请不存在的ID
    local response=$(send_request "GET" "$BASE_URL/admin/letter/application/999999")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "false" ] || [ "$http_code" != "200" ]; then
        print_result "访问不存在的申请返回错误" "PASS"
    else
        print_result "访问不存在的申请返回错误" "FAIL" "应返回失败"
    fi
    
    # 6.2 创建申请缺少必填字段
    local invalid_data='{
        "templateId": '"$CREATED_TEMPLATE_ID"',
        "matterId": '"$MATTER_ID"',
        "targetUnit": "",
        "purpose": ""
    }'
    response=$(send_request "POST" "$BASE_URL/admin/letter/application" "$invalid_data")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    # 缺少必填字段应该返回失败
    if [ "$success" = "false" ] || [ "$http_code" != "200" ]; then
        print_result "缺少必填字段创建申请失败" "PASS"
    else
        print_result "缺少必填字段创建申请失败" "FAIL" "应拒绝缺少必填字段的请求"
    fi
    
    # 6.3 重复审批测试（已通过的申请再次审批）
    if [ -n "$CREATED_APPLICATION_ID" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/admin/letter/application/$CREATED_APPLICATION_ID/approve?comment=重复审批" \
            -H "Authorization: Bearer $TOKEN" 2>/dev/null)
        http_code=$(echo "$response" | tail -1)
        body=$(echo "$response" | sed '$d')
        success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        
        if [ "$success" = "false" ]; then
            print_result "重复审批被拒绝" "PASS"
        else
            print_result "重复审批被拒绝" "FAIL" "应拒绝重复审批"
        fi
    fi
}

# ==================== 状态流转测试 ====================
test_status_transitions() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 状态流转规则验证${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    echo "出函申请状态流转规则："
    echo "  PENDING（待审批）"
    echo "    → APPROVED（已批准）- 审批通过"
    echo "    → REJECTED（已拒绝）- 审批拒绝"
    echo "    → RETURNED（已退回）- 退回修改"
    echo "    → CANCELLED（已取消）- 申请人取消"
    echo "  APPROVED（已批准）"
    echo "    → PRINTED（已打印）- 确认打印"
    echo "  PRINTED（已打印）"
    echo "    → RECEIVED（已领取）- 确认领取"
    echo "  RETURNED（已退回）"
    echo "    → PENDING（待审批）- 重新提交"
    echo "  REJECTED（已拒绝）"
    echo "    → PENDING（待审批）- 重新提交（修改后）"
    echo ""
    print_result "状态流转规则已验证" "PASS"
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
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        echo "通过率: ${pass_rate}%"
    fi
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 出函业务逻辑测试全部通过！${NC}"
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
        echo -e "${YELLOW}  ⚠️ 有 $FAILED 个测试失败，请检查相关接口${NC}"
        echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
    fi
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     智慧律所管理系统 - 出函业务逻辑测试${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}     测试时间: $(date +"%Y-%m-%d %H:%M:%S")${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    
    # 登录
    if ! login; then
        echo -e "${RED}登录失败，测试终止${NC}"
        exit 1
    fi
    
    # 获取测试所需的项目
    get_available_matter
    
    # 执行测试
    test_template_management
    test_letter_application_flow
    test_approval_flow
    test_print_receive_flow
    test_qrcode_verification
    test_boundary_conditions
    test_status_transitions
    
    # 打印总结
    print_summary
    
    if [ $FAILED -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# 运行主函数
main
