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
| `20-system-init-data.sql` | 菜单、角色、权限、字典、系统配置(62项)、AI模型集成 |
| `21-template-init-data.sql` | 卷宗模板、文档模板、合同模板、职级、提成规则 |
| `22-firm-config-patch.sql` | 律所配置补丁脚本（用于现有数据库补充律所配置） |
| `23-system-config-complete.sql` | 系统配置完整补丁脚本（用于现有数据库补充所有配置项） |

## 使用方法

### 全新初始化

按顺序执行所有脚本：

```bash
# 创建数据库
createdb -U postgres law_firm_dev

# 执行初始化脚本
for f in 00-extensions.sql \
         01-system-schema.sql 02-client-schema.sql 03-matter-schema.sql \
         04-finance-schema.sql 05-document-schema.sql 06-evidence-schema.sql \
         07-archive-schema.sql 08-timesheet-schema.sql 09-task-schema.sql \
         10-admin-schema.sql 11-asset-schema.sql 12-knowledge-schema.sql \
         13-hr-schema.sql 14-quality-schema.sql 15-workbench-schema.sql \
         16-contract-template-schema.sql \
         20-system-init-data.sql 21-template-init-data.sql; do
    psql -U law_admin -d law_firm_dev -f "$f"
done
```

或者使用提供的初始化脚本：

```bash
./init-database.sh
```

### Docker 环境

```bash
docker exec -i law-firm-postgres psql -U law_admin -d law_firm_dev < 00-extensions.sql
# ... 依次执行其他脚本
```

## 默认账号

初始化后可用的默认账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| director | lawyer123 | 律所主任 |
| lawyer1 | lawyer123 | 律师 |

## 版本历史

- **1.0.0** (2026-01-08): 初始版本，基于开发数据库导出并整理
