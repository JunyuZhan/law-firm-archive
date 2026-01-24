# SSL 证书配置说明

## 📁 证书存放位置

将您的 SSL 证书文件放在此目录下：

```
docker/ssl/
├── fullchain.pem    # 完整证书链（包含服务器证书和中间证书）
├── privkey.pem      # 私钥文件
└── README.md        # 本说明文件
```

## 🔐 证书文件说明

### 1. 完整证书链 (fullchain.pem)
- 包含服务器证书和所有中间证书
- 通常由证书颁发机构提供
- 文件名也可以是：`cert.pem`、`certificate.crt`、`fullchain.crt`

### 2. 私钥文件 (privkey.pem)
- 服务器私钥，必须保密
- 文件名也可以是：`key.pem`、`private.key`、`privkey.key`

## 📝 证书获取方式

### 方式一：Let's Encrypt（免费，推荐）
```bash
# 安装 certbot
sudo apt-get install certbot

# 获取证书（需要域名已解析到服务器）
sudo certbot certonly --standalone -d yourdomain.com

# 证书位置通常在：
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem

# 复制到项目目录
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem docker/ssl/fullchain.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem docker/ssl/privkey.pem
sudo chown $USER:$USER docker/ssl/*.pem
```

### 方式二：商业证书
- 从证书提供商（如阿里云、腾讯云、DigiCert等）下载证书
- 将证书文件和私钥文件复制到此目录

### 方式三：自签名证书（仅用于测试）
```bash
# 生成自签名证书（仅用于开发测试，生产环境请使用正式证书）
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/ssl/privkey.pem \
  -out docker/ssl/fullchain.pem \
  -subj "/C=CN/ST=State/L=City/O=Organization/CN=yourdomain.com"
```

## ⚙️ 配置使用

### 1. 修改 docker-compose 文件

在 `docker-compose.prod.yml` 或 `docker-compose.swarm.yml` 的 `frontend` 服务中添加 volume 挂载：

```yaml
frontend:
  volumes:
    - ./ssl:/etc/nginx/ssl:ro  # 只读挂载SSL证书目录
```

### 2. 使用支持 HTTPS 的 Nginx 配置

创建 `frontend/scripts/deploy/nginx-ssl.conf` 文件，或修改现有的 `nginx.conf` 添加 HTTPS 支持。

### 3. 更新端口映射

在 docker-compose 文件中添加 443 端口映射：

```yaml
frontend:
  ports:
    - "80:8080"   # HTTP
    - "443:8443"  # HTTPS
```

## 🔒 安全建议

1. **文件权限**：确保私钥文件权限正确
   ```bash
   chmod 600 docker/ssl/privkey.pem
   chmod 644 docker/ssl/fullchain.pem
   ```

2. **不要提交到 Git**：将证书文件添加到 `.gitignore`
   ```bash
   echo "docker/ssl/*.pem" >> .gitignore
   echo "docker/ssl/*.key" >> .gitignore
   echo "docker/ssl/*.crt" >> .gitignore
   ```

3. **定期更新**：Let's Encrypt 证书有效期为 90 天，建议设置自动续期
   ```bash
   # 添加到 crontab
   0 0 1 * * certbot renew --quiet && docker compose restart frontend
   ```

## 📚 相关文档

- [Nginx SSL 配置文档](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Let's Encrypt 文档](https://letsencrypt.org/docs/)
- [Docker Compose volumes 文档](https://docs.docker.com/compose/compose-file/compose-file-v3/#volumes)
