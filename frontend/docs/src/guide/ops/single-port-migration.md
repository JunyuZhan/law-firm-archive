# 单端口架构迁移指南

## 概述

本指南详细说明如何从多端口架构迁移到单端口架构，包括配置修改、验证步骤和常见问题。

## 迁移前准备

### 1. 备份当前配置

```bash
# 备份 docker-compose 配置
cp docker/docker-compose.prod.yml docker/docker-compose.prod.yml.backup

# 备份 Nginx 配置
cp frontend/scripts/deploy/nginx-ssl.conf frontend/scripts/deploy/nginx-ssl.conf.backup

# 备份环境变量
cp .env .env.backup
```

### 2. 检查当前状态

```bash
# 查看当前端口映射
docker ps --format "table {{.Names}}\t{{.Ports}}"

# 检查服务状态
docker compose -f docker/docker-compose.prod.yml ps
```

## 迁移步骤

### 步骤 1：更新 Docker Compose 配置

移除以下服务的端口映射：

- `minio`: 移除 `9000:9000` 和 `9001:9001`
- `onlyoffice`: 移除 `8088:80`
- `prometheus`: 移除 `9090:9090`（如果启用）
- `grafana`: 移除 `3000:3000`（如果启用）

**配置文件**: `docker/docker-compose.prod.yml`

### 步骤 2：配置 Prometheus 和 Grafana（如果启用监控）

在 `docker-compose.prod.yml` 中：

**Prometheus**:

```yaml
command:
  - '--web.external-url=/prometheus/'
```

**Grafana**:

```yaml
environment:
  - GF_SERVER_ROOT_URL=/grafana/
  - GF_SERVER_SERVE_FROM_SUB_PATH=true
```

### 步骤 3：更新 Nginx 配置

在 `frontend/scripts/deploy/nginx-ssl.conf` 中添加路径代理：

- `/minio/` → `http://law-firm-minio:9000/`
- `/minio-console/` → `http://law-firm-minio:9001/`
- `/onlyoffice/` → `http://law-firm-onlyoffice/`
- `/prometheus/` → `http://law-firm-prometheus:9090/`
- `/grafana/` → `http://law-firm-grafana:3000/`

### 步骤 4：更新环境变量

在 `.env` 文件中：

```bash
# MinIO 配置（改为相对路径）
MINIO_BROWSER_ENDPOINT=/minio

# OnlyOffice 配置（相对路径）
ONLYOFFICE_URL=/onlyoffice
```

### 步骤 5：重新构建和部署

```bash
# 停止当前服务
docker compose -f docker/docker-compose.prod.yml down

# 重新构建镜像
docker compose -f docker/docker-compose.prod.yml build

# 启动服务
docker compose --env-file .env -f docker/docker-compose.prod.yml --profile docs --profile monitoring up -d
```

## 验证步骤

### 1. 检查端口映射

```bash
# 应该只有 80 和 443 端口
docker ps --format "table {{.Names}}\t{{.Ports}}" | grep -E "80|443"
```

**预期结果**:

- Frontend: `0.0.0.0:80->8080/tcp, 0.0.0.0:443->8443/tcp`
- 其他容器：不应该有端口映射

### 2. 验证路径访问

```bash
# 检查 MinIO Console
curl -I http://localhost/minio-console/

# 检查 OnlyOffice（需要认证）
curl -I http://localhost/onlyoffice/

# 检查 Prometheus（如果启用）
curl -I http://localhost/prometheus/

# 检查 Grafana（如果启用）
curl -I http://localhost/grafana/
```

### 3. 测试 OnlyOffice 和 MinIO 集成

```bash
# 运行集成测试脚本
./scripts/test-onlyoffice-minio-integration.sh
```

### 4. 手动测试文档编辑

1. 登录系统
2. 进入卷宗管理
3. 上传一个 DOC 文件
4. 点击"编辑"按钮
5. 验证 OnlyOffice 编辑器正常加载
6. 修改文档内容
7. 点击"保存"
8. 验证文件已更新

## 关键配置检查

### 容器间通信

确保以下配置使用 Docker 内部地址：

- **OnlyOffice → Backend**: `http://backend:8080/api`
- **Backend → MinIO**: `http://law-firm-minio:9000`
- **Backend → OnlyOffice**: `http://law-firm-onlyoffice/`
- **Grafana → Prometheus**: `http://law-firm-prometheus:9090`

### Nginx 代理配置

确保以下配置正确：

- **MinIO 代理**: `proxy_set_header Host law-firm-minio:9000;`
- **OnlyOffice 代理**: `proxy_set_header Authorization $http_authorization;`
- **WebSocket 支持**: 已配置 `Upgrade` 和 `Connection` 头

## 常见问题

### Q: MinIO 缩略图无法显示

A: 检查 `MINIO_BROWSER_ENDPOINT` 是否设置为 `/minio`，并且 Nginx 配置中 `/minio/` 路径的 `Host` 头是否正确。

### Q: OnlyOffice 编辑器无法加载

A: 检查：

1. OnlyOffice 容器是否正常运行
2. Nginx `/onlyoffice/` 路径配置是否正确
3. 后端配置中 `ONLYOFFICE_URL` 是否为 `/onlyoffice`

### Q: Prometheus/Grafana 无法访问

A: 检查：

1. 是否启用了 `--profile monitoring`
2. Prometheus `--web.external-url` 是否配置为 `/prometheus/`
3. Grafana `GF_SERVER_ROOT_URL` 是否配置为 `/grafana/`

### Q: 数据库无法连接

A: 数据库端口不暴露是正常的。使用 `docker exec` 命令访问：

```bash
docker exec -it law-firm-postgres psql -U law_admin -d law_firm
```

## 回滚方案

如果迁移出现问题，可以回滚：

```bash
# 恢复配置文件
cp docker/docker-compose.prod.yml.backup docker/docker-compose.prod.yml
cp frontend/scripts/deploy/nginx-ssl.conf.backup frontend/scripts/deploy/nginx-ssl.conf
cp .env.backup .env

# 重新部署
docker compose -f docker/docker-compose.prod.yml down
docker compose -f docker/docker-compose.prod.yml up -d --build
```

## 相关文档

- [单端口架构说明](/guide/ops/single-port-architecture)
- [部署检查清单](/guide/ops/deployment-checklist)
- [OnlyOffice 配置](/guide/ops/onlyoffice)
- [监控告警](/guide/ops/monitoring)

---

**最后更新**: 2026-01-31
