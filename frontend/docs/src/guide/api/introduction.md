# API 文档

本文档描述系统后端 API 接口规范。

## 基础信息

- 基础路径：`/api`
- 认证方式：Bearer Token
- 数据格式：JSON

## 认证

除登录接口外，所有接口需要在请求头携带 Token：

```
Authorization: Bearer <access_token>
```

## 响应格式

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1234567890
}
```

## 分页响应

```json
{
  "list": [...],
  "total": 100,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 10
}
```

## 错误码

| 错误码 | 说明         |
| ------ | ------------ |
| 200    | 成功         |
| 400    | 请求参数错误 |
| 401    | 未认证       |
| 403    | 无权限       |
| 404    | 资源不存在   |
| 500    | 服务器错误   |

## API 模块

| 模块   | 路径前缀       | 说明                   |
| ------ | -------------- | ---------------------- |
| 认证   | /api/auth      | 登录、登出、刷新Token  |
| 系统   | /api/system    | 用户、角色、部门、菜单 |
| 客户   | /api/client    | 客户管理               |
| 利冲   | /api/conflict  | 利益冲突审查           |
| 案源   | /api/lead      | 案源管理               |
| 项目   | /api/matter    | 项目管理               |
| 合同   | /api/contract  | 合同管理               |
| 财务   | /api/finance   | 收款、提成、发票       |
| 文档   | /api/document  | 文档管理               |
| 档案   | /api/archive   | 档案管理               |
| 行政   | /api/admin     | 行政管理               |
| 人力   | /api/hr        | 人力资源               |
| 知识库 | /api/knowledge | 知识库                 |

## 接口详情

- [认证接口](/guide/api/auth)
- [用户接口](/guide/api/user)
- [项目接口](/guide/api/case)
- [合同接口](/guide/api/contract)
- [财务接口](/guide/api/finance)
- [客户接口](/guide/api/client)
- [利冲接口](/guide/api/conflict)
- [案源接口](/guide/api/lead)
- [文档接口](/guide/api/document)
- [档案接口](/guide/api/archive)
- [行政接口](/guide/api/admin)
- [人力接口](/guide/api/hr)
- [知识库接口](/guide/api/knowledge)

## 文档更新记录

| 更新时间 | 更新内容 | 操作人 |
|----------|----------|--------|
| 2026-01-11 | 更新API文档结构，补充缺失接口链接 | AI Assistant |
