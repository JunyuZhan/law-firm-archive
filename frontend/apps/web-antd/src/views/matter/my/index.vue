<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Space, Tag } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import type { VbenFormSchema } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMyMatters } from '#/api/matter';
import type { MatterDTO } from '#/api/matter/types';

defineOptions({ name: 'MatterMy' });

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

const gridColumns = [
  { title: '项目编号', field: 'matterNo', width: 130 },
  { title: '项目名称', field: 'name', width: 200, showOverflow: true },
  { title: '类型', field: 'matterTypeName', width: 100 },
  { title: '客户', field: 'clientName', width: 150 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '预估费用', field: 'estimatedFee', width: 120, slots: { default: 'estimatedFee' } },
  { title: '创建时间', field: 'createdAt', width: 180 },
  { title: '操作', field: 'action', width: 100, fixed: 'right' as const, slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: { page: number; pageSize: number } & Record<string, any>) {
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
        query: async ({ page, form }: { page: any; form: any }) => {
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
</script>

<template>
  <Page title="我的项目" description="查看我参与的项目" auto-content-height>
    <Grid>
      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName }}
        </Tag>
      </template>

      <!-- 预估费用列 -->
      <template #estimatedFee="{ row }">
        {{ formatMoney(row.estimatedFee) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleView(row)">查看详情</a>
        </Space>
      </template>
    </Grid>
  </Page>
</template>
