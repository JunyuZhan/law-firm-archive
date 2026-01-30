# FRP 内网穿透配置指南

## 📋 概述

FRP（Fast Reverse Proxy）用于将内网服务暴露到公网，支持通过域名访问内网部署的系统。

---

## 🏗️ 部署架构

```
用户浏览器
    ↓ HTTPS
Cloudflare CDN (代理模式)
    ↓ HTTPS/HTTP
公网服务器 (frp server)
    ↓ TCP/HTTP
frp 客户端 (内网服务器)
    ↓ HTTP
Docker 容器 (law-firm)
```

---

## 🔧 FRP 客户端配置

### 配置文件示例

```toml
# 律所系统主应用
[[proxies]]
name = "law-firm"
type = "http"
localIP = "127.0.0.1"
localPort = 80
customDomains = ["oa.example.com"]

# MinIO 控制台
[[proxies]]
name = "minio"
type = "http"
localIP = "127.0.0.1"
localPort = 9001
customDomains = ["minio.example.com"]

# Prometheus 监控
[[proxies]]
name = "prometheus"
type = "http"
localIP = "127.0.0.1"
localPort = 9090
customDomains = ["prometheus.example.com"]

# Grafana 监控
[[proxies]]
name = "grafana"
type = "http"
localIP = "127.0.0.1"
localPort = 3000
customDomains = ["grafana.example.com"]
```

---

## ☁️ Cloudflare 配置

### 代理模式设置

1. **DNS 设置**
   - 添加 A 记录，指向 FRP 服务器 IP
   - 开启代理（橙色云朵图标）

2. **SSL/TLS 设置**
   - SSL/TLS 加密模式：**完全**（Full）
   - 自动 HTTPS 重定向：开启

3. **安全设置**
   - 安全级别：中等
   - 始终使用 HTTPS：开启

### 注意事项

⚠️ **Cloudflare 代理会影响 WebSocket 连接**：

- OnlyOffice 需要 WebSocket 支持
- 如果 OnlyOffice 无法正常工作，可能需要：
  - 关闭 Cloudflare 代理（仅 DNS）
  - 或配置 Cloudflare WebSocket 支持

---

## 🔒 安全建议

### 1. 限制访问

**Prometheus 和 Grafana**：

- 建议关闭 Cloudflare 代理（仅内网访问）
- 或使用 Cloudflare Access 保护
- 或通过 Nginx 添加基本认证

### 2. 防火墙配置

```bash
# 仅允许 FRP 服务器 IP 访问
iptables -A INPUT -p tcp --dport 7000 -s <frp-server-ip> -j ACCEPT
iptables -A INPUT -p tcp --dport 7000 -j DROP
```

### 3. FRP 认证

在 FRP 客户端配置中添加认证：

```toml
[common]
serverAddr = "your-frp-server.com"
serverPort = 7000
token = "your-secret-token"
```

---

## 🐛 常见问题

### 问题1：域名无法访问

**检查项**：

1. DNS 解析是否正确
2. FRP 客户端是否运行
3. FRP 服务器端口是否开放
4. Cloudflare 代理是否开启

### 问题2：HTTPS 证书错误

**解决方案**：

- 确保 Cloudflare SSL/TLS 模式设置为"完全"
- 检查 FRP 服务器 SSL 证书配置

### 问题3：OnlyOffice 无法加载

**原因**：Cloudflare 代理可能影响 WebSocket 连接

**解决方案**：

- 关闭 OnlyOffice 相关域名的 Cloudflare 代理
- 或配置 Cloudflare WebSocket 支持

---

## 📚 相关文档

- [部署指南](./deployment.md)
- [配置说明](./configuration.md)
- [故障排查](./troubleshooting.md)

---

**最后更新**: 2026-01-27
