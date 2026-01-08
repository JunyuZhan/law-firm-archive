# 财务接口

## 收费管理

### 收费列表

```
GET /api/finance/fee/list
```

查询参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| pageNum | number | 页码 |
| pageSize | number | 每页数量 |
| feeNo | string | 收费编号 |
| clientId | number | 客户ID |
| matterId | number | 项目ID |
| status | string | 状态 |
| plannedDateFrom | string | 计划日期起 |
| plannedDateTo | string | 计划日期止 |

### 收费详情

```
GET /api/finance/fee/{id}
```

## 收款管理

### 创建收款

```
POST /api/finance/payment
```

请求：
```json
{
  "feeId": 1,
  "amount": 10000,
  "currency": "CNY",
  "paymentMethod": "BANK_TRANSFER",
  "paymentDate": "2026-01-15",
  "bankAccount": "工商银行",
  "transactionNo": "2026011500001",
  "remark": ""
}
```

### 确认收款

```
POST /api/finance/payment/{id}/confirm
```

确认收款后会自动触发提成计算。

## 提成管理

### 提成列表

```
GET /api/finance/commission/list
```

### 我的提成

```
GET /api/finance/commission/my
```

### 提成规则列表

```
GET /api/finance/commission-rule/list
```

### 创建提成规则

```
POST /api/finance/commission-rule
```

请求：
```json
{
  "name": "标准提成规则",
  "firmRate": 30,
  "originatorRate": 20,
  "isDefault": true
}
```

## 发票管理

### 发票列表

```
GET /api/finance/invoice/list
```

### 申请开票

```
POST /api/finance/invoice
```

请求：
```json
{
  "paymentId": 1,
  "invoiceType": "SPECIAL",
  "invoiceTitle": "XX公司",
  "taxNo": "91110000...",
  "amount": 10000
}
```

## 费用报销

### 报销列表

```
GET /api/finance/expense/list
```

### 提交报销

```
POST /api/finance/expense
```

请求：
```json
{
  "matterId": 1,
  "expenseType": "TRAVEL",
  "amount": 500,
  "description": "出差交通费",
  "attachments": []
}
```

## 收款方式

| 值 | 说明 |
|------|------|
| BANK_TRANSFER | 银行转账 |
| CASH | 现金 |
| CHECK | 支票 |
| OTHER | 其他 |

## 收费状态

| 值 | 说明 |
|------|------|
| PENDING | 待收款 |
| PARTIAL | 部分收款 |
| PAID | 已收款 |
