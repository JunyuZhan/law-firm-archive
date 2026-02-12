# 安全部署指南（Docker Secrets）

本指南介绍如何使用 Docker Secrets 安全部署生产环境。

## 为什么使用 Docker Secrets？

| 方式 | `docker inspect` | 进程列表 | 安全性 |
|------|-----------------|---------|--------|
| 环境变量 | ❌ 可见 | ❌ 可见 | 较低 |
| Docker Secrets | ✅ 不可见 | ✅ 不可见 | 高 |

## 快速部署

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 初始化密钥（首次部署）
cd docker/secrets
./init-secrets.sh
cd ..

# 3. 启动服务
docker compose -f docker-compose.prod-secrets.yml up -d

# 4. （可选）启用文档服务
docker compose -f docker-compose.prod-secrets.yml --profile docs up -d

# 5. （可选）启用监控服务
docker compose -f docker-compose.prod-secrets.yml --profile monitoring up -d
```

## 密钥文件说明

初始化后，`docker/secrets/` 目录包含以下文件：

| 文件 | 用途 | 必需 |
|------|------|------|
| `db_password` | PostgreSQL 密码 | ✅ |
| `jwt_secret` | JWT 签名密钥 | ✅ |
| `minio_access_key` | MinIO 用户名 | ✅ |
| `minio_secret_key` | MinIO 密码 | ✅ |
| `onlyoffice_jwt_secret` | OnlyOffice JWT | 启用文档服务时 |
| `redis_password` | Redis 密码 | 可选 |
| `grafana_password` | Grafana 密码 | 启用监控时 |

## 验证安全性

```bash
# 尝试查看容器环境变量 - 不应看到密码
docker inspect law-firm-backend | grep -A 50 '"Env"'

# 密钥只存在于 /run/secrets/ 目录中（容器内）
docker exec law-firm-backend ls -la /run/secrets/
```

## 密钥管理

### 更新密钥

```bash
# 1. 修改密钥文件
echo -n "new-password" > docker/secrets/db_password

# 2. 重启相关服务
docker compose -f docker/docker-compose.prod-secrets.yml restart backend postgres
```

### 备份密钥

```bash
# 备份到安全位置（加密）
tar -czf - docker/secrets/ | gpg -c > secrets-backup.tar.gz.gpg
```

### 恢复密钥

```bash
# 从备份恢复
gpg -d secrets-backup.tar.gz.gpg | tar -xzf -
```

## 与传统方式对比

### Docker Secrets 方式（推荐）

```bash
docker compose -f docker/docker-compose.yml up -d
```

- ✅ `docker inspect` 无法查看密钥
- ✅ 密钥不出现在进程列表
- ✅ 支持细粒度权限控制

### 环境变量方式（传统）

```bash
docker compose --env-file .env -f docker/docker-compose.yml up -d
```

- ❌ `docker inspect` 可查看所有密钥
- ❌ 密钥可能出现在日志中
- ⚠️ 仅建议在开发/测试环境使用

## 故障排除

### 启动失败：找不到密钥文件

```bash
# 检查密钥文件是否存在
ls -la docker/secrets/

# 重新初始化
cd docker/secrets && ./init-secrets.sh
```

### 数据库连接失败

```bash
# 检查密码是否正确
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT 1"

# 查看后端日志
docker logs law-firm-backend
```

### MinIO 无法访问

```bash
# 检查 MinIO 状态
docker logs law-firm-minio

# 验证密钥
docker exec law-firm-minio cat /run/secrets/minio_access_key
```
