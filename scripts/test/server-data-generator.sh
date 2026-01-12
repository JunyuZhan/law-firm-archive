#!/bin/bash

# =====================================================
# 测试服务器数据生成脚本
# =====================================================
# 用途：在测试/生产服务器上通过 API 生成测试数据
# 用法：./server-data-generator.sh [服务器地址]
# 示例：./server-data-generator.sh https://law.example.com
# =====================================================

# 服务器地址（默认本机）
SERVER_URL="${1:-http://localhost}"
BASE_URL="$SERVER_URL/api"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Token
TOKEN=""

# 统计
CREATED=0
FAILED=0

echo "=============================================="
echo "  测试服务器数据生成脚本"
echo "=============================================="
echo "服务器地址: $SERVER_URL"
echo ""

# 登录获取 Token
login() {
    echo -e "${BLUE}[1/8] 登录系统...${NC}"
    
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    local success=$(echo "$response" | grep -o '"success":true')
    
    if [ -n "$success" ]; then
        # 使用 grep 和 sed 提取 token（兼容不同格式）
        TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | sed 's/"token":"\([^"]*\)"/\1/')
        
        if [ -z "$TOKEN" ]; then
            # 尝试另一种格式：accessToken
            TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"\([^"]*\)"/\1/')
        fi
        
        if [ -n "$TOKEN" ]; then
            echo -e "${GREEN}✓${NC} 登录成功"
            echo -e "  Token: ${TOKEN:0:50}..."
            return 0
        else
            echo -e "${RED}✗${NC} 无法提取 Token"
            echo -e "  响应: $response"
            exit 1
        fi
    else
        echo -e "${RED}✗${NC} 登录失败: $response"
        exit 1
    fi
}

# API 请求函数
api_post() {
    local endpoint=$1
    local data=$2
    local name=$3
    
    local response=$(curl -s -X POST "$BASE_URL$endpoint" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$data")
    
    local success=$(echo "$response" | grep -o '"success":true')
    
    if [ -n "$success" ]; then
        echo -e "  ${GREEN}✓${NC} $name"
        CREATED=$((CREATED + 1))
        return 0
    else
        local msg=$(echo "$response" | grep -o '"message":"[^"]*"' | head -1)
        echo -e "  ${RED}✗${NC} $name - $msg"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

# 创建客户数据
create_clients() {
    echo ""
    echo -e "${BLUE}[2/8] 创建客户数据...${NC}"
    
    # 企业客户 (API路径: POST /client)
    api_post "/client" '{
        "name": "测试科技有限公司",
        "clientType": "ENTERPRISE",
        "creditCode": "91110108MA99TEST01",
        "legalRepresentative": "测试法人",
        "registeredAddress": "北京市测试区测试路1号",
        "contactPerson": "联系人A",
        "contactPhone": "13900001111",
        "contactEmail": "test1@example.com",
        "industry": "信息技术",
        "source": "API测试",
        "level": "A",
        "category": "VIP"
    }' "企业客户-测试科技有限公司"
    
    api_post "/client" '{
        "name": "演示贸易有限公司",
        "clientType": "ENTERPRISE",
        "creditCode": "91110108MA99TEST02",
        "legalRepresentative": "演示法人",
        "registeredAddress": "上海市演示区演示路2号",
        "contactPerson": "联系人B",
        "contactPhone": "13900002222",
        "contactEmail": "test2@example.com",
        "industry": "贸易",
        "source": "API测试",
        "level": "B",
        "category": "NORMAL"
    }' "企业客户-演示贸易有限公司"
    
    # 个人客户
    api_post "/client" '{
        "name": "测试个人客户",
        "clientType": "INDIVIDUAL",
        "idCard": "110101199001011234",
        "contactPerson": "测试个人客户",
        "contactPhone": "13900003333",
        "contactEmail": "individual@example.com",
        "source": "API测试",
        "level": "B"
    }' "个人客户-测试个人客户"
}

# 创建合同数据 (API路径: POST /matter/contract)
create_contracts() {
    echo ""
    echo -e "${BLUE}[3/8] 创建合同数据...${NC}"
    
    api_post "/matter/contract" '{
        "name": "API测试服务合同",
        "clientId": 101,
        "contractType": "SERVICE",
        "feeType": "FIXED",
        "totalAmount": 50000,
        "currency": "CNY",
        "signDate": "2026-01-12",
        "effectiveDate": "2026-01-12",
        "expiryDate": "2026-12-31",
        "content": "API测试创建的服务合同",
        "remark": "通过脚本自动创建"
    }' "服务合同-API测试"
    
    api_post "/matter/contract" '{
        "name": "API测试诉讼合同",
        "clientId": 102,
        "contractType": "LITIGATION",
        "feeType": "CONTINGENCY",
        "totalAmount": 100000,
        "currency": "CNY",
        "signDate": "2026-01-12",
        "effectiveDate": "2026-01-12",
        "expiryDate": "2026-06-30",
        "content": "API测试创建的诉讼代理合同",
        "remark": "风险代理"
    }' "诉讼合同-API测试"
}

# 创建项目数据 (API路径: POST /matter)
# 注意：创建项目需要关联已审批的合同，这里跳过
# 使用演示数据中已存在的项目（ID: 101-106）
create_matters() {
    echo ""
    echo -e "${BLUE}[4/8] 项目数据...${NC}"
    echo -e "  ${YELLOW}⊘${NC} 跳过 - 创建项目需要关联已审批合同"
    echo -e "  ${YELLOW}⊘${NC} 使用演示数据中已存在的6个项目"
}

# 创建任务数据 (API路径: POST /tasks)
create_tasks() {
    echo ""
    echo -e "${BLUE}[5/8] 创建任务数据...${NC}"
    
    api_post "/tasks" '{
        "title": "API测试任务-合同审查",
        "description": "通过API创建的测试任务",
        "matterId": 101,
        "priority": "HIGH",
        "dueDate": "2026-01-20"
    }' "任务-合同审查"
    
    api_post "/tasks" '{
        "title": "API测试任务-文件整理",
        "description": "整理项目相关文件",
        "matterId": 101,
        "priority": "MEDIUM",
        "dueDate": "2026-01-25"
    }' "任务-文件整理"
    
    api_post "/tasks" '{
        "title": "API测试任务-起诉状起草",
        "description": "起草民事起诉状",
        "matterId": 102,
        "priority": "HIGH",
        "dueDate": "2026-01-30"
    }' "任务-起诉状起草"
}

# 创建工时记录 (API路径: POST /timesheets)
create_timesheets() {
    echo ""
    echo -e "${BLUE}[6/8] 创建工时记录...${NC}"
    
    api_post "/timesheets" '{
        "matterId": 101,
        "workDate": "2026-01-10",
        "hours": 2.5,
        "workContent": "合同审查工作",
        "workType": "CONTRACT_REVIEW"
    }' "工时-合同审查"
    
    api_post "/timesheets" '{
        "matterId": 101,
        "workDate": "2026-01-11",
        "hours": 1.5,
        "workContent": "客户电话沟通",
        "workType": "CONSULTATION"
    }' "工时-客户沟通"
    
    api_post "/timesheets" '{
        "matterId": 102,
        "workDate": "2026-01-12",
        "hours": 4.0,
        "workContent": "案件分析研究",
        "workType": "RESEARCH"
    }' "工时-案件研究"
}

# 创建收款记录 (API路径: POST /finance/fee)
create_fees() {
    echo ""
    echo -e "${BLUE}[7/8] 创建收款记录...${NC}"
    
    api_post "/finance/fee" '{
        "name": "首期服务费",
        "clientId": 101,
        "contractId": 101,
        "matterId": 101,
        "feeType": "SERVICE_FEE",
        "amount": 10000,
        "description": "首期服务费",
        "dueDate": "2026-01-15"
    }' "收款-首期服务费"
    
    api_post "/finance/fee" '{
        "name": "第二期服务费",
        "clientId": 101,
        "contractId": 101,
        "matterId": 101,
        "feeType": "SERVICE_FEE",
        "amount": 10000,
        "description": "第二期服务费",
        "dueDate": "2026-04-15"
    }' "收款-第二期服务费"
}

# 查询各模块数据统计
show_statistics() {
    echo ""
    echo -e "${BLUE}[8/8] 数据统计...${NC}"
    
    # 客户统计 (GET /client/list)
    local clients=$(curl -s -X GET "$BASE_URL/client/list?pageNum=1&pageSize=1" \
        -H "Authorization: Bearer $TOKEN" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "  客户数量: ${clients:-0}"
    
    # 合同统计 (GET /matter/contract/list)
    local contracts=$(curl -s -X GET "$BASE_URL/matter/contract/list?pageNum=1&pageSize=1" \
        -H "Authorization: Bearer $TOKEN" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "  合同数量: ${contracts:-0}"
    
    # 项目统计 (GET /matter/list)
    local matters=$(curl -s -X GET "$BASE_URL/matter/list?pageNum=1&pageSize=1" \
        -H "Authorization: Bearer $TOKEN" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "  项目数量: ${matters:-0}"
    
    # 任务统计 (GET /tasks)
    local tasks=$(curl -s -X GET "$BASE_URL/tasks?pageNum=1&pageSize=1" \
        -H "Authorization: Bearer $TOKEN" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "  任务数量: ${tasks:-0}"
}

# 主流程
main() {
    login
    create_clients
    create_contracts
    create_matters
    create_tasks
    create_timesheets
    create_fees
    show_statistics
    
    echo ""
    echo "=============================================="
    echo -e "  ${GREEN}数据生成完成！${NC}"
    echo "=============================================="
    echo -e "  创建成功: ${GREEN}$CREATED${NC}"
    echo -e "  创建失败: ${RED}$FAILED${NC}"
    echo "=============================================="
}

main
