# 故障排查

本章节按常见故障分类给出排查步骤，优先从日志、健康检查和脚本入手。

---

## 🔄 系统升级和重置

### 升级代码（保留数据）

**推荐方式**：使用一键部署脚本

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 拉取最新代码
git pull origin main

# 3. 升级部署（保留数据）
./scripts/deploy.sh --quick
```

**手动升级**：

```bash
cd /opt/law-firm/docker

# 1. 停止服务（不删除数据卷）
docker compose --env-file ../.env -f docker-compose.prod.yml down

# 2. 重新构建镜像
docker compose --env-file ../.env -f docker-compose.prod.yml build

# 3. 启动服务
docker compose --env-file ../.env -f docker-compose.prod.yml up -d
```

### 完全清理并重新部署

⚠️ **警告**：此操作会删除所有数据！

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 拉取最新代码
git pull origin main

# 3. 完全清理（仅清理项目相关资源）
./scripts/clean-law-firm-only.sh

# 4. 重新部署
./scripts/deploy.sh --quick
```

**清理所有 Docker 资源**（包括其他项目）：

```bash
# ⚠️ 危险操作：会删除所有 Docker 资源
./scripts/clean-docker-all.sh --force
```

---

## 常见问题

### 服务无法启动

1. 检查容器状态

```bash
docker compose -f docker/docker-compose.prod.yml ps
```

2. 检查端口占用

```bash
lsof -i :5666
```

3. 检查后端日志

```bash
docker logs law-firm-backend
```

4. 检查数据库连接

```bash
docker exec law-firm-postgres pg_isready -U law_admin -d law_firm
```

如为源码运行模式，可使用：

```bash
psql -U lawfirm -h localhost -d lawfirm
```

### 数据库连接失败

```bash
# 检查 PostgreSQL 状态（Docker）
docker exec law-firm-postgres pg_isready -U law_admin -d law_firm

# 检查连接
psql -U law_admin -h localhost -d law_firm -c "SELECT 1"
```

**重点检查**：

- `.env` 中 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` 是否与容器配置一致
- 数据库容器是否重启频繁（`docker ps -a` 查看）

### 浏览器缓存问题

如果浏览器显示的是旧内容，需要清除缓存：

**Chrome / Edge（推荐方法）**：

1. **硬刷新**（最快）：
   - Windows/Linux: `Ctrl + Shift + R` 或 `Ctrl + F5`
   - Mac: `Cmd + Shift + R` 或 `Cmd + Option + R`

2. **清除缓存**：
   - 按 `F12` 打开开发者工具
   - 右键点击浏览器刷新按钮
   - 选择 **"清空缓存并硬性重新加载"**

**Firefox**：

- Windows/Linux: `Ctrl + Shift + R` 或 `Ctrl + F5`
- Mac: `Cmd + Shift + R`

**Safari**：

- Mac: `Cmd + Option + E`（清空缓存）然后 `Cmd + R`（刷新）

### Redis 连接失败

```bash
redis-cli -h localhost -p 6379 ping
```

或在 Docker 环境：

```bash
docker exec -it law-firm-redis redis-cli ping
```

### 文件上传失败

1. 检查 MinIO 服务状态
2. 检查存储空间
3. 检查文件大小限制（Nginx `client_max_body_size`，默认 50MB）
4. 检查 MinIO 访问密钥是否与 `.env` 配置一致

### 前端页面空白

1. 检查浏览器控制台错误
2. 检查 API 地址配置（前端 `.env.production` 中 `VITE_GLOB_API_URL`）
3. 清除浏览器缓存
4. 检查 Nginx 配置是否正确代理 `/api` 到后端

## 性能问题

### 响应慢

1. 检查数据库慢查询

```sql
SELECT * FROM pg_stat_activity WHERE state = 'active';
```

2. 检查 Redis 缓存命中率
3. 检查服务器资源使用（CPU、内存、IO）
4. 在 Prometheus/Grafana 中查看接口耗时、错误率变化

### 内存不足

```bash
# 查看内存使用
free -h

# 查看容器内存限制
docker stats

# 调整 JVM 参数（非容器模式示例）
java -Xmx2g -jar law-firm-backend.jar
```

如为 Docker 模式，可在 `docker-compose.prod.yml` 中调整 `Xmx` 或 container memory 限制。

## 数据问题

### 重置数据库（测试环境）

```bash
# 执行重置脚本
./scripts/reset-db.sh
```

注意：会清空当前数据库并重新初始化，谨慎在生产环境使用。

### 修复权限/初始化数据

如果权限配置或系统初始数据出错，可以重新执行初始化脚本：

```bash
psql -U lawfirm -d lawfirm < scripts/init-db/20-system-init-data.sql
```

或在 Docker 环境通过容器执行。

## 安全与生产检查

在首次上线或重大变更前，建议运行安全检查脚本：

```bash
./scripts/security-check.sh
```

该脚本会检查：

- `.env` 是否存在、敏感密钥是否配置且足够复杂
- 是否仍使用默认密码或默认密钥
- `.env` 是否被纳入 git 版本控制
- Docker 生产配置是否存在健康检查
- 是否配置 SSL/TLS（建议生产环境启用 HTTPS）

根据脚本输出修复所有错误与警告后再上线生产。

## 联系支持

如无法解决，请联系技术支持并提供：

1. 错误日志（后端、Nginx、容器日志）
2. 操作步骤（尽可能还原问题前后的操作）
3. 系统环境信息（服务器配置、部署方式、一键脚本或手动、版本号）
