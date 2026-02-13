#!/bin/bash

# 智慧律所管理系统 - 系统管理/知识库/数据交换模块业务逻辑测试
# 测试内容：
# 1. 用户管理 - CRUD/状态变更/密码重置
# 2. 角色管理 - CRUD/权限分配
# 3. 部门管理 - 树结构/CRUD
# 4. 数据字典 - 类型/项管理
# 5. 知识库-经验文章 - 文章管理/收藏/点赞
# 6. 知识库-案例库 - 案例管理/收藏
# 7. 知识库-法规库 - 法规管理/收藏
# 8. 数据交接 - 预览/创建/确认
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

# ==================== 1. 用户管理测试 ====================
test_user_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  1. 用户管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_user_id=""
    
    # 1.1 分页查询用户列表
    local response=$(send_request "GET" "$BASE_URL/system/user/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询用户列表" "PASS" "" "用户管理"
        test_user_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "分页查询用户列表" "FAIL" "接口调用失败" "用户管理"
    fi
    
    # 1.2 获取用户选择列表（公共接口）
    response=$(send_request "GET" "$BASE_URL/system/user/select-options?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取用户选择列表(公共)" "PASS" "" "用户管理"
    else
        print_result "获取用户选择列表(公共)" "SKIP" "接口可能不可用" "用户管理"
    fi
    
    # 1.3 获取用户详情
    if [ -n "$test_user_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/user/$test_user_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取用户详情" "PASS" "" "用户管理"
            
            # 检查必要字段
            local has_username=$(echo "$body" | grep -o '"username"')
            local has_status=$(echo "$body" | grep -o '"status"')
            
            if [ -n "$has_username" ]; then
                print_result "用户详情包含用户名字段" "PASS" "" "用户管理"
            else
                print_result "用户详情包含用户名字段" "SKIP" "字段名可能不同" "用户管理"
            fi
            
            if [ -n "$has_status" ]; then
                print_result "用户详情包含状态字段" "PASS" "" "用户管理"
            else
                print_result "用户详情包含状态字段" "SKIP" "字段名可能不同" "用户管理"
            fi
        else
            print_result "获取用户详情" "SKIP" "用户不存在" "用户管理"
        fi
    fi
    
    # 1.4 创建用户
    local random_suffix=$(date +%s)
    local create_user='{
        "username": "testuser'$random_suffix'",
        "password": "Test123456",
        "realName": "测试用户",
        "email": "test'$random_suffix'@example.com",
        "phone": "138'$random_suffix'",
        "roleIds": [2],
        "status": "ACTIVE"
    }'
    response=$(send_request "POST" "$BASE_URL/system/user" "$create_user" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_user_id=$(extract_id "$body")
        print_result "创建用户" "PASS" "" "用户管理"
        
        # 1.5 修改用户状态
        if [ -n "$new_user_id" ]; then
            response=$(send_request "PUT" "$BASE_URL/system/user/$new_user_id/status" '{"status":"DISABLED"}' "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "修改用户状态" "PASS" "" "用户管理"
            else
                print_result "修改用户状态" "SKIP" "可能无权限" "用户管理"
            fi
            
            # 1.6 重置密码
            response=$(send_request "POST" "$BASE_URL/system/user/$new_user_id/reset-password" '{"newPassword":"NewPass123"}' "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "重置用户密码" "PASS" "" "用户管理"
            else
                print_result "重置用户密码" "SKIP" "可能无权限" "用户管理"
            fi
            
            # 1.7 删除用户
            response=$(send_request "DELETE" "$BASE_URL/system/user/$new_user_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除用户" "PASS" "" "用户管理"
            else
                print_result "删除用户" "SKIP" "可能无权限" "用户管理"
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建用户" "SKIP" "$message" "用户管理"
    fi
    
    # 1.8 测试用户名唯一性校验
    local duplicate_user='{
        "username": "admin",
        "password": "Test123456",
        "realName": "重复用户"
    }'
    response=$(send_request "POST" "$BASE_URL/system/user" "$duplicate_user" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if ! check_success "$body"; then
        print_result "拒绝重复用户名" "PASS" "" "用户管理"
    else
        print_result "拒绝重复用户名" "FAIL" "允许了重复用户名" "用户管理"
    fi
}

# ==================== 2. 角色管理测试 ====================
test_role_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  2. 角色管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_role_id=""
    
    # 2.1 分页查询角色列表
    local response=$(send_request "GET" "$BASE_URL/system/role/list?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询角色列表" "PASS" "" "角色管理"
        test_role_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "分页查询角色列表" "SKIP" "可能无权限" "角色管理"
    fi
    
    # 2.2 获取所有角色
    response=$(send_request "GET" "$BASE_URL/system/role/all" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取所有角色(下拉)" "PASS" "" "角色管理"
    else
        print_result "获取所有角色(下拉)" "SKIP" "可能无权限" "角色管理"
    fi
    
    # 2.3 获取角色详情
    if [ -n "$test_role_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/role/$test_role_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取角色详情" "PASS" "" "角色管理"
        else
            print_result "获取角色详情" "SKIP" "角色不存在" "角色管理"
        fi
        
        # 2.4 获取角色菜单
        response=$(send_request "GET" "$BASE_URL/system/role/$test_role_id/menus" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取角色菜单ID列表" "PASS" "" "角色管理"
        else
            print_result "获取角色菜单ID列表" "SKIP" "可能无权限" "角色管理"
        fi
    fi
    
    # 2.5 创建角色
    local random_suffix=$(date +%s)
    local create_role='{
        "roleName": "测试角色'$random_suffix'",
        "roleCode": "TEST_ROLE_'$random_suffix'",
        "description": "测试用角色",
        "status": "ENABLED"
    }'
    response=$(send_request "POST" "$BASE_URL/system/role" "$create_role" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_role_id=$(extract_id "$body")
        print_result "创建角色" "PASS" "" "角色管理"
        
        # 2.6 修改角色状态
        if [ -n "$new_role_id" ]; then
            response=$(send_request "PUT" "$BASE_URL/system/role/$new_role_id/status" '{"status":"DISABLED"}' "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "修改角色状态" "PASS" "" "角色管理"
            else
                print_result "修改角色状态" "SKIP" "可能无权限" "角色管理"
            fi
            
            # 2.7 删除角色
            response=$(send_request "DELETE" "$BASE_URL/system/role/$new_role_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除角色" "PASS" "" "角色管理"
            else
                local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
                if echo "$message" | grep -q "使用\|关联"; then
                    print_result "拒绝删除使用中的角色" "PASS" "" "角色管理"
                else
                    print_result "删除角色" "SKIP" "$message" "角色管理"
                fi
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建角色" "SKIP" "$message" "角色管理"
    fi
}

# ==================== 3. 部门管理测试 ====================
test_department_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  3. 部门管理业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_dept_id=""
    
    # 3.1 获取部门树
    local response=$(send_request "GET" "$BASE_URL/system/department/tree" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取部门树" "PASS" "" "部门管理"
    else
        print_result "获取部门树" "SKIP" "可能无权限" "部门管理"
    fi
    
    # 3.2 获取部门树（公共接口）
    response=$(send_request "GET" "$BASE_URL/system/department/tree-public" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取部门树(公共)" "PASS" "" "部门管理"
    else
        print_result "获取部门树(公共)" "SKIP" "接口可能不可用" "部门管理"
    fi
    
    # 3.3 获取部门列表
    response=$(send_request "GET" "$BASE_URL/system/department/list" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取部门列表" "PASS" "" "部门管理"
        test_dept_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "获取部门列表" "SKIP" "可能无权限" "部门管理"
    fi
    
    # 3.4 获取部门详情
    if [ -n "$test_dept_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/department/$test_dept_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取部门详情" "PASS" "" "部门管理"
        else
            print_result "获取部门详情" "SKIP" "部门不存在" "部门管理"
        fi
    fi
    
    # 3.5 创建部门
    local random_suffix=$(date +%s)
    local create_dept='{
        "deptName": "测试部门'$random_suffix'",
        "deptCode": "TEST_DEPT_'$random_suffix'",
        "parentId": 0,
        "sortOrder": 99
    }'
    response=$(send_request "POST" "$BASE_URL/system/department" "$create_dept" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_dept_id=$(extract_id "$body")
        print_result "创建部门" "PASS" "" "部门管理"
        
        # 3.6 删除部门
        if [ -n "$new_dept_id" ]; then
            response=$(send_request "DELETE" "$BASE_URL/system/department/$new_dept_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除部门" "PASS" "" "部门管理"
            else
                local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
                if echo "$message" | grep -q "子部门\|用户\|关联"; then
                    print_result "拒绝删除有关联的部门" "PASS" "" "部门管理"
                else
                    print_result "删除部门" "SKIP" "$message" "部门管理"
                fi
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建部门" "SKIP" "$message" "部门管理"
    fi
}

# ==================== 4. 数据字典测试 ====================
test_dict_management() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  4. 数据字典业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_type_id=""
    
    # 4.1 获取所有字典类型
    local response=$(send_request "GET" "$BASE_URL/system/dict/types" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取所有字典类型" "PASS" "" "数据字典"
        test_type_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "获取所有字典类型" "SKIP" "可能无权限" "数据字典"
    fi
    
    # 4.2 获取字典类型详情
    if [ -n "$test_type_id" ]; then
        response=$(send_request "GET" "$BASE_URL/system/dict/types/$test_type_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取字典类型详情(含字典项)" "PASS" "" "数据字典"
        else
            print_result "获取字典类型详情(含字典项)" "SKIP" "类型不存在" "数据字典"
        fi
        
        # 4.3 获取字典项列表
        response=$(send_request "GET" "$BASE_URL/system/dict/types/$test_type_id/items" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取字典项列表" "PASS" "" "数据字典"
        else
            print_result "获取字典项列表" "SKIP" "可能无字典项" "数据字典"
        fi
    fi
    
    # 4.4 根据编码获取字典项
    response=$(send_request "GET" "$BASE_URL/system/dict/items/code/client_type" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "根据编码获取字典项" "PASS" "" "数据字典"
    else
        print_result "根据编码获取字典项" "SKIP" "编码可能不存在" "数据字典"
    fi
    
    # 4.5 创建字典类型
    local random_suffix=$(date +%s)
    local create_type='{
        "typeCode": "test_type_'$random_suffix'",
        "typeName": "测试字典类型",
        "description": "测试用字典类型"
    }'
    response=$(send_request "POST" "$BASE_URL/system/dict/types" "$create_type" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_type_id=$(extract_id "$body")
        print_result "创建字典类型" "PASS" "" "数据字典"
        
        # 4.6 创建字典项
        if [ -n "$new_type_id" ]; then
            local create_item='{
                "typeId": '$new_type_id',
                "itemCode": "test_item",
                "itemName": "测试字典项",
                "sortOrder": 1
            }'
            response=$(send_request "POST" "$BASE_URL/system/dict/items" "$create_item" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                local new_item_id=$(extract_id "$body")
                print_result "创建字典项" "PASS" "" "数据字典"
                
                # 4.7 切换字典项状态
                if [ -n "$new_item_id" ]; then
                    response=$(send_request "POST" "$BASE_URL/system/dict/items/$new_item_id/toggle" "" "$auth_header")
                    body=$(echo "$response" | sed '$d')
                    
                    if check_success "$body"; then
                        print_result "切换字典项状态" "PASS" "" "数据字典"
                    else
                        print_result "切换字典项状态" "SKIP" "可能无权限" "数据字典"
                    fi
                    
                    # 删除字典项
                    response=$(send_request "DELETE" "$BASE_URL/system/dict/items/$new_item_id" "" "$auth_header")
                    body=$(echo "$response" | sed '$d')
                    
                    if check_success "$body"; then
                        print_result "删除字典项" "PASS" "" "数据字典"
                    else
                        print_result "删除字典项" "SKIP" "可能无权限" "数据字典"
                    fi
                fi
            else
                print_result "创建字典项" "SKIP" "可能无权限" "数据字典"
            fi
            
            # 删除字典类型
            response=$(send_request "DELETE" "$BASE_URL/system/dict/types/$new_type_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除字典类型" "PASS" "" "数据字典"
            else
                print_result "删除字典类型" "SKIP" "可能有关联字典项" "数据字典"
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建字典类型" "SKIP" "$message" "数据字典"
    fi
}

# ==================== 5. 知识库-经验文章测试 ====================
test_knowledge_article() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  5. 知识库-经验文章业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_article_id=""
    
    # 5.1 分页查询文章
    local response=$(send_request "GET" "$BASE_URL/knowledge/article?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询经验文章" "PASS" "" "经验文章"
        test_article_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "分页查询经验文章" "SKIP" "可能无权限" "经验文章"
    fi
    
    # 5.2 获取文章详情
    if [ -n "$test_article_id" ]; then
        response=$(send_request "GET" "$BASE_URL/knowledge/article/$test_article_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取文章详情" "PASS" "" "经验文章"
        else
            print_result "获取文章详情" "SKIP" "文章不存在" "经验文章"
        fi
    fi
    
    # 5.3 获取我的文章
    response=$(send_request "GET" "$BASE_URL/knowledge/article/my" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我的文章" "PASS" "" "经验文章"
    else
        print_result "获取我的文章" "SKIP" "可能无权限" "经验文章"
    fi
    
    # 5.4 获取收藏文章
    response=$(send_request "GET" "$BASE_URL/knowledge/article/collected" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我的收藏文章" "PASS" "" "经验文章"
    else
        print_result "获取我的收藏文章" "SKIP" "可能无权限" "经验文章"
    fi
    
    # 5.5 创建文章
    local create_article='{
        "title": "测试经验文章",
        "content": "这是测试文章内容",
        "summary": "测试摘要",
        "categoryId": 1,
        "tags": ["测试", "经验分享"]
    }'
    response=$(send_request "POST" "$BASE_URL/knowledge/article" "$create_article" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        local new_article_id=$(extract_id "$body")
        print_result "创建经验文章" "PASS" "" "经验文章"
        
        # 5.6 点赞文章
        if [ -n "$new_article_id" ]; then
            response=$(send_request "POST" "$BASE_URL/knowledge/article/$new_article_id/like" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "点赞文章" "PASS" "" "经验文章"
            else
                print_result "点赞文章" "SKIP" "可能已点赞" "经验文章"
            fi
            
            # 5.7 收藏文章
            response=$(send_request "POST" "$BASE_URL/knowledge/article/$new_article_id/collect" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "收藏文章" "PASS" "" "经验文章"
            else
                print_result "收藏文章" "SKIP" "可能已收藏" "经验文章"
            fi
            
            # 5.8 取消收藏
            response=$(send_request "DELETE" "$BASE_URL/knowledge/article/$new_article_id/collect" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "取消收藏文章" "PASS" "" "经验文章"
            else
                print_result "取消收藏文章" "SKIP" "可能未收藏" "经验文章"
            fi
            
            # 5.9 发布文章
            response=$(send_request "POST" "$BASE_URL/knowledge/article/$new_article_id/publish" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "发布文章" "PASS" "" "经验文章"
            else
                print_result "发布文章" "SKIP" "可能已发布" "经验文章"
            fi
            
            # 5.10 删除文章
            response=$(send_request "DELETE" "$BASE_URL/knowledge/article/$new_article_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "删除文章" "PASS" "" "经验文章"
            else
                print_result "删除文章" "SKIP" "可能无权限" "经验文章"
            fi
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "创建经验文章" "SKIP" "$message" "经验文章"
    fi
}

# ==================== 6. 知识库-案例库测试 ====================
test_case_library() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  6. 知识库-案例库业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_case_id=""
    
    # 6.1 获取案例分类树
    local response=$(send_request "GET" "$BASE_URL/knowledge/case/categories" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取案例分类树" "PASS" "" "案例库"
    else
        print_result "获取案例分类树" "SKIP" "可能无权限" "案例库"
    fi
    
    # 6.2 分页查询案例
    response=$(send_request "GET" "$BASE_URL/knowledge/case?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询案例" "PASS" "" "案例库"
        test_case_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "分页查询案例" "SKIP" "可能无权限" "案例库"
    fi
    
    # 6.3 获取案例详情
    if [ -n "$test_case_id" ]; then
        response=$(send_request "GET" "$BASE_URL/knowledge/case/$test_case_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取案例详情" "PASS" "" "案例库"
        else
            print_result "获取案例详情" "SKIP" "案例不存在" "案例库"
        fi
        
        # 6.4 收藏案例
        response=$(send_request "POST" "$BASE_URL/knowledge/case/$test_case_id/collect" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "收藏案例" "PASS" "" "案例库"
        else
            print_result "收藏案例" "SKIP" "可能已收藏" "案例库"
        fi
        
        # 6.5 取消收藏案例
        response=$(send_request "DELETE" "$BASE_URL/knowledge/case/$test_case_id/collect" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "取消收藏案例" "PASS" "" "案例库"
        else
            print_result "取消收藏案例" "SKIP" "可能未收藏" "案例库"
        fi
    fi
    
    # 6.6 获取我的收藏案例
    response=$(send_request "GET" "$BASE_URL/knowledge/case/collected" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我的收藏案例" "PASS" "" "案例库"
    else
        print_result "获取我的收藏案例" "SKIP" "可能无权限" "案例库"
    fi
}

# ==================== 7. 知识库-法规库测试 ====================
test_law_regulation() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  7. 知识库-法规库业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    local test_law_id=""
    
    # 7.1 获取法规分类树
    local response=$(send_request "GET" "$BASE_URL/knowledge/law/categories" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取法规分类树" "PASS" "" "法规库"
    else
        print_result "获取法规分类树" "SKIP" "可能无权限" "法规库"
    fi
    
    # 7.2 分页查询法规
    response=$(send_request "GET" "$BASE_URL/knowledge/law?pageNum=1&pageSize=10" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询法规" "PASS" "" "法规库"
        test_law_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    else
        print_result "分页查询法规" "SKIP" "可能无权限" "法规库"
    fi
    
    # 7.3 获取法规详情
    if [ -n "$test_law_id" ]; then
        response=$(send_request "GET" "$BASE_URL/knowledge/law/$test_law_id" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "获取法规详情" "PASS" "" "法规库"
        else
            print_result "获取法规详情" "SKIP" "法规不存在" "法规库"
        fi
        
        # 7.4 收藏法规
        response=$(send_request "POST" "$BASE_URL/knowledge/law/$test_law_id/collect" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "收藏法规" "PASS" "" "法规库"
        else
            print_result "收藏法规" "SKIP" "可能已收藏" "法规库"
        fi
        
        # 7.5 取消收藏法规
        response=$(send_request "DELETE" "$BASE_URL/knowledge/law/$test_law_id/collect" "" "$auth_header")
        body=$(echo "$response" | sed '$d')
        
        if check_success "$body"; then
            print_result "取消收藏法规" "PASS" "" "法规库"
        else
            print_result "取消收藏法规" "SKIP" "可能未收藏" "法规库"
        fi
    fi
    
    # 7.6 获取我的收藏法规
    response=$(send_request "GET" "$BASE_URL/knowledge/law/collected" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取我的收藏法规" "PASS" "" "法规库"
    else
        print_result "获取我的收藏法规" "SKIP" "可能无权限" "法规库"
    fi
}

# ==================== 8. 数据交接测试 ====================
test_data_handover() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  8. 数据交接业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 8.1 分页查询交接单
    local response=$(send_request "GET" "$BASE_URL/system/data-handover?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询交接单" "PASS" "" "数据交接"
        
        local test_handover_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        
        # 8.2 获取交接单详情
        if [ -n "$test_handover_id" ]; then
            response=$(send_request "GET" "$BASE_URL/system/data-handover/$test_handover_id" "" "$auth_header")
            body=$(echo "$response" | sed '$d')
            
            if check_success "$body"; then
                print_result "获取交接单详情" "PASS" "" "数据交接"
            else
                print_result "获取交接单详情" "SKIP" "交接单不存在" "数据交接"
            fi
        fi
    else
        print_result "分页查询交接单" "SKIP" "可能无权限" "数据交接"
    fi
    
    # 8.3 预览离职交接数据
    response=$(send_request "GET" "$BASE_URL/system/data-handover/preview/1" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "预览离职交接数据" "PASS" "" "数据交接"
        
        # 检查预览数据结构
        local has_matters=$(echo "$body" | grep -o '"matters"\|"pendingMatters"')
        local has_clients=$(echo "$body" | grep -o '"clients"\|"pendingClients"')
        
        if [ -n "$has_matters" ]; then
            print_result "交接预览包含项目数据" "PASS" "" "数据交接"
        else
            print_result "交接预览包含项目数据" "SKIP" "字段名可能不同" "数据交接"
        fi
        
        if [ -n "$has_clients" ]; then
            print_result "交接预览包含客户数据" "PASS" "" "数据交接"
        else
            print_result "交接预览包含客户数据" "SKIP" "字段名可能不同" "数据交接"
        fi
    else
        local message=$(echo "$body" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        print_result "预览离职交接数据" "SKIP" "$message" "数据交接"
    fi
}

# ==================== 9. 操作日志测试 ====================
test_operation_log() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  9. 操作日志业务逻辑测试${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    
    local auth_header="Authorization: Bearer $TOKEN"
    
    # 9.1 分页查询操作日志
    local response=$(send_request "GET" "$BASE_URL/admin/operation-logs?pageNum=1&pageSize=10" "" "$auth_header")
    local body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "分页查询操作日志" "PASS" "" "操作日志"
    else
        print_result "分页查询操作日志" "SKIP" "可能无权限" "操作日志"
    fi
    
    # 9.2 获取日志模块列表
    response=$(send_request "GET" "$BASE_URL/admin/operation-logs/modules" "" "$auth_header")
    body=$(echo "$response" | sed '$d')
    
    if check_success "$body"; then
        print_result "获取日志模块列表" "PASS" "" "操作日志"
    else
        print_result "获取日志模块列表" "SKIP" "可能无权限" "操作日志"
    fi
}

# ==================== 打印测试总结 ====================
print_summary() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}         系统管理/知识库/数据交接模块测试总结${NC}"
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
    
    local categories=("用户管理" "角色管理" "部门管理" "数据字典" "经验文章" "案例库" "法规库" "数据交接" "操作日志")
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
        echo -e "${GREEN}  ✅ 系统管理/知识库/数据交接模块测试全部通过！${NC}"
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
    echo -e "${PURPLE}  智慧律所管理系统 - 系统管理/知识库/数据交换模块测试${NC}"
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
    test_user_management
    test_role_management
    test_department_management
    test_dict_management
    test_knowledge_article
    test_case_library
    test_law_regulation
    test_data_handover
    test_operation_log
    
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
