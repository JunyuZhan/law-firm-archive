# OnlyOffice CORS 问题修复总结

## 问题描述

用户点击预览/编辑 Word 文档时，出现 CORS 错误：
```
Access to XMLHttpRequest at 'http://127.0.0.1:8088/cache/files/.../Editor.bin' 
from origin 'http://localhost:5666' has been blocked by CORS policy
```

## 根本原因

1. **网络隔离问题**：
   - OnlyOffice 容器在 `law-firm-dev` 网络
   - MinIO/Postgres/Redis 在 `law-firm-dev-full` 网络
   - 两个网络隔离，导致容器无法通信

2. **CORS 配置问题**：
   - OnlyOffice 内部资源（Editor.bin）使用 `http://127.0.0.1:8088` 直接访问
   - 浏览器从 `http://localhost:5666` 访问，触发 CORS 策略
   - OnlyOffice 容器没有设置 CORS 响应头

3. **URL 路径问题**：
   - 后端返回的 OnlyOffice URL 是 `http://localhost:8088`（直接访问）
   - 应该通过代理路径 `/onlyoffice` 访问

## 已完成的修复

### 1. ✅ 后端 URL 配置修复
- **文件**: `backend/src/main/java/com/lawfirm/infrastructure/external/onlyoffice/OnlyOfficeService.java`
- **修改**: 检测到 `localhost:8088` 时，返回相对路径 `/onlyoffice`
- **效果**: 前端会自动转换为 `http://localhost:5666/onlyoffice`

### 2. ✅ 前端路径处理修复
- **文件**: `frontend/apps/web-antd/src/views/office-preview/index.vue`
- **修改**: 
  - 增强 `getOnlyOfficeUrl()` 函数，正确处理相对路径
  - 设置 `serverUrl` 配置项，告诉 OnlyOffice 使用代理路径
- **效果**: OnlyOffice 内部资源会使用代理路径生成 URL

### 3. ✅ Nginx 代理 CORS 配置
- **文件**: `frontend/scripts/deploy/nginx.conf`
- **修改**: 为 `/onlyoffice/` 代理添加 CORS 响应头
- **效果**: 通过代理访问的资源都有 CORS 头

### 4. ✅ Vite 代理配置（开发环境）
- **文件**: `frontend/apps/web-antd/vite.config.mts`
- **状态**: 已存在，无需修改（vite 代理是服务器端，不存在 CORS 问题）

### 5. ✅ 网络连接修复
- **操作**: 将 OnlyOffice 容器连接到 `law-firm-dev-full_law-firm-network`
- **命令**: `docker network connect law-firm-dev-full_law-firm-network law-firm-onlyoffice`
- **效果**: OnlyOffice 现在可以访问 MinIO、Postgres、Redis 等服务

### 6. ✅ OnlyOffice 配置简化
- **文件**: `docker/onlyoffice/entrypoint.sh`
- **修改**: 移除了复杂的 nginx CORS 配置逻辑（因为由前端代理处理）
- **效果**: 避免 nginx 配置错误

## 当前状态

### ✅ 已修复
1. 后端返回正确的相对路径
2. 前端正确处理路径转换
3. Nginx 代理添加了 CORS 头
4. OnlyOffice 已连接到正确的网络

### ⚠️ 待验证
1. OnlyOffice 容器需要完全启动（健康检查通过）
2. 测试文档预览/编辑功能是否正常
3. 确认 Editor.bin 等资源通过代理路径访问

## 测试步骤

1. **检查容器状态**：
   ```bash
   docker ps | grep -E "onlyoffice|minio"
   ```

2. **检查网络连接**：
   ```bash
   docker network inspect law-firm-dev-full_law-firm-network | grep onlyoffice
   ```

3. **测试代理访问**：
   ```bash
   curl -I http://localhost:5666/onlyoffice/web-apps/apps/api/documents/api.js
   ```
   应该返回 `Access-Control-Allow-Origin` 头

4. **测试文档预览**：
   - 在浏览器中打开文档列表
   - 点击预览/编辑 Word 文档
   - 检查浏览器控制台是否有 CORS 错误

## 如果问题仍然存在

1. **检查 OnlyOffice 是否完全启动**：
   ```bash
   docker logs law-firm-onlyoffice --tail 50
   ```

2. **检查网络连通性**：
   ```bash
   docker exec law-firm-onlyoffice curl http://minio:9000/minio/health/live
   ```

3. **检查浏览器网络请求**：
   - 打开浏览器开发者工具
   - 查看 Network 标签
   - 检查 `Editor.bin` 等资源的请求 URL
   - 确认是否使用代理路径（`/onlyoffice`）而不是直接路径（`127.0.0.1:8088`）

## 关键点

1. **所有 OnlyOffice 请求都应通过代理**：
   - ✅ 开发环境：`http://localhost:5666/onlyoffice/...`
   - ✅ 生产环境：`http://your-domain.com/onlyoffice/...`
   - ❌ 不应该：`http://127.0.0.1:8088/...`

2. **CORS 由前端代理处理**：
   - Vite 代理（开发环境）：服务器端代理，无 CORS 问题
   - Nginx 代理（生产环境）：已添加 CORS 响应头

3. **网络必须连通**：
   - OnlyOffice 需要访问 MinIO 获取文档文件
   - OnlyOffice 需要访问后端 API 进行回调
