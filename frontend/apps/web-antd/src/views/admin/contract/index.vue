<script setup lang="ts">
import type { AdminContractQueryDTO, AdminContractViewDTO } from '#/api/admin';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  downloadContractListExcel,
  downloadJudicialFilingExcel,
  getAdminContractDetail,
  getAdminContractList,
} from '#/api/admin';
import { findCauseNameInAll } from '#/composables/useCauseOfAction';

defineOptions({ name: 'AdminContract' });

// 响应式布局
const { isMobile } = useResponsive();

// 状态
const loading = ref(false);
const exportLoading = ref(false);
const exportListLoading = ref(false);
const contracts = ref<AdminContractViewDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const contractDetail = ref<AdminContractViewDTO | null>(null);

// 查询参数
const queryParams = reactive<AdminContractQueryDTO>({
  contractNo: '',
  clientName: '',
  caseType: undefined,
  leadLawyerId: undefined,
  signDateFrom: undefined,
  signDateTo: undefined,
  pageNum: 1,
  pageSize: 15,
});

// 导出参数
const exportParams = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
});

// 统计数据
const stats = computed(() => {
  const civil = contracts.value.filter((c) => c.caseType === 'CIVIL').length;
  const criminal = contracts.value.filter(
    (c) => c.caseType === 'CRIMINAL',
  ).length;
  const administrative = contracts.value.filter(
    (c) => c.caseType === 'ADMINISTRATIVE',
  ).length;
  const legalCounsel = contracts.value.filter(
    (c) => c.caseType === 'LEGAL_COUNSEL',
  ).length;
  const other =
    contracts.value.length - civil - criminal - administrative - legalCounsel;
  return {
    civil,
    criminal,
    administrative,
    legalCounsel,
    other,
    total: total.value,
  };
});

// 表格列
const columns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 140 },
  {
    title: '合同名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
    ellipsis: true,
  },
  {
    title: '委托人',
    dataIndex: 'clientName',
    key: 'clientName',
    width: 150,
    ellipsis: true,
  },
  {
    title: '对方当事人',
    dataIndex: 'opposingParty',
    key: 'opposingParty',
    width: 150,
    ellipsis: true,
  },
  {
    title: '案件类型',
    dataIndex: 'caseTypeName',
    key: 'caseTypeName',
    width: 100,
  },
  {
    title: '案由',
    dataIndex: 'causeOfAction',
    key: 'causeOfAction',
    width: 150,
    ellipsis: true,
  },
  {
    title: '承办律师',
    dataIndex: 'leadLawyerName',
    key: 'leadLawyerName',
    width: 100,
  },
  { title: '律师费', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '签约日期', dataIndex: 'signDate', key: 'signDate', width: 110 },
  {
    title: '管辖法院',
    dataIndex: 'jurisdictionCourt',
    key: 'jurisdictionCourt',
    width: 150,
    ellipsis: true,
  },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
];

// 案件类型选项
const caseTypeOptions = [
  { label: '全部', value: undefined },
  { label: '民事', value: 'CIVIL' },
  { label: '刑事', value: 'CRIMINAL' },
  { label: '行政', value: 'ADMINISTRATIVE' },
  { label: '破产', value: 'BANKRUPTCY' },
  { label: '知识产权', value: 'IP' },
  { label: '仲裁', value: 'ARBITRATION' },
  { label: '执行', value: 'ENFORCEMENT' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '专项服务', value: 'SPECIAL_SERVICE' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getAdminContractList(queryParams);
    contracts.value = res.records || [];
    total.value = res.total || 0;
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryParams.contractNo = '';
  queryParams.clientName = '';
  queryParams.caseType = undefined;
  queryParams.leadLawyerId = undefined;
  queryParams.signDateFrom = undefined;
  queryParams.signDateTo = undefined;
  queryParams.pageNum = 1;
  fetchData();
}

// 分页变化
function handleTableChange(pagination: any) {
  queryParams.pageNum = pagination.current;
  queryParams.pageSize = pagination.pageSize;
  fetchData();
}

// 获取审理阶段名称
function getTrialStageName(stage: string | undefined): string {
  if (!stage) return '-';
  const stageMap: Record<string, string> = {
    FIRST_INSTANCE: '一审',
    SECOND_INSTANCE: '二审',
    RETRIAL: '再审',
    EXECUTION: '执行',
    ARBITRATION: '仲裁',
    NON_LITIGATION: '非诉',
  };
  return stageMap[stage] || stage;
}

// 获取案由名称
function getCauseOfActionName(code: string | undefined): string {
  if (!code) return '-';
  // 使用前端案由数据查找名称
  const name = findCauseNameInAll(code);
  return name || code;
}

// 查看详情
async function handleView(record: AdminContractViewDTO) {
  try {
    const detail = await getAdminContractDetail(record.id);
    contractDetail.value = detail;
    modalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 导出司法局报备
async function handleExport() {
  if (!exportParams.year || !exportParams.month) {
    message.warning('请选择导出年月');
    return;
  }

  exportLoading.value = true;
  try {
    await downloadJudicialFilingExcel(exportParams.year, exportParams.month);
    message.success('导出成功');
  } catch (error: any) {
    message.error(error.message || '导出失败');
  } finally {
    exportLoading.value = false;
  }
}

// 导出合同列表（根据当前查询条件）
async function handleExportList() {
  exportListLoading.value = true;
  try {
    await downloadContractListExcel(queryParams);
    message.success('导出成功');
  } catch (error: any) {
    message.error(error.message || '导出失败');
  } finally {
    exportListLoading.value = false;
  }
}

// 格式化金额
function formatAmount(amount: number) {
  if (amount === null || amount === undefined) return '-';
  return `¥${amount.toLocaleString()}`;
}

// 日期范围变化
function handleDateRangeChange(dates: any) {
  if (dates && dates.length === 2) {
    queryParams.signDateFrom = dates[0]?.format('YYYY-MM-DD');
    queryParams.signDateTo = dates[1]?.format('YYYY-MM-DD');
  } else {
    queryParams.signDateFrom = undefined;
    queryParams.signDateTo = undefined;
  }
}

onMounted(() => {
  fetchData();
});
</script>

<script lang="ts">
// 获取案件类型颜色
function getCaseTypeColor(caseType: string) {
  const colorMap: Record<string, string> = {
    CIVIL: 'blue',
    CRIMINAL: 'red',
    ADMINISTRATIVE: 'orange',
    BANKRUPTCY: 'purple',
    IP: 'cyan',
    ARBITRATION: 'geekblue',
    ENFORCEMENT: 'volcano',
    LEGAL_COUNSEL: 'green',
    SPECIAL_SERVICE: 'lime',
  };
  return colorMap[caseType] || 'default';
}
</script>

<template>
  <Page
    title="合同查询"
    description="查看已审批通过的合同信息，用于司法局报备等"
  >
    <!-- 统计卡片 -->
    <Row :gutter="[16, 16]" style="margin-bottom: 16px">
      <Col :xs="8" :sm="8" :md="4" :lg="4">
        <Card size="small">
          <Statistic
            title="民事案件"
            :value="stats.civil"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>
      </Col>
      <Col :xs="8" :sm="8" :md="4" :lg="4">
        <Card size="small">
          <Statistic
            title="刑事案件"
            :value="stats.criminal"
            :value-style="{ color: '#f5222d' }"
          />
        </Card>
      </Col>
      <Col :xs="8" :sm="8" :md="4" :lg="4">
        <Card size="small">
          <Statistic
            title="行政案件"
            :value="stats.administrative"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
      <Col :xs="8" :sm="8" :md="4" :lg="4">
        <Card size="small">
          <Statistic
            title="法律顾问"
            :value="stats.legalCounsel"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="8" :sm="8" :md="4" :lg="4">
        <Card size="small">
          <Statistic
            title="其他"
            :value="stats.other"
            :value-style="{ color: '#666' }"
          />
        </Card>
      </Col>
      <Col :xs="8" :sm="8" :md="4" :lg="4">
        <Card size="small">
          <Statistic title="总计" :value="stats.total" />
        </Card>
      </Col>
    </Row>

    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 12]">
          <Col :xs="12" :sm="12" :md="4" :lg="4">
            <Input
              v-model:value="queryParams.contractNo"
              placeholder="合同编号"
              allow-clear
            />
          </Col>
          <Col :xs="12" :sm="12" :md="4" :lg="4">
            <Input
              v-model:value="queryParams.clientName"
              placeholder="委托人名称"
              allow-clear
            />
          </Col>
          <Col :xs="12" :sm="12" :md="3" :lg="3">
            <Select
              v-model:value="queryParams.caseType"
              placeholder="案件类型"
              allow-clear
              style="width: 100%"
              :options="caseTypeOptions"
            />
          </Col>
          <Col :xs="12" :sm="12" :md="5" :lg="5">
            <DatePicker.RangePicker
              style="width: 100%"
              :placeholder="['签约开始', '签约结束']"
              @change="handleDateRangeChange"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="8" :lg="8">
            <Space :wrap="isMobile">
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button :loading="exportListLoading" @click="handleExportList">
                导出Excel
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 导出栏 -->
      <div
        style="
          padding: 12px;
          margin-bottom: 16px;
          background: #fafafa;
          border-radius: 4px;
        "
      >
        <Row :gutter="[16, 12]" align="middle">
          <Col :xs="24" :sm="24" :md="2" :lg="2">
            <span style="font-weight: 500">导出合同：</span>
          </Col>
          <Col :xs="8" :sm="8" :md="3" :lg="3">
            <InputNumber
              v-model:value="exportParams.year"
              :min="2020"
              :max="2030"
              placeholder="年份"
              style="width: 100%"
            />
          </Col>
          <Col :xs="8" :sm="8" :md="3" :lg="3">
            <InputNumber
              v-model:value="exportParams.month"
              :min="1"
              :max="12"
              placeholder="月份"
              style="width: 100%"
            />
          </Col>
          <Col :xs="8" :sm="8" :md="4" :lg="4">
            <Button
              type="primary"
              :loading="exportLoading"
              @click="handleExport"
            >
              导出Excel
            </Button>
          </Col>
        </Row>
      </div>

      <Table
        :columns="columns"
        :data-source="contracts"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showTotal: (t: number) => `共 ${t} 条`,
          showSizeChanger: true,
        }"
        row-key="id"
        :scroll="{ x: isMobile ? 1000 : 1500 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount'">
            {{ formatAmount((record as AdminContractViewDTO).totalAmount) }}
          </template>
          <template v-if="column.key === 'caseTypeName'">
            <Tag
              :color="
                getCaseTypeColor((record as AdminContractViewDTO).caseType)
              "
            >
              {{ (record as AdminContractViewDTO).caseTypeName }}
            </Tag>
          </template>
          <template v-if="column.key === 'causeOfAction'">
            {{
              getCauseOfActionName(
                (record as AdminContractViewDTO).causeOfAction,
              )
            }}
          </template>
          <template v-if="column.key === 'action'">
            <a @click="handleView(record as AdminContractViewDTO)">查看</a>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 查看详情弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="合同详情"
      :width="isMobile ? '100%' : '800px'"
      :centered="isMobile"
      :footer="null"
    >
      <Descriptions v-if="contractDetail" :column="2" bordered>
        <Descriptions.Item label="合同编号" :span="1">
          {{ contractDetail.contractNo || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="合同名称" :span="1">
          {{ contractDetail.name || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="委托人" :span="1">
          {{ contractDetail.clientName || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="对方当事人" :span="1">
          {{ contractDetail.opposingParty || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="案件类型" :span="1">
          {{ contractDetail.caseTypeName || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="案由" :span="1">
          {{ getCauseOfActionName(contractDetail.causeOfAction) }}
        </Descriptions.Item>
        <Descriptions.Item label="承办律师" :span="1">
          {{ contractDetail.leadLawyerName || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="律师费" :span="1">
          {{ formatAmount(contractDetail.totalAmount) }}
        </Descriptions.Item>
        <Descriptions.Item label="签约日期" :span="1">
          {{ contractDetail.signDate || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="管辖法院" :span="1">
          {{ contractDetail.jurisdictionCourt || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="审理阶段" :span="1">
          {{ getTrialStageName(contractDetail.trialStage) }}
        </Descriptions.Item>
      </Descriptions>
    </Modal>
  </Page>
</template>
