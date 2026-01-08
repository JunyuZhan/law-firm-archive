# 认证接口

## 登录

```
POST /api/auth/login
```

请求：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

响应：
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

## 刷新 Token

```
POST /api/auth/refresh
```

请求：
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

## 登出

```
POST /api/auth/logout
```

## 获取当前用户信息

```
GET /api/auth/user-info
```

响应：
```json
{
  "id": 1,
  "username": "admin",
  "realName": "管理员",
  "avatar": "",
  "roles": ["ADMIN"],
  "permissions": ["*"]
}
```

## 获取用户菜单

```
GET /api/auth/menus
```

响应：
```json
[
  {
    "id": 1,
    "name": "工作台",
    "path": "/dashboard/workspace",
    "component": "dashboard/workspace/index",
    "icon": "DashboardOutlined",
    "children": []
  }
]
```
