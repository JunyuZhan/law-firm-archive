# 项目接口

## 项目列表

```
GET /api/matter/list
```

查询参数：

| 参数         | 类型   | 说明       |
| ------------ | ------ | ---------- |
| pageNum      | number | 页码       |
| pageSize     | number | 每页数量   |
| matterNo     | string | 项目编号   |
| name         | string | 项目名称   |
| matterType   | string | 项目类型   |
| clientId     | number | 客户ID     |
| status       | string | 状态       |
| leadLawyerId | number | 主办律师ID |

## 项目详情

```
GET /api/matter/{id}
```

## 创建项目

```
POST /api/matter
```

请求：

```json
{
  "name": "张三诉李四合同纠纷案",
  "matterType": "LITIGATION",
  "caseType": "CIVIL",
  "causeOfAction": "合同纠纷",
  "clientId": 1,
  "clients": [
    {
      "clientId": 1,
      "clientRole": "PLAINTIFF",
      "isPrimary": true
    }
  ],
  "opposingParty": "李四",
  "leadLawyerId": 2,
  "originatorId": 3,
  "departmentId": 1,
  "feeType": "FIXED",
  "estimatedFee": 50000,
  "contractId": 1
}
```

## 从合同创建项目

```
POST /api/matter/from-contract/{contractId}
```

## 更新项目

```
PUT /api/matter/{id}
```

## 修改项目状态

```
PUT /api/matter/{id}/status
```

请求：

```json
{
  "status": "CLOSED"
}
```

## 项目类型

| 值             | 说明     |
| -------------- | -------- |
| LITIGATION     | 诉讼业务 |
| NON_LITIGATION | 非诉业务 |
| COUNSEL        | 常年顾问 |

## 项目状态

| 值          | 说明   |
| ----------- | ------ |
| PENDING     | 待处理 |
| IN_PROGRESS | 进行中 |
| SUSPENDED   | 已暂停 |
| CLOSED      | 已结案 |
| ARCHIVED    | 已归档 |

## 任务管理

### 任务列表

```
GET /api/tasks
```

查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| pageNum | number | 页码 |
| pageSize | number | 每页数量 |
| matterId | number | 关联案件ID |
| assigneeId | number | 负责人ID |
| status | string | 状态 |
| priority | string | 优先级 |

### 创建任务

```
POST /api/tasks
```

请求：

```json
{
  "matterId": 1,
  "title": "起草起诉状",
  "description": "需要在本周五前完成",
  "assigneeId": 2,
  "priority": "HIGH",
  "dueDate": "2026-01-20"
}
```

### 更新任务

```
PUT /api/tasks/{id}
```

### 删除任务

```
DELETE /api/tasks/{id}
```

### 更新任务状态

```
PUT /api/tasks/{id}/status
```

## 期限提醒

### 期限列表

```
GET /api/matter/deadlines/list
```

### 创建期限

```
POST /api/matter/deadlines
```

请求：

```json
{
  "matterId": 1,
  "title": "举证期限",
  "deadlineTime": "2026-02-01 17:00:00",
  "reminderTime": "2026-01-30 09:00:00",
  "description": "注意提交证据原件"
}
```

### 自动创建期限

```
POST /api/matter/deadlines/auto-create/{matterId}
```

## 工时管理

### 工时列表

```
GET /api/timesheets
```

### 记录工时

```
POST /api/timesheets
```

请求：

```json
{
  "matterId": 1,
  "workDate": "2026-01-15",
  "hours": 2.5,
  "workType": "DRAFTING",
  "workContent": "起草起诉状初稿",
  "billable": true
}
```

### 提交工时

```
POST /api/timesheets/{id}/submit
```

### 批量提交工时

```
POST /api/timesheets/batch-submit
```

