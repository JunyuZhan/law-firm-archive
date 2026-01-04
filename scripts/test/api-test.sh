#!/bin/bash

# API测试脚本
# 用于测试后端API接口

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0

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
    local headers=$4
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -H "$headers" \
                -d "$data"
        else
            curl -s -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -d "$data"
        fi
    else
        if [ -n "$headers" ]; then
            curl -s -X "$method" "$url" \
                -H "$headers"
        else
            curl -s -X "$method" "$url"
        fi
    fi
}

# 检查服务是否运行
check_service() {
    echo "检查后端服务状态..."
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} 后端服务运行中"
        return 0
    else
        echo -e "${RED}✗${NC} 后端服务未运行，请先启动后端服务"
        echo "  启动命令: cd backend && mvn spring-boot:run"
        return 1
    fi
}

# 测试登录
test_login() {
    echo ""
    echo "=== 测试登录接口 ==="
    
    local response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
    local success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        print_result "用户登录" "PASS"
        echo "  Token: ${TOKEN:0:50}..."
        return 0
    else
        local message=$(echo "$response" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "用户登录" "FAIL" "$message"
        return 1
    fi
}

# 测试获取用户信息
test_get_user_info() {
    echo ""
    echo "=== 测试获取用户信息 ==="
    
    if [ -z "$TOKEN" ]; then
        print_result "获取用户信息" "FAIL" "未登录"
        return 1
    fi
    
    local response=$(send_request "GET" "$BASE_URL/auth/info" "" "Authorization: Bearer $TOKEN")
    local success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "获取用户信息" "PASS"
    else
        local message=$(echo "$response" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "获取用户信息" "FAIL" "$message"
    fi
}

# 测试客户列表
test_client_list() {
    echo ""
    echo "=== 测试客户列表接口 ==="
    
    if [ -z "$TOKEN" ]; then
        print_result "客户列表查询" "FAIL" "未登录"
        return 1
    fi
    
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=10" "" "Authorization: Bearer $TOKEN")
    local success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "客户列表查询" "PASS"
    else
        local message=$(echo "$response" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "客户列表查询" "FAIL" "$message"
    fi
}

# 测试项目列表
test_matter_list() {
    echo ""
    echo "=== 测试项目列表接口 ==="
    
    if [ -z "$TOKEN" ]; then
        print_result "项目列表查询" "FAIL" "未登录"
        return 1
    fi
    
    local response=$(send_request "GET" "$BASE_URL/matter/list?pageNum=1&pageSize=10" "" "Authorization: Bearer $TOKEN")
    local success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "项目列表查询" "PASS"
    else
        local message=$(echo "$response" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "项目列表查询" "FAIL" "$message"
    fi
}

# 测试工作台统计
test_workbench_stats() {
    echo ""
    echo "=== 测试工作台统计接口 ==="
    
    if [ -z "$TOKEN" ]; then
        print_result "工作台统计" "FAIL" "未登录"
        return 1
    fi
    
    local response=$(send_request "GET" "$BASE_URL/workbench/stats" "" "Authorization: Bearer $TOKEN")
    local success=$(echo "$response" | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    if [ "$success" = "true" ]; then
        print_result "工作台统计" "PASS"
    else
        local message=$(echo "$response" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "工作台统计" "FAIL" "$message"
    fi
}

# 打印测试总结
print_summary() {
    echo ""
    echo "=========================================="
    echo "测试总结"
    echo "=========================================="
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}所有测试通过！${NC}"
        exit 0
    else
        echo -e "${RED}有 $FAILED 个测试失败${NC}"
        exit 1
    fi
}

# 主函数
main() {
    echo "=========================================="
    echo "智慧律所管理系统 API 测试"
    echo "=========================================="
    
    # 检查服务
    if ! check_service; then
        exit 1
    fi
    
    # 执行测试
    test_login
    if [ $? -eq 0 ]; then
        test_get_user_info
        test_client_list
        test_matter_list
        test_workbench_stats
    fi
    
    # 打印总结
    print_summary
}

# 运行主函数
main

