# 客户门户接口

客户门户公开接口，使用令牌（Token）进行身份验证。

## 认证方式

所有接口需要在 Header 或 Query 参数中携带令牌：

- Header: `X-Portal-Token: <token>`
- Query: `?token=<token>`

Header 方式优先级高于 Query 参数。

## 接口列表

### 验证令牌

检查访问令牌是否有效。

```
GET /api/open/portal/validate
```

响应：

```json
{
  "code": 200,
  "data": {
    "valid": true,
    "message": "令牌有效",
    "expiresAt": "2026-02-01T12:00:00",
    "scope": ["MATTER:VIEW", "FILE:DOWNLOAD"]
  }
}
```

### 获取项目信息

客户通过令牌查看项目详情。

```
GET /api/open/portal/matter
```

响应：

```json
{
  "code": 200,
  "data": {
    "matterName": "张三诉李四合同纠纷案",
    "status": "IN_PROGRESS",
    "progress": 30,
    "lawyer": "王律师",
    "latestUpdate": "2026-01-15: 已提交起诉状"
  }
}
```
