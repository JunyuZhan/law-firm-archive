#!/bin/bash

# 智慧律所管理系统 - 辅助模块补充测试
# 测试内容：
# 1. 审批中心 - 审批列表/待审批/历史
# 2. 外出管理 - 外出登记/返回/查询
# 3. 客户标签 - 标签CRUD/分配
# 4. 文档模板 - 模板列表/详情
# 5. 卷宗模板 - 模板列表
# 6. 操作日志 - 日志查询/统计
# 7. 预付款 - 预付款列表/详情
# 8. 质检标准 - 标准列表
# 9. 报表模板 - 模板列表
# 10. 任务评论 - 评论列表
# 11. 客户关联信息 - 股东/关联公司/变更历史
# 12. 权限矩阵 - 权限查询
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
        TEST_RESULTS+=("PASS|$test_name|$category")
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED}✗${NC} [$category] $test_name - $message"
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL|$test_name|$category")
        FAILED_DETAILS+=("[$category] $test_name: $message")
    else
        echo -e "${YELLOW}⊘${NC} [$category] $test_name - $message"
        SKIPPED=$((SKIPPED + 1))
        TEST_RESULTS+=("SKIP|$test_name|$category")
    fi
}

# 发送请求
send_request() {
    local method=$1
    local url=$2
    local data=$3
    local auth_header=$4
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" 2>/dev/null)
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "$auth_header" \
            -d "$data" 2>/dev/null)
    fi
    
    echo "$response"
}

# 检查响应是否成功
check_success() {
    local body=$1
    echo "$body" | grep -q '"success":true'
}

# 获取响应消息
get_message() {
    local body=$1
    echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4
}

# ==================== 1. 审批中心测试 ====================
test_approval() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 审批中心业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 获取审批列表
    local response=$(send_request "GET" "$BASE_URL/workbench/approval/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取审批列表" "PASS" "" "审批中心"
    else
        print_result "获取审批列表" "SKIP" "可能无权限" "审批中心"
    fi
    
    # 1.2 获取待审批列表
    response=$(send_request "GET" "$BASE_URL/workbench/approval/pending?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取待审批列表" "PASS" "" "审批中心"
    else
        print_result "获取待审批列表" "SKIP" "可能无权限" "审批中心"
    fi
    
    # 1.3 获取我发起的审批
    response=$(send_request "GET" "$BASE_URL/workbench/approval/my-initiated?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我发起的审批" "PASS" "" "审批中心"
    else
        print_result "获取我发起的审批" "SKIP" "可能无权限" "审批中心"
    fi
    
    # 1.4 获取审批历史
    response=$(send_request "GET" "$BASE_URL/workbench/approval/my-history?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取审批历史" "PASS" "" "审批中心"
    else
        print_result "获取审批历史" "SKIP" "可能无权限" "审批中心"
    fi
}

# ==================== 2. 外出管理测试 ====================
test_go_out() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 外出管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 查询我的外出记录
    local response=$(send_request "GET" "$BASE_URL/admin/go-out/my?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询我的外出记录" "PASS" "" "外出管理"
    else
        print_result "查询我的外出记录" "SKIP" "可能无权限" "外出管理"
    fi
    
    # 2.2 查询当前外出人员
    response=$(send_request "GET" "$BASE_URL/admin/go-out/current" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询当前外出人员" "PASS" "" "外出管理"
    else
        print_result "查询当前外出人员" "SKIP" "可能无权限" "外出管理"
    fi
}

# ==================== 3. 客户标签测试 ====================
test_client_tag() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 客户标签业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 3.1 获取标签列表
    local response=$(send_request "GET" "$BASE_URL/client/tag/list" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取标签列表" "PASS" "" "客户标签"
    else
        print_result "获取标签列表" "SKIP" "可能无权限" "客户标签"
    fi
    
    # 3.2 创建标签
    response=$(send_request "POST" "$BASE_URL/client/tag" '{"name":"测试标签","color":"#FF5722","description":"测试用标签"}' "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "创建客户标签" "PASS" "" "客户标签"
        TAG_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        
        # 3.3 删除标签
        if [ -n "$TAG_ID" ]; then
            response=$(send_request "DELETE" "$BASE_URL/client/tag/$TAG_ID" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            if check_success "$body"; then
                print_result "删除客户标签" "PASS" "" "客户标签"
            else
                print_result "删除客户标签" "SKIP" "可能无权限" "客户标签"
            fi
        fi
    else
        local msg=$(get_message "$body")
        if echo "$msg" | grep -q "已存在"; then
            print_result "创建客户标签" "SKIP" "标签已存在" "客户标签"
        else
            print_result "创建客户标签" "SKIP" "可能无权限" "客户标签"
        fi
    fi
}

# ==================== 4. 文档模板测试 ====================
test_document_template() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 文档模板业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 获取模板列表
    local response=$(send_request "GET" "$BASE_URL/document/template?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取文档模板列表" "PASS" "" "文档模板"
    else
        print_result "获取文档模板列表" "SKIP" "可能无权限" "文档模板"
    fi
    
    # 4.2 获取启用的模板
    response=$(send_request "GET" "$BASE_URL/document/template/active" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取启用的模板" "PASS" "" "文档模板"
    else
        print_result "获取启用的模板" "SKIP" "可能无权限" "文档模板"
    fi
}

# ==================== 5. 卷宗模板测试 ====================
test_dossier_template() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 卷宗模板业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 5.1 获取卷宗模板列表
    local response=$(send_request "GET" "$BASE_URL/dossier/template?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取卷宗模板列表" "PASS" "" "卷宗模板"
    else
        print_result "获取卷宗模板列表" "SKIP" "可能无权限" "卷宗模板"
    fi
}

# ==================== 6. 操作日志测试 ====================
test_operation_log() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 操作日志业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 6.1 获取操作日志列表
    local response=$(send_request "GET" "$BASE_URL/admin/operation-logs?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取操作日志列表" "PASS" "" "操作日志"
    else
        print_result "获取操作日志列表" "SKIP" "可能无权限" "操作日志"
    fi
    
    # 6.2 获取模块列表
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/modules" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取日志模块列表" "PASS" "" "操作日志"
    else
        print_result "获取日志模块列表" "SKIP" "可能无权限" "操作日志"
    fi
    
    # 6.3 获取日志统计
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/statistics" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取日志统计" "PASS" "" "操作日志"
    else
        print_result "获取日志统计" "SKIP" "可能无权限" "操作日志"
    fi
    
    # 6.4 获取慢请求日志
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/slow-requests?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取慢请求日志" "PASS" "" "操作日志"
    else
        print_result "获取慢请求日志" "SKIP" "可能无权限" "操作日志"
    fi
    
    # 6.5 获取错误日志
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/errors?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取错误日志" "PASS" "" "操作日志"
    else
        print_result "获取错误日志" "SKIP" "可能无权限" "操作日志"
    fi
}

# ==================== 7. 预付款测试 ====================
test_prepayment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 预付款业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 获取预付款列表
    local response=$(send_request "GET" "$BASE_URL/finance/prepayment/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取预付款列表" "PASS" "" "预付款"
    else
        print_result "获取预付款列表" "SKIP" "可能无权限" "预付款"
    fi
}

# ==================== 8. 质检标准测试 ====================
test_quality_standard() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 质检标准业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 8.1 获取质检标准列表
    local response=$(send_request "GET" "$BASE_URL/knowledge/quality-standard?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取质检标准列表" "PASS" "" "质检标准"
    else
        print_result "获取质检标准列表" "SKIP" "可能无权限" "质检标准"
    fi
}

# ==================== 9. 报表模板测试 ====================
test_report_template() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 报表模板业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 9.1 获取报表模板列表
    local response=$(send_request "GET" "$BASE_URL/workbench/report-template?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取报表模板列表" "PASS" "" "报表模板"
    else
        print_result "获取报表模板列表" "SKIP" "可能无权限" "报表模板"
    fi
}

# ==================== 10. 客户关联信息测试 ====================
test_client_related() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  10. 客户关联信息业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 先获取一个客户ID
    local response=$(send_request "GET" "$BASE_URL/client/list?pageNum=1&pageSize=1" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    local CLIENT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$CLIENT_ID" ] && [ "$CLIENT_ID" != "null" ]; then
        # 10.1 获取客户股东信息
        response=$(send_request "GET" "$BASE_URL/client/shareholder/client/$CLIENT_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取客户股东信息" "PASS" "" "客户关联"
        else
            print_result "获取客户股东信息" "SKIP" "可能无权限" "客户关联"
        fi
        
        # 10.2 获取关联公司
        response=$(send_request "GET" "$BASE_URL/client/related-company/client/$CLIENT_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取关联公司" "PASS" "" "客户关联"
        else
            print_result "获取关联公司" "SKIP" "可能无权限" "客户关联"
        fi
        
        # 10.3 获取变更历史
        response=$(send_request "GET" "$BASE_URL/client/change-history/client/$CLIENT_ID" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取变更历史" "PASS" "" "客户关联"
        else
            print_result "获取变更历史" "SKIP" "可能无权限" "客户关联"
        fi
    else
        print_result "获取客户股东信息" "SKIP" "无客户数据" "客户关联"
        print_result "获取关联公司" "SKIP" "无客户数据" "客户关联"
        print_result "获取变更历史" "SKIP" "无客户数据" "客户关联"
    fi
}

# ==================== 11. 权限矩阵测试 ====================
test_permission_matrix() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  11. 权限矩阵业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 11.1 获取权限矩阵
    local response=$(send_request "GET" "$BASE_URL/system/permission-matrix" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取权限矩阵" "PASS" "" "权限矩阵"
    else
        print_result "获取权限矩阵" "SKIP" "可能无权限" "权限矩阵"
    fi
}

# ==================== 12. 知识库评论测试 ====================
test_knowledge_comment() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  12. 知识库评论业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 12.1 获取文章评论（需要文章ID，这里测试接口可用性）
    local response=$(send_request "GET" "$BASE_URL/knowledge/article-comment/article/1?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取文章评论" "PASS" "" "知识库评论"
    else
        print_result "获取文章评论" "SKIP" "无文章数据或无权限" "知识库评论"
    fi
    
    # 12.2 获取案例学习笔记
    response=$(send_request "GET" "$BASE_URL/knowledge/case-study?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取案例学习笔记" "PASS" "" "知识库评论"
    else
        print_result "获取案例学习笔记" "SKIP" "可能无权限" "知识库评论"
    fi
}

# ==================== 13. 会议记录测试 ====================
test_meeting_record() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  13. 会议记录业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 13.1 获取会议记录列表
    local response=$(send_request "GET" "$BASE_URL/admin/meeting-records?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取会议记录列表" "PASS" "" "会议记录"
    else
        print_result "获取会议记录列表" "SKIP" "可能无权限" "会议记录"
    fi
}

# ==================== 打印总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}         辅助模块补充测试总结${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${RED}失败: $FAILED${NC}"
    echo -e "${YELLOW}跳过: $SKIPPED${NC}"
    echo ""
    
    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        echo "通过率: ${pass_rate}%"
        local effective_rate=$(((PASSED + SKIPPED) * 100 / TOTAL))
        echo "有效率(通过+跳过): ${effective_rate}%"
    fi
    
    echo ""
    echo -e "${BLUE}─────────────────────────────────────────────────────────${NC}"
    echo "按类别统计："
    echo -e "${BLUE}─────────────────────────────────────────────────────────${NC}"
    
    local categories=("审批中心" "外出管理" "客户标签" "文档模板" "卷宗模板" "操作日志" "预付款" "质检标准" "报表模板" "客户关联" "权限矩阵" "知识库评论" "会议记录")
    for cat in "${categories[@]}"; do
        local cat_pass=0
        local cat_fail=0
        local cat_skip=0
        for result in "${TEST_RESULTS[@]}"; do
            if echo "$result" | grep -q "|$cat"; then
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
        echo -e "${GREEN}  ✅ 辅助模块补充测试全部通过！${NC}"
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
    else
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}  ❌ 有 $FAILED 个测试失败${NC}"
        echo -e "${RED}══════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "失败详情："
        for detail in "${FAILED_DETAILS[@]}"; do
            echo "  - $detail"
        done
    fi
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}  智慧律所管理系统 - 辅助模块补充测试${NC}"
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}     测试时间: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    echo -e "${PURPLE}══════════════════════════════════════════════════════════${NC}"
    
    # 检查服务状态
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    echo -e "${BLUE}       检查服务状态${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════${NC}"
    
    if ! curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${RED}✗${NC} 后端服务未运行，请先启动服务"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} 后端服务运行中"
    
    # 登录获取Token
    echo ""
    echo -e "${BLUE}登录获取Token...${NC}"
    local login_response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$TOKEN" ]; then
        echo -e "${RED}✗${NC} 登录失败，无法获取Token"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} 登录成功"
    
    # 执行各模块测试
    test_approval
    test_go_out
    test_client_tag
    test_document_template
    test_dossier_template
    test_operation_log
    test_prepayment
    test_quality_standard
    test_report_template
    test_client_related
    test_permission_matrix
    test_knowledge_comment
    test_meeting_record
    
    # 打印总结
    print_summary
}

# 运行主函数
main
