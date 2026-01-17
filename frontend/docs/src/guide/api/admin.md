# 行政接口

行政管理接口。

## 资产管理

### 资产列表

```
GET /api/admin/asset/list
```

查询参数：

| 参数      | 类型   | 说明     |
| --------- | ------ | -------- |
| pageNum   | number | 页码     |
| pageSize  | number | 每页数量 |
| assetNo   | string | 资产编号 |
| assetName | string | 资产名称 |
| category  | string | 资产类别 |
| status    | string | 资产状态 |
| keeperId  | number | 保管人ID |

### 创建资产

```
POST /api/admin/asset
```

请求示例：

```json
{
  "assetName": "笔记本电脑",
  "category": "ELECTRONIC",
  "assetNo": "ASSET202601001",
  "brand": "联想",
  "model": "ThinkPad X1",
  "purchaseDate": "2026-01-01",
  "purchasePrice": 8999.0,
  "keeperId": 1,
  "location": "财务部",
  "remark": "财务专用"
}
```

### 资产详情

```
GET /api/admin/asset/{id}
```

### 更新资产

```
PUT /api/admin/asset/{id}
```

### 删除资产

```
DELETE /api/admin/asset/{id}
```

## 资产类别

| 值         | 说明     |
| ---------- | -------- |
| ELECTRONIC | 电子产品 |
| FURNITURE  | 办公家具 |
| VEHICLE    | 车辆     |
| EQUIPMENT  | 设备     |
| OTHER      | 其他     |

## 资产状态

| 值        | 说明   |
| --------- | ------ |
| NORMAL    | 正常   |
| IN_USE    | 使用中 |
| REPAIRING | 维修中 |
| SCRAPPED  | 已报废 |
| LOST      | 已丢失 |

## 资产盘点

### 创建盘点任务

```
POST /api/admin/asset/inventory
```

请求示例：

```json
{
  "title": "2026年第一季度资产盘点",
  "description": "季度例行资产盘点",
  "startDate": "2026-03-01",
  "endDate": "2026-03-10"
}
```

### 盘点任务列表

```
GET /api/admin/asset/inventory/list
```

### 提交盘点结果

```
POST /api/admin/asset/inventory/{id}/submit
```

请求示例：

```json
{
  "results": [
    {
      "assetId": 1,
      "status": "NORMAL",
      "remark": "设备完好"
    }
  ]
}
```

## 会议室管理

### 会议室列表

```
GET /api/admin/meeting-room/list
```

### 创建会议室

```
POST /api/admin/meeting-room
```

请求示例：

```json
{
  "name": "第一会议室",
  "capacity": 20,
  "equipment": "投影仪,白板,音响",
  "location": "3楼301",
  "description": "大型会议室"
}
```

### 会议室预约

```
POST /api/admin/meeting-room/{id}/reserve
```

请求示例：

```json
{
  "title": "项目评审会议",
  "startTime": "2026-01-12 14:00:00",
  "endTime": "2026-01-12 16:00:00",
  "organizerId": 1,
  "participants": [2, 3, 4],
  "remark": "准备投影材料"
}
```

### 预约列表

```
GET /api/admin/meeting-room/reservation/list
```

查询参数：

| 参数      | 类型   | 说明     |
| --------- | ------ | -------- |
| roomId    | number | 会议室ID |
| startDate | string | 开始日期 |
| endDate   | string | 结束日期 |
| status    | string | 预约状态 |

## 供应商管理

### 供应商列表

```
GET /api/admin/supplier/list
```

### 创建供应商

```
POST /api/admin/supplier
```

请求示例：

```json
{
  "name": "某某办公用品公司",
  "type": "OFFICE_SUPPLIES",
  "contactPerson": "王经理",
  "contactPhone": "13800138000",
  "address": "北京市朝阳区",
  "bankAccount": "中国银行xxxxxxxx",
  "taxNumber": "91110108xxxxxxxxx",
  "remark": "长期合作供应商"
}
```

### 供应商类型

| 值              | 说明     |
| --------------- | -------- |
| OFFICE_SUPPLIES | 办公用品 |
| EQUIPMENT       | 设备     |
| SERVICE         | 服务     |
| OTHER           | 其他     |

## 采购管理

### 采购申请

```
POST /api/admin/purchase/apply
```

请求示例：

```json
{
  "title": "采购办公用品",
  "items": [
    {
      "itemName": "A4打印纸",
      "quantity": 10,
      "unit": "箱",
      "estimatedPrice": 180.0
    }
  ],
  "totalAmount": 1800.0,
  "applicantId": 1,
  "reason": "办公室日常使用"
}
```

### 采购审批

```
POST /api/admin/purchase/{id}/approve
```

请求示例：

```json
{
  "approved": true,
  "remark": "同意采购"
}
```

### 采购订单列表

```
GET /api/admin/purchase/list
```

---

## 文档更新记录

| 更新时间   | 更新内容             | 操作人       |
| ---------- | -------------------- | ------------ |
| 2026-01-11 | 创建行政接口文档骨架 | AI Assistant |
