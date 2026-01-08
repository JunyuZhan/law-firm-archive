# 数据库初始化脚本

整合日期：2026-01-05

## 脚本执行顺序

### 表结构脚本（01-15）
```
00-extensions.sql       # 数据库扩展
01-system-schema.sql    # 系统管理模块（用户、角色、菜单、日志等）
02-client-schema.sql    # 客户管理模块
03-matter-schema.sql    # 案件/项目管理模块
04-finance-schema.sql   # 财务管理模块（合同、收费、收款、提成、发票等）
05-document-schema.sql  # 文档管理模块
06-evidence-schema.sql  # 证据管理模块
07-archive-schema.sql   # 档案管理模块
08-timesheet-schema.sql # 工时管理模块
09-task-schema.sql      # 任务管理模块
10-admin-schema.sql     # 行政管理模块（出函、印章、会议室等）
11-asset-schema.sql     # 资产管理模块
12-knowledge-schema.sql # 知识库模块
13-hr-schema.sql        # 人事管理模块
14-quality-schema.sql   # 质量管理模块
15-workbench-schema.sql # 工作台模块
```

### 初始化数据脚本（20-29）
```
20-system-init-data.sql   # 系统初始化数据（部门、用户、角色、菜单）
21-template-init-data.sql # 模板初始化数据（出函模板、合同模板、提成规则）
```

## 执行方式

### 方式一：使用 reset-db.sh 脚本
```bash
./scripts/reset-db.sh
```

### 方式二：手动执行
```bash
# 连接数据库
docker exec -it law-postgres psql -U law_admin -d law_firm_dev

# 按顺序执行脚本
\i scripts/init-db/00-extensions.sql
\i scripts/init-db/01-system-schema.sql
# ... 依次执行其他脚本
```

## 备份说明

原始脚本已备份到 `scripts/backup/2026-01-05-before-consolidation/` 目录
