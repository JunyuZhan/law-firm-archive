<script setup lang="ts">
import type {
  AiModelUsageDTO,
  AiUsageLogDTO,
  AiUsageSummaryDTO,
} from '#/api/ai/types';

import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import echarts from '@vben/plugins/echarts';

import {
  Card,
  Col,
  message,
  Progress,
  Row,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getMyUsageByModel,
  getMyUsageLogs,
  getMyUsageSummary,
} from '#/api/ai/usage';

defineOptions({ name: 'MyAiUsage' });

const summary = ref<AiUsageSummaryDTO>({
  userId: 0,
  month: '',
  totalCalls: 0,
  totalTokens: 0,
  promptTokens: 0,
  completionTokens: 0,
  totalCost: 0,
  userCost: 0,
  companyCost: 0,
  monthlyTokenQuota: null,
  monthlyCostQuota: null,
  tokenUsagePercent: null,
  costUsagePercent: null,
});

const usageLogs = ref<AiUsageLogDTO[]>([]);
const modelUsage = ref<AiModelUsageDTO[]>([]);
const loading = ref(false);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const month = ref<string>(dayjs().format('YYYY-MM'));

const columns = [
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 160 },
  {
    title: '请求类型',
    dataIndex: 'requestType',
    key: 'requestType',
    width: 140,
  },
  {
    title: '业务类型',
    dataIndex: 'businessType',
    key: 'businessType',
    width: 120,
  },
  {
    title: 'Token 数',
    dataIndex: 'totalTokens',
    key: 'totalTokens',
    width: 120,
  },
  { title: '费用', key: 'cost', width: 180 },
  { title: '状态', dataIndex: 'success', key: 'success', width: 100 },
];

const modelColumns = [
  { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 160 },
  {
    title: '调用次数',
    dataIndex: 'totalCalls',
    key: 'totalCalls',
    width: 120,
  },
  {
    title: 'Token 数',
    dataIndex: 'totalTokens',
    key: 'totalTokens',
    width: 140,
  },
  {
    title: '总费用',
    dataIndex: 'totalCost',
    key: 'totalCost',
    width: 140,
  },
  {
    title: '我承担',
    dataIndex: 'userCost',
    key: 'userCost',
    width: 140,
  },
];

const chartRef = ref<HTMLDivElement>();
const pieChartRef = ref<HTMLDivElement>();

function buildTrendData() {
  const map = new Map<
    string,
    { calls: number; tokens: number; userCost: number }
  >();
  usageLogs.value.forEach((item) => {
    if (!item.createdAt) return;
    const date = item.createdAt.slice(0, 10);
    const stat = map.get(date) || {
      calls: 0,
      tokens: 0,
      userCost: 0,
    };
    stat.calls += 1;
    stat.tokens += item.totalTokens || 0;
    stat.userCost += Number(item.userCost || 0);
    map.set(date, stat);
  });
  const dates = [...map.keys()].toSorted();
  const calls = dates.map((d) => map.get(d)!.calls);
  const tokens = dates.map((d) => map.get(d)!.tokens);
  const costs = dates.map((d) => map.get(d)!.userCost);
  return { dates, calls, tokens, costs };
}

function renderTrendChart() {
  if (!chartRef.value) return;
  const chart =
    echarts.getInstanceByDom(chartRef.value) ||
    echarts.init(chartRef.value as HTMLDivElement);
  const { dates, tokens, costs } = buildTrendData();
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['Token 数', '费用'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      {
        type: 'value',
        name: 'Token',
      },
      {
        type: 'value',
        name: '费用(元)',
        axisLabel: { formatter: (v: number) => `¥${v.toFixed(2)}` },
      },
    ],
    series: [
      {
        name: 'Token 数',
        type: 'line',
        data: tokens,
        smooth: true,
        yAxisIndex: 0,
      },
      {
        name: '费用',
        type: 'bar',
        data: costs,
        yAxisIndex: 1,
      },
    ],
  });
}

function renderPieChart() {
  if (!pieChartRef.value) return;
  const chart =
    echarts.getInstanceByDom(pieChartRef.value) ||
    echarts.init(pieChartRef.value as HTMLDivElement);
  const data = modelUsage.value.map((item) => ({
    name: item.modelName || item.integrationCode || '未知模型',
    value: item.totalCost || 0,
  }));
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: '60%',
        center: ['50%', '45%'],
        data,
        label: { formatter: '{b}\n{d}%' },
      },
    ],
  });
}

function disposeCharts() {
  [chartRef.value, pieChartRef.value].forEach((el) => {
    if (!el) return;
    const chart = echarts.getInstanceByDom(el);
    if (chart) {
      chart.dispose();
    }
  });
}

async function fetchSummary() {
  try {
    const res = await getMyUsageSummary(month.value);
    summary.value = {
      ...summary.value,
      ...res,
    };
  } catch (error: any) {
    message.error(error.message || '加载使用统计失败');
  }
}

async function fetchUsageLogs() {
  loading.value = true;
  try {
    const m = dayjs(month.value, 'YYYY-MM');
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      createdAtFrom: m.startOf('month').format('YYYY-MM-DD 00:00:00'),
      createdAtTo: m.endOf('month').format('YYYY-MM-DD 23:59:59'),
    };
    const res = await getMyUsageLogs(params);
    usageLogs.value = res?.list ?? [];
    pagination.total = res?.total ?? 0;
  } catch (error: any) {
    message.error(error.message || '加载使用记录失败');
  } finally {
    loading.value = false;
  }
}

async function fetchModelUsage() {
  try {
    const res = await getMyUsageByModel(month.value);
    modelUsage.value = res;
  } catch (error: any) {
    message.error(error.message || '加载模型统计失败');
  }
}

function handleTableChange(p: any) {
  pagination.current = p.current;
  pagination.pageSize = p.pageSize;
  fetchUsageLogs();
}

async function refreshAll() {
  await Promise.all([fetchSummary(), fetchUsageLogs(), fetchModelUsage()]);
  renderTrendChart();
  renderPieChart();
}

// 监听数据变化渲染图表（数据整体替换时触发，无需 deep）
watch(
  () => usageLogs.value,
  () => {
    renderTrendChart();
  },
);

watch(
  () => modelUsage.value,
  () => {
    renderPieChart();
  },
);

onMounted(() => {
  refreshAll();
});

onBeforeUnmount(() => {
  disposeCharts();
});
</script>

<template>
  <Page title="我的AI使用">
    <Row :gutter="[16, 16]" class="summary-cards">
      <Col :xs="24" :sm="12" :lg="6">
        <Card>
          <Statistic
            title="本月调用次数"
            :value="summary.totalCalls"
            suffix="次"
          />
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :lg="6">
        <Card>
          <Statistic
            title="本月消耗Token"
            :value="summary.totalTokens"
            :precision="0"
            suffix="tokens"
          />
          <Progress
            v-if="summary.tokenUsagePercent"
            :percent="summary.tokenUsagePercent"
            :status="summary.tokenUsagePercent > 90 ? 'exception' : 'normal'"
            size="small"
          />
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :lg="6">
        <Card>
          <Statistic
            title="本月费用(我承担)"
            :value="summary.userCost"
            :precision="2"
            prefix="¥"
          />
          <div class="cost-detail">
            <span>总费用: ¥{{ summary.totalCost }}</span>
            <span>单位承担: ¥{{ summary.companyCost }}</span>
          </div>
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :lg="6">
        <Card>
          <Statistic
            title="费用配额"
            :value="summary.monthlyCostQuota || '无限制'"
            :precision="2"
            :prefix="summary.monthlyCostQuota ? '¥' : ''"
          />
          <Progress
            v-if="summary.costUsagePercent"
            :percent="summary.costUsagePercent"
            :status="summary.costUsagePercent > 90 ? 'exception' : 'normal'"
            size="small"
          />
        </Card>
      </Col>
    </Row>

    <Card title="使用趋势" style="margin-top: 16px">
      <div ref="chartRef" style="height: 300px"></div>
    </Card>

    <Row :gutter="[16, 16]" style="margin-top: 16px">
      <Col :xs="24" :lg="12">
        <Card title="模型使用分布">
          <div ref="pieChartRef" style="height: 250px"></div>
        </Card>
      </Col>
      <Col :xs="24" :lg="12">
        <Card title="费用明细">
          <Table
            :columns="modelColumns"
            :data-source="modelUsage"
            size="small"
            :pagination="false"
            row-key="modelName"
            :scroll="{ x: 700 }"
          />
        </Card>
      </Col>
    </Row>

    <Card title="使用记录" style="margin-top: 16px">
      <Table
        :columns="columns"
        :data-source="usageLogs"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'success'">
            <Tag :color="record.success ? 'green' : 'red'">
              {{ record.success ? '成功' : '失败' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'cost'">
            <span>¥{{ record.totalCost }}</span>
            <span class="cost-breakdown">
              (我承担: ¥{{ record.userCost }})
            </span>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>

<style scoped>
/* 移动端适配 */
@media (max-width: 576px) {
  .summary-cards :deep(.ant-statistic-title) {
    font-size: 12px;
  }

  .summary-cards :deep(.ant-statistic-content) {
    font-size: 20px;
  }

  .cost-detail {
    flex-flow: row wrap;
    gap: 12px;
  }
}

.cost-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.cost-breakdown {
  font-size: 12px;
  color: #999;
}
</style>
