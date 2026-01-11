# 合同接口

## 合同列表

```
GET /api/contract/list
```

查询参数：

| 参数       | 类型   | 说明     |
| ---------- | ------ | -------- |
| pageNum    | number | 页码     |
| pageSize   | number | 每页数量 |
| contractNo | string | 合同编号 |
| name       | string | 合同名称 |
| clientId   | number | 客户ID   |
| status     | string | 状态     |

## 已审批合同列表

```
GET /api/contract/approved
```

获取已审批通过的合同，用于创建项目时选择。

## 合同详情

```
GET /api/contract/{id}
```

## 创建合同

```
POST /api/contract
```

请求：

```json
{
  "name": "法律服务合同",
  "clientId": 1,
  "contractType": "SERVICE",
  "feeType": "FIXED",
  "totalAmount": 50000,
  "currency": "CNY",
  "signDate": "2026-01-01",
  "effectiveDate": "2026-01-01",
  "expiryDate": "2026-12-31",
  "signerId": 2,
  "departmentId": 1,
  "paymentTerms": "签约后3日内支付50%，结案后支付50%",
  "remark": ""
}
```

## 更新合同

```
PUT /api/contract/{id}
```

## 合同审批

```
POST /api/contract/{id}/approve
```

请求：

```json
{
  "approved": true,
  "comment": "同意"
}
```

## 合同类型

| 值         | 说明         |
| ---------- | ------------ |
| SERVICE    | 法律服务合同 |
| LITIGATION | 诉讼代理合同 |
| COUNSEL    | 法律顾问合同 |

## 收费方式

| 值          | 说明     |
| ----------- | -------- |
| FIXED       | 固定收费 |
| HOURLY      | 计时收费 |
| CONTINGENCY | 风险代理 |
| MIXED       | 混合收费 |

## 合同状态

| 值         | 说明   |
| ---------- | ------ |
| DRAFT      | 草稿   |
| PENDING    | 待审批 |
| APPROVED   | 已审批 |
| REJECTED   | 已拒绝 |
| ACTIVE     | 执行中 |
| COMPLETED  | 已完成 |
| TERMINATED | 已终止 |
