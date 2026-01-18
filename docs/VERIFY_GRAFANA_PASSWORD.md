# 验证 Grafana 密码配置

## ✅ 密码已成功添加

你的 Grafana 密码已设置：
- **密码**: `grYdv2ptvisxGO0j2PQR3A==`
- **用户名**: `admin`

---

## 🔍 验证步骤

### 1. 检查容器环境变量

```bash
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD
```

**应该显示**：
```
GF_SECURITY_ADMIN_PASSWORD=grYdv2ptvisxGO0j2PQR3A==
```

### 2. 检查容器状态

```bash
docker ps | grep grafana
```

**应该显示**：
```
law-firm-grafana   Up   ...   3000/tcp
```

### 3. 检查容器日志

```bash
docker logs law-firm-grafana --tail 20
```

**应该看到**：
- 没有错误信息
- Grafana 正常启动

### 4. 测试登录

访问：`http://grafana.albertzhan.top`

**登录信息**：
- 用户名：`admin`
- 密码：`grYdv2ptvisxGO0j2PQR3A==`

---

## 🔐 安全建议

### 1. 保存密码

⚠️ **重要**：请妥善保存此密码！

```bash
# 查看密码
cat /opt/law-firm/.env | grep GRAFANA_PASSWORD
```

### 2. 首次登录后修改密码

登录 Grafana 后：
1. 点击右上角用户图标 → Profile
2. 修改密码
3. 保存

**注意**：如果通过 Web 界面修改密码，需要同步更新 `.env` 文件：

```bash
# 修改 .env 文件中的密码
cd /opt/law-firm
nano .env
# 更新 GRAFANA_PASSWORD=新密码

# 重启 Grafana
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

### 3. 限制访问

建议：
- ⚠️ 关闭 Cloudflare 代理（仅内网访问）
- ⚠️ 或使用 Cloudflare Access 保护
- ⚠️ 定期更新密码

---

## 📋 完整验证命令

```bash
# 1. 检查 .env 文件
cd /opt/law-firm
cat .env | grep GRAFANA_PASSWORD

# 2. 检查容器环境变量
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD

# 3. 检查容器状态
docker ps | grep grafana

# 4. 检查容器日志
docker logs law-firm-grafana --tail 10

# 5. 测试访问
curl -I http://localhost:3000
```

---

## ✅ 配置完成

你的 Grafana 密码配置已完成：

- ✅ 密码已添加到 `.env` 文件
- ✅ Grafana 容器已重启
- ✅ 可以使用新密码登录

**登录信息**：
- 地址：`http://grafana.albertzhan.top`
- 用户名：`admin`
- 密码：`grYdv2ptvisxGO0j2PQR3A==`

---

## 🔗 相关文档

- [监控服务登录指南](./MONITORING_LOGIN_GUIDE.md)
- [添加 Grafana 密码](./ADD_GRAFANA_PASSWORD.md)
