# OnlyOffice 和 MinIO 集成测试指南

> ⚠️ **关键测试**：文档编辑功能涉及 OnlyOffice、MinIO、Backend 三个服务的配合，必须完整测试！

## 🔍 完整流程分析

### 文档编辑流程（DOC 文件示例）

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 用户点击"编辑"按钮                                        │
└─────────────────────────────────────────────────────────────┘
         │
         │ HTTP: GET /api/document/{id}/edit
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Backend (DocumentController.getEditConfig)              │
│    - 调用 onlyOfficeService.buildFileUrlForDocument(id)    │
│    - 生成文件 URL:                                          │
│      http://backend:8080/api/document/{id}/file-proxy?token=... │
│    - 返回 OnlyOffice 配置给前端                            │
└─────────────────────────────────────────────────────────────┘
         │
         │ 返回配置给前端
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. 前端加载 OnlyOffice 编辑器                               │
│    - 使用配置中的 documentServerUrl: /onlyoffice          │
│    - 使用配置中的 fileUrl: /api/document/{id}/file-proxy   │
└─────────────────────────────────────────────────────────────┘
         │
         │ HTTP: GET /onlyoffice/... (通过 Nginx)
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. OnlyOffice 容器获取文档文件                              │
│    - OnlyOffice 请求: http://backend:8080/api/document/{id}/file-proxy │
│    - 通过 Docker 内部网络访问 backend                      │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker 内部网络: onlyoffice → backend
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Backend (DocumentController.fileProxy)                   │
│    - 验证 token                                             │
│    - 从 MinIO 获取文件: http://minio:9000/...              │
│    - 返回文件内容给 OnlyOffice                              │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker 内部网络: backend → minio
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. MinIO 返回文件内容                                        │
│    - 通过 Docker 内部网络返回给 backend                     │
└─────────────────────────────────────────────────────────────┘
         │
         │ 文件内容返回给 OnlyOffice
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. OnlyOffice 显示文档，用户可以编辑                        │
└─────────────────────────────────────────────────────────────┘
         │
         │ 用户编辑并保存
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. OnlyOffice 保存文档                                       │
│    - 回调: http://backend:8080/api/document/{id}/callback  │
│    - 通过 Docker 内部网络访问 backend                       │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker 内部网络: onlyoffice → backend
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 9. Backend (DocumentController.onlyOfficeCallback)          │
│    - 接收 OnlyOffice 回调                                   │
│    - 调用 documentAppService.saveFromOnlyOffice()          │
│    - 从 OnlyOffice 下载编辑后的文件                         │
│    - 保存到 MinIO: http://minio:9000/...                   │
└─────────────────────────────────────────────────────────────┘
         │
         │ Docker 内部网络: backend → minio
         ▼
┌─────────────────────────────────────────────────────────────┐
│ 10. MinIO 保存文件成功                                       │
│     - 文件已更新                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ 关键配置检查

### 1. OnlyOffice 配置（docker-compose.prod.yml）

```yaml
onlyoffice:
  environment:
    - ONLYOFFICE_CALLBACK_URL=http://backend:8080/api  # ✅ Docker 内部地址
    - FILE_SERVER_URL=http://law-firm-minio:9000        # ✅ Docker 内部地址
```

**检查点：**
- ✅ `ONLYOFFICE_CALLBACK_URL` 使用 Docker 内部地址 `backend:8080`
- ✅ `FILE_SERVER_URL` 使用 Docker 内部地址 `law-firm-minio:9000`（注意容器名）
- ✅ 不依赖端口映射

### 2. Backend 配置（application-prod.yml）

```yaml
onlyoffice:
  document-server-url: ${ONLYOFFICE_URL:/onlyoffice}  # ✅ 浏览器访问路径
  callback-url: ${ONLYOFFICE_CALLBACK_URL:http://backend:8080/api}  # ✅ Docker 内部地址
  file-server-url: ${FILE_SERVER_URL:http://minio:9000}  # ✅ Docker 内部地址
```

**检查点：**
- ✅ `callback-url` 使用 Docker 内部地址
- ✅ `file-server-url` 使用 Docker 内部地址
- ✅ `document-server-url` 使用路径或完整 URL（浏览器访问）

### 3. 关键代码逻辑

#### buildFileUrlForDocument() 方法

```java
// 生成文件 URL 供 OnlyOffice 使用
public String buildFileUrlForDocument(final Long documentId) {
    // 使用 Docker 内部地址（backend:8080）
    String baseUrl = this.config.getCallbackUrl();  // http://backend:8080/api
    // 生成代理 URL
    String proxyUrl = baseUrl + "/api/document/" + documentId + "/file-proxy?token=...";
    return proxyUrl;
}
```

**关键点：**
- ✅ 使用 `callback-url`（Docker 内部地址）构建 URL
- ✅ OnlyOffice 通过 Docker 内部网络访问 backend
- ✅ **不依赖端口映射**

#### fileProxy() 方法

```java
// OnlyOffice 通过此接口获取文件
@GetMapping("/{id}/file-proxy")
public void fileProxy(@PathVariable Long id, ...) {
    // 1. 验证 token
    // 2. 从 MinIO 获取文件
    String objectName = minioService.extractObjectName(...);
    // 3. 从 MinIO 下载文件
    minioService.downloadFile(objectName);
    // 4. 返回给 OnlyOffice
}
```

**关键点：**
- ✅ backend 通过 `http://minio:9000` 访问 MinIO（Docker 内部网络）
- ✅ **不依赖端口映射**

#### saveFromOnlyOffice() 方法

```java
// OnlyOffice 保存文档后调用
public void saveFromOnlyOffice(final Long documentId, final String downloadUrl) {
    // 1. 规范化 OnlyOffice 提供的下载 URL
    String accessibleUrl = normalizeOnlyOfficeDownloadUrl(downloadUrl);
    // 2. 从 OnlyOffice 下载文件
    // 3. 保存到 MinIO
    minioService.uploadFile(...);
}
```

**关键点：**
- ✅ `normalizeOnlyOfficeDownloadUrl()` 将 localhost 替换为 `onlyoffice`（Docker 服务名）
- ✅ backend 通过 Docker 内部网络访问 OnlyOffice
- ✅ backend 通过 Docker 内部网络访问 MinIO
- ✅ **不依赖端口映射**

---

## 🧪 完整测试步骤

### 测试前准备

```bash
# 1. 确认服务已启动
docker compose -f docker/docker-compose.prod.yml ps

# 2. 确认容器间网络连通性
docker exec law-firm-backend ping -c 1 minio
docker exec law-firm-backend ping -c 1 onlyoffice
docker exec onlyoffice ping -c 1 backend
docker exec onlyoffice ping -c 1 minio
```

### 测试 1：文档编辑 - 获取文件

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

**验证点：**
- ✅ OnlyOffice 能通过 `http://backend:8080/api/document/{id}/file-proxy` 获取文件
- ✅ Backend 能通过 `http://minio:9000` 从 MinIO 获取文件
- ✅ 文件内容正确返回给 OnlyOffice

### 测试 2：文档编辑 - 保存文件

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

**验证点：**
- ✅ OnlyOffice 能通过 `http://backend:8080/api/document/{id}/callback` 回调 backend
- ✅ Backend 能通过 Docker 内部网络从 OnlyOffice 下载文件
- ✅ Backend 能通过 `http://minio:9000` 保存文件到 MinIO
- ✅ 文件已更新到 MinIO

### 测试 3：容器间通信验证

**测试 OnlyOffice → Backend：**
```bash
# 进入 OnlyOffice 容器
docker exec -it onlyoffice sh

# 测试访问 backend
curl http://backend:8080/api/actuator/health

# 测试访问 file-proxy（需要有效的 token）
# curl http://backend:8080/api/document/{id}/file-proxy?token=...

# 退出容器
exit
```

**测试 Backend → MinIO：**
```bash
# 进入 Backend 容器
docker exec -it law-firm-backend sh

# 测试访问 MinIO
curl http://minio:9000/minio/health/live

# 退出容器
exit
```

**测试 OnlyOffice → MinIO（如果需要）：**
```bash
# 进入 OnlyOffice 容器
docker exec -it onlyoffice sh

# 测试访问 MinIO（如果 FILE_SERVER_URL 直接指向 MinIO）
curl http://law-firm-minio:9000/minio/health/live

# 退出容器
exit
```

**预期结果：**
- ✅ 所有容器间通信都成功
- ✅ 不依赖端口映射

### 测试 4：文件路径验证

**检查生成的文件 URL：**
```bash
# 查看后端日志，找到生成的文件 URL
docker logs law-firm-backend | grep "生成 OnlyOffice 文档代理 URL"

# 应该看到类似：
# http://backend:8080/api/document/123/file-proxy?token=...&expires=...
```

**验证点：**
- ✅ URL 使用 Docker 内部地址 `backend:8080`
- ✅ 不包含外部 IP 或端口映射

### 测试 5：预签名 URL 验证（如果使用）

**检查 MinIO 预签名 URL：**
```bash
# 查看后端日志
docker logs law-firm-backend | grep "OnlyOffice 文件 URL\|预签名 URL"

# 如果使用预签名 URL，应该看到：
# http://minio:9000/law-firm/...?X-Amz-Algorithm=...
```

**验证点：**
- ✅ 预签名 URL 使用 Docker 内部地址 `minio:9000`
- ✅ OnlyOffice 容器可以访问（需要 `ALLOW_PRIVATE_IP_ADDRESS=true`）

---

## ⚠️ 潜在问题和解决方案

### 问题 1：OnlyOffice 无法获取文件

**症状：**
- OnlyOffice 编辑器显示"无法加载文档"
- 浏览器控制台显示错误

**检查：**
```bash
# 1. 检查 OnlyOffice 能否访问 backend
docker exec onlyoffice curl -I http://backend:8080/api/actuator/health

# 2. 检查 backend 日志
docker logs law-firm-backend | grep "file-proxy\|OnlyOffice"

# 3. 检查 token 验证
docker logs law-firm-backend | grep "拒绝未授权访问\|token"
```

**解决方案：**
- ✅ 确认 `ONLYOFFICE_CALLBACK_URL=http://backend:8080/api`（Docker 内部地址）
- ✅ 确认 backend 的 `/file-proxy` 接口正常工作
- ✅ 检查 token 验证逻辑

### 问题 2：Backend 无法从 MinIO 获取文件

**症状：**
- Backend 日志显示"无法从 MinIO 获取文件"
- file-proxy 接口返回 404

**检查：**
```bash
# 1. 检查 backend 能否访问 MinIO
docker exec law-firm-backend curl http://minio:9000/minio/health/live

# 2. 检查 MinIO 配置
docker exec law-firm-backend env | grep MINIO

# 3. 检查文件是否存在
docker exec law-firm-minio mc ls local/law-firm/
```

**解决方案：**
- ✅ 确认 `MINIO_ENDPOINT=http://minio:9000`（Docker 内部地址）
- ✅ 确认容器名正确（`minio` 或 `law-firm-minio`）
- ✅ 确认文件路径正确

### 问题 3：OnlyOffice 保存失败

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

### 问题 4：预签名 URL 验证失败

**症状：**
- MinIO 返回 403 Forbidden
- 日志显示签名验证失败

**检查：**
```bash
# 检查预签名 URL 格式
docker logs law-firm-backend | grep "预签名 URL\|presigned"

# 检查 MinIO Host 配置
docker exec law-firm-backend env | grep MINIO
```

**解决方案：**
- ✅ 确认使用 Docker 内部地址生成预签名 URL
- ✅ 确认 OnlyOffice 配置了 `ALLOW_PRIVATE_IP_ADDRESS=true`
- ✅ 如果使用 `/file-proxy` 接口，不需要预签名 URL

---

## 📋 测试检查清单

### 基础功能测试

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

- [ ] **文档查看**
  - [ ] 保存后可以再次打开
  - [ ] 修改内容正确显示

### 容器间通信测试

- [ ] **OnlyOffice → Backend**
  - [ ] OnlyOffice 能访问 backend 的 `/file-proxy` 接口
  - [ ] OnlyOffice 能回调 backend 的 `/callback` 接口

- [ ] **Backend → MinIO**
  - [ ] Backend 能从 MinIO 获取文件
  - [ ] Backend 能保存文件到 MinIO

- [ ] **OnlyOffice → MinIO（如果使用）**
  - [ ] OnlyOffice 能直接访问 MinIO（如果配置了 FILE_SERVER_URL）

### 配置验证

- [ ] **环境变量**
  - [ ] `ONLYOFFICE_CALLBACK_URL=http://backend:8080/api` ✅
  - [ ] `FILE_SERVER_URL=http://law-firm-minio:9000` ✅
  - [ ] `ONLYOFFICE_URL=/onlyoffice` ✅

- [ ] **Nginx 配置**
  - [ ] `/onlyoffice/` location 配置正确 ✅
  - [ ] WebSocket 支持配置 ✅
  - [ ] Authorization header 传递 ✅

- [ ] **Docker Compose**
  - [ ] OnlyOffice 端口映射已移除 ✅
  - [ ] MinIO 端口映射已移除 ✅
  - [ ] 所有服务在同一网络 ✅

---

## 🎯 关键验证点

### ✅ 配置正确性（已验证）

1. **OnlyOffice 获取文件**
   - ✅ 使用 `buildFileUrlForDocument()` 生成 URL
   - ✅ URL 格式：`http://backend:8080/api/document/{id}/file-proxy?token=...`
   - ✅ OnlyOffice 通过 Docker 内部网络访问 backend
   - ✅ **不依赖端口映射**

2. **Backend 从 MinIO 获取文件**
   - ✅ 通过 `http://minio:9000` 访问 MinIO（Docker 内部网络）
   - ✅ **不依赖端口映射**

3. **OnlyOffice 保存文件**
   - ✅ 回调 `http://backend:8080/api/document/{id}/callback`（Docker 内部网络）
   - ✅ Backend 从 OnlyOffice 下载文件（Docker 内部网络）
   - ✅ Backend 保存到 MinIO（Docker 内部网络）
   - ✅ **不依赖端口映射**

### ⚠️ 需要测试验证

1. **完整流程测试**
   - ⚠️ 上传 DOC 文件
   - ⚠️ 点击编辑
   - ⚠️ 修改内容
   - ⚠️ 保存文件
   - ⚠️ 验证文件已更新

2. **错误场景测试**
   - ⚠️ 网络中断情况
   - ⚠️ 大文件处理
   - ⚠️ 并发编辑

---

## 📝 测试脚本

### 快速测试脚本

```bash
#!/bin/bash
# OnlyOffice 和 MinIO 集成测试脚本

echo "=== OnlyOffice 和 MinIO 集成测试 ==="
echo ""

# 1. 检查容器状态
echo "1. 检查容器状态..."
docker compose -f docker/docker-compose.prod.yml ps | grep -E "onlyoffice|minio|backend"

# 2. 测试容器间网络
echo ""
echo "2. 测试容器间网络..."
echo "   OnlyOffice → Backend:"
docker exec onlyoffice curl -s -o /dev/null -w "%{http_code}" http://backend:8080/api/actuator/health || echo "失败"

echo "   Backend → MinIO:"
docker exec law-firm-backend curl -s -o /dev/null -w "%{http_code}" http://minio:9000/minio/health/live || echo "失败"

# 3. 检查配置
echo ""
echo "3. 检查配置..."
echo "   ONLYOFFICE_CALLBACK_URL:"
docker exec law-firm-backend env | grep ONLYOFFICE_CALLBACK_URL

echo "   FILE_SERVER_URL:"
docker exec law-firm-backend env | grep FILE_SERVER_URL

echo "   MINIO_ENDPOINT:"
docker exec law-firm-backend env | grep MINIO_ENDPOINT

# 4. 检查日志
echo ""
echo "4. 最近的 OnlyOffice 相关日志:"
docker logs law-firm-backend --tail 50 | grep -i "onlyoffice\|file-proxy" | tail -10

echo ""
echo "=== 测试完成 ==="
echo ""
echo "下一步："
echo "1. 登录系统"
echo "2. 上传一个 DOC 文件"
echo "3. 点击'编辑'按钮"
echo "4. 修改内容并保存"
echo "5. 验证文件已更新"
```

---

## ✅ 结论

### 配置分析结果

**✅ 所有关键配置都正确：**

1. **OnlyOffice 获取文件**
   - ✅ 使用 `/file-proxy` 接口（不直接访问 MinIO）
   - ✅ 通过 Docker 内部网络访问 backend
   - ✅ **不依赖端口映射**

2. **Backend 访问 MinIO**
   - ✅ 使用 Docker 内部地址 `http://minio:9000`
   - ✅ **不依赖端口映射**

3. **OnlyOffice 保存文件**
   - ✅ 回调使用 Docker 内部地址
   - ✅ Backend 访问 OnlyOffice 和 MinIO 都使用 Docker 内部网络
   - ✅ **不依赖端口映射**

### 测试建议

**必须测试的场景：**
1. ✅ 上传 DOC 文件
2. ✅ 点击编辑，OnlyOffice 正常加载
3. ✅ 修改文档内容
4. ✅ 保存文档
5. ✅ 验证文件已更新

**测试重点：**
- OnlyOffice 能否获取文件（通过 `/file-proxy`）
- Backend 能否从 MinIO 获取文件
- OnlyOffice 保存后，Backend 能否正确保存到 MinIO

---

**配置已检查完毕，理论上应该正常工作。但必须进行实际测试验证！**
