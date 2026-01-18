# 环境配置统一说明

本文档说明开发、测试、生产环境的统一配置规范。

## 📁 文件结构

### Docker Compose 配置文件

| 环境 | 配置文件 | 说明 |
|------|----------|------|
| 开发环境 | `docker/docker-compose.dev.yml` | 基础开发环境 |
| 开发环境（全量） | `docker/docker-compose.dev-full.yml` | 包含 OnlyOffice、OCR 等 |
| 测试环境 | `docker/docker-compose.test.yml` | 测试环境配置 |
| 生产环境 | `docker/docker-compose.prod.yml` | 生产环境配置 |

### Dockerfile 文件

| 环境 | Dockerfile | 说明 |
|------|------------|------|
| 开发环境 | `docker/Dockerfile.dev` | 后端开发环境镜像 |
| 测试环境 | `docker/Dockerfile.test` | 后端测试环境镜像 |
| 生产环境 | `docker/Dockerfile.prod` | 后端生产环境镜像 |
| 前端开发 | `docker/Dockerfile.frontend-dev` | 前端开发环境镜像 |

### 环境管理脚本

| 脚本 | 说明 | 用法 |
|------|------|------|
| `scripts/env-start.sh` | 统一环境启动脚本 | `./scripts/env-start.sh [dev\|test\|prod] [选项]` |
| `scripts/env-stop.sh` | 统一环境停止脚本 | `./scripts/env-stop.sh [dev\|test\|prod] [选项]` |
| `scripts/env-reset.sh` | 统一环境重置脚本 | `./scripts/env-reset.sh [dev\|test\|prod] [选项]` |

## 🚀 快速开始

### 开发环境

```bash
# 启动基础开发环境
./scripts/env-start.sh dev

# 启动全量开发环境（包含 OnlyOffice、OCR）
./scripts/env-start.sh dev --full

# 停止开发环境
./scripts/env-stop.sh dev

# 重置开发环境（删除所有数据并重新初始化）
./scripts/env-reset.sh dev
```

### 测试环境

```bash
# 启动测试环境
./scripts/env-start.sh test

# 停止测试环境
./scripts/env-stop.sh test

# 重置测试环境
./scripts/env-reset.sh test
```

### 生产环境

```bash
# 启动生产环境（需要 .env 配置文件）
./scripts/env-start.sh prod

# 停止生产环境
./scripts/env-stop.sh prod

# ⚠️ 重置生产环境（危险操作，不推荐）
./scripts/env-reset.sh prod
```

## 📋 命名规范

### 容器命名

所有容器使用统一前缀 `law-firm-`：

- **开发环境**: `law-firm-postgres`, `law-firm-redis`, `law-firm-minio` 等
- **测试环境**: `law-firm-test-postgres`, `law-firm-test-redis` 等
- **生产环境**: `law-firm-postgres`, `law-firm-redis` 等

### 网络命名

所有环境使用统一网络名：`law-firm-network`

### 数据卷命名

- **开发环境**: `postgres_data`, `redis_data`, `minio_data` 等
- **测试环境**: `postgres_test_data`, `redis_test_data`, `minio_test_data` 等
- **生产环境**: `postgres_data`, `redis_data`, `minio_data` 等

### 数据库命名

- **开发环境**: `law_firm_dev`
- **测试环境**: `law_firm_test`
- **生产环境**: `law_firm`

## 🔧 环境配置

### 开发环境

- **数据库端口**: 5432
- **Redis 端口**: 6379
- **MinIO API 端口**: 9000
- **MinIO 控制台端口**: 9001
- **OnlyOffice 端口**: 8088
- **数据库密码**: `dev_password_123`（仅开发环境）

### 测试环境

- **数据库端口**: 5433（避免与开发环境冲突）
- **Redis 端口**: 6380（避免与开发环境冲突）
- **MinIO API 端口**: 9002（避免与开发环境冲突）
- **MinIO 控制台端口**: 9003（避免与开发环境冲突）
- **OnlyOffice 端口**: 8089（避免与开发环境冲突）
- **数据库密码**: `test_password_123`（仅测试环境）

### 生产环境

- **端口**: 根据 `.env` 配置
- **密码**: 必须通过 `.env` 文件配置强密码
- **安全**: 所有敏感信息通过环境变量注入

## 📝 环境变量

### 开发/测试环境

开发环境和测试环境使用默认配置，无需额外环境变量。

### 生产环境

生产环境必须配置 `.env` 文件（位于项目根目录）：

```bash
# 数据库配置
DB_PASSWORD=your-strong-password
DB_USERNAME=law_admin

# Redis 配置
REDIS_PASSWORD=your-redis-password

# JWT 配置
JWT_SECRET=your-jwt-secret-key

# MinIO 配置
MINIO_ACCESS_KEY=your-minio-access-key
MINIO_SECRET_KEY=your-minio-secret-key

# OnlyOffice 配置
ONLYOFFICE_JWT_SECRET=your-onlyoffice-jwt-secret
```

## 🔄 数据库管理

### 初始化数据库

```bash
# 开发环境
./scripts/reset-db.sh --dev

# 测试环境
./scripts/reset-db.sh --test

# 生产环境
./scripts/reset-db.sh --prod
```

### 自动检测环境

如果不指定环境参数，脚本会自动检测：

```bash
./scripts/reset-db.sh
```

## 📚 相关文档

- [部署指南](../docker/DEPLOY.md)
- [生产环境部署清单](../docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md)
- [快速开始指南](../README.md)

## ⚠️ 注意事项

1. **生产环境安全**: 生产环境必须使用强密码，不要使用默认密码
2. **端口冲突**: 测试环境使用不同端口，可以与开发环境同时运行
3. **数据备份**: 重置环境会删除所有数据，请确保已备份重要数据
4. **环境隔离**: 不同环境的数据卷是隔离的，互不影响

## 🔍 故障排查

### 容器未启动

```bash
# 检查容器状态
docker ps -a | grep law-firm

# 查看日志
docker logs <container-name>
```

### 端口冲突

如果端口被占用，可以：

1. 停止占用端口的服务
2. 修改 `docker-compose.*.yml` 中的端口映射
3. 使用不同的环境（如测试环境使用不同端口）

### 数据卷问题

```bash
# 查看数据卷
docker volume ls | grep law-firm

# 删除数据卷（危险操作）
./scripts/env-stop.sh <env> --remove-volumes
```
