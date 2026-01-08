# 部署指南

## Docker 部署（推荐）

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
