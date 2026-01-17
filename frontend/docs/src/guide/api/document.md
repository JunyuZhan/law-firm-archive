# 文档接口

文档管理接口。

## 文档列表

```
GET /api/document/list
```

查询参数：

| 参数       | 类型   | 说明     |
| ---------- | ------ | -------- |
| pageNum    | number | 页码     |
| pageSize   | number | 每页数量 |
| matterId   | number | 项目ID   |
| categoryId | number | 分类ID   |
| fileName   | string | 文件名称 |
| uploaderId | number | 上传人ID |
| status     | string | 文档状态 |

## 文档详情

```
GET /api/document/{id}
```

## 上传文档

```
POST /api/document/upload
```

注意：此接口需要使用 `multipart/form-data` 格式上传文件。

参数：

| 参数        | 类型   | 说明             |
| ----------- | ------ | ---------------- |
| file        | file   | 文件             |
| matterId    | number | 项目ID           |
| categoryId  | number | 分类ID           |
| description | string | 文件描述         |
| tags        | string | 标签（逗号分隔） |

## 更新文档信息

```
PUT /api/document/{id}
```

请求示例：

```json
{
  "fileName": "新文件名称",
  "description": "文件描述",
  "categoryId": 2,
  "tags": "合同,重要"
}
```

## 删除文档

```
DELETE /api/document/{id}
```

## 下载文档

```
GET /api/document/{id}/download
```

## 文档预览

```
GET /api/document/{id}/preview
```

## 文档分类树

```
GET /api/document/category/tree
```

## 创建文档分类

```
POST /api/document/category
```

请求示例：

```json
{
  "name": "合同文件",
  "parentId": 0,
  "orderNum": 1
}
```

## 更新文档分类

```
PUT /api/document/category/{id}
```

## 删除文档分类

```
DELETE /api/document/category/{id}
```

## 文档状态

| 值        | 说明   |
| --------- | ------ |
| DRAFT     | 草稿   |
| PUBLISHED | 已发布 |
| ARCHIVED  | 已归档 |

## 文档模板列表

```
GET /api/document/template
```

## 文档模板详情

```
GET /api/document/template/{id}
```

## 创建文档模板

```
POST /api/document/template
```

请求示例：

```json
{
  "name": "委托代理合同模板",
  "content": "模板内容...",
  "variables": "clientName,contractAmount",
  "category": "CONTRACT"
}
```

## 使用模板生成文档

```
POST /api/document/template/{id}/generate
```

请求示例：

```json
{
  "variables": {
    "clientName": "某某公司",
    "contractAmount": "50000"
  },
  "matterId": 1
}
```

---

## 文档更新记录

| 更新时间   | 更新内容             | 操作人       |
| ---------- | -------------------- | ------------ |
| 2026-01-11 | 创建文档接口文档骨架 | AI Assistant |
