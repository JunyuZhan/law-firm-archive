# 环境配置统一化总结

本文档总结了开发、测试、生产环境的配置统一化工作。

## ✅ 已完成的统一化工作

### 1. Docker Compose 文件命名规范

| 旧名称 | 新名称 | 说明 |
|--------|--------|------|
| `docker-compose.yml` | `docker-compose.dev.yml` | 开发环境基础配置 |
| - | `docker-compose.test.yml` | 测试环境配置（新增） |
| `docker-compose.prod.yml` | `docker-compose.prod.yml` | 生产环境配置（已统一） |
| `docker-compose.dev-full.yml` | `docker-compose.dev-full.yml` | 开发环境全量配置（已统一） |

### 2. Dockerfile 命名规范

| 旧名称 | 新名称 | 说明 |
|--------|--------|------|
| `backend.Dockerfile` | `Dockerfile.prod` | 生产环境后端镜像 |
| `backend-dev.Dockerfile` | `Dockerfile.dev` | 开发环境后端镜像 |
| - | `Dockerfile.test` | 测试环境后端镜像（新增） |
| `frontend-dev.Dockerfile` | `Dockerfile.frontend-dev` | 前端开发镜像 |

### 3. 统一环境管理脚本

#### 新增脚本

- `scripts/env-start.sh` - 统一环境启动脚本
- `scripts/env-stop.sh` - 统一环境停止脚本
- `scripts/env-reset.sh` - 统一环境重置脚本

#### 更新的脚本

- `scripts/reset-db.sh` - 支持测试环境（新增 `--test` 参数）
- `scripts/dev-start.sh` - 标记为已弃用，自动调用新脚本

### 4. 容器命名规范

所有容器使用统一前缀 `law-firm-`：

- **开发环境**: `law-firm-postgres`, `law-firm-redis`, `law-firm-minio` 等
- **测试环境**: `law-firm-test-postgres`, `law-firm-test-redis` 等（新增 `-test-` 后缀）
- **生产环境**: `law-firm-postgres`, `law-firm-redis` 等

### 5. 网络命名规范

所有环境使用统一网络名：`law-firm-network`

- 统一了 `docker-compose.dev-full.yml` 的网络名称（从 `law-dev-net` 改为 `law-firm-network`）

### 6. 数据卷命名规范

- **开发环境**: `postgres_data`, `redis_data`, `minio_data` 等
- **测试环境**: `postgres_test_data`, `redis_test_data`, `minio_test_data` 等（新增 `_test_` 后缀）
- **生产环境**: `postgres_data`, `redis_data`, `minio_data` 等

### 7. 数据库命名规范

- **开发环境**: `law_firm_dev`
- **测试环境**: `law_firm_test`（新增）
- **生产环境**: `law_firm`

### 8. 端口配置规范

#### 开发环境
- PostgreSQL: 5432
- Redis: 6379
- MinIO API: 9000
- MinIO Console: 9001
- OnlyOffice: 8088

#### 测试环境（避免与开发环境冲突）
- PostgreSQL: 5433
- Redis: 6380
- MinIO API: 9002
- MinIO Console: 9003
- OnlyOffice: 8089

#### 生产环境
- 根据 `.env` 配置

## 📝 使用示例

### 开发环境

```bash
# 启动
./scripts/env-start.sh dev
./scripts/env-start.sh dev --full  # 全量服务

# 停止
./scripts/env-stop.sh dev

# 重置
./scripts/env-reset.sh dev

# 数据库初始化
./scripts/reset-db.sh --dev
```

### 测试环境

```bash
# 启动
./scripts/env-start.sh test

# 停止
./scripts/env-stop.sh test

# 重置
./scripts/env-reset.sh test

# 数据库初始化
./scripts/reset-db.sh --test
```

### 生产环境

```bash
# 启动（需要 .env 文件）
./scripts/env-start.sh prod

# 停止
./scripts/env-stop.sh prod

# 数据库初始化
./scripts/reset-db.sh --prod
```

## 🔄 迁移指南

### 从旧配置迁移

1. **更新脚本引用**:
   - 将 `docker-compose.yml` 改为 `docker-compose.dev.yml`
   - 将 `backend.Dockerfile` 改为 `Dockerfile.prod`
   - 将 `backend-dev.Dockerfile` 改为 `Dockerfile.dev`

2. **使用新的统一脚本**:
   - 替换 `./scripts/dev-start.sh` 为 `./scripts/env-start.sh dev`
   - 使用 `./scripts/env-stop.sh` 和 `./scripts/env-reset.sh`

3. **更新 CI/CD 配置**:
   - 更新构建脚本中的 Dockerfile 路径
   - 更新部署脚本中的 docker-compose 文件路径

## 📚 相关文档

- [环境配置说明](ENVIRONMENT_CONFIGURATION.md)
- [部署指南](../docker/DEPLOY.md)
- [README](../README.md)

## ⚠️ 注意事项

1. **向后兼容**: `dev-start.sh` 脚本已更新为自动调用新脚本，保持向后兼容
2. **数据迁移**: 重置环境会删除所有数据，请确保已备份
3. **端口冲突**: 测试环境使用不同端口，可与开发环境同时运行
4. **生产环境**: 生产环境必须使用 `.env` 文件配置强密码

## 🎯 统一化收益

1. **一致性**: 所有环境使用统一的命名和配置规范
2. **可维护性**: 统一的脚本和配置文件，易于维护
3. **可扩展性**: 新增环境只需遵循统一规范
4. **易用性**: 统一的命令接口，降低学习成本
5. **隔离性**: 不同环境完全隔离，互不影响

## 📋 检查清单

- [x] Docker Compose 文件命名统一
- [x] Dockerfile 命名统一
- [x] 容器命名统一
- [x] 网络命名统一
- [x] 数据卷命名统一
- [x] 数据库命名统一
- [x] 端口配置规范
- [x] 统一环境管理脚本
- [x] 更新相关文档
- [x] 向后兼容性保证
