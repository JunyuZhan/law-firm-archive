# 工作台接口

工作台相关接口，包括报表中心、审批中心等。

## 报表中心

### 可用报表列表

```
GET /api/workbench/report/available
```

### 报表生成记录列表

```
GET /api/workbench/report
```

查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| pageNum | number | 页码 |
| pageSize | number | 每页数量 |
| reportType | string | 报表类型 |
| status | string | 状态 |

### 同步生成报表

适用于小型报表。

```
POST /api/workbench/report/generate
```

请求：

```json
{
  "reportType": "CASE_SUMMARY",
  "parameters": {
    "startDate": "2026-01-01",
    "endDate": "2026-01-31"
  },
  "format": "EXCEL"
}
```

### 异步提交报表任务

适用于大型报表。

```
POST /api/workbench/report/submit
```

### 查询报表生成状态

```
GET /api/workbench/report/status/{id}
```

### 获取下载链接

```
GET /api/workbench/report/{id}/download-url
```

### 删除报表记录

```
DELETE /api/workbench/report/{id}
```

## 统计分析

### 首页统计数据

```
GET /api/workbench/statistics/dashboard
```

### 个人业绩统计

```
GET /api/workbench/statistics/performance/personal
```
