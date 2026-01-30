<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { MatterDTO } from '#/api/matter/types';

import { watch } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import { Modal, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMyMatters } from '#/api/matter';
import { useResponsive } from '#/hooks/useResponsive';

defineOptions({ name: 'MatterMy' });

const router = useRouter();

// 响应式布局
const { isMobile } = useResponsive();

// 获取当前年份
const currentYear = new Date().getFullYear();

// 年份选项（最近5年 + 全部）
const yearOptions = [
  { label: '全部年份', value: 0 },
  ...Array.from({ length: 5 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i,
  })),
];

// ==================== 常量选项 ====================

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已结案', value: 'CLOSED' },
  { label: '已暂停', value: 'SUSPENDED' },
];

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'year',
    label: '创建年份',
    component: 'Select',
    defaultValue: currentYear,
    componentProps: {
      placeholder: '请选择年份',
      options: yearOptions,
    },
  },
  {
    fieldName: 'name',
    label: '项目名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入项目名称',
      allowClear: true,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: statusOptions,
    },
  },
];

// ==================== 表格配置 ====================

// 响应式列配置
function getGridColumns() {
  const baseColumns = [
    { title: '类型', field: 'matterTypeName', width: 100, mobileShow: true },
    { title: '案件类型', field: 'caseTypeName', width: 100 },
    { title: '合同编号', field: 'contractNo', width: 130 },
    { title: '客户', field: 'clientName', width: 150, mobileShow: true },
    { title: '项目编号', field: 'matterNo', width: 130 },
    { title: '主办律师', field: 'leadLawyerName', width: 100 },
    {
      title: '合同金额',
      field: 'contractAmount',
      width: 120,
      slots: { default: 'contractAmount' },
    },
    { title: '创建时间', field: 'createdAt', width: 160 },
    {
      title: '状态',
      field: 'status',
      width: 100,
      slots: { default: 'status' },
      mobileShow: true,
    },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 100 : 200,
      fixed: 'right' as const,
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  // 移动端只显示标记的列
  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
}

// 加载数据
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  // 处理年份筛选参数
  let createdAtFrom: string | undefined;
  let createdAtTo: string | undefined;
  const year = params.year;
  // 只有当year有值且不为0时才设置时间范围
  if (year && year !== 0) {
    createdAtFrom = `${year}-01-01T00:00:00`;
    createdAtTo = `${year}-12-31T23:59:59`;
  }
  // year为undefined或0时，不设置时间范围，显示所有数据

  const res = await getMyMatters({
    pageNum: params.page,
    pageSize: params.pageSize,
    name: params.name,
    status: params.status,
    createdAtFrom,
    createdAtTo,
  });
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    submitButtonOptions: { content: '查询' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: getGridColumns(),
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page, form }: { form: any; page: any }) => {
          return await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...form,
          });
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
    },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

// ==================== 操作方法 ====================

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    IN_PROGRESS: 'blue',
    CLOSED: 'green',
    SUSPENDED: 'gray',
  };
  return colorMap[status] || 'default';
}

// 格式化金额
function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

// 查看详情
function handleView(row: MatterDTO) {
  router.push(`/matter/detail/${row.id}`);
}

// 编辑（跳转到项目列表页面进行编辑）
function handleEdit(row: MatterDTO) {
  router.push({ path: '/matter/list', query: { id: row.id } });
}

// 归档项目
async function handleArchive(row: MatterDTO) {
  // 检查项目状态是否允许归档
  if (row.status !== 'CLOSED' && row.status !== 'ARCHIVED') {
    Modal.confirm({
      title: '项目未结案',
      content: `项目 "${row.name}" 尚未结案，是否先申请结案？只有已结案的项目才能创建档案。`,
      okText: '前往结案',
      cancelText: '取消',
      onOk: () => {
        // 跳转到项目详情页
        router.push(`/matter/detail/${row.id}`);
      },
    });
    return;
  }

  // 已结案项目，跳转到档案列表页面并打开归档向导
  Modal.confirm({
    title: '创建档案',
    content: `确定要为项目 "${row.name}" 创建档案吗？系统将收集项目所有相关数据（合同、文档、审批记录等）。`,
    okText: '创建档案',
    cancelText: '取消',
    onOk: () => {
      // 跳转到档案列表页面，传递项目ID
      router.push({
        path: '/archive/list',
        query: { matterId: row.id },
      });
    },
  });
}
</script>

<template>
  <Page title="我的项目" description="查看我参与的项目">
    <Grid>
      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName }}
        </Tag>
      </template>

      <!-- 合同金额列 -->
      <template #contractAmount="{ row }">
        {{ formatMoney(row.contractAmount) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleView(row)">详情</a>
          <a
            v-if="!['ARCHIVED', 'CLOSED', 'PENDING_CLOSE'].includes(row.status)"
            @click="handleEdit(row)"
            >编辑</a
          >
          <a
            v-if="row.status !== 'ARCHIVED' && row.status !== 'CLOSED'"
            @click="handleArchive(row)"
            style="color: #722ed1"
          >
            归档
          </a>
        </Space>
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
/* 移动端适配 */
@media (max-width: 768px) {
  :deep(.vxe-table) {
    font-size: 13px;
  }

  :deep(.vxe-cell) {
    padding: 8px 4px;
  }

  :deep(.ant-space) {
    gap: 4px !important;
  }

  :deep(.ant-space-item a) {
    font-size: 12px;
  }
}
</style>
