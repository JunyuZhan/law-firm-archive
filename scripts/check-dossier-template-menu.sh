#!/bin/bash

# =====================================================
# 检查卷宗模板菜单配置脚本
# =====================================================
# 用途: 检查数据库中卷宗模板菜单是否存在，以及权限是否正确分配
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
    DB_USER="${POSTGRES_USER:-lawfirm}"
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}检查卷宗模板菜单配置${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 执行SQL查询的函数
run_sql() {
    local sql="$1"
    if [ "$USE_DOCKER" = true ]; then
        docker exec "$DOCKER_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "$sql" 2>/dev/null || echo ""
    else
        export PGPASSWORD="$DB_PASSWORD"
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "$sql" 2>/dev/null || echo ""
    fi
}

# 检查菜单是否存在
echo -e "${YELLOW}[1/3] 检查菜单是否存在...${NC}"
MENU_CHECK=$(run_sql "
SELECT 
    id,
    name,
    path,
    component,
    permission,
    visible,
    status,
    parent_id
FROM sys_menu 
WHERE id = 715 OR path = '/document/dossier-template';
")

if [ -z "$MENU_CHECK" ] || [ "$MENU_CHECK" = "" ]; then
    echo -e "${RED}❌ 菜单不存在！${NC}"
    MENU_EXISTS=false
else
    echo -e "${GREEN}✅ 菜单存在：${NC}"
    echo "$MENU_CHECK" | while IFS='|' read -r id name path component permission visible status parent_id; do
        echo "  ID: $id"
        echo "  名称: $name"
        echo "  路径: $path"
        echo "  组件: $component"
        echo "  权限: $permission"
        echo "  可见: $visible"
        echo "  状态: $status"
        echo "  父菜单ID: $parent_id"
    done
    MENU_EXISTS=true
fi
echo ""

# 检查角色权限分配
echo -e "${YELLOW}[2/3] 检查角色权限分配...${NC}"
ROLE_CHECK=$(run_sql "
SELECT 
    rm.id,
    r.id as role_id,
    r.name as role_name,
    rm.menu_id
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.id
WHERE rm.menu_id = 715
ORDER BY r.id;
")

if [ -z "$ROLE_CHECK" ] || [ "$ROLE_CHECK" = "" ]; then
    echo -e "${RED}❌ 未找到角色权限分配！${NC}"
    ROLE_EXISTS=false
else
    echo -e "${GREEN}✅ 已分配权限的角色：${NC}"
    echo "$ROLE_CHECK" | while IFS='|' read -r id role_id role_name menu_id; do
        echo "  角色ID: $role_id | 角色名称: $role_name"
    done
    ROLE_EXISTS=true
fi
echo ""

# 检查父菜单（卷宗管理）
echo -e "${YELLOW}[3/3] 检查父菜单（卷宗管理）...${NC}"
PARENT_CHECK=$(run_sql "
SELECT 
    id,
    name,
    path,
    visible,
    status
FROM sys_menu 
WHERE id = 6;
")

if [ -z "$PARENT_CHECK" ] || [ "$PARENT_CHECK" = "" ]; then
    echo -e "${RED}❌ 父菜单（卷宗管理）不存在！${NC}"
    PARENT_EXISTS=false
else
    echo -e "${GREEN}✅ 父菜单存在：${NC}"
    echo "$PARENT_CHECK" | while IFS='|' read -r id name path visible status; do
        echo "  ID: $id"
        echo "  名称: $name"
        echo "  路径: $path"
        echo "  可见: $visible"
        echo "  状态: $status"
    done
    PARENT_EXISTS=true
fi
echo ""

# 总结
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}检查结果总结${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$MENU_EXISTS" = true ] && [ "$ROLE_EXISTS" = true ] && [ "$PARENT_EXISTS" = true ]; then
    echo -e "${GREEN}✅ 所有检查通过！卷宗模板菜单配置正常。${NC}"
    exit 0
else
    echo -e "${RED}❌ 发现问题，需要修复：${NC}"
    [ "$MENU_EXISTS" = false ] && echo "  - 菜单不存在"
    [ "$ROLE_EXISTS" = false ] && echo "  - 角色权限未分配"
    [ "$PARENT_EXISTS" = false ] && echo "  - 父菜单不存在"
    echo ""
    echo -e "${YELLOW}修复方法：${NC}"
    echo "  1. 执行数据库初始化脚本："
    echo "     psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f scripts/init-db/20-init-data.sql"
    echo ""
    echo "  2. 或者手动执行以下SQL："
    echo ""
    echo "     -- 插入菜单"
    echo "     INSERT INTO sys_menu VALUES (715, 6, '卷宗模板', '/document/dossier-template', 'document/dossier-template/index', NULL, 'FileProtectOutlined', 'MENU', 'doc:list', 5, true, 'ENABLED', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false) ON CONFLICT (id) DO NOTHING;"
    echo ""
    echo "     -- 分配权限（管理员、律所主任、行政）"
    echo "     INSERT INTO sys_role_menu VALUES (1971, 1, 715, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING;"
    echo "     INSERT INTO sys_role_menu VALUES (1972, 2, 715, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING;"
    echo "     INSERT INTO sys_role_menu VALUES (1973, 8, 715, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING;"
    exit 1
fi
