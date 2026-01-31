# 移动端适配改造计划

## 一、现状分析

### 1.1 框架层面（已有的移动端支持）

Vben Admin 框架本身保留了基础的移动端支持：

| 功能 | 位置 | 状态 |
|------|------|------|
| 移动端检测 | `packages/@core/composables/src/use-is-mobile.ts` | ✅ 可用 |
| 偏好设置中的 isMobile | `packages/@core/preferences/src/preferences.ts` | ✅ 可用 |
| 布局自动切换 | `packages/@core/ui-kit/layout-ui/src/hooks/use-layout.ts` | ✅ 可用 |
| 侧边栏自动折叠 | `packages/@core/ui-kit/layout-ui/src/vben-layout.vue` | ✅ 可用 |
| 移动端遮罩层 | `packages/@core/ui-kit/layout-ui/src/vben-layout.vue` | ✅ 可用 |
| Modal/Drawer 移动端适配 | `packages/@core/ui-kit/popup-ui/` | ✅ 部分可用 |

### 1.2 业务页面层面（严重缺失）

经检查，`apps/web-antd/src/views/` 下的所有业务页面存在以下问题：

| 问题 | 严重程度 | 影响范围 |
|------|----------|----------|
| 完全没有使用 `useIsMobile` | 🔴 严重 | 100+ 个页面 |
| 表格列使用固定像素宽度 | 🔴 严重 | 所有列表页面 |
| 使用 VxeGrid 但无移动端适配 | 🔴 严重 | 50+ 个表格页面 |
| Row/Col 栅格没有响应式断点 | 🟡 中等 | 大部分表单页面 |
| Modal 没有移动端宽度优化 | 🟡 中等 | 所有弹窗 |
| 没有使用 Tailwind 响应式前缀 | 🟡 中等 | 几乎所有页面 |
| 按钮/操作区没有移动端适配 | 🟡 中等 | 所有列表页面 |

### 1.3 统计数据

```
业务页面总数：约 110+ 个 Vue 文件
使用 useIsMobile 的页面：0 个
使用响应式断点（sm:/md:/lg:）的文件：仅 3 个，且使用量极少
```

---

## 二、改造目标

### 2.1 核心目标

1. **保证基础可用性**：所有页面在移动端可以正常查看和操作
2. **重点页面优化**：高频使用页面（工作台、审批、项目列表等）提供良好的移动端体验
3. **渐进式改造**：按优先级分批实施，不影响现有功能

### 2.2 适配标准

| 屏幕宽度 | 设备类型 | 适配要求 |
|----------|----------|----------|
| < 640px (sm) | 手机竖屏 | 完全适配，核心功能可用 |
| 640-768px (md) | 手机横屏/小平板 | 完全适配 |
| 768-1024px (lg) | 平板 | 完全适配 |
| > 1024px | 桌面 | 现有体验（无需修改） |

---

## 三、技术方案

### 3.1 通用工具封装

#### 3.1.1 创建移动端响应式组合函数

```typescript
// src/hooks/useResponsive.ts
import { useIsMobile } from '@vben-core/composables';
import { breakpointsTailwind, useBreakpoints } from '@vueuse/core';
import { computed } from 'vue';

export function useResponsive() {
  const { isMobile } = useIsMobile();
  const breakpoints = useBreakpoints(breakpointsTailwind);
  
  const isXs = breakpoints.smaller('sm');  // < 640px
  const isSm = breakpoints.between('sm', 'md'); // 640-768px
  const isMd = breakpoints.between('md', 'lg'); // 768-1024px
  const isLg = breakpoints.between('lg', 'xl'); // 1024-1280px
  const isXl = breakpoints.greaterOrEqual('xl'); // >= 1280px
  
  const isTablet = breakpoints.between('md', 'lg');
  const isDesktop = breakpoints.greaterOrEqual('lg');
  
  // 表格列数建议
  const suggestedColumns = computed(() => {
    if (isXs.value) return 1;
    if (isSm.value) return 2;
    if (isMd.value) return 3;
    return 4;
  });
  
  // Modal 宽度建议
  const modalWidth = computed(() => {
    if (isMobile.value) return '100%';
    if (isTablet.value) return '80%';
    return 720;
  });
  
  return {
    isMobile,
    isXs,
    isSm,
    isMd,
    isLg,
    isXl,
    isTablet,
    isDesktop,
    suggestedColumns,
    modalWidth,
  };
}
```

#### 3.1.2 创建响应式表格适配器

```typescript
// src/adapter/responsive-table.ts
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useResponsive } from '#/hooks/useResponsive';
import { computed } from 'vue';

export function useResponsiveGrid(baseColumns: VxeGridProps['columns']) {
  const { isMobile, isTablet } = useResponsive();
  
  // 移动端优先显示的列（通过 mobileShow 标记）
  const responsiveColumns = computed(() => {
    if (!isMobile.value) return baseColumns;
    
    return baseColumns?.filter(col => {
      // 保留必要列：序号、名称、状态、操作
      if (col.type === 'seq' || col.type === 'checkbox') return true;
      if (col.field === 'name' || col.field === 'status') return true;
      if (col.slots?.default === 'action') return true;
      // 通过自定义属性标记移动端显示
      return (col as any).mobileShow === true;
    });
  });
  
  // 移动端表格配置
  const mobileGridProps = computed<Partial<VxeGridProps>>(() => {
    if (!isMobile.value) return {};
    
    return {
      height: 'auto',
      showOverflow: true,
      columnConfig: {
        resizable: false,
      },
    };
  });
  
  return {
    responsiveColumns,
    mobileGridProps,
  };
}
```

### 3.2 组件级适配方案

#### 3.2.1 表格页面适配

**方案A：响应式列显隐**
```vue
<script setup lang="ts">
import { useResponsive } from '#/hooks/useResponsive';

const { isMobile } = useResponsive();

const columns = computed(() => {
  const baseColumns = [
    { title: '名称', field: 'name', minWidth: 150 },
    { title: '客户', field: 'clientName', width: 120 },
    { title: '状态', field: 'status', width: 100 },
    // 移动端隐藏以下列
    ...(!isMobile.value ? [
      { title: '创建时间', field: 'createdAt', width: 150 },
      { title: '负责人', field: 'ownerName', width: 100 },
    ] : []),
    { title: '操作', field: 'action', width: isMobile.value ? 80 : 150 },
  ];
  return baseColumns;
});
</script>
```

**方案B：移动端卡片视图（推荐高频页面使用）**
```vue
<template>
  <!-- 桌面端：表格视图 -->
  <VxeGrid v-if="!isMobile" :columns="columns" :data="data" />
  
  <!-- 移动端：卡片视图 -->
  <div v-else class="space-y-3 p-3">
    <Card v-for="item in data" :key="item.id" size="small">
      <div class="flex items-center justify-between">
        <span class="font-medium">{{ item.name }}</span>
        <Tag :color="getStatusColor(item.status)">{{ item.statusName }}</Tag>
      </div>
      <div class="mt-2 text-sm text-gray-500">
        <div>客户：{{ item.clientName }}</div>
        <div>创建：{{ formatDate(item.createdAt) }}</div>
      </div>
      <div class="mt-3 flex justify-end space-x-2">
        <Button size="small" @click="handleView(item)">查看</Button>
        <Button size="small" type="primary" @click="handleEdit(item)">编辑</Button>
      </div>
    </Card>
  </div>
</template>
```

#### 3.2.2 表单弹窗适配

```vue
<script setup lang="ts">
import { useResponsive } from '#/hooks/useResponsive';

const { isMobile, modalWidth } = useResponsive();

// 表单栅格配置
const formColSpan = computed(() => ({
  xs: 24,
  sm: 24,
  md: 12,
  lg: 8,
  xl: 8,
}));
</script>

<template>
  <Modal
    :width="modalWidth"
    :centered="isMobile"
    :footer="isMobile ? null : undefined"
  >
    <Form :layout="isMobile ? 'vertical' : 'horizontal'">
      <Row :gutter="16">
        <Col v-bind="formColSpan">
          <FormItem label="名称">
            <Input v-model:value="form.name" />
          </FormItem>
        </Col>
        <!-- 更多表单项 -->
      </Row>
    </Form>
    
    <!-- 移动端底部固定按钮 -->
    <div v-if="isMobile" class="fixed bottom-0 left-0 right-0 bg-white p-3 shadow-lg">
      <Space class="w-full" direction="vertical">
        <Button type="primary" block @click="handleSubmit">提交</Button>
        <Button block @click="handleCancel">取消</Button>
      </Space>
    </div>
  </Modal>
</template>
```

#### 3.2.3 搜索栏适配

```vue
<template>
  <!-- 桌面端：展开式搜索 -->
  <div v-if="!isMobile" class="flex flex-wrap gap-4 mb-4">
    <Input v-model:value="query.name" placeholder="名称" style="width: 200px" />
    <Select v-model:value="query.status" style="width: 150px" />
    <Button type="primary" @click="handleSearch">搜索</Button>
    <Button @click="handleReset">重置</Button>
  </div>
  
  <!-- 移动端：抽屉式搜索 -->
  <div v-else>
    <div class="flex items-center justify-between p-3 bg-gray-50">
      <Input v-model:value="query.keyword" placeholder="搜索..." class="flex-1 mr-2" />
      <Button type="link" @click="showFilter = true">
        <FilterOutlined /> 筛选
      </Button>
    </div>
    
    <Drawer
      v-model:open="showFilter"
      title="筛选条件"
      placement="bottom"
      height="60%"
    >
      <Form layout="vertical">
        <FormItem label="状态">
          <Select v-model:value="query.status" />
        </FormItem>
        <!-- 更多筛选项 -->
      </Form>
      <div class="absolute bottom-0 left-0 right-0 p-4 bg-white border-t">
        <Space class="w-full">
          <Button block @click="handleReset">重置</Button>
          <Button type="primary" block @click="handleSearch">应用</Button>
        </Space>
      </div>
    </Drawer>
  </div>
</template>
```

### 3.3 全局样式增强

```css
/* src/styles/mobile.css */

/* 移动端表格优化 */
@media (max-width: 768px) {
  .vxe-table {
    font-size: 13px;
  }
  
  .vxe-table .vxe-cell {
    padding: 8px 4px;
  }
  
  /* 操作按钮优化 */
  .vxe-table .ant-btn {
    padding: 0 8px;
    font-size: 12px;
  }
  
  /* 表头优化 */
  .vxe-table .vxe-header--column {
    white-space: nowrap;
  }
}

/* 移动端弹窗优化 */
@media (max-width: 768px) {
  .ant-modal {
    max-width: 100vw !important;
    margin: 0 !important;
    top: 0 !important;
  }
  
  .ant-modal-content {
    border-radius: 0;
    min-height: 100vh;
  }
  
  .ant-modal-body {
    padding: 16px;
    padding-bottom: 80px; /* 为底部按钮留空间 */
  }
}

/* 移动端卡片优化 */
@media (max-width: 768px) {
  .ant-card {
    border-radius: 8px;
  }
  
  .ant-card-body {
    padding: 12px;
  }
}
```

---

## 四、改造优先级

### P0 - 高优先级（第一阶段，1-2周）

必须改造的核心页面，影响日常高频使用：

| 页面 | 路径 | 改造方案 |
|------|------|----------|
| 工作台 | `dashboard/workspace` | 响应式栅格 + 卡片适配 |
| 审批中心 | `dashboard/approval` | 卡片视图 + 操作优化 |
| 我的项目 | `matter/my` | 卡片视图 |
| 日程管理 | `workbench/schedule` | 日历组件适配 |
| 个人信息 | `_core/profile` | 表单适配 |
| 登录页 | `_core/authentication/login` | 已适配，需检查 |

### P1 - 中优先级（第二阶段，2-3周）

重要业务页面：

| 页面 | 路径 | 改造方案 |
|------|------|----------|
| 项目列表 | `matter/list` | 响应式列 + 可选卡片 |
| 客户列表 | `crm/client` | 响应式列 + 可选卡片 |
| 合同管理 | `matter/contract` | 响应式列 |
| 财务收款 | `finance/payment` | 响应式列 |
| 我的工时 | `matter/timesheet` | 表单适配 |
| 请假申请 | `admin/leave` | 表单适配 |

### P2 - 低优先级（第三阶段，3-4周）

管理类页面，移动端使用频率低：

| 页面 | 路径 | 改造方案 |
|------|------|----------|
| 用户管理 | `system/user` | 基础响应式 |
| 角色管理 | `system/role` | 基础响应式 |
| 菜单管理 | `system/menu` | 树形适配 |
| 字典管理 | `system/dict` | 基础响应式 |
| 部门管理 | `system/dept` | 树形适配 |
| 各类报表 | `finance/report` 等 | 图表响应式 |

### P3 - 可选改造

复杂度高或使用频率极低的页面，可考虑仅做基础适配：

- 档案管理模块
- 知识库模块
- 系统配置模块
- 数据迁移模块

---

## 五、实施步骤

### 阶段一：基础设施（3天）✅ 已完成

1. [x] 创建 `src/hooks/useResponsive.ts` 响应式工具函数 ✅
2. [x] 创建 `src/adapter/responsive-table.ts` 表格适配器 ✅
3. [x] 创建 `src/styles/mobile.css` 全局移动端样式 ✅
4. [x] 在 `bootstrap.ts` 中引入移动端样式 ✅
5. [x] 封装移动端卡片列表组件 `MobileCardList.vue` ✅

### 阶段二：P0 页面改造（1周）✅ 已完成

1. [x] 改造工作台页面 `dashboard/workspace/index.vue` ✅
2. [x] 改造审批中心页面 `dashboard/approval/index.vue` ✅
3. [x] 改造我的项目页面 `matter/my/index.vue` ✅
4. [x] 改造日程管理页面 `workbench/schedule/index.vue` ✅
5. [x] 检查并优化个人信息页面 ✅（框架组件已有移动端支持）

### 阶段三：P1 页面改造（2周）✅ 已完成

1. [x] 改造项目列表页面 `matter/list/index.vue` ✅
2. [x] 改造客户列表页面 `crm/client/index.vue` ✅
3. [x] 改造合同管理页面 `matter/contract/index.vue` ✅
4. [x] 改造财务相关页面 ✅
   - [x] 收款管理 `finance/payment/index.vue` ✅
   - [x] 发票管理 `finance/invoice/index.vue` ✅
   - [x] 报销管理 `finance/expense/index.vue` ✅
5. [x] 改造请假/工时管理页面 ✅
   - [x] 请假申请 `hr/leave/index.vue` ✅
   - [x] 工时管理 `matter/timesheet/index.vue` ✅

### 阶段四：P2/P3 页面及测试（2周）✅ 已完成

1. [x] 改造系统管理页面 ✅
   - [x] 用户管理 `system/user/index.vue` ✅
   - [x] 角色管理 `system/role/index.vue` ✅
   - [x] 部门管理 `system/dept/index.vue` ✅
   - [x] 字典管理 `system/dict/index.vue` ✅
2. [x] 改造其他低频页面 ✅
   - [x] 知识库-文章 `knowledge/article/index.vue` ✅
   - [x] 知识库-案例 `knowledge/case/index.vue` ✅
   - [x] 知识库-法规 `knowledge/law/index.vue` ✅
   - [x] 档案借阅 `archive/borrow/index.vue` ✅
3. [ ] 全面测试各页面移动端表现
4. [ ] 性能优化
5. [ ] 文档更新

---

## 六、测试方案

### 6.1 测试设备/视口

| 设备类型 | 视口尺寸 | 测试重点 |
|----------|----------|----------|
| iPhone SE | 375×667 | 小屏手机 |
| iPhone 14 | 390×844 | 主流手机 |
| iPhone 14 Pro Max | 430×932 | 大屏手机 |
| iPad | 768×1024 | 平板竖屏 |
| iPad 横屏 | 1024×768 | 平板横屏 |

### 6.2 测试要点

1. **布局测试**
   - [ ] 页面无水平滚动条
   - [ ] 内容不溢出屏幕
   - [ ] 表格可水平滚动
   - [ ] 弹窗全屏显示

2. **交互测试**
   - [ ] 按钮可点击，点击区域足够大（至少 44×44px）
   - [ ] 表单输入正常
   - [ ] 下拉选择正常
   - [ ] 手势操作正常（滑动、缩放）

3. **功能测试**
   - [ ] 核心业务流程可完成
   - [ ] 数据加载正常
   - [ ] 错误提示可见

---

## 七、注意事项

### 7.1 兼容性考虑

- 保持桌面端现有体验不变
- 移动端适配采用渐进增强策略
- 使用 `useIsMobile` 而非 CSS 媒体查询控制核心逻辑

### 7.2 性能考虑

- 移动端减少不必要的数据加载（如减少表格列）
- 图片使用响应式尺寸
- 避免移动端加载不使用的大型组件

### 7.3 用户体验原则

- 触控友好：按钮、链接点击区域足够大
- 减少输入：尽量使用选择代替输入
- 简化流程：移动端可适当简化非核心功能
- 快速反馈：操作后及时给予视觉反馈

---

## 八、参考资源

- [Tailwind CSS 响应式设计](https://tailwindcss.com/docs/responsive-design)
- [Ant Design Vue 响应式栅格](https://antdv.com/components/grid-cn)
- [VxeTable 移动端适配](https://vxetable.cn/#/table/other/mobileAdapt)
- [Vben Admin 官方文档](https://doc.vben.pro/)

---

## 九、更新记录

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本，完成现状分析和改造计划 |
| 2026-01-15 | v1.1 | 完成阶段一基础设施建设，完成P0页面部分改造（工作台、审批中心、我的项目） |
| 2026-01-15 | v1.2 | 完成P0全部页面改造（日程管理、个人信息），完成P1部分页面改造（项目列表、客户列表、合同管理） |
| 2026-01-15 | v1.3 | 完成P1全部页面改造（财务收款、发票、报销、请假申请、工时管理） |
| 2026-01-15 | v1.4 | 完成P2/P3页面改造（系统管理、知识库、档案管理），移动端适配基本完成 |
| 2026-01-15 | v1.5 | 修复 VxeGrid columns prop 类型错误：将 computed 响应式列配置改为函数+watch 模式，解决 "Expected Array, got Object" 警告 |
| 2026-01-15 | v1.6 | 修复 matter/timesheet 缺少 computed 导入错误；为项目详情页添加移动端适配（Modal 响应式宽度） |
| 2026-01-15 | v1.7 | 修复 finance/expense 缺少 computed 导入错误；修复合同管理统计卡片移动端响应式布局 |