# 数据库初始化脚本

## 概述

本目录包含律师事务所管理系统的数据库初始化脚本，用于创建完整的数据库结构和初始数据。

## 脚本说明

### Schema 脚本（表结构）

| 脚本 | 模块 | 描述 |
|------|------|------|
| `00-extensions.sql` | 扩展 | PostgreSQL 扩展（pg_trgm, uuid-ossp） |
| `01-system-schema.sql` | 系统管理 | 用户、角色、菜单、权限、配置、字典、日志 |
| `02-client-schema.sql` | 客户管理 | 客户信息、线索、跟进记录、利冲检查 |
| `03-matter-schema.sql` | 项目管理 | 项目/案件、参与人、期限、客户关联 |
| `04-finance-schema.sql` | 财务管理 | 合同、收费、发票、支付、提成、费用 |
| `05-document-schema.sql` | 文档管理 | 文档、版本、分类、模板、卷宗目录 |
| `06-evidence-schema.sql` | 证据管理 | 证据、证据清单、质证记录 |
| `07-archive-schema.sql` | 档案管理 | 档案、存放位置、借阅记录、数据源 |
| `08-timesheet-schema.sql` | 工时管理 | 工时记录、计时器会话、工时汇总 |
| `09-task-schema.sql` | 任务管理 | 任务、任务评论 |
| `10-admin-schema.sql` | 行政管理 | 印章、会议室、外出、采购、资产 |
| `11-asset-schema.sql` | 资产盘点 | 资产盘点、盘点明细 |
| `12-knowledge-schema.sql` | 知识库 | 文章、案例、法规、收藏、学习笔记 |
| `13-hr-schema.sql` | 人力资源 | 员工、考勤、培训、薪酬、绩效、晋升 |
| `14-quality-schema.sql` | 质量管理 | 质量检查、检查明细、检查标准、风险预警 |
| `15-workbench-schema.sql` | 工作台 | 审批、日程、定时报表 |
| `16-contract-template-schema.sql` | 合同模板 | 合同模板、参与人模板、付款计划模板 |

### 初始化数据脚本

| 脚本 | 描述 |
|------|------|
| `20-system-init-data.sql` | 菜单、角色、权限、用户、配置、AI模型集成 |
| `21-template-init-data.sql` | 卷宗模板、文档模板、合同模板、职级、提成规则 |

### 补丁和增强脚本

| 脚本 | 描述 |
|------|------|
| `22-firm-config-patch.sql` | 律所配置补丁（可选，配置已在 20 中） |
| `23-system-config-complete.sql` | 系统配置补充（可选，配置已在 20 中） |
| `25-add-version-column.sql` | ⚠️ **必须执行** - 为所有表添加乐观锁 version 列 |
| `26-add-dict-menu.sql` | 字典管理菜单 |
| `27-dict-init-data.sql` | 字典初始化数据（50种字典类型，约200个字典项） |
| `28-report-permission-refine.sql` | 报表权限细化（财务报表、业务报表分离） |
| `29-approval-permission-refine.sql` | 审批权限细化（合同、用印、利冲、费用、结案） |

### 演示数据脚本

| 脚本 | 描述 |
|------|------|
| `30-demo-data.sql` | 示例数据（客户、合同、项目、任务） |

## 依赖关系图

```
00-extensions.sql
        │
        ▼
01-system-schema.sql ◄──────────────────────────────────────┐
        │                                                    │
        ▼                                                    │
02-client-schema.sql ◄── crm_client 外键依赖 sys_user ──────┤
        │                                                    │
        ▼                                                    │
03-matter-schema.sql ◄── matter_client 外键依赖 crm_client  │
        │                                                    │
        ▼                                                    │
04-finance-schema.sql ◄── 外键依赖 sys_user, crm_client, matter
        │
        ▼
05~15 schema 脚本 (相对独立)
        │
        ▼
16-contract-template-schema.sql ◄── 外键依赖 finance_contract, sys_user
        │
        ▼
═══════════════════════════════════════════════════════════
        │  所有表结构创建完成
        ▼
20-system-init-data.sql ◄── 依赖所有 schema 表
        │
        ▼
21-template-init-data.sql ◄── 依赖 dossier_template, doc_template 等
        │
        ▼
25-add-version-column.sql ◄── 为所有表添加 version 列
        │
        ▼
26~29 权限补丁脚本 ◄── 依赖 sys_menu, sys_role_menu
        │
        ▼
30-demo-data.sql ◄── 依赖 sys_user, crm_client, finance_contract, matter
```

## 使用方法

### 全新初始化（推荐）

使用提供的初始化脚本：

```bash
# 方式1: 本地 PostgreSQL
./init-database.sh.manual --drop

# 方式2: Docker 环境
./init-database.sh.manual --docker --drop
```

### 手动执行

```bash
# 创建数据库
createdb -U postgres law_firm_dev

# 按顺序执行脚本
for f in 00-extensions.sql \
         01-system-schema.sql 02-client-schema.sql 03-matter-schema.sql \
         04-finance-schema.sql 05-document-schema.sql 06-evidence-schema.sql \
         07-archive-schema.sql 08-timesheet-schema.sql 09-task-schema.sql \
         10-admin-schema.sql 11-asset-schema.sql 12-knowledge-schema.sql \
         13-hr-schema.sql 14-quality-schema.sql 15-workbench-schema.sql \
         16-contract-template-schema.sql \
         20-system-init-data.sql 21-template-init-data.sql \
         25-add-version-column.sql \
         26-add-dict-menu.sql 27-dict-init-data.sql \
         28-report-permission-refine.sql 29-approval-permission-refine.sql \
         30-demo-data.sql; do
    echo "执行: $f"
    psql -U law_admin -d law_firm_dev -f "$f"
done
```

### Docker 环境

```bash
# 进入容器执行
docker exec -it law-firm-postgres bash
cd /path/to/scripts/init-db
# 然后按上述顺序执行脚本

# 或者从宿主机执行
docker exec -i law-firm-postgres psql -U law_admin -d law_firm_dev < 00-extensions.sql
# ... 依次执行其他脚本
```

## 默认账号

初始化后可用的默认账号（密码统一为 `admin123`）：

| 用户名 | 真实姓名 | 角色 |
|--------|---------|------|
| admin | 系统管理员 | 管理员 |
| director | 律所主任 | 律所主任 |
| lawyer1 | 张律师 | 律师 |
| leader | 李团长 | 团队负责人 |
| finance | 王财务 | 财务人员 |
| staff | 赵行政 | 行政人员 |
| trainee | 陈实习 | 实习律师 |

## 演示数据说明

执行 `30-demo-data.sql` 后会创建：

- **客户**: 7个（5企业 + 2个人）
- **合同**: 6份
- **项目**: 6个（4诉讼 + 1非诉 + 1顾问）
- **任务**: 13个

## 常见问题

### Q: 执行脚本报错 "relation does not exist"

检查是否按正确顺序执行脚本。特别是：
- 04-finance-schema.sql 必须在 02, 03 之后
- 16-contract-template-schema.sql 必须在 04 之后
- 所有数据脚本必须在 schema 脚本之后

### Q: 应用启动报错 "column version does not exist"

确保执行了 `25-add-version-column.sql` 脚本，该脚本为所有业务表添加乐观锁所需的 version 列。

### Q: 权限检查不生效

确保执行了 `26-add-dict-menu.sql`、`28-report-permission-refine.sql`、`29-approval-permission-refine.sql` 等权限补丁脚本。

## 版本历史

- **1.0.0** (2026-01-08): 初始版本，基于开发数据库导出并整理
- **1.0.1** (2026-01-11): 添加字典管理、报表权限细化、审批权限细化、演示数据
