# 脚本说明

本目录包含系统部署、运维、测试相关的自动化脚本。

## 🚀 快速开始

| 场景 | 命令 |
|------|------|
| **一键部署** | `./scripts/deploy.sh` |
| **更新版本号** | `./scripts/update-version.sh 3.2.3` |
| **重置数据库** | `./scripts/reset-db.sh` |
| **备份数据** | `./scripts/backup.sh` |

## 部署脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `deploy.sh` | 一键 Docker 部署 | `./deploy.sh` |
| `deploy-swarm.sh` | Docker Swarm 集群部署 | `./deploy-swarm.sh init` |

### deploy.sh

```bash
# 一键部署（自动构建并启动所有服务）
./scripts/deploy.sh
```

### deploy-swarm.sh

```bash
# 初始化 Swarm 集群（Manager 节点）
./scripts/deploy-swarm.sh init

# 部署服务
./scripts/deploy-swarm.sh deploy

# 扩缩容
./scripts/deploy-swarm.sh scale backend 4
```

## 版本管理脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `update-version.sh` | 手动更新版本号 | `./update-version.sh 3.2.3` |
| `sync-version.mjs` | 自动同步版本号 | 构建时自动执行 |

### update-version.sh

```bash
# 同时更新前后端版本号
./scripts/update-version.sh 3.2.3
```

### sync-version.mjs

前端构建时自动执行，从 `backend/pom.xml` 读取版本号同步到 `frontend/apps/web-antd/package.json`。

```bash
# 手动测试
node scripts/sync-version.mjs
```

## 数据库脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `reset-db.sh` | 重置数据库（开发用） | `./reset-db.sh` |
| `init-db/*.sql` | 数据库初始化 SQL | 自动执行 |

### reset-db.sh

```bash
# 重置数据库（会删除所有数据！）
./scripts/reset-db.sh

# 强制重置（跳过确认）
./scripts/reset-db.sh --force
```

### init-db/ 目录

数据库初始化脚本，按顺序执行：

| 脚本 | 说明 |
|------|------|
| `00-extensions.sql` | PostgreSQL 扩展 |
| `01-system-schema.sql` | 系统表结构 |
| `02-19-*-schema.sql` | 业务表结构 |
| `20-system-init-data.sql` | 系统初始数据 |
| `28-29-*-permission.sql` | 权限配置 |
| `30-demo-data.sql` | 示例数据 |

## 备份恢复脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `backup.sh` | 完整备份 | `./backup.sh` |
| `db-auto-backup.sh` | 数据库自动备份 | `./db-auto-backup.sh --schedule` |
| `restore.sh` | 数据恢复 | `./restore.sh list` |

### backup.sh

```bash
# 完整备份（数据库 + MinIO 文件）
./scripts/backup.sh

# 仅备份数据库
./scripts/backup.sh db

# 仅备份文件
./scripts/backup.sh files
```

### db-auto-backup.sh

```bash
# 执行备份
./scripts/db-auto-backup.sh

# 设置定时任务（每日凌晨 3 点）
./scripts/db-auto-backup.sh --schedule

# 查看备份状态
./scripts/db-auto-backup.sh --status
```

### restore.sh

```bash
# 列出可用备份
./scripts/restore.sh list

# 恢复数据库
./scripts/restore.sh db backups/db/daily/2026-01-09_030000.sql.gz

# 完整恢复
./scripts/restore.sh full 2026-01-09
```

## 检查与测试脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `check-production-ready.sh` | 生产环境检查 | `./check-production-ready.sh` |
| `test/api-test.sh` | API 接口测试 | `./test/api-test.sh` |
| `jmeter/*.jmx` | JMeter 压力测试 | `./jmeter/run-all-tests.sh` |

```bash
# 检查系统是否可以部署到生产环境
./scripts/check-production-ready.sh

# 运行 API 测试
./scripts/test/api-test.sh

# 运行压力测试
./scripts/jmeter/run-all-tests.sh all
```

## 目录结构

```
scripts/
├── deploy.sh                 # 一键部署
├── deploy-swarm.sh           # Swarm 集群部署
├── update-version.sh         # 手动更新版本号
├── sync-version.mjs          # 自动同步版本号（构建时）
├── backup.sh                 # 完整备份
├── db-auto-backup.sh         # 数据库自动备份
├── restore.sh                # 数据恢复
├── reset-db.sh               # 数据库重置（开发用）
├── check-production-ready.sh # 生产环境检查
├── init-db/                  # 数据库初始化脚本
│   ├── init-database.sh.manual  # 手动初始化脚本
│   ├── 00-extensions.sql
│   ├── 01-19-*-schema.sql    # 表结构
│   ├── 20-27-*-data.sql      # 初始数据
│   ├── 28-29-*-permission.sql # 权限配置
│   └── 30-demo-data.sql      # 示例数据
├── jmeter/                   # 压力测试
│   ├── run-all-tests.sh
│   └── *.jmx
├── test/                     # API 测试
│   └── api-test.sh
├── migration/                # 版本迁移脚本
├── backup/                   # 备份文件目录
└── README.md                 # 本文件
```
