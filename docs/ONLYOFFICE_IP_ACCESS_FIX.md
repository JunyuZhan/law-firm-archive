# OnlyOffice IP 访问修复指南

## 问题描述

在生产环境中使用 IP 地址访问时，OnlyOffice 文档预览和编辑失败，错误信息：
```
文档加载失败: 打开文件时出错
文件内容与文件扩展名不匹配。
```

控制台显示：
```
OnlyOffice documentUrl 已转换: {
  original: 'http://backend:8080/api/document/1/file-proxy?...',
  converted: 'http://192.168.50.10/api/document/1/file-proxy?...'
}
```

## 问题原因

1. **OnlyOffice 容器无法访问宿主机 IP**：
   - OnlyOffice 在 Docker 容器中运行
   - 后端生成的文件 URL 是 `http://backend:8080/api/document/1/file-proxy?...`
   - 前端错误地将此 URL 转换为 `http://192.168.50.10/api/document/1/file-proxy?...`
   - OnlyOffice 容器无法访问 `192.168.50.10`（这是宿主机的内网 IP）

2. **网络架构问题**：
   ```
   浏览器（用户机器）
       ↓ HTTP 请求
       ↓ 访问 192.168.50.10
   Nginx（宿主机）
       ↓ 代理到 Docker 网络
   OnlyOffice 容器
       ↓ 尝试访问 192.168.50.10 ❌ 失败
       ↓ 无法访问宿主机 IP
   ```

## 解决方案

### 方案一：使用外部访问地址（推荐）

配置 `ONLYOFFICE_EXTERNAL_ACCESS_URL` 环境变量，让后端生成外部可访问的 URL，OnlyOffice 通过 Nginx 代理访问文件。

**步骤**：

1. **配置环境变量**：
   ```bash
   # 在 docker/.env 文件中添加
   ONLYOFFICE_EXTERNAL_ACCESS_URL=http://192.168.50.10
   # 或使用域名
   ONLYOFFICE_EXTERNAL_ACCESS_URL=http://oa.albertzhan.top
   ```

2. **重启后端服务**：
   ```bash
   cd docker
   docker compose -f docker-compose.prod.yml restart backend
   ```

3. **验证配置**：
   - 查看后端日志，确认使用了外部访问地址：
     ```bash
     docker logs law-firm-backend | grep "OnlyOffice"
     ```
   - 应该看到类似日志：
     ```
     使用外部访问地址生成文件 URL: externalAccessUrl=http://192.168.50.10
     ```

### 方案二：保持 Docker 内部地址（不推荐）

如果 OnlyOffice 容器可以直接访问 Docker 内部网络，可以保持使用 `backend:8080`。

**注意**：前端代码已修改，不再转换 `backend:8080` 这样的 Docker 内部地址。

## 配置说明

### 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `ONLYOFFICE_URL` | OnlyOffice Document Server 地址（浏览器访问） | `http://192.168.50.10/onlyoffice` |
| `ONLYOFFICE_EXTERNAL_ACCESS_URL` | 外部访问地址（OnlyOffice 容器访问文件） | `http://192.168.50.10` |
| `ONLYOFFICE_CALLBACK_URL` | 回调地址（OnlyOffice 保存文档时调用） | `http://backend:8080/api` |

### 网络架构

```
用户浏览器
    ↓ HTTP
    ↓ http://192.168.50.10
Nginx（宿主机 :80）
    ↓ 代理
    ├─ /api/ → backend:8080/api/
    └─ /onlyoffice/ → onlyoffice:80/
    
OnlyOffice 容器
    ↓ HTTP 请求文件
    ↓ http://192.168.50.10/api/document/1/file-proxy
    ↓ 通过 Docker 网络
Nginx（宿主机）
    ↓ 代理到
backend:8080
    ↓ 从 MinIO 获取文件
minio:9000
```

## 验证步骤

1. **检查后端配置**：
   ```bash
   docker exec law-firm-backend env | grep ONLYOFFICE
   ```

2. **测试文件代理接口**：
   ```bash
   # 从宿主机测试
   curl http://192.168.50.10/api/document/1/file-proxy?token=xxx&expires=xxx
   ```

3. **检查 OnlyOffice 日志**：
   ```bash
   docker logs law-firm-onlyoffice --tail 50
   ```

4. **检查后端日志**：
   ```bash
   docker logs law-firm-backend --tail 50 | grep "file-proxy"
   ```

## 常见问题

### Q1: OnlyOffice 仍然无法加载文档

**检查**：
1. 确认 `ONLYOFFICE_EXTERNAL_ACCESS_URL` 配置正确
2. 确认 Nginx 代理配置正确（`/api/` → `backend:8080/api/`）
3. 检查后端日志，确认文件 URL 生成正确

### Q2: 文件代理接口返回 404

**检查**：
1. 确认后端服务正常运行：`docker ps | grep backend`
2. 确认 Nginx 配置正确：`docker exec law-firm-frontend nginx -t`
3. 检查后端日志：`docker logs law-firm-backend | grep "file-proxy"`

### Q3: Token 验证失败

**检查**：
1. 确认 `ONLYOFFICE_JWT_SECRET` 配置正确
2. 确认后端和 OnlyOffice 使用相同的 JWT 密钥
3. 检查 token 是否过期（默认 2 小时）

## 相关文档

- [OnlyOffice 数据流说明](./ONLYOFFICE_DATA_FLOW.md)
- [OnlyOffice 修复总结](./ONLYOFFICE_FIX_SUMMARY.md)
- [生产环境部署检查清单](./PRODUCTION_DEPLOYMENT_CHECKLIST.md)
