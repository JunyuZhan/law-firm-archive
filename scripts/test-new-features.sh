#!/bin/bash

# 新功能业务逻辑验证脚本
# 测试：预收款管理、系统公告、OpenAPI管理、报表模板、定时报表、质量管理

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "新功能业务逻辑验证脚本"
echo "=========================================="

# 1. 登录获取 Token
echo -e "\n${YELLOW}[1/7] 登录获取 Token...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ 登录失败${NC}"
  echo "$LOGIN_RESPONSE"
  exit 1
fi
echo -e "${GREEN}✅ 登录成功，获取到 Token${NC}"

# 通用请求函数
api_get() {
  curl -s -X GET "$BASE_URL$1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"
}

api_post() {
  curl -s -X POST "$BASE_URL$1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$2"
}

# 2. 测试预收款管理
echo -e "\n${YELLOW}[2/7] 测试预收款管理...${NC}"
PREPAYMENT_LIST=$(api_get "/finance/prepayment/list?pageNum=1&pageSize=10")
if echo "$PREPAYMENT_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$PREPAYMENT_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 预收款列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 预收款列表查询失败${NC}"
  echo "$PREPAYMENT_LIST"
fi

# 3. 测试系统公告
echo -e "\n${YELLOW}[3/7] 测试系统公告...${NC}"
ANNOUNCEMENT_LIST=$(api_get "/system/announcement?pageNum=1&pageSize=10")
if echo "$ANNOUNCEMENT_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$ANNOUNCEMENT_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 系统公告列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 系统公告列表查询失败${NC}"
  echo "$ANNOUNCEMENT_LIST"
fi

# 4. 测试 OpenAPI 管理
echo -e "\n${YELLOW}[4/7] 测试 OpenAPI 管理...${NC}"
OPENAPI_LIST=$(api_get "/system/openapi/token?pageNum=1&pageSize=10")
if echo "$OPENAPI_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$OPENAPI_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ OpenAPI令牌列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ OpenAPI令牌列表查询失败${NC}"
  echo "$OPENAPI_LIST"
fi

# 5. 测试报表模板
echo -e "\n${YELLOW}[5/7] 测试报表模板...${NC}"
TEMPLATE_LIST=$(api_get "/workbench/report-template?pageNum=1&pageSize=10")
if echo "$TEMPLATE_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$TEMPLATE_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 报表模板列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 报表模板列表查询失败${NC}"
  echo "$TEMPLATE_LIST"
fi

# 6. 测试定时报表
echo -e "\n${YELLOW}[6/7] 测试定时报表...${NC}"
SCHEDULED_LIST=$(api_get "/workbench/scheduled-report?pageNum=1&pageSize=10")
if echo "$SCHEDULED_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$SCHEDULED_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 定时报表列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 定时报表列表查询失败${NC}"
  echo "$SCHEDULED_LIST"
fi

# 7. 测试质量管理
echo -e "\n${YELLOW}[7/7] 测试质量管理...${NC}"

# 7.1 质量检查标准列表
STANDARD_LIST=$(api_get "/knowledge/quality-standard?pageNum=1&pageSize=10")
if echo "$STANDARD_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$STANDARD_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 质量检查标准列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 质量检查标准列表查询失败${NC}"
  echo "$STANDARD_LIST"
fi

# 7.2 质量问题列表
ISSUE_LIST=$(api_get "/knowledge/quality-issue?pageNum=1&pageSize=10")
if echo "$ISSUE_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$ISSUE_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 质量问题列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 质量问题列表查询失败${NC}"
  echo "$ISSUE_LIST"
fi

# 7.3 风险预警列表（活跃预警）
WARNING_LIST=$(api_get "/knowledge/risk-warning/active")
if echo "$WARNING_LIST" | grep -q '"code":"200"'; then
  TOTAL=$(echo "$WARNING_LIST" | grep -o '"total":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✅ 风险预警列表查询成功，共 ${TOTAL:-0} 条记录${NC}"
else
  echo -e "${RED}❌ 风险预警列表查询失败${NC}"
  echo "$WARNING_LIST"
fi

# 8. 测试菜单权限
echo -e "\n${YELLOW}[额外] 验证菜单配置...${NC}"
MENU_CHECK=$(docker exec law-firm-postgres psql -U law_admin -d law_firm_dev -t -c "
SELECT COUNT(*) FROM sys_menu WHERE id IN (760,765,770,775,780,785);
")
MENU_COUNT=$(echo $MENU_CHECK | tr -d ' ')
if [ "$MENU_COUNT" -eq "6" ]; then
  echo -e "${GREEN}✅ 6个新功能菜单配置正确${NC}"
else
  echo -e "${RED}❌ 菜单配置不完整，只有 $MENU_COUNT 个${NC}"
fi

# 权限检查
ROLE_MENU_CHECK=$(docker exec law-firm-postgres psql -U law_admin -d law_firm_dev -t -c "
SELECT COUNT(*) FROM sys_role_menu WHERE menu_id IN (760,765,770,775,780,785);
")
ROLE_MENU_COUNT=$(echo $ROLE_MENU_CHECK | tr -d ' ')
echo -e "${GREEN}✅ 角色菜单权限配置：$ROLE_MENU_COUNT 条记录${NC}"

echo -e "\n=========================================="
echo -e "${GREEN}验证完成！${NC}"
echo "=========================================="
