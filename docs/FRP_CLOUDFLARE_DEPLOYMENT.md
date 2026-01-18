# FRP + Cloudflare 代理部署指南

## ✅ 架构可行性分析

**结论**: 该部署架构**完全可行**，但需要注意一些配置细节。

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

## ✅ 当前配置支持情况

### 1. 反向代理头配置 ✅

**Nginx 已配置**:
```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
```

✅ **支持多级代理**: 已正确配置 `X-Forwarded-*` 头

### 2. WebSocket 支持 ✅

**OnlyOffice WebSocket 已配置**:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

✅ **支持 WebSocket**: OnlyOffice 实时协作功能可用

### 3. 文件上传支持 ✅

```nginx
client_max_body_size 100M;
```

✅ **支持大文件上传**: 已配置 100MB 限制

---

## ⚠️ 需要注意的问题

### 1. Cloudflare 代理模式限制

#### WebSocket 支持

**问题**: Cloudflare 免费版对 WebSocket 支持有限制

**解决方案**:
- ✅ **推荐**: 使用 Cloudflare 的 WebSocket 支持（需要配置）
- ⚠️ **备选**: OnlyOffice 路径不使用 Cloudflare 代理，直接通过 frp 访问

#### 文件上传大小限制

**Cloudflare 限制**:
- 免费版: 100MB
- 付费版: 500MB

**当前配置**: 100MB，与免费版限制一致 ✅

#### 超时限制

**Cloudflare 限制**:
- HTTP 请求超时: 100秒
- WebSocket 连接: 无限制（但可能受其他因素影响）

**当前配置**: 
- API 超时: 600秒（10分钟）⚠️
- OnlyOffice 超时: 600秒（10分钟）⚠️

**建议**: 
- 对于长时间操作（如 AI 生成），考虑使用异步任务
- 或调整 Cloudflare 设置（付费版）

### 2. HTTPS/HTTP 协议处理

#### 问题

- Cloudflare → frp: HTTPS
- frp → 内网服务器: HTTP（通常）
- 内网服务器 → Docker: HTTP

#### 解决方案

**方案一**: frp 支持 HTTPS（推荐）

```ini
# frpc.ini
[web]
type = https
local_port = 80
custom_domains = your-domain.com

# 如果需要 HTTPS 证书
plugin = https2http
plugin_local_addr = 127.0.0.1:80
plugin_crt_path = /path/to/cert.crt
plugin_key_path = /path/to/cert.key
```

**方案二**: 使用 HTTP（简单，但 frp → 内网是明文）

```ini
# frpc.ini
[web]
type = http
local_port = 80
custom_domains = your-domain.com
```

**Nginx 配置**: 已支持 `X-Forwarded-Proto`，可以正确识别 HTTPS ✅

### 3. OnlyOffice 回调 URL 配置

#### 问题

OnlyOffice 需要回调后端保存文档，但 OnlyOffice 在内网，后端也在内网。

#### 解决方案

**当前配置**:
```yaml
# docker-compose.prod.yml
ONLYOFFICE_CALLBACK_URL=http://backend:8080/api
```

✅ **正确**: OnlyOffice 和 Backend 在同一 Docker 网络，可以直接通信

**注意**: 不需要通过公网回调，内网通信即可 ✅

### 4. 真实 IP 获取

#### Cloudflare 代理模式

Cloudflare 会修改 `X-Forwarded-For` 头，需要从 `CF-Connecting-IP` 获取真实 IP。

#### 解决方案

**更新 Nginx 配置**（如果需要记录真实 IP）:

```nginx
# 优先使用 Cloudflare 的真实 IP 头
set_real_ip_from 173.245.48.0/20;
set_real_ip_from 103.21.244.0/22;
set_real_ip_from 103.22.200.0/22;
set_real_ip_from 103.31.4.0/22;
set_real_ip_from 141.101.64.0/18;
set_real_ip_from 108.162.192.0/18;
set_real_ip_from 190.93.240.0/20;
set_real_ip_from 188.114.96.0/20;
set_real_ip_from 197.234.240.0/22;
set_real_ip_from 198.41.128.0/17;
set_real_ip_from 162.158.0.0/15;
set_real_ip_from 104.16.0.0/13;
set_real_ip_from 104.24.0.0/14;
set_real_ip_from 172.64.0.0/13;
set_real_ip_from 131.0.72.0/22;
real_ip_header CF-Connecting-IP;
```

---

## 📋 部署步骤

### 1. 内网服务器配置

#### 1.1 启动 Docker 服务

```bash
# 在内网服务器上
cd /path/to/law-firm
./scripts/deploy.sh --quick
```

#### 1.2 配置环境变量

```bash
# 编辑 .env 文件
vim .env

# 设置 OnlyOffice URL（使用 Cloudflare 域名）
ONLYOFFICE_URL=https://your-domain.com/onlyoffice
```

### 2. FRP 配置

#### 2.1 公网服务器（frps）

```ini
# frps.ini
[common]
bind_port = 7000
token = your-secret-token

# HTTP/HTTPS 端口
vhost_http_port = 80
vhost_https_port = 443
```

#### 2.2 内网服务器（frpc）

```ini
# frpc.ini
[common]
server_addr = your-public-server-ip
server_port = 7000
token = your-secret-token

[web]
type = http
local_port = 80
custom_domains = your-domain.com

# 如果需要 WebSocket 支持
[web_ws]
type = tcp
local_ip = 127.0.0.1
local_port = 80
remote_port = 8080
```

### 3. Cloudflare 配置

#### 3.1 DNS 设置

1. 添加 A 记录指向公网服务器 IP
2. **代理状态**: 开启（橙色云朵）✅
3. **SSL/TLS**: 完全（严格）或完全

#### 3.2 SSL/TLS 设置

- **加密模式**: 完全（严格）或完全
- **自动 HTTPS 重写**: 开启
- **始终使用 HTTPS**: 开启

#### 3.3 网络设置

- **WebSocket**: 开启 ✅
- **HTTP/2**: 开启
- **HTTP/3 (QUIC)**: 可选

#### 3.4 页面规则（可选）

如果需要为 OnlyOffice 路径设置特殊规则：

```
URL: your-domain.com/onlyoffice/*
设置:
  - WebSocket: 开启
  - 缓存级别: 绕过
  - 边缘缓存 TTL: 不缓存
```

### 4. 验证配置

#### 4.1 检查 WebSocket

```bash
# 在浏览器控制台
const ws = new WebSocket('wss://your-domain.com/onlyoffice/...');
ws.onopen = () => console.log('WebSocket connected');
ws.onerror = (e) => console.error('WebSocket error', e);
```

#### 4.2 检查真实 IP

```bash
# 在后端日志中查看
# 应该能看到 Cloudflare 的真实 IP（CF-Connecting-IP）
```

#### 4.3 检查 HTTPS

```bash
# 访问
curl -I https://your-domain.com

# 应该返回 200，且协议是 HTTPS
```

---

## 🔧 配置优化建议

### 1. Nginx 配置增强

创建 `frontend/scripts/deploy/nginx-cloudflare.conf`:

```nginx
# Cloudflare 真实 IP 配置
map $http_cf_connecting_ip $real_ip {
    ""      $remote_addr;
    default $http_cf_connecting_ip;
}

# 在 server 块中使用
real_ip_header CF-Connecting-IP;
set_real_ip_from 173.245.48.0/20;
# ... 其他 Cloudflare IP 段
```

### 2. 后端配置增强

如果需要记录真实 IP，在后端添加：

```java
@Configuration
public class CloudflareConfig {
    @Bean
    public FilterRegistrationBean<CloudflareIpFilter> cloudflareIpFilter() {
        FilterRegistrationBean<CloudflareIpFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CloudflareIpFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
```

### 3. OnlyOffice 配置

确保 OnlyOffice 可以访问后端：

```yaml
# docker-compose.prod.yml
onlyoffice:
  environment:
    - ALLOW_PRIVATE_IP_ADDRESS=true
    - ALLOW_META_IP_ADDRESS=true
```

✅ **已配置**: 当前配置已包含这些设置

---

## ⚠️ 常见问题

### 1. WebSocket 连接失败

**症状**: OnlyOffice 实时协作无法使用

**解决方案**:
1. 检查 Cloudflare WebSocket 是否开启
2. 检查 frp 是否支持 WebSocket（使用 tcp 类型）
3. 检查防火墙规则

### 2. 文件上传失败

**症状**: 大文件上传失败

**解决方案**:
1. 检查 Cloudflare 文件大小限制（免费版 100MB）
2. 检查 Nginx `client_max_body_size` 配置
3. 检查 frp 超时设置

### 3. HTTPS 重定向问题

**症状**: 访问 HTTP 不自动跳转 HTTPS

**解决方案**:
1. 在 Cloudflare 开启"始终使用 HTTPS"
2. 或在 Nginx 添加重定向规则

### 4. 真实 IP 获取错误

**症状**: 日志中 IP 是 Cloudflare IP 而不是用户 IP

**解决方案**:
1. 配置 Nginx `real_ip_header CF-Connecting-IP`
2. 添加 Cloudflare IP 段到 `set_real_ip_from`

---

## 📊 架构优势

### ✅ 优点

1. **安全性**: 
   - Cloudflare DDoS 防护
   - WAF 防护
   - SSL/TLS 加密

2. **性能**:
   - Cloudflare CDN 加速
   - 全球节点缓存

3. **成本**:
   - Cloudflare 免费版可用
   - frp 开源免费

4. **灵活性**:
   - 内网服务器无需公网 IP
   - 易于扩展和维护

### ⚠️ 注意事项

1. **WebSocket**: Cloudflare 免费版支持，但需要正确配置
2. **文件大小**: 免费版限制 100MB
3. **超时**: HTTP 请求超时 100秒
4. **真实 IP**: 需要特殊配置才能获取

---

## 🔗 相关文档

- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
- [网络通信检查](./NETWORK_COMMUNICATION_CHECK.md)
- [OnlyOffice 集成文档](./ONLYOFFICE_DATA_FLOW.md)

---

## 📝 总结

**该部署架构完全可行** ✅

### 关键配置点

1. ✅ **Nginx 配置**: 已支持反向代理头
2. ✅ **WebSocket**: 已配置，Cloudflare 需要开启
3. ✅ **文件上传**: 已配置 100MB，符合 Cloudflare 免费版限制
4. ⚠️ **超时设置**: 需要注意 Cloudflare 100秒限制
5. ✅ **OnlyOffice**: 内网通信，无需特殊配置

### 推荐配置

- **Cloudflare**: 代理模式 + WebSocket 开启
- **frp**: HTTP 类型（简单）或 HTTPS 类型（更安全）
- **Nginx**: 已配置，可能需要添加 Cloudflare IP 段
- **OnlyOffice URL**: 使用 Cloudflare 域名
