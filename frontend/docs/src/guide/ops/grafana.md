# Grafana 配置指南

## 📋 概述

Grafana 用于监控系统性能和查看指标。本文档介绍 Grafana 的配置和使用。

---

## 🔧 配置说明

### 环境变量配置

在项目根目录的 `.env` 文件中配置：

```bash
# Grafana 管理员密码（必须配置）
GRAFANA_PASSWORD=$(openssl rand -base64 16)

# Grafana 管理员用户名（可选，默认为 admin）
GRAFANA_USERNAME=admin
```

### 默认登录信息

- **用户名**：`admin`（或 `.env` 中的 `GRAFANA_USERNAME`）
- **密码**：`.env` 文件中的 `GRAFANA_PASSWORD`，如果没有设置则默认为 `admin`

⚠️ **首次登录后会要求修改密码**

---

## 🌐 设置中文界面

### 方法一：通过用户偏好设置（推荐）

#### 步骤

1. **登录 Grafana**
   - 访问：`http://your-server-ip:3000` 或 `http://localhost:3000`
   - 使用默认账号登录

2. **打开用户偏好设置**
   - 点击左下角用户图标（或右上角头像）
   - 选择 **"Preferences"**（偏好设置）

3. **选择中文语言**
   - 找到 **"Language"**（语言）选项
   - 选择 **"中文（简体）"**

4. **保存设置**
   - 点击 **"Save"**（保存）按钮
   - 界面会立即刷新并显示为中文

### 方法二：通过配置文件设置（全局默认）

在 `docker-compose.prod.yml` 中配置：

```yaml
grafana:
  environment:
    - GF_DEFAULT_LANGUAGE=zh-Hans
```

---

## 🔑 密码管理

### 添加密码到现有部署

如果 `.env` 文件中没有 `GRAFANA_PASSWORD`，Grafana 会使用默认密码 `admin`，存在安全风险。

**解决方案**：

```bash
# 1. 生成密码（16字符）
NEW_PASSWORD=$(openssl rand -base64 16)

# 2. 添加到 .env 文件
echo "GRAFANA_PASSWORD=$NEW_PASSWORD" >> .env

# 3. 查看生成的密码
cat .env | grep GRAFANA_PASSWORD

# 4. 重启 Grafana 使配置生效
cd docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart grafana
```

### 验证密码配置

```bash
# 检查容器环境变量
docker exec law-firm-grafana env | grep GF_SECURITY_ADMIN_PASSWORD

# 应该显示：
# GF_SECURITY_ADMIN_PASSWORD=你的密码
```

---

## 📊 访问地址

部署成功后，可以通过以下地址访问 Grafana：

- **本地开发**：`http://localhost:3000`
- **生产环境**：`http://your-server-ip:3000` 或 `http://grafana.example.com`

---

## 🔍 常见问题

### 问题1：忘记密码

**解决方案**：

1. **查看 .env 文件中的密码**：
   ```bash
   cat .env | grep GRAFANA_PASSWORD
   ```

2. **如果 .env 中没有配置，使用默认密码**：
   - 用户名：`admin`
   - 密码：`admin`

3. **重置密码**：
   ```bash
   # 进入 Grafana 容器
   docker exec -it law-firm-grafana grafana-cli admin reset-admin-password <新密码>
   ```

### 问题2：无法访问 Grafana

**检查项**：

1. **检查容器状态**：
   ```bash
   docker ps | grep grafana
   ```

2. **检查端口是否开放**：
   ```bash
   # 检查 3000 端口
   sudo lsof -i :3000
   ```

3. **查看日志**：
   ```bash
   docker logs law-firm-grafana
   ```

---

## 📚 相关文档

- [部署指南](./deployment.md)
- [配置说明](./configuration.md)
- [监控告警](./monitoring.md)

---

**最后更新**: 2026-01-27
