# 单端口架构

## 概述

系统采用**单端口架构**，仅暴露 HTTP（80）和 HTTPS（443）端口，所有其他服务通过 Nginx 路径代理访问，提高安全性和简化运维。

## 架构优势

### ✅ 安全性提升

- **最小化攻击面**：只暴露必要的 HTTP/HTTPS 端口
- **统一 SSL 管理**：所有服务通过 Nginx 统一处理 SSL/TLS
- **防火墙简化**：只需开放 80 和 443 端口

### ✅ 运维简化

- **端口管理简单**：不需要管理多个端口映射
- **统一访问入口**：所有服务通过统一域名访问
- **配置集中化**：Nginx 配置集中管理

## 端口配置

### 暴露的端口（仅 2 个）

| 端口 | 服务 | 说明 |
|------|------|------|
| **80** | Frontend (Nginx) | HTTP 访问 |
| **443** | Frontend (Nginx) | HTTPS 访问 |

**✅ 其他端口全部不暴露！**

### 通过 Nginx 路径访问的服务

| 路径 | 服务 | 原端口 | 说明 |
|------|------|--------|------|
| `/minio/` | MinIO API | 9000 | 文件存储 API |
| `/minio-console/` | MinIO Console | 9001 | MinIO 管理控制台 |
| `/onlyoffice/` | OnlyOffice | 80 | 文档编辑服务 |
| `/prometheus/` | Prometheus | 9090 | 监控数据收集（可选） |
| `/grafana/` | Grafana | 3000 | 监控可视化（可选） |

### 不暴露端口的服务

| 服务 | 端口 | 访问方式 | 维护方式 |
|------|------|---------|---------|
| **PostgreSQL** | 5432 | Docker 内部网络 | ✅ 服务器上执行 `docker exec` |
| **Redis** | 6379 | Docker 内部网络 | ✅ Docker 内部访问 |
| **MinIO** | 9000, 9001 | Nginx 路径代理 | ✅ 通过 `/minio/` 和 `/minio-console/` |
| **OnlyOffice** | 80 | Nginx 路径代理 | ✅ 通过 `/onlyoffice/` |
| **Prometheus** | 9090 | Nginx 路径代理 | ✅ 通过 `/prometheus/` |
| **Grafana** | 3000 | Nginx 路径代理 | ✅ 通过 `/grafana/` |

## 访问地址

### 生产环境

- 🌐 **主应用**: `https://your-domain.com/`
- 📚 **文档站点**: `https://your-domain.com/docs/`
- 🔧 **API 地址**: `https://your-domain.com/api`
- 📦 **MinIO 控制台**: `https://your-domain.com/minio-console/`
- 📝 **OnlyOffice**: `https://your-domain.com/onlyoffice/`（通过应用调用）
- 📊 **Prometheus**: `https://your-domain.com/prometheus/`（可选）
- 📈 **Grafana**: `https://your-domain.com/grafana/`（可选）

### 开发环境

- 🌐 **主应用**: `http://localhost/`
- 📚 **文档站点**: `http://localhost/docs/`
- 🔧 **API 地址**: `http://localhost/api`
- 📦 **MinIO 控制台**: `http://localhost/minio-console/`

## 配置说明

### 环境变量

单端口架构需要以下环境变量配置：

```bash
# MinIO 配置（相对路径）
MINIO_BROWSER_ENDPOINT=/minio

# OnlyOffice 配置（相对路径）
ONLYOFFICE_URL=/onlyoffice
ONLYOFFICE_CALLBACK_URL=http://backend:8080/api
FILE_SERVER_URL=http://minio:9000
```

### Nginx 配置

Nginx 配置位于 `frontend/scripts/deploy/nginx-ssl.conf`，包含以下路径代理：

- `/minio/` → MinIO API (9000)
- `/minio-console/` → MinIO Console (9001)
- `/onlyoffice/` → OnlyOffice (80)
- `/prometheus/` → Prometheus (9090)
- `/grafana/` → Grafana (3000)

## 数据库访问

### 为什么数据库端口不暴露？

- ✅ **安全性**：数据库端口不暴露到公网，降低被攻击风险
- ✅ **标准做法**：这是生产环境的标准安全实践
- ✅ **不影响维护**：通过 `docker exec` 命令可以正常维护

### 如何访问数据库？

#### 方法 1：服务器上直接访问（推荐）

```bash
# 进入数据库容器
docker exec -it law-firm-postgres psql -U law_admin -d law_firm

# 执行 SQL 命令
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT version();"

# 备份数据库
docker exec law-firm-postgres pg_dump -U law_admin law_firm > backup.sql
```

#### 方法 2：SSH 隧道（远程访问）

如果需要从本地访问远程数据库：

```bash
# 建立 SSH 隧道
ssh -L 5432:localhost:5432 user@your-server-ip

# 然后可以在本地使用数据库客户端连接 localhost:5432
```

## MinIO 访问

### MinIO API

- **访问路径**: `/minio/`
- **用途**: 文件上传、下载、缩略图等
- **示例**: `https://your-domain.com/minio/law-firm/documents/file.pdf`

### MinIO Console

- **访问路径**: `/minio-console/`
- **用途**: MinIO 管理控制台
- **登录**: 使用 `.env` 文件中的 `MINIO_ACCESS_KEY` 和 `MINIO_SECRET_KEY`

## OnlyOffice 集成

### 工作原理

1. **前端调用**: 用户点击"编辑"按钮
2. **后端配置**: 后端生成 OnlyOffice 配置（包含文档 URL 和回调 URL）
3. **OnlyOffice 加载**: OnlyOffice 通过 `/onlyoffice/` 路径加载
4. **文件获取**: OnlyOffice 通过后端代理获取文件（`/api/document/{id}/file-proxy`）
5. **保存回调**: OnlyOffice 编辑完成后回调后端保存文件

### 关键配置

- **OnlyOffice URL**: `/onlyoffice`（相对路径，通过 Nginx 代理）
- **回调 URL**: `http://backend:8080/api`（Docker 内部地址）
- **文件服务器**: `http://law-firm-minio:9000`（Docker 内部地址）

## 监控服务（可选）

### Prometheus

- **访问路径**: `/prometheus/`
- **启用方式**: `docker compose --profile monitoring up -d`
- **用途**: 监控数据收集和查询

### Grafana

- **访问路径**: `/grafana/`
- **启用方式**: `docker compose --profile monitoring up -d`
- **用途**: 监控数据可视化
- **默认账号**: `admin` / `admin`（首次登录需修改）

## 迁移指南

如果你是从多端口架构迁移到单端口架构，请参考：

- [单端口架构迁移指南](/guide/ops/single-port-migration)
- [部署检查清单](/guide/ops/deployment-checklist)

## 常见问题

### Q: 为什么不能直接访问 MinIO 的 9000 端口？

A: 单端口架构下，MinIO 端口不暴露，必须通过 Nginx 路径 `/minio/` 访问。这样可以：
- 统一 SSL 管理
- 提高安全性
- 简化防火墙配置

### Q: 数据库不暴露端口，如何维护？

A: 通过 `docker exec` 命令在服务器上直接访问数据库容器，这是标准做法。参考[数据库维护说明](/guide/ops/database-maintenance)。

### Q: OnlyOffice 编辑文档失败怎么办？

A: 检查以下几点：
1. OnlyOffice 容器是否正常运行
2. Nginx 配置中 `/onlyoffice/` 路径是否正确
3. 后端配置中 `ONLYOFFICE_URL` 是否为 `/onlyoffice`
4. 运行测试脚本：`./scripts/test-onlyoffice-minio-integration.sh`

## 相关文档

- [部署指南](/guide/ops/deployment)
- [配置说明](/guide/ops/configuration)
- [OnlyOffice 配置](/guide/ops/onlyoffice)
- [监控告警](/guide/ops/monitoring)
- [故障排查](/guide/ops/troubleshooting)

---

**最后更新**: 2026-01-31
