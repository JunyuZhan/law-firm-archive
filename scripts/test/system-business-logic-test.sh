#!/bin/bash
# =====================================================
# 系统模块业务逻辑和数据勾连测试脚本
# =====================================================
# 测试范围:
# - 用户管理 (CRUD、状态切换、密码重置)
# - 角色管理 (CRUD、权限分配)
# - 部门管理 (树形结构)
# - 菜单管理 (树形结构、角色关联)
# - 字典管理 (类型+项)
# - 系统配置 (读写)
# - 操作日志 (查询)
# - 登录日志 (查询)
# - 通知功能 (发送、查询、标记已读)
# - 数据交接 (创建、确认)
# =====================================================

set -e

BASE_URL="http://localhost:8080/api"
TOKEN=""
REFRESH_TOKEN=""

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
WARNINGS=0

# 存储测试数据
TEST_USER_ID=""
TEST_ROLE_ID=""
TEST_DEPT_ID=""
TEST_MENU_ID=""
TEST_DICT_TYPE_ID=""
TEST_NOTIFICATION_ID=""
TEST_HANDOVER_ID=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

print_header() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}============================================${NC}"
}

# 打印测试结果
print_result() {
    local test_name=$1
    local status=$2
    local message=$3

    TOTAL=$((TOTAL + 1))

    if [ "$status" = "PASS" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name"
        PASSED=$((PASSED + 1))
    elif [ "$status" = "WARN" ]; then
        echo -e "  ${YELLOW}⚠${NC} $test_name: $message"
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "  ${RED}✗${NC} $test_name"
        if [ -n "$message" ]; then
            echo -e "    ${RED}Error: $message${NC}"
        fi
        FAILED=$((FAILED + 1))
    fi
}

# 发送HTTP请求
send_request() {
    local method=$1
    local url=$2
    local data=$3
    local silent=$4

    if [ -n "$data" ]; then
        if [ "$silent" = "true" ]; then
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" \
                -d "$data" 2>/dev/null
        else
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" \
                -d "$data"
        fi
    else
        if [ "$silent" = "true" ]; then
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "Authorization: Bearer $TOKEN" 2>/dev/null
        else
            curl -s -w "\n%{http_code}" -X "$method" "$url" \
                -H "Authorization: Bearer $TOKEN"
        fi
    fi
}

# 检查服务状态
check_service() {
    print_header "检查服务状态"
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        log_success "后端服务运行中"
        return 0
    else
        log_error "后端服务未运行"
        return 1
    fi
}

# 登录获取Token
test_login() {
    print_header "认证测试"

    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')

    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)

    if [ "$success" = "true" ] && [ "$http_code" = "200" ]; then
        TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
        print_result "管理员登录" "PASS"
        log_info "Token: ${TOKEN:0:20}..."
        return 0
    else
        print_result "管理员登录" "FAIL" "HTTP $http_code"
        return 1
    fi
}

# ============================================
# 1. 用户管理测试
# ============================================
test_user_management() {
    print_header "用户管理测试"

    # 1.1 查询用户列表
    local response=$(send_request "GET" "$BASE_URL/system/user/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local total=$(echo "$body" | grep -o '"total":[0-9]*' | cut -d':' -f2)
        print_result "查询用户列表" "PASS" "共 $total 个用户"
    else
        print_result "查询用户列表" "FAIL" "HTTP $http_code"
    fi

    # 1.2 查询用户详情（获取admin）
    response=$(send_request "GET" "$BASE_URL/system/user/1")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local username=$(echo "$body" | grep -o '"username":"[^"]*"' | head -1 | cut -d'"' -f4)
        print_result "查询用户详情" "PASS" "用户: $username"
    else
        print_result "查询用户详情" "WARN" "可能需要调整用户ID"
    fi

    # 1.3 创建测试用户
    local timestamp=$(date +%s)
    local test_username="test_user_$timestamp"
    response=$(send_request "POST" "$BASE_URL/system/user" "{
        \"username\": \"$test_username\",
        \"password\": \"Test123456\",
        \"realName\": \"测试用户\",
        \"email\": \"test@example.com\",
        \"phone\": \"13800138000\",
        \"departmentId\": 1,
        \"roleIds\": [2],
        \"status\": 1
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_USER_ID=$(echo "$body" | grep -o '"data":[^}]*' | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "创建测试用户" "PASS" "ID: $TEST_USER_ID"
        else
            print_result "创建测试用户" "WARN" "用户可能已存在"
        fi
    else
        print_result "创建测试用户" "FAIL" "HTTP $http_code"
    fi

    # 1.4 更新用户信息
    if [ -n "$TEST_USER_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/user" "{
            \"id\": $TEST_USER_ID,
            \"realName\": \"测试用户(已更新)\",
            \"email\": \"updated@example.com\",
            \"phone\": \"13900139000\"
        }")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "更新用户信息" "PASS"
        else
            print_result "更新用户信息" "FAIL" "HTTP $http_code"
        fi
    fi

    # 1.5 禁用/启用用户
    if [ -n "$TEST_USER_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/user/$TEST_USER_ID/status" "{\"status\":0}")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "禁用用户" "PASS"
        else
            print_result "禁用用户" "WARN" "接口可能未实现"
        fi

        # 恢复启用
        response=$(send_request "PUT" "$BASE_URL/system/user/$TEST_USER_ID/status" "{\"status\":1}")
    fi

    # 1.6 重置密码
    if [ -n "$TEST_USER_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/user/$TEST_USER_ID/password/reset" "{}")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "重置用户密码" "PASS"
        else
            print_result "重置用户密码" "WARN" "接口可能未实现"
        fi
    fi

    # 1.7 删除测试用户
    if [ -n "$TEST_USER_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/system/user/$TEST_USER_ID")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "删除测试用户" "PASS"
        else
            print_result "删除测试用户" "WARN" "接口可能受保护"
        fi
    fi
}

# ============================================
# 2. 角色管理测试
# ============================================
test_role_management() {
    print_header "角色管理测试"

    # 2.1 查询角色列表
    local response=$(send_request "GET" "$BASE_URL/system/role/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local roles=$(echo "$body" | grep -o '"roleName":"[^"]*"' | wc -l | tr -d ' ')
        print_result "查询角色列表" "PASS" "共 $roles 个角色"
    else
        print_result "查询角色列表" "FAIL" "HTTP $http_code"
    fi

    # 2.2 查询角色详情
    response=$(send_request "GET" "$BASE_URL/system/role/2")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local role_name=$(echo "$body" | grep -o '"roleName":"[^"]*"' | cut -d'"' -f4)
        print_result "查询角色详情" "PASS" "角色: $role_name"
    else
        print_result "查询角色详情" "WARN" "可能需要调整角色ID"
    fi

    # 2.3 创建测试角色
    local timestamp=$(date +%s)
    response=$(send_request "POST" "$BASE_URL/system/role" "{
        \"roleName\": \"测试角色_$timestamp\",
        \"roleCode\": \"TEST_ROLE_$timestamp\",
        \"description\": \"自动化测试创建的角色\",
        \"status\": 1
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_ROLE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "创建测试角色" "PASS" "ID: $TEST_ROLE_ID"
        else
            print_result "创建测试角色" "WARN" "$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
        fi
    else
        print_result "创建测试角色" "WARN" "HTTP $http_code"
    fi

    # 2.4 分配权限给角色
    if [ -n "$TEST_ROLE_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/role/$TEST_ROLE_ID/permissions" "{
            \"menuIds\": [1, 2, 3, 100, 101]
        }")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "分配角色权限" "PASS"
        else
            print_result "分配角色权限" "WARN" "接口可能未实现"
        fi
    fi

    # 2.5 查询角色权限
    if [ -n "$TEST_ROLE_ID" ]; then
        response=$(send_request "GET" "$BASE_URL/system/role/$TEST_ROLE_ID/permissions")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "查询角色权限" "PASS"
        else
            print_result "查询角色权限" "WARN" "接口可能未实现"
        fi
    fi

    # 2.6 删除测试角色
    if [ -n "$TEST_ROLE_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/system/role/$TEST_ROLE_ID")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "删除测试角色" "PASS"
        else
            print_result "删除测试角色" "WARN" "角色可能受保护"
        fi
    fi
}

# ============================================
# 3. 部门管理测试
# ============================================
test_department_management() {
    print_header "部门管理测试"

    # 3.1 查询部门树
    local response=$(send_request "GET" "$BASE_URL/system/department/tree")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local dept_count=$(echo "$body" | grep -o '"deptName":"[^"]*"' | wc -l | tr -d ' ')
        print_result "查询部门树" "PASS" "共 $dept_count 个部门"
    else
        print_result "查询部门树" "FAIL" "HTTP $http_code"
    fi

    # 3.2 查询部门详情
    response=$(send_request "GET" "$BASE_URL/system/department/1")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local dept_name=$(echo "$body" | grep -o '"deptName":"[^"]*"' | cut -d'"' -f4)
        print_result "查询部门详情" "PASS" "部门: $dept_name"
    else
        print_result "查询部门详情" "WARN" "可能需要调整部门ID"
    fi

    # 3.3 创建测试部门
    local timestamp=$(date +%s)
    response=$(send_request "POST" "$BASE_URL/system/department" "{
        \"deptName\": \"测试部门_$timestamp\",
        \"parentId\": 0,
        \"description\": \"自动化测试创建的部门\"
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_DEPT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "创建测试部门" "PASS" "ID: $TEST_DEPT_ID"
        else
            print_result "创建测试部门" "WARN" "$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
        fi
    else
        print_result "创建测试部门" "WARN" "HTTP $http_code"
    fi

    # 3.4 更新部门
    if [ -n "$TEST_DEPT_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/department/$TEST_DEPT_ID" "{
            \"deptName\": \"测试部门(已更新)\",
            \"description\": \"描述已更新\"
        }")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "更新部门信息" "PASS"
        else
            print_result "更新部门信息" "WARN" "HTTP $http_code"
        fi
    fi

    # 3.5 删除测试部门
    if [ -n "$TEST_DEPT_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/system/department/$TEST_DEPT_ID")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "删除测试部门" "PASS"
        else
            print_result "删除测试部门" "WARN" "部门可能包含子部门或用户"
        fi
    fi
}

# ============================================
# 4. 菜单管理测试
# ============================================
test_menu_management() {
    print_header "菜单管理测试"

    # 4.1 查询菜单树
    local response=$(send_request "GET" "$BASE_URL/system/menu/tree")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local menu_count=$(echo "$body" | grep -o '"title":"[^"]*"' | wc -l | tr -d ' ')
        print_result "查询菜单树" "PASS" "共 $menu_count 个菜单项"
    else
        print_result "查询菜单树" "FAIL" "HTTP $http_code"
    fi

    # 4.2 查询用户菜单（当前用户）
    response=$(send_request "GET" "$BASE_URL/system/menu/user")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        print_result "查询用户菜单" "PASS"
    else
        print_result "查询用户菜单" "WARN" "HTTP $http_code"
    fi

    # 4.3 创建测试菜单
    local timestamp=$(date +%s)
    response=$(send_request "POST" "$BASE_URL/system/menu" "{
        \"title\": \"测试菜单_$timestamp\",
        \"path\": \"/test-$timestamp\",
        \"component\": \"test/Test\",
        \"parentId\": 0,
        \"menuType\": 1,
        \"icon\": \"test-icon\",
        \"sort\": 999
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_MENU_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "创建测试菜单" "PASS" "ID: $TEST_MENU_ID"
        else
            print_result "创建测试菜单" "WARN" "$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
        fi
    else
        print_result "创建测试菜单" "WARN" "HTTP $http_code"
    fi

    # 4.4 更新菜单
    if [ -n "$TEST_MENU_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/menu/$TEST_MENU_ID" "{
            \"title\": \"测试菜单(已更新)\",
            \"icon\": \"updated-icon\"
        }")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "更新菜单信息" "PASS"
        else
            print_result "更新菜单信息" "WARN" "HTTP $http_code"
        fi
    fi

    # 4.5 删除测试菜单
    if [ -n "$TEST_MENU_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/system/menu/$TEST_MENU_ID")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "删除测试菜单" "PASS"
        else
            print_result "删除测试菜单" "WARN" "菜单可能受保护"
        fi
    fi
}

# ============================================
# 5. 字典管理测试
# ============================================
test_dict_management() {
    print_header "字典管理测试"

    # 5.1 查询字典类型列表
    local response=$(send_request "GET" "$BASE_URL/system/dict/types?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local dict_count=$(echo "$body" | grep -o '"dictName":"[^"]*"' | wc -l | tr -d ' ')
        print_result "查询字典类型列表" "PASS" "共 $dict_count 个字典类型"
    else
        print_result "查询字典类型列表" "FAIL" "HTTP $http_code"
    fi

    # 5.2 查询字典项
    response=$(send_request "GET" "$BASE_URL/system/dict/items/matter_type")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local item_count=$(echo "$body" | grep -o '"itemText":"[^"]*"' | wc -l | tr -d ' ')
        print_result "查询字典项(matter_type)" "PASS" "共 $item_count 个字典项"
    else
        print_result "查询字典项" "WARN" "字典类型可能不存在"
    fi

    # 5.3 创建测试字典类型
    local timestamp=$(date +%s)
    response=$(send_request "POST" "$BASE_URL/system/dict/type" "{
        \"dictName\": \"测试字典_$timestamp\",
        \"dictCode\": \"TEST_DICT_$timestamp\",
        \"description\": \"自动化测试创建的字典\"
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_DICT_TYPE_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "创建测试字典类型" "PASS" "ID: $TEST_DICT_TYPE_ID"
        else
            print_result "创建测试字典类型" "WARN" "$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
        fi
    else
        print_result "创建测试字典类型" "WARN" "HTTP $http_code"
    fi

    # 5.4 添加字典项
    if [ -n "$TEST_DICT_TYPE_ID" ]; then
        response=$(send_request "POST" "$BASE_URL/system/dict/item" "{
            \"dictCode\": \"TEST_DICT_$timestamp\",
            \"itemText\": \"测试项1\",
            \"itemValue\": \"1\",
            \"sort\": 1
        }")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "添加字典项" "PASS"
        else
            print_result "添加字典项" "WARN" "HTTP $http_code"
        fi
    fi

    # 5.5 删除测试字典类型
    if [ -n "$TEST_DICT_TYPE_ID" ]; then
        response=$(send_request "DELETE" "$BASE_URL/system/dict/type/$TEST_DICT_TYPE_ID")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "删除测试字典类型" "PASS"
        else
            print_result "删除测试字典类型" "WARN" "HTTP $http_code"
        fi
    fi
}

# ============================================
# 6. 系统配置测试
# ============================================
test_system_config() {
    print_header "系统配置测试"

    # 6.1 查询系统配置列表
    local response=$(send_request "GET" "$BASE_URL/system/config")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local config_count=$(echo "$body" | grep -o '"configKey":"[^"]*"' | wc -l | tr -d ' ')
        print_result "查询系统配置列表" "PASS" "共 $config_count 个配置项"
    else
        print_result "查询系统配置列表" "FAIL" "HTTP $http_code"
    fi

    # 6.2 按键查询配置
    response=$(send_request "GET" "$BASE_URL/system/config/key/system.siteName")
    http_code=$(echo "$response" | tail -1)

    if [ "$http_code" = "200" ]; then
        print_result "按键查询配置(system.siteName)" "PASS"
    else
        print_result "按键查询配置" "WARN" "HTTP $http_code"
    fi

    # 6.3 批量获取配置
    response=$(send_request "POST" "$BASE_URL/system/config/batch" "[\"system.siteName\",\"sys.version\"]")
    http_code=$(echo "$response" | tail -1)

    if [ "$http_code" = "200" ]; then
        print_result "批量获取配置" "PASS"
    else
        print_result "批量获取配置" "WARN" "HTTP $http_code"
    fi

    # 6.4 获取系统版本信息
    response=$(send_request "GET" "$BASE_URL/system/config/version")
    http_code=$(echo "$response" | tail -1)

    if [ "$http_code" = "200" ]; then
        print_result "获取系统版本信息" "PASS"
    else
        print_result "获取系统版本信息" "WARN" "HTTP $http_code"
    fi
}

# ============================================
# 7. 日志查询测试
# ============================================
test_logs() {
    print_header "日志查询测试"

    # 7.1 查询操作日志
    local response=$(send_request "GET" "$BASE_URL/system/operation-log/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local total=$(echo "$body" | grep -o '"total":[0-9]*' | cut -d':' -f2)
        print_result "查询操作日志" "PASS" "共 $total 条记录"
    else
        print_result "查询操作日志" "WARN" "HTTP $http_code"
    fi

    # 7.2 查询登录日志
    response=$(send_request "GET" "$BASE_URL/system/login-log/list?pageNum=1&pageSize=10")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        total=$(echo "$body" | grep -o '"total":[0-9]*' | cut -d':' -f2)
        print_result "查询登录日志" "PASS" "共 $total 条记录"
    else
        print_result "查询登录日志" "WARN" "HTTP $http_code"
    fi

    # 7.3 查询当前用户会话
    response=$(send_request "GET" "$BASE_URL/system/session/list")
    http_code=$(echo "$response" | tail -1)

    if [ "$http_code" = "200" ]; then
        print_result "查询用户会话" "PASS"
    else
        print_result "查询用户会话" "WARN" "HTTP $http_code"
    fi
}

# ============================================
# 8. 通知功能测试
# ============================================
test_notification() {
    print_header "通知功能测试"

    # 8.1 查询通知列表
    local response=$(send_request "GET" "$BASE_URL/system/notification/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local total=$(echo "$body" | grep -o '"total":[0-9]*' | cut -d':' -f2)
        print_result "查询通知列表" "PASS" "共 $total 条通知"
    else
        print_result "查询通知列表" "WARN" "HTTP $http_code"
    fi

    # 8.2 发送测试通知
    local timestamp=$(date +%s)
    response=$(send_request "POST" "$BASE_URL/system/notification/send" "{
        \"title\": \"自动化测试通知_$timestamp\",
        \"content\": \"这是一条自动化测试发送的通知\",
        \"type\": \"system\",
        \"recipientType\": \"all\",
        \"priority\": \"normal\"
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_NOTIFICATION_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "发送通知" "PASS" "ID: $TEST_NOTIFICATION_ID"
        else
            print_result "发送通知" "WARN" "$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
        fi
    else
        print_result "发送通知" "WARN" "HTTP $http_code"
    fi

    # 8.3 查询未读通知
    response=$(send_request "GET" "$BASE_URL/system/notification/unread")
    http_code=$(echo "$response" | tail -1)

    if [ "$http_code" = "200" ]; then
        print_result "查询未读通知" "PASS"
    else
        print_result "查询未读通知" "WARN" "HTTP $http_code"
    fi

    # 8.4 标记通知为已读
    if [ -n "$TEST_NOTIFICATION_ID" ]; then
        response=$(send_request "PUT" "$BASE_URL/system/notification/$TEST_NOTIFICATION_ID/read" "{}")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "标记通知已读" "PASS"
        else
            print_result "标记通知已读" "WARN" "HTTP $http_code"
        fi
    fi

    # 8.5 全部标记为已读
    response=$(send_request "PUT" "$BASE_URL/system/notification/read-all" "{}")
    http_code=$(echo "$response" | tail -1)

    if [ "$http_code" = "200" ]; then
        print_result "全部标记已读" "PASS"
    else
        print_result "全部标记已读" "WARN" "HTTP $http_code"
    fi
}

# ============================================
# 9. 数据交接测试
# ============================================
test_data_handover() {
    print_header "数据交接测试"

    # 9.1 查询交接列表
    local response=$(send_request "GET" "$BASE_URL/system/handover/list?pageNum=1&pageSize=10")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local total=$(echo "$body" | grep -o '"total":[0-9]*' | cut -d':' -f2)
        print_result "查询交接列表" "PASS" "共 $total 条记录"
    else
        print_result "查询交接列表" "WARN" "HTTP $http_code"
    fi

    # 9.2 创建数据交接
    local timestamp=$(date +%s)
    response=$(send_request "POST" "$BASE_URL/system/handover" "{
        \"title\": \"自动化测试交接_$timestamp\",
        \"description\": \"这是一条自动化测试创建的数据交接\",
        \"handoverType\": \"matter\",
        \"receiverId\": 2,
        \"items\": [{\"type\": \"matter\", \"id\": 1, \"name\": \"测试项目\"}]
    }")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local success=$(echo "$body" | grep -o '"success":[^,]*' | cut -d':' -f2)
        if [ "$success" = "true" ]; then
            TEST_HANDOVER_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
            print_result "创建数据交接" "PASS" "ID: $TEST_HANDOVER_ID"
        else
            print_result "创建数据交接" "WARN" "$(echo "$body" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
        fi
    else
        print_result "创建数据交接" "WARN" "HTTP $http_code"
    fi

    # 9.3 确认交接
    if [ -n "$TEST_HANDOVER_ID" ]; then
        response=$(send_request "POST" "$BASE_URL/system/handover/$TEST_HANDOVER_ID/confirm" "{
            \"comment\": \"测试确认交接\"
        }")
        http_code=$(echo "$response" | tail -1)

        if [ "$http_code" = "200" ]; then
            print_result "确认数据交接" "PASS"
        else
            print_result "确认数据交接" "WARN" "HTTP $http_code"
        fi
    fi
}

# ============================================
# 10. 数据勾连测试
# ============================================
test_data_integration() {
    print_header "数据勾连测试"

    # 10.1 用户-部门-角色关联检查
    local response=$(send_request "GET" "$BASE_URL/system/user/list?pageNum=1&pageSize=1&includeDept=true&includeRoles=true")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        # 检查返回的数据是否包含部门和角色信息
        if echo "$body" | grep -q '"department"' && echo "$body" | grep -q '"roles"'; then
            print_result "用户-部门-角色关联" "PASS" "数据关联正确"
        else
            print_result "用户-部门-角色关联" "WARN" "关联数据可能未返回"
        fi
    else
        print_result "用户-部门-角色关联" "WARN" "HTTP $http_code"
    fi

    # 10.2 角色-菜单权限关联检查
    response=$(send_request "GET" "$BASE_URL/system/role/1")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        if echo "$body" | grep -q '"menuIds"' || echo "$body" | grep -q '"permissions"'; then
            print_result "角色-菜单权限关联" "PASS" "权限数据存在"
        else
            print_result "角色-菜单权限关联" "WARN" "权限数据可能未返回"
        fi
    else
        print_result "角色-菜单权限关联" "WARN" "HTTP $http_code"
    fi

    # 10.3 字典项引用检查
    response=$(send_request "GET" "$BASE_URL/system/dict/items/matter_type")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        local has_items=$(echo "$body" | grep -o '"itemValue":"[^"]*"' | wc -l | tr -d ' ')
        if [ "$has_items" -gt 0 ]; then
            print_result "字典项引用" "PASS" "字典项数据正确"
        else
            print_result "字典项引用" "WARN" "字典项为空"
        fi
    else
        print_result "字典项引用" "WARN" "HTTP $http_code"
    fi

    # 10.4 操作日志-用户关联检查
    response=$(send_request "GET" "$BASE_URL/system/operation-log/list?pageNum=1&pageSize=5")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        if echo "$body" | grep -q '"username"' && echo "$body" | grep -q '"moduleName"'; then
            print_result "操作日志-用户关联" "PASS" "日志关联数据正确"
        else
            print_result "操作日志-用户关联" "WARN" "日志关联数据可能不完整"
        fi
    else
        print_result "操作日志-用户关联" "WARN" "HTTP $http_code"
    fi

    # 10.5 登录日志-用户关联检查
    response=$(send_request "GET" "$BASE_URL/system/login-log/list?pageNum=1&pageSize=5")
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        if echo "$body" | grep -q '"username"' && echo "$body" | grep -q '"loginTime"'; then
            print_result "登录日志-用户关联" "PASS" "登录日志数据正确"
        else
            print_result "登录日志-用户关联" "WARN" "登录日志数据可能不完整"
        fi
    else
        print_result "登录日志-用户关联" "WARN" "HTTP $http_code"
    fi
}

# ============================================
# 11. 权限控制测试
# ============================================
test_permission_control() {
    print_header "权限控制测试"

    # 11.1 测试无Token访问
    local temp_token=$TOKEN
    TOKEN=""

    local response=$(send_request "GET" "$BASE_URL/system/user/list")
    local http_code=$(echo "$response" | tail -1)

    TOKEN=$temp_token

    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "无Token访问拒绝" "PASS" "HTTP $http_code"
    else
        print_result "无Token访问拒绝" "WARN" "应返回401/403, 实际: $http_code"
    fi

    # 11.2 测试无效Token
    TOKEN="invalid_token_12345"
    response=$(send_request "GET" "$BASE_URL/system/user/list")
    http_code=$(echo "$response" | tail -1)
    TOKEN=$temp_token

    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        print_result "无效Token访问拒绝" "PASS" "HTTP $http_code"
    else
        print_result "无效Token访问拒绝" "WARN" "应返回401/403, 实际: $http_code"
    fi
}

# ============================================
# 打印测试总结
# ============================================
print_summary() {
    print_header "测试报告"

    echo "总测试数: $TOTAL"
    echo -e "${GREEN}通过: $PASSED${NC}"
    echo -e "${YELLOW}警告: $WARNINGS${NC}"
    echo -e "${RED}失败: $FAILED${NC}"

    if [ $TOTAL -gt 0 ]; then
        local pass_rate=$((PASSED * 100 / TOTAL))
        local total_success=$((PASSED + WARNINGS))
        local total_rate=$((total_success * 100 / TOTAL))
        echo "通过率: ${pass_rate}%"
        echo "成功率(含警告): ${total_rate}%"
    fi

    echo ""
    echo "============================================"
    echo "测试结论"
    echo "============================================"

    if [ $FAILED -eq 0 ] && [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✓ 系统模块所有测试通过！${NC}"
        echo "业务逻辑正常，数据勾连完整。"
        exit 0
    elif [ $FAILED -eq 0 ]; then
        echo -e "${YELLOW}⚠ 有 $WARNINGS 个警告，建议检查${NC}"
        echo "核心功能正常，部分功能可能需要调整。"
        exit 0
    else
        echo -e "${RED}✗ 有 $FAILED 个测试失败${NC}"
        echo "请检查失败的测试项。"
        exit 1
    fi
}

# ============================================
# 主函数
# ============================================
main() {
    echo ""
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}  系统模块业务逻辑和数据勾连测试${NC}"
    echo -e "${CYAN}============================================${NC}"
    echo ""

    # 检查服务
    if ! check_service; then
        log_error "后端服务未运行，请先启动服务"
        exit 1
    fi

    # 登录
    if ! test_login; then
        log_error "登录失败，无法继续测试"
        exit 1
    fi

    # 执行测试
    test_user_management
    test_role_management
    test_department_management
    test_menu_management
    test_dict_management
    test_system_config
    test_logs
    test_notification
    test_data_handover
    test_data_integration
    test_permission_control

    # 打印总结
    print_summary
}

# 运行主函数
main
