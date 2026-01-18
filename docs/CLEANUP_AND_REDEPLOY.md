# 完全清理和重新部署命令

## 📋 快速命令清单

### 1. 拉取最新代码

```bash
# 进入项目目录
cd /opt/law-firm

# 拉取最新代码
git pull origin main

# 或者如果本地有未提交的更改，强制拉取
git fetch origin
git reset --hard origin/main
```

### 2. 完全清理（清除所有容器、镜像、数据卷）

#### 方式一：仅清理项目相关资源（推荐，保留其他容器如 frpc）

```bash
# 仅清理律所系统相关资源，保留其他容器（如 frpc）
./scripts/clean-law-firm-only.sh

# 或者强制清理（跳过确认）
./scripts/clean-law-firm-only.sh --force
```

#### 方式二：清理所有 Docker 资源（⚠️ 会删除包括 frpc 在内的所有容器）

```bash
# 完全清理所有 Docker 资源（包括 frpc）
./scripts/clean-docker-all.sh

# 或者强制清理（跳过确认）
./scripts/clean-docker-all.sh --force
```

#### 方式二：手动清理命令

```bash
# 1. 停止并删除所有容器
docker stop $(docker ps -aq) 2>/dev/null || true
docker rm -f $(docker ps -aq) 2>/dev/null || true

# 2. 删除所有镜像
docker rmi -f $(docker images -q) 2>/dev/null || true

# 3. 删除所有数据卷（⚠️ 会删除所有持久化数据）
docker volume rm $(docker volume ls -q) 2>/dev/null || true

# 4. 删除所有自定义网络
docker network prune -f

# 5. 清理构建缓存和未使用的资源
docker builder prune -af
docker system prune -af --volumes
```

#### 方式三：仅清理项目相关资源

```bash
# 停止并删除项目相关容器和数据卷
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml down -v --remove-orphans

# 删除项目相关的镜像
docker images | grep law-firm | awk '{print $3}' | xargs docker rmi -f 2>/dev/null || true

# 删除项目相关的数据卷
docker volume ls | grep -E "(law-firm|postgres_data|redis_data|minio_data|onlyoffice)" | awk '{print $2}' | xargs docker volume rm 2>/dev/null || true
```

### 3. 重新部署

```bash
# 方式一：使用一键部署脚本（推荐）
cd /opt/law-firm
./scripts/deploy.sh --quick

# 方式二：使用环境启动脚本
cd /opt/law-firm
./scripts/env-start.sh prod

# 方式三：手动部署
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml build
docker compose --env-file ../.env -f docker-compose.prod.yml up -d
```

---

## 🔄 完整流程（一键执行）

### 完全清理并重新部署

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 拉取最新代码
git pull origin main

# 3. 完全清理
./scripts/clean-docker-all.sh --force

# 4. 重新部署
./scripts/deploy.sh --quick
```

### 或者使用一行命令

```bash
cd /opt/law-firm && git pull origin main && ./scripts/clean-docker-all.sh --force && ./scripts/deploy.sh --quick
```

---

## 📝 详细步骤说明

### 步骤 1: 拉取最新代码

```bash
cd /opt/law-firm
git pull origin main
```

**如果遇到冲突或本地有未提交的更改**：

```bash
# 方式一：放弃本地更改，强制拉取
git fetch origin
git reset --hard origin/main

# 方式二：暂存本地更改
git stash
git pull origin main
git stash pop
```

### 步骤 2: 完全清理

#### 选项 A: 清理所有 Docker 资源（最彻底）

```bash
./scripts/clean-docker-all.sh
# 输入 'DELETE ALL' 确认
```

**清理内容**：
- ✅ 所有容器（运行中和已停止）
- ✅ 所有镜像
- ✅ 所有数据卷（数据库、文件等）
- ✅ 所有自定义网络
- ✅ 所有构建缓存

#### 选项 B: 仅清理项目相关资源（保留其他 Docker 资源）

```bash
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml down -v --remove-orphans
```

**清理内容**：
- ✅ 项目相关容器
- ✅ 项目相关数据卷
- ✅ 项目相关网络

### 步骤 3: 重新部署

```bash
# 使用一键部署脚本（自动生成密钥、检查配置）
cd /opt/law-firm
./scripts/deploy.sh --quick
```

**部署过程**：
1. ✅ 检查 Docker 环境
2. ✅ 创建/验证 `.env` 文件
3. ✅ 自动生成安全密钥（如果首次部署）
4. ✅ 运行生产环境检查
5. ✅ 构建 Docker 镜像
6. ✅ 启动所有服务

---

## ⚠️ 注意事项

### 清理前确认

1. **数据备份**（如需要）：
   ```bash
   # 备份数据库
   ./scripts/backup.sh
   
   # 或手动备份
   docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup.sql
   ```

2. **确认清理范围**：
   - `clean-docker-all.sh` 会删除**所有** Docker 资源（包括其他项目）
   - 如果只想清理本项目，使用 `docker compose down -v`

### 部署后验证

```bash
# 1. 检查服务状态
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 2. 检查健康状态
curl http://localhost/api/actuator/health

# 3. 查看日志
docker compose --env-file ../.env -f docker-compose.prod.yml logs -f
```

---

## 🚀 快速参考

### 最常用命令组合

```bash
# 完全清理并重新部署（生产环境）
cd /opt/law-firm
git pull origin main
./scripts/clean-docker-all.sh --force
./scripts/deploy.sh --quick
```

### 仅清理项目相关资源

```bash
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml down -v --remove-orphans
cd ..
./scripts/deploy.sh --quick
```

### 查看当前状态

```bash
# 查看容器状态
docker ps -a

# 查看镜像
docker images

# 查看数据卷
docker volume ls

# 查看网络
docker network ls
```

---

## 📚 相关文档

- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
- [FRP 配置指南](./FRP_CONFIGURATION_GUIDE.md)
