<script setup lang="ts">
import type { AiModelUsageDTO, AiUsageSummaryDTO } from '#/api/ai/types';

import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  Alert,
  Button,
  Card,
  Col,
  message,
  Progress,
  Row,
  Statistic,
  Table,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getMyUsageByModel, getMyUsageSummary } from '#/api/ai/usage';

const router = useRouter();

const loading = ref(false);

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

const modelUsage = ref<AiModelUsageDTO[]>([]);

const month = dayjs().format('YYYY-MM');

const modelColumns = [
  { title: '模型', dataIndex: 'modelName', key: 'modelName' },
  { title: '调用次数', dataIndex: 'totalCalls', key: 'totalCalls' },
  { title: 'Token 数', dataIndex: 'totalTokens', key: 'totalTokens' },
  { title: '总费用', dataIndex: 'totalCost', key: 'totalCost' },
  { title: '我承担', dataIndex: 'userCost', key: 'userCost' },
];

async function loadData() {
  loading.value = true;
  try {
    const [summaryRes, modelRes] = await Promise.all([
      getMyUsageSummary(month),
      getMyUsageByModel(month),
    ]);
    summary.value = summaryRes;
    modelUsage.value = modelRes;
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function goToDetail() {
  router.push('/personal/ai-usage');
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="ai-usage-setting">
    <Alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="AI使用统计"
      description="以下是您本月的AI使用情况概览。系统会根据您的AI调用自动计费，费用将在月末汇总。"
    />

    <Row :gutter="[16, 16]">
      <Col :xs="24" :sm="12" :md="6">
        <Card :loading="loading">
          <Statistic
            title="本月调用次数"
            :value="summary.totalCalls"
            suffix="次"
          />
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <Card :loading="loading">
          <Statistic
            title="Token 消耗"
            :value="summary.totalTokens"
            :precision="0"
          />
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <Card :loading="loading">
          <Statistic
            title="API 总费用"
            :value="summary.totalCost"
            :precision="4"
            prefix="¥"
          />
        </Card>
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <Card :loading="loading">
          <Statistic
            title="我承担费用"
            :value="summary.userCost"
            :precision="4"
            prefix="¥"
            :value-style="{ color: '#cf1322' }"
          />
        </Card>
      </Col>
    </Row>

    <Card
      v-if="summary.monthlyTokenQuota || summary.monthlyCostQuota"
      title="配额使用"
      style="margin-top: 16px"
      :loading="loading"
    >
      <Row :gutter="[16, 16]">
        <Col v-if="summary.monthlyTokenQuota" :xs="24" :sm="12">
          <div class="quota-item">
            <span>Token 配额</span>
            <Progress
              :percent="summary.tokenUsagePercent || 0"
              :status="
                (summary.tokenUsagePercent || 0) > 90 ? 'exception' : 'active'
              "
            />
            <span class="quota-text">
              {{ summary.totalTokens?.toLocaleString() }} /
              {{ summary.monthlyTokenQuota?.toLocaleString() }}
            </span>
          </div>
        </Col>
        <Col v-if="summary.monthlyCostQuota" :xs="24" :sm="12">
          <div class="quota-item">
            <span>费用配额</span>
            <Progress
              :percent="summary.costUsagePercent || 0"
              :status="
                (summary.costUsagePercent || 0) > 90 ? 'exception' : 'active'
              "
            />
            <span class="quota-text">
              ¥{{ summary.userCost?.toFixed(2) }} / ¥{{
                summary.monthlyCostQuota?.toFixed(2)
              }}
            </span>
          </div>
        </Col>
      </Row>
    </Card>

    <Card title="按模型统计" style="margin-top: 16px" :loading="loading">
      <Table
        :columns="modelColumns"
        :data-source="modelUsage"
        row-key="modelName"
        :pagination="false"
        size="small"
        :scroll="{ x: 600 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalCost'">
            ¥{{ Number(record.totalCost || 0).toFixed(4) }}
          </template>
          <template v-else-if="column.key === 'userCost'">
            ¥{{ Number(record.userCost || 0).toFixed(4) }}
          </template>
        </template>
      </Table>
    </Card>

    <div style="margin-top: 16px; text-align: center">
      <Button type="primary" @click="goToDetail"> 查看详细使用记录 </Button>
    </div>
  </div>
</template>

<style scoped>
.ai-usage-setting {
  padding: 16px;
}

.quota-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quota-text {
  font-size: 12px;
  color: #666;
}
</style>
