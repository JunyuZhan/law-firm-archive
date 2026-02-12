# SSL 证书完整配置指南

本文档整合了所有 SSL 证书相关的配置说明，包括基础配置、Cloudflare 集成、FRP 架构配置等。

---

## 目录

1. [快速开始](#快速开始)
2. [证书文件说明](#证书文件说明)
3. [获取证书方式](#获取证书方式)
4. [配置使用](#配置使用)
5. [Cloudflare 集成](#cloudflare-集成)
6. [FRP 架构配置](#frp-架构配置)
7. [消除浏览器安全提示](#消除浏览器安全提示)
8. [部署注意事项](#部署注意事项)
9. [常见问题](#常见问题)

---

## 快速开始

**把 SSL 证书的两个文件放到 `docker/ssl/` 文件夹里就可以了！**

```
docker/ssl/
├── fullchain.pem    # 证书文件
├── privkey.pem      # 私钥文件
└── README.md        # 基础说明
```

### 快速配置步骤

```bash
# 1. 复制证书文件
cp /你的证书路径/fullchain.pem docker/ssl/fullchain.pem
cp /你的证书路径/privkey.pem docker/ssl/privkey.pem

# 2. 设置文件权限
chmod 644 docker/ssl/fullchain.pem
chmod 600 docker/ssl/privkey.pem

# 3. 重启服务
cd docker
docker compose -f docker-compose.yml restart frontend
```

---

## 证书文件说明

### 完整证书链 (fullchain.pem)
- 包含服务器证书和所有中间证书
- 通常由证书颁发机构提供
- 文件名也可以是：`cert.pem`、`certificate.crt`、`fullchain.crt`

### 私钥文件 (privkey.pem)
- 服务器私钥，必须保密
- 文件名也可以是：`key.pem`、`private.key`、`privkey.key`

---

## 获取证书方式

### 方式一：Let's Encrypt（免费，推荐）

```bash
# 安装 certbot
sudo apt-get install certbot

# 获取证书（需要域名已解析到服务器）
sudo certbot certonly --standalone -d yourdomain.com

# 证书位置：
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem

# 复制到项目目录
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem docker/ssl/fullchain.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem docker/ssl/privkey.pem
sudo chown $USER:$USER docker/ssl/*.pem
```

**优点：**
- ✅ 浏览器完全信任
- ✅ 免费
- ✅ 支持自动续期（90天有效期）

### 方式二：Cloudflare Origin Certificate

如果使用 Cloudflare，推荐使用 Origin Certificate：

1. 登录 Cloudflare 控制台
2. SSL/TLS → 源服务器 → 创建证书
3. 域名：你的域名（如：`yourdomain.com`）
4. 有效期：15年
5. 下载证书和私钥

### 方式三：商业证书

从证书提供商（如阿里云、腾讯云、DigiCert等）购买证书。

### 方式四：自签名证书（仅用于测试）

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/ssl/privkey.pem \
  -out docker/ssl/fullchain.pem \
  -subj "/C=CN/ST=State/L=City/O=Organization/CN=yourdomain.com"
```

---

## 配置使用

### 1. 修改 docker-compose 文件

在 `docker-compose.yml` 的 `frontend` 服务中添加：

```yaml
frontend:
  volumes:
    - ./ssl:/etc/nginx/ssl:ro
  ports:
    - "80:8080"
    - "443:8443"
```

### 2. Nginx SSL 配置

使用 `frontend/scripts/deploy/nginx-ssl.conf` 配置文件。

### 3. 安全建议

```bash
# 文件权限
chmod 600 docker/ssl/privkey.pem
chmod 644 docker/ssl/fullchain.pem

# 添加到 .gitignore
echo "docker/ssl/*.pem" >> .gitignore
echo "docker/ssl/*.key" >> .gitignore
echo "docker/ssl/*.crt" >> .gitignore
```

---

## Cloudflare 集成

### 架构说明

```
用户浏览器
    ↓ HTTPS (Cloudflare证书)
Cloudflare (代理模式)
    ↓ HTTP/HTTPS
源站服务器
```

### SSL/TLS 模式说明

| 模式 | 用户→Cloudflare | Cloudflare→源站 | 适用场景 |
|------|-----------------|-----------------|----------|
| 灵活 | HTTPS | HTTP | 快速配置 |
| 完全 | HTTPS | HTTPS | 推荐配置 |
| 完全（严格）| HTTPS | HTTPS（验证证书）| 高安全需求 |

### 配置完全模式

1. **确保源站 HTTPS 正常**
   ```bash
   curl -k https://localhost:443
   ```

2. **修改 Cloudflare 设置**
   - 登录 Cloudflare 控制台
   - SSL/TLS → 加密模式 → 完全（Full）

3. **使用 Cloudflare Origin Certificate**
   - SSL/TLS → 源服务器 → 创建证书
   - 下载并替换服务器证书

---

## FRP 架构配置

### 架构说明

```
用户浏览器
    ↓ HTTPS (Cloudflare证书)
Cloudflare (代理)
    ↓ HTTPS
FRPS 服务器 (公网)
    ↓ HTTP
FRPC 客户端 (内网)
    ↓ HTTP/HTTPS
内网应用
```

### FRPS 服务器配置

**方式一：使用 Nginx 反向代理（推荐）**

```nginx
# /etc/nginx/sites-available/frps-https
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
```

### FRPC 客户端配置

```ini
[common]
server_addr = 你的公网服务器IP
server_port = 7000

[web_https]
type = https
local_ip = 192.168.50.10
local_port = 443
custom_domains = yourdomain.com
```

### 宝塔面板配置

1. 创建网站并配置 SSL
2. 配置反向代理到 `http://127.0.0.1:7000`
3. 修改 Cloudflare 为完全模式

---

## 消除浏览器安全提示

### 方案一：使用受信任的证书（推荐）

使用 Let's Encrypt 或 Cloudflare Origin Certificate。

### 方案二：导入 CA 证书

如果使用自签名证书，需要用户导入 CA 证书：

**Windows：**
1. 双击 `ca.crt` 文件
2. 点击"安装证书"
3. 选择"本地计算机" → "受信任的根证书颁发机构"

**macOS：**
```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ca.crt
```

**Linux：**
```bash
sudo cp ca.crt /usr/local/share/ca-certificates/law-firm-ca.crt
sudo update-ca-certificates
```

---

## 部署注意事项

### SSL 证书不会通过 Git 同步

证书文件是敏感信息，已在 `.gitignore` 中排除：
- ✅ 代码拉取后：证书文件不会自动下载
- ✅ 必须单独上传：每次部署都需要手动上传证书

### 部署流程

```bash
# 1. 拉取代码
git pull

# 2. 上传证书（使用脚本）
./scripts/upload-ssl-certs.sh <服务器IP> [用户名]

# 3. 重新部署
cd docker
docker compose -f docker-compose.yml up -d --build
```

---

## 常见问题

### Q: 证书文件名不一样怎么办？

重命名文件：
```bash
mv certificate.crt fullchain.pem
mv private.key privkey.pem
```

### Q: 导入证书后还是提示不安全？

1. 确认导入的是 CA 证书（`ca.crt`）
2. 确认导入到了"受信任的根证书颁发机构"
3. 清除浏览器缓存，重启浏览器

### Q: Cloudflare 显示"源站证书错误"？

1. 检查证书是否正确配置
2. 使用 Cloudflare Origin Certificate
3. 确认域名匹配

### Q: 502 Bad Gateway？

1. 检查 Nginx 配置：`nginx -t`
2. 检查服务是否运行：`docker ps`
3. 检查端口映射是否正确

### Q: 证书过期了怎么办？

```bash
# Let's Encrypt 自动续期
certbot renew --quiet

# 手动更新
./scripts/generate-server-cert.sh 192.168.50.10
```

---

## 验证配置

```bash
# 检查证书文件
ls -la docker/ssl/

# 检查容器挂载
docker exec law-firm-frontend ls -la /etc/nginx/ssl/

# 检查 Nginx 配置
docker exec law-firm-frontend nginx -t

# 检查证书信息
openssl x509 -in docker/ssl/fullchain.pem -text -noout

# 测试 HTTPS 连接
curl -I https://localhost:443
```
