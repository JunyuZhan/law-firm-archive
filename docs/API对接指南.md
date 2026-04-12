# 档案管理系统 API 对接指南

本文档描述当前仓库后端实际提供的开放接口，以及外部系统接入时需要关注的认证、请求格式与回调约定。

## 1. 基础信息

默认基础地址：

- 本地后端：`http://localhost:8090/api`
- Docker 前端反代入口：`http://localhost:3001/api`

开放接口统一位于：

- `/api/open/**`

返回结构统一为：

```json
{
  "success": true,
  "code": "200",
  "message": "操作结果说明",
  "data": {}
}
```

## 2. 认证规则

### 2.1 需要 `X-API-Key`

- `POST /api/open/archive/receive`
- `POST /api/open/borrow/apply`

### 2.2 无需 `X-API-Key`

- `GET /api/open/health`
- `GET /api/open/borrow/access/{token}`
- `POST /api/open/borrow/access/{token}/download/{fileId}`

说明：

- 当前代码使用 `X-API-Key` 作为开放接口认证方式
- 当前版本不要求请求签名，也不要求 `X-Timestamp` / `X-Signature`
- API Key 在后台“来源管理”中创建与维护

## 3. 档案接收接口

### 3.1 接口定义

- 方法：`POST`
- 路径：`/api/open/archive/receive`
- 请求头：

```http
Content-Type: application/json
X-API-Key: your-api-key
```

### 3.2 必填字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `sourceType` | String | 来源类型，例如 `LAW_FIRM` |
| `sourceId` | String | 来源系统中的唯一业务标识 |
| `title` | String | 档案题名 |
| `archiveType` | String | 档案门类 |
| `retentionPeriod` | String | 保管期限代码 |

### 3.3 常用字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `sourceNo` | String | 来源业务编号 |
| `callbackUrl` | String | 处理完成后的回调地址 |
| `async` | Boolean | 是否异步处理，默认 `true` |
| `responsibility` | String | 责任者 |
| `responsiblePerson` | String | 兼容字段，会映射到 `responsibility` |
| `documentDate` | Date | 文件日期，格式 `yyyy-MM-dd` |
| `securityLevel` | String | `PUBLIC` / `INTERNAL` / `SECRET` / `CONFIDENTIAL` |
| `caseNo` | String | 案件编号 |
| `caseName` | String | 案件名称 |
| `clientName` | String | 委托人 |
| `lawyerName` | String | 主办律师 |
| `caseCloseDate` | Date/String | 结案日期，兼容字符串格式 |
| `keywords` | String | 关键词 |
| `archiveAbstract` | String | 摘要 |
| `description` | String | 兼容字段，会映射到摘要 |
| `metadata` | Object | 扩展元数据 |
| `extraAttributes` | Object | 兼容字段，会与 `metadata` 合并 |

### 3.4 文件数组

`files` 为可选数组，元素结构如下：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileName` | String | 是 | 文件名 |
| `downloadUrl` | String | 是 | 文件下载地址 |
| `fileType` | String | 否 | 文件类型 |
| `mimeType` | String | 否 | MIME 类型，兼容字段 |
| `fileSize` | Long | 否 | 文件大小（字节） |
| `fileCategory` | String | 否 | 文件分类 |
| `description` | String | 否 | 文件描述 |

### 3.5 请求示例

```json
{
  "sourceType": "LAW_FIRM",
  "sourceId": "CASE-2026-001234",
  "sourceNo": "（2026）沪民初字第1234号",
  "callbackUrl": "https://lawfirm.example.com/api/archive/callback",
  "async": true,
  "title": "张三诉李四合同纠纷案",
  "archiveType": "DOCUMENT",
  "retentionPeriod": "Y10",
  "responsiblePerson": "王律师",
  "documentDate": "2026-02-13",
  "securityLevel": "INTERNAL",
  "caseNo": "CASE-2026-001234",
  "caseName": "张三诉李四合同纠纷案",
  "clientName": "张三",
  "lawyerName": "王律师",
  "caseCloseDate": "2026-02-10",
  "files": [
    {
      "fileName": "起诉状.pdf",
      "fileType": "application/pdf",
      "downloadUrl": "https://lawfirm.example.com/files/12345",
      "fileSize": 102400,
      "fileCategory": "诉讼文书"
    }
  ],
  "keywords": "合同纠纷,民事诉讼",
  "archiveAbstract": "本案为合同纠纷归档材料",
  "metadata": {
    "courtName": "上海市浦东新区人民法院",
    "caseAmount": 500000
  }
}
```

### 3.6 响应示例

```json
{
  "success": true,
  "code": "200",
  "message": "档案接收成功",
  "data": {
    "archiveId": 12345,
    "archiveNo": "ARC-20260328-0001",
    "status": "PROCESSING",
    "receivedAt": "2026-03-28T23:00:00",
    "message": "档案接收成功，正在处理中"
  }
}
```

## 4. 电子借阅申请接口

### 4.1 接口定义

- 方法：`POST`
- 路径：`/api/open/borrow/apply`
- 请求头：

```http
Content-Type: application/json
X-API-Key: your-api-key
```

### 4.2 用途

外部系统为电子档案申请一个临时访问链接，用于无登录场景的受控访问。

### 4.3 返回值

返回借阅链接、过期时间、是否允许下载等信息。具体字段以当前 Swagger / Knife4j 文档为准。

### 4.4 撤销电子借阅链接

- 方法：`POST`
- 路径：`/api/open/borrow/revoke/{linkId}`
- 请求头：

```http
Content-Type: application/json
X-API-Key: your-api-key
```

- 查询参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reason` | String | 否 | 撤销原因 |

- 说明：
  - 外部系统可主动撤销已发出的电子借阅链接
  - 撤销后，原访问地址会立即失效

## 5. 公开访问与下载记录

### 5.1 公开访问

- 方法：`GET`
- 路径：`/api/open/borrow/access/{token}`
- 认证：无需 `X-API-Key`

成功时返回借阅内容的访问结果；无效 token 会返回业务失败信息。

### 5.2 记录下载

- 方法：`POST`
- 路径：`/api/open/borrow/access/{token}/download/{fileId}`
- 认证：无需 `X-API-Key`

用于记录通过公开借阅链接下载文件的行为。

## 6. 健康检查

- 方法：`GET`
- 路径：`/api/open/health`

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": "ok"
}
```

## 7. 回调约定

如果档案接收请求中提供了 `callbackUrl`，系统处理完成后会按业务实现发送回调通知。

建议外部系统：

- 提供稳定可访问的 HTTPS 地址
- 对回调请求记录日志
- 返回 HTTP 200 表示已成功接收
- 对重复回调做幂等处理

当前仓库中对回调头的约定包括：

- `X-Callback-Timestamp`
- `X-Callback-Source`
- `X-Callback-Signature`

`X-Callback-Source` 当前固定为 `archive-system`。

## 8. 错误处理建议

- `401`：通常表示缺少或错误的 `X-API-Key`
- `400`：请求参数校验失败
- `500`：服务端处理异常，应结合服务端日志排查

建议外部系统保留以下字段：

- `sourceType`
- `sourceId`
- `archiveId`
- `archiveNo`

这样便于排查幂等、回调和状态追踪问题。

## 9. 联调建议

- 优先先打 `GET /api/open/health`
- 再用最小必填字段调用 `POST /api/open/archive/receive`
- 最后再补充文件列表、回调、扩展元数据

如需实际 curl 示例，参见 [integration-test.md](./integration-test.md)。
