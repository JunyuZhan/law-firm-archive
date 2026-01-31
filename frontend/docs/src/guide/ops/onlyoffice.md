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

## 🔍 配置检查清单

### 关键配置检查

#### 1. MinIO 容器名称

**docker-compose.prod.yml 中的配置：**
```yaml
minio:
  container_name: law-firm-minio  # ← 容器名称
```

**Backend 环境变量配置：**
```yaml
backend:
  environment:
    - MINIO_ENDPOINT=http://law-firm-minio:9000  # ✅ 使用完整容器名
    - FILE_SERVER_URL=http://law-firm-minio:9000  # ✅ 使用完整容器名
```

**检查点：**
- ✅ 容器名：`law-firm-minio`
- ✅ Backend 使用 `http://law-firm-minio:9000` 访问 MinIO
- ✅ OnlyOffice 配置的 `FILE_SERVER_URL` 也应该使用 `http://law-firm-minio:9000`

#### 2. OnlyOffice 文件获取流程

**关键代码：`buildFileUrlForDocument()`**
```java
public String buildFileUrlForDocument(final Long documentId) {
    // 使用 callback-url 构建 baseUrl
    String baseUrl = this.config.getCallbackUrl();  // http://backend:8080/api
    // 移除 /api
    baseUrl = baseUrl.substring(0, baseUrl.length() - 4);  // http://backend:8080
    
    // 生成代理 URL
    String proxyUrl = baseUrl + "/api/document/" + documentId + "/file-proxy?token=...";
    // 结果：http://backend:8080/api/document/{id}/file-proxy?token=...
    return proxyUrl;
}
```

**流程：**
1. OnlyOffice 请求：`http://backend:8080/api/document/{id}/file-proxy?token=...`
2. Backend 验证 token
3. Backend 从 MinIO 获取文件：`http://law-firm-minio:9000/...`
4. Backend 返回文件内容给 OnlyOffice

**关键点：**
- ✅ OnlyOffice 通过 Docker 内部网络访问 backend
- ✅ Backend 通过 Docker 内部网络访问 MinIO
- ✅ **不依赖端口映射**

#### 3. OnlyOffice 保存流程

**关键代码：`saveFromOnlyOffice()`**
```java
public void saveFromOnlyOffice(final Long documentId, final String downloadUrl) {
    // 1. 规范化 OnlyOffice 提供的下载 URL
    String accessibleUrl = normalizeOnlyOfficeDownloadUrl(downloadUrl);
    // 将 localhost 替换为 onlyoffice（Docker 服务名）
    
    // 2. 从 OnlyOffice 下载文件
    java.net.URL url = new java.net.URI(accessibleUrl).toURL();
    InputStream inputStream = url.openStream();
    
    // 3. 保存到 MinIO
    minioService.uploadFile(inputStream, objectName, mimeType);
}
```

**流程：**
1. OnlyOffice 回调：`http://backend:8080/api/document/{id}/callback`
2. Backend 从 OnlyOffice 下载文件：`http://onlyoffice/...`（Docker 内部网络）
3. Backend 保存到 MinIO：`http://law-firm-minio:9000/...`（Docker 内部网络）

**关键点：**
- ✅ OnlyOffice 回调使用 Docker 内部地址
- ✅ Backend 访问 OnlyOffice 使用 Docker 内部地址
- ✅ Backend 访问 MinIO 使用 Docker 内部地址
- ✅ **不依赖端口映射**

---

## 🧪 集成测试

### 完整流程测试

#### 文档编辑流程（DOC 文件示例）

```
1. 用户点击"编辑"按钮
   ↓
2. Backend 生成 OnlyOffice 配置
   - 调用 onlyOfficeService.buildFileUrlForDocument(id)
   - 生成文件 URL: http://backend:8080/api/document/{id}/file-proxy?token=...
   ↓
3. 前端加载 OnlyOffice 编辑器
   - 使用配置中的 documentServerUrl: /onlyoffice
   - 使用配置中的 fileUrl: /api/document/{id}/file-proxy
   ↓
4. OnlyOffice 容器获取文档文件
   - OnlyOffice 请求: http://backend:8080/api/document/{id}/file-proxy
   - 通过 Docker 内部网络访问 backend
   ↓
5. Backend 从 MinIO 获取文件
   - 验证 token
   - 从 MinIO 获取文件: http://law-firm-minio:9000/...
   - 返回文件内容给 OnlyOffice
   ↓
6. OnlyOffice 显示文档，用户可以编辑
   ↓
7. 用户保存文档
   ↓
8. OnlyOffice 回调 Backend
   - 回调: http://backend:8080/api/document/{id}/callback
   ↓
9. Backend 保存文件到 MinIO
   - 从 OnlyOffice 下载编辑后的文件
   - 保存到 MinIO: http://law-firm-minio:9000/...
```

### 测试步骤

#### 测试 1：文档编辑 - 获取文件

**步骤：**
1. 登录系统
2. 进入卷宗管理
3. 上传一个 DOC 文件（或使用已有 DOC 文件）
4. 点击"编辑"按钮

**预期结果：**
- ✅ OnlyOffice 编辑器正常加载
- ✅ 文档内容正确显示
- ✅ 可以正常编辑

**检查日志：**
```bash
# Backend 日志
docker logs law-firm-backend | grep "OnlyOffice\|file-proxy" | tail -20

# 应该看到：
# - "生成 OnlyOffice 文档代理 URL（带token）"
# - "OnlyOffice 文件代理（使用新字段）" 或 "OnlyOffice 文件代理（使用file_path）"
```

#### 测试 2：文档编辑 - 保存文件

**步骤：**
1. 在 OnlyOffice 编辑器中修改文档
2. 点击"保存"按钮
3. 等待保存完成

**预期结果：**
- ✅ 保存成功提示
- ✅ 文档已更新
- ✅ 可以再次打开查看修改内容

**检查日志：**
```bash
# Backend 日志
docker logs law-firm-backend | grep "OnlyOffice.*回调\|saveFromOnlyOffice" | tail -20

# 应该看到：
# - "OnlyOffice 回调: documentId=..., status=2, url=..."
# - "OnlyOffice 下载 URL 规范化: ... -> ..."
# - "OnlyOffice 文档保存成功: id=..."
```

### 自动化测试脚本

```bash
# 运行集成测试脚本
./scripts/test/test-onlyoffice-minio-integration.sh
```

测试脚本会检查：
- ✅ 容器状态
- ✅ 容器间网络连通性
- ✅ HTTP 连接测试
- ✅ 配置检查
- ✅ Nginx 配置检查
- ✅ 端口映射检查

### 测试检查清单

- [ ] **文档上传**
  - [ ] 上传 DOC 文件成功
  - [ ] 文件保存到 MinIO

- [ ] **文档编辑**
  - [ ] 点击"编辑"按钮
  - [ ] OnlyOffice 编辑器正常加载
  - [ ] 文档内容正确显示
  - [ ] 可以正常编辑

- [ ] **文档保存**
  - [ ] 修改文档内容
  - [ ] 点击"保存"
  - [ ] 保存成功提示
  - [ ] 文档已更新

- [ ] **容器间通信**
  - [ ] OnlyOffice → Backend 通信正常
  - [ ] Backend → MinIO 通信正常

---

## ⚠️ 常见问题排查

### 问题1：OnlyOffice 无法获取文件

**症状：**
- OnlyOffice 编辑器显示"无法加载文档"
- 浏览器控制台显示错误

**检查：**
```bash
# 1. 检查 OnlyOffice 能否访问 backend
docker exec law-firm-onlyoffice curl -I http://backend:8080/api/actuator/health

# 2. 检查 backend 日志
docker logs law-firm-backend | grep "file-proxy\|OnlyOffice"

# 3. 检查 token 验证
docker logs law-firm-backend | grep "拒绝未授权访问\|token"
```

**解决方案：**
- ✅ 确认 `ONLYOFFICE_CALLBACK_URL=http://backend:8080/api`（Docker 内部地址）
- ✅ 确认 backend 的 `/file-proxy` 接口正常工作
- ✅ 检查 token 验证逻辑

### 问题2：Backend 无法从 MinIO 获取文件

**症状：**
- Backend 日志显示"无法从 MinIO 获取文件"
- file-proxy 接口返回 404

**检查：**
```bash
# 1. 检查 backend 能否访问 MinIO
docker exec law-firm-backend curl http://law-firm-minio:9000/minio/health/live

# 2. 检查 MinIO 配置
docker exec law-firm-backend env | grep MINIO

# 3. 检查文件是否存在
docker exec law-firm-minio mc ls local/law-firm/
```

**解决方案：**
- ✅ 确认 `MINIO_ENDPOINT=http://law-firm-minio:9000`（Docker 内部地址）
- ✅ 确认容器名正确（`law-firm-minio`）
- ✅ 确认文件路径正确

### 问题3：OnlyOffice 保存失败

**症状：**
- 保存后提示错误
- Backend 日志显示"从 OnlyOffice 保存文档失败"

**检查：**
```bash
# 1. 检查 OnlyOffice 回调
docker logs law-firm-backend | grep "OnlyOffice 回调"

# 2. 检查 URL 规范化
docker logs law-firm-backend | grep "OnlyOffice 下载 URL 规范化"

# 3. 检查 MinIO 上传
docker logs law-firm-backend | grep "文件上传\|uploadFile"
```

**解决方案：**
- ✅ 确认 `normalizeOnlyOfficeDownloadUrl()` 正确替换 localhost
- ✅ 确认 backend 能访问 OnlyOffice（Docker 内部网络）
- ✅ 确认 backend 能访问 MinIO（Docker 内部网络）

---

## 📚 相关文档

- [部署指南](./deployment.md)
- [配置说明](./configuration.md)
- [单端口架构](./single-port-architecture.md)
- [故障排查](./troubleshooting.md)

---

**最后更新**: 2026-01-31
