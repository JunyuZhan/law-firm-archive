# 案源接口

案源管理接口。

## 案源列表

```
GET /api/lead/list
```

查询参数：

| 参数         | 类型   | 说明         |
| ------------ | ------ | ------------ |
| pageNum      | number | 页码         |
| pageSize     | number | 每页数量     |
| clientName   | string | 客户名称     |
| source       | string | 案源来源     |
| status       | string | 案源状态     |
| followerId   | number | 跟进人ID     |

## 案源详情

```
GET /api/lead/{id}
```

## 创建案源

```
POST /api/lead
```

请求示例：

```json
{
  "clientName": "潜在客户名称",
  "contactPerson": "联系人",
  "contactPhone": "联系电话",
  "source": "REFERRAL",
  "estimatedAmount": 50000,
  "followerId": 1,
  "remark": "备注信息"
}
```

## 更新案源

```
PUT /api/lead/{id}
```

## 删除案源

```
DELETE /api/lead/{id}
```

## 案源来源

| 值           | 说明       |
| ------------ | ---------- |
| REFERRAL     | 转介绍     |
| ONLINE       | 网络       |
| PHONE        | 电话咨询   |
| VISIT        | 上门咨询   |
| OTHER        | 其他       |

## 案源状态

| 值           | 说明       |
| ------------ | ---------- |
| NEW          | 新建       |
| FOLLOWING    | 跟进中     |
| CONVERTED    | 已转化     |
| LOST         | 已流失     |

## 转化案源

将案源转化为正式客户。

```
POST /api/lead/{id}/convert
```

## 更新跟进记录

```
POST /api/lead/{id}/follow-up
```

请求示例：

```json
{
  "content": "跟进记录内容",
  "nextStep": "下一步计划",
  "nextContactTime": "2026-01-12 10:00:00"
}
```

## 获取案源跟进记录

```
GET /api/lead/{id}/follow-ups
```

---

## 文档更新记录

| 更新时间 | 更新内容 | 操作人 |
|----------|----------|--------|
| 2026-01-11 | 创建案源接口文档骨架 | AI Assistant |
