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

**症状**：卷宗列表的文档管理无法上传文件，提示"文件上传失败"或"文件服务暂时不可用"

**排查步骤**：

1. **检查 MinIO 服务状态**
```bash
# 检查 MinIO 容器是否运行
docker ps | grep minio

# 检查 MinIO 日志
docker logs law-firm-minio --tail 50

# 检查 MinIO 健康状态
docker exec law-firm-minio curl -f http://localhost:9000/minio/health/live
```

2. **检查后端与 MinIO 的连接**
```bash
# 检查后端环境变量配置
docker exec law-firm-backend env | grep MINIO

# 应该看到：
# MINIO_ENDPOINT=http://minio:9000  ✅ 正确（使用服务名）
# 如果看到 http://law-firm-minio:9000，需要修复配置

# 测试后端能否访问 MinIO
docker exec law-firm-backend curl -f http://minio:9000/minio/health/live
```

3. **检查 MinIO 访问密钥**
```bash
# 检查 .env 文件中的 MinIO 密钥
grep MINIO .env

# 确认密钥与 MinIO 容器配置一致
docker exec law-firm-minio env | grep MINIO_ROOT
```

4. **检查存储空间**
```bash
# 检查 MinIO 存储卷
docker volume inspect law-firm_minio_data

# 检查磁盘空间
df -h
```

5. **检查文件大小限制**
- Nginx 配置：`client_max_body_size 100M`（API 上传）
- Nginx 配置：`client_max_body_size 500M`（MinIO 代理）
- Spring Boot：`spring.servlet.multipart.max-file-size=100MB`

**常见问题**：

❌ **问题1**：后端无法连接 MinIO
- **原因**：`MINIO_ENDPOINT` 配置错误，使用了容器名而不是服务名
- **解决**：在 `docker-compose.prod.yml` 中修改为 `MINIO_ENDPOINT=http://minio:9000`

❌ **问题2**：是否需要暴露 MinIO 端口？
- **答案**：**不需要！** 单端口架构的设计：
  - ✅ **后端服务**：通过 Docker 内部网络直接访问 MinIO（`http://minio:9000`），不需要暴露端口
  - ✅ **浏览器访问**：通过 Nginx 路径代理（`/minio/`），不需要直接暴露端口
  - ❌ **暴露端口**：会破坏单端口架构的安全性，不推荐

❌ **问题3**：Mixed Content 错误（HTTPS 页面加载 HTTP 资源）
- **症状**：浏览器控制台显示 "Mixed Content" 警告，图片无法加载
- **原因**：预签名 URL 包含 IP 地址（如 `http://192.168.50.10:9000/...`），导致 HTTPS 页面加载 HTTP 资源被阻止
- **解决**：
  1. 确保 `.env` 文件中配置：`MINIO_BROWSER_ENDPOINT=/minio`
  2. 重启后端服务应用配置
  3. 修复后的 URL 格式：`/minio/law-firm/thumbnails/file.jpg?query`（相对路径，自动适配 HTTP/HTTPS）

**验证修复**：
```bash
# 1. 重启后端服务（应用新配置）
docker compose --env-file .env -f docker/docker-compose.prod.yml restart backend

# 2. 检查后端日志，确认 MinIO 连接成功
docker logs law-firm-backend | grep "MinIO 客户端初始化成功"

# 3. 尝试上传文件，检查是否成功
```

### MinIO 文件访问方式

在单端口架构下，有多种方式可以访问和管理 MinIO 文件：

#### 方式 1：MinIO Console（Web 管理界面）⭐ 推荐

**访问地址**：
- 生产环境：`https://your-domain.com/minio-console/`
- 开发环境：`http://localhost/minio-console/`

**登录信息**：
- 用户名：`.env` 文件中的 `MINIO_ACCESS_KEY`
- 密码：`.env` 文件中的 `MINIO_SECRET_KEY`

**功能**：
- ✅ 可视化文件浏览和管理
- ✅ 上传、下载、删除文件
- ✅ 查看文件信息（大小、修改时间等）
- ✅ Bucket 管理
- ✅ 用户和权限管理

**优点**：图形界面，操作简单，无需命令行

#### 方式 2：mc 命令行工具

**使用场景**：服务器上批量操作、脚本自动化、CI/CD 集成

```bash
# 1. 进入 MinIO 容器或使用 mc 容器
docker exec -it law-firm-minio-init sh

# 2. 配置 MinIO 别名（如果还没配置）
mc alias set local http://minio:9000 $MINIO_ACCESS_KEY $MINIO_SECRET_KEY

# 3. 常用命令
mc ls local/law-firm/                    # 列出文件
mc ls local/law-firm/documents/ -r        # 递归列出目录
mc cp local/law-firm/file.pdf ./          # 下载文件
mc cp ./file.pdf local/law-firm/          # 上传文件
mc rm local/law-firm/file.pdf             # 删除文件
mc stat local/law-firm/file.pdf           # 查看文件信息
mc find local/law-firm/ --name "*.pdf"    # 查找文件
mc du local/law-firm/                     # 查看存储使用情况
```

**或者直接使用 mc 容器执行命令**：
```bash
# 列出文件
docker run --rm --network law-firm_law-firm-network \
  -e MINIO_ACCESS_KEY=your_access_key \
  -e MINIO_SECRET_KEY=your_secret_key \
  minio/mc:latest \
  mc alias set local http://minio:9000 $MINIO_ACCESS_KEY $MINIO_SECRET_KEY && \
  mc ls local/law-firm/
```

#### 方式 3：通过后端 API（如果实现了文件列表接口）

如果后端实现了文件管理 API，可以通过应用界面访问：
- 文档管理页面
- 卷宗列表页面
- 其他文件管理功能

#### 方式 4：直接访问文件 URL（浏览器）

文件上传后，可以通过以下方式访问：
- 通过应用界面点击文件链接
- 直接访问：`https://your-domain.com/minio/law-firm/文件路径`
- 使用预签名 URL（临时访问链接）

**总结**：
- 🎯 **日常使用**：MinIO Console（`/minio-console/`）
- 🔧 **服务器维护**：mc 命令行工具
- 📱 **应用内访问**：通过后端 API 和应用界面

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
