# 环境变量配置文件说明

## 📋 文件位置

项目中有两个 `env.example` 文件：

1. **`/env.example`**（项目根目录）✅ **生产环境使用此文件**
2. **`/docker/env.example`**（Docker 目录）⚠️ 仅作参考，实际不使用

## 🎯 使用哪个文件？

### 生产环境

**使用项目根目录的 `.env` 文件**

```bash
# 1. 复制模板文件
cp env.example .env

# 2. 编辑配置
vim .env

# 3. 启动服务（会自动使用根目录的 .env）
./scripts/env-start.sh prod
```

**原因**：
- `scripts/env-start.sh` 第 155 行明确指定：`--env-file "$PROJECT_ROOT/.env"`
- `docker-compose.prod.yml` 注释说明：环境变量配置文件在项目根目录 `.env`

### 开发/测试环境

开发环境和测试环境不使用 `.env` 文件，配置直接写在 `docker-compose.dev.yml` 和 `docker-compose.test.yml` 中。

## 📝 配置文件对比

| 配置项 | 根目录 `env.example` | `docker/env.example` | 说明 |
|--------|---------------------|---------------------|------|
| **用途** | ✅ 生产环境实际使用 | ⚠️ 仅作参考 | - |
| **DB_HOST** | ✅ 已包含 | ✅ 已包含 | Docker 内部服务名 |
| **MINIO_ENDPOINT** | ✅ 已包含 | ✅ 已包含 | MinIO 内部地址 |
| **ONLYOFFICE_EXTERNAL_ACCESS_URL** | ✅ 已包含 | ✅ 已包含 | OnlyOffice 外部访问地址 |
| **详细注释** | ✅ 已更新 | ✅ 更详细 | - |

## 🔧 配置说明

### 必须修改的配置

1. **数据库密码** (`DB_PASSWORD`)
   ```bash
   # 生成强密码
   openssl rand -base64 24
   ```

2. **JWT 密钥** (`JWT_SECRET`)
   ```bash
   # 生成至少 64 字符的密钥
   openssl rand -base64 64
   ```

3. **MinIO 密钥** (`MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`)
   ```bash
   # 不能使用默认的 minioadmin
   openssl rand -base64 24
   ```

4. **OnlyOffice JWT 密钥** (`ONLYOFFICE_JWT_SECRET`)
   ```bash
   # 生成密钥
   openssl rand -base64 64
   ```

### 重要配置项

#### OnlyOffice 外部访问地址

```bash
# 如果使用 IP 访问
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://192.168.50.10

# 如果使用域名访问
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://oa.albertzhan.top
```

**作用**：OnlyOffice 容器通过 Nginx 代理访问文件，而不是直接访问 Docker 内部地址。

#### MinIO 外部端点

```bash
# 如果配置了外部访问地址，缩略图等资源会使用外部地址
MINIO_EXTERNAL_ENDPOINT=http://minio:9000
```

## 🚀 快速开始

### 首次部署

```bash
# 1. 复制模板文件
cp env.example .env

# 2. 编辑配置文件
vim .env
# 修改所有密码和密钥

# 3. 启动服务
./scripts/env-start.sh prod
```

### 更新配置

```bash
# 1. 编辑配置文件
vim .env

# 2. 重启服务（使配置生效）
./scripts/env-stop.sh prod
./scripts/env-start.sh prod
```

## ⚠️ 注意事项

1. **不要提交 `.env` 文件**
   - `.env` 文件包含敏感信息，已在 `.gitignore` 中忽略
   - 只提交 `env.example` 模板文件

2. **生产环境必须修改所有默认密码**
   - 使用 `scripts/security-check.sh` 检查配置安全性

3. **配置变更后需要重启服务**
   - 修改 `.env` 后，需要重启相关服务才能生效

## 🔍 验证配置

### 检查配置是否正确

```bash
# 运行安全检查脚本
./scripts/security-check.sh

# 运行生产环境检查
./scripts/check-production-ready.sh
```

### 查看当前配置

```bash
# 查看环境变量（不显示敏感信息）
docker exec law-firm-backend env | grep -E "DB_|MINIO_|ONLYOFFICE_" | sort
```

## 📚 相关文档

- [OnlyOffice IP 访问修复指南](./ONLYOFFICE_IP_ACCESS_FIX.md)
- [生产环境部署检查清单](./PRODUCTION_DEPLOYMENT_CHECKLIST.md)
- [安全配置指南](./SECURITY_CONFIG.md)
