# 生产环境部署指南

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Node.js | 20+ | 前端构建 |
| PostgreSQL | 14+ | 数据库 |
| Redis | 6+ | 缓存 |
| MinIO | 最新版 | 文件存储 |
| Nginx | 1.20+ | 反向代理 |

## 必须配置的环境变量

### 后端服务

```bash
# 数据库配置（必须）
export DB_HOST=your-db-host
export DB_PORT=5432
export DB_NAME=law_firm
export DB_USERNAME=law_admin
export DB_PASSWORD=your-strong-password

# Redis配置（必须）
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password

# JWT密钥（必须，至少256位随机字符串）
export JWT_SECRET=$(openssl rand -base64 32)

# MinIO配置（必须）
export MINIO_ENDPOINT=http://your-minio-host:9000
export MINIO_ACCESS_KEY=your-access-key
export MINIO_SECRET_KEY=your-secret-key
export MINIO_BUCKET=law-firm

# 可选配置
export OCR_SERVICE_URL=http://ocr-service:8000
export SWAGGER_ENABLED=false
```

### 前端服务

前端构建时会读取 `.env.production`，默认 API 地址为 `/api`。
如需修改，编辑 `frontend/apps/web-antd/.env.production`。

## 部署步骤

### 1. 数据库初始化

```bash
# 创建数据库
psql -h $DB_HOST -U postgres -c "CREATE DATABASE law_firm;"
psql -h $DB_HOST -U postgres -c "CREATE USER law_admin WITH PASSWORD 'your-password';"
psql -h $DB_HOST -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE law_firm TO law_admin;"

# 执行初始化脚本
cd scripts/init-db
./init-database.sh
```

### 2. 后端部署

```bash
cd backend

# 打包
./mvnw clean package -Pprod -DskipTests

# 运行
java -jar target/law-firm-*.jar --spring.profiles.active=prod
```

### 3. 前端部署

```bash
cd frontend

# 安装依赖
pnpm install

# 构建
pnpm build:antd

# 产物位于 apps/web-antd/dist/
```

### 4. Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/law-firm/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 文件上传大小限制
    client_max_body_size 100M;
}
```

## Docker 一键部署（推荐）

### 1. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置（必须修改密码！）
vim .env
```

### 2. 一键启动

```bash
# 方法1：使用部署脚本
./scripts/deploy.sh

# 方法2：手动启动
cd docker
docker compose -f docker-compose.prod.yml up -d --build
```

### 3. 访问服务

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost |
| 后端 API | http://localhost:8080/api |
| MinIO 控制台 | http://localhost:9001 |

### 常用命令

```bash
cd docker

# 查看日志
docker compose -f docker-compose.prod.yml logs -f

# 查看服务状态
docker compose -f docker-compose.prod.yml ps

# 停止服务
docker compose -f docker-compose.prod.yml down

# 停止并删除数据（谨慎！）
docker compose -f docker-compose.prod.yml down -v
```

## 安全检查清单

- [ ] 所有密码使用强密码（16位以上，包含大小写字母、数字、特殊字符）
- [ ] JWT_SECRET 使用随机生成的256位密钥
- [ ] 数据库只允许内网访问
- [ ] Redis 设置密码并只允许内网访问
- [ ] MinIO 设置强密码
- [ ] HTTPS 已配置（使用 Let's Encrypt 或商业证书）
- [ ] 生产环境关闭 Swagger UI
- [ ] 定期备份数据库

## 备份策略

```bash
# 数据库备份
pg_dump -h $DB_HOST -U $DB_USERNAME $DB_NAME > backup_$(date +%Y%m%d).sql

# MinIO 备份（使用 mc 工具）
mc mirror minio/law-firm ./backup/files/
```

