# 数据库初始化脚本

## 概述

本目录包含律师事务所管理系统的数据库初始化脚本，用于创建完整的数据库结构和初始数据。

> **v2.0 整合版**: 脚本已整合优化，从 46 个减少到 **26 个核心脚本**，同时保留原有脚本作为备份。

## 快速开始

```bash
# 全新初始化（推荐）
./init-database.sh.manual --drop

# 或手动执行核心脚本
for f in 00-extensions.sql 01-19-*.sql 20-init-data.sql 25-enhancement.sql \
         27-dict-init-data.sql 30-demo-data-full.sql 60-optimization.sql; do
    psql -U law_admin -d law_firm_dev -f "$f"
done
```

## 核心脚本说明

### Schema 脚本（00-19）

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
| `17-openapi-schema.sql` | 开放API | 客户文件上传、开放API集成 |
| `18-cause-of-action-schema.sql` | 案由管理 | 案由分类、罪名、起诉理由 |
| `19-system-integration-schema.sql` | 系统集成 | 节假日缓存、企业微信绑定 |

### 初始化脚本（20-29）

| 脚本 | 描述 | 必需 |
|------|------|------|
| `20-init-data.sql` | **整合版**：菜单、角色、权限、用户、配置、模板 | ✅ 必须 |
| `25-enhancement.sql` | **整合版**：version字段、权限细化、自动归档 | ✅ 必须 |
| `27-dict-init-data.sql` | 字典初始化数据（50种字典类型） | ✅ 必须 |

> **注意**: 22, 23 为冗余脚本，配置已包含在 20 中，无需执行。

### 演示数据脚本（30-39）

| 脚本 | 描述 | 必需 |
|------|------|------|
| `30-demo-data-full.sql` | **整合版**：所有模块演示数据 | 可选 |

> 包含：客户、合同、项目、任务、知识库、日程、人力资源、行政管理、财务等完整演示数据。

### 性能优化脚本（60-69）

| 脚本 | 描述 | 必需 |
|------|------|------|
| `60-optimization.sql` | **整合版**：外键、索引、约束、分区、全文搜索、物化视图、触发器 | 推荐 |

> 优化内容：
> - **P0**: 外键约束、部分索引
> - **P1**: 复合索引、检查约束、日志分区
> - **P2**: 全文搜索、物化视图、触发器

## 依赖关系图

```
00-extensions.sql
        │
        ▼
01-19 schema 脚本 (按模块顺序)
        │
        ▼
═══════════════════════════════════════════════════════════
        │  所有表结构创建完成
        ▼
20-init-data.sql ◄── 系统配置、菜单、角色、模板
        │
        ▼
25-enhancement.sql ◄── version字段、权限细化、功能增强
        │
        ▼
27-dict-init-data.sql ◄── 字典数据
        │
        ▼
30-demo-data-full.sql ◄── 演示数据（可选）
        │
        ▼
60-optimization.sql ◄── 性能优化（可选）
```

## 整合说明

### v2.0 整合变更

| 操作 | 原脚本 | 新脚本 |
|------|--------|--------|
| **合并** | 20 + 21 + 33 | 20-init-data.sql |
| **合并** | 25 + 26 + 28 + 29 + 31 | 25-enhancement.sql |
| **合并** | 30 + 40-44 + 99 | 30-demo-data-full.sql |
| **合并** | 60-67 | 60-optimization.sql |
| **重命名** | 32 | 17-openapi-schema.sql |
| **重命名** | 50 | 18-cause-of-action-schema.sql |
| **重命名** | 34 | 19-system-integration-schema.sql |
| **保留** | 22, 23 | 冗余备份（配置已在20中） |

### 原有脚本位置

原有的细分脚本保留在目录中作为备份，可以继续单独使用：

- `60-optimize-*.sql` (60-67) → 已合并到 `60-optimization.sql`
- `30-demo-data.sql`, `40-44-demo-*.sql` → 已合并到 `30-demo-data-full.sql`

## 使用方法

### 全新初始化（推荐）

```bash
# 方式1: 本地 PostgreSQL
./init-database.sh.manual --drop

# 方式2: Docker 环境
./init-database.sh.manual --docker --drop
```

### 手动执行（精简版）

```bash
# 创建数据库
createdb -U postgres law_firm_dev

# 执行核心脚本（按顺序）
psql -U law_admin -d law_firm_dev -f 00-extensions.sql

# 执行所有 Schema 脚本
for f in 01-*.sql 02-*.sql 03-*.sql 04-*.sql 05-*.sql 06-*.sql \
         07-*.sql 08-*.sql 09-*.sql 10-*.sql 11-*.sql 12-*.sql \
         13-*.sql 14-*.sql 15-*.sql 16-*.sql 17-*.sql 18-*.sql 19-*.sql; do
    psql -U law_admin -d law_firm_dev -f "$f"
done

# 初始化数据
psql -U law_admin -d law_firm_dev -f 20-init-data.sql
psql -U law_admin -d law_firm_dev -f 25-enhancement.sql
psql -U law_admin -d law_firm_dev -f 27-dict-init-data.sql

# 演示数据（可选）
psql -U law_admin -d law_firm_dev -f 30-demo-data-full.sql

# 性能优化（推荐）
psql -U law_admin -d law_firm_dev -f 60-optimization.sql
```

### Docker 环境

```bash
# 进入容器执行
docker exec -it law-firm-postgres bash
cd /docker-entrypoint-initdb.d
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

### 完整演示数据（30-demo-data-full.sql）

| 模块 | 数据量 | 说明 |
|------|--------|------|
| 客户 | 14个 | 10企业 + 4个人 |
| 合同 | 12份 | 各类型合同 |
| 项目 | 12个 | 8诉讼 + 2非诉 + 2顾问 |
| 任务 | 26个 | 各状态任务 |
| 归档 | 6个 | 已结案归档 |
| 知识库 | 分类+文章 | 8分类 + 10篇文章 |
| 日程 | 15条 | 庭审、会议、培训 |
| 考勤 | 20条 | 出勤记录 |
| 行政 | 会议室+印章 | 各5个 + 6个用印申请 |
| 财务 | 收付发票 | 各10条左右 |

## 常见问题

### Q: 执行脚本报错 "relation does not exist"

检查是否按正确顺序执行脚本。特别是：
- 所有 schema 脚本（00-19）必须先执行
- 数据脚本（20-30）依赖 schema 脚本
- 优化脚本（60）在数据脚本之后执行

### Q: 应用启动报错 "column version does not exist"

确保执行了 `25-enhancement.sql` 脚本，该脚本包含 version 列的创建。

### Q: 权限检查不生效

确保执行了 `25-enhancement.sql`，其中包含权限细化相关内容。

### Q: 是否需要执行优化脚本（60）？

**强烈推荐生产环境执行**，包含：
- **P0**: 外键约束（数据完整性）、部分索引（软删除优化）
- **P1**: 复合索引（查询性能）、检查约束（数据验证）、日志分区（大数据量）
- **P2**: 全文搜索（中文搜索）、物化视图（统计加速）、触发器（自动维护）

### Q: 如何只执行部分优化？

使用原有的细分脚本（60-67）：
- `60-62`: P0+P1 优化（推荐）
- `63-67`: P1+P2 优化（可选）

## 版本历史

- **1.0.0** (2026-01-08): 初始版本
- **1.0.1** (2026-01-11): 添加字典管理、权限细化、演示数据
- **1.0.2** (2026-01-12): 合并迁移脚本
- **1.1.0** (2026-01-15): 添加数据库优化脚本（60-67）
- **2.0.0** (2026-01-15): **脚本整合优化**
  - 合并初始化脚本：20-init-data.sql, 25-enhancement.sql
  - 合并演示数据：30-demo-data-full.sql
  - 合并优化脚本：60-optimization.sql
  - 重排 schema 脚本：17-19
  - 脚本数量：46 → 26（减少 43%）
