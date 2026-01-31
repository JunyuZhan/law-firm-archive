# 最终配置总结

**配置完成时间：** 2026-01-31  
**架构模式：** 单端口架构（80/443）

---

## ✅ 最终配置状态

### 1. 暴露的端口（仅 2 个）

| 端口 | 服务 | 说明 |
|------|------|------|
| **80** | Frontend (Nginx) | HTTP 访问 |
| **443** | Frontend (Nginx) | HTTPS 访问 |

**✅ 其他端口全部不暴露！**

### 2. 通过 Nginx 路径访问的服务

| 路径 | 服务 | 原端口 | 状态 |
|------|------|--------|------|
| `/minio/` | MinIO API | 9000 | ✅ 已配置 |
| `/minio-console/` | MinIO Console | 9001 | ✅ 已配置 |
| `/onlyoffice/` | OnlyOffice | 8088 | ✅ 已配置 |
| `/prometheus/` | Prometheus | 9090 | ✅ 已配置（可选） |
| `/grafana/` | Grafana | 3000 | ✅ 已配置（可选） |

### 3. 不暴露端口的服务

| 服务 | 端口 | 访问方式 | 维护方式 |
|------|------|---------|---------|
| **PostgreSQL** | 5432 | Docker 内部网络 | ✅ 服务器上执行 `docker exec` |
| **Redis** | 6379 | Docker 内部网络 | ✅ 服务器上执行 `docker exec` |
| **MinIO** | 9000, 9001 | Nginx 路径代理 | ✅ 通过 `/minio/` 和 `/minio-console/` |
| **OnlyOffice** | 80 | Nginx 路径代理 | ✅ 通过 `/onlyoffice/` |
| **Prometheus** | 9090 | Nginx 路径代理 | ✅ 通过 `/prometheus/` |
| **Grafana** | 3000 | Nginx 路径代理 | ✅ 通过 `/grafana/` |

---

## 🎯 配置决策

### ✅ 保持当前配置（推荐）

**数据库：**
- ✅ **不暴露端口**（当前状态）
- ✅ 通过服务器上的 `docker exec` 命令维护
- ✅ 备份、导出、恢复都在服务器上完成
- ✅ 更安全，这是标准做法

**MinIO：**
- ✅ **不暴露端口**（已修改）
- ✅ 通过 Nginx 路径 `/minio/` 和 `/minio-console/` 访问
- ✅ 统一 SSL 管理，更安全

**OnlyOffice：**
- ✅ **不暴露端口**（已修改）
- ✅ 通过 Nginx 路径 `/onlyoffice/` 访问
- ✅ 统一 SSL 管理，更安全

---

## 📋 最终配置清单

### ✅ 已完成的修改

1. **Nginx 配置**
   - ✅ 添加了 `/minio-console/` location（HTTP + HTTPS）
   - ✅ 添加了 `/prometheus/` location（HTTP + HTTPS，可选）
   - ✅ 添加了 `/grafana/` location（HTTP + HTTPS，可选）

2. **Docker Compose 配置**
   - ✅ 移除了 MinIO 端口映射（9000, 9001）
   - ✅ 移除了 OnlyOffice 端口映射（8088）
   - ✅ 移除了 Prometheus 端口映射（9090）
   - ✅ 移除了 Grafana 端口映射（3000）
   - ✅ 添加了 Prometheus `--web.external-url=/prometheus/`
   - ✅ 添加了 Grafana 子路径配置

3. **环境变量配置**
   - ✅ 更新了 `MINIO_BROWSER_ENDPOINT=/minio`
   - ✅ 添加了 MinIO 相关配置到 `.env`

4. **数据库配置**
   - ✅ **保持不暴露端口**（正确配置）
   - ✅ 通过服务器上的工具维护

### ✅ 保持不变的配置

1. **数据库端口**
   - ✅ 不暴露端口（标准做法）
   - ✅ 通过 `docker exec` 在服务器上维护

2. **Redis 端口**
   - ✅ 不暴露端口（标准做法）
   - ✅ 通过 Docker 内部网络访问

---

## 🚀 部署后的访问方式

### 用户访问

```
http://your-domain/              → 主应用
http://your-domain/docs/         → 文档站点
http://your-domain/minio-console/ → MinIO 管理（需要登录）
http://your-domain/onlyoffice/   → OnlyOffice（通过应用调用）
```

### 运维访问

**数据库维护：**
```bash
# SSH 到服务器
ssh user@your-server

# 备份
./scripts/db-auto-backup.sh

# 导出
docker exec law-firm-postgres pg_dump -U law_admin -d law_firm > backup.sql

# 查询
docker exec law-firm-postgres psql -U law_admin -d law_firm -c "SELECT ..."
```

**MinIO 管理：**
```
http://your-domain/minio-console/  # 通过浏览器访问
```

---

## ✅ 最终建议

### **保持当前配置（推荐）**

**理由：**
1. ✅ **安全性高**：只暴露 2 个端口（80, 443）
2. ✅ **统一管理**：所有服务通过 Nginx 统一入口
3. ✅ **标准做法**：符合生产环境最佳实践
4. ✅ **维护方便**：数据库通过服务器工具维护，操作简单
5. ✅ **性能良好**：Nginx 代理延迟可忽略不计

### 配置总结

| 服务 | 端口暴露 | 访问方式 | 维护方式 |
|------|---------|---------|---------|
| **Frontend** | ✅ 80, 443 | 直接访问 | Nginx 配置 |
| **Backend** | ❌ 不暴露 | Docker 内部 | 通过前端 API |
| **PostgreSQL** | ❌ 不暴露 | Docker 内部 | ✅ 服务器上 `docker exec` |
| **Redis** | ❌ 不暴露 | Docker 内部 | Docker 内部 |
| **MinIO** | ❌ 不暴露 | Nginx `/minio/` | Nginx `/minio-console/` |
| **OnlyOffice** | ❌ 不暴露 | Nginx `/onlyoffice/` | 通过应用调用 |

---

## 🎉 配置完成

**当前配置是最佳实践：**
- ✅ 只暴露必要的端口（80, 443）
- ✅ 数据库不暴露，通过服务器维护（安全、标准）
- ✅ MinIO 和 OnlyOffice 通过 Nginx 代理（统一、安全）
- ✅ 所有配置已完成，可以部署

**下一步：**
1. 部署到测试环境验证
2. 确认功能正常后部署到生产环境
