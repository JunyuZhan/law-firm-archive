# 档案管理系统 API 对接指南

## 1. 概述

档案管理系统提供开放API，供外部系统（如律所案件管理系统）进行对接，实现自动归档功能。

### 系统架构

```
┌─────────────────────┐           ┌─────────────────────┐
│   律所管理系统      │  HTTP/MQ  │   档案管理系统      │
│   (Law Firm CMS)    │ ────────► │   (Archive System)  │
│                     │           │                     │
│  - 案件管理         │ ◄──────── │  - 档案接收         │
│  - 文档管理         │  回调通知  │  - 文件下载存储     │
│  - 归档触发         │           │  - 索引建立         │
└─────────────────────┘           └─────────────────────┘
```

### 对接流程

1. **注册来源** - 在档案系统注册外部系统信息
2. **推送档案** - 案件归档时调用API推送档案数据
3. **文件处理** - 档案系统下载并存储文件
4. **回调通知** - 处理完成后通知外部系统

---

## 2. API 接口

### 2.1 档案接收接口

**接口地址**: `POST /api/open/archive/receive`

**说明**: 接收外部系统推送的档案数据，支持同步/异步处理

**请求头**:
```
Content-Type: application/json
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| sourceType | String | 是 | 来源类型：LAW_FIRM, COURT, ENTERPRISE |
| sourceId | String | 是 | 来源系统的唯一标识（如案件ID） |
| sourceNo | String | 否 | 来源系统的业务编号（如案件号） |
| callbackUrl | String | 否 | 回调通知URL |
| async | Boolean | 否 | 是否异步处理，默认true |
| title | String | 是 | 档案题名 |
| archiveType | String | 是 | 档案类型：DOCUMENT, LITIGATION, CONTRACT等 |
| retentionPeriod | String | 是 | 保管期限：PERMANENT, Y30, Y15, Y10, Y5 |
| responsibility | String | 否 | 责任者/主办律师 |
| documentDate | Date | 否 | 文件日期 |
| securityLevel | String | 否 | 密级：PUBLIC, INTERNAL, SECRET |
| caseNo | String | 否 | 案件编号 |
| caseName | String | 否 | 案件名称 |
| clientName | String | 否 | 委托人 |
| lawyerName | String | 否 | 主办律师 |
| caseCloseDate | Date | 否 | 结案日期 |
| files | Array | 否 | 电子文件列表 |
| keywords | String | 否 | 关键词 |
| archiveAbstract | String | 否 | 摘要 |
| remarks | String | 否 | 备注 |
| metadata | Object | 否 | 扩展元数据 |

**files 数组元素**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| fileName | String | 是 | 文件名 |
| fileType | String | 否 | 文件类型 |
| downloadUrl | String | 是 | 文件下载URL |
| fileSize | Long | 否 | 文件大小(字节) |
| fileCategory | String | 否 | 文件分类 |
| description | String | 否 | 文件描述 |

**请求示例**:

```json
{
    "sourceType": "LAW_FIRM",
    "sourceId": "CASE-2026-001234",
    "sourceNo": "（2026）沪民初字第1234号",
    "callbackUrl": "https://lawfirm.example.com/api/archive/callback",
    "async": true,
    "title": "张三诉李四合同纠纷案",
    "archiveType": "LITIGATION",
    "retentionPeriod": "Y10",
    "responsibility": "王律师",
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
            "fileType": "pdf",
            "downloadUrl": "https://lawfirm.example.com/files/download/12345",
            "fileSize": 102400,
            "fileCategory": "诉讼文书"
        },
        {
            "fileName": "授权委托书.pdf",
            "fileType": "pdf",
            "downloadUrl": "https://lawfirm.example.com/files/download/12346",
            "fileSize": 51200,
            "fileCategory": "委托文书"
        }
    ],
    "keywords": "合同纠纷,民事诉讼",
    "archiveAbstract": "本案为张三与李四之间的买卖合同纠纷...",
    "metadata": {
        "courtName": "上海市浦东新区人民法院",
        "caseAmount": 500000,
        "caseResult": "胜诉"
    }
}
```

**响应示例**:

```json
{
    "success": true,
    "code": "200",
    "message": "档案接收成功",
    "data": {
        "archiveId": 12345,
        "archiveNo": "ARC-2026-0001234",
        "status": "RECEIVED",
        "receivedAt": "2026-02-13T15:30:00",
        "fileCount": 2,
        "message": "档案已接收，正在处理中"
    }
}
```

### 2.2 健康检查接口

**接口地址**: `GET /api/open/health`

**响应示例**:
```json
{
    "success": true,
    "code": "200",
    "message": "success",
    "data": "ok"
}
```

---

## 3. 回调通知

当档案处理完成后，系统会向 `callbackUrl` 发送POST请求通知处理结果。

### 回调请求

**请求头**:
```
Content-Type: application/json
X-Callback-Timestamp: 1707812345678
X-Callback-Source: archive-system
X-Callback-Signature: abc123...
```

**请求体**:
```json
{
    "archiveId": 12345,
    "archiveNo": "ARC-2026-0001234",
    "sourceType": "LAW_FIRM",
    "sourceId": "CASE-2026-001234",
    "status": "STORED",
    "successCount": 2,
    "failedCount": 0,
    "totalCount": 2,
    "completedAt": "2026-02-13T15:35:00"
}
```

**回调状态说明**:

| 状态 | 说明 |
|-----|------|
| STORED | 归档完成 |
| PARTIAL | 部分成功 |
| FAILED | 处理失败 |

### 回调响应

外部系统需返回HTTP 200表示接收成功，否则档案系统会进行重试。

---

## 4. 状态码说明

| 代码 | 说明 |
|-----|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 认证失败 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 5. 最佳实践

### 5.1 文件下载URL要求

- URL必须可公开访问，或提供认证信息
- 建议使用HTTPS
- 建议设置合理的过期时间（如1小时）
- 文件大小建议不超过100MB

### 5.2 幂等性处理

- 使用 `sourceType + sourceId` 作为唯一标识
- 重复推送会更新原有档案

### 5.3 异步处理

- 建议使用异步模式（async=true）
- 通过回调获取处理结果
- 避免长时间等待响应

### 5.4 错误处理

- 保存接口返回的 archiveId 用于后续查询
- 实现回调接口接收处理结果
- 处理回调失败时的重试逻辑
