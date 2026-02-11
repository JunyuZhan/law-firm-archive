#!/bin/bash

# 智慧律所管理系统 - 前后端权限码一致性检查
# 用于检测前端使用的权限码是否与后端定义一致

FRONTEND_DIR="/Users/apple/Documents/Project/law-firm/frontend"
BACKEND_DIR="/Users/apple/Documents/Project/law-firm/backend"
SCRIPTS_DIR="/Users/apple/Documents/Project/law-firm/scripts"

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo ""
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}     前后端权限码一致性检查${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo ""

# 临时文件
FRONTEND_CODES=$(mktemp)
BACKEND_CODES=$(mktemp)
MENU_CODES=$(mktemp)

# 1. 提取前端使用的权限码
echo -e "${CYAN}[1/4] 提取前端使用的权限码...${NC}"

# v-access 指令
grep -rhoE "v-access:code=\"[^\"]+\"" "$FRONTEND_DIR" 2>/dev/null | \
    sed "s/v-access:code=\"//g" | sed 's/"//g' | \
    tr "'" '"' | tr ',' '\n' | tr -d '[]' | \
    sed 's/^[[:space:]]*//g' | sed 's/[[:space:]]*$//g' >> "$FRONTEND_CODES"

# v-access 数组形式
grep -rhoE "v-access:code=\"\[[^]]+\]\"" "$FRONTEND_DIR" 2>/dev/null | \
    sed "s/v-access:code=\"//g" | sed 's/"//g' | \
    tr "'" ' ' | tr ',' '\n' | tr -d '[]' | \
    sed 's/^[[:space:]]*//g' | sed 's/[[:space:]]*$//g' >> "$FRONTEND_CODES"

# hasPermission 调用
grep -rhoE "hasPermission\(['\"][^'\"]+['\"]" "$FRONTEND_DIR" 2>/dev/null | \
    sed "s/hasPermission(//g" | tr -d "'" | tr -d '"' >> "$FRONTEND_CODES"

# hasAnyPermission 调用
grep -rhoE "hasAnyPermission\(['\"][^'\"]+['\"]" "$FRONTEND_DIR" 2>/dev/null | \
    sed "s/hasAnyPermission(//g" | tr -d "'" | tr -d '"' >> "$FRONTEND_CODES"

# 路由 authority
grep -rhoE "authority:[[:space:]]*\[[^]]+\]" "$FRONTEND_DIR" 2>/dev/null | \
    sed 's/authority://g' | tr -d '[]' | tr ',' '\n' | \
    tr -d "'" | tr -d '"' | \
    sed 's/^[[:space:]]*//g' | sed 's/[[:space:]]*$//g' >> "$FRONTEND_CODES"

# 去重排序
sort -u "$FRONTEND_CODES" | grep -v "^$" > "${FRONTEND_CODES}.tmp"
mv "${FRONTEND_CODES}.tmp" "$FRONTEND_CODES"

FRONTEND_COUNT=$(wc -l < "$FRONTEND_CODES" | tr -d ' ')
echo -e "   找到 ${GREEN}$FRONTEND_COUNT${NC} 个前端权限码"

# 2. 提取后端 @RequirePermission 使用的权限码
echo -e "${CYAN}[2/4] 提取后端 @RequirePermission 权限码...${NC}"

grep -rhoE "@RequirePermission\([^)]+\)" "$BACKEND_DIR" 2>/dev/null | \
    sed 's/@RequirePermission(//g' | sed 's/)//g' | \
    sed 's/value[[:space:]]*=[[:space:]]*//g' | \
    sed 's/logical[[:space:]]*=[[:space:]]*Logical\.[A-Z]*//g' | \
    tr ',' '\n' | tr -d '{}[]"' | tr -d "'" | \
    sed 's/^[[:space:]]*//g' | sed 's/[[:space:]]*$//g' | \
    grep -v "^$" | sort -u >> "$BACKEND_CODES"

BACKEND_COUNT=$(wc -l < "$BACKEND_CODES" | tr -d ' ')
echo -e "   找到 ${GREEN}$BACKEND_COUNT${NC} 个后端权限码"

# 3. 提取 sys_menu 中定义的权限码
echo -e "${CYAN}[3/4] 提取数据库 sys_menu 权限码定义...${NC}"

# 从 SQL 初始化脚本提取
for sql_file in "$SCRIPTS_DIR"/init-db/*.sql; do
    if [ -f "$sql_file" ]; then
        grep -hoE "'[a-z]+:[a-z:_-]+'" "$sql_file" 2>/dev/null | \
            tr -d "'" >> "$MENU_CODES"
    fi
done

sort -u "$MENU_CODES" | grep -v "^$" > "${MENU_CODES}.tmp"
mv "${MENU_CODES}.tmp" "$MENU_CODES"

MENU_COUNT=$(wc -l < "$MENU_CODES" | tr -d ' ')
echo -e "   找到 ${GREEN}$MENU_COUNT${NC} 个菜单权限码定义"

# 4. 对比分析
echo ""
echo -e "${CYAN}[4/4] 权限码对比分析...${NC}"
echo ""

# 4.1 前端使用但后端未定义
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  ⚠️  前端使用但后端未定义的权限码（可能导致按钮不生效）${NC}"
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"

FRONTEND_ONLY=$(comm -23 "$FRONTEND_CODES" "$BACKEND_CODES")
FRONTEND_ONLY_COUNT=0

if [ -n "$FRONTEND_ONLY" ]; then
    while IFS= read -r code; do
        # 检查是否在 menu 中定义
        if grep -qF "$code" "$MENU_CODES" 2>/dev/null; then
            echo -e "  ${YELLOW}$code${NC} (在 sys_menu 中有定义，但 Controller 未使用)"
        else
            echo -e "  ${RED}$code${NC} (未在任何地方定义！)"
        fi
        FRONTEND_ONLY_COUNT=$((FRONTEND_ONLY_COUNT + 1))
    done <<< "$FRONTEND_ONLY"
else
    echo -e "  ${GREEN}无${NC}"
fi

echo ""

# 4.2 命名不一致检测
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  ⚠️  疑似命名不一致的权限码${NC}"
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"

declare -A NAMING_ISSUES

# 常见的命名不一致模式
check_naming() {
    local frontend_code=$1
    local backend_pattern=$2
    local description=$3
    
    if grep -qF "$frontend_code" "$FRONTEND_CODES" 2>/dev/null; then
        if ! grep -qF "$frontend_code" "$BACKEND_CODES" 2>/dev/null; then
            local similar=$(grep -E "$backend_pattern" "$BACKEND_CODES" 2>/dev/null | head -1)
            if [ -n "$similar" ]; then
                echo -e "  前端: ${RED}$frontend_code${NC}"
                echo -e "  后端: ${GREEN}$similar${NC}"
                echo -e "  建议: $description"
                echo ""
            fi
        fi
    fi
}

# 检查常见不一致
check_naming "user:create" "sys:user:create" "前端应改为 sys:user:create 或后端去掉 sys 前缀"
check_naming "user:edit" "sys:user:update" "前端应改为 sys:user:update"
check_naming "user:delete" "sys:user:delete" "前端应改为 sys:user:delete"
check_naming "user:reset-password" "sys:user" "检查后端是否有对应权限定义"
check_naming "client:edit" "client:update" "前端应改为 client:update"
check_naming "matter:edit" "matter:update" "前端应改为 matter:update"

# 4.3 后端定义但前端未使用（信息性）
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  ℹ️  后端定义但前端未使用的权限码（仅供参考）${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"

BACKEND_ONLY=$(comm -13 "$FRONTEND_CODES" "$BACKEND_CODES" | head -20)
BACKEND_ONLY_COUNT=$(comm -13 "$FRONTEND_CODES" "$BACKEND_CODES" | wc -l | tr -d ' ')

if [ -n "$BACKEND_ONLY" ]; then
    echo "$BACKEND_ONLY" | while read -r code; do
        echo -e "  ${BLUE}$code${NC}"
    done
    if [ "$BACKEND_ONLY_COUNT" -gt 20 ]; then
        echo -e "  ... 还有 $((BACKEND_ONLY_COUNT - 20)) 个 (这些可能是后端接口权限控制)"
    fi
else
    echo -e "  ${GREEN}无${NC}"
fi

echo ""

# 5. 总结
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}                      检查总结${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo ""
echo "  前端使用权限码数量: $FRONTEND_COUNT"
echo "  后端定义权限码数量: $BACKEND_COUNT"
echo "  菜单定义权限码数量: $MENU_COUNT"
echo ""

if [ "$FRONTEND_ONLY_COUNT" -gt 0 ]; then
    echo -e "  ${RED}⚠️  发现 $FRONTEND_ONLY_COUNT 个前端使用但后端未定义的权限码${NC}"
    echo -e "  ${YELLOW}    这些权限码可能导致按钮永远不显示或始终显示${NC}"
else
    echo -e "  ${GREEN}✅ 所有前端权限码都在后端有定义${NC}"
fi

echo ""

# 清理
rm -f "$FRONTEND_CODES" "$BACKEND_CODES" "$MENU_CODES"

echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  检查完成！请根据上述信息修复权限码不一致问题${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
echo ""
