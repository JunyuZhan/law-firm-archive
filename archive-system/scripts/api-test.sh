#!/bin/bash
# 档案管理系统API测试脚本
# 测试所有主要功能模块

set -e

BASE_URL="http://localhost:8090/api"
TOKEN=""
PASSED=0
FAILED=0
TOTAL=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印分隔线
print_separator() {
    echo ""
    echo "========================================"
    echo "$1"
    echo "========================================"
}

# 测试函数
test_api() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_success=$5
    
    TOTAL=$((TOTAL + 1))
    
    echo -n "  [$TOTAL] $name ... "
    
    if [ "$method" == "GET" ]; then
        if [ -n "$TOKEN" ]; then
            response=$(curl -s -X GET "$BASE_URL$endpoint" \
                -H "Authorization: Bearer $TOKEN" \
                -H "Content-Type: application/json")
        else
            response=$(curl -s -X GET "$BASE_URL$endpoint" \
                -H "Content-Type: application/json")
        fi
    elif [ "$method" == "POST" ]; then
        if [ -n "$TOKEN" ]; then
            response=$(curl -s -X POST "$BASE_URL$endpoint" \
                -H "Authorization: Bearer $TOKEN" \
                -H "Content-Type: application/json" \
                -d "$data")
        else
            response=$(curl -s -X POST "$BASE_URL$endpoint" \
                -H "Content-Type: application/json" \
                -d "$data")
        fi
    elif [ "$method" == "PUT" ]; then
        response=$(curl -s -X PUT "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data")
    elif [ "$method" == "DELETE" ]; then
        response=$(curl -s -X DELETE "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json")
    fi
    
    # 检查响应
    success=$(echo "$response" | grep -o '"success":[^,}]*' | head -1 | cut -d: -f2)
    
    if [ "$expected_success" == "true" ] && [ "$success" == "true" ]; then
        echo -e "${GREEN}通过${NC}"
        PASSED=$((PASSED + 1))
        return 0
    elif [ "$expected_success" == "false" ] && [ "$success" == "false" ]; then
        echo -e "${GREEN}通过${NC} (预期失败)"
        PASSED=$((PASSED + 1))
        return 0
    elif [ "$expected_success" == "any" ]; then
        echo -e "${GREEN}通过${NC}"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "${RED}失败${NC}"
        echo "    响应: $(echo $response | head -c 200)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

# 获取Token并提取
get_token() {
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "admin", "password": "admin123"}')
    
    TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$TOKEN" ]; then
        echo -e "  ${GREEN}登录成功，已获取Token${NC}"
        return 0
    else
        echo -e "  ${RED}登录失败${NC}"
        echo "  响应: $response"
        return 1
    fi
}

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        档案管理系统 API 测试脚本               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

# ==================== 1. 认证测试 ====================
print_separator "1. 认证模块测试"

echo "  获取认证Token..."
get_token

test_api "登录-错误密码" "POST" "/auth/login" \
    '{"username": "admin", "password": "wrongpassword"}' "false"

test_api "登录-用户不存在" "POST" "/auth/login" \
    '{"username": "nonexistent", "password": "password"}' "false"

test_api "获取当前用户信息" "GET" "/auth/me" "" "true"

# ==================== 2. 用户管理测试 ====================
print_separator "2. 用户管理测试"

test_api "获取用户列表" "GET" "/users?page=1&size=10" "" "true"

test_api "获取用户详情" "GET" "/users/1" "" "true"

test_api "获取角色列表" "GET" "/roles" "" "true"

# ==================== 3. 全宗管理测试 ====================
print_separator "3. 全宗管理测试"

test_api "获取全宗列表" "GET" "/fonds" "" "true"

test_api "获取全宗详情" "GET" "/fonds/1" "" "true"

test_api "创建全宗" "POST" "/fonds" \
    '{"fondsNo": "F-TEST-'$(date +%s)'", "fondsName": "脚本测试全宗", "description": "脚本测试创建"}' "true"

# ==================== 4. 分类管理测试 ====================
print_separator "4. 分类管理测试"

test_api "获取分类树" "GET" "/categories/tree" "" "true"

test_api "获取分类详情" "GET" "/categories/1" "" "true"

test_api "获取子分类" "GET" "/categories/1/children" "" "true"

# ==================== 5. 档案管理测试 ====================
print_separator "5. 档案管理测试"

test_api "获取档案列表" "GET" "/archives?page=1&size=10" "" "true"

test_api "按状态筛选档案" "GET" "/archives?status=RECEIVED" "" "true"

test_api "创建档案" "POST" "/archives" \
    '{"title": "脚本测试档案-'$(date +%s)'", "archiveType": "DOCUMENT", "retentionPeriod": "Y10", "fondsId": 1, "categoryId": 1, "securityLevel": "INTERNAL"}' "true"

# 获取刚创建的档案ID
ARCHIVE_RESPONSE=$(curl -s -X GET "$BASE_URL/archives?title=脚本测试档案" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json")
ARCHIVE_ID=$(echo "$ARCHIVE_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

if [ -n "$ARCHIVE_ID" ]; then
    test_api "获取档案详情" "GET" "/archives/$ARCHIVE_ID" "" "true"
fi

# ==================== 6. 借阅管理测试 ====================
print_separator "6. 借阅管理测试"

test_api "获取我的借阅列表" "GET" "/borrows/my" "" "true"

if [ -n "$ARCHIVE_ID" ]; then
    test_api "提交借阅申请" "POST" "/borrows/apply" \
        '{"archiveId": '$ARCHIVE_ID', "borrowPurpose": "脚本测试借阅", "expectedReturnDate": "2026-03-15"}' "true"
fi

# ==================== 7. 统计报表测试 ====================
print_separator "7. 统计报表测试"

test_api "获取统计概览" "GET" "/statistics/overview" "" "true"

test_api "获取档案类型统计" "GET" "/statistics/by-type" "" "true"

test_api "获取档案状态统计" "GET" "/statistics/by-status" "" "true"

test_api "获取借阅统计" "GET" "/statistics/borrow" "" "true"

test_api "获取存储统计" "GET" "/statistics/storage" "" "true"

# ==================== 8. 系统配置测试 ====================
print_separator "8. 系统配置测试"

test_api "获取系统配置列表" "GET" "/configs" "" "true"

test_api "获取配置详情" "GET" "/configs/system.upload.max.size" "" "true"

test_api "获取系统组配置" "GET" "/configs/group/SYSTEM" "" "true"

# ==================== 9. 日志查询测试 ====================
print_separator "9. 日志查询测试"

test_api "获取操作日志" "GET" "/operation-logs?pageNum=1&pageSize=10" "" "true"

test_api "获取日志统计" "GET" "/operation-logs/statistics" "" "true"

# ==================== 10. 保管期限测试 ====================
print_separator "10. 保管期限测试"

test_api "获取保管期限配置" "GET" "/configs/retention" "" "true"

# ==================== 结果统计 ====================
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  测试结果汇总                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  总测试数: ${YELLOW}$TOTAL${NC}"
echo -e "  通过: ${GREEN}$PASSED${NC}"
echo -e "  失败: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过!${NC}"
    exit 0
else
    echo -e "${RED}✗ 有 $FAILED 个测试失败${NC}"
    exit 1
fi
