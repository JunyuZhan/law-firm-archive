# FRP 配置指南

## 📋 当前 FRP 配置分析

### 你的配置

```toml
# 律所系统
[[proxies]]
name = "law-firm"
type = "http"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.albertzhan.top"]

# MinIO 控制台
[[proxies]]
name = "minio"
type = "http"
localIP = "127.0.0.1"
localPort = 9001
customDomains = ["minio.albertzhan.top"]
```

---

## ✅ 配置评估

### 已配置的服务

1. **主应用** (`oa.albertzhan.top` → `127.0.0.1:80`)
   - ✅ **正确**: 主应用通过前端 Nginx 提供服务
   - ✅ **包含**: 前端页面、API 接口、OnlyOffice 代理
   - ✅ **端口**: 80（前端容器端口映射）

2. **MinIO 控制台** (`minio.albertzhan.top` → `127.0.0.1:9001`)
   - ✅ **正确**: MinIO 控制台独立访问
   - ✅ **端口**: 9001（MinIO 控制台端口）

---

## 🔍 生产环境服务端口清单

### 需要对外暴露的服务

| 服务 | 容器端口映射 | 是否需要 FRP | 说明 |
|------|------------|------------|------|
| **frontend** | `80:8080` | ✅ **已配置** | 主应用入口（包含 API、OnlyOffice） |
| **minio** | `9001:9001` | ✅ **已配置** | MinIO 控制台 |
| **onlyoffice** | `8088:80` | ❌ **不需要** | 已通过前端 Nginx 代理 |
| **prometheus** | `9090:9090` | ⚠️ **可选** | 监控面板（建议仅内网访问） |
| **grafana** | `3000:3000` | ⚠️ **可选** | 监控可视化（建议仅内网访问） |

### 不需要对外暴露的服务

| 服务 | 说明 |
|------|------|
| **backend** | 通过前端 Nginx 代理访问，无需直接暴露 |
| **postgres** | 数据库，仅内网访问 |
| **redis** | 缓存，仅内网访问 |
| **minio API** | MinIO API (9000)，通过后端访问，无需直接暴露 |

---

## ✅ 配置完整性检查

### 核心功能 ✅

你的配置**已覆盖核心功能**：

1. ✅ **主应用访问**: `oa.albertzhan.top`
   - 前端页面
   - API 接口 (`/api/`)
   - OnlyOffice 文档服务 (`/onlyoffice/`)
   - 文档站点 (`/docs/`)

2. ✅ **MinIO 管理**: `minio.albertzhan.top`
   - MinIO 控制台
   - 文件管理

### 可选服务 ⚠️

如果需要监控功能，可以添加：

```toml
# Prometheus 监控（可选，建议仅内网访问）
[[proxies]]
name = "prometheus"
type = "http"
localIP = "127.0.0.1"
localPort = 9090
customDomains = ["prometheus.albertzhan.top"]

# Grafana 监控面板（可选，建议仅内网访问）
[[proxies]]
name = "grafana"
type = "http"
localIP = "127.0.0.1"
localPort = 3000
customDomains = ["grafana.albertzhan.top"]
```

**安全建议**: 
- ⚠️ 监控服务建议仅内网访问或添加认证
- ✅ 如果不需要外部访问，可以不配置

---

## 🔧 配置优化建议

### 1. 添加 HTTPS 支持（推荐）

如果 Cloudflare 使用 HTTPS，frp 也应该支持：

```toml
# 律所系统（HTTPS）
[[proxies]]
name = "law-firm"
type = "https"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.albertzhan.top"]

# MinIO 控制台（HTTPS）
[[proxies]]
name = "minio"
type = "https"
localIP = "127.0.0.1"
localPort = 9001
customDomains = ["minio.albertzhan.top"]
```

### 2. 添加健康检查（可选）

```toml
# 律所系统
[[proxies]]
name = "law-firm"
type = "http"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.albertzhan.top"]
healthCheckType = "http"
healthCheckPath = "/api/actuator/health"
healthCheckIntervalSeconds = 10
maxFailedTimes = 3
```

### 3. 添加访问控制（可选）

```toml
# 律所系统
[[proxies]]
name = "law-firm"
type = "http"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.albertzhan.top"]
# HTTP 基础认证（可选）
httpUser = "admin"
httpPwd = "your-password"
```

---

## 📝 完整配置示例

### 最小配置（你当前的配置）✅

```toml
# frpc.ini
serverAddr = "your-frp-server-ip"
serverPort = 7000
token = "your-token"

# 律所系统
[[proxies]]
name = "law-firm"
type = "http"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.albertzhan.top"]

# MinIO 控制台
[[proxies]]
name = "minio"
type = "http"
localIP = "127.0.0.1"
localPort = 9001
customDomains = ["minio.albertzhan.top"]
```

### 完整配置（包含监控）

```toml
# frpc.ini
serverAddr = "your-frp-server-ip"
serverPort = 7000
token = "your-token"

# 律所系统
[[proxies]]
name = "law-firm"
type = "http"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.albertzhan.top"]
healthCheckType = "http"
healthCheckPath = "/api/actuator/health"
healthCheckIntervalSeconds = 10

# MinIO 控制台
[[proxies]]
name = "minio"
type = "http"
localIP = "127.0.0.1"
localPort = 9001
customDomains = ["minio.albertzhan.top"]

# Prometheus 监控（可选）
[[proxies]]
name = "prometheus"
type = "http"
localIP = "127.0.0.1"
localPort = 9090
customDomains = ["prometheus.albertzhan.top"]
# 建议添加 HTTP 基础认证
httpUser = "admin"
httpPwd = "your-password"

# Grafana 监控面板（可选）
[[proxies]]
name = "grafana"
type = "http"
localIP = "127.0.0.1"
localPort = 3000
customDomains = ["grafana.albertzhan.top"]
# Grafana 本身有登录认证，可以不添加 HTTP 基础认证
```

---

## 🌐 Cloudflare DNS 配置

### 需要配置的 DNS 记录

| 域名 | 类型 | 内容 | 代理状态 | 说明 |
|------|------|------|---------|------|
| `oa.albertzhan.top` | A | 公网服务器 IP | ✅ 开启（橙色云朵） | 主应用 |
| `minio.albertzhan.top` | A | 公网服务器 IP | ⚠️ **建议关闭**（灰色云朵） | MinIO 控制台（建议仅内网） |
| `prometheus.albertzhan.top` | A | 公网服务器 IP | ⚠️ **建议关闭**（灰色云朵） | 监控（如配置） |
| `grafana.albertzhan.top` | A | 公网服务器 IP | ⚠️ **建议关闭**（灰色云朵） | 监控（如配置） |

### DNS 配置建议

1. **主应用** (`oa.albertzhan.top`)
   - ✅ **代理状态**: 开启（橙色云朵）
   - ✅ **SSL/TLS**: 完全（严格）
   - ✅ **WebSocket**: 开启
   - ✅ **始终使用 HTTPS**: 开启

2. **MinIO 控制台** (`minio.albertzhan.top`)
   - ⚠️ **代理状态**: **建议关闭**（灰色云朵）
   - **原因**: MinIO 控制台包含敏感操作，建议仅内网访问
   - **备选**: 如果必须公网访问，开启代理并添加 Cloudflare Access 保护

---

## 🔒 安全建议

### 1. MinIO 控制台安全

**当前风险**: MinIO 控制台直接暴露在公网

**建议**:
1. **方案一**（推荐）: 关闭 Cloudflare 代理，仅内网访问
2. **方案二**: 开启 Cloudflare Access，添加访问控制
3. **方案三**: 在 frp 配置中添加 HTTP 基础认证

### 2. 监控服务安全

如果配置了 Prometheus/Grafana：
- ⚠️ **建议**: 关闭 Cloudflare 代理，仅内网访问
- ⚠️ **或**: 使用 Cloudflare Access 保护
- ⚠️ **或**: 使用 VPN 访问

### 3. API 安全

主应用 API (`/api/`) 已通过以下方式保护：
- ✅ JWT 认证
- ✅ 权限控制
- ✅ Cloudflare WAF（如果开启代理）

---

## ✅ 配置验证

### 1. 测试主应用

```bash
# 测试主应用访问
curl -I https://oa.albertzhan.top

# 测试 API 健康检查
curl https://oa.albertzhan.top/api/actuator/health

# 测试 OnlyOffice（需要登录）
curl -I https://oa.albertzhan.top/onlyoffice/
```

### 2. 测试 MinIO

```bash
# 测试 MinIO 控制台
curl -I http://minio.albertzhan.top

# 或浏览器访问
# http://minio.albertzhan.top
```

### 3. 检查 WebSocket

```javascript
// 在浏览器控制台测试 OnlyOffice WebSocket
const ws = new WebSocket('wss://oa.albertzhan.top/onlyoffice/...');
ws.onopen = () => console.log('WebSocket connected');
```

---

## 📊 配置总结

### ✅ 你的配置评估

| 项目 | 状态 | 说明 |
|------|------|------|
| **核心功能** | ✅ **完整** | 主应用和 MinIO 已配置 |
| **OnlyOffice** | ✅ **已包含** | 通过主应用 Nginx 代理 |
| **API 接口** | ✅ **已包含** | 通过主应用 Nginx 代理 |
| **监控服务** | ⚠️ **可选** | 如需要可添加 |
| **安全性** | ⚠️ **需加强** | MinIO 建议仅内网访问 |

### 🎯 推荐配置

**最小配置**（你当前的配置）✅:
- ✅ 主应用: `oa.albertzhan.top`
- ✅ MinIO: `minio.albertzhan.top`（建议关闭 Cloudflare 代理）

**完整配置**（如需要监控）:
- ✅ 主应用: `oa.albertzhan.top`
- ✅ MinIO: `minio.albertzhan.top`
- ⚠️ Prometheus: `prometheus.albertzhan.top`（建议仅内网）
- ⚠️ Grafana: `grafana.albertzhan.top`（建议仅内网）

---

## 🔗 相关文档

- [FRP + Cloudflare 代理部署指南](./FRP_CLOUDFLARE_DEPLOYMENT.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
- [网络通信检查](./NETWORK_COMMUNICATION_CHECK.md)

---

## 📝 结论

**你的配置已覆盖核心功能** ✅

### 已配置 ✅
1. ✅ 主应用（包含前端、API、OnlyOffice）
2. ✅ MinIO 控制台

### 可选配置 ⚠️
1. ⚠️ Prometheus（如需要监控）
2. ⚠️ Grafana（如需要监控）

### 安全建议 🔒
1. ⚠️ MinIO 控制台建议关闭 Cloudflare 代理或添加访问控制
2. ⚠️ 监控服务建议仅内网访问

**总结**: 你的配置**足够使用**，核心功能都已覆盖。如果需要监控功能，可以添加 Prometheus 和 Grafana 的配置。
