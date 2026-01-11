# 用户接口

## 用户列表

```
GET /api/system/user/list
```

查询参数：

| 参数     | 类型   | 说明     |
| -------- | ------ | -------- |
| pageNum  | number | 页码     |
| pageSize | number | 每页数量 |
| username | string | 用户名   |
| realName | string | 姓名     |
| status   | string | 状态     |
| deptId   | number | 部门ID   |

## 用户详情

```
GET /api/system/user/{id}
```

## 创建用户

```
POST /api/system/user
```

请求：

```json
{
  "username": "zhangsan",
  "password": "123456",
  "realName": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "deptId": 1,
  "roleIds": [6]
}
```

## 更新用户

```
PUT /api/system/user/{id}
```

## 删除用户

```
DELETE /api/system/user/{id}
```

## 重置密码

```
POST /api/system/user/{id}/reset-password
```

## 修改状态

```
PUT /api/system/user/{id}/status
```

请求：

```json
{
  "status": "DISABLED"
}
```
