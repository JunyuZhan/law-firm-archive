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
