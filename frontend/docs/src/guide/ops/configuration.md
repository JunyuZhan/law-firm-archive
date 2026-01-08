# 配置说明

## 后端配置

配置文件：`backend/src/main/resources/application.yml`

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lawfirm
    username: lawfirm
    password: your-password
```

### Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your-password
```

### MinIO 配置

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: law-firm
```

### JWT 配置

```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时
  refresh-expiration: 604800000  # 7天
```

## 前端配置

配置文件：`frontend/apps/web-antd/.env.production`

```env
VITE_GLOB_API_URL=/api
```

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_HOST | 数据库地址 | localhost |
| DB_PORT | 数据库端口 | 5432 |
| DB_NAME | 数据库名 | lawfirm |
| DB_USER | 数据库用户 | lawfirm |
| DB_PASSWORD | 数据库密码 | - |
| REDIS_HOST | Redis 地址 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| MINIO_ENDPOINT | MinIO 地址 | http://localhost:9000 |
| MINIO_ACCESS_KEY | MinIO 访问密钥 | minioadmin |
| MINIO_SECRET_KEY | MinIO 密钥 | minioadmin |

## Docker Compose 配置

配置文件：`docker/docker-compose.yml`

可以修改各服务的端口、密码等配置。
