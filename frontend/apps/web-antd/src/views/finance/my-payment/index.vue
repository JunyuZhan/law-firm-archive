<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Card,
  Col,
  Empty,
  message,
  Progress,
  Row,
  Statistic,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { requestClient } from '#/api/request';

defineOptions({ name: 'MyPayment' });

interface MyContractPayment {
  contractId: number;
  contractNo: string;
  contractName: string;
  clientName: string;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  paymentProgress: number;
  myRole: string;
  myRoleName: string;
  lastPaymentDate?: string;
  payments: PaymentRecord[];
}

interface PaymentRecord {
  id: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  paymentMethodName: string;
  remark?: string;
}

const contracts = ref<MyContractPayment[]>([]);

// 统计数据
const stats = computed(() => {
  const totalAmount = contracts.value.reduce(
    (sum, c) => sum + c.totalAmount,
    0,
  );
  const paidAmount = contracts.value.reduce((sum, c) => sum + c.paidAmount, 0);
  const unpaidAmount = contracts.value.reduce(
    (sum, c) => sum + c.unpaidAmount,
    0,
  );
  const contractCount = contracts.value.length;
  return { totalAmount, paidAmount, unpaidAmount, contractCount };
});

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '合同编号', field: 'contractNo', width: 140 },
  {
    title: '合同名称',
    field: 'contractName',
    minWidth: 200,
    showOverflow: true,
  },
  { title: '客户', field: 'clientName', width: 120 },
  {
    title: '我的角色',
    field: 'myRoleName',
    width: 100,
    slots: { default: 'role' },
  },
  {
    title: '合同金额',
    field: 'totalAmount',
    width: 120,
    slots: { default: 'totalAmount' },
  },
  {
    title: '已收金额',
    field: 'paidAmount',
    width: 120,
    slots: { default: 'paidAmount' },
  },
  {
    title: '收款进度',
    field: 'paymentProgress',
    width: 150,
    slots: { default: 'progress' },
  },
  { title: '最近收款', field: 'lastPaymentDate', width: 110 },
];

async function loadData() {
  try {
    const res = await requestClient.get<MyContractPayment[]>(
      '/finance/my/payments',
    );
    contracts.value = res || [];
    return { items: res || [], total: (res || []).length };
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
    return { items: [], total: 0 };
  }
}

const [Grid] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    // 移除高度限制，让表格完整显示所有数据
    height: '',
    minHeight: 200,
    pagerConfig: { enabled: false },
    proxyConfig: { ajax: { query: loadData } },
  },
});

// ==================== 辅助方法 ====================

function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

function getRoleColor(role: string) {
  const colorMap: Record<string, string> = {
    LEAD: 'blue',
    CO_COUNSEL: 'green',
    ORIGINATOR: 'orange',
    PARALEGAL: 'default',
  };
  return colorMap[role] || 'default';
}

function getProgressStatus(progress: number) {
  if (progress >= 100) return 'success';
  if (progress >= 50) return 'active';
  return 'normal';
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page title="我的收款" description="查看您参与合同的收款进度">
    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="参与合同数"
            :value="stats.contractCount"
            suffix="个"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="合同总金额"
            :value="stats.totalAmount"
            prefix="¥"
            :precision="2"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="已收金额"
            :value="stats.paidAmount"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="待收金额"
            :value="stats.unpaidAmount"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
    </Row>

    <Card>
      <Grid>
        <template #totalAmount="{ row }">
          {{ formatMoney(row.totalAmount) }}
        </template>
        <template #paidAmount="{ row }">
          <span style="color: #52c41a">{{ formatMoney(row.paidAmount) }}</span>
        </template>
        <template #role="{ row }">
          <Tag :color="getRoleColor(row.myRole)">{{ row.myRoleName }}</Tag>
        </template>
        <template #progress="{ row }">
          <Progress
            :percent="row.paymentProgress"
            :status="getProgressStatus(row.paymentProgress)"
            size="small"
          />
        </template>
        <template #empty>
          <Empty description="暂无参与的合同" />
        </template>
      </Grid>
    </Card>
  </Page>
</template>
