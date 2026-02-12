# Docker Secrets 目录

此目录用于存放敏感信息文件，**所有文件内容都不会被 Git 跟踪**。

## 安全特性

使用文件存储密钥而非环境变量的优势：
- `docker inspect` 无法查看密钥内容
- 密钥不会出现在进程列表或日志中
- 支持更细粒度的文件权限控制

## 快速初始化

```bash
# 运行初始化脚本（自动生成所有密钥文件）
./init-secrets.sh
```

## 手动创建密钥文件

如果需要手动创建，每个文件只包含密钥值本身（无换行符）：

```bash
# 数据库密码
echo -n "your-strong-db-password" > db_password

# JWT 密钥（建议64字符以上）
openssl rand -base64 64 | tr -d '\n' > jwt_secret

# MinIO 访问密钥
echo -n "your-minio-access-key" > minio_access_key
openssl rand -base64 32 | tr -d '\n' > minio_secret_key

# OnlyOffice JWT 密钥
openssl rand -base64 64 | tr -d '\n' > onlyoffice_jwt_secret

# Redis 密码（可选）
openssl rand -base64 32 | tr -d '\n' > redis_password

# Grafana 管理员密码
openssl rand -base64 16 | tr -d '\n' > grafana_password

# 设置权限（仅所有者可读）
chmod 600 *
```

## 文件列表

| 文件名 | 用途 | 必需 |
|--------|------|------|
| `db_password` | PostgreSQL 数据库密码 | ✅ |
| `jwt_secret` | JWT 签名密钥 | ✅ |
| `minio_access_key` | MinIO 访问密钥 | ✅ |
| `minio_secret_key` | MinIO 密钥 | ✅ |
| `onlyoffice_jwt_secret` | OnlyOffice JWT 密钥 | 启用文档服务时必需 |
| `redis_password` | Redis 密码 | 可选 |
| `grafana_password` | Grafana 管理员密码 | 启用监控时建议 |

## 权限要求

```bash
# 确保密钥文件权限正确
chmod 600 docker/secrets/*
```

## 备份提醒

⚠️ **重要**：这些文件不会被 Git 跟踪，请确保：
1. 在安全位置备份这些密钥
2. 使用密码管理器存储密钥副本
3. 在新环境部署时需要重新创建或恢复
