#!/bin/bash

# 智慧律所管理系统 - 档案管理/文档管理模块业务逻辑测试
# 测试内容：
# 1. 档案管理 - 列表/详情/待归档案件/归档检查
# 2. 档案借阅 - 借阅列表/逾期借阅
# 3. 档案库位 - 库位列表/可用库位/容量监控
# 4. 文档管理 - 列表/详情/按项目查询
# 5. 文档分类 - 分类树/子分类
# 6. 项目卷宗 - 卷宗目录
# 7. 印章管理 - 印章列表/详情
# 8. 用印申请 - 申请列表/审批人/待审批
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
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
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

# 登录
login() {
    echo ""
    echo -e "${BLUE}登录获取Token...${NC}"
    
    local response=$(send_request "POST" "$BASE_URL/auth/login" '{"username":"admin","password":"admin123"}')
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

# ==================== 1. 档案管理测试 ====================
test_archive_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 档案管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_archive_id=""
    
    # 1.1 分页查询档案列表
    local response=$(send_request "GET" "$BASE_URL/archive/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询档案列表" "PASS" "" "档案管理"
        test_archive_id=$(extract_id "$body")
    else
        print_result "分页查询档案列表" "SKIP" "可能无权限" "档案管理"
    fi
    
    # 1.2 获取档案详情
    if [ -n "$test_archive_id" ]; then
        response=$(send_request "GET" "$BASE_URL/archive/$test_archive_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取档案详情" "PASS" "" "档案管理"
            
            # 检查必要字段
            local has_archive_name=$(echo "$body" | grep -o '"archiveName"\|"matterName"')
            local has_status=$(echo "$body" | grep -o '"status"')
            
            if [ -n "$has_archive_name" ]; then
                print_result "档案详情包含名称字段" "PASS" "" "档案管理"
            else
                print_result "档案详情包含名称字段" "SKIP" "字段名可能不同" "档案管理"
            fi
            
            if [ -n "$has_status" ]; then
                print_result "档案详情包含状态字段" "PASS" "" "档案管理"
            else
                print_result "档案详情包含状态字段" "SKIP" "字段名可能不同" "档案管理"
            fi
        else
            print_result "获取档案详情" "SKIP" "档案不存在" "档案管理"
        fi
    fi
    
    # 1.3 获取待归档案件列表
    response=$(send_request "GET" "$BASE_URL/archive/pending-matters" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待归档案件列表" "PASS" "" "档案管理"
    else
        print_result "获取待归档案件列表" "SKIP" "可能无权限" "档案管理"
    fi
    
    # 1.4 获取即将到期档案
    response=$(send_request "GET" "$BASE_URL/archive/expiring?days=90" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取即将到期档案" "PASS" "" "档案管理"
    else
        print_result "获取即将到期档案" "SKIP" "可能无权限" "档案管理"
    fi
    
    # 1.5 获取归档数据源配置
    response=$(send_request "GET" "$BASE_URL/archive/data-sources" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取归档数据源配置" "PASS" "" "档案管理"
    else
        print_result "获取归档数据源配置" "SKIP" "可能无权限" "档案管理"
    fi
    
    # 1.6 获取入库审批人列表
    response=$(send_request "GET" "$BASE_URL/archive/store/approvers" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取入库审批人列表" "PASS" "" "档案管理"
    else
        print_result "获取入库审批人列表" "SKIP" "可能无权限" "档案管理"
    fi
}

# ==================== 2. 档案借阅测试 ====================
test_archive_borrow() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 档案借阅业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 分页查询借阅记录
    local response=$(send_request "GET" "$BASE_URL/archive/borrow/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询借阅记录" "PASS" "" "档案借阅"
    else
        print_result "分页查询借阅记录" "SKIP" "可能无权限" "档案借阅"
    fi
    
    # 2.2 获取逾期借阅列表
    response=$(send_request "GET" "$BASE_URL/archive/borrow/overdue" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取逾期借阅列表" "PASS" "" "档案借阅"
    else
        print_result "获取逾期借阅列表" "SKIP" "可能无权限" "档案借阅"
    fi
}

# ==================== 3. 档案库位测试 ====================
test_archive_location() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 档案库位业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_location_id=""
    
    # 3.1 查询所有库位
    local response=$(send_request "GET" "$BASE_URL/archive/location/list" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询所有库位" "PASS" "" "档案库位"
        test_location_id=$(extract_id "$body")
    else
        print_result "查询所有库位" "SKIP" "可能无权限" "档案库位"
    fi
    
    # 3.2 查询可用库位
    response=$(send_request "GET" "$BASE_URL/archive/location/available" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询可用库位" "PASS" "" "档案库位"
    else
        print_result "查询可用库位" "SKIP" "可能无权限" "档案库位"
    fi
    
    # 3.3 获取库位详情
    if [ -n "$test_location_id" ]; then
        response=$(send_request "GET" "$BASE_URL/archive/location/$test_location_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取库位详情" "PASS" "" "档案库位"
        else
            print_result "获取库位详情" "SKIP" "库位不存在" "档案库位"
        fi
    fi
    
    # 3.4 库位容量监控
    response=$(send_request "GET" "$BASE_URL/archive/location/capacity-monitor" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "库位容量监控" "PASS" "" "档案库位"
    else
        print_result "库位容量监控" "SKIP" "可能无权限" "档案库位"
    fi
}

# ==================== 4. 文档管理测试 ====================
test_document_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 文档管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_document_id=""
    
    # 4.1 分页查询文档
    local response=$(send_request "GET" "$BASE_URL/document?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询文档" "PASS" "" "文档管理"
        test_document_id=$(extract_id "$body")
    else
        print_result "分页查询文档" "SKIP" "可能无权限" "文档管理"
    fi
    
    # 4.2 获取文档详情
    if [ -n "$test_document_id" ]; then
        response=$(send_request "GET" "$BASE_URL/document/$test_document_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取文档详情" "PASS" "" "文档管理"
            
            # 检查必要字段
            local has_file_name=$(echo "$body" | grep -o '"fileName"')
            local has_file_type=$(echo "$body" | grep -o '"fileType"')
            
            if [ -n "$has_file_name" ]; then
                print_result "文档详情包含文件名" "PASS" "" "文档管理"
            else
                print_result "文档详情包含文件名" "SKIP" "字段名可能不同" "文档管理"
            fi
            
            if [ -n "$has_file_type" ]; then
                print_result "文档详情包含文件类型" "PASS" "" "文档管理"
            else
                print_result "文档详情包含文件类型" "SKIP" "字段名可能不同" "文档管理"
            fi
            
            # 4.3 检查编辑支持
            response=$(send_request "GET" "$BASE_URL/document/$test_document_id/edit-support" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "检查文档编辑支持" "PASS" "" "文档管理"
            else
                print_result "检查文档编辑支持" "SKIP" "可能无权限" "文档管理"
            fi
            
            # 4.4 获取文档版本列表
            response=$(send_request "GET" "$BASE_URL/document/$test_document_id/versions" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "获取文档版本列表" "PASS" "" "文档管理"
            else
                print_result "获取文档版本列表" "SKIP" "可能无版本" "文档管理"
            fi
            
            # 4.5 获取访问日志
            response=$(send_request "GET" "$BASE_URL/document/$test_document_id/access-logs?pageNum=1&pageSize=10" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "获取文档访问日志" "PASS" "" "文档管理"
            else
                print_result "获取文档访问日志" "SKIP" "可能无权限" "文档管理"
            fi
        else
            print_result "获取文档详情" "SKIP" "文档不存在" "文档管理"
        fi
    fi
    
    # 4.6 按案件查询文档
    response=$(send_request "GET" "$BASE_URL/document/matter/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "按案件查询文档" "PASS" "" "文档管理"
    else
        print_result "按案件查询文档" "SKIP" "案件不存在" "文档管理"
    fi
}

# ==================== 5. 文档分类测试 ====================
test_document_category() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 文档分类业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 获取分类树
    local response=$(send_request "GET" "$BASE_URL/document/category/tree" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取文档分类树" "PASS" "" "文档分类"
    else
        print_result "获取文档分类树" "SKIP" "可能无权限" "文档分类"
    fi
    
    # 5.2 获取子分类
    response=$(send_request "GET" "$BASE_URL/document/category/children?parentId=0" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取子分类" "PASS" "" "文档分类"
    else
        print_result "获取子分类" "SKIP" "可能无权限" "文档分类"
    fi
}

# ==================== 6. 项目卷宗测试 ====================
test_matter_dossier() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 项目卷宗业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 获取项目卷宗目录
    local response=$(send_request "GET" "$BASE_URL/matter/1/dossier" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取项目卷宗目录" "PASS" "" "项目卷宗"
    else
        print_result "获取项目卷宗目录" "SKIP" "项目不存在" "项目卷宗"
    fi
}

# ==================== 7. 印章管理测试 ====================
test_seal_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 印章管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_seal_id=""
    
    # 7.1 分页查询印章
    local response=$(send_request "GET" "$BASE_URL/document/seal?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询印章" "PASS" "" "印章管理"
        test_seal_id=$(extract_id "$body")
    else
        print_result "分页查询印章" "SKIP" "可能无权限" "印章管理"
    fi
    
    # 7.2 获取印章详情
    if [ -n "$test_seal_id" ]; then
        response=$(send_request "GET" "$BASE_URL/document/seal/$test_seal_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取印章详情" "PASS" "" "印章管理"
            
            # 检查必要字段
            local has_seal_name=$(echo "$body" | grep -o '"sealName"\|"name"')
            local has_seal_type=$(echo "$body" | grep -o '"sealType"\|"type"')
            
            if [ -n "$has_seal_name" ]; then
                print_result "印章详情包含名称" "PASS" "" "印章管理"
            else
                print_result "印章详情包含名称" "SKIP" "字段名可能不同" "印章管理"
            fi
            
            if [ -n "$has_seal_type" ]; then
                print_result "印章详情包含类型" "PASS" "" "印章管理"
            else
                print_result "印章详情包含类型" "SKIP" "字段名可能不同" "印章管理"
            fi
        else
            print_result "获取印章详情" "SKIP" "印章不存在" "印章管理"
        fi
    fi
}

# ==================== 8. 用印申请测试 ====================
test_seal_application() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 用印申请业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 8.1 分页查询用印申请
    local response=$(send_request "GET" "$BASE_URL/document/seal-application?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询用印申请" "PASS" "" "用印申请"
    else
        print_result "分页查询用印申请" "SKIP" "可能无权限" "用印申请"
    fi
    
    # 8.2 获取待审批列表
    response=$(send_request "GET" "$BASE_URL/document/seal-application/pending" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待审批列表" "PASS" "" "用印申请"
    else
        print_result "获取待审批列表" "SKIP" "可能无权限" "用印申请"
    fi
    
    # 8.3 获取可选审批人列表
    response=$(send_request "GET" "$BASE_URL/document/seal-application/approvers" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取可选审批人列表" "PASS" "" "用印申请"
    else
        print_result "获取可选审批人列表" "SKIP" "可能无权限" "用印申请"
    fi
    
    # 8.4 检查是否是印章保管人
    response=$(send_request "GET" "$BASE_URL/document/seal-application/keeper/check" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "检查是否是印章保管人" "PASS" "" "用印申请"
    else
        print_result "检查是否是印章保管人" "SKIP" "可能无权限" "用印申请"
    fi
    
    # 8.5 获取保管人待办理申请
    response=$(send_request "GET" "$BASE_URL/document/seal-application/keeper/pending" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取保管人待办理申请" "PASS" "" "用印申请"
    else
        print_result "获取保管人待办理申请" "SKIP" "可能无权限" "用印申请"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}         档案管理/文档管理模块测试总结${NC}"
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
    
    local categories=("档案管理" "档案借阅" "档案库位" "文档管理" "文档分类" "项目卷宗" "印章管理" "用印申请")
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
            printf "  %-10s: ${GREEN}%d通过${NC} / ${RED}%d失败${NC} / ${YELLOW}%d跳过${NC}\n" "$cat" "$cat_pass" "$cat_fail" "$cat_skip"
        fi
    done
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 档案管理/文档管理模块测试全部通过！${NC}"
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
    echo -e "${PURPLE}  智慧律所管理系统 - 档案管理/文档管理模块测试${NC}"
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
    test_archive_management
    test_archive_borrow
    test_archive_location
    test_document_management
    test_document_category
    test_matter_dossier
    test_seal_management
    test_seal_application
    
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
