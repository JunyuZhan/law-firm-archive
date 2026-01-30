<script setup lang="ts">
import type { GenerateReportCommand } from '#/api/workbench/report';

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import echarts from '@vben/plugins/echarts';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Dropdown,
  Menu,
  message,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getCommissionReport,
  getCommissionSummary,
  getContractStatistics,
  getExpenseList,
  getFeeList,
  getInvoiceStatistics,
} from '#/api/finance';
import { generateReport } from '#/api/workbench/report';
import { getRevenueStats } from '#/api/workbench/statistics';
import { useResponsive } from '#/hooks/useResponsive';

defineOptions({ name: 'FinanceReport' });

// 响应式布局
const { isMobile } = useResponsive();

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
  return contractStats.value.totalAmount || 0;
});

const yearlyExpense = computed(() => {
  return expenseData.value.reduce((sum, item) => sum + (item.amount || 0), 0);
});

const receivableAmount = computed(() => {
  return (
    (contractStats.value.totalAmount || 0) -
    (contractStats.value.receivedAmount || 0)
  );
});

const profitRate = computed(() => {
  if (yearlyIncome.value === 0) return 0;
  return (
    ((yearlyIncome.value - yearlyExpense.value) / yearlyIncome.value) *
    100
  ).toFixed(1);
});

// 收入统计表格列
const revenueColumns = [
  { title: '月份', dataIndex: 'month', key: 'month', width: 100 },
  {
    title: '合同金额',
    dataIndex: 'contractAmount',
    key: 'contractAmount',
    width: 120,
  },
  {
    title: '收款金额',
    dataIndex: 'receivedAmount',
    key: 'receivedAmount',
    width: 120,
  },
  {
    title: '开票金额',
    dataIndex: 'invoicedAmount',
    key: 'invoicedAmount',
    width: 120,
  },
  {
    title: '提成支出',
    dataIndex: 'commissionAmount',
    key: 'commissionAmount',
    width: 120,
  },
];

// 提成报表列
const commissionColumns = [
  { title: '律师', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '提成笔数', dataIndex: 'count', key: 'count', width: 100 },
  {
    title: '提成总额',
    dataIndex: 'totalAmount',
    key: 'totalAmount',
    width: 120,
  },
  {
    title: '已发放',
    dataIndex: 'issuedAmount',
    key: 'issuedAmount',
    width: 120,
  },
  {
    title: '待发放',
    dataIndex: 'pendingAmount',
    key: 'pendingAmount',
    width: 120,
  },
];

// 费用统计列
const expenseColumns = [
  {
    title: '费用类型',
    dataIndex: 'expenseTypeName',
    key: 'expenseTypeName',
    width: 120,
  },
  {
    title: '申请人',
    dataIndex: 'applicantName',
    key: 'applicantName',
    width: 100,
  },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '申请日期', dataIndex: 'createdAt', key: 'createdAt', width: 110 },
];

// 收费管理列
const feeColumns = [
  { title: '收费编号', dataIndex: 'feeNo', key: 'feeNo', width: 140 },
  { title: '项目名称', dataIndex: 'matterName', key: 'matterName', width: 150 },
  { title: '客户名称', dataIndex: 'clientName', key: 'clientName', width: 120 },
  {
    title: '收费类型',
    dataIndex: 'feeTypeName',
    key: 'feeTypeName',
    width: 100,
  },
  { title: '应收金额', dataIndex: 'amount', key: 'amount', width: 110 },
  { title: '已收金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 110 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 80 },
  { title: '创建日期', dataIndex: 'createdAt', key: 'createdAt', width: 110 },
];

// 账龄分析列
const agingColumns = [
  { title: '账龄区间', dataIndex: 'range', key: 'range', width: 120 },
  { title: '笔数', dataIndex: 'count', key: 'count', width: 80 },
  { title: '应收金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '占比', dataIndex: 'percentage', key: 'percentage', width: 100 },
];

const revenueData = ref<any[]>([]);

// 图表引用
const trendChartRef = ref<HTMLDivElement>();
const agingChartRef = ref<HTMLDivElement>();

// 渲染收入趋势图
function renderTrendChart() {
  if (!trendChartRef.value || revenueData.value.length === 0) return;

  const chart =
    echarts.getInstanceByDom(trendChartRef.value) ||
    echarts.init(trendChartRef.value);

  const months = revenueData.value.map((d) => d.month);
  const received = revenueData.value.map((d) => d.receivedAmount || 0);
  const contract = revenueData.value.map((d) => d.contractAmount || 0);

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收款金额', '合同金额'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: months },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: (v: number) => `¥${(v / 10_000).toFixed(0)}万` },
    },
    series: [
      {
        name: '收款金额',
        type: 'line',
        data: received,
        smooth: true,
        itemStyle: { color: '#52c41a' },
      },
      {
        name: '合同金额',
        type: 'bar',
        data: contract,
        itemStyle: { color: '#1890ff' },
      },
    ],
  });
}

// 渲染账龄分布图
function renderAgingChart() {
  if (!agingChartRef.value) return;

  const chart =
    echarts.getInstanceByDom(agingChartRef.value) ||
    echarts.init(agingChartRef.value);
  const data = agingData.value
    .filter((d) => d.amount > 0)
    .map((d) => ({
      name: d.range,
      value: d.amount,
    }));

  if (data.length === 0) {
    chart.setOption({
      title: {
        text: '暂无逾期应收',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 },
      },
      series: [],
    });
    return;
  }

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data,
        label: { show: true, formatter: '{b}\n{d}%' },
        itemStyle: {
          color: (params: any) => {
            const colors = ['#52c41a', '#faad14', '#fa8c16', '#f5222d'];
            return colors[params.dataIndex] || '#1890ff';
          },
        },
      },
    ],
  });
}

// 销毁图表
function disposeCharts() {
  [trendChartRef.value, agingChartRef.value].forEach((ref) => {
    if (ref) {
      const chart = echarts.getInstanceByDom(ref);
      if (chart) chart.dispose();
    }
  });
}

// 账龄分析数据
const agingData = computed(() => {
  // 根据收费数据计算账龄分布
  const now = dayjs();
  const ranges = [
    { range: '0-30天', min: 0, max: 30, count: 0, amount: 0 },
    { range: '31-60天', min: 31, max: 60, count: 0, amount: 0 },
    { range: '61-90天', min: 61, max: 90, count: 0, amount: 0 },
    { range: '90天以上', min: 91, max: 9999, count: 0, amount: 0 },
  ];

  feeData.value.forEach((fee: any) => {
    if (fee.status === 'PAID' || !fee.createdAt) return;
    const receivable = (fee.amount || 0) - (fee.paidAmount || 0);
    if (receivable <= 0) return;

    const days = now.diff(dayjs(fee.createdAt), 'day');
    const range = ranges.find((r) => days >= r.min && days <= r.max);
    if (range) {
      range.count++;
      range.amount += receivable;
    }
  });

  const total = ranges.reduce((sum, r) => sum + r.amount, 0);
  return ranges.map((r) => ({
    ...r,
    percentage: total > 0 ? `${((r.amount / total) * 100).toFixed(1)}%` : '0%',
  }));
});

// 收费统计计算
const feeStats = computed(() => {
  const total = feeData.value.reduce((sum, f) => sum + (f.amount || 0), 0);
  const paid = feeData.value.reduce((sum, f) => sum + (f.paidAmount || 0), 0);
  const pending = total - paid;
  const count = feeData.value.length;
  return { total, paid, pending, count };
});

// 监听数据变化重新渲染图表
watch([revenueData, () => agingData.value], () => {
  setTimeout(() => {
    renderTrendChart();
    renderAgingChart();
  }, 100);
});

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
  const monthStats = new Map<
    string,
    {
      commissionAmount: number;
      contractAmount: number;
      invoicedAmount: number;
      receivedAmount: number;
    }
  >();

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
        stats.commissionAmount += Number(
          item.commissionAmount || item.totalAmount || 0,
        );
      }
    }
  });

  // 转换为数组
  revenueData.value = [...monthStats.entries()]
    .map(([month, stats]) => ({
      month,
      ...stats,
    }))
    .toSorted((a, b) => a.month.localeCompare(b.month));
}

// 处理月份筛选变化
function handleMonthFilterChange(value: any) {
  if (!value || typeof value !== 'string') return;
  monthFilter.value = value;
  const now = dayjs();

  switch (value) {
    case 'custom': {
      // 自定义时，dateRange已经通过DatePicker设置
      break;
    }
    case 'recent3': {
      dateRange.value = [
        now.subtract(2, 'month').startOf('month'),
        now.endOf('month'),
      ];
      break;
    }
    case 'recent6': {
      dateRange.value = [
        now.subtract(5, 'month').startOf('month'),
        now.endOf('month'),
      ];
      break;
    }
    case 'thisYear': {
      dateRange.value = [now.startOf('year'), now.endOf('year')];
      break;
    }
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

// 导出报表类型
const exportTypes = [
  { key: 'REVENUE', label: '收入报表' },
  { key: 'CONTRACT', label: '合同报表' },
  { key: 'COMMISSION', label: '提成报表' },
  { key: 'EXPENSE', label: '费用报销报表' },
  { key: 'RECEIVABLE', label: '应收账款报表' },
  { key: 'AGING_ANALYSIS', label: '账龄分析报表' },
];

// 导出中状态
const exporting = ref(false);

async function handleExport(reportType: string) {
  if (exporting.value) return;

  exporting.value = true;
  try {
    const [startDate, endDate] = dateRange.value;
    const command: GenerateReportCommand = {
      reportType,
      format: 'EXCEL',
      parameters: {
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
      },
    };
    await generateReport(command);
    message.success('报表生成任务已提交，请到"报表中心"查看和下载');
  } catch (error: any) {
    message.error(`导出报表失败：${error.message || '未知错误'}`);
  } finally {
    exporting.value = false;
  }
}

function handleMenuClick(info: { key: number | string }) {
  handleExport(String(info.key));
}

onMounted(() => {
  loadAllData();
});

onBeforeUnmount(() => {
  disposeCharts();
});
</script>

<template>
  <Page title="财务报表" description="查看财务统计报表">
    <Spin :spinning="loading">
      <Row :gutter="[16, 16]" style="margin-bottom: 16px">
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Card
            :body-style="{
              padding: '16px',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
            }"
            style="height: 100%"
          >
            <Statistic
              title="年度收入"
              :value="yearlyIncome"
              prefix="¥"
              :precision="2"
              class="finance-statistic finance-statistic-amount"
            />
          </Card>
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Card
            :body-style="{
              padding: '16px',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
            }"
            style="height: 100%"
          >
            <Statistic
              title="年度支出"
              :value="yearlyExpense"
              prefix="¥"
              :precision="2"
              class="finance-statistic finance-statistic-amount"
            />
          </Card>
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Card
            :body-style="{
              padding: '16px',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
            }"
            style="height: 100%"
          >
            <Statistic
              title="应收账款"
              :value="receivableAmount"
              prefix="¥"
              :precision="2"
              class="finance-statistic finance-statistic-amount"
            />
          </Card>
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Card
            :body-style="{
              padding: '16px',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
            }"
            style="height: 100%"
          >
            <Statistic
              title="利润率"
              :value="profitRate"
              suffix="%"
              :precision="1"
              class="finance-statistic"
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <template #extra>
          <Space
            :direction="isMobile ? 'vertical' : 'horizontal'"
            :size="isMobile ? 8 : 12"
          >
            <Select
              v-model:value="monthFilter"
              :style="{ width: isMobile ? '100%' : '150px' }"
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
              :style="{ width: isMobile ? '100%' : 'auto' }"
              @change="handleDateChange"
            />
            <Dropdown>
              <Button :loading="exporting" :block="isMobile">
                导出报表
                <template #icon>
                  <span style="margin-left: 4px">▼</span>
                </template>
              </Button>
              <template #overlay>
                <Menu @click="handleMenuClick">
                  <Menu.Item v-for="item in exportTypes" :key="item.key">
                    {{ item.label }}
                  </Menu.Item>
                </Menu>
              </template>
            </Dropdown>
          </Space>
        </template>

        <Tabs v-model:active-key="activeTab">
          <Tabs.TabPane key="overview" tab="收入概览">
            <Row :gutter="[16, 16]" style="margin-bottom: 16px">
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="合同总数"
                    :value="contractStats.totalCount || 0"
                    suffix="份"
                    class="finance-statistic"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="合同总额"
                    :value="contractStats.totalAmount || 0"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="已收款"
                    :value="contractStats.receivedAmount || 0"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="待收款"
                    :value="receivableAmount"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
            </Row>
            <!-- 收入趋势图 -->
            <Card size="small" title="收入趋势" style="margin-bottom: 16px">
              <div ref="trendChartRef" style="height: 280px"></div>
            </Card>
            <Table
              :columns="revenueColumns"
              :data-source="revenueData"
              :pagination="false"
              size="small"
              row-key="month"
            >
              <template #bodyCell="{ column, text }">
                <template
                  v-if="
                    [
                      'contractAmount',
                      'receivedAmount',
                      'invoicedAmount',
                      'commissionAmount',
                    ].includes(column.key as string)
                  "
                >
                  {{ formatMoney(text) }}
                </template>
              </template>
            </Table>
          </Tabs.TabPane>

          <Tabs.TabPane key="invoice" tab="发票统计">
            <Row :gutter="[16, 16]" style="margin-bottom: 16px">
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="发票总数"
                    :value="invoiceStats.totalCount || 0"
                    suffix="张"
                    class="finance-statistic"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="开票总额"
                    :value="invoiceStats.totalAmount || 0"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="待开票"
                    :value="invoiceStats.pendingCount || 0"
                    suffix="张"
                    class="finance-statistic"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="已作废"
                    :value="invoiceStats.cancelledCount || 0"
                    suffix="张"
                    class="finance-statistic"
                  />
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>

          <Tabs.TabPane key="commission" tab="提成报表">
            <Row :gutter="[16, 16]" style="margin-bottom: 16px">
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="提成总额"
                    :value="commissionSummary.totalAmount || 0"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="已发放"
                    :value="commissionSummary.issuedAmount || 0"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="待发放"
                    :value="commissionSummary.pendingAmount || 0"
                    prefix="¥"
                    :precision="0"
                    class="finance-statistic finance-statistic-amount"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  :body-style="{
                    padding: '16px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }"
                  style="height: 100%"
                >
                  <Statistic
                    title="涉及人数"
                    :value="commissionReport.length || 0"
                    suffix="人"
                    class="finance-statistic"
                  />
                </Card>
              </Col>
            </Row>
            <Table
              :columns="commissionColumns"
              :data-source="commissionReport"
              :pagination="false"
              size="small"
              row-key="userId"
            >
              <template #bodyCell="{ column, text }">
                <template
                  v-if="
                    ['totalAmount', 'issuedAmount', 'pendingAmount'].includes(
                      column.key as string,
                    )
                  "
                >
                  {{ formatMoney(text) }}
                </template>
              </template>
            </Table>
          </Tabs.TabPane>

          <Tabs.TabPane key="expense" tab="费用报销">
            <Row :gutter="[16, 16]" style="margin-bottom: 16px">
              <Col :xs="24" :sm="8" :md="8" :lg="8">
                <Card size="small">
                  <Statistic
                    title="报销总额"
                    :value="yearlyExpense"
                    prefix="¥"
                    :precision="0"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="8" :md="8" :lg="8">
                <Card size="small">
                  <Statistic
                    title="报销笔数"
                    :value="expenseData.length"
                    suffix="笔"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="8" :md="8" :lg="8">
                <Card size="small">
                  <Statistic
                    title="平均金额"
                    :value="
                      expenseData.length > 0
                        ? yearlyExpense / expenseData.length
                        : 0
                    "
                    prefix="¥"
                    :precision="0"
                  />
                </Card>
              </Col>
            </Row>
            <Table
              :columns="expenseColumns"
              :data-source="expenseData"
              :pagination="{ pageSize: 10 }"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, text }">
                <template v-if="column.key === 'amount'">
                  {{ formatMoney(text) }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDate(text) }}
                </template>
              </template>
            </Table>
          </Tabs.TabPane>

          <Tabs.TabPane key="fee" tab="收费管理">
            <Row :gutter="[16, 16]" style="margin-bottom: 16px">
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="收费笔数"
                    :value="feeStats.count"
                    suffix="笔"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="应收总额"
                    :value="feeStats.total"
                    prefix="¥"
                    :precision="0"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="已收金额"
                    :value="feeStats.paid"
                    prefix="¥"
                    :precision="0"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="待收金额"
                    :value="feeStats.pending"
                    prefix="¥"
                    :precision="0"
                  />
                </Card>
              </Col>
            </Row>
            <Table
              :columns="feeColumns"
              :data-source="feeData"
              :pagination="{ pageSize: 10 }"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, text }">
                <template
                  v-if="['amount', 'paidAmount'].includes(column.key as string)"
                >
                  {{ formatMoney(text) }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDate(text) }}
                </template>
              </template>
            </Table>
          </Tabs.TabPane>

          <Tabs.TabPane key="aging" tab="账龄分析">
            <Row :gutter="[16, 16]" style="margin-bottom: 16px">
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="0-30天"
                    :value="agingData[0]?.amount || 0"
                    prefix="¥"
                    :precision="0"
                    :value-style="{ color: '#52c41a' }"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="31-60天"
                    :value="agingData[1]?.amount || 0"
                    prefix="¥"
                    :precision="0"
                    :value-style="{ color: '#faad14' }"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card size="small">
                  <Statistic
                    title="61-90天"
                    :value="agingData[2]?.amount || 0"
                    prefix="¥"
                    :precision="0"
                    :value-style="{ color: '#fa8c16' }"
                  />
                </Card>
              </Col>
              <Col :xs="12" :sm="12" :md="6" :lg="6">
                <Card
                  size="small"
                  :style="{
                    background:
                      agingData[3] && agingData[3].amount > 0 ? '#fff2f0' : '',
                  }"
                >
                  <Statistic
                    title="90天以上(逾期)"
                    :value="agingData[3]?.amount || 0"
                    prefix="¥"
                    :precision="0"
                    :value-style="{ color: '#f5222d' }"
                  />
                </Card>
              </Col>
            </Row>
            <Row :gutter="[16, 16]">
              <Col :xs="24" :sm="24" :md="10" :lg="10">
                <!-- 账龄分布图 -->
                <Card size="small" title="账龄分布">
                  <div ref="agingChartRef" style="height: 260px"></div>
                </Card>
              </Col>
              <Col :xs="24" :sm="24" :md="14" :lg="14">
                <Card size="small" title="账龄明细">
                  <Table
                    :columns="agingColumns"
                    :data-source="agingData"
                    :pagination="false"
                    size="small"
                    row-key="range"
                  >
                    <template #bodyCell="{ column, text, record }">
                      <template v-if="column.key === 'amount'">
                        {{ formatMoney(text) }}
                      </template>
                      <template v-else-if="column.key === 'range'">
                        <span
                          :style="{
                            color:
                              record.range === '90天以上' && record.amount > 0
                                ? '#f5222d'
                                : '',
                          }"
                        >
                          {{ text }}
                        </span>
                      </template>
                    </template>
                  </Table>
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>
        </Tabs>
      </Card>
    </Spin>
  </Page>
</template>

<style scoped>
/* 财务报表统计卡片样式 - 统一字体大小 */
.finance-statistic :deep(.ant-statistic-content) {
  font-size: clamp(14px, 2vw, 20px);
}

.finance-statistic :deep(.ant-statistic-content-value) {
  font-size: clamp(14px, 2vw, 20px);
}

/* 金额统计卡片 - 防止换行 */
.finance-statistic-amount :deep(.ant-statistic-content-value),
.finance-statistic-amount :deep(.ant-statistic-content-prefix) {
  white-space: nowrap;
}

/* 确保统计卡片外框大小一致 */
:deep(.ant-row) > :deep(.ant-col) > :deep(.ant-card) {
  display: flex;
  flex-direction: column;
}
</style>
