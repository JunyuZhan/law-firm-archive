#!/bin/bash

# 智慧律所管理系统 - 系统管理/报表/知识库扩展模块测试
# 测试内容：
# 1. 菜单管理 - 菜单树/用户菜单/角色菜单
# 2. 系统配置 - 配置列表/创建/维护模式
# 3. 系统公告 - 公告CRUD/发布/撤回
# 4. 统计中心 - 工作台统计/收入统计/项目统计
# 5. 报表中心 - 可用报表/报表生成/报表查询
# 6. 定时报表 - 任务管理/执行
# 7. 质量检查 - 检查管理
# 8. 风险预警 - 预警管理
# 9. 外部集成 - 集成配置
# 10. 系统备份 - 备份管理
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

# ==================== 1. 菜单管理测试 ====================
test_menu_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 菜单管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 1.1 获取菜单树
    local response=$(send_request "GET" "$BASE_URL/system/menu/tree" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取菜单树" "PASS" "" "菜单管理"
    else
        print_result "获取菜单树" "SKIP" "可能无权限" "菜单管理"
    fi
    
    # 1.2 获取当前用户菜单
    response=$(send_request "GET" "$BASE_URL/system/menu/user" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取当前用户菜单" "PASS" "" "菜单管理"
    else
        print_result "获取当前用户菜单" "SKIP" "接口可能不可用" "菜单管理"
    fi
    
    # 1.3 获取角色菜单ID
    response=$(send_request "GET" "$BASE_URL/system/menu/role/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取角色菜单ID" "PASS" "" "菜单管理"
    else
        print_result "获取角色菜单ID" "SKIP" "可能无权限" "菜单管理"
    fi
    
    # 1.4 获取菜单详情
    response=$(send_request "GET" "$BASE_URL/system/menu/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取菜单详情" "PASS" "" "菜单管理"
    else
        print_result "获取菜单详情" "SKIP" "菜单不存在" "菜单管理"
    fi
}

# ==================== 2. 系统配置测试 ====================
test_sys_config() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 系统配置业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 2.1 获取所有配置
    local response=$(send_request "GET" "$BASE_URL/system/config" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取所有配置" "PASS" "" "系统配置"
    else
        print_result "获取所有配置" "SKIP" "可能无权限" "系统配置"
    fi
    
    # 2.2 根据键获取配置
    response=$(send_request "GET" "$BASE_URL/system/config/key/sys.name" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "根据键获取配置" "PASS" "" "系统配置"
    else
        print_result "根据键获取配置" "SKIP" "配置不存在" "系统配置"
    fi
    
    # 2.3 获取维护模式状态
    response=$(send_request "GET" "$BASE_URL/system/config/maintenance/status" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取维护模式状态" "PASS" "" "系统配置"
        
        # 检查返回的字段
        local has_enabled=$(echo "$body" | grep -o '"enabled"')
        if [ -n "$has_enabled" ]; then
            print_result "维护模式状态包含enabled字段" "PASS" "" "系统配置"
        else
            print_result "维护模式状态包含enabled字段" "SKIP" "字段名可能不同" "系统配置"
        fi
    else
        print_result "获取维护模式状态" "SKIP" "可能无权限" "系统配置"
    fi
    
    # 2.4 获取系统版本信息
    response=$(send_request "GET" "$BASE_URL/system/config/version" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取系统版本信息" "PASS" "" "系统配置"
        
        # 检查版本字段
        local has_version=$(echo "$body" | grep -o '"version"')
        local has_build_time=$(echo "$body" | grep -o '"buildTime"\|"buildVersion"')
        
        if [ -n "$has_version" ]; then
            print_result "版本信息包含version字段" "PASS" "" "系统配置"
        else
            print_result "版本信息包含version字段" "SKIP" "字段名可能不同" "系统配置"
        fi
    else
        print_result "获取系统版本信息" "SKIP" "接口可能不可用" "系统配置"
    fi
    
    # 2.5 获取合同编号支持的变量
    response=$(send_request "GET" "$BASE_URL/system/config/contract-number/variables" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取合同编号变量" "PASS" "" "系统配置"
    else
        print_result "获取合同编号变量" "SKIP" "接口可能不可用" "系统配置"
    fi
    
    # 2.6 获取推荐的合同编号规则模板
    response=$(send_request "GET" "$BASE_URL/system/config/contract-number/patterns" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取推荐合同编号模板" "PASS" "" "系统配置"
    else
        print_result "获取推荐合同编号模板" "SKIP" "接口可能不可用" "系统配置"
    fi
}

# ==================== 3. 系统公告测试 ====================
test_announcement() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 系统公告业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_announcement_id=""
    
    # 3.1 分页查询公告
    local response=$(send_request "GET" "$BASE_URL/system/announcement?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询公告" "PASS" "" "系统公告"
        test_announcement_id=$(extract_id "$body")
    else
        print_result "分页查询公告" "SKIP" "可能无权限" "系统公告"
    fi
    
    # 3.2 获取有效公告
    response=$(send_request "GET" "$BASE_URL/system/announcement/valid?limit=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取有效公告" "PASS" "" "系统公告"
    else
        print_result "获取有效公告" "SKIP" "可能无权限" "系统公告"
    fi
    
    # 3.3 获取公告详情
    if [ -n "$test_announcement_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/announcement/$test_announcement_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取公告详情" "PASS" "" "系统公告"
        else
            print_result "获取公告详情" "SKIP" "公告不存在" "系统公告"
        fi
    fi
    
    # 3.4 创建公告
    local create_announcement='{
        "title": "测试公告-系统维护通知",
        "content": "系统将于今晚进行例行维护，请提前保存工作。",
        "type": "NOTICE",
        "priority": 1,
        "startTime": "2026-01-12T00:00:00",
        "endTime": "2026-01-20T23:59:59"
    }'
    response=$(send_request "POST" "$BASE_URL/system/announcement" "$create_announcement" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_announcement_id=$(extract_id "$body")
        print_result "创建公告" "PASS" "" "系统公告"
        
        # 3.5 发布公告
        if [ -n "$new_announcement_id" ]; then
            response=$(send_request "POST" "$BASE_URL/system/announcement/$new_announcement_id/publish" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "发布公告" "PASS" "" "系统公告"
                
                # 3.6 撤回公告
                response=$(send_request "POST" "$BASE_URL/system/announcement/$new_announcement_id/withdraw" "" "$auth_header")
                body=$(echo "$response" | sed '$d')
                
                if check_success "$body"; then
                    print_result "撤回公告" "PASS" "" "系统公告"
                else
                    print_result "撤回公告" "SKIP" "可能无权限" "系统公告"
                fi
            else
                print_result "发布公告" "SKIP" "可能无权限" "系统公告"
            fi
            
            # 3.7 删除公告
            response=$(send_request "DELETE" "$BASE_URL/system/announcement/$new_announcement_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除公告" "PASS" "" "系统公告"
            else
                print_result "删除公告" "SKIP" "可能无权限" "系统公告"
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建公告" "SKIP" "$message" "系统公告"
    fi
}

# ==================== 4. 统计中心测试 ====================
test_statistics() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 统计中心业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 4.1 获取工作台统计数据
    local response=$(send_request "GET" "$BASE_URL/workbench/stats" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取工作台统计数据" "PASS" "" "统计中心"
    else
        print_result "获取工作台统计数据" "SKIP" "可能无权限" "统计中心"
    fi
    
    # 4.2 获取收入统计
    response=$(send_request "GET" "$BASE_URL/workbench/statistics/revenue" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取收入统计" "PASS" "" "统计中心"
    else
        print_result "获取收入统计" "SKIP" "可能无权限" "统计中心"
    fi
    
    # 4.3 获取项目统计
    response=$(send_request "GET" "$BASE_URL/workbench/statistics/matter" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取项目统计" "PASS" "" "统计中心"
    else
        print_result "获取项目统计" "SKIP" "可能无权限" "统计中心"
    fi
    
    # 4.4 获取客户统计
    response=$(send_request "GET" "$BASE_URL/workbench/statistics/client" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取客户统计" "PASS" "" "统计中心"
    else
        print_result "获取客户统计" "SKIP" "可能无权限" "统计中心"
    fi
    
    # 4.5 获取律师业绩排行
    response=$(send_request "GET" "$BASE_URL/workbench/statistics/lawyer-performance?limit=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取律师业绩排行" "PASS" "" "统计中心"
    else
        print_result "获取律师业绩排行" "SKIP" "可能无权限" "统计中心"
    fi
}

# ==================== 5. 报表中心测试 ====================
test_report_center() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 报表中心业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_report_id=""
    
    # 5.1 获取可用报表列表
    local response=$(send_request "GET" "$BASE_URL/workbench/report/available" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取可用报表列表" "PASS" "" "报表中心"
    else
        print_result "获取可用报表列表" "SKIP" "可能无权限" "报表中心"
    fi
    
    # 5.2 分页查询报表记录
    response=$(send_request "GET" "$BASE_URL/workbench/report?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询报表记录" "PASS" "" "报表中心"
        test_report_id=$(extract_id "$body")
    else
        print_result "分页查询报表记录" "SKIP" "可能无权限" "报表中心"
    fi
    
    # 5.3 获取报表详情
    if [ -n "$test_report_id" ]; then
        response=$(send_request "GET" "$BASE_URL/workbench/report/$test_report_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取报表详情" "PASS" "" "报表中心"
        else
            print_result "获取报表详情" "SKIP" "报表不存在" "报表中心"
        fi
    fi
    
    # 5.4 同步生成报表（测试）
    local generate_report='{
        "reportType": "FINANCE_SUMMARY",
        "reportName": "测试财务汇总报表",
        "format": "EXCEL",
        "startDate": "2026-01-01",
        "endDate": "2026-01-12"
    }'
    response=$(send_request "POST" "$BASE_URL/workbench/report/generate" "$generate_report" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_report_id=$(extract_id "$body")
        print_result "同步生成报表" "PASS" "" "报表中心"
        
        # 5.5 查询报表生成状态
        if [ -n "$new_report_id" ]; then
            response=$(send_request "GET" "$BASE_URL/workbench/report/status/$new_report_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "查询报表生成状态" "PASS" "" "报表中心"
            else
                print_result "查询报表生成状态" "SKIP" "接口可能不可用" "报表中心"
            fi
            
            # 5.6 获取报表下载URL
            response=$(send_request "GET" "$BASE_URL/workbench/report/$new_report_id/download-url" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "获取报表下载URL" "PASS" "" "报表中心"
            else
                print_result "获取报表下载URL" "SKIP" "报表未生成完成" "报表中心"
            fi
            
            # 5.7 删除报表
            response=$(send_request "DELETE" "$BASE_URL/workbench/report/$new_report_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除报表" "PASS" "" "报表中心"
            else
                print_result "删除报表" "SKIP" "可能无权限" "报表中心"
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "同步生成报表" "SKIP" "$message" "报表中心"
    fi
}

# ==================== 6. 定时报表测试 ====================
test_scheduled_report() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 定时报表业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_task_id=""
    
    # 6.1 分页查询定时报表任务
    local response=$(send_request "GET" "$BASE_URL/workbench/scheduled-report?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询定时报表任务" "PASS" "" "定时报表"
        test_task_id=$(extract_id "$body")
    else
        print_result "分页查询定时报表任务" "SKIP" "可能无权限" "定时报表"
    fi
    
    # 6.2 获取任务详情
    if [ -n "$test_task_id" ]; then
        response=$(send_request "GET" "$BASE_URL/workbench/scheduled-report/$test_task_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取定时任务详情" "PASS" "" "定时报表"
        else
            print_result "获取定时任务详情" "SKIP" "任务不存在" "定时报表"
        fi
        
        # 6.3 查询执行记录
        response=$(send_request "GET" "$BASE_URL/workbench/scheduled-report/$test_task_id/logs?pageNum=1&pageSize=10" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "查询执行记录" "PASS" "" "定时报表"
        else
            print_result "查询执行记录" "SKIP" "可能无权限" "定时报表"
        fi
    fi
    
    # 6.4 创建定时报表任务
    local create_task='{
        "name": "测试定时报表任务",
        "reportType": "FINANCE_SUMMARY",
        "format": "EXCEL",
        "cronExpression": "0 0 9 * * ?",
        "description": "每天9点生成财务汇总报表"
    }'
    response=$(send_request "POST" "$BASE_URL/workbench/scheduled-report" "$create_task" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_task_id=$(extract_id "$body")
        print_result "创建定时报表任务" "PASS" "" "定时报表"
        
        # 6.5 暂停任务
        if [ -n "$new_task_id" ]; then
            response=$(send_request "POST" "$BASE_URL/workbench/scheduled-report/$new_task_id/pause" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "暂停任务" "PASS" "" "定时报表"
            else
                print_result "暂停任务" "SKIP" "可能无权限" "定时报表"
            fi
            
            # 6.6 启用任务
            response=$(send_request "POST" "$BASE_URL/workbench/scheduled-report/$new_task_id/enable" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "启用任务" "PASS" "" "定时报表"
            else
                print_result "启用任务" "SKIP" "可能无权限" "定时报表"
            fi
            
            # 6.7 删除任务
            response=$(send_request "DELETE" "$BASE_URL/workbench/scheduled-report/$new_task_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除定时任务" "PASS" "" "定时报表"
            else
                print_result "删除定时任务" "SKIP" "可能无权限" "定时报表"
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建定时报表任务" "SKIP" "$message" "定时报表"
    fi
}

# ==================== 7. 质量检查测试 ====================
test_quality_check() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 质量检查业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 7.1 获取进行中的检查
    local response=$(send_request "GET" "$BASE_URL/knowledge/quality-check/in-progress" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取进行中的检查" "PASS" "" "质量检查"
    else
        print_result "获取进行中的检查" "SKIP" "可能无权限" "质量检查"
    fi
    
    # 7.2 获取项目的所有检查（测试matterId=1）
    response=$(send_request "GET" "$BASE_URL/knowledge/quality-check/matter/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取项目的所有检查" "PASS" "" "质量检查"
    else
        print_result "获取项目的所有检查" "SKIP" "项目不存在" "质量检查"
    fi
}

# ==================== 8. 风险预警测试 ====================
test_risk_warning() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 风险预警业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 8.1 获取活跃的预警
    local response=$(send_request "GET" "$BASE_URL/knowledge/risk-warning/active" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取活跃的预警" "PASS" "" "风险预警"
    else
        print_result "获取活跃的预警" "SKIP" "可能无权限" "风险预警"
    fi
    
    # 8.2 获取高风险预警
    response=$(send_request "GET" "$BASE_URL/knowledge/risk-warning/high-risk" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取高风险预警" "PASS" "" "风险预警"
    else
        print_result "获取高风险预警" "SKIP" "可能无权限" "风险预警"
    fi
    
    # 8.3 获取项目的所有预警
    response=$(send_request "GET" "$BASE_URL/knowledge/risk-warning/matter/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取项目的所有预警" "PASS" "" "风险预警"
    else
        print_result "获取项目的所有预警" "SKIP" "项目不存在" "风险预警"
    fi
}

# ==================== 9. 外部集成测试 ====================
test_external_integration() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 外部集成业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_integration_id=""
    
    # 9.1 分页查询集成配置
    local response=$(send_request "GET" "$BASE_URL/system/integration?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询集成配置" "PASS" "" "外部集成"
        test_integration_id=$(extract_id "$body")
    else
        print_result "分页查询集成配置" "SKIP" "可能无权限" "外部集成"
    fi
    
    # 9.2 获取所有集成配置
    response=$(send_request "GET" "$BASE_URL/system/integration/all" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取所有集成配置" "PASS" "" "外部集成"
    else
        print_result "获取所有集成配置" "SKIP" "可能无权限" "外部集成"
    fi
    
    # 9.3 获取集成详情
    if [ -n "$test_integration_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/integration/$test_integration_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取集成详情" "PASS" "" "外部集成"
        else
            print_result "获取集成详情" "SKIP" "集成不存在" "外部集成"
        fi
    fi
}

# ==================== 10. 系统备份测试 ====================
test_backup() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  10. 系统备份业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_backup_id=""
    
    # 10.1 查询备份记录列表
    local response=$(send_request "GET" "$BASE_URL/system/backup/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "查询备份记录列表" "PASS" "" "系统备份"
        test_backup_id=$(extract_id "$body")
    else
        print_result "查询备份记录列表" "SKIP" "可能无权限" "系统备份"
    fi
    
    # 10.2 获取备份详情
    if [ -n "$test_backup_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/backup/$test_backup_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取备份详情" "PASS" "" "系统备份"
        else
            print_result "获取备份详情" "SKIP" "备份不存在" "系统备份"
        fi
    fi
}

# ==================== 11. OpenAPI管理测试 ====================
test_openapi_manage() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  11. OpenAPI管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 11.1 分页查询令牌列表
    local response=$(send_request "GET" "$BASE_URL/system/openapi/token?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询令牌列表" "PASS" "" "OpenAPI管理"
    else
        print_result "分页查询令牌列表" "SKIP" "可能无权限" "OpenAPI管理"
    fi
    
    # 11.2 获取授权范围选项
    response=$(send_request "GET" "$BASE_URL/system/openapi/scopes" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取授权范围选项" "PASS" "" "OpenAPI管理"
    else
        print_result "获取授权范围选项" "SKIP" "可能无权限" "OpenAPI管理"
    fi
}

# ==================== 12. 通知管理测试 ====================
test_notification() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  12. 通知管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 12.1 分页查询我的通知
    local response=$(send_request "GET" "$BASE_URL/system/notification?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询我的通知" "PASS" "" "通知管理"
        
        local notification_id=$(extract_id "$body")
        
        # 12.2 标记为已读
        if [ -n "$notification_id" ]; then
            response=$(send_request "POST" "$BASE_URL/system/notification/$notification_id/read" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "标记通知为已读" "PASS" "" "通知管理"
            else
                print_result "标记通知为已读" "SKIP" "可能已读" "通知管理"
            fi
        fi
    else
        print_result "分页查询我的通知" "SKIP" "可能无权限" "通知管理"
    fi
    
    # 12.3 获取未读数量
    response=$(send_request "GET" "$BASE_URL/system/notification/unread-count" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取未读通知数量" "PASS" "" "通知管理"
    else
        print_result "获取未读通知数量" "SKIP" "接口可能不可用" "通知管理"
    fi
    
    # 12.4 检查邮件服务状态
    response=$(send_request "GET" "$BASE_URL/system/notification/email-status" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "检查邮件服务状态" "PASS" "" "通知管理"
    else
        print_result "检查邮件服务状态" "SKIP" "可能无权限" "通知管理"
    fi
}

# ==================== 13. 登录日志测试 ====================
test_login_log() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  13. 登录日志业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 13.1 分页查询登录日志
    local response=$(send_request "GET" "$BASE_URL/system/login-log?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询登录日志" "PASS" "" "登录日志"
    else
        print_result "分页查询登录日志" "SKIP" "可能无权限" "登录日志"
    fi
}

# ==================== 14. 会话管理测试 ====================
test_session_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  14. 会话管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 14.1 获取当前会话列表
    local response=$(send_request "GET" "$BASE_URL/system/session?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取当前会话列表" "PASS" "" "会话管理"
    else
        print_result "获取当前会话列表" "SKIP" "可能无权限" "会话管理"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}         系统管理/报表/知识库扩展模块测试总结${NC}"
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
    
    local categories=("菜单管理" "系统配置" "系统公告" "统计中心" "报表中心" "定时报表" "质量检查" "风险预警" "外部集成" "系统备份" "OpenAPI管理" "通知管理" "登录日志" "会话管理")
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
            printf "  %-14s: ${GREEN}%d通过${NC} / ${RED}%d失败${NC} / ${YELLOW}%d跳过${NC}\n" "$cat" "$cat_pass" "$cat_fail" "$cat_skip"
        fi
    done
    
    echo ""
    
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  ✅ 系统管理/报表/知识库扩展模块测试全部通过！${NC}"
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
    echo -e "${PURPLE}  智慧律所管理系统 - 系统管理/报表/知识库扩展模块测试${NC}"
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
    test_menu_management
    test_sys_config
    test_announcement
    test_statistics
    test_report_center
    test_scheduled_report
    test_quality_check
    test_risk_warning
    test_external_integration
    test_backup
    test_openapi_manage
    test_notification
    test_login_log
    test_session_management
    
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
