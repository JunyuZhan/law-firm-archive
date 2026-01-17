# 客户接口

## 客户列表

```
GET /api/client/list
```

查询参数：

| 参数     | 类型   | 说明     |
| -------- | ------ | -------- |
| pageNum  | number | 页码     |
| pageSize | number | 每页数量 |
| name     | string | 客户名称 |
| type     | string | 客户类型 |
| industry | string | 所属行业 |
| status   | string | 客户状态 |

## 客户详情

```
GET /api/client/{id}
```

## 创建客户

```
POST /api/client
```

请求示例：

```json
{
  "name": "某某科技有限公司",
  "type": "ENTERPRISE",
  "industry": "信息技术",
  "contactPerson": "张经理",
  "contactPhone": "13800138000",
  "contactEmail": "contact@example.com",
  "address": "北京市海淀区",
  "remark": "重点客户"
}
```

## 更新客户

```
PUT /api/client/{id}
```

## 删除客户

```
DELETE /api/client/{id}
```

## 客户类型

| 值          | 说明     |
| ----------- | -------- |
| INDIVIDUAL  | 个人     |
| ENTERPRISE  | 企业     |
| GOVERNMENT  | 政府     |
| INSTITUTION | 事业单位 |

## 客户状态

| 值        | 说明   |
| --------- | ------ |
| ACTIVE    | 活跃   |
| INACTIVE  | 不活跃 |
| BLACKLIST | 黑名单 |

## 关联项目列表

```
GET /api/client/{id}/matters
```

## 关联合同列表

```
GET /api/client/{id}/contracts
```

---

## 文档更新记录

| 更新时间   | 更新内容             | 操作人       |
| ---------- | -------------------- | ------------ |
| 2026-01-11 | 创建客户接口文档骨架 | AI Assistant |
