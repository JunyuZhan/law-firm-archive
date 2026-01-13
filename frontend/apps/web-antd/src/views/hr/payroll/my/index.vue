<script setup lang="ts">
import type {
  ConfirmPayrollCommand,
  PayrollItemDTO,
  PayrollSheetDTO,
} from '#/api/hr/payroll';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Divider,
  Empty,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  confirmPayrollItem,
  getMyPayrollSheetDetail,
  getMyPayrollSheets,
} from '#/api/hr/payroll';

defineOptions({ name: 'MyPayroll' });

// 状态
const loading = ref(false);
const dataSource = ref<PayrollSheetDTO[]>([]);
const detailModalVisible = ref(false);
const confirmModalVisible = ref(false);
const currentSheet = ref<null | PayrollSheetDTO>(null);
const currentItem = ref<null | PayrollItemDTO>(null);
const confirmForm = reactive<ConfirmPayrollCommand>({
  payrollItemId: 0,
  confirmStatus: 'CONFIRMED',
  confirmComment: '',
});

// 查询参数
const queryParams = reactive({
  year: new Date().getFullYear(),
  month: undefined as number | undefined,
});

// 年份选项（最近5年）
const currentYear = new Date().getFullYear();
const yearOptions = Array.from({ length: 5 }, (_, i) => ({
  label: `${currentYear - i}年`,
  value: currentYear - i,
}));

// 月份选项
const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  label: `${i + 1}月`,
  value: i + 1,
}));

// 表格列
const columns = [
  { title: '工资表编号', dataIndex: 'payrollNo', key: 'payrollNo', width: 150 },
  {
    title: '年月',
    key: 'yearMonth',
    width: 120,
    customRender: ({ record }: { record: PayrollSheetDTO }) =>
      `${record.payrollYear}年${record.payrollMonth}月`,
  },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  {
    title: '应发工资',
    dataIndex: 'totalGrossAmount',
    key: 'totalGrossAmount',
    width: 120,
    customRender: ({ record }: { record: PayrollSheetDTO }) => {
      const item = record.items?.[0];
      return formatCurrency(item?.grossAmount);
    },
  },
  {
    title: '扣减总额',
    dataIndex: 'totalDeductionAmount',
    key: 'totalDeductionAmount',
    width: 120,
    customRender: ({ record }: { record: PayrollSheetDTO }) => {
      const item = record.items?.[0];
      return formatCurrency(item?.deductionAmount);
    },
  },
  {
    title: '实发工资',
    dataIndex: 'totalNetAmount',
    key: 'totalNetAmount',
    width: 120,
    customRender: ({ record }: { record: PayrollSheetDTO }) => {
      const item = record.items?.[0];
      return formatCurrency(item?.netAmount);
    },
  },
  {
    title: '确认状态',
    key: 'confirmStatus',
    width: 100,
    customRender: ({ record }: { record: PayrollSheetDTO }) => {
      const item = record.items?.[0];
      return item ? item.confirmStatusName : '-';
    },
  },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 格式化货币
function formatCurrency(amount: number | string | undefined): string {
  if (amount === undefined || amount === null) return '¥0.00';
  const num = typeof amount === 'string' ? Number.parseFloat(amount) : amount;
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getMyPayrollSheets(queryParams.year, queryParams.month);
    dataSource.value = res;
  } catch (error: any) {
    message.error(error.message || '加载我的工资列表失败');
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  fetchData();
}

// 重置
function handleReset() {
  queryParams.year = currentYear;
  queryParams.month = undefined;
  fetchData();
}

// 查看详情
async function handleView(record: Record<string, any>) {
  try {
    const res = await getMyPayrollSheetDetail(record.id!);
    currentSheet.value = res;
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '加载工资明细失败');
  }
}

// 确认工资
function handleConfirm(item: PayrollItemDTO) {
  currentItem.value = item;
  confirmForm.payrollItemId = item.id!;
  confirmForm.confirmStatus = 'CONFIRMED';
  confirmForm.confirmComment = '';
  confirmModalVisible.value = true;
}

// 拒绝工资
function handleReject(item: PayrollItemDTO) {
  currentItem.value = item;
  confirmForm.payrollItemId = item.id!;
  confirmForm.confirmStatus = 'REJECTED';
  confirmForm.confirmComment = '';
  confirmModalVisible.value = true;
}

// 提交确认
async function handleSubmitConfirm() {
  if (confirmForm.confirmStatus === 'REJECTED' && !confirmForm.confirmComment) {
    message.warning('拒绝时必须填写原因');
    return;
  }

  try {
    await confirmPayrollItem(confirmForm);
    message.success(
      confirmForm.confirmStatus === 'CONFIRMED' ? '确认成功' : '已拒绝',
    );
    confirmModalVisible.value = false;
    fetchData();
    if (detailModalVisible.value && currentSheet.value) {
      await handleView(currentSheet.value);
    }
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page>
    <Card>
      <!-- 搜索表单 -->
      <div class="mb-4">
        <Row :gutter="[16, 16]">
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="queryParams.year"
              placeholder="年份"
              style="width: 100%"
              :options="yearOptions"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="queryParams.month"
              placeholder="月份"
              style="width: 100%"
              :options="monthOptions"
              allow-clear
            />
          </Col>
          <Col :xs="24" :sm="8" :md="16" :lg="18">
            <Space wrap>
              <Button type="primary" @click="handleSearch">搜索</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 表格 -->
      <Table
        v-if="dataSource.length > 0"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag
              :color="
                record.status === 'DRAFT'
                  ? 'default'
                  : record.status === 'PENDING_CONFIRM'
                    ? 'orange'
                    : record.status === 'CONFIRMED'
                      ? 'blue'
                      : record.status === 'FINANCE_CONFIRMED'
                        ? 'green'
                        : 'success'
              "
            >
              {{ record.statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'confirmStatus'">
            <Tag
              :color="
                record.items?.[0]?.confirmStatus === 'PENDING'
                  ? 'orange'
                  : record.items?.[0]?.confirmStatus === 'CONFIRMED'
                    ? 'green'
                    : 'red'
              "
            >
              {{ record.items?.[0]?.confirmStatusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleView(record)">
                查看
              </Button>
              <Button
                v-if="
                  record.items?.[0]?.confirmStatus === 'PENDING' &&
                  record.status === 'PENDING_CONFIRM'
                "
                type="link"
                size="small"
                @click="handleConfirm(record.items![0])"
              >
                确认
              </Button>
              <Button
                v-if="
                  record.items?.[0]?.confirmStatus === 'PENDING' &&
                  record.status === 'PENDING_CONFIRM'
                "
                type="link"
                size="small"
                danger
                @click="handleReject(record.items![0])"
              >
                拒绝
              </Button>
            </Space>
          </template>
        </template>
      </Table>
      <Empty v-else description="暂无工资记录" />
    </Card>

    <!-- 工资明细弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="我的工资明细"
      width="1000px"
      :footer="null"
    >
      <div
        v-if="
          currentSheet && currentSheet.items && currentSheet.items.length > 0
        "
      >
        <Descriptions :column="3" bordered>
          <DescriptionsItem label="工资表编号">
            {{ currentSheet.payrollNo }}
          </DescriptionsItem>
          <DescriptionsItem label="年月">
            {{ currentSheet.payrollYear }}年{{ currentSheet.payrollMonth }}月
          </DescriptionsItem>
          <DescriptionsItem label="状态">
            <Tag
              :color="
                currentSheet.status === 'DRAFT'
                  ? 'default'
                  : currentSheet.status === 'PENDING_CONFIRM'
                    ? 'orange'
                    : currentSheet.status === 'CONFIRMED'
                      ? 'blue'
                      : currentSheet.status === 'FINANCE_CONFIRMED'
                        ? 'green'
                        : 'success'
              "
            >
              {{ currentSheet.statusName }}
            </Tag>
          </DescriptionsItem>
        </Descriptions>

        <Divider>我的工资明细</Divider>
        <div v-for="item in currentSheet.items" :key="item.id">
          <Descriptions :column="2" bordered>
            <DescriptionsItem label="工号">
              {{ item.employeeNo }}
            </DescriptionsItem>
            <DescriptionsItem label="姓名">
              {{ item.employeeName }}
            </DescriptionsItem>
            <DescriptionsItem label="应发工资">
              {{ formatCurrency(item.grossAmount) }}
            </DescriptionsItem>
            <DescriptionsItem label="扣减总额">
              {{ formatCurrency(item.deductionAmount) }}
            </DescriptionsItem>
            <DescriptionsItem label="实发工资" :span="2">
              <span class="text-lg font-bold text-primary">{{
                formatCurrency(item.netAmount)
              }}</span>
            </DescriptionsItem>
            <DescriptionsItem label="确认状态" :span="2">
              <Tag
                :color="
                  item.confirmStatus === 'PENDING'
                    ? 'orange'
                    : item.confirmStatus === 'CONFIRMED'
                      ? 'green'
                      : 'red'
                "
              >
                {{ item.confirmStatusName }}
              </Tag>
            </DescriptionsItem>
          </Descriptions>

          <Divider>收入项明细</Divider>
          <Table
            :columns="[
              {
                title: '收入类型',
                dataIndex: 'incomeTypeName',
                key: 'incomeTypeName',
                width: 150,
              },
              {
                title: '金额',
                dataIndex: 'amount',
                key: 'amount',
                width: 150,
                customRender: ({ record }: { record: any }) =>
                  formatCurrency(record.amount),
              },
              { title: '备注', dataIndex: 'remark', key: 'remark' },
            ]"
            :data-source="item.incomes"
            :pagination="false"
            size="small"
          />

          <Divider>扣减项明细</Divider>
          <Table
            :columns="[
              {
                title: '扣减类型',
                dataIndex: 'deductionTypeName',
                key: 'deductionTypeName',
                width: 150,
              },
              {
                title: '金额',
                dataIndex: 'amount',
                key: 'amount',
                width: 150,
                customRender: ({ record }: { record: any }) =>
                  formatCurrency(record.amount),
              },
              { title: '备注', dataIndex: 'remark', key: 'remark' },
            ]"
            :data-source="item.deductions"
            :pagination="false"
            size="small"
          />

          <div
            v-if="
              item.confirmStatus === 'PENDING' &&
              currentSheet.status === 'PENDING_CONFIRM'
            "
            class="mt-4 text-center"
          >
            <Space>
              <Button type="primary" @click="handleConfirm(item)">
                确认无误
              </Button>
              <Button danger @click="handleReject(item)">有误，拒绝</Button>
            </Space>
          </div>
        </div>
      </div>
    </Modal>

    <!-- 确认/拒绝弹窗 -->
    <Modal
      v-model:open="confirmModalVisible"
      :title="
        confirmForm.confirmStatus === 'CONFIRMED' ? '确认工资' : '拒绝工资'
      "
      width="500px"
      @ok="handleSubmitConfirm"
    >
      <Form :model="confirmForm" layout="vertical">
        <FormItem
          label="确认意见"
          :required="confirmForm.confirmStatus === 'REJECTED'"
        >
          <Input.TextArea
            v-model:value="confirmForm.confirmComment"
            :rows="4"
            :placeholder="
              confirmForm.confirmStatus === 'CONFIRMED'
                ? '确认无误请留空或填写备注'
                : '请填写拒绝原因'
            "
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
