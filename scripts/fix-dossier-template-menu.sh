#!/bin/bash

# =====================================================
# 修复卷宗模板菜单配置脚本
# =====================================================
# 用途: 如果卷宗模板菜单不存在，自动修复
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查是否使用Docker
USE_DOCKER=false
DOCKER_CONTAINER=""

# 检查PostgreSQL容器是否运行
if docker ps --format '{{.Names}}' | grep -q "law-firm.*postgres\|postgres.*law-firm"; then
    USE_DOCKER=true
    DOCKER_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E "law-firm.*postgres|postgres.*law-firm" | head -n1)
    echo -e "${BLUE}检测到Docker环境，使用容器: $DOCKER_CONTAINER${NC}"
fi

# 获取数据库连接信息
if [ "$USE_DOCKER" = false ]; then
    if [ -f ".env" ]; then
        source .env
    elif [ -f "docker/.env" ]; then
        source docker/.env
    elif [ -f "../docker/.env" ]; then
        source ../docker/.env
    else
        echo -e "${RED}错误: 未找到 .env 文件${NC}"
        exit 1
    fi
    
    # 数据库连接参数
    DB_HOST="${POSTGRES_HOST:-localhost}"
    DB_PORT="${POSTGRES_PORT:-5432}"
    DB_NAME="${POSTGRES_DB:-law_firm}"
    DB_USER="${POSTGRES_USER:-lawfirm}"
    DB_PASSWORD="${POSTGRES_PASSWORD}"
    
    if [ -z "$DB_PASSWORD" ]; then
        echo -e "${RED}错误: 未找到数据库密码配置${NC}"
        exit 1
    fi
else
    # Docker环境，从容器环境变量获取
    DB_NAME="${POSTGRES_DB:-law_firm}"
    DB_USER="${POSTGRES_USER:-law_admin}"
fi

# 执行SQL的函数
run_sql() {
    local sql="$1"
    if [ "$USE_DOCKER" = true ]; then
        docker exec "$DOCKER_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "$sql" 2>&1
    else
        export PGPASSWORD="$DB_PASSWORD"
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$sql" 2>&1
    fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}修复卷宗模板菜单配置${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 强制更新菜单（无论是否存在都更新，确保所有字段正确）
echo -e "${YELLOW}更新菜单配置...${NC}"
run_sql "INSERT INTO sys_menu VALUES (715, 6, '卷宗模板', '/document/dossier-template', 'document/dossier-template/index', NULL, 'FileProtectOutlined', 'MENU', 'doc:list', 5, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO UPDATE SET 
    parent_id = 6,
    name = '卷宗模板',
    path = '/document/dossier-template',
    component = 'document/dossier-template/index',
    icon = 'FileProtectOutlined',
    menu_type = 'MENU',
    permission = 'doc:list',
    sort_order = 5,
    visible = true,
    status = 'ENABLED',
    deleted = false,
    updated_at = CURRENT_TIMESTAMP;" > /dev/null 2>&1
echo -e "${GREEN}✅ 菜单配置已更新${NC}"

# 检查并分配角色权限
echo ""
echo -e "${YELLOW}检查并分配角色权限...${NC}"

# 管理员角色 (role_id = 1)
ROLE1_EXISTS=$(run_sql "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id = 715;" | grep -oE '[0-9]+' | head -n1 | tr -d ' ')

if [ "$ROLE1_EXISTS" = "0" ]; then
    echo -e "${YELLOW}  为管理员角色分配权限...${NC}"
    run_sql "INSERT INTO sys_role_menu (role_id, menu_id, created_at) SELECT 1, 715, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 715);" > /dev/null 2>&1
fi

# 律所主任角色 (role_id = 2)
ROLE2_EXISTS=$(run_sql "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2 AND menu_id = 715;" | grep -oE '[0-9]+' | head -n1 | tr -d ' ')

if [ "$ROLE2_EXISTS" = "0" ]; then
    echo -e "${YELLOW}  为律所主任角色分配权限...${NC}"
    run_sql "INSERT INTO sys_role_menu (role_id, menu_id, created_at) SELECT 2, 715, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 715);" > /dev/null 2>&1
fi

# 行政角色 (role_id = 8)
ROLE8_EXISTS=$(run_sql "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 8 AND menu_id = 715;" | grep -oE '[0-9]+' | head -n1 | tr -d ' ')

if [ "$ROLE8_EXISTS" = "0" ]; then
    echo -e "${YELLOW}  为行政角色分配权限...${NC}"
    run_sql "INSERT INTO sys_role_menu (role_id, menu_id, created_at) SELECT 8, 715, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 8 AND menu_id = 715);" > /dev/null 2>&1
fi

echo -e "${GREEN}✅ 权限分配完成${NC}"
echo ""

# 验证修复结果
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}验证修复结果${NC}"
echo -e "${BLUE}========================================${NC}"

MENU_COUNT=$(run_sql "SELECT COUNT(*) FROM sys_menu WHERE id = 715;" | grep -oE '[0-9]+' | head -n1 | tr -d ' ')

ROLE_COUNT=$(run_sql "SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = 715;" | grep -oE '[0-9]+' | head -n1 | tr -d ' ')

if [ "$MENU_COUNT" = "1" ] && [ "$ROLE_COUNT" -ge "3" ]; then
    echo -e "${GREEN}✅ 修复成功！${NC}"
    echo "  菜单存在: ✅"
    echo "  权限分配: ✅ ($ROLE_COUNT 个角色)"
    echo ""
    echo -e "${YELLOW}提示: 请重新登录系统或刷新页面以查看菜单${NC}"
    exit 0
else
    echo -e "${RED}❌ 修复失败，请检查数据库连接和权限${NC}"
    exit 1
fi
