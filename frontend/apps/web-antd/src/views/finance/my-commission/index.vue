<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Card,
  Col,
  Empty,
  message,
  Row,
  Select,
  Statistic,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { requestClient } from '#/api/request';

defineOptions({ name: 'MyCommission' });

interface MyCommission {
  id: number;
  contractNo: string;
  contractName: string;
  clientName: string;
  paymentAmount: number;
  commissionRate: number;
  commissionAmount: number;
  status: string;
  statusName: string;
  calculatedAt: string;
  paidAt?: string;
  remark?: string;
}

// ==================== 状态定义 ====================

const commissions = ref<MyCommission[]>([]);
const statusFilter = ref<string | undefined>(undefined);

// 统计数据
const stats = computed(() => {
  const total = commissions.value.reduce(
    (sum, c) => sum + c.commissionAmount,
    0,
  );
  const paid = commissions.value
    .filter((c) => c.status === 'PAID')
    .reduce((sum, c) => sum + c.commissionAmount, 0);
  const pending = commissions.value
    .filter((c) => c.status === 'PENDING')
    .reduce((sum, c) => sum + c.commissionAmount, 0);
  const count = commissions.value.length;
  return { total, paid, pending, count };
});

// ==================== 常量选项 ====================

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待发放', value: 'PENDING' },
  { label: '已发放', value: 'PAID' },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '合同编号', field: 'contractNo', width: 140 },
  {
    title: '合同名称',
    field: 'contractName',
    minWidth: 180,
    showOverflow: true,
  },
  { title: '客户', field: 'clientName', width: 120 },
  {
    title: '收款金额',
    field: 'paymentAmount',
    width: 120,
    slots: { default: 'paymentAmount' },
  },
  {
    title: '提成比例',
    field: 'commissionRate',
    width: 100,
    slots: { default: 'commissionRate' },
  },
  {
    title: '提成金额',
    field: 'commissionAmount',
    width: 120,
    slots: { default: 'commissionAmount' },
  },
  {
    title: '状态',
    field: 'statusName',
    width: 100,
    slots: { default: 'status' },
  },
  { title: '计算时间', field: 'calculatedAt', width: 110 },
  { title: '发放时间', field: 'paidAt', width: 110 },
];

async function loadData() {
  try {
    const params: Record<string, any> = {};
    if (statusFilter.value) {
      params.status = statusFilter.value;
    }
    const res = await requestClient.get<MyCommission[]>(
      '/finance/my/commissions',
      { params },
    );
    commissions.value = res || [];
    return { items: res || [], total: (res || []).length };
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
    return { items: [], total: 0 };
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    // 移除高度限制，让表格完整显示所有数据
    height: '',
    minHeight: 200,
    pagerConfig: { enabled: false },
    proxyConfig: { ajax: { query: loadData } },
  },
});

// ==================== 操作方法 ====================

function handleStatusChange() {
  gridApi.reload();
}

// ==================== 辅助方法 ====================

function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    PAID: 'green',
    CANCELLED: 'red',
  };
  return colorMap[status] || 'default';
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page title="我的提成" description="查看您的提成记录和发放情况">
    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic title="提成记录数" :value="stats.count" suffix="条" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="提成总额"
            :value="stats.total"
            prefix="¥"
            :precision="2"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="已发放"
            :value="stats.paid"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="待发放"
            :value="stats.pending"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
    </Row>

    <Card>
      <div style="margin-bottom: 16px">
        <Select
          v-model:value="statusFilter"
          placeholder="状态筛选"
          style="width: 150px"
          :options="statusOptions"
          @change="handleStatusChange"
        />
      </div>

      <Grid>
        <template #paymentAmount="{ row }">
          {{ formatMoney(row.paymentAmount) }}
        </template>
        <template #commissionRate="{ row }">
          {{ row.commissionRate }}%
        </template>
        <template #commissionAmount="{ row }">
          <span style="font-weight: 500; color: #1890ff">
            {{ formatMoney(row.commissionAmount) }}
          </span>
        </template>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #empty>
          <Empty description="暂无提成记录" />
        </template>
      </Grid>
    </Card>
  </Page>
</template>
