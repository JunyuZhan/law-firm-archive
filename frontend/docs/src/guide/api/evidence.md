# 证据接口

## 证据列表

```
GET /api/evidence
```

查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| pageNum | number | 页码 |
| pageSize | number | 每页数量 |
| matterId | number | 关联案件ID |
| evidenceName | string | 证据名称 |
| type | string | 证据类型 |
| status | string | 状态 |

## 证据详情

```
GET /api/evidence/{id}
```

## 创建证据

```
POST /api/evidence
```

请求（Multipart/form-data）：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| matterId | number | 案件ID |
| evidenceName | string | 证据名称 |
| type | string | 证据类型（DOCUMENT/AUDIO/VIDEO/IMAGE/PHYSICAL） |
| source | string | 证据来源 |
| collectedAt | string | 收集时间 |
| description | string | 描述 |
| file | file | 证据文件（可选） |

## 更新证据

```
PUT /api/evidence/{id}
```

## 删除证据

```
DELETE /api/evidence/{id}
```

## 质证记录

### 创建质证记录

```
POST /api/evidence/{id}/cross-exam
```

请求：

```json
{
  "round": 1,
  "questioner": "原告律师",
  "question": "证据的真实性有异议...",
  "answerer": "被告",
  "answer": "真实性无异议，但关联性..."
}
```

### 获取质证记录列表

```
GET /api/evidence/{id}/cross-exam
```
