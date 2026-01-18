# OnlyOffice 文档预览/编辑数据流向说明

## 问题描述

用户点击预览/编辑 Word 文档时，出现 CORS 错误：
```
Access to XMLHttpRequest at 'http://127.0.0.1:8088/cache/files/.../Editor.bin' 
from origin 'http://localhost:5666' has been blocked by CORS policy
```

## 完整数据流向

### 1. 前端发起请求

**位置**: `frontend/apps/web-antd/src/views/document/list/index.vue`

用户点击"预览"或"编辑"按钮：
```typescript
// 预览文档
async function handlePreview(record: DocumentDTO) {
  // 跳转到预览页面
  router.push({
    path: '/office-preview',
    query: { documentId: record.id, mode: 'view' }
  });
}
```

### 2. 预览页面加载配置

**位置**: `frontend/apps/web-antd/src/views/office-preview/index.vue`

```typescript
// 步骤 1: 调用后端 API 获取 OnlyOffice 配置
const response = await getDocumentPreviewConfig(documentId);

// 步骤 2: 后端返回配置
// {
//   documentServerUrl: "/onlyoffice",  // 相对路径（已修复）
//   document: {
//     url: "http://backend:8080/api/document/1/file-proxy?token=...",
//     fileType: "docx",
//     key: "doc_1_v1",
//     title: "文档名称.docx"
//   },
//   editorConfig: { ... },
//   token: "jwt_token..."
// }
```

### 3. 后端生成配置

**位置**: `backend/src/main/java/com/lawfirm/interfaces/rest/document/DocumentController.java`

```java
@GetMapping("/{id}/preview")
public Result<Map<String, Object>> getPreviewConfig(@PathVariable Long id) {
    // 1. 获取文档信息
    DocumentDTO doc = documentAppService.getDocumentById(id);
    
    // 2. 生成文件访问 URL（供 OnlyOffice 容器使用）
    String fileUrl = onlyOfficeService.buildFileUrlForDocument(id);
    // 结果: "http://backend:8080/api/document/1/file-proxy?token=..."
    
    // 3. 生成 OnlyOffice 配置
    Map<String, Object> config = onlyOfficeService.generateViewConfig(
        fileUrl, doc.getFileName(), userId, userName
    );
    
    // 4. 返回配置（documentServerUrl 已转换为相对路径 "/onlyoffice"）
    return Result.success(config);
}
```

**位置**: `backend/src/main/java/com/lawfirm/infrastructure/external/onlyoffice/OnlyOfficeService.java`

```java
// 关键修复：检测到 localhost/127.0.0.1 时，返回相对路径
String documentServerUrl = this.config.getDocumentServerUrl();
if (documentServerUrl != null && 
    (documentServerUrl.contains("localhost") || documentServerUrl.contains("127.0.0.1"))) {
    documentServerUrl = "/onlyoffice";  // 返回相对路径
}
config.put("documentServerUrl", documentServerUrl);
```

### 4. 前端加载 OnlyOffice API

**位置**: `frontend/apps/web-antd/src/views/office-preview/index.vue`

```typescript
// 步骤 1: 智能检测 OnlyOffice URL
const onlyOfficeUrl = getOnlyOfficeUrl(response.documentServerUrl);
// 结果: "http://localhost:5666/onlyoffice"（相对路径转换为绝对路径）

// 步骤 2: 加载 OnlyOffice API
const apiJsUrl = `${onlyOfficeUrl}/web-apps/apps/api/documents/api.js`;
await loadOnlyOfficeApi(apiJsUrl);
// 实际请求: GET http://localhost:5666/onlyoffice/web-apps/apps/api/documents/api.js
// Vite 代理转发到: http://127.0.0.1:8088/web-apps/apps/api/documents/api.js
```

### 5. 初始化 OnlyOffice 编辑器

**位置**: `frontend/apps/web-antd/src/views/office-preview/index.vue`

```typescript
const editorConfig = {
  document: {
    url: "http://backend:8080/api/document/1/file-proxy?token=...",
    fileType: "docx",
    key: "doc_1_v1",
    title: "文档名称.docx"
  },
  // 关键修复：设置 serverUrl，告诉 OnlyOffice 使用代理路径
  serverUrl: "http://localhost:5666/onlyoffice",
  editorConfig: { ... },
  // ...
};

// 初始化编辑器
new DocsAPI.DocEditor('onlyoffice-editor', editorConfig);
```

### 6. OnlyOffice 加载文档文件

**文档文件访问流程**：

1. **OnlyOffice 请求文档文件**
   ```javascript
   // OnlyOffice 配置中的 document.url
   document: {
     url: "http://backend:8080/api/document/1/file-proxy?token=...&expires=..."
   }
   ```

2. **OnlyOffice 容器访问后端 API**
   - OnlyOffice 容器通过 Docker 网络访问后端
   - 请求：`GET http://backend:8080/api/document/1/file-proxy?token=...`
   - 后端验证 token 和权限

3. **后端从 MinIO 下载文件**
   ```java
   // DocumentController.fileProxy()
   String objectName = minioService.extractObjectName(doc.getFilePath());
   InputStream inputStream = minioService.downloadFile(objectName);
   // 返回文件流给 OnlyOffice
   ```

4. **MinIO 访问检查**
   - ✅ 后端必须能访问 MinIO（`http://minio:9000`）
   - ✅ MinIO bucket 必须有读取权限
   - ✅ MinIO 访问密钥配置正确

**如果文档文件下载失败**：
- 检查后端日志：`docker logs law-firm-backend | grep "file-proxy"`
- 检查 MinIO 连接：`docker exec law-firm-backend curl http://minio:9000/minio/health/live`
- 检查 MinIO 权限：确保 bucket `law-firm` 有读取权限

### 7. OnlyOffice 加载内部资源（Editor.bin）

**问题点**: OnlyOffice 内部生成的资源 URL（如 `Editor.bin`）仍然使用 `http://127.0.0.1:8088`

**原因**: OnlyOffice 可能从以下来源获取服务器地址：
1. API 加载 URL（已通过代理，应该能推断出 `/onlyoffice`）
2. 配置中的 `serverUrl`（已设置）
3. 环境变量或默认配置（可能指向 `127.0.0.1:8088`）

**当前修复**:
- ✅ 后端返回相对路径 `/onlyoffice`
- ✅ 前端转换为 `http://localhost:5666/onlyoffice`
- ✅ 设置 `serverUrl` 配置项
- ✅ Nginx 添加 CORS 头

### 7. 代理转发（开发环境）

**位置**: `frontend/apps/web-antd/vite.config.mts`

```typescript
proxy: {
  '/onlyoffice': {
    target: 'http://127.0.0.1:8088',
    rewrite: (path) => path.replace(/^\/onlyoffice/, ''),
    changeOrigin: true,
  }
}
```

**流程**:
1. 浏览器请求: `GET http://localhost:5666/onlyoffice/web-apps/apps/api/documents/api.js`
2. Vite 代理转发: `GET http://127.0.0.1:8088/web-apps/apps/api/documents/api.js`
3. OnlyOffice 返回 API 脚本

### 8. 代理转发（生产环境）

**位置**: `frontend/scripts/deploy/nginx.conf`

```nginx
location /onlyoffice/ {
  proxy_pass http://onlyoffice:80/;
  # CORS 头（已添加）
  add_header Access-Control-Allow-Origin * always;
  # ...
}
```

## 问题根源

### 问题 1: OnlyOffice 内部资源 CORS 错误

OnlyOffice 内部资源（如 `Editor.bin`）的 URL 生成逻辑可能：
1. 从 API 加载 URL 推断（应该能正确推断）
2. 从配置的 `serverUrl` 获取（已设置）
3. 使用默认值或环境变量（可能指向 `127.0.0.1:8088`）

### 问题 2: MinIO 文档文件访问（重要！）

**文档文件访问流程**：
1. OnlyOffice 需要访问文档文件（Word文档）
2. 文档存储在 MinIO 中
3. OnlyOffice 通过后端代理接口获取文件：`http://backend:8080/api/document/{id}/file-proxy`
4. 后端从 MinIO 下载文件并返回给 OnlyOffice

**MinIO 相关的问题**：

1. **后端无法访问 MinIO**
   - 如果后端无法连接到 MinIO（网络问题、配置错误）
   - OnlyOffice 请求文档文件时会失败
   - 错误信息：`文档加载失败: 下载失败`

2. **MinIO CORS 配置**（如果 OnlyOffice 直接访问 MinIO）
   - 当前实现使用后端代理，MinIO CORS 不影响 OnlyOffice
   - 但如果使用预签名 URL 直接访问 MinIO，需要配置 CORS
   - MinIO 默认可能不允许跨域访问

3. **MinIO 访问权限**
   - 检查 MinIO bucket 权限设置
   - 确保后端有读取权限
   - 检查 MinIO 访问密钥配置

**检查 MinIO 配置**：
```bash
# 检查 MinIO 容器状态
docker ps | grep minio

# 检查 MinIO 日志
docker logs law-firm-minio

# 检查后端能否访问 MinIO
docker exec law-firm-backend curl -I http://minio:9000/minio/health/live
```

## 解决方案

### 已实施的修复

1. **后端返回相对路径**
   - 检测到 `localhost:8088` 时，返回 `/onlyoffice`
   - 前端自动转换为当前域名的绝对路径

2. **前端设置 serverUrl**
   - 在编辑器配置中添加 `serverUrl: "http://localhost:5666/onlyoffice"`
   - 告诉 OnlyOffice 使用代理路径生成内部资源 URL

3. **Nginx 添加 CORS 头**
   - 为 `/onlyoffice/` 代理添加 CORS 响应头
   - 允许跨域访问

### 如果问题仍然存在

#### 1. 检查文档文件下载问题（MinIO 相关）

```bash
# 检查后端能否访问 MinIO
docker exec law-firm-backend curl -I http://minio:9000/minio/health/live

# 检查 MinIO 容器状态
docker ps | grep minio

# 检查 MinIO 日志
docker logs law-firm-minio --tail 50

# 检查后端日志中的文件代理请求
docker logs law-firm-backend | grep "file-proxy"

# 测试 MinIO 访问（需要访问密钥）
docker exec law-firm-backend curl -X GET \
  "http://minio:9000/law-firm/test-file" \
  -H "Authorization: AWS minioadmin:minioadmin"
```

#### 2. 检查 OnlyOffice 内部资源问题

```bash
# 检查 OnlyOffice 容器配置
docker exec law-firm-onlyoffice env | grep -i onlyoffice

# 检查 OnlyOffice 日志
docker logs law-firm-onlyoffice --tail 50

# 检查 OnlyOffice 配置文件
docker exec law-firm-onlyoffice cat /etc/onlyoffice/documentserver/default.json | grep -i url
```

#### 3. 检查网络连通性

```bash
# 检查 OnlyOffice 能否访问后端
docker exec law-firm-onlyoffice curl -I http://backend:8080/api/actuator/health

# 检查后端能否访问 OnlyOffice
docker exec law-firm-backend curl -I http://onlyoffice:80/web-apps/apps/api/documents/api.js
```

## 测试步骤

1. 清除浏览器缓存
2. 重启前端服务（开发环境）
3. 重启 Nginx（生产环境）
4. 打开浏览器开发者工具，查看网络请求
5. 检查 `Editor.bin` 等资源的请求 URL 是否正确使用代理路径

## 预期结果

修复后，所有 OnlyOffice 资源请求应该：
- ✅ 使用代理路径: `http://localhost:5666/onlyoffice/cache/files/.../Editor.bin`
- ❌ 不再使用直接路径: `http://127.0.0.1:8088/cache/files/.../Editor.bin`
