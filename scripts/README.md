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
| `deploy-to-server.sh` | 快速部署到服务器 | `./deploy-to-server.sh <IP> [用户]` |
| `force-update-server.sh` | 强制更新服务器代码 | `./force-update-server.sh [IP] [用户]` |

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

### deploy-to-server.sh

快速部署到服务器（使用 rsync 上传代码）。

```bash
# 部署到服务器
./scripts/deploy-to-server.sh 192.168.1.100 root
```

### force-update-server.sh

强制更新服务器代码（使用 git pull，会丢弃本地未提交的更改）。

```bash
# 方式1: 在服务器上直接运行
cd /opt/law-firm
./scripts/force-update-server.sh

# 方式2: 从本地SSH到服务器执行
./scripts/force-update-server.sh 192.168.1.100 root

# 指定项目路径
./scripts/force-update-server.sh 192.168.1.100 root /opt/law-firm
```

**⚠️ 警告**: 此脚本会强制重置到远程分支，**丢弃所有本地未提交的更改**！

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

> **v2.0 整合版**: 脚本已从 46 个整合为 26 个核心脚本，详见 [init-db/README.md](init-db/README.md)

数据库初始化脚本，按顺序执行：

| 脚本 | 说明 | 必需 |
|------|------|------|
| `00-extensions.sql` | PostgreSQL 扩展（pg_trgm, uuid-ossp） | ✅ |
| `01-19-*-schema.sql` | 19个模块表结构 | ✅ |
| `20-init-data.sql` | 系统初始化数据（整合版：菜单、角色、权限、配置、模板） | ✅ |
| `25-enhancement.sql` | 增强功能（整合版：version字段、权限细化、自动归档） | ✅ |
| `27-dict-init-data.sql` | 字典数据（50种字典类型） | ✅ |
| `30-demo-data-full.sql` | 完整演示数据（整合版：所有模块演示数据） | 可选 |
| `60-optimization.sql` | 性能优化（整合版：外键、索引、约束、触发器） | 推荐 |

**快速执行**：

```bash
# 生产环境
for f in 00-extensions.sql 01-*.sql 02-*.sql ... 19-*.sql \
         20-init-data.sql 25-enhancement.sql 27-dict-init-data.sql 60-optimization.sql; do
    psql -U law_admin -d law_firm_dev -f "$f"
done

# 开发环境（包含演示数据）
# 在上述基础上添加 30-demo-data-full.sql
```

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
| `pre-deploy-check.sh` | 部署前统一检查（推荐） | `./pre-deploy-check.sh` |
| `check-production-ready.sh` | 生产环境检查（旧版） | `./check-production-ready.sh` |
| `security-check.sh` | 安全检查 | `./security-check.sh` |
| `test/run-all-tests.sh` | 一键运行所有测试 | `./test/run-all-tests.sh` |
| `test/module-test-final.sh` | 模块功能测试 | `./test/module-test-final.sh` |
| `test/full-api-test.sh` | 完整API测试 | `./test/full-api-test.sh` |
| `test/business-logic-test.sh` | 业务逻辑测试 | `./test/business-logic-test.sh` |
| `jmeter/run-all-tests.sh` | JMeter 压力测试 | `./jmeter/run-all-tests.sh` |

```bash
# 部署前检查（推荐）
./scripts/pre-deploy-check.sh

# 运行所有测试
./scripts/test/run-all-tests.sh

# 运行压力测试
./scripts/jmeter/run-all-tests.sh all
```

## Docker 清理脚本

| 脚本 | 说明 | 用法 | 推荐场景 |
|------|------|------|----------|
| `clean-docker.sh` | 清理项目相关Docker资源 | `./clean-docker.sh` | **日常开发清理（推荐）** |
| `clean-law-firm-only.sh` | 仅清理律所系统资源（保留frpc等） | `./clean-law-firm-only.sh` | 服务器上保留其他服务 |
| `clean-docker-all.sh` | 清理所有Docker资源 | `./clean-docker-all.sh` | 完全重置Docker环境 |

```bash
# 清理项目相关资源（推荐）
./scripts/clean-docker.sh

# 仅清理律所系统资源（保留其他容器如frpc）
./scripts/clean-law-firm-only.sh

# 清理所有Docker资源（危险！会删除所有容器和镜像）
./scripts/clean-docker-all.sh --force
```

## 目录结构

```
scripts/
├── deploy.sh                 # 一键部署（推荐）
├── deploy-swarm.sh           # Swarm 集群部署
├── deploy-to-server.sh        # 快速部署到服务器
├── pre-deploy-check.sh       # 部署前统一检查（推荐）
├── check-production-ready.sh # 生产环境检查（旧版）
├── security-check.sh         # 安全检查
├── update-version.sh         # 手动更新版本号
├── sync-version.mjs          # 自动同步版本号（构建时）
├── backup.sh                 # 完整备份
├── db-auto-backup.sh         # 数据库自动备份
├── restore.sh                # 数据恢复
├── reset-db.sh               # 数据库重置（开发用）
├── env-start.sh              # 环境启动
├── env-stop.sh               # 环境停止
├── env-reset.sh              # 环境重置
├── clean-docker.sh            # 清理项目Docker资源（推荐）
├── clean-law-firm-only.sh     # 仅清理律所系统资源（保留其他容器）
├── start-backend.sh          # 启动后端服务（宿主机）
├── install-java-maven.sh     # 安装Java和Maven（macOS）
├── setup-github-ssh.sh       # GitHub SSH配置
├── init-db/                  # 数据库初始化脚本（v2.0 整合版）
│   ├── README.md             # 详细说明文档
│   ├── init-database.sh.manual  # 手动初始化脚本
│   ├── 00-extensions.sql     # PostgreSQL 扩展
│   ├── 01-19-*-schema.sql    # 19个模块表结构
│   ├── 20-init-data.sql      # 系统初始化数据（整合版）
│   ├── 25-enhancement.sql    # 增强功能（整合版）
│   ├── 27-dict-init-data.sql # 字典数据
│   ├── 30-demo-data-full.sql # 完整演示数据（整合版）
│   ├── 60-optimization.sql   # 性能优化（整合版）
│   └── backup/               # 原有脚本备份
├── jmeter/                   # 压力测试
│   ├── run-all-tests.sh
│   └── *.jmx
├── test/                     # 测试脚本
│   ├── run-all-tests.sh      # 一键运行所有测试
│   ├── module-test-final.sh  # 模块功能测试
│   ├── full-api-test.sh      # 完整API测试
│   ├── business-logic-test.sh # 业务逻辑测试
│   └── *.sh                  # 其他专项测试脚本
├── migration/                # 版本迁移脚本
├── backup/                   # 备份文件目录
└── README.md                 # 本文件
```
