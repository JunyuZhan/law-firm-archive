# 部署指南

## 🚀 一键部署（生产环境推荐）

如果你使用的是「一键部署」方案，可以直接通过根目录脚本完成全栈部署。

### 1. 准备环境

```bash
# 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com | sh
```

### 2. 部署前检查（推荐）

```bash
# 运行部署前检查脚本
./scripts/pre-deploy-check.sh
```

检查脚本会验证：

- ✅ Docker 环境
- ✅ 环境变量配置
- ✅ 配置文件完整性
- ✅ 数据库初始化脚本
- ✅ 备份配置
- ✅ 敏感信息泄露
- ✅ 系统资源

### 3. 一键部署

在项目根目录执行：

```bash
# 引导式部署（推荐）
./scripts/deploy.sh

# 或快速部署（非交互）
./scripts/deploy.sh --quick

# 快速部署并初始化示例数据
./scripts/deploy.sh --quick --with-demo
```

脚本会自动完成：

- ✅ 检查 Docker 环境
- ✅ 创建或验证 `.env` 配置文件
- ✅ 自动生成安全密钥（首次部署）
- ✅ 运行生产环境检查
- ✅ 构建 Docker 镜像
- ✅ 启动所有服务（前端、后端、PostgreSQL、Redis、MinIO、OnlyOffice、OCR 等）
- ✅ 初始化数据库和示例数据（根据脚本提示）

### 4. 部署模式选择

部署脚本支持多种部署模式：

1. **单机部署**（推荐小型律所）

   ```bash
   ./scripts/deploy.sh --mode=standalone
   ```

2. **NAS 存储分离部署**（推荐中型律所）

   ```bash
   ./scripts/deploy.sh --mode=nas --nas-ip=192.168.1.100
   ```

3. **Docker Swarm 分布式部署**（推荐大型律所）

   ```bash
   ./scripts/deploy.sh --mode=swarm
   ```

4. **MinIO 分布式存储集群**（企业级）
   ```bash
   ./scripts/deploy.sh --mode=minio-cluster
   ```

### 5. 部署后验证

部署成功后，默认访问地址（单端口架构）：

- 🌐 **主应用**: `http://localhost/`
- 📚 **文档站点**: `http://localhost/docs/`
- 🔧 **API 地址**: `http://localhost/api`
- 📦 **MinIO 控制台**: `http://localhost/minio-console/`
- 📝 **OnlyOffice**: `http://localhost/onlyoffice/`（通过应用调用）
- 📊 **Prometheus**: `http://localhost/prometheus/`（可选，需启用监控）
- 📈 **Grafana**: `http://localhost/grafana/`（可选，需启用监控）

> 📖 关于单端口架构的详细说明，请参考 [单端口架构](/guide/ops/single-port-architecture)

**检查服务状态**：

```bash
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps
```

**检查健康状态**：

```bash
curl http://localhost/api/actuator/health
# 应返回: {"status":"UP"}
```

**默认账号**（密码统一为 `admin123`）：

- `admin` - 系统管理员
- `director` - 主任
- `lawyer1` - 律师

如需分布式部署或 NAS 存储模式，请参考项目根目录下的 `docker/DEPLOY.md` 和 `docker/DEPLOY-SWARM.md`。

---

## Docker 部署（手动方式）

### 1. 准备环境

```bash
# 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com | sh
```

### 2. 启动基础服务

```bash
cd docker
docker-compose up -d
```

这将启动：

- PostgreSQL
- Redis
- MinIO
- Elasticsearch（可选）

### 3. 初始化数据库

```bash
# 执行初始化脚本
docker exec -i postgres psql -U lawfirm < scripts/init-db/10-schema.sql
docker exec -i postgres psql -U lawfirm < scripts/init-db/20-system-init-data.sql
```

### 4. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/law-firm-backend.jar
```

### 5. 启动前端

开发环境：

```bash
cd frontend
pnpm install
pnpm dev
# 选择 @vben/web-antd
```

生产环境：

```bash
cd frontend
pnpm build:antd
# 将 dist 目录部署到 Nginx
```

## 手动部署

### 后端部署

```bash
cd backend
mvn clean package -DskipTests
java -Xmx2g -jar target/law-firm-backend.jar --spring.profiles.active=prod
```

### 前端部署

```bash
cd frontend
pnpm install
pnpm build:antd
```

将 `frontend/apps/web-antd/dist` 目录部署到 Web 服务器。

## Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/law-firm;
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://localhost:5666;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 文件上传大小限制
    client_max_body_size 50M;
}
```

## 文档站点部署

```bash
cd frontend
pnpm dev
# 选择 @vben/docs
```

文档站点运行在 6173 端口。
