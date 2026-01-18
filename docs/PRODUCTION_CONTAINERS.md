# 生产环境容器清单

## 📊 容器统计

生产环境共有 **9 个服务容器**，其中 **8 个长期运行**，**1 个初始化容器**（执行完任务后退出）。

---

## 🐳 容器列表

### 1. 应用服务（2个）

| 容器名 | 服务名 | 说明 | 端口 | 内存限制 |
|--------|--------|------|------|----------|
| `law-firm-frontend` | `frontend` | 前端服务（Nginx + Vue3） | 80:8080 | 512M |
| `law-firm-backend` | `backend` | 后端服务（Spring Boot） | - | 2G |

### 2. 数据存储服务（3个）

| 容器名 | 服务名 | 说明 | 端口 | 数据卷 |
|--------|--------|------|------|--------|
| `law-firm-postgres` | `postgres` | PostgreSQL 数据库 | - | `postgres_data` |
| `law-firm-redis` | `redis` | Redis 缓存 | - | `redis_data` |
| `law-firm-minio` | `minio` | MinIO 对象存储 | 9001:9001 | `minio_data` |

### 3. 文档服务（1个）

| 容器名 | 服务名 | 说明 | 端口 | 数据卷 |
|--------|--------|------|------|--------|
| `law-firm-onlyoffice` | `onlyoffice` | OnlyOffice 文档服务器 | 8088:80 | `onlyoffice_data`, `onlyoffice_logs`, `onlyoffice_cache` |

### 4. 监控服务（2个）

| 容器名 | 服务名 | 说明 | 端口 | 数据卷 |
|--------|--------|------|------|--------|
| `law-firm-prometheus` | `prometheus` | Prometheus 监控 | 9090:9090 | `prometheus_data` |
| `law-firm-grafana` | `grafana` | Grafana 可视化 | 3000:3000 | `grafana_data` |

### 5. 初始化容器（1个，执行完退出）

| 容器名 | 服务名 | 说明 | 状态 |
|--------|--------|------|------|
| `law-firm-minio-init` | `minio-init` | MinIO 初始化（创建 bucket） | 执行完退出 |

---

## 📋 详细说明

### 应用服务

#### 1. Frontend（前端）
- **容器名**: `law-firm-frontend`
- **镜像**: 从 `frontend/scripts/deploy/Dockerfile` 构建
- **功能**: 
  - 提供 Vue3 前端静态文件
  - Nginx 反向代理
  - API 代理到后端
  - OnlyOffice 代理
- **端口**: `80:8080`
- **依赖**: `backend`

#### 2. Backend（后端）
- **容器名**: `law-firm-backend`
- **镜像**: 从 `docker/Dockerfile.prod` 构建
- **功能**: Spring Boot 应用服务
- **端口**: 内部 8080（通过前端代理）
- **依赖**: `postgres`, `redis`
- **健康检查**: `/api/actuator/health`

### 数据存储服务

#### 3. PostgreSQL（数据库）
- **容器名**: `law-firm-postgres`
- **镜像**: `postgres:15-alpine`
- **功能**: 主数据库
- **数据卷**: `postgres_data`
- **初始化**: 自动执行 `scripts/init-db/` 下的 SQL 脚本
- **健康检查**: `pg_isready`

#### 4. Redis（缓存）
- **容器名**: `law-firm-redis`
- **镜像**: `redis:7-alpine`
- **功能**: 缓存和会话存储
- **数据卷**: `redis_data`
- **持久化**: AOF（appendonly yes）
- **健康检查**: `redis-cli ping`

#### 5. MinIO（对象存储）
- **容器名**: `law-firm-minio`
- **镜像**: `minio/minio:RELEASE.2024-12-13T22-19-12Z`
- **功能**: 文件对象存储
- **端口**: `9001:9001`（控制台）
- **数据卷**: `minio_data`
- **Bucket**: `law-firm`（由 minio-init 创建）

### 文档服务

#### 6. OnlyOffice（文档服务器）
- **容器名**: `law-firm-onlyoffice`
- **镜像**: 从 `docker/onlyoffice/Dockerfile` 构建
- **功能**: 在线文档编辑和预览
- **端口**: `8088:80`
- **数据卷**: 
  - `onlyoffice_data` - 数据目录
  - `onlyoffice_logs` - 日志目录
  - `onlyoffice_cache` - 缓存目录
- **健康检查**: `/healthcheck`

### 监控服务

#### 7. Prometheus（监控）
- **容器名**: `law-firm-prometheus`
- **镜像**: `prom/prometheus:latest`
- **功能**: 指标收集和存储
- **端口**: `9090:9090`
- **数据卷**: `prometheus_data`
- **配置**: `docker/prometheus/prometheus.yml`

#### 8. Grafana（可视化）
- **容器名**: `law-firm-grafana`
- **镜像**: `grafana/grafana:latest`
- **功能**: 监控数据可视化
- **端口**: `3000:3000`
- **数据卷**: `grafana_data`
- **配置**: `docker/grafana/provisioning/`

### 初始化容器

#### 9. MinIO Init（初始化）
- **容器名**: `law-firm-minio-init`
- **镜像**: `minio/mc:latest`
- **功能**: 初始化 MinIO bucket
- **执行任务**:
  1. 等待 MinIO 服务启动（sleep 5）
  2. 配置 MinIO 别名
  3. 创建 `law-firm` bucket
  4. 设置 bucket 为公开访问
- **状态**: 执行完任务后退出（exit 0）

---

## 🔄 容器启动顺序

```
1. postgres (等待健康检查通过)
2. redis (等待健康检查通过)
3. minio (启动)
4. minio-init (执行初始化后退出)
5. backend (依赖 postgres 和 redis)
6. onlyoffice (启动)
7. frontend (依赖 backend)
8. prometheus (启动)
9. grafana (启动)
```

---

## 📊 资源统计

### 内存使用

| 服务 | 内存限制 | 说明 |
|------|----------|------|
| frontend | 512M | 前端静态文件 |
| backend | 2G | Spring Boot 应用 |
| postgres | 无限制 | 数据库（建议至少 1G） |
| redis | 无限制 | 缓存（建议至少 512M） |
| minio | 无限制 | 对象存储（根据数据量） |
| onlyoffice | 无限制 | 文档服务（建议至少 1G） |
| prometheus | 无限制 | 监控（建议至少 512M） |
| grafana | 无限制 | 可视化（建议至少 256M） |

**总计内存建议**: 至少 **6GB**（不包括数据存储）

### 端口占用

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 | frontend | 主应用入口 |
| 3000 | grafana | 监控面板 |
| 8088 | onlyoffice | 文档服务（直接访问） |
| 9001 | minio | MinIO 控制台 |
| 9090 | prometheus | Prometheus 面板 |

---

## 🚫 已停用的服务

### PaddleOCR（已注释）

```yaml
# paddle-ocr 服务已停用，如需启用请取消注释
# paddle-ocr:
#   ...
```

**说明**: OCR 服务已停用，如需启用请取消注释相关配置。

---

## 🔍 查看容器状态

### 查看所有容器

```bash
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml ps
```

### 查看容器日志

```bash
# 查看所有服务日志
docker compose --env-file ../.env -f docker-compose.prod.yml logs -f

# 查看特定服务日志
docker compose --env-file ../.env -f docker-compose.prod.yml logs -f backend
```

### 查看容器资源使用

```bash
docker stats
```

---

## 📝 总结

- **总容器数**: 9 个服务
- **长期运行**: 8 个容器
- **初始化容器**: 1 个（minio-init，执行完退出）
- **最小内存要求**: 6GB
- **端口占用**: 5 个端口（80, 3000, 8088, 9001, 9090）

---

## 🔗 相关文档

- [生产环境配置检查报告](./PRODUCTION_CONFIG_CHECK_REPORT.md)
- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md)
- [环境配置统一说明](./ENVIRONMENT_CONFIGURATION.md)
