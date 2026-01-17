#!/bin/bash

# 合同审批表案由显示测试脚本
# 测试：民事、刑事、行政案件的案由/罪名在审批表中的显示是否正确

BASE_URL="http://localhost:8080/api"
TOKEN=""
TEST_CLIENT_ID=""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "=========================================="
echo "合同审批表案由显示测试"
echo "=========================================="

# 发送请求函数
send_request() {
    local method=$1
    local url=$2
    local data=$3
    local token=$4
    
    if [ -z "$data" ]; then
        curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$url" \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json"
    else
        curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$url" \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d "$data"
    fi
}

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" = "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}✓${NC} $test_name: ${GREEN}PASS${NC}"
        if [ -n "$message" ]; then
            echo "  $message"
        fi
    elif [ "$status" = "FAIL" ]; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}✗${NC} $test_name: ${RED}FAIL${NC}"
        if [ -n "$message" ]; then
            echo "  $message"
        fi
    else
        echo -e "${YELLOW}⊘${NC} $test_name: ${YELLOW}SKIP${NC}"
        if [ -n "$message" ]; then
            echo "  $message"
        fi
    fi
}

# ==================== 1. 登录获取Token ====================
echo -e "\n${CYAN}[1/5] 登录获取Token...${NC}"
LOGIN_RESPONSE=$(send_request "POST" "/auth/login" '{"username":"admin","password":"admin123"}')
HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -1)
BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    if [ -n "$TOKEN" ]; then
        print_result "登录获取Token" "PASS" "Token获取成功"
    else
        print_result "登录获取Token" "FAIL" "无法从响应中提取Token"
        echo "$BODY"
        exit 1
    fi
else
    print_result "登录获取Token" "FAIL" "HTTP状态码: $HTTP_CODE"
    echo "$BODY"
    exit 1
fi

# ==================== 2. 获取或创建测试客户 ====================
echo -e "\n${CYAN}[2/5] 获取或创建测试客户...${NC}"

# 先尝试获取客户列表，找到一个可用的客户
CLIENT_LIST=$(send_request "GET" "/client/list?pageNum=1&pageSize=10" "" "$TOKEN")
HTTP_CODE=$(echo "$CLIENT_LIST" | tail -1)
BODY=$(echo "$CLIENT_LIST" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    TEST_CLIENT_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -n "$TEST_CLIENT_ID" ]; then
        print_result "获取测试客户" "PASS" "使用现有客户ID: $TEST_CLIENT_ID"
    else
        # 创建测试客户
        echo "  创建新的测试客户..."
        CREATE_CLIENT_DATA='{
            "name":"案由测试客户",
            "clientType":"INDIVIDUAL",
            "idCard":"110101199001011234",
            "contactPhone":"13900001111",
            "source":"自动化测试",
            "level":"B"
        }'
        CREATE_RESPONSE=$(send_request "POST" "/client" "$CREATE_CLIENT_DATA" "$TOKEN")
        HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -1)
        CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')
        
        if [ "$HTTP_CODE" = "200" ]; then
            TEST_CLIENT_ID=$(echo "$CREATE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            if [ -n "$TEST_CLIENT_ID" ]; then
                print_result "创建测试客户" "PASS" "客户ID: $TEST_CLIENT_ID"
            else
                print_result "创建测试客户" "FAIL" "无法获取客户ID"
                exit 1
            fi
        else
            print_result "创建测试客户" "FAIL" "HTTP状态码: $HTTP_CODE"
            echo "$CREATE_BODY"
            exit 1
        fi
    fi
else
    print_result "获取测试客户" "FAIL" "HTTP状态码: $HTTP_CODE"
    echo "$BODY"
    exit 1
fi

# ==================== 3. 测试民事合同案由显示 ====================
echo -e "\n${CYAN}[3/5] 测试民事合同案由显示...${NC}"

CIVIL_CONTRACT_DATA="{
    \"name\":\"民事合同案由测试-$(date +%s)\",
    \"clientId\":$TEST_CLIENT_ID,
    \"contractType\":\"CIVIL_PROXY\",
    \"feeType\":\"FIXED\",
    \"totalAmount\":30000,
    \"currency\":\"CNY\",
    \"signDate\":\"$(date +%Y-%m-%d)\",
    \"caseType\":\"CIVIL\",
    \"causeOfAction\":\"14\",
    \"trialStage\":\"FIRST_INSTANCE\",
    \"opposingParty\":\"对方当事人A\"
}"

CREATE_RESPONSE=$(send_request "POST" "/matter/contract" "$CIVIL_CONTRACT_DATA" "$TOKEN")
HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -1)
CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    CIVIL_CONTRACT_ID=$(echo "$CREATE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -n "$CIVIL_CONTRACT_ID" ]; then
        print_result "创建民事合同" "PASS" "合同ID: $CIVIL_CONTRACT_ID"
        
        # 获取合同打印数据
        PRINT_RESPONSE=$(send_request "GET" "/matter/contract/$CIVIL_CONTRACT_ID/print-data" "" "$TOKEN")
        HTTP_CODE=$(echo "$PRINT_RESPONSE" | tail -1)
        PRINT_BODY=$(echo "$PRINT_RESPONSE" | sed '$d')
        
        if [ "$HTTP_CODE" = "200" ]; then
            CAUSE_OF_ACTION_NAME=$(echo "$PRINT_BODY" | grep -o '"causeOfActionName":"[^"]*"' | cut -d'"' -f4)
            CAUSE_OF_ACTION=$(echo "$PRINT_BODY" | grep -o '"causeOfAction":"[^"]*"' | cut -d'"' -f4)
            
            if [ -n "$CAUSE_OF_ACTION_NAME" ] && [ "$CAUSE_OF_ACTION_NAME" != "" ] && [ "$CAUSE_OF_ACTION_NAME" != "民事" ]; then
                print_result "民事合同案由名称" "PASS" "案由代码: $CAUSE_OF_ACTION, 案由名称: $CAUSE_OF_ACTION_NAME"
            else
                print_result "民事合同案由名称" "FAIL" "案由名称为空或显示错误: '$CAUSE_OF_ACTION_NAME'"
            fi
        else
            print_result "获取合同打印数据" "FAIL" "HTTP状态码: $HTTP_CODE"
        fi
    else
        print_result "创建民事合同" "FAIL" "无法获取合同ID"
    fi
else
    print_result "创建民事合同" "FAIL" "HTTP状态码: $HTTP_CODE"
    echo "$CREATE_BODY"
fi

# ==================== 4. 测试刑事案件罪名显示 ====================
echo -e "\n${CYAN}[4/5] 测试刑事案件罪名显示...${NC}"

CRIMINAL_CONTRACT_DATA="{
    \"name\":\"刑事案件罪名测试-$(date +%s)\",
    \"clientId\":$TEST_CLIENT_ID,
    \"contractType\":\"CRIMINAL_DEFENSE\",
    \"feeType\":\"FIXED\",
    \"totalAmount\":50000,
    \"currency\":\"CNY\",
    \"signDate\":\"$(date +%Y-%m-%d)\",
    \"caseType\":\"CRIMINAL\",
    \"causeOfAction\":\"1.1\",
    \"trialStage\":\"INVESTIGATION\",
    \"defendantName\":\"张三\",
    \"criminalCharge\":\"盗窃罪\"
}"

CREATE_RESPONSE=$(send_request "POST" "/matter/contract" "$CRIMINAL_CONTRACT_DATA" "$TOKEN")
HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -1)
CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    CRIMINAL_CONTRACT_ID=$(echo "$CREATE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -n "$CRIMINAL_CONTRACT_ID" ]; then
        print_result "创建刑事案件合同" "PASS" "合同ID: $CRIMINAL_CONTRACT_ID"
        
        # 获取合同打印数据
        PRINT_RESPONSE=$(send_request "GET" "/matter/contract/$CRIMINAL_CONTRACT_ID/print" "" "$TOKEN")
        HTTP_CODE=$(echo "$PRINT_RESPONSE" | tail -1)
        PRINT_BODY=$(echo "$PRINT_RESPONSE" | sed '$d')
        
        if [ "$HTTP_CODE" = "200" ]; then
            CAUSE_OF_ACTION_NAME=$(echo "$PRINT_BODY" | grep -o '"causeOfActionName":"[^"]*"' | cut -d'"' -f4)
            CAUSE_OF_ACTION=$(echo "$PRINT_BODY" | grep -o '"causeOfAction":"[^"]*"' | cut -d'"' -f4)
            CASE_TYPE=$(echo "$PRINT_BODY" | grep -o '"caseType":"[^"]*"' | cut -d'"' -f4)
            
            # 关键测试：刑事案件案由名称不应是"刑事案件"
            if [ -n "$CAUSE_OF_ACTION_NAME" ] && [ "$CAUSE_OF_ACTION_NAME" != "" ] && [ "$CAUSE_OF_ACTION_NAME" != "刑事案件" ]; then
                print_result "刑事案件罪名名称" "PASS" "罪名代码: $CAUSE_OF_ACTION, 罪名名称: $CAUSE_OF_ACTION_NAME"
            else
                print_result "刑事案件罪名名称" "FAIL" "罪名名称为空或显示为'刑事案件': '$CAUSE_OF_ACTION_NAME' (应为具体罪名)"
                echo "  详情: caseType=$CASE_TYPE, causeOfAction=$CAUSE_OF_ACTION"
            fi
        else
            print_result "获取合同打印数据" "FAIL" "HTTP状态码: $HTTP_CODE"
        fi
    else
        print_result "创建刑事案件合同" "FAIL" "无法获取合同ID"
    fi
else
    print_result "创建刑事案件合同" "FAIL" "HTTP状态码: $HTTP_CODE"
    echo "$CREATE_BODY"
fi

# ==================== 5. 测试行政案件案由显示 ====================
echo -e "\n${CYAN}[5/5] 测试行政案件案由显示...${NC}"

ADMIN_CONTRACT_DATA="{
    \"name\":\"行政案件案由测试-$(date +%s)\",
    \"clientId\":$TEST_CLIENT_ID,
    \"contractType\":\"ADMINISTRATIVE_PROXY\",
    \"feeType\":\"FIXED\",
    \"totalAmount\":40000,
    \"currency\":\"CNY\",
    \"signDate\":\"$(date +%Y-%m-%d)\",
    \"caseType\":\"ADMINISTRATIVE\",
    \"causeOfAction\":\"1\",
    \"trialStage\":\"FIRST_INSTANCE\",
    \"opposingParty\":\"某政府部门\"
}"

CREATE_RESPONSE=$(send_request "POST" "/matter/contract" "$ADMIN_CONTRACT_DATA" "$TOKEN")
HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -1)
CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    ADMIN_CONTRACT_ID=$(echo "$CREATE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -n "$ADMIN_CONTRACT_ID" ]; then
        print_result "创建行政案件合同" "PASS" "合同ID: $ADMIN_CONTRACT_ID"
        
        # 获取合同打印数据
        PRINT_RESPONSE=$(send_request "GET" "/matter/contract/$ADMIN_CONTRACT_ID/print" "" "$TOKEN")
        HTTP_CODE=$(echo "$PRINT_RESPONSE" | tail -1)
        PRINT_BODY=$(echo "$PRINT_RESPONSE" | sed '$d')
        
        if [ "$HTTP_CODE" = "200" ]; then
            CAUSE_OF_ACTION_NAME=$(echo "$PRINT_BODY" | grep -o '"causeOfActionName":"[^"]*"' | cut -d'"' -f4)
            CAUSE_OF_ACTION=$(echo "$PRINT_BODY" | grep -o '"causeOfAction":"[^"]*"' | cut -d'"' -f4)
            
            if [ -n "$CAUSE_OF_ACTION_NAME" ] && [ "$CAUSE_OF_ACTION_NAME" != "" ] && [ "$CAUSE_OF_ACTION_NAME" != "行政" ]; then
                print_result "行政案件案由名称" "PASS" "案由代码: $CAUSE_OF_ACTION, 案由名称: $CAUSE_OF_ACTION_NAME"
            else
                print_result "行政案件案由名称" "FAIL" "案由名称为空或显示错误: '$CAUSE_OF_ACTION_NAME'"
            fi
        else
            print_result "获取合同打印数据" "FAIL" "HTTP状态码: $HTTP_CODE"
        fi
    else
        print_result "创建行政案件合同" "FAIL" "无法获取合同ID"
    fi
else
    print_result "创建行政案件合同" "FAIL" "HTTP状态码: $HTTP_CODE"
    echo "$CREATE_BODY"
fi

# ==================== 测试总结 ====================
echo ""
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
if [ "$FAILED_TESTS" -gt 0 ]; then
    echo -e "${RED}失败: $FAILED_TESTS${NC}"
else
    echo -e "失败: $FAILED_TESTS"
fi

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo -e "\n${GREEN}✅ 所有测试通过！${NC}"
    exit 0
else
    echo -e "\n${RED}❌ 部分测试失败，请检查上述输出${NC}"
    exit 1
fi
