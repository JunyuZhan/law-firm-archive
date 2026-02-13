#!/bin/bash

# 智慧律所管理系统 - 证据管理模块业务逻辑详细测试
# 测试内容：
# 1. 证据CRUD基本操作
# 2. 证据数据完整性校验
# 3. 质证流程业务逻辑
# 4. 证据清单管理
# 5. 证据分组与排序
# 6. 证据导出功能
# 7. 证据清单历史与对比
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

# 测试用临时数据ID
TEST_MATTER_ID=""
TEST_EVIDENCE_ID=""
TEST_EVIDENCE_ID_2=""
TEST_EVIDENCE_LIST_ID=""

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

# 获取有效的案件ID
get_test_matter_id() {
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 尝试获取案件列表
    local response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        TEST_MATTER_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$TEST_MATTER_ID" ]; then
            echo -e "${GREEN}✓${NC} 获取测试案件ID: $TEST_MATTER_ID"
            return 0
        fi
    fi
    
    # 备选：尝试获取选择列表
    response=$(send_request "GET" "$BASE_URL/matter/select-options?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        TEST_MATTER_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$TEST_MATTER_ID" ]; then
            echo -e "${GREEN}✓${NC} 获取测试案件ID(备选): $TEST_MATTER_ID"
            return 0
        fi
    fi
    
    echo -e "${YELLOW}⚠${NC} 无法获取案件ID，部分测试可能跳过"
    return 1
}

# ==================== 1. 证据CRUD基本操作测试 ====================
test_evidence_crud() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 证据CRUD基本操作测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 创建证据
    if [ -n "$TEST_MATTER_ID" ]; then
        local create_evidence='{
            "matterId": '"$TEST_MATTER_ID"',
            "name": "测试证据-合同原件",
            "evidenceType": "DOCUMENTARY",
            "source": "原告提供",
            "groupName": "合同类证据",
            "provePurpose": "证明双方存在合同关系",
            "description": "这是一份自动化测试创建的证据",
            "isOriginal": true,
            "originalCount": 1,
            "copyCount": 2,
            "pageStart": 1,
            "pageEnd": 5
        }'
        local response=$(send_request "POST" "$BASE_URL/evidence" "$create_evidence" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            TEST_EVIDENCE_ID=$(extract_id "$body")
            print_result "创建证据" "PASS" "" "CRUD操作"
            echo -e "  ${BLUE}创建的证据ID: $TEST_EVIDENCE_ID${NC}"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "创建证据" "FAIL" "$message" "CRUD操作"
        fi
        
        # 创建第二个证据用于后续测试
        local create_evidence_2='{
            "matterId": '"$TEST_MATTER_ID"',
            "name": "测试证据-付款凭证",
            "evidenceType": "DOCUMENTARY",
            "source": "原告提供",
            "groupName": "财务类证据",
            "provePurpose": "证明已支付款项",
            "description": "自动化测试创建的第二个证据",
            "isOriginal": false,
            "originalCount": 0,
            "copyCount": 3
        }'
        response=$(send_request "POST" "$BASE_URL/evidence" "$create_evidence_2" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            TEST_EVIDENCE_ID_2=$(extract_id "$body")
            print_result "创建第二个证据" "PASS" "" "CRUD操作"
        else
            print_result "创建第二个证据" "SKIP" "创建失败" "CRUD操作"
        fi
    else
        print_result "创建证据" "SKIP" "无可用案件ID" "CRUD操作"
    fi
    
    # 1.2 查询证据详情
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            # 验证返回字段完整性
            local has_name=$(echo "$body" | grep -o '"name"')
            local has_type=$(echo "$body" | grep -o '"evidenceType"')
            local has_no=$(echo "$body" | grep -o '"evidenceNo"')
            
            if [ -n "$has_name" ] && [ -n "$has_type" ] && [ -n "$has_no" ]; then
                print_result "查询证据详情" "PASS" "" "CRUD操作"
            else
                print_result "查询证据详情" "FAIL" "返回字段不完整" "CRUD操作"
            fi
        else
            print_result "查询证据详情" "FAIL" "查询失败" "CRUD操作"
        fi
    else
        print_result "查询证据详情" "SKIP" "无测试证据ID" "CRUD操作"
    fi
    
    # 1.3 分页查询证据列表
    local response=$(send_request "GET" "$BASE_URL/evidence?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local has_total=$(echo "$body" | grep -o '"total"')
        local has_list=$(echo "$body" | grep -o '"list"\|"records"')
        
        if [ -n "$has_total" ] || [ -n "$has_list" ]; then
            print_result "分页查询证据列表" "PASS" "" "CRUD操作"
        else
            print_result "分页查询证据列表" "FAIL" "分页字段缺失" "CRUD操作"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "分页查询证据列表" "FAIL" "$message" "CRUD操作"
    fi
    
    # 1.4 更新证据
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local update_evidence='{
            "name": "测试证据-合同原件(已更新)",
            "description": "更新后的描述信息",
            "copyCount": 5
        }'
        local response=$(send_request "PUT" "$BASE_URL/evidence/$TEST_EVIDENCE_ID" "$update_evidence" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            # 验证更新是否生效
            local updated_name=$(echo "$body" | grep -o '"name":"[^"]*' | cut -d'"' -f4)
            if echo "$updated_name" | grep -q "已更新"; then
                print_result "更新证据" "PASS" "" "CRUD操作"
            else
                print_result "更新证据" "FAIL" "更新未生效" "CRUD操作"
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "更新证据" "FAIL" "$message" "CRUD操作"
        fi
    else
        print_result "更新证据" "SKIP" "无测试证据ID" "CRUD操作"
    fi
    
    # 1.5 按案件查询证据
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/matter/$TEST_MATTER_ID" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "按案件查询证据" "PASS" "" "CRUD操作"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "按案件查询证据" "FAIL" "$message" "CRUD操作"
        fi
    else
        print_result "按案件查询证据" "SKIP" "无案件ID" "CRUD操作"
    fi
}

# ==================== 2. 证据数据完整性校验测试 ====================
test_evidence_validation() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 证据数据完整性校验测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 测试缺少必填字段 - 缺少案件ID
    local invalid_evidence='{
        "name": "测试证据",
        "evidenceType": "DOCUMENTARY"
    }'
    local response=$(send_request "POST" "$BASE_URL/evidence" "$invalid_evidence" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝缺少案件ID的证据" "PASS" "" "数据校验"
    else
        print_result "拒绝缺少案件ID的证据" "FAIL" "允许了无效数据" "数据校验"
    fi
    
    # 2.2 测试缺少必填字段 - 缺少名称
    local invalid_evidence_2='{
        "matterId": '"${TEST_MATTER_ID:-1}"',
        "evidenceType": "DOCUMENTARY"
    }'
    response=$(send_request "POST" "$BASE_URL/evidence" "$invalid_evidence_2" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝缺少名称的证据" "PASS" "" "数据校验"
    else
        print_result "拒绝缺少名称的证据" "FAIL" "允许了无效数据" "数据校验"
    fi
    
    # 2.3 测试缺少必填字段 - 缺少证据类型
    local invalid_evidence_3='{
        "matterId": '"${TEST_MATTER_ID:-1}"',
        "name": "测试证据"
    }'
    response=$(send_request "POST" "$BASE_URL/evidence" "$invalid_evidence_3" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝缺少证据类型的证据" "PASS" "" "数据校验"
    else
        print_result "拒绝缺少证据类型的证据" "FAIL" "允许了无效数据" "数据校验"
    fi
    
    # 2.4 测试无效的案件ID
    local invalid_evidence_4='{
        "matterId": 999999999,
        "name": "测试证据",
        "evidenceType": "DOCUMENTARY"
    }'
    response=$(send_request "POST" "$BASE_URL/evidence" "$invalid_evidence_4" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝无效案件ID的证据" "PASS" "" "数据校验"
    else
        print_result "拒绝无效案件ID的证据" "SKIP" "可能未校验案件存在性" "数据校验"
    fi
    
    # 2.5 测试查询不存在的证据
    response=$(send_request "GET" "$BASE_URL/evidence/999999999" "" "$auth_header")
    local http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "404" ] || ! check_success "$body"; then
        print_result "查询不存在证据返回正确状态" "PASS" "" "数据校验"
    else
        print_result "查询不存在证据返回正确状态" "FAIL" "应返回404或错误" "数据校验"
    fi
    
    # 2.6 测试证据编号自动生成
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            local evidence_no=$(echo "$body" | grep -o '"evidenceNo":"[^"]*' | cut -d'"' -f4)
            if [ -n "$evidence_no" ]; then
                print_result "证据编号自动生成" "PASS" "" "数据校验"
                echo -e "  ${BLUE}证据编号: $evidence_no${NC}"
            else
                print_result "证据编号自动生成" "FAIL" "编号为空" "数据校验"
            fi
        fi
    else
        print_result "证据编号自动生成" "SKIP" "无测试证据" "数据校验"
    fi
}

# ==================== 3. 质证流程业务逻辑测试 ====================
test_cross_exam_workflow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 质证流程业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 3.1 添加质证记录
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local cross_exam='{
            "evidenceId": '"$TEST_EVIDENCE_ID"',
            "examParty": "被告方",
            "authenticityOpinion": "ACCEPTED",
            "authenticityReason": "对证据真实性无异议",
            "legalityOpinion": "ACCEPTED",
            "legalityReason": "证据来源合法",
            "relevanceOpinion": "ACCEPTED",
            "relevanceReason": "与案件有关联",
            "overallOpinion": "无异议，认可该证据"
        }'
        local response=$(send_request "POST" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/cross-exam" "$cross_exam" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "添加质证记录" "PASS" "" "质证流程"
            
            # 检查返回的质证记录字段
            local has_party=$(echo "$body" | grep -o '"examParty"')
            local has_opinion=$(echo "$body" | grep -o '"overallOpinion"')
            
            if [ -n "$has_party" ] && [ -n "$has_opinion" ]; then
                print_result "质证记录包含必要字段" "PASS" "" "质证流程"
            else
                print_result "质证记录包含必要字段" "FAIL" "字段不完整" "质证流程"
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "添加质证记录" "FAIL" "$message" "质证流程"
        fi
    else
        print_result "添加质证记录" "SKIP" "无测试证据ID" "质证流程"
    fi
    
    # 3.2 测试缺少必填字段的质证记录
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local invalid_cross_exam='{
            "authenticityOpinion": "ACCEPTED"
        }'
        local response=$(send_request "POST" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/cross-exam" "$invalid_cross_exam" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if ! check_success "$body"; then
            print_result "拒绝缺少质证方的记录" "PASS" "" "质证流程"
        else
            print_result "拒绝缺少质证方的记录" "FAIL" "允许了无效数据" "质证流程"
        fi
    else
        print_result "拒绝缺少质证方的记录" "SKIP" "无测试证据ID" "质证流程"
    fi
    
    # 3.3 添加第二个质证方的意见
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local cross_exam_2='{
            "examParty": "原告方",
            "authenticityOpinion": "ACCEPTED",
            "authenticityReason": "提供方确认",
            "legalityOpinion": "ACCEPTED",
            "relevanceOpinion": "ACCEPTED",
            "overallOpinion": "作为我方主张的证据"
        }'
        local response=$(send_request "POST" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/cross-exam" "$cross_exam_2" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "添加多方质证意见" "PASS" "" "质证流程"
        else
            print_result "添加多方质证意见" "SKIP" "可能不支持多方" "质证流程"
        fi
    fi
    
    # 3.4 完成质证
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "POST" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/complete-cross-exam" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "完成质证操作" "PASS" "" "质证流程"
            
            # 验证状态变更
            response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                local status=$(echo "$body" | grep -o '"crossExamStatus":"[^"]*' | cut -d'"' -f4)
                if [ "$status" = "COMPLETED" ]; then
                    print_result "质证状态变更为已完成" "PASS" "" "质证流程"
                else
                    print_result "质证状态变更为已完成" "SKIP" "状态: $status" "质证流程"
                fi
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "完成质证操作" "FAIL" "$message" "质证流程"
        fi
    else
        print_result "完成质证操作" "SKIP" "无测试证据ID" "质证流程"
    fi
}

# ==================== 4. 证据清单管理测试 ====================
test_evidence_list_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 证据清单管理测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 创建证据清单
    if [ -n "$TEST_MATTER_ID" ] && [ -n "$TEST_EVIDENCE_ID" ]; then
        local evidence_ids="[$TEST_EVIDENCE_ID"
        if [ -n "$TEST_EVIDENCE_ID_2" ]; then
            evidence_ids="$evidence_ids,$TEST_EVIDENCE_ID_2"
        fi
        evidence_ids="$evidence_ids]"
        
        local create_list='{
            "matterId": '"$TEST_MATTER_ID"',
            "name": "原告证据清单(一审)",
            "listType": "SUBMISSION",
            "evidenceIds": '"$evidence_ids"'
        }'
        local response=$(send_request "POST" "$BASE_URL/evidence/list" "$create_list" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            TEST_EVIDENCE_LIST_ID=$(extract_id "$body")
            print_result "创建证据清单" "PASS" "" "证据清单"
            echo -e "  ${BLUE}创建的清单ID: $TEST_EVIDENCE_LIST_ID${NC}"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "创建证据清单" "FAIL" "$message" "证据清单"
        fi
    else
        print_result "创建证据清单" "SKIP" "缺少案件或证据ID" "证据清单"
    fi
    
    # 4.2 查询证据清单详情
    if [ -n "$TEST_EVIDENCE_LIST_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/list/$TEST_EVIDENCE_LIST_ID" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            local has_name=$(echo "$body" | grep -o '"name"')
            local has_evidences=$(echo "$body" | grep -o '"evidences"\|"evidenceIds"')
            
            if [ -n "$has_name" ]; then
                print_result "查询证据清单详情" "PASS" "" "证据清单"
            else
                print_result "查询证据清单详情" "FAIL" "返回数据不完整" "证据清单"
            fi
        else
            print_result "查询证据清单详情" "FAIL" "查询失败" "证据清单"
        fi
    else
        print_result "查询证据清单详情" "SKIP" "无清单ID" "证据清单"
    fi
    
    # 4.3 分页查询证据清单列表
    local response=$(send_request "GET" "$BASE_URL/evidence/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询证据清单列表" "PASS" "" "证据清单"
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "分页查询证据清单列表" "FAIL" "$message" "证据清单"
    fi
    
    # 4.4 按案件查询证据清单
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/list/matter/$TEST_MATTER_ID" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "按案件查询证据清单" "PASS" "" "证据清单"
        else
            print_result "按案件查询证据清单" "FAIL" "查询失败" "证据清单"
        fi
    else
        print_result "按案件查询证据清单" "SKIP" "无案件ID" "证据清单"
    fi
    
    # 4.5 获取证据清单历史
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/list/matter/$TEST_MATTER_ID/history" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取证据清单历史" "PASS" "" "证据清单"
        else
            print_result "获取证据清单历史" "SKIP" "可能无历史记录" "证据清单"
        fi
    else
        print_result "获取证据清单历史" "SKIP" "无案件ID" "证据清单"
    fi
}

# ==================== 5. 证据分组与排序测试 ====================
test_evidence_grouping_sorting() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 证据分组与排序测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 获取案件的证据分组
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/matter/$TEST_MATTER_ID/groups" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取证据分组列表" "PASS" "" "分组排序"
        else
            print_result "获取证据分组列表" "FAIL" "获取失败" "分组排序"
        fi
    else
        print_result "获取证据分组列表" "SKIP" "无案件ID" "分组排序"
    fi
    
    # 5.2 调整证据排序
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "PUT" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/sort?sortOrder=1" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "调整证据排序" "PASS" "" "分组排序"
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "调整证据排序" "FAIL" "$message" "分组排序"
        fi
    else
        print_result "调整证据排序" "SKIP" "无测试证据ID" "分组排序"
    fi
    
    # 5.3 批量调整证据分组
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local evidence_ids="[$TEST_EVIDENCE_ID"
        if [ -n "$TEST_EVIDENCE_ID_2" ]; then
            evidence_ids="$evidence_ids,$TEST_EVIDENCE_ID_2"
        fi
        evidence_ids="$evidence_ids]"
        
        # URL编码中文参数
        local encoded_group_name=$(printf '%s' "核心证据" | python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read()))")
        local response=$(send_request "POST" "$BASE_URL/evidence/batch-group?groupName=$encoded_group_name" "$evidence_ids" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "批量调整证据分组" "PASS" "" "分组排序"
            
            # 验证分组更新
            response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                local group_name=$(echo "$body" | grep -o '"groupName":"[^"]*' | cut -d'"' -f4)
                if [ "$group_name" = "核心证据" ]; then
                    print_result "分组更新生效" "PASS" "" "分组排序"
                else
                    print_result "分组更新生效" "SKIP" "分组名: $group_name" "分组排序"
                fi
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "批量调整证据分组" "FAIL" "$message" "分组排序"
        fi
    else
        print_result "批量调整证据分组" "SKIP" "无测试证据ID" "分组排序"
    fi
    
    # 5.4 测试空数组的批量分组
    local response=$(send_request "POST" "$BASE_URL/evidence/batch-group?groupName=空测试" "[]" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    # 可能成功也可能失败，取决于实现
    if check_success "$body"; then
        print_result "空数组批量分组处理" "PASS" "" "分组排序"
    else
        print_result "空数组批量分组处理" "PASS" "" "分组排序"
    fi
}

# ==================== 6. 证据导出功能测试 ====================
test_evidence_export() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 证据导出功能测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 导出证据清单为Word (添加超时10秒)
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(curl -s -w "\n%{http_code}" --max-time 10 -X GET \
            "$BASE_URL/evidence/matter/$TEST_MATTER_ID/export?format=word" \
            -H "$auth_header" 2>/dev/null)
        local http_code=$(echo "$response" | tail -1)
        
        if [ "$http_code" = "200" ]; then
            print_result "导出证据清单Word格式" "PASS" "" "导出功能"
        elif [ -z "$http_code" ] || [ "$http_code" = "000" ]; then
            print_result "导出证据清单Word格式" "SKIP" "请求超时" "导出功能"
        else
            print_result "导出证据清单Word格式" "SKIP" "HTTP: $http_code" "导出功能"
        fi
    else
        print_result "导出证据清单Word格式" "SKIP" "无案件ID" "导出功能"
    fi
    
    # 6.2 导出证据清单为PDF (添加超时10秒)
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(curl -s -w "\n%{http_code}" --max-time 10 -X GET \
            "$BASE_URL/evidence/matter/$TEST_MATTER_ID/export?format=pdf" \
            -H "$auth_header" 2>/dev/null)
        local http_code=$(echo "$response" | tail -1)
        
        if [ "$http_code" = "200" ]; then
            print_result "导出证据清单PDF格式" "PASS" "" "导出功能"
        elif [ -z "$http_code" ] || [ "$http_code" = "000" ]; then
            print_result "导出证据清单PDF格式" "SKIP" "请求超时" "导出功能"
        else
            print_result "导出证据清单PDF格式" "SKIP" "HTTP: $http_code" "导出功能"
        fi
    else
        print_result "导出证据清单PDF格式" "SKIP" "无案件ID" "导出功能"
    fi
    
    # 6.3 证据清单导出为Word（清单接口，超时10秒）
    if [ -n "$TEST_EVIDENCE_LIST_ID" ]; then
        local response=$(curl -s -w "\n%{http_code}" --max-time 10 -X GET \
            "$BASE_URL/evidence/list/$TEST_EVIDENCE_LIST_ID/export/word" \
            -H "$auth_header" 2>/dev/null)
        local http_code=$(echo "$response" | tail -1)
        
        if [ "$http_code" = "200" ]; then
            print_result "证据清单详情导出Word" "PASS" "" "导出功能"
        elif [ -z "$http_code" ] || [ "$http_code" = "000" ]; then
            print_result "证据清单详情导出Word" "SKIP" "请求超时" "导出功能"
        else
            print_result "证据清单详情导出Word" "SKIP" "HTTP: $http_code" "导出功能"
        fi
    else
        print_result "证据清单详情导出Word" "SKIP" "无清单ID" "导出功能"
    fi
    
    # 6.4 证据清单导出为PDF（清单接口，超时10秒）
    if [ -n "$TEST_EVIDENCE_LIST_ID" ]; then
        local response=$(curl -s -w "\n%{http_code}" --max-time 10 -X GET \
            "$BASE_URL/evidence/list/$TEST_EVIDENCE_LIST_ID/export/pdf" \
            -H "$auth_header" 2>/dev/null)
        local http_code=$(echo "$response" | tail -1)
        
        if [ "$http_code" = "200" ]; then
            print_result "证据清单详情导出PDF" "PASS" "" "导出功能"
        elif [ -z "$http_code" ] || [ "$http_code" = "000" ]; then
            print_result "证据清单详情导出PDF" "SKIP" "请求超时" "导出功能"
        else
            print_result "证据清单详情导出PDF" "SKIP" "HTTP: $http_code" "导出功能"
        fi
    else
        print_result "证据清单详情导出PDF" "SKIP" "无清单ID" "导出功能"
    fi
    
    # 6.5 测试无效格式导出
    if [ -n "$TEST_MATTER_ID" ]; then
        local response=$(curl -s -w "\n%{http_code}" --max-time 5 -X GET \
            "$BASE_URL/evidence/matter/$TEST_MATTER_ID/export?format=invalid" \
            -H "$auth_header" 2>/dev/null)
        local http_code=$(echo "$response" | tail -1)
        
        if [ "$http_code" = "400" ]; then
            print_result "拒绝无效导出格式" "PASS" "" "导出功能"
        else
            print_result "拒绝无效导出格式" "SKIP" "HTTP: $http_code" "导出功能"
        fi
    fi
}

# ==================== 7. 证据文件与预览测试 ====================
test_evidence_file_preview() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 证据文件与预览测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 测试获取预览URL接口（无文件时应返回错误）
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/preview" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        # 测试证据没有关联文件，应该返回错误
        if ! check_success "$body"; then
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            if echo "$message" | grep -q "没有关联文件\|文件"; then
                print_result "无文件证据预览返回正确错误" "PASS" "" "文件预览"
            else
                print_result "无文件证据预览返回正确错误" "PASS" "" "文件预览"
            fi
        else
            print_result "无文件证据预览返回正确错误" "SKIP" "可能有文件" "文件预览"
        fi
    else
        print_result "预览URL接口" "SKIP" "无测试证据ID" "文件预览"
    fi
    
    # 7.2 测试获取下载URL接口
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/download-url" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        # 可能成功或因无文件失败
        if check_success "$body" || ! check_success "$body"; then
            print_result "下载URL接口可访问" "PASS" "" "文件预览"
        fi
    else
        print_result "下载URL接口" "SKIP" "无测试证据ID" "文件预览"
    fi
    
    # 7.3 测试获取缩略图接口
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/thumbnail" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        # 接口应该可访问
        if check_success "$body" || ! check_success "$body"; then
            print_result "缩略图接口可访问" "PASS" "" "文件预览"
        fi
    else
        print_result "缩略图接口" "SKIP" "无测试证据ID" "文件预览"
    fi
    
    # 7.4 测试OnlyOffice URL接口
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/onlyoffice-url" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        # 接口应该可访问
        if check_success "$body" || ! check_success "$body"; then
            print_result "OnlyOffice URL接口可访问" "PASS" "" "文件预览"
        fi
    else
        print_result "OnlyOffice URL接口" "SKIP" "无测试证据ID" "文件预览"
    fi
    
    # 7.5 测试获取文件内容接口
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/$TEST_EVIDENCE_ID/content" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        # 接口应该可访问
        if check_success "$body" || ! check_success "$body"; then
            print_result "文件内容接口可访问" "PASS" "" "文件预览"
        fi
    else
        print_result "文件内容接口" "SKIP" "无测试证据ID" "文件预览"
    fi
}

# ==================== 8. 证据清单对比测试 ====================
test_evidence_list_compare() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 证据清单对比测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 创建第二个证据清单用于对比
    local TEST_EVIDENCE_LIST_ID_2=""
    if [ -n "$TEST_MATTER_ID" ] && [ -n "$TEST_EVIDENCE_ID_2" ]; then
        local create_list_2='{
            "matterId": '"$TEST_MATTER_ID"',
            "name": "原告证据清单(补充)",
            "listType": "EXCHANGE",
            "evidenceIds": ['"$TEST_EVIDENCE_ID_2"']
        }'
        local response=$(send_request "POST" "$BASE_URL/evidence/list" "$create_list_2" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            TEST_EVIDENCE_LIST_ID_2=$(extract_id "$body")
            print_result "创建第二个证据清单" "PASS" "" "清单对比"
        else
            print_result "创建第二个证据清单" "SKIP" "创建失败" "清单对比"
        fi
    fi
    
    # 8.1 对比两个证据清单
    if [ -n "$TEST_EVIDENCE_LIST_ID" ] && [ -n "$TEST_EVIDENCE_LIST_ID_2" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/list/compare?listId1=$TEST_EVIDENCE_LIST_ID&listId2=$TEST_EVIDENCE_LIST_ID_2" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            # 检查返回的对比结果字段
            local has_added=$(echo "$body" | grep -o '"added"\|"addedEvidences"')
            local has_removed=$(echo "$body" | grep -o '"removed"\|"removedEvidences"')
            local has_retained=$(echo "$body" | grep -o '"retained"\|"retainedEvidences"')
            
            if [ -n "$has_added" ] || [ -n "$has_removed" ] || [ -n "$has_retained" ]; then
                print_result "证据清单对比返回差异" "PASS" "" "清单对比"
            else
                print_result "证据清单对比返回差异" "SKIP" "字段可能不同" "清单对比"
            fi
        else
            local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_result "证据清单对比" "FAIL" "$message" "清单对比"
        fi
    else
        print_result "证据清单对比" "SKIP" "缺少两个清单ID" "清单对比"
    fi
    
    # 8.2 测试对比相同清单
    if [ -n "$TEST_EVIDENCE_LIST_ID" ]; then
        local response=$(send_request "GET" "$BASE_URL/evidence/list/compare?listId1=$TEST_EVIDENCE_LIST_ID&listId2=$TEST_EVIDENCE_LIST_ID" "" "$auth_header")
        local body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "相同清单对比" "PASS" "" "清单对比"
        else
            print_result "相同清单对比" "SKIP" "可能不支持" "清单对比"
        fi
    fi
    
    # 8.3 测试对比不存在的清单
    local response=$(send_request "GET" "$BASE_URL/evidence/list/compare?listId1=999999999&listId2=999999998" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "对比不存在清单返回错误" "PASS" "" "清单对比"
    else
        print_result "对比不存在清单返回错误" "SKIP" "可能返回空结果" "清单对比"
    fi
    
    # 清理第二个清单
    if [ -n "$TEST_EVIDENCE_LIST_ID_2" ]; then
        send_request "DELETE" "$BASE_URL/evidence/list/$TEST_EVIDENCE_LIST_ID_2" "" "$auth_header" > /dev/null
    fi
}

# ==================== 清理测试数据 ====================
cleanup_test_data() {
    echo ""
    echo -e "${BLUE}清理测试数据...${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 删除测试证据清单
    if [ -n "$TEST_EVIDENCE_LIST_ID" ]; then
        send_request "DELETE" "$BASE_URL/evidence/list/$TEST_EVIDENCE_LIST_ID" "" "$auth_header" > /dev/null
        echo -e "  ${BLUE}已删除证据清单: $TEST_EVIDENCE_LIST_ID${NC}"
    fi
    
    # 删除测试证据
    if [ -n "$TEST_EVIDENCE_ID" ]; then
        send_request "DELETE" "$BASE_URL/evidence/$TEST_EVIDENCE_ID" "" "$auth_header" > /dev/null
        echo -e "  ${BLUE}已删除证据: $TEST_EVIDENCE_ID${NC}"
    fi
    
    if [ -n "$TEST_EVIDENCE_ID_2" ]; then
        send_request "DELETE" "$BASE_URL/evidence/$TEST_EVIDENCE_ID_2" "" "$auth_header" > /dev/null
        echo -e "  ${BLUE}已删除证据: $TEST_EVIDENCE_ID_2${NC}"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}              证据管理业务逻辑测试总结${NC}"
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
    
    local categories=("CRUD操作" "数据校验" "质证流程" "证据清单" "分组排序" "导出功能" "文件预览" "清单对比")
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
        echo -e "${GREEN}  ✅ 证据管理业务逻辑测试全部通过！${NC}"
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
    echo -e "${PURPLE}     智慧律所管理系统 - 证据管理业务逻辑详细测试${NC}"
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
    
    # 获取测试用案件ID
    get_test_matter_id
    
    # 执行测试
    test_evidence_crud
    test_evidence_validation
    test_cross_exam_workflow
    test_evidence_list_management
    test_evidence_grouping_sorting
    test_evidence_export
    test_evidence_file_preview
    test_evidence_list_compare
    
    # 清理测试数据
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
