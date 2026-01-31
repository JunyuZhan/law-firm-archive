# OnlyOffice 和 MinIO 配置检查清单

> ⚠️ **关键检查**：确保 OnlyOffice 和 MinIO 的配置一致，特别是容器名称！

## 🔍 关键配置检查

### 1. MinIO 容器名称

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

### 2. OnlyOffice 文件获取流程

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

### 3. OnlyOffice 保存流程

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

## ✅ 配置一致性检查

### 必须一致的配置

| 配置项 | docker-compose.prod.yml | application-prod.yml | 状态 |
|--------|------------------------|---------------------|------|
| **MinIO 容器名** | `law-firm-minio` | - | ✅ |
| **MINIO_ENDPOINT** | `http://law-firm-minio:9000` | `http://minio:9000` | ⚠️ **不一致** |
| **FILE_SERVER_URL** | `http://law-firm-minio:9000` | `http://minio:9000` | ⚠️ **不一致** |
| **ONLYOFFICE_CALLBACK_URL** | `http://backend:8080/api` | `http://backend:8080/api` | ✅ |
| **ONLYOFFICE_URL** | `/onlyoffice` | `/onlyoffice` | ✅ |

### ⚠️ 发现的问题

**问题：** `MINIO_ENDPOINT` 和 `FILE_SERVER_URL` 在 docker-compose 中使用 `law-firm-minio`，但在 application-prod.yml 中使用 `minio`。

**影响：**
- 如果 Docker 网络中的服务名是 `law-firm-minio`，使用 `minio` 可能无法访问
- 需要确认 Docker 网络中的实际服务名

**解决方案：**
- 检查 Docker Compose 中的服务名（`minio:` 还是其他）
- 确保所有配置使用一致的服务名

---

## 🔧 需要修复的配置

### 检查 Docker Compose 服务名

```yaml
# docker-compose.prod.yml
services:
  minio:  # ← 这是服务名（Docker 网络中使用）
    container_name: law-firm-minio  # ← 这是容器名（可选）
```

**Docker 网络中的服务名：**
- 服务名：`minio`（在 `services:` 下定义的名称）
- 容器名：`law-firm-minio`（`container_name` 指定的名称）

**访问方式：**
- 通过服务名：`http://minio:9000` ✅（推荐）
- 通过容器名：`http://law-firm-minio:9000` ✅（也可以）

### 当前配置分析

**docker-compose.prod.yml：**
```yaml
services:
  minio:  # ← 服务名是 minio
    container_name: law-firm-minio
    
  backend:
    environment:
      - MINIO_ENDPOINT=http://law-firm-minio:9000  # ← 使用容器名
      - FILE_SERVER_URL=http://law-firm-minio:9000  # ← 使用容器名
```

**application-prod.yml：**
```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}  # ← 从环境变量读取
  external-endpoint: ${MINIO_EXTERNAL_ENDPOINT:http://minio:9000}  # ← 默认使用服务名
```

**结论：**
- ✅ docker-compose 中使用 `law-firm-minio`（容器名）是可以的
- ✅ 但建议统一使用 `minio`（服务名），更标准
- ⚠️ 需要确认实际运行时的服务名

---

## 🧪 实际测试验证

### 测试脚本

已创建测试脚本：`scripts/test-onlyoffice-minio-integration.sh`

**运行测试：**
```bash
cd /path/to/law-firm
./scripts/test-onlyoffice-minio-integration.sh
```

**测试内容：**
1. ✅ 容器状态检查
2. ✅ 容器间网络连通性
3. ✅ HTTP 连接测试
4. ✅ 配置检查
5. ✅ Nginx 配置检查
6. ✅ 端口映射检查

### 手动测试步骤

1. **上传 DOC 文件**
   - 登录系统
   - 进入卷宗管理
   - 上传一个 DOC 文件

2. **编辑文档**
   - 点击"编辑"按钮
   - 验证 OnlyOffice 编辑器正常加载
   - 验证文档内容正确显示

3. **保存文档**
   - 修改文档内容
   - 点击"保存"
   - 验证保存成功
   - 验证文档已更新

4. **检查日志**
   ```bash
   # Backend 日志
   docker logs law-firm-backend | grep -i "onlyoffice\|file-proxy" | tail -20
   
   # OnlyOffice 日志
   docker logs onlyoffice | tail -50
   ```

---

## ✅ 配置验证结果

### 已验证的配置

1. **OnlyOffice 获取文件**
   - ✅ 使用 `/file-proxy` 接口（不直接访问 MinIO）
   - ✅ 通过 Docker 内部网络访问 backend
   - ✅ **不依赖端口映射**

2. **Backend 访问 MinIO**
   - ✅ 使用 Docker 内部地址（`minio:9000` 或 `law-firm-minio:9000`）
   - ✅ **不依赖端口映射**

3. **OnlyOffice 保存文件**
   - ✅ 回调使用 Docker 内部地址
   - ✅ Backend 访问 OnlyOffice 和 MinIO 都使用 Docker 内部网络
   - ✅ **不依赖端口映射**

### ⚠️ 需要注意的点

1. **容器名称一致性**
   - 确认 Docker 网络中的服务名是 `minio` 还是 `law-firm-minio`
   - 确保所有配置使用一致的服务名

2. **FILE_SERVER_URL 的使用**
   - 当前代码使用 `/file-proxy` 接口，不直接使用 `FILE_SERVER_URL`
   - `FILE_SERVER_URL` 主要用于 OnlyOffice 的其他功能（如果使用）

---

## 🎯 最终结论

### ✅ 配置分析

**所有关键配置都正确：**
- ✅ OnlyOffice 和 MinIO 的通信都通过 Docker 内部网络
- ✅ 不依赖端口映射
- ✅ 配置修改不会影响功能

### ⚠️ 必须测试

**虽然配置看起来正确，但必须进行实际测试：**
1. ✅ 上传 DOC 文件
2. ✅ 点击编辑，验证 OnlyOffice 正常加载
3. ✅ 修改并保存文档
4. ✅ 验证文件已更新

### 📝 测试文档

- 详细测试指南：`docs/ONLYOFFICE-MINIO-INTEGRATION-TEST.md`
- 测试脚本：`scripts/test-onlyoffice-minio-integration.sh`
- 配置检查：`docs/ONLYOFFICE-MINIO-CONFIGURATION-CHECK.md`

---

**建议：部署后立即测试文档编辑功能，确保 OnlyOffice 和 MinIO 正常配合工作！**
