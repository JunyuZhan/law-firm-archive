# 关键功能测试清单 - OnlyOffice 和 MinIO 集成

> ⚠️ **部署后必须测试**：文档编辑功能涉及多个服务配合，必须完整测试！

## 🎯 测试目标

验证单端口架构迁移后，OnlyOffice 文档编辑功能（特别是 DOC 文件）与 MinIO 的配合是否正常。

---

## 📋 完整测试流程

### 测试场景：编辑 DOC 文件

#### 步骤 1：准备测试文件

1. **登录系统**
   - 使用管理员账号登录
   - 进入卷宗管理模块

2. **上传测试文件**
   - 上传一个 DOC 文件（Word 文档）
   - 确认上传成功
   - 记录文档 ID

#### 步骤 2：测试文档编辑 - 获取文件

**操作：**
1. 点击文档的"编辑"按钮
2. 等待 OnlyOffice 编辑器加载

**预期结果：**
- ✅ OnlyOffice 编辑器正常加载（不显示错误）
- ✅ 文档内容正确显示
- ✅ 可以正常查看和编辑文档

**检查日志：**
```bash
# Backend 日志 - 应该看到文件 URL 生成
docker logs law-firm-backend | grep "生成 OnlyOffice 文档代理 URL" | tail -5

# Backend 日志 - 应该看到文件代理请求
docker logs law-firm-backend | grep "OnlyOffice 文件代理" | tail -5

# OnlyOffice 日志 - 检查是否有错误
docker logs onlyoffice | tail -30 | grep -i error
```

**验证点：**
- ✅ OnlyOffice 能通过 `http://backend:8080/api/document/{id}/file-proxy` 获取文件
- ✅ Backend 能通过 `http://law-firm-minio:9000` 从 MinIO 获取文件
- ✅ 文件内容正确返回给 OnlyOffice

#### 步骤 3：测试文档编辑 - 修改内容

**操作：**
1. 在 OnlyOffice 编辑器中修改文档内容
   - 添加一些文字
   - 修改格式
   - 保存修改

**预期结果：**
- ✅ 可以正常编辑
- ✅ 修改实时显示
- ✅ 没有错误提示

#### 步骤 4：测试文档保存

**操作：**
1. 点击"保存"按钮（或关闭编辑器）
2. 等待保存完成提示

**预期结果：**
- ✅ 保存成功提示
- ✅ 没有错误信息
- ✅ 文档已更新

**检查日志：**
```bash
# Backend 日志 - 应该看到回调请求
docker logs law-firm-backend | grep "OnlyOffice 回调" | tail -5

# Backend 日志 - 应该看到保存成功
docker logs law-firm-backend | grep "OnlyOffice 文档保存成功" | tail -5

# Backend 日志 - 检查 MinIO 上传
docker logs law-firm-backend | grep "文件上传成功\|uploadFile" | tail -5
```

**验证点：**
- ✅ OnlyOffice 能通过 `http://backend:8080/api/document/{id}/callback` 回调 backend
- ✅ Backend 能从 OnlyOffice 下载文件（Docker 内部网络）
- ✅ Backend 能保存文件到 MinIO（Docker 内部网络）
- ✅ 文件已更新到 MinIO

#### 步骤 5：验证文件已更新

**操作：**
1. 重新打开文档
2. 查看修改内容是否正确

**预期结果：**
- ✅ 文档内容已更新
- ✅ 修改的内容正确显示

**检查：**
```bash
# 检查 MinIO 中的文件（通过 Console）
# 访问 http://your-domain/minio-console/
# 查看文件修改时间是否更新
```

---

## 🔍 关键配置验证

### 1. 容器间网络连通性

**运行测试脚本：**
```bash
./scripts/test-onlyoffice-minio-integration.sh
```

**手动验证：**
```bash
# OnlyOffice → Backend
docker exec onlyoffice curl -I http://backend:8080/api/actuator/health

# Backend → MinIO
docker exec law-firm-backend curl -I http://law-firm-minio:9000/minio/health/live

# OnlyOffice → MinIO（如果需要）
docker exec onlyoffice curl -I http://law-firm-minio:9000/minio/health/live
```

**预期结果：**
- ✅ 所有连接都返回 200 OK
- ✅ 不依赖端口映射

### 2. 环境变量配置

**检查 Backend 环境变量：**
```bash
docker exec law-firm-backend env | grep -E "ONLYOFFICE|FILE_SERVER|MINIO_ENDPOINT"
```

**应该看到：**
```
ONLYOFFICE_CALLBACK_URL=http://backend:8080/api
FILE_SERVER_URL=http://law-firm-minio:9000
MINIO_ENDPOINT=http://law-firm-minio:9000
ONLYOFFICE_URL=/onlyoffice
```

**验证点：**
- ✅ 所有地址都使用 Docker 内部网络
- ✅ 不包含外部 IP 或端口映射

### 3. Nginx 配置

**检查 Nginx 配置：**
```bash
docker exec law-firm-frontend cat /etc/nginx/nginx.conf | grep -A 10 "location /onlyoffice/"
```

**应该看到：**
- ✅ `proxy_pass http://onlyoffice:80/;`
- ✅ `proxy_set_header Authorization $http_authorization;`
- ✅ WebSocket 支持配置

---

## ⚠️ 常见问题排查

### 问题 1：OnlyOffice 无法加载文档

**症状：**
- OnlyOffice 编辑器显示"无法加载文档"
- 浏览器控制台显示错误

**排查步骤：**
```bash
# 1. 检查 OnlyOffice 能否访问 backend
docker exec onlyoffice curl -v http://backend:8080/api/actuator/health

# 2. 检查 backend 日志
docker logs law-firm-backend | grep -i "file-proxy\|onlyoffice" | tail -20

# 3. 检查 token 验证
docker logs law-firm-backend | grep -i "拒绝未授权访问\|token" | tail -10
```

**可能原因：**
- OnlyOffice 无法访问 backend（网络问题）
- Token 验证失败
- MinIO 文件不存在

### 问题 2：保存失败

**症状：**
- 保存后提示错误
- Backend 日志显示保存失败

**排查步骤：**
```bash
# 1. 检查 OnlyOffice 回调
docker logs law-firm-backend | grep "OnlyOffice 回调" | tail -10

# 2. 检查 URL 规范化
docker logs law-firm-backend | grep "OnlyOffice 下载 URL 规范化" | tail -10

# 3. 检查 MinIO 上传
docker logs law-firm-backend | grep "文件上传\|uploadFile" | tail -10
```

**可能原因：**
- OnlyOffice 回调 URL 错误
- Backend 无法从 OnlyOffice 下载文件
- Backend 无法保存到 MinIO

### 问题 3：文件内容不正确

**症状：**
- 文档可以打开，但内容错误
- 保存后内容丢失

**排查步骤：**
```bash
# 1. 检查文件代理是否正确
docker logs law-firm-backend | grep "OnlyOffice 文件代理" | tail -10

# 2. 检查 MinIO 文件
docker exec law-firm-minio mc ls local/law-firm/ | head -10
```

---

## ✅ 测试检查清单

### 基础功能

- [ ] **文档上传**
  - [ ] 上传 DOC 文件成功
  - [ ] 文件保存到 MinIO

- [ ] **文档编辑 - 获取文件**
  - [ ] 点击"编辑"按钮
  - [ ] OnlyOffice 编辑器正常加载
  - [ ] 文档内容正确显示
  - [ ] 没有错误提示

- [ ] **文档编辑 - 修改内容**
  - [ ] 可以正常编辑
  - [ ] 修改实时显示
  - [ ] 没有错误提示

- [ ] **文档保存**
  - [ ] 点击"保存"
  - [ ] 保存成功提示
  - [ ] 没有错误信息

- [ ] **文档验证**
  - [ ] 重新打开文档
  - [ ] 修改内容正确显示
  - [ ] 文件已更新

### 容器间通信

- [ ] **OnlyOffice → Backend**
  - [ ] OnlyOffice 能访问 `/file-proxy` 接口
  - [ ] OnlyOffice 能回调 `/callback` 接口

- [ ] **Backend → MinIO**
  - [ ] Backend 能从 MinIO 获取文件
  - [ ] Backend 能保存文件到 MinIO

### 配置验证

- [ ] **环境变量**
  - [ ] `ONLYOFFICE_CALLBACK_URL=http://backend:8080/api` ✅
  - [ ] `FILE_SERVER_URL=http://law-firm-minio:9000` ✅
  - [ ] `MINIO_ENDPOINT=http://law-firm-minio:9000` ✅

- [ ] **Nginx 配置**
  - [ ] `/onlyoffice/` location 配置正确 ✅
  - [ ] WebSocket 支持配置 ✅

- [ ] **端口映射**
  - [ ] OnlyOffice 端口未暴露 ✅
  - [ ] MinIO 端口未暴露 ✅

---

## 📝 测试记录模板

```
测试时间：____年__月__日 __:__
测试人员：_______

测试文件：
- 文件名：_______
- 文件大小：_______ MB
- 文档 ID：_______

测试结果：
1. 文档上传：✅ / ❌
2. 编辑器加载：✅ / ❌
3. 文档显示：✅ / ❌
4. 编辑功能：✅ / ❌
5. 保存功能：✅ / ❌
6. 文件更新：✅ / ❌

错误信息（如有）：
_________________________________

日志检查：
- Backend 日志：✅ / ❌
- OnlyOffice 日志：✅ / ❌
- MinIO 日志：✅ / ❌
```

---

## 🎯 关键验证点总结

### ✅ 配置正确性（已验证）

1. **OnlyOffice 获取文件**
   - ✅ 使用 `/file-proxy` 接口（不直接访问 MinIO）
   - ✅ 通过 Docker 内部网络访问 backend
   - ✅ **不依赖端口映射**

2. **Backend 访问 MinIO**
   - ✅ 使用 Docker 内部地址 `http://law-firm-minio:9000`
   - ✅ **不依赖端口映射**

3. **OnlyOffice 保存文件**
   - ✅ 回调使用 Docker 内部地址
   - ✅ Backend 访问 OnlyOffice 和 MinIO 都使用 Docker 内部网络
   - ✅ **不依赖端口映射**

### ⚠️ 必须实际测试

**虽然配置看起来正确，但必须进行实际测试验证！**

**测试重点：**
1. ✅ 上传 DOC 文件
2. ✅ 点击编辑，OnlyOffice 正常加载
3. ✅ 修改文档内容
4. ✅ 保存文档
5. ✅ 验证文件已更新

---

## 🚨 如果测试失败

1. **立即停止部署**
2. **检查日志**：`docker logs law-firm-backend | grep -i onlyoffice`
3. **运行测试脚本**：`./scripts/test-onlyoffice-minio-integration.sh`
4. **参考文档**：
   - `docs/ONLYOFFICE-MINIO-INTEGRATION-TEST.md`
   - `docs/ONLYOFFICE-MINIO-CONFIGURATION-CHECK.md`
5. **回滚配置**（如果需要）：使用备份文件恢复

---

**⚠️ 重要：部署后必须立即测试文档编辑功能！**
