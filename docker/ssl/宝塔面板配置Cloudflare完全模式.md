# 宝塔面板 + FRPS Docker 配置 Cloudflare 完全模式

## 📋 架构说明

```
用户浏览器
    ↓ HTTPS (Cloudflare证书)
Cloudflare (代理)
    ↓ HTTPS (验证源站证书)
宝塔 Nginx (443端口，证书配置在这里)
    ↓ HTTP
FRPS Docker 容器 (7000端口)
    ↓ HTTP
FRPC 客户端 (内网)
    ↓ HTTP
内网应用 (192.168.50.10)
```

## ✅ 配置步骤

### 步骤1：在宝塔面板创建网站

1. **登录宝塔面板**
   - 访问：`http://你的服务器IP:8888`
   - 或通过域名访问

2. **创建网站**
   - 网站 → 添加站点
   - 域名：`yourdomain.com`
   - 其他设置默认即可

---

### 步骤2：配置 SSL 证书

#### 方式一：使用 Cloudflare Origin Certificate（推荐）

1. **在 Cloudflare 创建 Origin Certificate**
   - Cloudflare 控制台 → SSL/TLS → 源服务器 → 创建证书
   - 域名：`yourdomain.com`
   - 有效期：15年
   - 下载证书和私钥

2. **在宝塔面板配置 SSL**
   - 网站 → 选择你的网站 → 设置 → SSL
   - 选择"其他证书"
   - 粘贴证书内容（Certificate）
   - 粘贴私钥内容（Private Key）
   - 保存

#### 方式二：使用 Let's Encrypt（如果有域名解析）

1. **在宝塔面板申请 Let's Encrypt 证书**
   - 网站 → 选择你的网站 → 设置 → SSL
   - 选择"Let's Encrypt"
   - 点击"申请"
   - 等待申请完成

---

### 步骤3：配置 Nginx 反向代理到 FRPS

1. **在宝塔面板配置反向代理**
   - 网站 → 选择你的网站 → 设置 → 反向代理
   - 添加反向代理：
     - **代理名称**：frps
     - **目标URL**：`http://127.0.0.1:7000`（FRPS 容器端口）
     - **发送域名**：`$host`
     - **缓存**：关闭

2. **或者手动编辑 Nginx 配置**

   在宝塔面板：
   - 网站 → 选择你的网站 → 设置 → 配置文件
   
   添加或修改配置：
   ```nginx
   server {
       listen 443 ssl http2;
       server_name yourdomain.com;
       
       # SSL 证书（宝塔自动配置）
       ssl_certificate /www/server/panel/vhost/cert/yourdomain.com/fullchain.pem;
       ssl_certificate_key /www/server/panel/vhost/cert/yourdomain.com/privkey.pem;
       
       # 反向代理到 FRPS 容器
       location / {
           proxy_pass http://127.0.0.1:7000;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_set_header X-Forwarded-Host $host;
           
           # WebSocket 支持（如果需要）
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection "upgrade";
       }
   }
   ```

---

### 步骤4：确保 FRPS 容器端口映射正确

**检查 FRPS Docker 容器配置：**

```bash
# SSH 到公网服务器
ssh root@frps-server-ip

# 检查 FRPS 容器
docker ps | grep frps

# 检查端口映射
docker port frps_container_name
# 应该看到：7000/tcp -> 0.0.0.0:7000
```

**如果 FRPS 容器没有映射 7000 端口到宿主机：**

```bash
# 查看 FRPS 容器配置
docker inspect frps_container_name | grep -A 10 Ports

# 如果需要，重启容器并映射端口
docker stop frps_container_name
docker start -p 7000:7000 frps_container_name
```

---

### 步骤5：配置 FRPC（内网）

**内网 FRPC 配置（frpc.ini）：**

```ini
[common]
server_addr = 你的公网服务器IP
server_port = 7000

# HTTP 转发（用于 Cloudflare 完全模式）
[web]
type = http
local_ip = 192.168.50.10
local_port = 80  # 内网应用 HTTP 端口
custom_domains = yourdomain.com

# 或者如果内网应用使用 HTTPS
[web_https]
type = https
local_ip = 192.168.50.10
local_port = 443  # 内网应用 HTTPS 端口
custom_domains = yourdomain.com
```

---

### 步骤6：修改 Cloudflare SSL/TLS 模式

1. **登录 Cloudflare 控制台**
   - 选择你的域名
   - SSL/TLS → 加密模式

2. **修改为"完全"（Full）**
   - 从"灵活"改为"完全"
   - 保存

3. **等待生效**
   - 通常几分钟内生效
   - Cloudflare 会自动测试源站 HTTPS 连接

---

## 🔍 验证配置

### 1. 测试宝塔 Nginx HTTPS

```bash
# SSH 到公网服务器
ssh root@frps-server-ip

# 测试 HTTPS
curl -I https://yourdomain.com
# 应该返回 200 OK

# 检查证书
openssl s_client -connect yourdomain.com:443 -servername yourdomain.com < /dev/null
```

### 2. 测试 FRPS 容器连接

```bash
# 在公网服务器上
curl http://127.0.0.1:7000
# 应该能连接到 FRPS
```

### 3. 测试完整链路

```bash
# 从外部访问
curl -I https://yourdomain.com
# 应该返回内网应用的响应
```

### 4. 检查 Cloudflare 状态

- Cloudflare 控制台 → SSL/TLS → 概述
- 应该显示"完全"模式
- 源站连接状态应该正常

---

## 📝 配置检查清单

- [ ] 在宝塔面板创建了网站
- [ ] 配置了 SSL 证书（Cloudflare Origin Certificate 或 Let's Encrypt）
- [ ] 配置了 Nginx 反向代理到 `http://127.0.0.1:7000`
- [ ] FRPS 容器端口 7000 映射到宿主机
- [ ] FRPC 配置正确（转发到内网应用）
- [ ] Cloudflare SSL/TLS 模式改为"完全"
- [ ] 测试通过域名访问正常

---

## ⚠️ 常见问题

### 问题1：宝塔 Nginx 无法连接到 FRPS 容器

**原因：**
- FRPS 容器端口没有映射到宿主机
- 或者使用了容器网络，Nginx 无法访问

**解决方案：**
```bash
# 检查 FRPS 容器端口映射
docker ps | grep frps

# 确保端口映射正确
# 应该看到：0.0.0.0:7000->7000/tcp

# 如果使用 Docker Compose，检查配置
# 确保 ports: - "7000:7000"
```

### 问题2：Cloudflare 显示"源站证书错误"

**检查：**
1. 宝塔 SSL 证书是否正确配置
2. 证书是否过期
3. 域名是否匹配

**解决方案：**
- 重新申请或更新证书
- 检查证书域名是否包含你的域名

### 问题3：502 Bad Gateway

**原因：**
- Nginx 无法连接到 FRPS 容器
- FRPS 容器未运行
- 端口映射错误

**检查：**
```bash
# 检查 FRPS 容器状态
docker ps | grep frps

# 检查端口
netstat -tlnp | grep 7000

# 测试连接
curl http://127.0.0.1:7000
```

---

## 🎯 完整配置示例

### 宝塔 Nginx 配置（完整）

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    # SSL 证书（宝塔自动配置的路径）
    ssl_certificate /www/server/panel/vhost/cert/yourdomain.com/fullchain.pem;
    ssl_certificate_key /www/server/panel/vhost/cert/yourdomain.com/privkey.pem;
    
    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers off;
    
    # 反向代理到 FRPS
    location / {
        proxy_pass http://127.0.0.1:7000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$host$request_uri;
}
```

---

## 📞 需要帮助？

如果遇到问题：
1. 检查宝塔面板日志：网站 → 日志
2. 检查 FRPS 容器日志：`docker logs frps_container_name`
3. 检查 Nginx 配置：`nginx -t`
