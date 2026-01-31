# 单端口架构迁移 - 实施完成报告

**实施时间：** 2026-01-31  
**状态：** ✅ 配置修改完成，待部署验证

---

## ✅ 已完成的修改

### 1. Nginx 配置更新

**文件：** `frontend/scripts/deploy/nginx-ssl.conf`

- ✅ 添加了 `/minio-console/` location（HTTP server 块，第 126-145 行）
- ✅ 添加了 `/minio-console/` location（HTTPS server 块，第 311-330 行）
- ✅ 配置了 WebSocket 支持（Console 需要）
- ✅ 配置了正确的代理头

**备份文件：** `frontend/scripts/deploy/nginx-ssl.conf.backup`

### 2. Docker Compose 配置更新

**文件：** `docker/docker-compose.prod.yml`

- ✅ 移除了 MinIO 端口映射（9000, 9001）
- ✅ 移除了 OnlyOffice 端口映射（8088）
- ✅ 移除了 Prometheus 端口映射（9090）
- ✅ 移除了 Grafana 端口映射（3000）
- ✅ 添加了 Prometheus `--web.external-url=/prometheus/` 参数
- ✅ 添加了 Grafana 子路径配置（`GF_SERVER_ROOT_URL`, `GF_SERVER_SERVE_FROM_SUB_PATH`）

**备份文件：** `docker/docker-compose.prod.yml.backup`

### 3. 环境变量配置更新

**文件：** `env.example` 和 `docker/env.example`

- ✅ 更新了 `MINIO_BROWSER_ENDPOINT` 从 `http://your-ip:9000` 改为 `/minio`
- ✅ 添加了注释说明单端口架构的使用方式

---

## 📋 部署前检查清单

在部署到生产环境前，请确认：

### 1. 环境变量配置

- [ ] **更新 `.env` 文件**（如果存在）
  ```bash
  # 将 MINIO_BROWSER_ENDPOINT 改为：
  MINIO_BROWSER_ENDPOINT=/minio
  ```

- [ ] **确认其他环境变量**（不需要修改）
  ```bash
  # 这些保持 Docker 内部地址，不需要修改：
  MINIO_ENDPOINT=http://minio:9000
  MINIO_EXTERNAL_ENDPOINT=http://minio:9000
  FILE_SERVER_URL=http://law-firm-minio:9000
  ONLYOFFICE_CALLBACK_URL=http://backend:8080/api
  ONLYOFFICE_URL=/onlyoffice  # 或完整 URL
  ```

### 2. 备份确认

- [ ] 确认备份文件已创建：
  - `frontend/scripts/deploy/nginx-ssl.conf.backup`
  - `docker/docker-compose.prod.yml.backup`

### 3. 部署步骤

```bash
# 1. 进入项目目录
cd /path/to/law-firm

# 2. 确认环境变量已更新
cat .env | grep MINIO_BROWSER_ENDPOINT
# 应该显示：MINIO_BROWSER_ENDPOINT=/minio

# 3. 停止现有服务
docker compose -f docker/docker-compose.prod.yml down

# 4. 重新构建前端镜像（包含新的 Nginx 配置）
docker compose -f docker/docker-compose.prod.yml build frontend

# 5. 启动服务
docker compose --env-file .env -f docker/docker-compose.prod.yml up -d

# 6. 检查服务状态
docker compose -f docker/docker-compose.prod.yml ps

# 7. 查看日志
docker compose -f docker/docker-compose.prod.yml logs -f frontend
```

---

## 🧪 测试验证步骤

部署完成后，请按以下步骤测试：

### 1. 端口验证

```bash
# 检查端口映射（应该只有 80 和 443）
docker ps --format "table {{.Names}}\t{{.Ports}}" | grep -E "80|443|9000|9001|8088|9090|3000"

# 预期结果：
# law-firm-frontend  0.0.0.0:80->8080/tcp, 0.0.0.0:443->8443/tcp
# 其他容器不应该有端口映射
```

### 2. MinIO API 测试

```bash
# 测试 MinIO API 路径代理
curl -I http://localhost/minio/law-firm/

# 预期结果：200 OK 或 403 Forbidden（需要认证）
```

### 3. MinIO Console 测试

```bash
# 测试 MinIO Console 路径代理
curl -I http://localhost/minio-console/

# 预期结果：200 OK 或 302 Redirect（到登录页）
```

**浏览器测试：**
- [ ] 打开 `http://your-domain/minio-console/`
- [ ] 应该显示 MinIO Console 登录页面
- [ ] 登录后可以正常使用控制台
- [ ] WebSocket 连接正常（检查浏览器 Network 标签）

### 4. OnlyOffice 测试

```bash
# 测试 OnlyOffice 健康检查
curl http://localhost/onlyoffice/healthcheck

# 预期结果：{"status": "ok"} 或类似响应
```

**浏览器测试：**
- [ ] 打开文档预览页面
- [ ] 点击"编辑"按钮
- [ ] OnlyOffice 编辑器应该正常加载
- [ ] 可以编辑文档
- [ ] 可以保存文档

### 5. 文件功能测试

- [ ] **文件上传**：上传一个文档，确认成功
- [ ] **文件下载**：下载文件，确认正常
- [ ] **缩略图显示**：查看文档列表，缩略图应该正常显示
- [ ] **预签名 URL**：检查生成的 URL 格式是否为 `/minio/...`

### 6. 容器间通信测试

```bash
# 进入后端容器
docker exec -it law-firm-backend sh

# 测试访问 MinIO（Docker 内部网络）
curl http://minio:9000/minio/health/live

# 测试访问 OnlyOffice（Docker 内部网络）
curl http://onlyoffice:80/healthcheck

# 退出容器
exit
```

**预期结果：** 所有测试都应该成功（不依赖端口映射）

---

## 🔄 回滚方案

如果部署后出现问题，可以快速回滚：

```bash
# 1. 停止当前服务
docker compose -f docker/docker-compose.prod.yml down

# 2. 恢复配置文件
cp frontend/scripts/deploy/nginx-ssl.conf.backup frontend/scripts/deploy/nginx-ssl.conf
cp docker/docker-compose.prod.yml.backup docker/docker-compose.prod.yml

# 3. 恢复环境变量（如果需要）
# 编辑 .env 文件，将 MINIO_BROWSER_ENDPOINT 改回 http://your-ip:9000

# 4. 重新启动服务
docker compose --env-file .env -f docker/docker-compose.prod.yml up -d
```

---

## 📝 修改摘要

### 新增的路径代理

| 路径 | 目标服务 | 端口 | 状态 |
|------|---------|------|------|
| `/minio/` | MinIO API | 9000 | ✅ 已存在 |
| `/minio-console/` | MinIO Console | 9001 | ✅ **新增** |
| `/onlyoffice/` | OnlyOffice | 80 | ✅ 已存在 |
| `/prometheus/` | Prometheus | 9090 | ⚡ 可选（已配置） |
| `/grafana/` | Grafana | 3000 | ⚡ 可选（已配置） |

### 移除的端口映射

| 服务 | 原端口映射 | 状态 |
|------|-----------|------|
| MinIO | 9000:9000, 9001:9001 | ✅ 已移除 |
| OnlyOffice | 8088:80 | ✅ 已移除 |
| Prometheus | 9090:9090 | ✅ 已移除 |
| Grafana | 3000:3000 | ✅ 已移除 |

---

## ⚠️ 重要提醒

1. **环境变量必须更新**：确保 `.env` 文件中的 `MINIO_BROWSER_ENDPOINT=/minio`
2. **前端镜像需要重建**：新的 Nginx 配置需要重新构建前端镜像
3. **测试后再上线**：建议先在测试环境验证，确认无误后再部署到生产环境
4. **保留备份文件**：备份文件已创建，不要删除，以便回滚

---

## 📞 问题排查

如果遇到问题，请参考：
- 详细文档：`docs/SINGLE-PORT-MIGRATION-GUIDE.md`
- 常见问题：文档第 9 节
- 回滚方案：文档第 8 节

---

**实施完成时间：** 2026-01-31  
**下一步：** 部署到测试环境并验证功能
