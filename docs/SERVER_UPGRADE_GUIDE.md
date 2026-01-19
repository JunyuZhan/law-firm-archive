# 服务器端：升级代码（保留数据）完整指南

## 📋 概述

本指南介绍如何在服务器上**仅升级代码**，**保留所有数据**：
- ✅ 更新代码和 Docker 镜像
- ✅ 保留数据库数据
- ✅ 保留文件存储（MinIO）
- ✅ 保留 Redis 缓存
- ✅ 保留监控数据
- ✅ 自动执行数据库迁移（如有）

---

## 🚀 快速操作（推荐）

### 方式一：使用脚本（最简单）

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 拉取最新代码
git pull origin main

# 3. 升级部署（保留数据）
./scripts/deploy.sh --quick
# 或使用短参数：
./scripts/deploy.sh -q
```

### 方式二：一行命令执行

```bash
cd /opt/law-firm && git pull origin main && ./scripts/deploy.sh --quick
# 或使用短参数：
cd /opt/law-firm && git pull origin main && ./scripts/deploy.sh -q
```

---

## 📝 详细步骤说明

### 步骤 1: 拉取最新代码

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

### 步骤 2: 备份数据（可选但推荐）

虽然升级不会删除数据，但建议在升级前备份：

```bash
# 备份数据库
./scripts/backup.sh

# 或手动备份数据库
docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup_$(date +%Y%m%d_%H%M%S).sql
```

---

### 步骤 3: 升级部署（保留数据）

#### 方式一：使用一键部署脚本（推荐）

```bash
cd /opt/law-firm
./scripts/deploy.sh --quick
# 或使用短参数：
./scripts/deploy.sh -q
```

**部署脚本会自动**：
1. ✅ 检查 Docker 环境
2. ✅ 创建/验证 `.env` 文件
3. ✅ 停止现有服务（**不删除数据卷**）
4. ✅ 重新构建 Docker 镜像
5. ✅ 启动所有服务
6. ✅ 自动执行数据库迁移（如有）

#### 方式二：手动升级

```bash
cd /opt/law-firm/docker

# 1. 停止服务（不删除数据卷，使用 down 而不是 down -v）
docker compose --env-file ../.env -f docker-compose.prod.yml down

# 2. 重新构建镜像
docker compose --env-file ../.env -f docker-compose.prod.yml build

# 3. 启动服务
docker compose --env-file ../.env -f docker-compose.prod.yml up -d

# 4. 等待服务启动
sleep 10

# 5. 检查服务状态
docker compose --env-file ../.env -f docker-compose.prod.yml ps
```

---

### 步骤 4: 执行数据库迁移（如有）

如果新版本包含数据库结构变更，需要执行迁移：

#### 方式一：通过系统界面执行（推荐）

1. 登录系统管理界面
2. 进入 **系统管理** → **数据库迁移**
3. 查看待执行的迁移脚本
4. 执行迁移（需要确认码）

#### 方式二：手动执行迁移脚本

```bash
# 查看迁移脚本
ls -la scripts/migration/

# 手动执行迁移（需要数据库连接信息）
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < scripts/migration/xxx.sql
```

---

### 步骤 5: 验证升级

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

# 5. 检查数据库连接和数据
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT COUNT(*) FROM sys_user;"

# 6. 检查版本号（如果应用有版本接口）
curl http://localhost/api/actuator/info
```

---

## 🔄 升级 vs 重置的区别

| 操作 | 升级（本指南） | 重置（SERVER_RESET_AND_REDEPLOY.md） |
|------|---------------|-------------------------------------|
| **代码更新** | ✅ 更新 | ✅ 更新 |
| **镜像重建** | ✅ 重建 | ✅ 重建 |
| **数据库数据** | ✅ **保留** | ❌ 删除 |
| **文件存储** | ✅ **保留** | ❌ 删除 |
| **Redis 缓存** | ✅ **保留** | ❌ 删除 |
| **监控数据** | ✅ **保留** | ❌ 删除 |
| **数据库迁移** | ✅ 自动执行 | ✅ 重新初始化 |
| **适用场景** | 生产环境升级 | 开发测试重置 |

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
echo -e "${YELLOW}开始升级（保留数据）${NC}"
echo -e "${YELLOW}========================================${NC}"

# 1. 进入项目目录
cd /opt/law-firm
echo -e "${GREEN}[1/4] 进入项目目录${NC}"

# 2. 备份数据（可选）
echo -e "${GREEN}[2/4] 备份数据...${NC}"
if [ -f "./scripts/backup.sh" ]; then
    ./scripts/backup.sh || echo -e "${YELLOW}⚠️  备份失败，继续升级...${NC}"
else
    echo -e "${YELLOW}⚠️  备份脚本不存在，跳过备份${NC}"
fi

# 3. 拉取最新代码
echo -e "${GREEN}[3/4] 拉取最新代码...${NC}"
git fetch origin
git reset --hard origin/main
echo -e "${GREEN}✓ 代码已更新${NC}"

# 4. 升级部署
echo -e "${GREEN}[4/4] 升级部署（保留数据）...${NC}"
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
echo -e "${GREEN}升级完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${YELLOW}提示：如有数据库迁移，请通过系统界面执行${NC}"
```

**使用方法**：

```bash
# 保存为脚本
cat > /tmp/upgrade.sh << 'EOF'
[上面的脚本内容]
EOF

# 添加执行权限
chmod +x /tmp/upgrade.sh

# 执行
/tmp/upgrade.sh
```

---

## 📊 数据保留说明

### 会保留的数据

升级操作会**保留**以下数据：

| 数据卷名称 | 内容 | 说明 |
|-----------|------|------|
| `postgres_data` | PostgreSQL 数据库 | ✅ **所有业务数据保留** |
| `redis_data` | Redis 缓存 | ✅ **缓存数据保留** |
| `minio_data` | MinIO 文件存储 | ✅ **所有文件保留** |
| `onlyoffice_data` | OnlyOffice 数据 | ✅ **文档数据保留** |
| `onlyoffice_logs` | OnlyOffice 日志 | ✅ **日志保留** |
| `onlyoffice_cache` | OnlyOffice 缓存 | ✅ **缓存保留** |
| `prometheus_data` | Prometheus 数据 | ✅ **监控数据保留** |
| `grafana_data` | Grafana 数据 | ✅ **仪表板配置保留** |

### 会更新的内容

- ✅ 代码文件（最新版本）
- ✅ Docker 镜像（重新构建）
- ✅ 容器配置（如有变更）

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

### 问题 2: 容器无法停止

**错误**：`Error response from daemon: cannot stop container`

**解决**：
```bash
# 强制停止
docker stop law-firm-backend law-firm-frontend 2>/dev/null || true

# 然后重新执行升级
./scripts/deploy.sh --quick
```

### 问题 3: 镜像构建失败

**错误**：`Error building image`

**解决**：
```bash
# 清理构建缓存
docker builder prune -af

# 重新构建
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml build --no-cache
```

### 问题 4: 升级后服务无法启动

**检查步骤**：
```bash
# 1. 查看容器日志
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml logs

# 2. 检查容器状态
docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 3. 检查数据卷是否还在
docker volume ls | grep -E "(postgres|redis|minio)"

# 4. 检查数据库连接
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT 1;"
```

### 问题 5: 数据库迁移失败

**错误**：迁移脚本执行失败

**解决**：
```bash
# 1. 查看迁移日志
docker logs law-firm-backend | grep -i migration

# 2. 手动执行迁移（备份后）
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < scripts/migration/xxx.sql

# 3. 回滚到之前版本（如果迁移失败）
git checkout <previous-version>
./scripts/deploy.sh --quick
```

### 问题 6: 升级后数据丢失

**如果发现数据丢失**：

1. **检查数据卷是否还在**：
   ```bash
   docker volume ls | grep postgres_data
   ```

2. **检查是否误用了重置命令**：
   - 确认使用的是 `down` 而不是 `down -v`
   - 确认没有执行 `clean-law-firm-only.sh`

3. **从备份恢复**：
   ```bash
   ./scripts/restore.sh backup_file.sql
   ```

---

## ⚠️ 注意事项

### 升级前检查

- [ ] 已备份重要数据
- [ ] 已查看更新日志（了解变更内容）
- [ ] 已确认数据库迁移脚本（如有）
- [ ] 已通知用户（生产环境）

### 升级后检查

- [ ] 所有服务正常运行
- [ ] 数据库连接正常
- [ ] 文件访问正常
- [ ] 用户登录正常
- [ ] 关键功能测试通过

### 数据库迁移注意事项

1. **自动迁移**：部署脚本会自动检测并执行迁移
2. **手动迁移**：如有复杂迁移，建议手动执行
3. **迁移回滚**：迁移前备份，失败可回滚
4. **迁移测试**：生产环境迁移前，先在测试环境验证

---

## 📚 相关文档

- [服务器端：重置数据、重新部署完整指南](./SERVER_RESET_AND_REDEPLOY.md) - **重置所有数据**
- [完全清理和重新部署命令](./CLEANUP_AND_REDEPLOY.md)
- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)

---

## 🔄 升级 vs 重置对比

| 操作 | 升级（本指南） | 重置（SERVER_RESET_AND_REDEPLOY.md） |
|------|---------------|-------------------------------------|
| **代码更新** | ✅ 更新 | ✅ 更新 |
| **镜像重建** | ✅ 重建 | ✅ 重建 |
| **数据库数据** | ✅ **保留** | ❌ 删除 |
| **文件存储** | ✅ **保留** | ❌ 删除 |
| **Redis 缓存** | ✅ **保留** | ❌ 删除 |
| **监控数据** | ✅ **保留** | ❌ 删除 |
| **数据库迁移** | ✅ 自动执行 | ✅ 重新初始化 |
| **适用场景** | 生产环境升级 | 开发测试重置 |

---

## ✅ 升级检查清单

升级前确认：

- [ ] 已备份数据库
- [ ] 已查看更新日志
- [ ] 已确认数据库迁移（如有）
- [ ] 已通知用户（生产环境）

升级后验证：

- [ ] 所有容器正常运行
- [ ] 健康检查通过
- [ ] 数据库连接正常
- [ ] 文件访问正常
- [ ] 用户登录正常
- [ ] 关键功能测试通过
- [ ] 数据库迁移已执行（如有）

---

## 🎯 快速参考命令

```bash
# 完整升级流程（一行命令）
cd /opt/law-firm && git pull origin main && ./scripts/deploy.sh --quick
# 或使用短参数：
cd /opt/law-firm && git pull origin main && ./scripts/deploy.sh -q

# 仅拉取代码
git pull origin main

# 仅重新部署（保留数据）
./scripts/deploy.sh --quick
# 或使用短参数：
./scripts/deploy.sh -q

# 手动升级（保留数据）
cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml down && \
docker compose --env-file ../.env -f docker-compose.prod.yml build && \
docker compose --env-file ../.env -f docker-compose.prod.yml up -d

# 查看服务状态
cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml ps

# 查看日志
cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml logs -f
```

---

## 🔍 升级流程对比

### 升级（保留数据）

```bash
git pull                    # 拉取代码
docker compose down         # 停止服务（不删除数据卷）
docker compose build        # 重建镜像
docker compose up -d        # 启动服务
# ✅ 数据保留
```

### 重置（删除数据）

```bash
git pull                    # 拉取代码
docker compose down -v      # 停止服务并删除数据卷
docker compose build        # 重建镜像
docker compose up -d        # 启动服务
reset-db.sh                 # 重新初始化数据库
# ❌ 数据删除
```

---

## 💡 最佳实践

1. **定期升级**：建议每月升级一次，及时获取安全补丁
2. **测试先行**：生产环境升级前，先在测试环境验证
3. **备份优先**：升级前必须备份，即使升级不删除数据
4. **分步执行**：复杂升级分步执行，每步验证
5. **监控观察**：升级后观察监控，确保系统稳定
