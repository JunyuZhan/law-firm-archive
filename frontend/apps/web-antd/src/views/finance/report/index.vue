<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { message, Card, Row, Col, Statistic, DatePicker, Space, Button, Table, Tabs, Spin, Select } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  getContractStatistics,
  getInvoiceStatistics,
  getCommissionSummary,
  getCommissionReport,
  getFeeList,
  getExpenseList,
} from '#/api/finance';
import { getRevenueStats } from '#/api/workbench/statistics';
import dayjs from 'dayjs';

defineOptions({ name: 'FinanceReport' });

const loading = ref(false);
const activeTab = ref('overview');
// 默认显示最近3个月的数据
const dateRange = ref<[dayjs.Dayjs, dayjs.Dayjs]>([
  dayjs().subtract(2, 'month').startOf('month'),
  dayjs().endOf('month'),
]);
// 月份筛选选项
const monthFilter = ref<string>('recent3'); // recent3-最近3个月, recent6-最近6个月, thisYear-本年, custom-自定义

// 统计数据
const contractStats = ref<Record<string, any>>({});
const invoiceStats = ref<Record<string, any>>({});
const commissionSummary = ref<Record<string, any>>({});
const commissionReport = ref<any[]>([]);
const feeData = ref<any[]>([]);
const expenseData = ref<any[]>([]);
const revenueTrends = ref<any[]>([]);

// 计算属性
const yearlyIncome = computed(() => {
  return (contractStats.value.totalAmount || 0);
});

const yearlyExpense = computed(() => {
  return expenseData.value.reduce((sum, item) => sum + (item.amount || 0), 0);
});

const receivableAmount = computed(() => {
  return (contractStats.value.totalAmount || 0) - (contractStats.value.receivedAmount || 0);
});

const profitRate = computed(() => {
  if (yearlyIncome.value === 0) return 0;
  return ((yearlyIncome.value - yearlyExpense.value) / yearlyIncome.value * 100).toFixed(1);
});

// 收入统计表格列
const revenueColumns = [
  { title: '月份', dataIndex: 'month', key: 'month', width: 100 },
  { title: '合同金额', dataIndex: 'contractAmount', key: 'contractAmount', width: 120 },
  { title: '收款金额', dataIndex: 'receivedAmount', key: 'receivedAmount', width: 120 },
  { title: '开票金额', dataIndex: 'invoicedAmount', key: 'invoicedAmount', width: 120 },
  { title: '提成支出', dataIndex: 'commissionAmount', key: 'commissionAmount', width: 120 },
];

// 提成报表列
const commissionColumns = [
  { title: '律师', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '提成笔数', dataIndex: 'count', key: 'count', width: 100 },
  { title: '提成总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '已发放', dataIndex: 'issuedAmount', key: 'issuedAmount', width: 120 },
  { title: '待发放', dataIndex: 'pendingAmount', key: 'pendingAmount', width: 120 },
];

// 费用统计列
const expenseColumns = [
  { title: '费用类型', dataIndex: 'expenseTypeName', key: 'expenseTypeName', width: 120 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '申请日期', dataIndex: 'createdAt', key: 'createdAt', width: 110 },
];

const revenueData = ref<any[]>([]);

async function loadContractStats() {
  try {
    const [startDate, endDate] = dateRange.value;
    const res = await getContractStatistics({
      startDate: startDate.format('YYYY-MM-DD'),
      endDate: endDate.format('YYYY-MM-DD'),
    });
    contractStats.value = res || {};
  } catch (error) {
    console.error('加载合同统计失败', error);
  }
}

async function loadInvoiceStats() {
  try {
    const res = await getInvoiceStatistics();
    invoiceStats.value = res || {};
  } catch (error) {
    console.error('加载发票统计失败', error);
  }
}

async function loadCommissionSummary() {
  try {
    const [startDate, endDate] = dateRange.value;
    const res = await getCommissionSummary(
      startDate.format('YYYY-MM-DD'),
      endDate.format('YYYY-MM-DD'),
    );
    commissionSummary.value = res || {};
  } catch (error) {
    console.error('加载提成汇总失败', error);
  }
}

async function loadCommissionReport() {
  try {
    const [startDate, endDate] = dateRange.value;
    const res = await getCommissionReport(
      startDate.format('YYYY-MM-DD'),
      endDate.format('YYYY-MM-DD'),
    );
    commissionReport.value = Array.isArray(res) ? res : [];
  } catch (error) {
    console.error('加载提成报表失败', error);
  }
}

async function loadFeeData() {
  try {
    const res = await getFeeList({ pageNum: 1, pageSize: 100 });
    feeData.value = res.list || [];
  } catch (error) {
    console.error('加载收费数据失败', error);
  }
}

async function loadExpenseData() {
  try {
    const res = await getExpenseList({ pageNum: 1, pageSize: 100 });
    expenseData.value = res.list || [];
  } catch (error) {
    console.error('加载费用数据失败', error);
  }
}

async function loadRevenueTrends() {
  try {
    const res = await getRevenueStats();
    revenueTrends.value = res.trends || [];
  } catch (error) {
    console.error('加载收入趋势失败', error);
  }
}

// 生成月度收入数据（基于实际合同和收款数据）
function generateMonthlyData() {
  const [startDate, endDate] = dateRange.value;
  let current = startDate.startOf('month');
  
  // 按月份分组统计合同和收款数据
  const monthStats = new Map<string, {
    contractAmount: number;
    receivedAmount: number;
    invoicedAmount: number;
    commissionAmount: number;
  }>();
  
  // 初始化所有月份
  while (current.isBefore(endDate) || current.isSame(endDate, 'month')) {
    const monthKey = current.format('YYYY-MM');
    monthStats.set(monthKey, {
      contractAmount: 0,
      receivedAmount: 0,
      invoicedAmount: 0,
      commissionAmount: 0,
    });
    current = current.add(1, 'month');
  }
  
  // 填充收入趋势数据（收款金额）
  const startMonth = startDate.format('YYYY-MM');
  const endMonth = endDate.format('YYYY-MM');
  const filteredTrends = revenueTrends.value.filter((trend: any) => {
    if (!trend) return false;
    const month = trend.period;
    return month >= startMonth && month <= endMonth;
  });
  
  filteredTrends.forEach((trend: any) => {
    if (!trend) return;
    const monthKey = trend.period;
    if (monthStats.has(monthKey)) {
      const stats = monthStats.get(monthKey)!;
      // 收款金额来自收入趋势
      stats.receivedAmount = Number(trend.amount) || 0;
      // 合同金额和收款金额相同（简化处理，实际应该从合同统计中获取）
      stats.contractAmount = stats.receivedAmount;
    }
  });
  
  // 统计提成数据
  commissionReport.value.forEach((item: any) => {
    if (item && item.createdAt) {
      const commDate = dayjs(item.createdAt);
      const monthKey = commDate.format('YYYY-MM');
      if (monthStats.has(monthKey)) {
        const stats = monthStats.get(monthKey)!;
        stats.commissionAmount += Number(item.commissionAmount || item.totalAmount || 0);
      }
    }
  });
  
  // 转换为数组
  revenueData.value = Array.from(monthStats.entries())
    .map(([month, stats]) => ({
      month,
      ...stats,
    }))
    .sort((a, b) => a.month.localeCompare(b.month));
}

// 处理月份筛选变化
function handleMonthFilterChange(value: any) {
  if (!value || typeof value !== 'string') return;
  monthFilter.value = value;
  const now = dayjs();
  
  switch (value) {
    case 'recent3':
      dateRange.value = [
        now.subtract(2, 'month').startOf('month'),
        now.endOf('month'),
      ];
      break;
    case 'recent6':
      dateRange.value = [
        now.subtract(5, 'month').startOf('month'),
        now.endOf('month'),
      ];
      break;
    case 'thisYear':
      dateRange.value = [
        now.startOf('year'),
        now.endOf('year'),
      ];
      break;
    case 'custom':
      // 自定义时，dateRange已经通过DatePicker设置
      break;
  }
  
  loadAllData();
}

async function loadAllData() {
  loading.value = true;
  try {
    await Promise.all([
      loadContractStats(),
      loadInvoiceStats(),
      loadCommissionSummary(),
      loadCommissionReport(),
      loadFeeData(),
      loadExpenseData(),
      loadRevenueTrends(),
    ]);
    generateMonthlyData();
  } finally {
    loading.value = false;
  }
}

function handleDateChange() {
  loadAllData();
}

function formatMoney(amount?: number) {
  if (amount === undefined || amount === null) return '¥0';
  return `¥${amount.toLocaleString()}`;
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

function handleExport() {
  message.info('导出功能开发中');
}

onMounted(() => {
  loadAllData();
});
</script>

<template>
  <Page title="财务报表" description="查看财务统计报表">
    <Spin :spinning="loading">
      <Row :gutter="16" style="margin-bottom: 16px">
        <Col :span="6">
          <Card>
            <Statistic title="年度收入" :value="yearlyIncome" prefix="¥" :precision="2" />
          </Card>
        </Col>
        <Col :span="6">
          <Card>
            <Statistic title="年度支出" :value="yearlyExpense" prefix="¥" :precision="2" />
          </Card>
        </Col>
        <Col :span="6">
          <Card>
            <Statistic title="应收账款" :value="receivableAmount" prefix="¥" :precision="2" />
          </Card>
        </Col>
        <Col :span="6">
          <Card>
            <Statistic title="利润率" :value="profitRate" suffix="%" :precision="1" />
          </Card>
        </Col>
      </Row>
      
      <Card>
        <template #extra>
          <Space>
            <Select
              v-model:value="monthFilter"
              style="width: 150px"
              @change="handleMonthFilterChange"
            >
              <Select.Option value="recent3">最近3个月</Select.Option>
              <Select.Option value="recent6">最近6个月</Select.Option>
              <Select.Option value="thisYear">本年度</Select.Option>
              <Select.Option value="custom">自定义</Select.Option>
            </Select>
            <DatePicker.RangePicker
              v-model:value="dateRange"
              :disabled="monthFilter !== 'custom'"
              @change="handleDateChange"
            />
            <Button @click="handleExport">导出报表</Button>
          </Space>
        </template>
        
        <Tabs v-model:activeKey="activeTab">
          <Tabs.TabPane key="overview" tab="收入概览">
            <Row :gutter="16" style="margin-bottom: 16px">
              <Col :span="6">
                <Card size="small">
                  <Statistic title="合同总数" :value="contractStats.totalCount || 0" suffix="份" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="合同总额" :value="contractStats.totalAmount || 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="已收款" :value="contractStats.receivedAmount || 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="待收款" :value="receivableAmount" prefix="¥" :precision="0" />
                </Card>
              </Col>
            </Row>
            <Table
              :columns="revenueColumns"
              :dataSource="revenueData"
              :pagination="false"
              size="small"
              rowKey="month"
              :scroll="{ y: 400 }"
            >
              <template #bodyCell="{ column, text }">
                <template v-if="['contractAmount', 'receivedAmount', 'invoicedAmount', 'commissionAmount'].includes(column.key as string)">
                  {{ formatMoney(text) }}
                </template>
              </template>
            </Table>
          </Tabs.TabPane>
          
          <Tabs.TabPane key="invoice" tab="发票统计">
            <Row :gutter="16" style="margin-bottom: 16px">
              <Col :span="6">
                <Card size="small">
                  <Statistic title="发票总数" :value="invoiceStats.totalCount || 0" suffix="张" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="开票总额" :value="invoiceStats.totalAmount || 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="待开票" :value="invoiceStats.pendingCount || 0" suffix="张" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="已作废" :value="invoiceStats.cancelledCount || 0" suffix="张" />
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>
          
          <Tabs.TabPane key="commission" tab="提成报表">
            <Row :gutter="16" style="margin-bottom: 16px">
              <Col :span="6">
                <Card size="small">
                  <Statistic title="提成总额" :value="commissionSummary.totalAmount || 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="已发放" :value="commissionSummary.issuedAmount || 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="待发放" :value="commissionSummary.pendingAmount || 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="6">
                <Card size="small">
                  <Statistic title="涉及人数" :value="commissionReport.length || 0" suffix="人" />
                </Card>
              </Col>
            </Row>
            <Table :columns="commissionColumns" :dataSource="commissionReport" :pagination="false" size="small" rowKey="userId">
              <template #bodyCell="{ column, text }">
                <template v-if="['totalAmount', 'issuedAmount', 'pendingAmount'].includes(column.key as string)">
                  {{ formatMoney(text) }}
                </template>
              </template>
            </Table>
          </Tabs.TabPane>
          
          <Tabs.TabPane key="expense" tab="费用报销">
            <Row :gutter="16" style="margin-bottom: 16px">
              <Col :span="8">
                <Card size="small">
                  <Statistic title="报销总额" :value="yearlyExpense" prefix="¥" :precision="0" />
                </Card>
              </Col>
              <Col :span="8">
                <Card size="small">
                  <Statistic title="报销笔数" :value="expenseData.length" suffix="笔" />
                </Card>
              </Col>
              <Col :span="8">
                <Card size="small">
                  <Statistic title="平均金额" :value="expenseData.length ? yearlyExpense / expenseData.length : 0" prefix="¥" :precision="0" />
                </Card>
              </Col>
            </Row>
            <Table :columns="expenseColumns" :dataSource="expenseData" :pagination="{ pageSize: 10 }" size="small" rowKey="id">
              <template #bodyCell="{ column, text }">
                <template v-if="column.key === 'amount'">{{ formatMoney(text) }}</template>
                <template v-else-if="column.key === 'createdAt'">{{ formatDate(text) }}</template>
              </template>
            </Table>
          </Tabs.TabPane>
        </Tabs>
      </Card>
    </Spin>
  </Page>
</template>
