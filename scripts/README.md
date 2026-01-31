# 脚本说明

本目录包含系统部署、运维、测试相关的自动化脚本。

## 📁 目录结构

```
scripts/
├── deploy.sh                    # 部署脚本入口（重定向到 deploy/）
├── update-version.sh            # 版本管理
├── sync-version.mjs             # 版本同步（构建时）
├── security-check.sh            # 安全检查
├── setup-github-ssh.sh          # GitHub SSH配置
│
├── deploy/                      # 📦 部署相关脚本
│   ├── deploy.sh                # 一键部署（推荐）
│   ├── deploy-swarm.sh          # Swarm 集群部署
│   ├── deploy-to-server.sh      # 快速部署到服务器
│   ├── force-update-server.sh   # 强制更新服务器代码
│   ├── pre-deploy-check.sh      # 部署前统一检查（推荐）
│   └── check-production-ready.sh # 生产环境检查
│
├── ops/                         # 🔧 运维相关脚本
│   ├── backup.sh                # 完整备份
│   ├── db-auto-backup.sh        # 数据库自动备份
│   ├── restore.sh               # 数据恢复
│   ├── reset-db.sh              # 数据库重置（开发用）
│   ├── init-demo-data.sh        # 初始化示例数据
│   ├── env-start.sh             # 环境启动
│   ├── env-stop.sh              # 环境停止
│   ├── env-reset.sh             # 环境重置
│   ├── clean-docker.sh          # 清理项目Docker资源（推荐）
│   └── clean-law-firm-only.sh   # 仅清理律所系统资源
│
├── ssl/                         # 🔐 SSL证书相关脚本
│   ├── download-ca-cert.sh     # 下载CA证书
│   ├── generate-server-cert.sh # 生成服务器证书
│   └── upload-ssl-certs.sh      # 上传SSL证书
│
├── test/                        # 🧪 测试脚本
│   ├── run-all-tests.sh         # 一键运行所有测试
│   ├── test-onlyoffice-minio-integration.sh # OnlyOffice集成测试
│   └── ...
│
├── jmeter/                      # 📊 压力测试
│   └── ...
│
├── init-db/                     # 🗄️ 数据库初始化脚本
│   └── ...
│
└── migration/                   # 🔄 数据库迁移脚本
    └── ...
```

## 🚀 快速开始

| 场景 | 命令 |
|------|------|
| **一键部署** | `./scripts/deploy.sh` |
| **更新版本号** | `./scripts/update-version.sh 3.2.3` |
| **重置数据库** | `./scripts/ops/reset-db.sh` |
| **备份数据** | `./scripts/ops/backup.sh` |

## 📦 部署脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `deploy.sh` | 一键 Docker 部署 | `./scripts/deploy.sh` |
| `deploy-swarm.sh` | Docker Swarm 集群部署 | `./scripts/deploy/deploy-swarm.sh init` |
| `deploy-to-server.sh` | 快速部署到服务器 | `./scripts/deploy/deploy-to-server.sh <IP> [用户]` |
| `force-update-server.sh` | 强制更新服务器代码 | `./scripts/deploy/force-update-server.sh [IP] [用户]` |

### deploy.sh

```bash
# 一键部署（自动构建并启动所有服务）
./scripts/deploy.sh

# 快速部署（非交互）
./scripts/deploy.sh --quick
```

### deploy-swarm.sh

```bash
# 初始化 Swarm 集群（Manager 节点）
./scripts/deploy/deploy-swarm.sh init

# 部署服务
./scripts/deploy/deploy-swarm.sh deploy

# 扩缩容
./scripts/deploy/deploy-swarm.sh scale backend 4
```

### deploy-to-server.sh

快速部署到服务器（使用 rsync 上传代码）。

```bash
# 部署到服务器
./scripts/deploy/deploy-to-server.sh 192.168.1.100 root
```

### force-update-server.sh

强制更新服务器代码（使用 git pull，会丢弃本地未提交的更改）。

```bash
# 方式1: 在服务器上直接运行
cd /opt/law-firm
./scripts/deploy/force-update-server.sh

# 方式2: 从本地SSH到服务器执行
./scripts/deploy/force-update-server.sh 192.168.1.100 root
```

**⚠️ 警告**: 此脚本会强制重置到远程分支，**丢弃所有本地未提交的更改**！

## 🔧 运维脚本

### 备份恢复

| 脚本 | 说明 | 用法 |
|------|------|------|
| `backup.sh` | 完整备份 | `./scripts/ops/backup.sh` |
| `db-auto-backup.sh` | 数据库自动备份 | `./scripts/ops/db-auto-backup.sh --schedule` |
| `restore.sh` | 数据恢复 | `./scripts/ops/restore.sh list` |

```bash
# 完整备份（数据库 + MinIO 文件）
./scripts/ops/backup.sh

# 仅备份数据库
./scripts/ops/backup.sh db

# 仅备份文件
./scripts/ops/backup.sh files

# 设置定时备份（每日凌晨 3 点）
./scripts/ops/db-auto-backup.sh --schedule

# 恢复数据库
./scripts/ops/restore.sh db backups/db/daily/2026-01-09_030000.sql.gz
```

### 数据库管理

| 脚本 | 说明 | 用法 |
|------|------|------|
| `reset-db.sh` | 重置数据库（开发用） | `./scripts/ops/reset-db.sh` |
| `init-demo-data.sh` | 初始化示例数据 | `./scripts/ops/init-demo-data.sh` |

```bash
# 重置数据库（会删除所有数据！）
./scripts/ops/reset-db.sh

# 初始化示例数据
./scripts/ops/init-demo-data.sh --docker --full
```

### 环境管理

| 脚本 | 说明 | 用法 |
|------|------|------|
| `env-start.sh` | 启动环境 | `./scripts/ops/env-start.sh` |
| `env-stop.sh` | 停止环境 | `./scripts/ops/env-stop.sh` |
| `env-reset.sh` | 重置环境 | `./scripts/ops/env-reset.sh` |

### Docker 清理

| 脚本 | 说明 | 用法 | 推荐场景 |
|------|------|------|----------|
| `clean-docker.sh` | 清理项目相关Docker资源 | `./scripts/ops/clean-docker.sh` | **日常开发清理（推荐）** |
| `clean-law-firm-only.sh` | 仅清理律所系统资源（保留frpc等） | `./scripts/ops/clean-law-firm-only.sh` | 服务器上保留其他服务 |

```bash
# 清理项目相关资源（推荐）
./scripts/ops/clean-docker.sh

# 仅清理律所系统资源（保留其他容器如frpc）
./scripts/ops/clean-law-firm-only.sh
```

## 🔐 SSL证书脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `download-ca-cert.sh` | 下载CA证书 | `./scripts/ssl/download-ca-cert.sh` |
| `generate-server-cert.sh` | 生成服务器证书 | `./scripts/ssl/generate-server-cert.sh` |
| `upload-ssl-certs.sh` | 上传SSL证书 | `./scripts/ssl/upload-ssl-certs.sh <IP>` |

## 🧪 测试脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `run-all-tests.sh` | 一键运行所有测试 | `./scripts/test/run-all-tests.sh` |
| `test-onlyoffice-minio-integration.sh` | OnlyOffice集成测试 | `./scripts/test/test-onlyoffice-minio-integration.sh` |
| `module-test-final.sh` | 模块功能测试 | `./scripts/test/module-test-final.sh` |
| `full-api-test.sh` | 完整API测试 | `./scripts/test/full-api-test.sh` |

```bash
# 运行所有测试
./scripts/test/run-all-tests.sh

# OnlyOffice和MinIO集成测试
./scripts/test/test-onlyoffice-minio-integration.sh

# 运行压力测试
./scripts/jmeter/run-all-tests.sh all
```

## 📊 检查脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `pre-deploy-check.sh` | 部署前统一检查（推荐） | `./scripts/deploy/pre-deploy-check.sh` |
| `check-production-ready.sh` | 生产环境检查 | `./scripts/deploy/check-production-ready.sh` |
| `security-check.sh` | 安全检查 | `./scripts/security-check.sh` |

```bash
# 部署前检查（推荐）
./scripts/deploy/pre-deploy-check.sh

# 安全检查
./scripts/security-check.sh
```

## 📝 版本管理脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `update-version.sh` | 手动更新版本号 | `./scripts/update-version.sh 3.2.3` |
| `sync-version.mjs` | 自动同步版本号 | 构建时自动执行 |

```bash
# 同时更新前后端版本号
./scripts/update-version.sh 3.2.3

# 手动测试版本同步
node scripts/sync-version.mjs
```

## 🗄️ 数据库初始化脚本

数据库初始化脚本位于 `init-db/` 目录，详见 [init-db/README.md](init-db/README.md)

| 脚本 | 说明 | 必需 |
|------|------|------|
| `00-extensions.sql` | PostgreSQL 扩展（pg_trgm, uuid-ossp） | ✅ |
| `01-19-*-schema.sql` | 19个模块表结构 | ✅ |
| `20-init-data.sql` | 系统初始化数据（整合版：菜单、角色、权限、配置、模板） | ✅ |
| `25-enhancement.sql` | 增强功能（整合版：version字段、权限细化、自动归档） | ✅ |
| `27-dict-init-data.sql` | 字典数据（50种字典类型） | ✅ |
| `30-demo-data-full.sql` | 完整演示数据（整合版：所有模块演示数据） | 可选 |
| `60-optimization.sql` | 性能优化（整合版：外键、索引、约束、触发器） | 推荐 |

---

**最后更新**: 2026-01-31
