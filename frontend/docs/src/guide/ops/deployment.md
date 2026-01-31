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

## 🖥️ 服务器部署完整指南

### 部署前准备

#### 1. 服务器要求

**最低配置：**
- CPU: 2核
- 内存: 4GB
- 磁盘: 20GB
- 操作系统: Ubuntu 20.04+ / CentOS 7+ / Debian 10+

**推荐配置：**
- CPU: 4核+
- 内存: 8GB+
- 磁盘: 50GB+ SSD

#### 2. 需要开放的端口

| 端口 | 协议 | 说明 |
|------|------|------|
| 22 | TCP | SSH（远程管理） |
| 80 | TCP | HTTP |
| 443 | TCP | HTTPS |

> 📖 单端口架构下，其他端口不暴露，通过 Nginx 路径访问

### 服务器环境准备

#### 1. 连接到服务器

```bash
ssh root@你的服务器IP
```

#### 2. 安装 Docker 和 Docker Compose

**Ubuntu/Debian:**
```bash
# 更新系统
apt update && apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 启动 Docker
systemctl start docker
systemctl enable docker

# 安装 Docker Compose
apt install docker-compose-plugin -y

# 验证安装
docker --version
docker compose version
```

**CentOS/RHEL:**
```bash
# 更新系统
yum update -y

# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 启动 Docker
systemctl start docker
systemctl enable docker

# 安装 Docker Compose
yum install docker-compose-plugin -y

# 验证安装
docker --version
docker compose version
```

#### 3. 配置防火墙

```bash
# Ubuntu/Debian (ufw)
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable

# CentOS/RHEL (firewalld)
firewall-cmd --permanent --add-port=22/tcp
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --reload
```

### 上传代码到服务器

#### 方式一：使用 Git（推荐）

```bash
# 在服务器上克隆代码
cd /opt  # 或其他目录
git clone https://github.com/JunyuZhan/law-firm.git
cd law-firm
```

**⚠️ 重要提示：** 代码拉取后，SSL 证书文件**不会**自动下载（因为已在 `.gitignore` 中），需要单独上传证书！

#### 方式二：使用部署脚本（推荐）

```bash
# 从本地执行
./scripts/deploy/deploy-to-server.sh <服务器IP> [用户名]

# 示例
./scripts/deploy/deploy-to-server.sh 192.168.1.100 root
```

脚本会自动：
- ✅ 检查 SSH 连接
- ✅ 使用 rsync 同步代码
- ✅ 自动检查并安装 Docker
- ✅ 自动执行部署

#### 方式三：使用 SCP 上传

```bash
# 在本地电脑执行
scp -r . root@你的服务器IP:/opt/law-firm
```

### 配置 SSL 证书

**⚠️ 重要：** 证书文件**不会**通过 Git 同步，必须单独上传！

#### 方式一：使用上传脚本（推荐）

```bash
# 在本地项目目录执行
./scripts/ssl/upload-ssl-certs.sh <服务器IP> [用户名]

# 例如：
./scripts/ssl/upload-ssl-certs.sh 192.168.1.100 root
```

#### 方式二：手动上传

```bash
# 在服务器上创建证书目录
ssh root@你的服务器IP "mkdir -p /opt/law-firm/docker/ssl"

# 在本地执行，上传证书文件
scp docker/ssl/ca.crt root@你的服务器IP:/opt/law-firm/docker/ssl/
scp docker/ssl/ca.key root@你的服务器IP:/opt/law-firm/docker/ssl/

# 创建符号链接
ssh root@你的服务器IP "cd /opt/law-firm/docker/ssl && ln -sf ca.crt fullchain.pem && ln -sf ca.key privkey.pem && chmod 644 fullchain.pem && chmod 600 privkey.pem"
```

#### 方式三：使用 Let's Encrypt（推荐生产环境）

```bash
# SSH 到服务器
ssh root@你的服务器IP

# 安装 certbot
apt install certbot -y

# 获取证书（需要域名已解析到服务器）
certbot certonly --standalone -d 你的域名.com

# 复制证书到项目目录
mkdir -p /opt/law-firm/docker/ssl
cp /etc/letsencrypt/live/你的域名.com/fullchain.pem /opt/law-firm/docker/ssl/fullchain.pem
cp /etc/letsencrypt/live/你的域名.com/privkey.pem /opt/law-firm/docker/ssl/privkey.pem
```

### 配置环境变量

```bash
cd /opt/law-firm

# 复制环境变量模板
cp env.example .env

# 编辑环境变量（重要！）
vim .env  # 或使用 nano .env
```

> **⚠️ 注意：** 环境配置统一使用项目根目录的 `.env` 文件，不要在 `docker/` 目录下创建 `.env`。

**必须修改的配置：**

```bash
# 数据库密码（必须修改！）
DB_PASSWORD=你的强密码

# JWT 密钥（必须修改！至少64字符）
JWT_SECRET=你的JWT密钥（使用 openssl rand -base64 64 生成）

# MinIO 密钥（必须修改！）
MINIO_ACCESS_KEY=你的访问密钥
MINIO_SECRET_KEY=你的秘密密钥
```

**生成安全密钥：**

```bash
# 生成数据库密码
openssl rand -base64 24

# 生成 JWT 密钥
openssl rand -base64 64

# 生成 MinIO 密钥
openssl rand -base64 24
```

> 💡 **提示**：使用 `./scripts/deploy.sh` 一键部署时，会自动生成所有安全密钥。

### 部署应用

#### 方式一：一键部署（推荐）

```bash
cd /opt/law-firm

# 给脚本执行权限
chmod +x scripts/deploy.sh

# 执行部署
./scripts/deploy.sh --quick
```

脚本会自动：
- ✅ 检查环境
- ✅ 生成安全密钥（如果未配置）
- ✅ 构建前端和后端
- ✅ 启动所有服务
- ✅ 初始化数据库

#### 方式二：手动部署

```bash
cd /opt/law-firm

# 1. 构建并启动服务（使用根目录的 .env 文件）
docker compose --env-file .env -f docker/docker-compose.prod.yml up -d --build

# 2. 查看日志
docker compose --env-file .env -f docker/docker-compose.prod.yml logs -f

# 3. 检查服务状态
docker compose --env-file .env -f docker/docker-compose.prod.yml ps
```

### 验证部署

#### 检查服务状态

```bash
cd /opt/law-firm
docker compose --env-file .env -f docker/docker-compose.prod.yml ps
```

所有服务应该显示 `Up` 状态。

#### 检查健康状态

```bash
# 测试 HTTP
curl http://localhost

# 测试 HTTPS（如果配置了）
curl -k https://localhost

# 测试 API
curl http://localhost/api/actuator/health
# 应返回: {"status":"UP"}
```

#### 浏览器访问

- 主应用: `http://你的服务器IP/` 或 `https://你的域名/`
- 文档站点: `http://你的服务器IP/docs/`
- MinIO 控制台: `http://你的服务器IP/minio-console/`

**默认账号密码：** `admin123`

### 日常维护

#### 更新代码

```bash
cd /opt/law-firm

# 拉取最新代码
git pull

# 重新部署（从根目录执行，使用根目录的 .env 文件）
docker compose --env-file .env -f docker/docker-compose.prod.yml up -d --build
```

#### 查看日志

```bash
cd /opt/law-firm

# 查看所有日志
docker compose --env-file .env -f docker/docker-compose.prod.yml logs -f

# 查看最近100行日志
docker compose --env-file .env -f docker/docker-compose.prod.yml logs --tail=100
```

#### 重启服务

```bash
cd /opt/law-firm

# 重启所有服务
docker compose --env-file .env -f docker/docker-compose.prod.yml restart

# 重启单个服务
docker compose --env-file .env -f docker/docker-compose.prod.yml restart backend
```

#### 停止服务

```bash
cd /opt/law-firm

# 停止所有服务（保留数据）
docker compose --env-file .env -f docker/docker-compose.prod.yml stop

# 停止并删除容器（数据保留在 volumes 中）
docker compose --env-file .env -f docker/docker-compose.prod.yml down
```

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
