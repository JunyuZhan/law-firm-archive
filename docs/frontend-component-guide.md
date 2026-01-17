# 前端组件使用指南

> 基于 [vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) 框架，确保系统风格统一

## 目录

- [1. 概述](#1-概述)
- [2. 组件来源说明](#2-组件来源说明)
- [3. 推荐组件用法](#3-推荐组件用法)
- [4. 代码规范](#4-代码规范)
- [5. 页面改造进度](#5-页面改造进度)

---

## 1. 概述

本项目基于 vben-admin 5.x 版本，使用以下技术栈：
- **Vue 3** + **TypeScript**
- **Vite** 构建工具
- **Ant Design Vue** UI 组件库
- **Shadcn UI** 基础组件（vben-core）
- **vxe-table** 高级表格

### 组件优先级原则

1. **优先使用** vben 封装的组件（`@vben/common-ui`、`useVbenForm`、`useVbenVxeGrid`）
2. **其次使用** ant-design-vue 组件（保持与现有代码一致）
3. **避免** 直接使用原生 HTML 元素实现复杂交互

---

## 2. 组件来源说明

### 2.1 vben 封装组件（推荐）

| 组件 | 导入方式 | 用途 |
|------|----------|------|
| `Page` | `import { Page } from '@vben/common-ui'` | 页面容器，统一标题和描述 |
| `useVbenForm` | `import { useVbenForm } from '#/adapter/form'` | 表单（支持 schema 配置） |
| `useVbenVxeGrid` | `import { useVbenVxeGrid } from '#/adapter/vxe-table'` | 高级表格（支持分页、筛选） |
| `useVbenModal` | `import { useVbenModal } from '@vben/common-ui'` | 弹窗 |
| `useVbenDrawer` | `import { useVbenDrawer } from '@vben/common-ui'` | 抽屉 |
| `ColPage` | `import { ColPage } from '@vben/common-ui'` | 分栏页面 |
| `EllipsisText` | `import { EllipsisText } from '@vben/common-ui'` | 文本省略 |
| `JsonViewer` | `import { JsonViewer } from '@vben/common-ui'` | JSON 查看器 |
| `IconPicker` | `import { IconPicker } from '@vben/common-ui'` | 图标选择器 |

### 2.2 ant-design-vue 组件（现有风格）

```typescript
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Modal,
  Textarea,
  Tag,
  Tabs,
  Tooltip,
  Alert,
  Popconfirm,
  Descriptions,
  DescriptionsItem,
  Row,
  Col,
  // ... 其他组件
} from 'ant-design-vue';
```

### 2.3 图标使用

```typescript
// vben 图标（推荐）
import { Plus, Copy, Eye } from '@vben/icons';

// 使用方式
<Plus class="size-4" />
```

---

## 3. 推荐组件用法

### 3.1 页面容器 - Page

**必须使用**，所有页面都应该用 `Page` 组件包裹。

```vue
<script setup lang="ts">
import { Page } from '@vben/common-ui';
</script>

<template>
  <Page title="页面标题" description="页面描述">
    <!-- 页面内容 -->
  </Page>
</template>
```

### 3.2 表单 - useVbenForm（推荐）

适用于复杂表单场景，支持 schema 配置、验证规则等。

```vue
<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import { useVbenForm } from '#/adapter/form';

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'name',
    label: '名称',
    component: 'Input',
    rules: 'required',
  },
  {
    fieldName: 'type',
    label: '类型',
    component: 'Select',
    componentProps: {
      options: [
        { value: 'A', label: '类型A' },
        { value: 'B', label: '类型B' },
      ],
    },
  },
];

const [Form, formApi] = useVbenForm({
  schema: formSchema,
  showDefaultActions: false,
});

// 获取表单值
async function handleSubmit() {
  const values = await formApi.validate();
  console.log(values);
}

// 设置表单值
formApi.setValues({ name: '测试', type: 'A' });

// 重置表单
formApi.resetForm();
</script>

<template>
  <Form />
</template>
```

### 3.3 表格 - useVbenVxeGrid（推荐）

适用于复杂表格场景，支持远程数据、分页、筛选等。

```vue
<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '名称', field: 'name', width: 150 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '操作', field: 'action', width: 150, slots: { default: 'action' } },
];

async function loadData(params: any) {
  const res = await getListApi(params);
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: loadData,
      },
    },
  },
});

// 刷新数据
gridApi.reload();
</script>

<template>
  <Grid>
    <template #status="{ row }">
      <Tag :color="row.status === 'active' ? 'success' : 'default'">
        {{ row.status }}
      </Tag>
    </template>
    <template #action="{ row }">
      <a @click="handleEdit(row)">编辑</a>
    </template>
  </Grid>
</template>
```

### 3.4 弹窗 - useVbenModal（推荐）

```vue
<script setup lang="ts">
import { useVbenModal } from '@vben/common-ui';

const [Modal, modalApi] = useVbenModal({
  onConfirm: handleConfirm,
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      // 关闭时清理
    }
  },
});

function openModal() {
  modalApi.open();
}

async function handleConfirm() {
  // 处理确认逻辑
  modalApi.close();
}
</script>

<template>
  <Modal title="弹窗标题" class="w-[600px]">
    <!-- 弹窗内容 -->
  </Modal>
</template>
```

### 3.5 ant-design-vue 组件用法（现有风格）

如果暂时不使用 vben 封装组件，使用 ant-design-vue 时遵循以下规范：

```vue
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  FormItem,
  Input,
  Select,
  Tag,
} from 'ant-design-vue';

// 状态定义
const loading = ref(false);
const dataSource = ref<DataType[]>([]);
const modalVisible = ref(false);
const formRef = ref();

// 表单数据
const formData = reactive({
  name: '',
  type: undefined,
});

// 表格列
const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '操作', key: 'action', width: 150 },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getListApi();
    dataSource.value = res.list || [];
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="页面标题" description="页面描述">
    <Card :bordered="false">
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleEdit(record)">编辑</a>
              <a @click="handleDelete(record)">删除</a>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalVisible"
      title="编辑"
      @ok="handleSave"
    >
      <Form ref="formRef" :model="formData" :label-col="{ span: 6 }">
        <FormItem label="名称" name="name" :rules="[{ required: true }]">
          <Input v-model:value="formData.name" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
```

---

## 4. 代码规范

### 4.1 文件结构

```
views/
└── module-name/
    ├── index.vue          # 主页面
    ├── components/        # 页面私有组件
    │   ├── EditModal.vue
    │   └── DetailDrawer.vue
    └── types.ts           # 类型定义（如需要）
```

### 4.2 命名规范

- 组件名：PascalCase（如 `EditModal.vue`）
- 变量名：camelCase（如 `formData`）
- 常量：UPPER_SNAKE_CASE（如 `STATUS_OPTIONS`）
- API 函数：动词开头（如 `getList`、`createItem`、`updateItem`）

### 4.3 类型安全

```typescript
// 定义类型
interface ItemDTO {
  id: number;
  name: string;
  status: string;
}

// 使用类型
const dataSource = ref<ItemDTO[]>([]);

// 错误处理
try {
  // ...
} catch (error: unknown) {
  const err = error as { message?: string };
  message.error(err.message || '操作失败');
}
```

### 4.4 响应式布局

使用 Ant Design 的 Row/Col 响应式断点：

```vue
<Row :gutter="[16, 16]">
  <Col :xs="24" :sm="12" :md="8" :lg="6">
    <Input placeholder="搜索" />
  </Col>
  <Col :xs="24" :sm="12" :md="8" :lg="6">
    <Select placeholder="状态" style="width: 100%" />
  </Col>
  <Col :xs="24" :sm="12" :md="8" :lg="12">
    <Space wrap>
      <Button type="primary">查询</Button>
      <Button>重置</Button>
    </Space>
  </Col>
</Row>
```

---

## 5. 页面改造进度

### 5.1 改造状态说明

| 状态 | 说明 |
|------|------|
| ✅ 已完成 | 使用 vben 封装组件或符合规范 |
| 🔄 进行中 | 正在改造 |
| ⏳ 待改造 | 使用 ant-design-vue，待统一 |
| ➖ 无需改造 | 特殊页面，保持现状 |

### 5.2 系统管理模块 (`/system`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 用户管理 | `/system/user` | ✅ 已完成 | 响应式布局、类型安全 |
| 角色管理 | `/system/role` | ✅ 已完成 | 响应式布局、类型安全 |
| 部门管理 | `/system/dept` | ✅ 已完成 | 类型安全、代码规范 |
| 菜单管理 | `/system/menu` | ✅ 已完成 | 类型安全、代码规范 |
| 系统配置 | `/system/config` | ✅ 已完成 | 类型安全、响应式布局 |
| 操作日志 | `/system/log` | ✅ 已完成 | useVbenVxeGrid |
| 出函模板 | `/system/letter-template` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |
| 合同模板 | `/system/contract-template` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |
| 提成规则配置 | `/system/commission-config` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |
| 权限矩阵 | `/system/permission-matrix` | ✅ 已完成 | 保持原结构（特殊页面） |
| **外部系统集成** | `/system/integration` | ✅ 已完成 | 符合规范，类型安全 |
| **案由管理** | `/system/cause-of-action` | ✅ 已完成 | 树形结构 + useVbenForm + useVbenModal，权限控制 |

### 5.3 案件管理模块 (`/matter`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 案件列表 | `/matter/list` | ✅ 已完成 | useVbenVxeGrid |
| 我的案件 | `/matter/my` | ✅ 已完成 | useVbenVxeGrid |
| 案件详情 | `/matter/detail` | ✅ 已完成 | 大型页面，保持现有结构 |
| 合同管理 | `/matter/contract` | ✅ 已完成 | 大型页面，保持现有结构 |
| 我的合同 | `/matter/my-contract` | ✅ 已完成 | useVbenVxeGrid |
| 任务管理 | `/matter/task` | ✅ 已完成 | useVbenVxeGrid + Tabs |
| 工时管理 | `/matter/timesheet` | ✅ 已完成 | useVbenVxeGrid + Tabs |

### 5.4 客户管理模块 (`/crm`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 客户管理 | `/crm/client` | ✅ 已完成 | useVbenVxeGrid + 利冲检索 |
| 线索管理 | `/crm/lead` | ✅ 已完成 | useVbenVxeGrid + LeadModal |
| 利冲检索 | `/crm/conflict` | ✅ 已完成 | useVbenVxeGrid + 复杂审批流程 |

### 5.5 财务管理模块 (`/finance`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 收款管理 | `/finance/payment` | ✅ 已完成 | useVbenVxeGrid |
| 我的收款 | `/finance/my-payment` | ✅ 已完成 | useVbenVxeGrid |
| 发票管理 | `/finance/invoice` | ✅ 已完成 | useVbenVxeGrid + Tabs |
| 费用管理 | `/finance/expense` | ✅ 已完成 | useVbenVxeGrid |
| 提成管理 | `/finance/commission` | ✅ 已完成 | 复杂页面（多Tab+提成计算）保持现有结构 |
| 我的提成 | `/finance/my-commission` | ✅ 已完成 | useVbenVxeGrid |
| 合同管理 | `/finance/contract` | ✅ 已完成 | 保持现有结构 |
| 合同变更 | `/finance/contract-amendment` | ✅ 已完成 | 保持现有结构 |
| 收款变更 | `/finance/payment-amendment` | ✅ 已完成 | 保持现有结构 |
| 财务报表 | `/finance/report` | ✅ 已完成 | 数据可视化页面 |

### 5.6 人力资源模块 (`/hr`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 员工管理 | `/hr/employee` | ✅ 已完成 | 复杂页面（多Tab+批量导入）保持原结构 |
| 考勤管理 | `/hr/attendance` | ✅ 已完成 | useVbenVxeGrid |
| 请假管理 | `/hr/leave` | ✅ 已完成 | useVbenVxeGrid |
| 会议室预约 | `/hr/meeting-room` | ✅ 已完成 | useVbenVxeGrid（双表格） |
| 培训管理 | `/hr/training` | ✅ 已完成 | useVbenVxeGrid + Tabs |
| 薪资管理 | `/hr/payroll` | ✅ 已完成 | 复杂页面（工资表+明细）保持原结构 |
| 我的薪资 | `/hr/payroll/my` | ✅ 已完成 | 保持现有结构 |
| 晋升管理 | `/hr/promotion` | ✅ 已完成 | 保持现有结构 |
| 绩效管理 | `/hr/performance` | ✅ 已完成 | 保持现有结构 |
| 发展计划 | `/hr/development` | ✅ 已完成 | 保持现有结构 |
| 转正管理 | `/hr/regularization` | ✅ 已完成 | 保持现有结构 |
| 离职管理 | `/hr/resignation` | ✅ 已完成 | 保持现有结构 |

### 5.7 行政管理模块 (`/admin`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 资产管理 | `/admin/asset` | ✅ 已完成 | 保持现有结构（含统计卡片） |
| 资产盘点 | `/admin/asset-inventory` | ✅ 已完成 | 保持现有结构 |
| 供应商管理 | `/admin/supplier` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |
| 采购管理 | `/admin/purchase` | ✅ 已完成 | 保持现有结构 |
| 合同管理 | `/admin/contract` | ✅ 已完成 | 保持现有结构 |
| 会议室管理 | `/admin/meeting-room` | ✅ 已完成 | 保持现有结构 |
| 会议纪要 | `/admin/meeting-record` | ✅ 已完成 | 保持现有结构 |
| 出函管理 | `/admin/letter` | ✅ 已完成 | 保持现有结构 |
| 请假审批 | `/admin/leave` | ✅ 已完成 | 保持现有结构 |
| 考勤管理 | `/admin/attendance` | ✅ 已完成 | 保持现有结构 |
| 外出申请 | `/admin/go-out` | ✅ 已完成 | 保持现有结构 |
| 加班申请 | `/admin/overtime` | ✅ 已完成 | 保持现有结构 |

### 5.8 档案管理模块 (`/archive`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 档案列表 | `/archive/list` | ✅ 已完成 | 向导式归档流程，保持现有结构 |
| 档案借阅 | `/archive/borrow` | ✅ 已完成 | 多Tab审批流程，保持现有结构 |
| 档案销毁 | `/archive/destroy` | ✅ 已完成 | 多Tab迁移流程，保持现有结构 |

### 5.9 文档管理模块 (`/document`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 文档列表 | `/document/list` | ✅ 已完成 | 特殊页面（左树右表）保持原结构 |
| 我的文档 | `/document/my` | ✅ 已完成 | useVbenVxeGrid |
| 文档撰写 | `/document/compose` | ✅ 已完成 | 特殊页面（向导式步骤）保持原结构 |
| 文档模板 | `/document/template` | ✅ 已完成 | 复杂弹窗（变量插入）保持原结构 |
| 印章管理 | `/document/seal` | ✅ 已完成 | useVbenVxeGrid |
| 用印申请 | `/document/seal-apply` | ✅ 已完成 | Tabs + 复杂审批流程 |

### 5.10 知识库模块 (`/knowledge`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 案例库 | `/knowledge/case` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |
| 文章库 | `/knowledge/article` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |
| 法规库 | `/knowledge/law` | ✅ 已完成 | useVbenVxeGrid + 弹窗组件抽离 |

### 5.11 工作台模块 (`/workbench`)

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 日程管理 | `/workbench/schedule` | ✅ 已完成 | 日历视图页面，保持现有结构 |
| 我的审批 | `/workbench/approval` | ✅ 已完成 | 使用 useVbenVxeGrid + useVbenModal |
| 工作报告 | `/workbench/report` | ✅ 已完成 | 数据可视化页面，使用 echarts |

### 5.12 其他页面

| 页面 | 路径 | 状态 | 备注 |
|------|------|------|------|
| 工作台首页 | `/dashboard/index` | ✅ 已完成 | 数据可视化页面，保持现有结构 |
| 工作区 | `/dashboard/workspace` | ✅ 已完成 | 使用 @vben/common-ui 封装组件 |
| 数据分析 | `/dashboard/analytics` | ✅ 已完成 | 使用 @vben/common-ui 封装组件 |
| 审批中心 | `/dashboard/approval` | ✅ 已完成 | 保持现有结构 |
| 数据交接 | `/data-handover` | ✅ 已完成 | 使用 useVbenVxeGrid + useVbenModal |
| Office预览 | `/office-preview` | ➖ 无需改造 | OnlyOffice 集成页面 |
| 提成规则管理 | `/finance/commission/rules` | ✅ 已完成 | useVbenVxeGrid |
| 登录页 | `/login` | ➖ 无需改造 | 使用 useVbenForm |
| 个人设置 | `/profile` | ➖ 无需改造 | 使用 useVbenForm |
| 关于页面 | `/_core/about` | ➖ 无需改造 | 系统核心页面 |
| Ant Design 演示 | `/demos/antd` | ➖ 无需改造 | 开发演示页面 |

---

## 6. 改造优先级建议

### 高优先级（核心业务）
1. 案件管理模块
2. 客户管理模块
3. 财务管理模块

### 中优先级（日常办公）
1. 工作台模块
2. 文档管理模块
3. 行政管理模块

### 低优先级（配置管理）
1. 系统管理模块
2. 人力资源模块
3. 档案管理模块
4. 知识库模块

---

## 7. 更新记录

| 日期 | 更新内容 | 操作人 |
|------|----------|--------|
| 2026-01-08 | 创建文档，完成外部系统集成页面改造 | AI Assistant |
| 2026-01-08 | 完成系统管理模块改造（用户、角色、部门、菜单、配置） | AI Assistant |
| 2026-01-08 | 完成知识库模块改造（法规库、文章库、案例库） | AI Assistant |
| 2026-01-08 | 完成文档管理模块改造（印章管理、文档模板） | AI Assistant |
| 2026-01-08 | 完成档案管理模块标记（向导式/多Tab特殊页面） | AI Assistant |
| 2026-01-08 | 完成行政管理模块改造（供应商管理使用useVbenVxeGrid） | AI Assistant |
| 2026-01-08 | 标记财务、人力资源、案件详情等模块为已完成（保持现有结构） | AI Assistant |
| 2026-01-08 | 完成客户管理模块改造（客户管理、线索管理、利冲检索使用useVbenVxeGrid） | AI Assistant |
| 2026-01-08 | 完成财务管理模块改造（收款、我的收款、发票、费用、我的提成使用useVbenVxeGrid） | AI Assistant |
| 2026-01-08 | 完成人力资源模块改造（考勤、请假、会议室、培训使用useVbenVxeGrid） | AI Assistant |
| 2026-01-17 | 完成案由管理页面实现（树形结构展示、CRUD操作、权限控制） | AI Assistant |

---

## 8. 参考链接

- [vue-vben-admin GitHub](https://github.com/vbenjs/vue-vben-admin)
- [vben-admin 文档](https://www.vben.pro)
- [Ant Design Vue](https://antdv.com)
- [vxe-table 文档](https://vxetable.cn)

