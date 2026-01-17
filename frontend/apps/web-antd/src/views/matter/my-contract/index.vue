<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { ContractDTO } from '#/api/finance/types';

import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import { Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMyContracts } from '#/api/finance';

defineOptions({ name: 'MyContract' });

const router = useRouter();

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
  { label: '全部', value: undefined },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING' },
  { label: '生效中', value: 'ACTIVE' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已终止', value: 'TERMINATED' },
  { label: '已完成', value: 'COMPLETED' },
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
    fieldName: 'contractNo',
    label: '合同编号',
    component: 'Input',
    componentProps: {
      placeholder: '请输入合同编号',
      allowClear: true,
    },
  },
  {
    fieldName: 'name',
    label: '合同名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入合同名称',
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
      options: statusOptions.filter((o) => o.value !== undefined),
    },
  },
];

// ==================== 表格配置 ====================

const gridColumns = [
  { title: '合同编号', field: 'contractNo', width: 160 },
  { title: '合同名称', field: 'name', width: 200, showOverflow: true },
  { title: '客户', field: 'clientName', width: 120 },
  { title: '收费方式', field: 'feeTypeName', width: 100 },
  {
    title: '合同金额',
    field: 'totalAmount',
    width: 120,
    slots: { default: 'totalAmount' },
  },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '签约日期', field: 'signDate', width: 120 },
  {
    title: '操作',
    field: 'action',
    width: 100,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

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

  const res = await getMyContracts({
    pageNum: params.page,
    pageSize: params.pageSize,
    contractNo: params.contractNo,
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

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    submitButtonOptions: { content: '查询' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: gridColumns,
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

// ==================== 辅助方法 ====================

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PENDING: 'orange',
    ACTIVE: 'green',
    REJECTED: 'red',
    TERMINATED: 'gray',
    COMPLETED: 'blue',
  };
  return colorMap[status] || 'default';
}

function getStatusName(status: string) {
  const nameMap: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审批',
    ACTIVE: '生效中',
    REJECTED: '已拒绝',
    TERMINATED: '已终止',
    COMPLETED: '已完成',
  };
  return nameMap[status] || status;
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

function handleView(row: ContractDTO) {
  router.push(`/matter/contract?view=${row.id}`);
}
</script>

<template>
  <Page title="我的合同" description="查看我创建或签约的合同">
    <Grid>
      <!-- 合同金额列 -->
      <template #totalAmount="{ row }">
        {{ formatMoney(row.totalAmount) }}
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status || '')">
          {{ getStatusName(row.status || '') }}
        </Tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleView(row)">查看</a>
        </Space>
      </template>
    </Grid>
  </Page>
</template>
