# 容器间网络通信检查报告

## ✅ 网络配置总体评估

**结论**: 容器间网络通信配置**正确**，所有服务都在同一 Docker 网络中，使用服务名进行通信。

---

## 🌐 网络架构

### Docker 网络配置

```yaml
networks:
  law-firm-network:
    driver: bridge
```

**所有容器都连接到 `law-firm-network` 网络** ✅

---

## 📡 容器间通信路径

### 1. 前端 → 后端

**配置位置**: `frontend/scripts/deploy/nginx.conf`

```nginx
location /api/ {
  proxy_pass http://backend:8080/api/;
}
```

- ✅ **状态**: 正确
- ✅ **服务名**: `backend`
- ✅ **端口**: `8080`
- ✅ **协议**: HTTP

### 2. 前端 → OnlyOffice

**配置位置**: `frontend/scripts/deploy/nginx.conf`

```nginx
location /onlyoffice/ {
  proxy_pass http://onlyoffice:80/;
}
```

- ✅ **状态**: 正确
- ✅ **服务名**: `onlyoffice`
- ✅ **端口**: `80`
- ✅ **协议**: HTTP
- ✅ **WebSocket 支持**: 已配置（OnlyOffice 实时协作需要）

### 3. 后端 → PostgreSQL

**配置位置**: `docker-compose.prod.yml`

```yaml
environment:
  - DB_HOST=postgres
  - DB_PORT=5432
```

- ✅ **状态**: 正确
- ✅ **服务名**: `postgres`
- ✅ **端口**: `5432`
- ✅ **依赖关系**: 配置了健康检查等待

### 4. 后端 → Redis

**配置位置**: `docker-compose.prod.yml`

```yaml
environment:
  - REDIS_HOST=redis
  - REDIS_PORT=6379
```

- ✅ **状态**: 正确
- ✅ **服务名**: `redis`
- ✅ **端口**: `6379`
- ✅ **依赖关系**: 配置了健康检查等待

### 5. 后端 → MinIO

**配置位置**: `docker-compose.prod.yml`

```yaml
environment:
  - MINIO_ENDPOINT=http://minio:9000
  - MINIO_EXTERNAL_ENDPOINT=http://minio:9000
  - FILE_SERVER_URL=http://minio:9000
```

- ✅ **状态**: 正确
- ✅ **服务名**: `minio`
- ✅ **端口**: `9000`
- ✅ **协议**: HTTP

### 6. 后端 → OnlyOffice（回调）

**配置位置**: `docker-compose.prod.yml` 和 `application-prod.yml`

```yaml
# docker-compose.prod.yml
environment:
  - ONLYOFFICE_CALLBACK_URL=http://backend:8080/api

# application-prod.yml
onlyoffice:
  callback-url: ${ONLYOFFICE_CALLBACK_URL:http://backend:8080/api}
```

- ✅ **状态**: 正确
- ✅ **说明**: OnlyOffice 保存文档时回调后端
- ✅ **服务名**: `backend`
- ✅ **端口**: `8080`

### 7. MinIO Init → MinIO

**配置位置**: `docker-compose.prod.yml`

```yaml
entrypoint: >
  /bin/sh -c "
  mc alias set local http://minio:9000 ...
  "
```

- ✅ **状态**: 正确
- ✅ **服务名**: `minio`
- ✅ **端口**: `9000`

### 8. Prometheus → Backend

**配置位置**: `docker/prometheus/prometheus.yml`

```yaml
scrape_configs:
  - job_name: 'law-firm-backend'
    static_configs:
      - targets: ['backend:8080']
```

- ✅ **状态**: 正确
- ✅ **服务名**: `backend`
- ✅ **端口**: `8080`
- ✅ **路径**: `/api/actuator/prometheus`

---

## ⚠️ 潜在问题

### 1. OCR 服务已停用（不影响主要功能）

**问题**: 后端配置了 OCR 服务 URL，但服务已停用

```yaml
# docker-compose.prod.yml
environment:
  - OCR_SERVICE_URL=http://paddle-ocr:8000

# 但 paddle-ocr 服务已被注释掉
# paddle-ocr:
#   ...
```

**影响**: 
- ⚠️ 如果后端代码尝试连接 OCR 服务，可能会失败
- ✅ 不影响主要功能（OCR 服务已停用）

**建议**: 
- 如果不需要 OCR 功能，可以忽略
- 如果需要 OCR 功能，需要取消注释并启动服务

### 2. OnlyOffice URL 配置

**当前配置**:
```yaml
# application-prod.yml
document-server-url: ${ONLYOFFICE_URL:http://localhost/onlyoffice}
```

**说明**:
- ✅ 生产环境应通过环境变量 `ONLYOFFICE_URL` 设置正确的域名
- ✅ 如果使用默认值 `localhost`，前端会自动检测当前域名并替换
- ✅ 前端通过 Nginx 代理访问：`/onlyoffice/` -> `onlyoffice:80/`

**建议**:
- 如果使用域名，设置 `ONLYOFFICE_URL=http://yourdomain.com/onlyoffice`
- 如果使用 IP，设置 `ONLYOFFICE_URL=http://your-ip/onlyoffice`

---

## 🔍 依赖关系检查

### 启动顺序

```yaml
# 1. 基础设施服务（无依赖）
postgres:  # 健康检查
redis:     # 健康检查
minio:     # 无健康检查

# 2. 初始化服务
minio-init:  # 依赖 minio（通过 sleep 5 等待）

# 3. 应用服务（依赖基础设施）
backend:     # depends_on: postgres (healthy), redis (healthy)
onlyoffice:  # 无依赖
frontend:    # depends_on: backend

# 4. 监控服务（无依赖）
prometheus:  # 无依赖
grafana:     # 无依赖
```

**依赖关系配置** ✅:
- `frontend` → `backend` ✅
- `backend` → `postgres` (with healthcheck) ✅
- `backend` → `redis` (with healthcheck) ✅

---

## 🧪 网络通信测试

### 测试方法

```bash
# 1. 进入后端容器测试数据库连接
docker exec -it law-firm-backend sh
ping postgres
nc -zv postgres 5432

# 2. 测试 Redis 连接
ping redis
nc -zv redis 6379

# 3. 测试 MinIO 连接
ping minio
curl http://minio:9000/minio/health/live

# 4. 测试 OnlyOffice 连接
ping onlyoffice
curl http://onlyoffice/healthcheck

# 5. 从前端容器测试后端
docker exec -it law-firm-frontend sh
ping backend
curl http://backend:8080/api/actuator/health
```

### 预期结果

所有测试应该成功：
- ✅ DNS 解析正常（ping 成功）
- ✅ 端口连接正常（nc/curl 成功）
- ✅ 服务响应正常（HTTP 200）

---

## 📊 网络通信总结

| 通信路径 | 服务名 | 端口 | 状态 | 说明 |
|---------|--------|------|------|------|
| frontend → backend | `backend` | 8080 | ✅ | Nginx 代理 |
| frontend → onlyoffice | `onlyoffice` | 80 | ✅ | Nginx 代理 |
| backend → postgres | `postgres` | 5432 | ✅ | 数据库连接 |
| backend → redis | `redis` | 6379 | ✅ | 缓存连接 |
| backend → minio | `minio` | 9000 | ✅ | 对象存储 |
| onlyoffice → backend | `backend` | 8080 | ✅ | 回调接口 |
| minio-init → minio | `minio` | 9000 | ✅ | 初始化 |
| prometheus → backend | `backend` | 8080 | ✅ | 指标收集 |
| backend → paddle-ocr | `paddle-ocr` | 8000 | ⚠️ | 服务已停用 |

---

## ✅ 结论

**容器间网络通信配置正确** ✅

### 优点

1. ✅ **统一网络**: 所有容器在同一 Docker 网络中
2. ✅ **服务发现**: 使用服务名进行 DNS 解析
3. ✅ **依赖管理**: 正确配置了服务依赖和健康检查
4. ✅ **代理配置**: Nginx 反向代理配置正确
5. ✅ **WebSocket 支持**: OnlyOffice 实时协作已配置

### 建议

1. ⚠️ **OCR 服务**: 如果不需要，可以从配置中移除 `OCR_SERVICE_URL`
2. ✅ **OnlyOffice URL**: 生产环境建议通过环境变量设置正确的域名
3. ✅ **监控**: Prometheus 和 Grafana 配置正确，可以正常收集指标

---

## 🔗 相关文档

- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
- [生产环境配置检查报告](./PRODUCTION_CONFIG_CHECK_REPORT.md)
- [部署脚本使用指南](./DEPLOYMENT_SCRIPTS_GUIDE.md)
