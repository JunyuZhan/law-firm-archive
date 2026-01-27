# OnlyOffice 配置指南

## 📋 概述

OnlyOffice 用于在线预览和编辑 Word、Excel、PPT 等文档。本文档介绍 OnlyOffice 的配置和常见问题解决。

---

## 🔧 配置说明

### 环境变量配置

在项目根目录的 `.env` 文件中配置：

```bash
# OnlyOffice 外部访问地址（重要！）
# 如果使用 IP 访问
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://192.168.50.10

# 如果使用域名访问
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://oa.example.com

# OnlyOffice JWT 密钥（必须配置）
ONLYOFFICE_JWT_SECRET=$(openssl rand -base64 64)

# OnlyOffice JWT 启用
ONLYOFFICE_JWT_ENABLED=true
```

### 配置说明

**ONLYOFFICE_EXTERNAL_ACCESS_URL**：
- OnlyOffice 容器通过 Nginx 代理访问文件，而不是直接访问 Docker 内部地址
- 必须配置为外部可访问的地址（IP 或域名）
- 如果不配置，OnlyOffice 可能无法正常加载文档

**ONLYOFFICE_JWT_SECRET**：
- 用于 OnlyOffice 与后端之间的安全通信
- 必须使用强随机密钥
- 生成命令：`openssl rand -base64 64`

---

## 🐛 常见问题

### 问题1：文档加载失败

**错误信息**：
```
文档加载失败: 打开文件时出错
文件内容与文件扩展名不匹配。
```

**原因**：
- OnlyOffice 容器无法访问宿主机 IP
- 后端生成的文件 URL 不正确

**解决方案**：

1. **配置外部访问地址**：
   ```bash
   # 在 .env 文件中添加
   ONLYOFFICE_EXTERNAL_ACCESS_URL=http://192.168.50.10
   ```

2. **重启后端服务**：
   ```bash
   cd docker
   docker compose --env-file ../.env -f docker-compose.prod.yml restart backend
   ```

3. **验证配置**：
   ```bash
   # 查看后端日志
   docker logs law-firm-backend | grep "OnlyOffice"
   ```

### 问题2：CORS 错误

**错误信息**：
```
Access to XMLHttpRequest at 'http://127.0.0.1:8088/...' 
from origin 'http://localhost:5666' has been blocked by CORS policy
```

**原因**：
- OnlyOffice 内部资源使用 `http://127.0.0.1:8088` 直接访问
- 浏览器从不同域名访问，触发 CORS 策略

**解决方案**：
- 确保配置了 `ONLYOFFICE_EXTERNAL_ACCESS_URL`
- 确保 OnlyOffice 通过 Nginx 代理访问

### 问题3：Token 验证失败

**错误信息**：
```
Token 验证失败
```

**原因**：
- JWT 密钥未配置或配置错误
- JWT 验证未启用

**解决方案**：

1. **配置 JWT 密钥**：
   ```bash
   # 在 .env 文件中添加
   ONLYOFFICE_JWT_SECRET=$(openssl rand -base64 64)
   ONLYOFFICE_JWT_ENABLED=true
   ```

2. **重启 OnlyOffice 服务**：
   ```bash
   cd docker
   docker compose --env-file ../.env -f docker-compose.prod.yml restart onlyoffice
   ```

---

## 📊 数据流向

### 文档预览/编辑流程

```
用户点击预览/编辑
  ↓
前端请求后端获取文档 URL
  ↓
后端生成 OnlyOffice 配置（包含文档 URL）
  ↓
前端加载 OnlyOffice 编辑器
  ↓
OnlyOffice 容器通过 Nginx 代理访问文档
  ↓
文档加载成功 ✅
```

### 网络架构

```
浏览器（用户机器）
  ↓ HTTP 请求
  ↓ 访问外部地址
Nginx（宿主机）
  ↓ 代理到 Docker 网络
OnlyOffice 容器
  ↓ 通过 Nginx 代理访问文件
后端服务（生成文档 URL）
```

---

## 🔍 验证配置

### 检查环境变量

```bash
# 检查 OnlyOffice 相关配置
docker exec law-firm-backend env | grep ONLYOFFICE
```

应该看到：
```
ONLYOFFICE_EXTERNAL_ACCESS_URL=http://192.168.50.10
ONLYOFFICE_JWT_SECRET=...
ONLYOFFICE_JWT_ENABLED=true
```

### 检查服务状态

```bash
# 检查 OnlyOffice 容器状态
docker ps | grep onlyoffice

# 查看 OnlyOffice 日志
docker logs law-firm-onlyoffice
```

---

## 🔐 OnlyOffice 欢迎页面密码

OnlyOffice Document Server 的欢迎页面在首次访问时需要输入 **Bootstrap Code**（引导代码）来完成初始化设置。

### 访问地址

- 直接访问：`http://localhost:8088/welcome/`
- 通过代理：`http://localhost/docs/onlyoffice/welcome/`

### 获取 Bootstrap Code

**方法一：查看 Admin Panel 日志**（推荐）

```bash
docker exec law-firm-onlyoffice cat /var/log/onlyoffice/documentserver/adminpanel/out.log | grep -i bootstrap
```

**方法二：查看容器启动日志**

```bash
docker logs law-firm-onlyoffice | grep -i bootstrap
```

**方法三：实时监控日志**

```bash
# 终端1：监控日志
docker exec law-firm-onlyoffice tail -f /var/log/onlyoffice/documentserver/adminpanel/out.log

# 终端2：访问 welcome 页面
# 浏览器访问 http://localhost:8088/welcome/
# 日志中会显示 Bootstrap Code
```

### Bootstrap Code 格式

Bootstrap Code 通常是 6 位数字，例如：`123456`

### 使用说明

1. 访问 OnlyOffice 欢迎页面
2. 输入从日志中获取的 Bootstrap Code
3. 完成初始化设置
4. 之后访问欢迎页面不再需要 Bootstrap Code

---

## 📚 相关文档

- [部署指南](./deployment.md)
- [配置说明](./configuration.md)
- [故障排查](./troubleshooting.md)

---

**最后更新**: 2026-01-27
