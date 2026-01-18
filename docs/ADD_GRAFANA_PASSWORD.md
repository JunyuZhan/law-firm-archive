# 添加 Grafana 密码到现有部署

## 🔍 问题说明

如果 `.env` 文件中没有 `GRAFANA_PASSWORD`，Grafana 会使用默认密码 `admin`，存在安全风险。

---

## ✅ 解决方案

### 方法一：手动添加到 .env 文件（推荐）

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 生成密码（16字符）
NEW_PASSWORD=$(openssl rand -base64 16)

# 3. 添加到 .env 文件
echo "GRAFANA_PASSWORD=$NEW_PASSWORD" >> .env

# 4. 查看生成的密码
cat .env | grep GRAFANA_PASSWORD

# 5. 重启 Grafana 使配置生效
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

### 方法二：使用部署脚本自动添加

部署脚本已更新，会在下次部署时自动检测并生成 `GRAFANA_PASSWORD`。

```bash
# 运行部署脚本（会自动检测并添加缺失的密码）
cd /opt/law-firm
./scripts/deploy.sh --quick
```

### 方法三：一键脚本

```bash
# 创建并运行一键添加脚本
cat > /tmp/add-grafana-password.sh << 'EOF'
#!/bin/bash
cd /opt/law-firm

# 生成密码
NEW_PASSWORD=$(openssl rand -base64 16)

# 检查是否已存在
if grep -q "^GRAFANA_PASSWORD=" .env; then
    echo "GRAFANA_PASSWORD 已存在，更新为: $NEW_PASSWORD"
    sed -i "s|^GRAFANA_PASSWORD=.*|GRAFANA_PASSWORD=$NEW_PASSWORD|" .env
else
    echo "添加 GRAFANA_PASSWORD: $NEW_PASSWORD"
    echo "GRAFANA_PASSWORD=$NEW_PASSWORD" >> .env
fi

# 重启 Grafana
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana

echo ""
echo "✓ Grafana 密码已设置"
echo "用户名: admin"
echo "密码: $NEW_PASSWORD"
echo ""
echo "请保存此密码！"
EOF

chmod +x /tmp/add-grafana-password.sh
/tmp/add-grafana-password.sh
```

---

## 🔐 验证密码设置

### 1. 检查 .env 文件

```bash
cd /opt/law-firm
cat .env | grep GRAFANA_PASSWORD
```

**应该显示**：
```
GRAFANA_PASSWORD=your-generated-password-here
```

### 2. 检查容器环境变量

```bash
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD
```

**应该显示**：
```
GF_SECURITY_ADMIN_PASSWORD=your-generated-password-here
```

### 3. 测试登录

访问 `http://grafana.albertzhan.top`，使用：
- 用户名：`admin`
- 密码：`.env` 文件中的 `GRAFANA_PASSWORD`

---

## 📋 完整操作步骤

```bash
# 1. 进入项目目录
cd /opt/law-firm

# 2. 生成密码并添加到 .env
NEW_PASSWORD=$(openssl rand -base64 16)
echo "GRAFANA_PASSWORD=$NEW_PASSWORD" >> .env

# 3. 查看密码（保存好）
echo "Grafana 密码: $NEW_PASSWORD"
cat .env | grep GRAFANA_PASSWORD

# 4. 重启 Grafana
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana

# 5. 验证
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD
```

---

## 🔄 更新后的部署脚本

部署脚本已更新，现在会：

1. **首次部署时**：自动生成 `GRAFANA_PASSWORD`
2. **非首次部署时**：如果 `GRAFANA_PASSWORD` 不存在或使用默认值，自动生成并添加

下次运行 `./scripts/deploy.sh` 时会自动处理。

---

## 📝 总结

### 当前状态

- ⚠️ `.env` 文件中没有 `GRAFANA_PASSWORD`
- ⚠️ Grafana 使用默认密码 `admin`（不安全）

### 立即操作

```bash
# 快速添加密码
cd /opt/law-firm
echo "GRAFANA_PASSWORD=$(openssl rand -base64 16)" >> .env
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

### 查看密码

```bash
cat /opt/law-firm/.env | grep GRAFANA_PASSWORD
```

---

## 🔗 相关文档

- [监控服务登录指南](./MONITORING_LOGIN_GUIDE.md)
- [Grafana 密码问题解决](./GRAFANA_PASSWORD_FIX.md)
