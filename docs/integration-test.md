# 档案系统开放接口联调指南

本文档用于本地或测试环境下联调开放接口，内容以当前仓库实际实现为准。

## 1. 联调前准备

### 1.1 启动服务

推荐：

```bash
cd docker
docker compose up -d
```

可用入口：

- 后端 API：`http://localhost:8090/api`
- 前端反代：`http://localhost:3001/api`

### 1.2 创建来源并获取 API Key

开放接口中的档案接收与借阅申请依赖 `X-API-Key`。

创建方式：

1. 登录系统后台
2. 进入“来源管理”
3. 新建来源系统
4. 保存创建时返回的 API Key

## 2. 健康检查

```bash
curl -X GET "http://localhost:8090/api/open/health"
```

预期响应：

```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": "ok"
}
```

## 3. 档案接收测试

### 3.1 最小请求样例

```bash
API_KEY="your-api-key"

curl -X POST "http://localhost:8090/api/open/archive/receive" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -d '{
    "sourceType": "LAW_FIRM",
    "sourceId": "CASE-TEST-001",
    "title": "联调测试档案",
    "archiveType": "DOCUMENT",
    "retentionPeriod": "Y10"
  }'
```

### 3.2 带文件元数据的样例

```bash
API_KEY="your-api-key"

curl -X POST "http://localhost:8090/api/open/archive/receive" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -d '{
    "sourceType": "LAW_FIRM",
    "sourceId": "CASE-TEST-002",
    "sourceNo": "（2026）测试字第002号",
    "callbackUrl": "https://example.com/archive/callback",
    "async": true,
    "title": "联调测试档案-含文件",
    "archiveType": "DOCUMENT",
    "retentionPeriod": "Y10",
    "responsiblePerson": "王律师",
    "caseNo": "CASE-TEST-002",
    "caseName": "测试案件",
    "clientName": "测试客户",
    "lawyerName": "王律师",
    "files": [
      {
        "fileName": "起诉状.pdf",
        "fileType": "application/pdf",
        "downloadUrl": "https://example.com/files/complaint.pdf",
        "fileSize": 102400,
        "fileCategory": "诉讼文书"
      }
    ]
  }'
```

### 3.3 预期响应

```json
{
  "success": true,
  "code": "200",
  "message": "档案接收成功",
  "data": {
    "archiveId": 123,
    "archiveNo": "ARC-20260328-0001",
    "status": "PROCESSING"
  }
}
```

## 4. 电子借阅申请测试

```bash
API_KEY="your-api-key"

curl -X POST "http://localhost:8090/api/open/borrow/apply" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -d '{
    "archiveId": 123,
    "userId": "U1001",
    "userName": "测试用户",
    "reason": "联调测试借阅"
  }'
```

说明：

- 具体必填字段以当前 Swagger / Knife4j 生成为准
- 如果你是从业务系统联调，建议先在后台或数据库准备一条可借阅档案

## 5. 借阅链接公开访问

拿到借阅 token 后：

```bash
TOKEN="your-token"

curl -X GET "http://localhost:8090/api/open/borrow/access/${TOKEN}"
```

记录下载：

```bash
TOKEN="your-token"
FILE_ID="1"

curl -X POST "http://localhost:8090/api/open/borrow/access/${TOKEN}/download/${FILE_ID}"
```

## 6. 常见问题

### 6.1 返回 401

通常是：

- 缺少 `X-API-Key`
- API Key 无效
- 对应来源已禁用

### 6.2 返回 400

通常是：

- 缺少必填字段
- 字段格式错误
- JSON 结构不符合 DTO

### 6.3 返回 500

优先检查：

- 后端日志
- 外部依赖服务是否可用：PostgreSQL / Redis / MinIO / RabbitMQ / Elasticsearch
- `downloadUrl` 是否可访问

## 7. 联调检查清单

- [ ] `GET /api/open/health` 可用
- [ ] 已在后台创建来源并取得 API Key
- [ ] 使用最小请求可成功接收档案
- [ ] 带 `files` 的请求可成功接收
- [ ] 如果配置了 `callbackUrl`，回调链路可接收
- [ ] 借阅申请可返回链接或业务错误信息
- [ ] 公开借阅链接访问与下载记录接口可用
