<script setup lang="ts">
import type {
  AiDepartmentSummaryDTO,
  AiMonthlyBillDTO,
  AiUsageSummaryDTO,
  SalaryDeductionLinkCommand,
} from '#/api/ai/types';

import {
  generateMonthlyBills,
  getAllUsersSummary,
  getDepartmentSummary,
  getMonthlyBills,
  linkToSalaryDeduction,
  markBillDeducted,
  waiveBill,
} from '#/api/ai/usage';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'ant-design-vue';

defineOptions({ name: 'FinanceAiBilling' });

const loadingSummary = ref(false);
const loadingBills = ref(false);

const userSummaryList = ref<AiUsageSummaryDTO[]>([]);
const departmentSummaryList = ref<AiDepartmentSummaryDTO[]>([]);
const bills = ref<AiMonthlyBillDTO[]>([]);

const currentYear = new Date().getFullYear();
const query = reactive({
  year: currentYear,
  month: new Date().getMonth() + 1,
});

const yearOptions = Array.from({ length: 5 }, (_, i) => ({
  label: `${currentYear - i}年`,
  value: currentYear - i,
}));

const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  label: `${i + 1}月`,
  value: i + 1,
}));

const userColumns = [
  {
    title: '用户',
    dataIndex: 'userName',
    key: 'userName',
    width: 140,
  },
  {
    title: '部门',
    dataIndex: 'departmentName',
    key: 'departmentName',
    width: 140,
  },
  {
    title: '调用次数',
    dataIndex: 'totalCalls',
    key: 'totalCalls',
    width: 100,
  },
  {
    title: 'Token 数',
    dataIndex: 'totalTokens',
    key: 'totalTokens',
    width: 140,
  },
  {
    title: 'API 总费用',
    dataIndex: 'totalCost',
    key: 'totalCost',
    width: 140,
  },
  {
    title: '用户承担',
    dataIndex: 'userCost',
    key: 'userCost',
    width: 140,
  },
  {
    title: '单位承担',
    dataIndex: 'companyCost',
    key: 'companyCost',
    width: 140,
  },
];

const deptColumns = [
  {
    title: '部门',
    dataIndex: 'departmentName',
    key: 'departmentName',
    width: 160,
  },
  {
    title: '调用次数',
    dataIndex: 'totalCalls',
    key: 'totalCalls',
    width: 100,
  },
  {
    title: 'Token 数',
    dataIndex: 'totalTokens',
    key: 'totalTokens',
    width: 140,
  },
  {
    title: 'API 总费用',
    dataIndex: 'totalCost',
    key: 'totalCost',
    width: 140,
  },
  {
    title: '用户承担',
    dataIndex: 'userCost',
    key: 'userCost',
    width: 140,
  },
];

const billColumns = [
  {
    title: '用户',
    dataIndex: 'userName',
    key: 'userName',
    width: 140,
  },
  {
    title: '部门',
    dataIndex: 'departmentName',
    key: 'departmentName',
    width: 140,
  },
  {
    title: '调用次数',
    dataIndex: 'totalCalls',
    key: 'totalCalls',
    width: 90,
  },
  {
    title: 'Token 数',
    dataIndex: 'totalTokens',
    key: 'totalTokens',
    width: 120,
  },
  {
    title: 'API 总费用',
    dataIndex: 'totalCost',
    key: 'totalCost',
    width: 120,
  },
  {
    title: '用户应付',
    dataIndex: 'userCost',
    key: 'userCost',
    width: 120,
  },
  {
    title: '收费比例',
    dataIndex: 'chargeRatio',
    key: 'chargeRatio',
    width: 100,
  },
  {
    title: '扣减状态',
    dataIndex: 'deductionStatus',
    key: 'deductionStatus',
    width: 110,
  },
  {
    title: '扣减金额',
    dataIndex: 'deductionAmount',
    key: 'deductionAmount',
    width: 120,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right' as const,
  },
];

const selectedRowKeys = ref<number[]>([]);

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (number | string)[]) => {
    selectedRowKeys.value = keys as number[];
  },
}));

function formatMonth() {
  const mm = query.month.toString().padStart(2, '0');
  return `${query.year}-${mm}`;
}

async function fetchSummary() {
  loadingSummary.value = true;
  try {
    const month = formatMonth();
    const [users, depts] = await Promise.all([
      getAllUsersSummary(month),
      getDepartmentSummary(month),
    ]);
    userSummaryList.value = users;
    departmentSummaryList.value = depts;
  } catch (error: any) {
    message.error(error.message || '加载使用统计失败');
  } finally {
    loadingSummary.value = false;
  }
}

async function fetchBills() {
  loadingBills.value = true;
  try {
    const res = await getMonthlyBills(query.year, query.month);
    bills.value = res;
  } catch (error: any) {
    message.error(error.message || '加载账单失败');
  } finally {
    loadingBills.value = false;
  }
}

async function handleSearch() {
  await Promise.all([fetchSummary(), fetchBills()]);
}

async function handleGenerateBills() {
  Modal.confirm({
    title: '生成月度账单',
    content: `确认为 ${query.year} 年 ${query.month} 月生成账单吗？`,
    onOk: async () => {
      try {
        const count = await generateMonthlyBills(query.year, query.month);
        message.success(`生成账单 ${count} 条`);
        await fetchBills();
      } catch (error: any) {
        message.error(error.message || '生成账单失败');
      }
    },
  });
}

async function handleMarkDeducted(record: AiMonthlyBillDTO | Record<string, any>) {
  Modal.confirm({
    title: '标记已扣减',
    content: `确认标记 ${record.userName || ''} 的账单已扣减吗？`,
    onOk: async () => {
      try {
        await markBillDeducted(record.id, '');
        message.success('已标记为已扣减');
        await fetchBills();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

async function handleWaive(record: AiMonthlyBillDTO | Record<string, any>) {
  Modal.confirm({
    title: '减免费用',
    content: `确认减免 ${record.userName || ''} 的 AI 费用吗？`,
    onOk: async () => {
      try {
        await waiveBill(record.id, '');
        message.success('已减免');
        await fetchBills();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

async function handleBatchLinkSalary() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先选择要关联的账单');
    return;
  }
  const payload: SalaryDeductionLinkCommand = {
    billIds: selectedRowKeys.value,
    salaryYear: query.year,
    salaryMonth: query.month,
  };
  try {
    await linkToSalaryDeduction(payload);
    message.success('已关联工资扣减');
  } catch (error: any) {
    message.error(error.message || '关联工资扣减失败');
  }
}

onMounted(() => {
  handleSearch();
});
</script>

<template>
  <Page title="AI使用统计与账单管理">
    <Card class="mb-4">
      <Row :gutter="[16, 16]" align="middle">
        <Col :xs="12" :sm="6" :md="4" :lg="3">
          <Select
            v-model:value="query.year"
            :options="yearOptions"
            style="width: 100%"
            placeholder="年份"
          />
        </Col>
        <Col :xs="12" :sm="6" :md="4" :lg="3">
          <Select
            v-model:value="query.month"
            :options="monthOptions"
            style="width: 100%"
            placeholder="月份"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="16" :lg="18">
          <Space wrap>
            <Button type="primary" @click="handleSearch">查询</Button>
            <Button @click="handleGenerateBills">生成账单</Button>
            <Button
              type="dashed"
              :disabled="!selectedRowKeys.length"
              @click="handleBatchLinkSalary"
            >
              批量关联工资扣减
            </Button>
          </Space>
        </Col>
      </Row>
    </Card>

    <Row :gutter="[16, 16]">
      <Col :xs="24" :lg="12">
        <Card title="全员AI使用统计" :loading="loadingSummary">
          <Table
            :columns="userColumns"
            :data-source="userSummaryList"
            row-key="userId"
            :pagination="false"
            size="small"
            :scroll="{ x: 900 }"
          />
        </Card>
      </Col>
      <Col :xs="24" :lg="12">
        <Card title="按部门统计" :loading="loadingSummary">
          <Table
            :columns="deptColumns"
            :data-source="departmentSummaryList"
            row-key="departmentId"
            :pagination="false"
            size="small"
            :scroll="{ x: 680 }"
          />
        </Card>
      </Col>
    </Row>

    <Card
      title="月度AI费用账单"
      style="margin-top: 16px"
      :loading="loadingBills"
    >
      <Table
        :columns="billColumns"
        :data-source="bills"
        row-key="id"
        :pagination="false"
        :row-selection="rowSelection"
        size="small"
        :scroll="{ x: 1300 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'deductionStatus'">
            <Tag
              :color="
                record.deductionStatus === 'DEDUCTED'
                  ? 'green'
                  : record.deductionStatus === 'WAIVED'
                    ? 'blue'
                    : 'orange'
              "
            >
              {{ record.deductionStatus }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'chargeRatio'">
            {{ record.chargeRatio }}%
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button
                type="link"
                size="small"
                :disabled="record.deductionStatus === 'DEDUCTED'"
                @click="handleMarkDeducted(record)"
              >
                标记已扣减
              </Button>
              <Button
                type="link"
                size="small"
                :disabled="record.deductionStatus === 'WAIVED'"
                @click="handleWaive(record)"
              >
                减免
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
