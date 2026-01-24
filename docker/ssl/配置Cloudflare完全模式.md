# 配置 Cloudflare 完全模式指南

## 📋 目标

将 Cloudflare SSL/TLS 模式从"灵活"改为"完全"，实现端到端 HTTPS 加密。

## 🔄 架构变化

**灵活模式（当前）：**
```
用户 → Cloudflare (HTTPS) → FRPC (HTTP) → 内网应用 (HTTP)
```

**完全模式（目标）：**
```
用户 → Cloudflare (HTTPS) → FRPC (HTTPS) → 内网应用 (HTTPS)
```

---

## ✅ 配置步骤

### 步骤1：确保内网应用 HTTPS 正常

**检查内网应用 HTTPS：**
```bash
# SSH 到内网服务器
ssh root@192.168.50.10

# 测试 HTTPS
curl -k https://localhost:443
# 应该返回正常响应

# 检查证书
openssl s_client -connect localhost:443 -showcerts < /dev/null
```

**✅ 你的应用已经配置好 HTTPS（443端口），这一步已完成**

---

### 步骤2：配置 FRPC 支持 HTTPS 转发

#### 2.1 公网 FRPC 服务器配置（frps.ini）

```ini
[common]
bind_port = 7000
# HTTPS 端口（如果需要）
vhost_https_port = 7443
# 如果需要认证
privilege_token = your_secure_token
```

#### 2.2 内网 FRPC 客户端配置（frpc.ini）

**重要：需要添加 HTTPS 类型的转发**

```ini
[common]
server_addr = 你的公网frpc服务器IP
server_port = 7000
# 如果需要认证
privilege_token = your_secure_token

# HTTP 转发（保留，用于兼容）
[web_http]
type = http
local_ip = 192.168.50.10
local_port = 80
custom_domains = yourdomain.com

# HTTPS 转发（新增，用于完全模式）
[web_https]
type = https
local_ip = 192.168.50.10
local_port = 443
custom_domains = yourdomain.com
```

**注意：**
- `local_port = 443` 对应内网应用的 HTTPS 端口
- `custom_domains` 是你的 Cloudflare 域名

---

### 步骤3：重启 FRPC 服务

```bash
# 在内网服务器上
systemctl restart frpc
# 或
docker restart frpc_container

# 检查日志
journalctl -u frpc -f
# 或
docker logs frpc_container
```

---

### 步骤4：在 Cloudflare 中修改 SSL/TLS 模式

1. **登录 Cloudflare 控制台**
   - 访问 https://dash.cloudflare.com
   - 选择你的域名

2. **进入 SSL/TLS 设置**
   - 左侧菜单：SSL/TLS
   - 或直接访问：`https://dash.cloudflare.com/[你的账户ID]/[域名]/ssl-tls`

3. **修改加密模式**
   - 找到"加密模式"（Encryption mode）
   - 从"灵活"（Flexible）改为"完全"（Full）
   - 保存

4. **等待生效**
   - 通常几分钟内生效
   - Cloudflare 会自动测试源站 HTTPS 连接

---

### 步骤5：验证配置

#### 5.1 测试内网 HTTPS

```bash
# 在内网服务器上测试
curl -k https://localhost:443
curl -k https://192.168.50.10:443
```

#### 5.2 测试通过 FRPC 访问

```bash
# 在公网服务器上测试（如果有访问权限）
curl -k https://yourdomain.com
```

#### 5.3 检查 Cloudflare SSL/TLS 状态

在 Cloudflare 控制台：
- SSL/TLS → 概述
- 应该显示"完全"模式
- 如果显示错误，检查源站 HTTPS 配置

---

## ⚠️ 常见问题

### 问题1：Cloudflare 显示"源站证书错误"

**原因：**
- Cloudflare 无法验证内网应用的 HTTPS 证书
- 证书是自签名的，Cloudflare 不信任

**解决方案：**

**方案A：使用 Cloudflare Origin Certificate（推荐）**

1. 在 Cloudflare 控制台：
   - SSL/TLS → 源服务器 → 创建证书

2. 下载证书：
   - 证书（Certificate）
   - 私钥（Private Key）

3. 替换内网应用的证书：
   ```bash
   # 上传到服务器
   scp certificate.pem root@192.168.50.10:/opt/law-firm/docker/ssl/fullchain.pem
   scp private.key root@192.168.50.10:/opt/law-firm/docker/ssl/privkey.pem
   
   # 设置权限
   chmod 644 /opt/law-firm/docker/ssl/fullchain.pem
   chmod 600 /opt/law-firm/docker/ssl/privkey.pem
   
   # 重启前端服务
   cd /opt/law-firm/docker
   docker compose -f docker-compose.prod.yml restart frontend
   ```

**方案B：使用"完全（严格）"模式（需要有效证书）**

如果使用 Let's Encrypt 证书，可以使用"完全（严格）"模式。

---

### 问题2：FRPC 连接失败

**检查：**
```bash
# 检查 FRPC 日志
journalctl -u frpc -n 50
# 或
docker logs frpc_container

# 检查端口是否开放
netstat -tlnp | grep 443
```

**常见原因：**
- FRPC 配置错误
- 端口冲突
- 防火墙阻止

---

### 问题3：HTTPS 连接超时

**检查：**
1. 内网应用 HTTPS 是否正常
2. FRPC 是否正确转发到 443 端口
3. Cloudflare 是否能访问源站

**测试：**
```bash
# 在内网服务器上
curl -k https://localhost:443

# 检查 Cloudflare 源站检查
# 在 Cloudflare 控制台：SSL/TLS → 源服务器 → 查看源站连接状态
```

---

## 📝 配置检查清单

- [ ] 内网应用 HTTPS 正常（443端口）
- [ ] FRPC 配置了 HTTPS 转发（type = https, local_port = 443）
- [ ] FRPC 服务已重启
- [ ] Cloudflare SSL/TLS 模式改为"完全"
- [ ] 测试通过域名访问正常
- [ ] Cloudflare 源站连接状态正常

---

## 🎯 推荐配置

### 使用 Cloudflare Origin Certificate（最简单）

1. **创建 Origin Certificate**
   - Cloudflare 控制台 → SSL/TLS → 源服务器 → 创建证书
   - 有效期：15年
   - 域名：你的域名（如：`yourdomain.com`）

2. **下载证书**
   - 证书文件（Certificate）
   - 私钥文件（Private Key）

3. **替换内网应用证书**
   ```bash
   # 上传证书
   scp certificate.pem root@192.168.50.10:/opt/law-firm/docker/ssl/fullchain.pem
   scp private.key root@192.168.50.10:/opt/law-firm/docker/ssl/privkey.pem
   
   # 重启服务
   ssh root@192.168.50.10
   cd /opt/law-firm/docker
   docker compose -f docker-compose.prod.yml restart frontend
   ```

4. **修改 Cloudflare 模式**
   - SSL/TLS → 加密模式 → 完全（Full）

5. **验证**
   - 访问 `https://yourdomain.com`
   - 应该正常，无警告

---

## 🔍 验证命令

```bash
# 1. 检查内网 HTTPS
curl -k -I https://192.168.50.10:443

# 2. 检查证书
openssl s_client -connect 192.168.50.10:443 -servername yourdomain.com < /dev/null

# 3. 检查 FRPC 状态
# 查看 FRPC 日志，确认 HTTPS 转发正常

# 4. 检查 Cloudflare 状态
# 在 Cloudflare 控制台查看源站连接状态
```
