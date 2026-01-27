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
  - [ ] MinIO 配置为仅内网访问

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

### 1.3 网络安全

- [ ] **防火墙规则**
  - [ ] 仅开放必要端口（80, 443, 22）
  - [ ] 数据库端口（5432）仅允许内网访问
  - [ ] Redis 端口（6379）仅允许内网访问
  - [ ] MinIO 端口（9000, 9001）仅允许内网访问

- [ ] **安全组配置**（云服务器）
  - [ ] 入站规则已限制 IP 范围
  - [ ] 出站规则已配置
  - [ ] 已启用 DDoS 防护（如可用）

---

## 🟡 二、应用配置检查

### 2.1 环境变量配置

- [ ] **数据库配置**
  ```bash
  DB_HOST=your-db-host
  DB_PORT=5432
  DB_NAME=law_firm
  DB_USERNAME=law_admin
  DB_PASSWORD=<强密码>
  ```

- [ ] **Redis 配置**
  ```bash
  REDIS_HOST=your-redis-host
  REDIS_PORT=6379
  REDIS_PASSWORD=<密码>
  ```

- [ ] **MinIO 配置**
  ```bash
  MINIO_ENDPOINT=http://minio:9000
  MINIO_ACCESS_KEY=<访问密钥>
  MINIO_SECRET_KEY=<秘密密钥>
  MINIO_BUCKET=law-firm
  ```

- [ ] **应用配置**
  ```bash
  SPRING_PROFILES_ACTIVE=prod
  SWAGGER_ENABLED=false
  JWT_SECRET=<强随机密钥>
  ```

### 2.2 日志配置

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

### 3.2 数据库性能

- [ ] **连接池配置**
  - [ ] 连接池大小已调整（`maximum-pool-size: 50`）
  - [ ] 最小空闲连接数已设置（`minimum-idle: 10`）
  - [ ] 连接超时已配置

---

## 🔵 四、存储检查

### 4.1 MinIO 配置

- [ ] **存储桶**
  - [ ] 默认存储桶已创建（`law-firm`）
  - [ ] 存储桶策略已配置
  - [ ] 访问权限已设置

- [ ] **备份**
  - [ ] MinIO 数据备份已配置
  - [ ] 备份策略已制定

---

## 🟣 五、监控和告警

### 5.1 监控配置

- [ ] **Prometheus**
  - [ ] Prometheus 已配置
  - [ ] 指标收集已启用
  - [ ] 监控目标已配置

- [ ] **Grafana**
  - [ ] Grafana 已配置
  - [ ] 仪表板已导入
  - [ ] 告警规则已配置

### 5.2 日志监控

- [ ] **日志收集**
  - [ ] 日志收集已配置（如 ELK）
  - [ ] 日志分析已设置
  - [ ] 错误告警已配置

---

## ⚪ 六、性能检查

### 6.1 系统资源

- [ ] **服务器资源**
  - [ ] CPU 核心数 ≥ 4
  - [ ] 内存 ≥ 8GB
  - [ ] 磁盘空间 ≥ 100GB
  - [ ] 网络带宽 ≥ 100Mbps

- [ ] **Docker 资源**
  - [ ] Docker 内存限制已设置
  - [ ] Docker CPU 限制已设置

---

## 📝 检查脚本

运行以下脚本进行自动检查：

```bash
# 统一部署前检查（推荐）
./scripts/pre-deploy-check.sh

# 生产环境检查（旧版）
./scripts/check-production-ready.sh

# 安全检查
./scripts/security-check.sh
```

---

## ✅ 检查完成

完成所有检查项后，可以开始部署：

```bash
./scripts/deploy.sh
```

---

**最后更新**: 2026-01-27
