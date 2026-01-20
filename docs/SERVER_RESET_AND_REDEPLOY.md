# 服务器端：拉取代码、重置数据、重新部署完整指南

## 📋 概述

本指南介绍如何在服务器上：
1. 拉取最新代码
2. 重置所有数据持久化（清空数据库、Redis、MinIO等）
3. 重新部署系统

---

## 🔄 升级 vs 重置

**如果你只是想升级代码，保留数据**，请参考：
- 📖 [服务器端：升级代码（保留数据）完整指南](./SERVER_UPGRADE_GUIDE.md) - **推荐用于生产环境升级**

**如果你想重置所有数据，重新初始化**，请继续阅读本文档。

---

## ⚠️ 重要警告

**重置数据持久化会永久删除以下数据**：
- ✅ 数据库中的所有数据（PostgreSQL）
- ✅ Redis 缓存数据
- ✅ MinIO 文件存储（所有上传的文件）
- ✅ OnlyOffice 数据
- ✅ Prometheus 监控数据
- ✅ Grafana 配置和仪表板

**请确保在重置前已备份重要数据！**

---

## 🚀 快速操作（推荐）

### 方式一：使用脚本（最简单）

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 拉取最新代码
git pull origin main

# 3. 清理项目相关资源并重置数据（保留其他容器如 frpc）
./scripts/clean-law-firm-only.sh --force

# 4. 重新部署
./scripts/deploy.sh --quick
# 或使用短参数：
./scripts/deploy.sh -q
```

### 方式二：一行命令执行

```bash
cd /opt/law-firm && git pull origin main && ./scripts/clean-law-firm-only.sh --force && ./scripts/deploy.sh --quick
# 或使用短参数：
cd /opt/law-firm && git pull origin main && ./scripts/clean-law-firm-only.sh --force && ./scripts/deploy.sh -q
```

---

## 📝 详细步骤说明

### 步骤 1: 拉取最新代码

**⚠️ 如果仓库是私有仓库，请先配置 SSH 密钥或 Personal Access Token**

参考文档：[GitHub 私有仓库部署配置指南](./GITHUB_PRIVATE_REPO_SETUP.md)

```bash
# 进入项目目录（根据你的实际路径调整）
cd /opt/law-firm

# 拉取最新代码
git pull origin main
```

**如果遇到冲突或本地有未提交的更改**：

```bash
# 方式一：放弃本地更改，强制拉取（推荐）
git fetch origin
git reset --hard origin/main

# 方式二：暂存本地更改
git stash
git pull origin main
git stash pop
```

**验证代码已更新**：

```bash
git log -1 --oneline
```

---

### 步骤 2: 备份数据（可选但强烈推荐）

在重置前备份重要数据：

```bash
# 备份数据库
./scripts/backup.sh

# 或手动备份数据库
docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份 MinIO 文件（如果有重要文件）
# MinIO 文件存储在数据卷中，重置后无法恢复
```

---

### 步骤 3: 停止服务并清理数据

#### 选项 A: 仅清理项目相关资源（推荐，保留其他容器如 frpc）

```bash
# 使用脚本清理（推荐）
./scripts/clean-law-firm-only.sh --force

# 或手动清理
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml down -v --remove-orphans
```

**清理内容**：
- ✅ 停止并删除所有项目相关容器
- ✅ 删除所有项目相关数据卷（数据库、Redis、MinIO等）
- ✅ 删除项目相关网络
- ✅ 保留其他 Docker 资源（如 frpc 容器）

#### 选项 B: 清理所有 Docker 资源（⚠️ 会删除包括 frpc 在内的所有容器）

```bash
# 使用脚本清理
./scripts/clean-docker-all.sh --force

# 或手动清理
docker stop $(docker ps -aq) 2>/dev/null || true
docker rm -f $(docker ps -aq) 2>/dev/null || true
docker volume rm $(docker volume ls -q) 2>/dev/null || true
docker network prune -f
docker system prune -af --volumes
```

---

### 步骤 4: 重新部署

#### 方式一：使用一键部署脚本（推荐）

```bash
cd /opt/law-firm
./scripts/deploy.sh --quick
```

**部署脚本会自动**：
1. ✅ 检查 Docker 环境
2. ✅ 创建/验证 `.env` 文件
3. ✅ 自动生成安全密钥（如果首次部署）
4. ✅ 运行生产环境检查
5. ✅ 构建 Docker 镜像
6. ✅ 启动所有服务
7. ✅ 初始化数据库

#### 方式二：使用环境启动脚本

```bash
cd /opt/law-firm
./scripts/env-start.sh prod
```

#### 方式三：手动部署

```bash
cd /opt/law-firm/docker

# 构建镜像
docker compose --env-file ../.env -f docker-compose.prod.yml build

# 启动服务
docker compose --env-file ../.env -f docker-compose.prod.yml up -d

# 等待服务启动
sleep 10

# 初始化数据库（如果需要）
cd ..
./scripts/reset-db.sh --prod --force
```

---

### 步骤 5: 验证部署

```bash
# 1. 检查容器状态
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 应该看到所有容器状态为 "Up"

# 2. 检查健康状态
curl http://localhost/api/actuator/health

# 应该返回：{"status":"UP"}

# 3. 检查前端
curl http://localhost

# 4. 查看日志（检查是否有错误）
docker compose --env-file ../.env -f docker-compose.prod.yml logs --tail=50

# 5. 检查数据库连接
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT COUNT(*) FROM sys_user;"
```

---

## 🔄 完整操作流程（生产环境）

### 完整脚本（复制到服务器执行）

```bash
#!/bin/bash
set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}开始重置和重新部署${NC}"
echo -e "${YELLOW}========================================${NC}"

# 1. 进入项目目录
cd /opt/law-firm
echo -e "${GREEN}[1/4] 进入项目目录${NC}"

# 2. 拉取最新代码
echo -e "${GREEN}[2/4] 拉取最新代码...${NC}"
git fetch origin
git reset --hard origin/main
echo -e "${GREEN}✓ 代码已更新${NC}"

# 3. 清理项目资源
echo -e "${GREEN}[3/4] 清理项目资源...${NC}"
./scripts/clean-law-firm-only.sh --force
echo -e "${GREEN}✓ 资源已清理${NC}"

# 4. 重新部署
echo -e "${GREEN}[4/4] 重新部署...${NC}"
./scripts/deploy.sh --quick
echo -e "${GREEN}✓ 部署完成${NC}"

# 5. 等待服务启动
echo -e "${YELLOW}等待服务启动（30秒）...${NC}"
sleep 30

# 6. 验证部署
echo -e "${GREEN}[验证] 检查服务状态...${NC}"
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}重置和部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
```

**使用方法**：

```bash
# 保存为脚本
cat > /tmp/reset-redeploy.sh << 'EOF'
[上面的脚本内容]
EOF

# 添加执行权限
chmod +x /tmp/reset-redeploy.sh

# 执行
/tmp/reset-redeploy.sh
```

---

## 📊 数据持久化说明

### 会被删除的数据卷

重置操作会删除以下数据卷：

| 数据卷名称 | 内容 | 说明 |
|-----------|------|------|
| `postgres_data` | PostgreSQL 数据库 | 所有业务数据 |
| `redis_data` | Redis 缓存 | 会话和缓存数据 |
| `minio_data` | MinIO 文件存储 | 所有上传的文件 |
| `onlyoffice_data` | OnlyOffice 数据 | 文档服务数据 |
| `onlyoffice_logs` | OnlyOffice 日志 | 文档服务日志 |
| `onlyoffice_cache` | OnlyOffice 缓存 | 文档服务缓存 |
| `prometheus_data` | Prometheus 数据 | 监控指标数据 |
| `grafana_data` | Grafana 数据 | 仪表板和配置 |

### 不会被删除的内容

- ✅ `.env` 配置文件（会保留）
- ✅ 代码文件
- ✅ Docker 镜像（如果使用 `clean-law-firm-only.sh`）
- ✅ 其他项目的容器和数据（如果使用 `clean-law-firm-only.sh`）

---

## 🛠️ 故障排查

### 问题 1: Git 拉取失败

**错误**：`fatal: not a git repository`

**解决**：
```bash
# 检查是否在正确的目录
pwd
# 应该是 /opt/law-firm

# 检查 git 状态
git status
```

### 问题 2: 清理脚本权限不足

**错误**：`Permission denied`

**解决**：
```bash
# 添加执行权限
chmod +x scripts/clean-law-firm-only.sh
chmod +x scripts/deploy.sh

# 或使用 sudo（不推荐）
sudo ./scripts/clean-law-firm-only.sh --force
```

### 问题 3: 容器无法删除

**错误**：`Error response from daemon: cannot remove container`

**解决**：
```bash
# 强制停止并删除
docker stop $(docker ps -aq) 2>/dev/null || true
docker rm -f $(docker ps -aq) 2>/dev/null || true

# 然后重新执行清理脚本
./scripts/clean-law-firm-only.sh --force
```

### 问题 4: 数据卷无法删除

**错误**：`Error response from daemon: volume is in use`

**解决**：
```bash
# 先停止所有容器
docker stop $(docker ps -aq) 2>/dev/null || true

# 再删除数据卷
docker volume rm $(docker volume ls -q) 2>/dev/null || true

# 或使用强制清理
docker system prune -af --volumes
```

### 问题 5: 部署后服务无法启动

**检查步骤**：
```bash
# 1. 查看容器日志
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml logs

# 2. 检查容器状态
docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 3. 检查网络
docker network ls | grep law-firm

# 4. 检查数据卷
docker volume ls | grep -E "(postgres|redis|minio)"

# 5. 检查 .env 文件
cat ../.env | grep -v PASSWORD
```

---

## 📚 相关文档

- [服务器端：升级代码（保留数据）完整指南](./SERVER_UPGRADE_GUIDE.md) - **仅升级代码，保留数据**
- [完全清理和重新部署命令](./CLEANUP_AND_REDEPLOY.md)
- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
- [环境配置说明](./ENVIRONMENT_CONFIGURATION.md)

---

## ✅ 检查清单

重置和部署前确认：

- [ ] 已备份重要数据（数据库、文件等）
- [ ] 已确认要重置所有数据
- [ ] 已拉取最新代码
- [ ] 已停止所有服务
- [ ] 已清理所有数据卷
- [ ] 已重新部署服务
- [ ] 已验证服务正常运行
- [ ] 已检查日志无错误

---

## 🎯 快速参考命令

```bash
# 完整流程（一行命令）
cd /opt/law-firm && git pull origin main && ./scripts/clean-law-firm-only.sh --force && ./scripts/deploy.sh --quick
# 或使用短参数：
cd /opt/law-firm && git pull origin main && ./scripts/clean-law-firm-only.sh --force && ./scripts/deploy.sh -q

# 仅清理项目资源
./scripts/clean-law-firm-only.sh --force

# 仅重新部署
./scripts/deploy.sh --quick
# 或使用短参数：
./scripts/deploy.sh -q

# 查看服务状态
cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 查看日志
cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml logs -f
```
