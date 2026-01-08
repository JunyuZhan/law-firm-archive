# 智慧律所管理系统 - 前端模块对接指南

## 一、项目架构概述

本项目基于 **Vben Admin 5.x** (monorepo 结构)，主应用位于 `frontend/apps/web-antd`。

### 核心特性
- **国际化 (i18n)**: 支持中英文切换，语言包位于 `src/locales/langs/`
- **权限控制**: 支持前端/后端两种权限模式，基于权限码 (accessCodes) 控制
- **动态菜单**: 从后端 `/system/menu/user` 获取用户菜单

---

## 二、国际化配置

### 2.1 语言包结构
```
src/locales/langs/
├── zh-CN/
│   ├── page.json      # 页面相关翻译
│   └── demos.json     # 示例翻译
└── en-US/
    ├── page.json
    └── demos.json
```

### 2.2 添加新模块翻译

1. 在 `zh-CN/page.json` 添加中文：
```json
{
  "client": {
    "title": "客户管理",
    "list": "客户列表",
    "add": "新增客户",
    "edit": "编辑客户",
    "detail": "客户详情"
  }
}
```

2. 在 `en-US/page.json` 添加英文：
```json
{
  "client": {
    "title": "Client Management",
    "list": "Client List",
    "add": "Add Client",
    "edit": "Edit Client",
    "detail": "Client Detail"
  }
}
```

### 2.3 使用翻译
```typescript
import { $t } from '#/locales';

// 在模板中
<span>{{ $t('page.client.title') }}</span>

// 在路由中
meta: {
  title: $t('page.client.title'),
}
```

---

## 三、权限控制

### 3.1 权限模式

Vben Admin 支持两种权限模式（在 `preferences.ts` 中配置）：

- **frontend**: 前端控制，基于角色 (roles) 过滤路由
- **backend**: 后端控制，菜单完全由后端返回（推荐）

当前系统使用 **backend** 模式。

### 3.2 权限码 (accessCodes)

登录时后端返回用户权限码列表，存储在 `accessStore.accessCodes` 中。

后端权限码格式：`模块:资源:操作`
```
sys:user:list      - 用户列表
sys:user:create    - 创建用户
sys:user:update    - 更新用户
sys:user:delete    - 删除用户
client:list        - 客户列表
matter:create      - 创建项目
```

### 3.3 权限指令

在组件中使用 `v-access` 指令控制元素显示：

```vue
<!-- 基于权限码控制 -->
<a-button v-access:code="'sys:user:create'">新增用户</a-button>
<a-button v-access:code="['sys:user:update', 'sys:user:delete']">批量操作</a-button>

<!-- 基于角色控制（仅 frontend 模式有效） -->
<a-button v-access:role="'ADMIN'">管理员操作</a-button>
```

### 3.4 编程式权限判断

```typescript
import { useAccess } from '@vben/access';

const { hasAccessByCodes, hasAccessByRoles } = useAccess();

// 判断是否有权限
if (hasAccessByCodes(['sys:user:create'])) {
  // 有创建用户权限
}
```

---

## 四、路由配置

### 4.1 静态路由（前端定义）

位于 `src/router/routes/modules/` 目录，用于开发调试或固定页面。

```typescript
// src/router/routes/modules/client.ts
import type { RouteRecordRaw } from 'vue-router';
import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:users',
      order: 3,
      title: $t('page.client.title'),
    },
    name: 'Client',
    path: '/client',
    children: [
      {
        name: 'ClientList',
        path: '/client/list',
        component: () => import('#/views/client/list/index.vue'),
        meta: {
          icon: 'lucide:list',
          title: $t('page.client.list'),
          // 权限码，用于前端模式过滤
          authority: ['client:list'],
        },
      },
    ],
  },
];

export default routes;
```

### 4.2 动态路由（后端返回）

后端 `/system/menu/user` 返回菜单数据，前端自动转换为路由。

后端菜单格式：
```json
{
  "id": 1,
  "parentId": 0,
  "name": "客户管理",
  "path": "/client",
  "component": "LAYOUT",
  "icon": "TeamOutlined",
  "menuType": "DIRECTORY",
  "permission": null,
  "sortOrder": 3,
  "visible": true,
  "children": [
    {
      "id": 2,
      "parentId": 1,
      "name": "客户列表",
      "path": "/client/list",
      "component": "client/list/index",
      "permission": "client:list",
      "menuType": "MENU"
    }
  ]
}
```

前端转换逻辑在 `src/api/core/menu.ts`：
- `component: "LAYOUT"` → 布局组件
- `component: "client/list/index"` → 自动映射到 `src/views/client/list/index.vue`

---

## 五、API 开发规范

### 5.1 目录结构
```
src/api/
├── core/           # 核心API（认证、菜单）
│   ├── auth.ts
│   ├── menu.ts
│   └── index.ts
├── client/         # 客户模块
│   ├── index.ts
│   └── types.ts
├── matter/         # 项目模块
├── finance/        # 财务模块
└── request.ts      # 请求配置
```

### 5.2 API 文件模板

```typescript
// src/api/client/types.ts
export interface ClientDTO {
  id: number;
  name: string;
  clientType: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  status: string;
  createTime: string;
}

export interface ClientQuery {
  name?: string;
  clientType?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateClientCommand {
  name: string;
  clientType: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
}
```

```typescript
// src/api/client/index.ts
import { requestClient } from '#/api/request';
import type { ClientDTO, ClientQuery, CreateClientCommand } from './types';

/** 获取客户列表 */
export function getClientList(params: ClientQuery) {
  return requestClient.get<{ list: ClientDTO[]; total: number }>('/crm/client', { params });
}

/** 获取客户详情 */
export function getClientDetail(id: number) {
  return requestClient.get<ClientDTO>(`/crm/client/${id}`);
}

/** 创建客户 */
export function createClient(data: CreateClientCommand) {
  return requestClient.post<ClientDTO>('/crm/client', data);
}

/** 更新客户 */
export function updateClient(id: number, data: Partial<CreateClientCommand>) {
  return requestClient.put<ClientDTO>(`/crm/client/${id}`, data);
}

/** 删除客户 */
export function deleteClient(id: number) {
  return requestClient.delete(`/crm/client/${id}`);
}
```

### 5.3 请求响应格式

后端统一响应格式：
```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": { ... }
}
```

前端 `request.ts` 已配置响应拦截器，自动提取 `data` 字段。

---

## 六、页面开发规范

### 6.1 目录结构
```
src/views/
├── _core/              # 核心页面（登录、404等）
├── dashboard/          # 工作台
├── client/             # 客户模块
│   ├── list/
│   │   └── index.vue
│   ├── detail/
│   │   └── index.vue
│   └── components/     # 模块内共享组件
└── matter/             # 项目模块
```

### 6.2 列表页面模板

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { getClientList, deleteClient } from '#/api/client';
import type { ClientDTO, ClientQuery } from '#/api/client/types';

const loading = ref(false);
const dataSource = ref<ClientDTO[]>([]);
const total = ref(0);
const queryParams = ref<ClientQuery>({
  pageNum: 1,
  pageSize: 10,
});

const columns = [
  { title: '客户名称', dataIndex: 'name', key: 'name' },
  { title: '客户类型', dataIndex: 'clientType', key: 'clientType' },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 200 },
];

async function fetchData() {
  loading.value = true;
  try {
    const { list, total: t } = await getClientList(queryParams.value);
    dataSource.value = list;
    total.value = t;
  } finally {
    loading.value = false;
  }
}

async function handleDelete(id: number) {
  await deleteClient(id);
  message.success('删除成功');
  fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="p-4">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form layout="inline">
        <a-form-item label="客户名称">
          <a-input v-model:value="queryParams.name" placeholder="请输入" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="fetchData">查询</a-button>
          <a-button class="ml-2" @click="queryParams = { pageNum: 1, pageSize: 10 }">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮 -->
    <div class="mb-4">
      <a-button v-access:code="'client:create'" type="primary">
        新增客户
      </a-button>
    </div>

    <!-- 数据表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="{
        current: queryParams.pageNum,
        pageSize: queryParams.pageSize,
        total,
        onChange: (page, size) => {
          queryParams.pageNum = page;
          queryParams.pageSize = size;
          fetchData();
        },
      }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button v-access:code="'client:update'" type="link" size="small">
            编辑
          </a-button>
          <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
            <a-button v-access:code="'client:delete'" type="link" size="small" danger>
              删除
            </a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>
```

---

## 七、模块对接清单

根据系统功能清单，需要对接以下模块：

| 模块 | 路由前缀 | API前缀 | 状态 |
|------|---------|---------|------|
| 工作台 | /dashboard | /workbench | 待开发 |
| 系统管理 | /system | /system | 待开发 |
| 客户管理 | /crm | /crm | 待开发 |
| 项目管理 | /matter | /matter | 待开发 |
| 财务管理 | /finance | /finance | 待开发 |
| 文书管理 | /document | /document | 待开发 |
| 证据管理 | /evidence | /evidence | 待开发 |
| 档案管理 | /archive | /archive | 待开发 |
| 行政管理 | /admin | /admin | 部分完成 |
| 人力资源 | /hr | /hr | 待开发 |
| 知识库 | /knowledge | /knowledge | 待开发 |

---

## 八、开发流程

1. **创建 API 文件**: `src/api/{module}/index.ts` 和 `types.ts`
2. **创建页面组件**: `src/views/{module}/{page}/index.vue`
3. **添加国际化**: `src/locales/langs/zh-CN/page.json`
4. **配置路由**（可选）: `src/router/routes/modules/{module}.ts`
5. **后端配置菜单**: 在数据库 `sys_menu` 表添加菜单记录

---

## 九、注意事项

1. **路径别名**: 使用 `#/` 代替 `@/`，如 `#/api`、`#/views`
2. **API 路径**: 不要加 `/api` 前缀，代理已配置
3. **组件导入**: 使用动态导入 `() => import('#/views/...')`
4. **权限码**: 与后端 `@RequirePermission` 注解保持一致
5. **菜单图标**: 使用 Ant Design 图标名或 Lucide 图标（如 `lucide:users`）
