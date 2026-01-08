<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue';
import { message } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Row,
  Col,
  Statistic,
  Button,
  Space,
  Tabs,
  TabPane,
  Select,
  DatePicker,
  Spin,
} from 'ant-design-vue';
import dayjs from 'dayjs';
// 使用项目封装的 echarts
import echarts from '@vben/plugins/echarts';
import {
  getRevenueStats,
  getMatterStats,
  getClientStats,
  getLawyerPerformanceRanking,
  type RevenueStats,
  type MatterStats,
  type ClientStats,
  type LawyerPerformance,
} from '#/api/workbench/statistics';
import {
  getAvailableReports,
  generateReport,
  type GenerateReportCommand,
} from '#/api/workbench/report';

defineOptions({ name: 'ReportCenter' });

// 状态
const loading = ref(false);
const activeTab = ref('overview');
const dateRange = ref<[dayjs.Dayjs, dayjs.Dayjs]>([
  dayjs().startOf('year'),
  dayjs().endOf('year'),
]);

// 统计数据
const revenueStats = ref<RevenueStats | null>(null);
const matterStats = ref<MatterStats | null>(null);
const clientStats = ref<ClientStats | null>(null);
const lawyerRanking = ref<LawyerPerformance[]>([]);

// 图表引用
const revenueTrendChartRef = ref<HTMLDivElement>();
const matterStatusChartRef = ref<HTMLDivElement>();
const matterTypeChartRef = ref<HTMLDivElement>();
const clientTypeChartRef = ref<HTMLDivElement>();
const lawyerRankingChartRef = ref<HTMLDivElement>();

// 状态名称映射
const matterStatusMap: Record<string, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  ARCHIVED: '已归档',
  CLOSED: '已关闭',
};

const matterTypeMap: Record<string, string> = {
  CIVIL: '民事',
  CRIMINAL: '刑事',
  ADMINISTRATIVE: '行政',
  ARBITRATION: '仲裁',
  CONSULTATION: '咨询',
};

const clientTypeMap: Record<string, string> = {
  INDIVIDUAL: '个人',
  ENTERPRISE: '企业',
  GOVERNMENT: '政府',
  OTHER: '其他',
};

// 加载收入统计
async function loadRevenueStats() {
  try {
    const data = await getRevenueStats();
    revenueStats.value = data;
    renderRevenueTrendChart();
  } catch (error: any) {
    message.error('加载收入统计失败：' + (error.message || '未知错误'));
  }
}

// 加载项目统计
async function loadMatterStats() {
  try {
    const data = await getMatterStats();
    matterStats.value = data;
    renderMatterStatusChart();
    renderMatterTypeChart();
  } catch (error: any) {
    message.error('加载项目统计失败：' + (error.message || '未知错误'));
  }
}

// 加载客户统计
async function loadClientStats() {
  try {
    const data = await getClientStats();
    clientStats.value = data;
    renderClientTypeChart();
  } catch (error: any) {
    message.error('加载客户统计失败：' + (error.message || '未知错误'));
  }
}

// 加载律师业绩排行
async function loadLawyerRanking() {
  try {
    const data = await getLawyerPerformanceRanking(10);
    lawyerRanking.value = data || [];
    renderLawyerRankingChart();
  } catch (error: any) {
    message.error('加载律师业绩排行失败：' + (error.message || '未知错误'));
  }
}

// 渲染收入趋势图
function renderRevenueTrendChart() {
  if (!revenueTrendChartRef.value || !revenueStats.value) return;
  
  const chart = getOrCreateChart(revenueTrendChartRef.value);
  if (!chart) return;
  const trends = revenueStats.value.trends || [];
  
  chart.setOption({
    title: {
      text: '收入趋势',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const param = params[0];
        const value = getNumberValue(param.value);
        return `${param.name}<br/>${param.seriesName}: ¥${value.toLocaleString()}`;
      },
    },
    xAxis: {
      type: 'category',
      data: trends.map(t => t.period),
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) return (value / 10000).toFixed(1) + '万';
          return value.toString();
        },
      },
    },
    series: [
      {
        name: '收入',
        type: 'line',
        data: trends.map(t => getNumberValue(t.amount)),
        smooth: true,
        areaStyle: {},
        itemStyle: {
          color: '#1890ff',
        },
      },
    ],
  });
}

// 渲染项目状态分布图
function renderMatterStatusChart() {
  if (!matterStatusChartRef.value || !matterStats.value) return;
  
  const chart = getOrCreateChart(matterStatusChartRef.value);
  if (!chart) return;
  const statusCount = matterStats.value.statusCount || {};
  
  chart.setOption({
    title: {
      text: '项目状态分布',
      left: 'center',
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        name: '项目状态',
        type: 'pie',
        radius: '50%',
        data: Object.entries(statusCount).map(([status, count]) => ({
          value: count,
          name: matterStatusMap[status] || status,
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  });
}

// 渲染项目类型分布图
function renderMatterTypeChart() {
  if (!matterTypeChartRef.value || !matterStats.value) return;
  
  const chart = getOrCreateChart(matterTypeChartRef.value);
  if (!chart) return;
  const typeCount = matterStats.value.typeCount || {};
  
  chart.setOption({
    title: {
      text: '项目类型分布',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    xAxis: {
      type: 'category',
      data: Object.entries(typeCount).map(([type]) => matterTypeMap[type] || type),
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: '项目数',
        type: 'bar',
        data: Object.values(typeCount),
        itemStyle: {
          color: '#52c41a',
        },
      },
    ],
  });
}

// 渲染客户类型分布图
function renderClientTypeChart() {
  if (!clientTypeChartRef.value || !clientStats.value) return;
  
  const chart = getOrCreateChart(clientTypeChartRef.value);
  if (!chart) return;
  const typeCount = clientStats.value.typeCount || {};
  
  chart.setOption({
    title: {
      text: '客户类型分布',
      left: 'center',
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        name: '客户类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '30',
            fontWeight: 'bold',
          },
        },
        data: Object.entries(typeCount).map(([type, count]) => ({
          value: count,
          name: clientTypeMap[type] || type,
        })),
      },
    ],
  });
}

// 渲染律师业绩排行图
function renderLawyerRankingChart() {
  if (!lawyerRankingChartRef.value || !lawyerRanking.value.length) return;
  
  const chart = getOrCreateChart(lawyerRankingChartRef.value);
  if (!chart) return;
  
  chart.setOption({
    title: {
      text: '律师业绩排行（Top 10）',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const param = params[0];
        const data = lawyerRanking.value[param.dataIndex];
        return `${data.lawyerName}<br/>
                收入: ¥${data.revenue.toLocaleString()}<br/>
                案件数: ${data.matterCount}<br/>
                工时: ${data.hours.toFixed(1)}小时`;
      },
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) return (value / 10000).toFixed(1) + '万';
          return value.toString();
        },
      },
    },
    yAxis: {
      type: 'category',
      data: lawyerRanking.value.map(l => l.lawyerName),
      inverse: true,
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: lawyerRanking.value.map(l => getNumberValue(l.revenue)),
        itemStyle: {
          color: '#faad14',
        },
      },
    ],
  });
}

// 加载所有统计数据
async function loadAllStats() {
  loading.value = true;
  try {
    await Promise.all([
      loadRevenueStats(),
      loadMatterStats(),
      loadClientStats(),
      loadLawyerRanking(),
    ]);
  } finally {
    loading.value = false;
  }
}

// 导出报表
async function handleExportReport(reportType: string) {
  try {
    const command: GenerateReportCommand = {
      reportType,
      format: 'EXCEL',
      parameters: {
        startDate: dateRange.value[0].format('YYYY-MM-DD'),
        endDate: dateRange.value[1].format('YYYY-MM-DD'),
      },
    };
    await generateReport(command);
    message.success('报表生成任务已提交，请稍后查看报表列表');
  } catch (error: any) {
    message.error('导出报表失败：' + (error.message || '未知错误'));
  }
}

// 格式化金额（处理BigDecimal类型）
function formatCurrency(amount: number | string | undefined | null | any): string {
  if (amount === undefined || amount === null) return '¥0.00';
  
  // 如果是对象（BigDecimal序列化后的结果），尝试提取值
  let numValue: number;
  if (typeof amount === 'object') {
    // BigDecimal序列化后可能是 {value: "123.45"} 或类似结构
    numValue = parseFloat(amount.toString() || '0');
  } else {
    numValue = Number(amount);
  }
  
  if (isNaN(numValue)) return '¥0.00';
  return '¥' + numValue.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// 获取数值（处理BigDecimal）
function getNumberValue(value: number | string | undefined | null | any): number {
  if (value === undefined || value === null) return 0;
  if (typeof value === 'object') {
    return parseFloat(value.toString() || '0');
  }
  return Number(value) || 0;
}

// 获取或创建图表实例（避免重复初始化）
function getOrCreateChart(dom: HTMLElement | null): echarts.ECharts | null {
  if (!dom) return null;
  
  // 检查是否已存在实例
  const existingChart = echarts.getInstanceByDom(dom);
  if (existingChart) {
    // 如果已存在，先销毁再创建新实例
    existingChart.dispose();
  }
  
  // 创建新实例
  return echarts.init(dom);
}

// Tab切换
function handleTabChange(key: string) {
  activeTab.value = key;
  if (key === 'overview') {
    setTimeout(() => {
      loadAllStats();
    }, 100);
  }
}

// 销毁所有图表实例
function disposeAllCharts() {
  const chartRefs = [
    revenueTrendChartRef.value,
    matterStatusChartRef.value,
    matterTypeChartRef.value,
    clientTypeChartRef.value,
    lawyerRankingChartRef.value,
  ];
  
  chartRefs.forEach((ref) => {
    if (ref) {
      const chart = echarts.getInstanceByDom(ref);
      if (chart) {
        chart.dispose();
      }
    }
  });
}

// 初始化
onMounted(() => {
  loadAllStats();
});

// 组件卸载时清理图表实例
onBeforeUnmount(() => {
  disposeAllCharts();
});
</script>

<template>
  <Page title="报表中心" description="查看各类业务数据统计图表，直观了解业务情况">
    <Spin :spinning="loading">
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <TabPane key="overview" tab="数据概览">
          <!-- 收入统计卡片 -->
          <Card title="收入统计" style="margin-bottom: 16px;">
            <Row :gutter="16">
              <Col :xs="24" :sm="12" :md="6">
                <Statistic
                  title="总收入"
                  :value="getNumberValue(revenueStats?.totalRevenue)"
                  :formatter="(val: number) => formatCurrency(val)"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Statistic
                  title="本月收入"
                  :value="getNumberValue(revenueStats?.monthlyRevenue)"
                  :formatter="(val: number) => formatCurrency(val)"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Statistic
                  title="本年收入"
                  :value="getNumberValue(revenueStats?.yearlyRevenue)"
                  :formatter="(val: number) => formatCurrency(val)"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Statistic
                  title="待收金额"
                  :value="getNumberValue(revenueStats?.pendingRevenue)"
                  :formatter="(val: number) => formatCurrency(val)"
                />
              </Col>
            </Row>
            <div ref="revenueTrendChartRef" style="width: 100%; height: 400px; margin-top: 24px;"></div>
          </Card>

          <!-- 项目统计卡片 -->
          <Card title="项目统计" style="margin-bottom: 16px;">
            <Row :gutter="16">
              <Col :xs="24" :sm="8">
                <Statistic title="总项目数" :value="matterStats?.totalMatters || 0" />
              </Col>
              <Col :xs="24" :sm="8">
                <Statistic title="进行中" :value="matterStats?.activeMatters || 0" />
              </Col>
              <Col :xs="24" :sm="8">
                <Statistic title="已完成" :value="matterStats?.completedMatters || 0" />
              </Col>
            </Row>
            <Row :gutter="16" style="margin-top: 24px;">
              <Col :xs="24" :sm="12">
                <div ref="matterStatusChartRef" style="width: 100%; height: 300px;"></div>
              </Col>
              <Col :xs="24" :sm="12">
                <div ref="matterTypeChartRef" style="width: 100%; height: 300px;"></div>
              </Col>
            </Row>
          </Card>

          <!-- 客户统计卡片 -->
          <Card title="客户统计" style="margin-bottom: 16px;">
            <Row :gutter="16">
              <Col :xs="24" :sm="6">
                <Statistic title="总客户数" :value="clientStats?.totalClients || 0" />
              </Col>
              <Col :xs="24" :sm="6">
                <Statistic title="正式客户" :value="clientStats?.formalClients || 0" />
              </Col>
              <Col :xs="24" :sm="6">
                <Statistic title="潜在客户" :value="clientStats?.potentialClients || 0" />
              </Col>
              <Col :xs="24" :sm="6">
                <Statistic title="本月新增" :value="clientStats?.newClientsThisMonth || 0" />
              </Col>
            </Row>
            <div ref="clientTypeChartRef" style="width: 100%; height: 400px; margin-top: 24px;"></div>
          </Card>

          <!-- 律师业绩排行 -->
          <Card title="律师业绩排行">
            <div ref="lawyerRankingChartRef" style="width: 100%; height: 400px;"></div>
          </Card>
        </TabPane>

        <TabPane key="export" tab="导出报表">
          <Card>
            <Space direction="vertical" style="width: 100%;" size="large">
              <div>
                <h3>选择时间范围</h3>
                <DatePicker.RangePicker
                  v-model:value="dateRange"
                  style="width: 100%; max-width: 400px;"
                />
              </div>
              <div>
                <h3>选择报表类型</h3>
                <Space wrap>
                  <Button @click="handleExportReport('REVENUE')">导出收入报表</Button>
                  <Button @click="handleExportReport('MATTER')">导出项目报表</Button>
                  <Button @click="handleExportReport('CLIENT')">导出客户报表</Button>
                  <Button @click="handleExportReport('LAWYER_PERFORMANCE')">导出律师业绩报表</Button>
                  <Button @click="handleExportReport('RECEIVABLE')">导出应收报表</Button>
                </Space>
              </div>
            </Space>
          </Card>
        </TabPane>
      </Tabs>
    </Spin>
  </Page>
</template>

<style scoped>
:deep(.ant-statistic-content) {
  font-size: 24px;
}
</style>
