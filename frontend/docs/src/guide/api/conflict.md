# 利冲接口

利益冲突（利冲）检查接口。

## 利冲检查列表

```
GET /api/conflict/list
```

查询参数：

| 参数       | 类型   | 说明         |
| ---------- | ------ | ------------ |
| pageNum    | number | 页码         |
| pageSize   | number | 每页数量     |
| type       | string | 检查类型     |
| status     | string | 检查状态     |
| clientName | string | 客户名称     |

## 利冲检查详情

```
GET /api/conflict/{id}
```

## 创建利冲检查

```
POST /api/conflict
```

请求示例：

```json
{
  "type": "NEW_CLIENT",
  "clientName": "某某公司",
  "opposingParty": "对方当事人",
  "projectName": "项目名称",
  "remark": "备注信息"
}
```

## 检查类型

| 值          | 说明     |
| ----------- | -------- |
| NEW_CLIENT  | 新客户   |
| NEW_PROJECT | 新项目   |

## 检查状态

| 值           | 说明         |
| ------------ | ------------ |
| PENDING      | 待检查       |
| CHECKING     | 检查中       |
| PASSED       | 已通过       |
| CONFLICT     | 存在冲突     |
| EXEMPTION    | 豁免待审批   |
| EXEMPTED     | 已豁免       |
| REJECTED     | 已拒绝       |

## 申请豁免

```
POST /api/conflict/{id}/exempt
```

请求示例：

```json
{
  "reason": "豁免理由说明"
}
```

## 审批豁免

```
POST /api/conflict/{id}/approve-exemption
```

请求示例：

```json
{
  "approved": true,
  "remark": "审批意见"
}
```

## 重新检查

```
POST /api/conflict/{id}/recheck
```

## 删除检查

```
DELETE /api/conflict/{id}
```

---

## 文档更新记录

| 更新时间 | 更新内容 | 操作人 |
|----------|----------|--------|
| 2026-01-11 | 创建利冲接口文档骨架 | AI Assistant |
