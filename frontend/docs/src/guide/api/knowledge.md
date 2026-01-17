# 知识库接口

知识库管理接口。

## 文章管理

### 文章列表

```
GET /api/knowledge/article/list
```

查询参数：

| 参数       | 类型   | 说明     |
| ---------- | ------ | -------- |
| pageNum    | number | 页码     |
| pageSize   | number | 每页数量 |
| title      | string | 文章标题 |
| categoryId | number | 分类ID   |
| authorId   | number | 作者ID   |
| status     | string | 文章状态 |
| tags       | string | 标签     |

### 创建文章

```
POST /api/knowledge/article
```

请求示例：

```json
{
  "title": "民事诉讼证据规则解读",
  "content": "文章内容...",
  "summary": "摘要",
  "categoryId": 1,
  "tags": "民事,证据,规则",
  "status": "PUBLISHED",
  "isTop": false
}
```

### 文章详情

```
GET /api/knowledge/article/{id}
```

### 更新文章

```
PUT /api/knowledge/article/{id}
```

### 删除文章

```
DELETE /api/knowledge/article/{id}
```

### 文章状态

| 值        | 说明   |
| --------- | ------ |
| DRAFT     | 草稿   |
| PUBLISHED | 已发布 |
| REVIEWING | 审核中 |
| REJECTED  | 已驳回 |

## 文章分类

### 分类列表

```
GET /api/knowledge/category/list
```

### 创建分类

```
POST /api/knowledge/category
```

请求示例：

```json
{
  "name": "民事法律",
  "parentId": 0,
  "orderNum": 1,
  "description": "民事相关法律知识"
}
```

### 更新分类

```
PUT /api/knowledge/category/{id}
```

### 删除分类

```
DELETE /api/knowledge/category/{id}
```

## 案例库

### 案例列表

```
GET /api/knowledge/case/list
```

查询参数：

| 参数     | 类型   | 说明     |
| -------- | ------ | -------- |
| pageNum  | number | 页码     |
| pageSize | number | 每页数量 |
| title    | string | 案例标题 |
| caseType | string | 案例类型 |
| court    | string | 法院     |
| tags     | string | 标签     |

### 创建案例

```
POST /api/knowledge/case
```

请求示例：

```json
{
  "title": "张三诉李四合同纠纷案",
  "caseNo": "（2025）京0101民初1234号",
  "caseType": "CIVIL",
  "causeOfAction": "合同纠纷",
  "court": "北京市东城区人民法院",
  "judgmentDate": "2025-06-15",
  "content": "案例详情...",
  "summary": "本案涉及...",
  "tags": "合同,纠纷,违约",
  "status": "PUBLISHED"
}
```

### 案例详情

```
GET /api/knowledge/case/{id}
```

### 案例类型

| 值             | 说明 |
| -------------- | ---- |
| CIVIL          | 民事 |
| CRIMINAL       | 刑事 |
| ADMINISTRATIVE | 行政 |
| COMMERCIAL     | 商事 |

## 法规库

### 法规列表

```
GET /api/knowledge/law/list
```

查询参数：

| 参数       | 类型   | 说明     |
| ---------- | ------ | -------- |
| pageNum    | number | 页码     |
| pageSize   | number | 每页数量 |
| title      | string | 法规标题 |
| issuingOrg | string | 发布机关 |
| lawType    | string | 法规类型 |
| effectDate | string | 生效日期 |

### 创建法规

```
POST /api/knowledge/law
```

请求示例：

```json
{
  "title": "中华人民共和国民法典",
  "lawNo": "中华人民共和国主席令第四十五号",
  "issuingOrg": "全国人民代表大会",
  "lawType": "LAW",
  "effectDate": "2021-01-01",
  "content": "法规全文...",
  "summary": "民法典是...",
  "status": "PUBLISHED"
}
```

### 法规详情

```
GET /api/knowledge/law/{id}
```

### 法规类型

| 值               | 说明       |
| ---------------- | ---------- |
| CONSTITUTION     | 宪法       |
| LAW              | 法律       |
| REGULATION       | 行政法规   |
| LOCAL_REGULATION | 地方性法规 |
| RULE             | 部门规章   |

## 知识标签

### 标签列表

```
GET /api/knowledge/tag/list
```

### 创建标签

```
POST /api/knowledge/tag
```

请求示例：

```json
{
  "name": "合同",
  "color": "#1890ff",
  "description": "合同相关"
}
```

### 热门标签

```
GET /api/knowledge/tag/hot
```

参数：

| 参数  | 类型   | 说明     |
| ----- | ------ | -------- |
| limit | number | 返回数量 |

## 搜索

### 知识库搜索

```
GET /api/knowledge/search
```

参数：

| 参数       | 类型   | 说明                     |
| ---------- | ------ | ------------------------ |
| keyword    | string | 关键词                   |
| type       | string | 类型（article/case/law） |
| categoryId | number | 分类ID                   |
| pageNum    | number | 页码                     |
| pageSize   | number | 每页数量                 |

---

## 文档更新记录

| 更新时间   | 更新内容               | 操作人       |
| ---------- | ---------------------- | ------------ |
| 2026-01-11 | 创建知识库接口文档骨架 | AI Assistant |
