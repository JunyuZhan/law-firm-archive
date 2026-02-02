# 客户服务系统与管理系统数据交换流程

## 概述

本文档详细说明客户服务系统和管理系统之间的所有数据交换流程，包括事件触发机制、数据流向和驱动方式。

---

## 一、数据流向总览

```
┌─────────────────┐                    ┌─────────────────┐
│   管理系统      │                    │  客户服务系统    │
│ (Management)   │                    │ (Client Service)│
└─────────────────┘                    └─────────────────┘
         │                                       │
         │ 1. 推送项目信息                        │
         ├──────────────────────────────────────>│
         │                                       │
         │                                       │ 2. 客户操作触发
         │                                       │    - 访问链接
         │                                       │    - 上传文件
         │                                       │    - 下载文件
         │                                       │
         │ 3. 回调通知                           │
         │<──────────────────────────────────────┤
         │    - 文件上传回调                      │
         │    - 访问日志回调                      │
         │    - 下载日志回调                      │
         │                                       │
         │ 4. 前端查询显示                        │
         │<──────────────────────────────────────┤
         │    - 查询客户文件                      │
         │    - 查询访问日志                      │
         │    - 查询下载日志                      │
```

---

## 二、详细流程说明

### 2.1 管理系统主动推送（单向）

**触发时机：**
- 管理员在项目详情页点击"推送数据到客户服务系统"
- 通过 `ClientServicePanel` 组件的 `handlePush()` 方法触发

**流程：**
1. **前端调用** (`ClientServicePanel/index.vue`)
   ```typescript
   await pushMatterData({
     matterId: props.matterId,
     clientId: props.clientId,
     scopes: pushForm.scopes,
     validDays: pushForm.validDays,
     documentIds: selectedDocumentIds.value,
   })
   ```

2. **后端处理** (`MatterClientServiceController.pushMatterData()`)
   - 调用 `DataPushService.pushMatterData()`
   - 生成访问令牌（Token）
   - 调用客户服务系统 API 推送项目信息
   - 发送通知（短信/微信/邮件）

3. **客户服务系统接收** (`MatterManagementController.receiveMatter()`)
   - 保存项目信息到 `client_matter` 表
   - 保存推送的文件信息到 `client_file` 表

**数据存储：**
- 管理系统：`push_record` 表记录推送历史
- 客户服务系统：`client_matter` 和 `client_file` 表

---

### 2.2 客户上传文件（回调驱动）

**触发时机：**
- 客户在客户服务系统（小程序/公众号/H5）上传文件
- 通过客户服务系统的文件上传接口触发

**完整流程：**

#### 步骤1：客户上传文件
```
客户 → 客户服务系统前端 → POST /api/client/files/upload
```

**代码位置：** `client-service/backend/.../FileController.uploadFile()`

**处理逻辑：**
1. 验证访问令牌（Token）和项目ID
2. 调用 `FileService.uploadFile()` 保存文件
   - 文件存储到 MinIO/OSS/本地
   - 保存文件元数据到 `client_file` 表
3. **异步回调管理系统**（关键步骤）
   ```java
   callbackService.callbackUploadFile(clientFile, matter, token);
   ```

#### 步骤2：客户服务系统回调管理系统
```
客户服务系统 → POST /open/client/files → 管理系统
```

**代码位置：** 
- 发送：`client-service/.../CallbackService.callbackUploadFile()`
- 接收：`backend/.../ClientFileOpenController.receiveFile()`

**回调数据：**
```json
{
  "matterId": 123,              // 管理系统项目ID
  "clientId": 456,              // 客户ID
  "clientName": "张三",         // 客户姓名
  "fileName": "合同.pdf",       // 文件名
  "fileSize": 1024000,          // 文件大小
  "fileType": "application/pdf", // MIME类型
  "fileCategory": "CONTRACT",    // 文件类别
  "description": "合同文件",     // 描述
  "externalFileId": "file-123",  // 客户服务系统文件ID
  "externalFileUrl": "https://...", // 文件下载URL
  "uploadedBy": "张三",          // 上传人
  "uploadedAt": "2026-02-03T10:00:00" // 上传时间
}
```

#### 步骤3：管理系统接收并保存
**代码位置：** `backend/.../ClientFileService.receiveFile()`

**处理逻辑：**
1. 验证项目存在性
2. 检查是否重复（通过 `externalFileId`）
3. 保存到 `openapi_client_file` 表，状态为 `PENDING`（待同步）

#### 步骤4：管理系统前端显示
**代码位置：** `frontend/.../ClientServicePanel/index.vue`

**触发时机：**
- 组件挂载时：`onMounted()` → `loadClientFiles()`
- 项目ID变化时：`watch()` → `loadClientFiles()`
- 用户点击刷新按钮

**查询接口：**
```typescript
const res = await getPendingClientFiles({
  matterId: props.matterId,
  clientId: props.clientId,
});
```

**后端接口：** `MatterClientServiceController.getPendingFiles()`
- 查询 `openapi_client_file` 表
- 过滤条件：`matterId` + `status = 'PENDING'`

**显示位置：**
- 项目详情页 → "客户服务" Tab → "客户上传的文件" 卡片
- 显示文件列表，支持预览、同步到卷宗、忽略操作

---

### 2.3 客户访问链接（回调驱动）

**触发时机：**
- 客户点击管理系统推送的链接（短信/微信中的链接）
- 客户访问客户服务系统的门户页面

**完整流程：**

#### 步骤1：客户访问链接
```
客户 → 客户服务系统门户 → GET /api/portal/matter?token=xxx
```

**处理逻辑：**
- 验证 Token
- **记录访问日志**：调用 `AccessLogService.recordAccess()`
  ```java
  accessLogMapper.insert(accessLog);
  // 异步回调给管理系统
  callbackService.callbackAccessLog(accessLog);
  ```

#### 步骤2：客户服务系统回调管理系统
```
客户服务系统 → POST /open/client/access-log → 管理系统
```

**代码位置：**
- 发送：`client-service/.../CallbackService.callbackAccessLog()`
- 接收：`backend/.../ClientFileOpenController.receiveAccessLog()`

**回调数据：**
```json
{
  "matterId": 123,              // 管理系统项目ID
  "clientId": 456,              // 客户ID
  "accessTime": "2026-02-03T10:00:00", // 访问时间
  "ipAddress": "192.168.1.100", // IP地址
  "userAgent": "Mozilla/5.0...", // 用户代理
  "eventType": "ACCESS"         // 事件类型
}
```

#### 步骤3：管理系统接收并保存
**代码位置：** `backend/.../ClientAccessLogService.saveAccessLog()`

**处理逻辑：**
1. 验证项目存在性
2. 验证事件类型
3. 保存到 `client_access_log` 表

#### 步骤4：管理系统前端显示
**代码位置：** `frontend/.../ClientServicePanel/index.vue`

**触发时机：**
- 组件挂载时：`onMounted()` → `loadAccessLogs()`
- 项目ID变化时：`watch()` → `loadAccessLogs()`
- 用户点击刷新按钮

**查询接口：**
```typescript
const res = await getClientAccessLogs({
  matterId: props.matterId,
  clientId: props.clientId,
  pageNum: accessLogsPage.value,
  pageSize: accessLogsPageSize.value,
});
```

**后端接口：** `MatterClientServiceController.getAccessLogs()`
- 查询 `client_access_log` 表
- 过滤条件：`matterId` + `clientId`（可选）

**显示位置：**
- 项目详情页 → "客户服务" Tab → "📊 访问记录" 卡片
- 显示最新5条记录，包含访问时间、IP地址

---

### 2.4 客户下载文件（回调驱动）

**触发时机：**
- 客户在客户服务系统下载文件
- 通过客户服务系统的文件下载接口触发

**完整流程：**

#### 步骤1：客户下载文件
```
客户 → 客户服务系统 → GET /api/client/files/{fileId}/download?token=xxx
```

**代码位置：** `client-service/.../FileController.downloadFile()`

**处理逻辑：**
1. 验证 Token 和项目ID
2. 获取文件资源
3. **记录下载日志**（异步，失败不影响下载）
   ```java
   downloadLogService.recordDownload(
       matterId, clientId, fileId, fileName, token, request
   );
   ```
4. **记录推送文件首次下载时间**（如果是管理系统推送的文件）
   ```java
   fileService.recordFileDownload(fileId);
   ```

#### 步骤2：客户服务系统回调管理系统
```
客户服务系统 → POST /open/client/download-log → 管理系统
```

**代码位置：**
- 发送：`client-service/.../CallbackService.callbackDownloadLog()`
- 接收：`backend/.../ClientFileOpenController.receiveDownloadLog()`

**回调数据：**
```json
{
  "matterId": 123,              // 管理系统项目ID
  "clientId": 456,              // 客户ID
  "fileId": "file-123",         // 客户服务系统文件ID
  "fileName": "合同.pdf",       // 文件名
  "downloadTime": "2026-02-03T10:00:00", // 下载时间
  "ipAddress": "192.168.1.100", // IP地址
  "userAgent": "Mozilla/5.0...", // 用户代理
  "eventType": "DOWNLOAD"       // 事件类型
}
```

#### 步骤3：管理系统接收并保存
**代码位置：** `backend/.../ClientDownloadLogService.saveDownloadLog()`

**处理逻辑：**
1. 验证项目存在性
2. 验证事件类型
3. 保存到 `client_download_log` 表

#### 步骤4：管理系统前端显示
**代码位置：** `frontend/.../ClientServicePanel/index.vue`

**触发时机：**
- 组件挂载时：`onMounted()` → `loadDownloadLogs()`
- 项目ID变化时：`watch()` → `loadDownloadLogs()`
- 用户点击刷新按钮

**查询接口：**
```typescript
const res = await getClientDownloadLogs({
  matterId: props.matterId,
  clientId: props.clientId,
  pageNum: downloadLogsPage.value,
  pageSize: downloadLogsPageSize.value,
});
```

**后端接口：** `MatterClientServiceController.getDownloadLogs()`
- 查询 `client_download_log` 表
- 过滤条件：`matterId` + `clientId`（可选）

**显示位置：**
- 项目详情页 → "客户服务" Tab → "📥 下载记录" 卡片
- 显示最新5条记录，包含下载时间、文件名、IP地址

---

## 三、数据存储位置

### 3.1 管理系统数据库表

| 表名 | 用途 | 数据来源 |
|------|------|----------|
| `push_record` | 推送记录 | 管理系统主动推送 |
| `openapi_client_file` | 客户上传的文件 | 客户服务系统回调 |
| `client_access_log` | 客户访问日志 | 客户服务系统回调 |
| `client_download_log` | 客户下载日志 | 客户服务系统回调 |

### 3.2 客户服务系统数据库表

| 表名 | 用途 | 数据来源 |
|------|------|----------|
| `client_matter` | 项目信息 | 管理系统推送 |
| `client_file` | 文件信息 | 客户上传 + 管理系统推送 |
| `access_log` | 访问日志 | 客户访问触发 |
| `download_log` | 下载日志 | 客户下载触发 |

---

## 四、关键配置

### 4.1 客户服务系统配置

**回调地址配置：**
- 配置文件：`client-service/backend/src/main/resources/application.yml`
- 配置项：`client-service.callback.law-firm-url`
- 系统配置表：`sys_config` 表中的 `callback.law-firm-url` 键值

**回调开关：**
- 配置项：`client-service.callback.enabled`（默认：`true`）

### 4.2 管理系统开放接口

所有回调接口位于：`/open/client/*`
- `/open/client/files` - 接收文件上传回调
- `/open/client/access-log` - 接收访问日志回调
- `/open/client/download-log` - 接收下载日志回调

---

## 五、异步处理机制

### 5.1 回调异步化

所有回调都使用 `@Async` 注解，确保：
- **不影响主流程**：文件上传/下载/访问不会因为回调失败而失败
- **非阻塞**：回调在后台线程执行，不阻塞用户操作
- **容错性**：回调失败只记录日志，不影响业务逻辑

**代码示例：**
```java
@Async
public void callbackAccessLog(final AccessLog accessLog) {
    // 回调逻辑
    // 失败只记录日志，不影响主流程
}
```

### 5.2 双重存储

所有日志都采用**双重存储**策略：
1. **客户服务系统**：本地存储，用于客户服务系统内部查询
2. **管理系统**：通过回调存储，用于管理系统查询和展示

---

## 六、前端数据刷新机制

### 6.1 自动刷新

**触发时机：**
1. **组件挂载**：`onMounted()` 钩子
2. **项目切换**：`watch()` 监听 `matterId` 和 `clientId` 变化
3. **手动刷新**：用户点击"刷新"按钮

**代码示例：**
```typescript
onMounted(() => {
  if (props.clientId && props.matterId) {
    loadData();
    loadClientFiles();      // 加载客户上传的文件
    loadAccessLogs();       // 加载访问日志
    loadDownloadLogs();      // 加载下载日志
  }
});
```

### 6.2 数据展示

**客户上传的文件：**
- 位置：`ClientServicePanel` → "客户上传的文件" 卡片
- 数据源：`getPendingClientFiles()` API
- 显示：文件列表，支持预览、同步、忽略操作

**访问记录：**
- 位置：`ClientServicePanel` → "📊 访问记录" 卡片
- 数据源：`getClientAccessLogs()` API
- 显示：最新5条记录，包含时间、IP地址

**下载记录：**
- 位置：`ClientServicePanel` → "📥 下载记录" 卡片
- 数据源：`getClientDownloadLogs()` API
- 显示：最新5条记录，包含时间、文件名、IP地址

---

## 七、总结

### 7.1 数据流向

1. **管理系统 → 客户服务系统**（主动推送）
   - 项目信息推送
   - 文件推送

2. **客户服务系统 → 管理系统**（回调通知）
   - 文件上传回调
   - 访问日志回调
   - 下载日志回调

3. **管理系统前端 → 管理系统后端**（查询显示）
   - 查询客户文件
   - 查询访问日志
   - 查询下载日志

### 7.2 驱动机制

- **事件驱动**：客户操作（上传/下载/访问）触发回调
- **用户驱动**：管理员在项目详情页查看数据
- **自动驱动**：前端组件挂载时自动加载数据

### 7.3 数据同步

- **实时性**：回调采用异步方式，确保实时性
- **可靠性**：双重存储确保数据不丢失
- **容错性**：回调失败不影响主流程

---

## 八、常见问题

### Q1: 客户上传的文件为什么没有立即显示在管理系统？

**A:** 检查以下几点：
1. 客户服务系统的回调地址配置是否正确
2. 回调是否启用（`client-service.callback.enabled=true`）
3. 管理系统是否成功接收回调（查看日志）
4. 前端是否刷新了数据（点击刷新按钮）

### Q2: 访问日志和下载日志为什么没有记录？

**A:** 检查以下几点：
1. 客户是否真的访问/下载了文件
2. 客户服务系统的回调服务是否正常
3. 管理系统的回调接收接口是否正常
4. 数据库表是否正确创建

### Q3: 如何测试回调功能？

**A:** 
1. 确保客户服务系统和管理系统都正常运行
2. 配置正确的回调地址
3. 执行客户操作（上传/下载/访问）
4. 查看客户服务系统日志，确认回调已发送
5. 查看管理系统日志，确认回调已接收
6. 在管理系统前端查看数据是否显示

---

**文档版本：** 1.0  
**最后更新：** 2026-02-03
