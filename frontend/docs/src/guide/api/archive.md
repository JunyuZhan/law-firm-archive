# 档案接口

档案管理接口。

## 档案列表

```
GET /api/archive/list
```

查询参数：

| 参数        | 类型   | 说明     |
| ----------- | ------ | -------- |
| pageNum     | number | 页码     |
| pageSize    | number | 每页数量 |
| archiveNo   | string | 档案编号 |
| matterId    | number | 项目ID   |
| archiveType | string | 档案类型 |
| status      | string | 档案状态 |
| keeperId    | number | 保管人ID |

## 档案详情

```
GET /api/archive/{id}
```

## 创建档案

```
POST /api/archive
```

请求示例：

```json
{
  "matterId": 1,
  "archiveType": "CASE_FILE",
  "title": "某某合同纠纷案卷宗",
  "volumeCount": 2,
  "pages": 150,
  "keeperId": 1,
  "location": "档案室A区3排2号",
  "description": "案件完整卷宗材料"
}
```

## 更新档案

```
PUT /api/archive/{id}
```

## 删除档案

```
DELETE /api/archive/{id}
```

## 档案类型

| 值            | 说明     |
| ------------- | -------- |
| CASE_FILE     | 案件卷宗 |
| CONTRACT_FILE | 合同档案 |
| FINANCE_FILE  | 财务档案 |
| ADMIN_FILE    | 行政档案 |
| OTHER         | 其他     |

## 档案状态

| 值           | 说明   |
| ------------ | ------ |
| IN_STORAGE   | 在库   |
| BORROWED     | 已借出 |
| UNDER_REVIEW | 查阅中 |
| TRANSFERRED  | 已移交 |
| DESTROYED    | 已销毁 |

## 借阅申请

```
POST /api/archive/borrow
```

请求示例：

```json
{
  "archiveId": 1,
  "borrowerId": 2,
  "borrowPurpose": "案件参考",
  "expectedReturnDate": "2026-01-20",
  "remark": "需要查阅相关案例材料"
}
```

## 借阅审批

```
POST /api/archive/borrow/{id}/approve
```

请求示例：

```json
{
  "approved": true,
  "remark": "同意借阅"
}
```

## 归还档案

```
POST /api/archive/borrow/{id}/return
```

## 销毁申请

```
POST /api/archive/destroy
```

请求示例：

```json
{
  "archiveId": 1,
  "destroyReason": "超过保管期限",
  "remark": "根据档案管理规定执行销毁"
}
```

## 销毁审批

```
POST /api/archive/destroy/{id}/approve
```

请求示例：

```json
{
  "approved": true,
  "remark": "同意销毁"
}
```

## 档案转移

```
POST /api/archive/transfer
```

请求示例：

```json
{
  "archiveId": 1,
  "newKeeperId": 3,
  "transferReason": "人员调动",
  "remark": "档案保管人变更"
}
```

## 借阅记录列表

```
GET /api/archive/borrow/list
```

查询参数：

| 参数       | 类型   | 说明     |
| ---------- | ------ | -------- |
| pageNum    | number | 页码     |
| pageSize   | number | 每页数量 |
| archiveId  | number | 档案ID   |
| borrowerId | number | 借阅人ID |
| status     | string | 借阅状态 |

## 销毁记录列表

```
GET /api/archive/destroy/list
```

---

## 文档更新记录

| 更新时间   | 更新内容             | 操作人       |
| ---------- | -------------------- | ------------ |
| 2026-01-11 | 创建档案接口文档骨架 | AI Assistant |
