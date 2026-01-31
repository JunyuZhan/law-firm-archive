# 生产环境部署检查清单

## 📋 部署前必检项

本文档列出了生产环境部署前必须检查的所有项目，请逐项确认后再进行部署。

---

## 🔴 一、安全配置检查（最高优先级）

### 1.1 密钥和密码配置

- [ ] **JWT 密钥**
  - [ ] 已生成强随机密钥（至少256位）
  - [ ] 生成命令：`openssl rand -base64 32`
  - [ ] 已设置环境变量 `JWT_SECRET`
  - [ ] 确认未使用默认值 `your-256-bit-secret-key-here-change-in-production`

- [ ] **数据库密码**
  - [ ] 已设置强密码（16位以上，包含大小写字母、数字、特殊字符）
  - [ ] 已设置环境变量 `DB_PASSWORD`
  - [ ] 确认未使用默认值 `dev_password_123`
  - [ ] 数据库用户权限已最小化（仅授予必要权限）

- [ ] **Redis 密码**
  - [ ] 已设置密码（如需要）
  - [ ] 已设置环境变量 `REDIS_PASSWORD`
  - [ ] Redis 配置为仅内网访问

- [ ] **MinIO 访问密钥**
  - [ ] 已修改默认密钥 `minioadmin/minioadmin`
  - [ ] 已设置环境变量 `MINIO_ACCESS_KEY` 和 `MINIO_SECRET_KEY`
  - [ ] MinIO 控制台已设置强密码
  - [ ] MinIO 配置为仅内网访问（单端口架构：通过 Nginx 访问）

- [ ] **OnlyOffice JWT 密钥**（如启用）
  - [ ] 已设置 `ONLYOFFICE_JWT_SECRET`
  - [ ] OnlyOffice 已启用 JWT 验证

### 1.2 API 安全

- [ ] **Swagger UI**
  - [ ] 生产环境已禁用（`SWAGGER_ENABLED=false`）
  - [ ] 或已限制为仅内网访问
  - [ ] 确认 `/swagger-ui/**` 和 `/v3/api-docs/**` 路径已保护

- [ ] **HTTPS 配置**
  - [ ] 已配置 SSL/TLS 证书
  - [ ] 已启用 HTTPS（端口 443）
  - [ ] HTTP 请求已重定向到 HTTPS
  - [ ] 证书有效期检查（建议设置自动续期）

- [ ] **CORS 配置**
  - [ ] 已配置允许的前端域名
  - [ ] 未使用通配符 `*`（生产环境）
  - [ ] 已限制允许的 HTTP 方法

### 1.3 网络安全（单端口架构）

- [ ] **端口暴露检查**
  - [ ] **仅暴露 80 和 443 端口**（HTTP/HTTPS）
  - [ ] 数据库端口（5432）**不暴露**（通过服务器维护）
  - [ ] Redis 端口（6379）**不暴露**（Docker 内部访问）
  - [ ] MinIO 端口（9000, 9001）**不暴露**（通过 Nginx `/minio/` 和 `/minio-console/` 访问）
  - [ ] OnlyOffice 端口（8088）**不暴露**（通过 Nginx `/onlyoffice/` 访问）

- [ ] **防火墙规则**
  - [ ] 仅开放必要端口（80, 443, 22）
  - [ ] 其他端口已关闭或限制访问

- [ ] **安全组配置**（云服务器）
  - [ ] 入站规则已限制 IP 范围
  - [ ] 出站规则已配置
  - [ ] 已启用 DDoS 防护（如可用）

---

## 🟡 二、应用配置检查

### 2.1 环境变量配置

- [ ] **数据库配置**

  ```bash
  DB_HOST=postgres  # Docker 内部服务名
  DB_PORT=5432
  DB_NAME=law_firm
  DB_USERNAME=law_admin
  DB_PASSWORD=<强密码>
  ```

- [ ] **Redis 配置**

  ```bash
  REDIS_HOST=redis  # Docker 内部服务名
  REDIS_PORT=6379
  REDIS_PASSWORD=<密码>
  ```

- [ ] **MinIO 配置（单端口架构）**

  ```bash
  MINIO_ENDPOINT=http://minio:9000  # Docker 内部地址
  MINIO_EXTERNAL_ENDPOINT=http://minio:9000  # Docker 内部地址
  MINIO_BROWSER_ENDPOINT=/minio  # ⚠️ 相对路径（通过 Nginx）
  MINIO_ACCESS_KEY=<访问密钥>
  MINIO_SECRET_KEY=<秘密密钥>
  MINIO_BUCKET=law-firm
  ```

- [ ] **OnlyOffice 配置（单端口架构）**

  ```bash
  ONLYOFFICE_URL=/onlyoffice  # ⚠️ 相对路径（通过 Nginx）
  ONLYOFFICE_CALLBACK_URL=http://backend:8080/api  # Docker 内部地址
  FILE_SERVER_URL=http://law-firm-minio:9000  # Docker 内部地址
  ONLYOFFICE_JWT_ENABLED=true
  ONLYOFFICE_JWT_SECRET=<强密钥>
  ```

- [ ] **应用配置**
  ```bash
  SPRING_PROFILES_ACTIVE=prod
  SWAGGER_ENABLED=false
  JWT_SECRET=<强随机密钥>
  ```

### 2.2 Nginx 配置检查（单端口架构）

- [ ] **路径代理配置**
  - [ ] `/minio/` location 已配置（HTTP + HTTPS）
  - [ ] `/minio-console/` location 已配置（HTTP + HTTPS）
  - [ ] `/onlyoffice/` location 已配置（HTTP + HTTPS）
  - [ ] WebSocket 支持已配置（OnlyOffice 和 MinIO Console）

- [ ] **关键配置验证**
  - [ ] MinIO 代理设置了 `proxy_set_header Host minio:9000;`（预签名 URL 需要）
  - [ ] OnlyOffice 代理设置了 `proxy_set_header Authorization $http_authorization;`（JWT 验证需要）

### 2.3 Docker Compose 配置检查（单端口架构）

- [ ] **端口映射检查**
  - [ ] Frontend 端口：80, 443 ✅（必须暴露）
  - [ ] MinIO 端口：**已移除** ✅
  - [ ] OnlyOffice 端口：**已移除** ✅
  - [ ] Prometheus 端口：**已移除**（如果启用监控）✅
  - [ ] Grafana 端口：**已移除**（如果启用监控）✅
  - [ ] PostgreSQL 端口：**不暴露** ✅（标准做法）
  - [ ] Redis 端口：**不暴露** ✅（标准做法）

- [ ] **服务配置**
  - [ ] Prometheus `--web.external-url=/prometheus/` 已配置（如果启用）
  - [ ] Grafana `GF_SERVER_ROOT_URL=/grafana/` 已配置（如果启用）

### 2.4 日志配置

- [ ] **日志级别**
  - [ ] 已设置为 `info`（`com.lawfirm: info`）
  - [ ] 安全相关日志设置为 `warn`（`org.springframework.security: warn`）
  - [ ] SQL 日志已关闭或设置为 `warn`（`org.mybatis: warn`）

- [ ] **日志文件**
  - [ ] 日志文件路径已配置
  - [ ] 日志文件目录已创建且有写权限
  - [ ] 日志轮转已配置（最大100MB，保留30天）

---

## 🟢 三、数据库检查

### 3.1 数据库初始化

- [ ] **初始化脚本**
  - [ ] 所有初始化脚本已检查
  - [ ] 数据库 schema 已创建
  - [ ] 初始数据已导入
  - [ ] 索引已创建

- [ ] **备份配置**
  - [ ] 自动备份已配置
  - [ ] 备份路径已设置
  - [ ] 备份保留策略已配置

### 3.2 数据库访问（单端口架构）

- [ ] **数据库端口**
  - [ ] 数据库端口**不暴露**（标准做法）
  - [ ] 通过服务器上的 `docker exec` 命令维护
  - [ ] 备份脚本已配置（`scripts/db-auto-backup.sh`）

- [ ] **数据库性能**
  - [ ] 连接池大小已调整（`maximum-pool-size: 50`）
  - [ ] 最小空闲连接数已设置（`minimum-idle: 10`）
  - [ ] 连接超时已配置

---

## 🔵 四、存储检查（单端口架构）

### 4.1 MinIO 配置

- [ ] **存储桶**
  - [ ] 默认存储桶已创建（`law-firm`）
  - [ ] 存储桶策略已配置
  - [ ] 访问权限已设置

- [ ] **访问方式**
  - [ ] MinIO API 通过 `/minio/` 路径访问（Nginx 代理）
  - [ ] MinIO Console 通过 `/minio-console/` 路径访问（Nginx 代理）
  - [ ] 端口映射已移除

- [ ] **环境变量**
  - [ ] `MINIO_BROWSER_ENDPOINT=/minio`（相对路径）

- [ ] **备份**
  - [ ] MinIO 数据备份已配置
  - [ ] 备份策略已制定

---

## 🟣 五、OnlyOffice 集成检查（关键）

### 5.1 OnlyOffice 配置

- [ ] **访问方式**
  - [ ] OnlyOffice 通过 `/onlyoffice/` 路径访问（Nginx 代理）
  - [ ] 端口映射已移除

- [ ] **环境变量**
  - [ ] `ONLYOFFICE_URL=/onlyoffice`（相对路径或完整 URL）
  - [ ] `ONLYOFFICE_CALLBACK_URL=http://backend:8080/api`（Docker 内部地址）
  - [ ] `FILE_SERVER_URL=http://law-firm-minio:9000`（Docker 内部地址）

- [ ] **Nginx 配置**
  - [ ] `/onlyoffice/` location 已配置
  - [ ] WebSocket 支持已配置
  - [ ] Authorization header 传递已配置

### 5.2 OnlyOffice 和 MinIO 集成测试（必须）

- [ ] **文档编辑功能测试**
  - [ ] 上传 DOC 文件成功
  - [ ] 点击"编辑"按钮，OnlyOffice 编辑器正常加载
  - [ ] 文档内容正确显示
  - [ ] 可以正常编辑文档
  - [ ] 保存文档成功
  - [ ] 文件已更新到 MinIO

- [ ] **容器间通信测试**
  - [ ] OnlyOffice → Backend：`docker exec onlyoffice curl http://backend:8080/api/actuator/health`
  - [ ] Backend → MinIO：`docker exec law-firm-backend curl http://minio:9000/minio/health/live`
  - [ ] 所有通信都成功（不依赖端口映射）

**测试脚本：**
```bash
./scripts/test-onlyoffice-minio-integration.sh
```

---

## 🟣 六、监控和告警

### 6.1 监控配置（可选）

- [ ] **Prometheus**
  - [ ] Prometheus 已配置（通过 `--profile monitoring` 启用）
  - [ ] 通过 `/prometheus/` 路径访问（Nginx 代理）
  - [ ] 端口映射已移除
  - [ ] `--web.external-url=/prometheus/` 已配置

- [ ] **Grafana**
  - [ ] Grafana 已配置（通过 `--profile monitoring` 启用）
  - [ ] 通过 `/grafana/` 路径访问（Nginx 代理）
  - [ ] 端口映射已移除
  - [ ] `GF_SERVER_ROOT_URL=/grafana/` 已配置
  - [ ] `GF_SERVER_SERVE_FROM_SUB_PATH=true` 已配置

### 6.2 日志监控

- [ ] **日志收集**
  - [ ] 日志收集已配置（如 ELK）
  - [ ] 日志分析已设置
  - [ ] 错误告警已配置

---

## ⚪ 七、性能检查

### 7.1 系统资源

- [ ] **服务器资源**
  - [ ] CPU 核心数 ≥ 4
  - [ ] 内存 ≥ 8GB
  - [ ] 磁盘空间 ≥ 100GB
  - [ ] 网络带宽 ≥ 100Mbps

- [ ] **Docker 资源**
  - [ ] Docker 内存限制已设置
  - [ ] Docker CPU 限制已设置

---

## 🔧 八、单端口架构专项检查

### 8.1 Nginx 配置验证

- [ ] **路径代理**
  - [ ] `/minio/` location（HTTP + HTTPS）✅
  - [ ] `/minio-console/` location（HTTP + HTTPS）✅
  - [ ] `/onlyoffice/` location（HTTP + HTTPS）✅
  - [ ] `/prometheus/` location（HTTP + HTTPS，可选）✅
  - [ ] `/grafana/` location（HTTP + HTTPS，可选）✅

- [ ] **关键配置**
  - [ ] MinIO 代理：`proxy_set_header Host minio:9000;` ✅
  - [ ] OnlyOffice 代理：`proxy_set_header Authorization $http_authorization;` ✅
  - [ ] WebSocket 支持已配置 ✅

### 8.2 端口映射验证

```bash
# 检查端口映射（应该只有 80 和 443）
docker ps --format "table {{.Names}}\t{{.Ports}}" | grep -E "80|443|9000|9001|8088|9090|3000"
```

**预期结果：**
- ✅ Frontend: `0.0.0.0:80->8080/tcp, 0.0.0.0:443->8443/tcp`
- ✅ 其他容器：**不应该有端口映射**

### 8.3 环境变量验证

```bash
# 检查关键环境变量
docker exec law-firm-backend env | grep -E "MINIO_BROWSER_ENDPOINT|ONLYOFFICE_URL|FILE_SERVER_URL"
```

**预期结果：**
- ✅ `MINIO_BROWSER_ENDPOINT=/minio`
- ✅ `ONLYOFFICE_URL=/onlyoffice`（或完整 URL）
- ✅ `FILE_SERVER_URL=http://law-firm-minio:9000`（Docker 内部地址）

---

## 📝 检查脚本

运行以下脚本进行自动检查：

```bash
# 统一部署前检查（推荐）
./scripts/pre-deploy-check.sh

# OnlyOffice 和 MinIO 集成测试
./scripts/test-onlyoffice-minio-integration.sh

# 生产环境检查（旧版）
./scripts/check-production-ready.sh

# 安全检查
./scripts/security-check.sh
```

---

## 🧪 部署后测试清单

### 必须测试的功能

- [ ] **基础功能**
  - [ ] 系统登录
  - [ ] 页面正常加载
  - [ ] API 调用正常

- [ ] **文件功能**
  - [ ] 文件上传
  - [ ] 文件下载
  - [ ] 缩略图显示

- [ ] **文档编辑功能（关键）**
  - [ ] 上传 DOC 文件
  - [ ] 点击"编辑"按钮
  - [ ] OnlyOffice 编辑器正常加载
  - [ ] 修改文档内容
  - [ ] 保存文档
  - [ ] 验证文件已更新

- [ ] **MinIO Console**
  - [ ] 访问 `http://your-domain/minio-console/`
  - [ ] 可以登录
  - [ ] 可以管理文件

- [ ] **路径访问验证**
  - [ ] `/minio/` 路径可以访问
  - [ ] `/minio-console/` 路径可以访问
  - [ ] `/onlyoffice/` 路径可以访问

---

## ✅ 检查完成

完成所有检查项后，可以开始部署：

```bash
./scripts/deploy.sh
```

部署完成后，**必须立即测试文档编辑功能**！

**测试步骤：**
1. 运行集成测试：`./scripts/test-onlyoffice-minio-integration.sh`
2. 手动测试：上传 DOC 文件 → 编辑 → 保存 → 验证

---

## 📚 相关文档

- **单端口架构迁移指南**：`docs/SINGLE-PORT-MIGRATION-GUIDE.md`
- **OnlyOffice 和 MinIO 集成测试**：`docs/ONLYOFFICE-MINIO-INTEGRATION-TEST.md`
- **关键测试清单**：`docs/CRITICAL-TEST-CHECKLIST.md`
- **配置总结**：`docs/FINAL-CONFIGURATION-SUMMARY.md`

---

**最后更新**: 2026-01-31  
**版本**: 2.0（单端口架构）
