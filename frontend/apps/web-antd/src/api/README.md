# API 开发规范

## 目录结构

```
src/api/
├── core/           # 核心API（认证、用户、菜单）
│   ├── auth.ts     # 认证相关
│   ├── user.ts     # 用户信息
│   ├── menu.ts     # 菜单
│   └── index.ts    # 导出
├── hr/             # 行政后勤模块
│   ├── attendance.ts
│   ├── leave.ts
│   ├── meeting-room.ts
│   ├── types.ts
│   └── index.ts
├── request.ts      # 请求客户端配置
└── index.ts        # 统一导出
```

## 开发规范

### 1. 导入路径

使用 `#/api/request` 而不是 `@/api/request`：

```typescript
// ✅ 正确
import { requestClient } from '#/api/request';

// ❌ 错误
import { requestClient } from '@/api/request';
```

### 2. API 路径

requestClient 的 baseURL 已经是 `/api`，所以 API 路径不需要再加 `/api` 前缀：

```typescript
// ✅ 正确
requestClient.get('/attendance/list');

// ❌ 错误
requestClient.get('/api/attendance/list');
```

### 3. 类型定义

每个模块的类型定义放在 `types.ts` 文件中：

```typescript
// types.ts
export interface AttendanceRecord {
  id: number;
  userId: number;
  // ...
}

// attendance.ts
import type { AttendanceRecord } from './types';
```

### 4. 后端响应格式

后端统一返回格式：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1234567890
}
```

requestClient 已配置自动解析，API 函数直接返回 `data` 字段内容。

### 5. 分页响应

分页接口返回格式：

```typescript
interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}
```

### 6. 命名规范

- 获取列表：`fetchXxxList`
- 获取详情：`getXxxDetail` 或 `getXxxById`
- 创建：`createXxx`
- 更新：`updateXxx`
- 删除：`deleteXxx`

## 后端 API 对接

后端 API 基础路径：`http://localhost:8080/api`

开发环境通过 Vite 代理转发，配置在 `vite.config.mts`：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}
```
