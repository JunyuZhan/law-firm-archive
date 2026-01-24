# FRP 架构下的证书配置说明

## 📋 架构分析

```
用户浏览器
    ↓ HTTPS (Cloudflare证书)
Cloudflare (代理)
    ↓ HTTPS (需要验证源站证书)
FRPS 服务器 (公网)
    ↓ HTTP/HTTPS
FRPC 客户端 (内网)
    ↓ HTTP
内网应用 (192.168.50.10)
```

## 🎯 证书应该放在哪里？

### 关键理解

**Cloudflare 完全模式下：**
- Cloudflare 会连接到 **FRPS 服务器**（公网）验证 HTTPS
- Cloudflare 看到的"源站"是 FRPS 服务器，不是内网应用
- 所以证书需要配置在 **FRPS 服务器**上

---

## ✅ 配置方案

### 方案一：在 FRPS 服务器配置 HTTPS（推荐）

**步骤1：在 FRPS 服务器下载/配置证书**

**选项A：使用 Cloudflare Origin Certificate（推荐）**

1. **在 Cloudflare 控制台创建 Origin Certificate**
   - SSL/TLS → 源服务器 → 创建证书
   - 域名：你的域名（如：`yourdomain.com`）
   - 有效期：15年

2. **下载证书文件**
   - Certificate（证书）
   - Private Key（私钥）

3. **上传到 FRPS 服务器**
   ```bash
   # 上传证书到 FRPS 服务器
   scp certificate.pem root@frps-server-ip:/etc/frp/
   scp private.key root@frps-server-ip:/etc/frp/
   ```

**选项B：使用 Let's Encrypt（如果有域名解析到 FRPS）**

```bash
# SSH 到 FRPS 服务器
ssh root@frps-server-ip

# 安装 certbot
apt install certbot -y

# 获取证书
certbot certonly --standalone -d yourdomain.com

# 证书位置
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

**步骤2：配置 FRPS 支持 HTTPS**

**frps.ini 配置：**
```ini
[common]
bind_port = 7000

# HTTPS 端口（Cloudflare 会连接这个端口）
vhost_https_port = 7443

# 如果需要认证
privilege_token = your_secure_token

# HTTPS 证书配置（如果 FRPS 支持）
# https_cert_file = /etc/frp/certificate.pem
# https_key_file = /etc/frp/private.key
```

**注意：** 标准 FRP 的 frps 不直接支持 HTTPS 证书配置。需要：

**选项1：使用 Nginx 反向代理（推荐）**

在 FRPS 服务器上配置 Nginx：

```nginx
# /etc/nginx/sites-available/frps-https
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/frp/certificate.pem;
    ssl_certificate_key /etc/frp/private.key;

    location / {
        proxy_pass http://localhost:7000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

然后：
- Cloudflare → FRPS 服务器 443端口（Nginx）→ FRPS 7000端口 → FRPC → 内网应用

**选项2：使用支持 HTTPS 的 FRP 版本**

某些 FRP 版本支持直接配置 HTTPS，检查你的 FRP 版本。

---

### 方案二：在内网应用配置 HTTPS（当前方案）

**当前你的配置：**
- 内网应用（192.168.50.10）已配置 HTTPS（443端口）
- FRPC 转发到内网应用的 443 端口

**FRPC 配置：**
```ini
[web_https]
type = https
local_ip = 192.168.50.10
local_port = 443
custom_domains = yourdomain.com
```

**这样配置后：**
- Cloudflare → FRPS（HTTP）→ FRPC → 内网应用（HTTPS）
- Cloudflare 无法直接验证内网应用的证书（因为中间是 HTTP）

**要使用完全模式，需要：**
- Cloudflare → FRPS（HTTPS）→ FRPC → 内网应用（HTTPS）

---

## 🎯 推荐配置（完整方案）

### 架构

```
用户 → Cloudflare (HTTPS) → FRPS服务器Nginx (HTTPS) → FRPS (HTTP) → FRPC → 内网应用 (HTTP/HTTPS)
```

### 配置步骤

**1. 在 FRPS 服务器配置 Nginx + HTTPS**

```bash
# SSH 到 FRPS 服务器
ssh root@frps-server-ip

# 安装 Nginx
apt install nginx -y

# 创建 Nginx 配置
cat > /etc/nginx/sites-available/frps-https << 'EOF'
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/frp/certificate.pem;
    ssl_certificate_key /etc/frp/private.key;

    location / {
        proxy_pass http://127.0.0.1:7000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

# 启用配置
ln -s /etc/nginx/sites-available/frps-https /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

**2. 上传 Cloudflare Origin Certificate 到 FRPS 服务器**

```bash
# 从本地上传
scp certificate.pem root@frps-server-ip:/etc/frp/
scp private.key root@frps-server-ip:/etc/frp/

# 设置权限
ssh root@frps-server-ip
chmod 644 /etc/frp/certificate.pem
chmod 600 /etc/frp/private.key
```

**3. 配置 FRPC（内网）**

```ini
[web_https]
type = https
local_ip = 192.168.50.10
local_port = 443  # 内网应用 HTTPS 端口
custom_domains = yourdomain.com
```

**4. 修改 Cloudflare SSL/TLS 模式**

- Cloudflare 控制台 → SSL/TLS → 加密模式 → 完全（Full）

---

## 📝 总结

### 证书位置

**对于 Cloudflare 完全模式：**
- ✅ **证书应该配置在 FRPS 服务器上**（公网服务器）
- ✅ 使用 Cloudflare Origin Certificate 或 Let's Encrypt
- ✅ 通过 Nginx 反向代理提供 HTTPS

**内网应用：**
- 可以继续使用 HTTP（因为 Cloudflare 已经加密了）
- 或者使用 HTTPS（更安全）

### 你的情况

1. **在 FRPS 服务器**：
   - 下载 Cloudflare Origin Certificate
   - 配置 Nginx 提供 HTTPS
   - 转发到 FRPS 的 7000 端口

2. **在内网应用**（可选）：
   - 可以继续使用当前配置
   - 或者改为 HTTP（因为 Cloudflare 已经加密）

3. **在 Cloudflare**：
   - 修改 SSL/TLS 模式为"完全"

---

## 🔍 验证

```bash
# 1. 测试 FRPS 服务器 HTTPS
curl -k https://frps-server-ip:443

# 2. 测试通过域名访问
curl -I https://yourdomain.com

# 3. 检查 Cloudflare 源站连接
# 在 Cloudflare 控制台查看 SSL/TLS 状态
```
