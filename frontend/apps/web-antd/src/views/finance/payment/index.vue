<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Input,
  Select,
  Form,
  FormItem,
  DatePicker,
  InputNumber,
  Textarea,
  Row,
  Col,
  Statistic,
  Modal,
} from 'ant-design-vue';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getFeeList,
  getFeeDetail,
  createPayment,
  confirmPayment,
} from '#/api/finance';
import { getClientList } from '#/api/client';
import type { FeeDTO, FeeQuery, CreatePaymentCommand, PaymentDTO } from '#/api/finance/types';
import type { ClientDTO } from '#/api/client/types';

defineOptions({ name: 'FinancePayment' });

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const formRef = ref();
const clients = ref<ClientDTO[]>([]);
const selectedFee = ref<FeeDTO | null>(null);
const detailModalVisible = ref(false);
const currentFeePayments = ref<PaymentDTO[]>([]);
const currentFee = ref<FeeDTO | null>(null);
const detailLoading = ref(false);

const currentYear = new Date().getFullYear();
// 年份选项（最近5年 + 全部）
const yearOptions = [
  { label: '全部年份', value: 0 },
  ...Array.from({ length: 5 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i,
  })),
];
const selectedYear = ref<number>(currentYear);

const queryParams = ref<FeeQuery>({
  pageNum: 1,
  pageSize: 10,
  // 默认筛选当前年份（按创建时间）
  createdAtFrom: `${currentYear}-01-01T00:00:00`,
  createdAtTo: `${currentYear}-12-31T23:59:59`,
});

const formData = reactive<Partial<CreatePaymentCommand>>({
  feeId: undefined,
  amount: undefined,
  currency: 'CNY',
  paymentMethod: 'BANK_TRANSFER',
  paymentDate: '',
  bankAccount: '',
  transactionNo: '',
  remark: '',
});

// ==================== 常量选项 ====================

const paymentMethodOptions = [
  { label: '银行转账', value: 'BANK_TRANSFER' },
  { label: '现金', value: 'CASH' },
  { label: '支票', value: 'CHECK' },
  { label: '其他', value: 'OTHER' },
];

const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: '美元', value: 'USD' },
  { label: '欧元', value: 'EUR' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待收款', value: 'PENDING' },
  { label: '部分收款', value: 'PARTIAL' },
  { label: '已收款', value: 'PAID' },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '收费编号', field: 'feeNo', width: 130 },
  { title: '项目', field: 'matterName', minWidth: 200, showOverflow: true },
  { title: '客户', field: 'clientName', width: 150 },
  { title: '收费类型', field: 'feeTypeName', width: 100 },
  { title: '收费名称', field: 'feeName', width: 150 },
  { title: '应收金额', field: 'amount', width: 120, slots: { default: 'amount' } },
  { title: '已收金额', field: 'paidAmount', width: 120, slots: { default: 'paidAmount' } },
  { title: '计划日期', field: 'plannedDate', width: 120 },
  { title: '实际日期', field: 'actualDate', width: 120 },
  { title: '状态', field: 'statusName', width: 100, slots: { default: 'status' } },
  { title: '操作', field: 'action', width: 150, fixed: 'right', slots: { default: 'action' } },
];

async function loadData({ page }: { page: { currentPage: number; pageSize: number } }) {
  const params = { ...queryParams.value, pageNum: page.currentPage, pageSize: page.pageSize };
  const res = await getFeeList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

// 收款明细表格列
const paymentColumns = [
  { title: '收款编号', dataIndex: 'paymentNo', key: 'paymentNo', width: 130 },
  { title: '收款金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '收款方式', dataIndex: 'paymentMethodName', key: 'paymentMethodName', width: 100 },
  { title: '收款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 120 },
  { title: '交易流水号', dataIndex: 'transactionNo', key: 'transactionNo', width: 150 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
];

// ==================== 加载选项 ====================

async function loadOptions() {
  const clientRes = await getClientList({ pageNum: 1, pageSize: 1000 });
  clients.value = clientRes.list;
}

// ==================== 搜索操作 ====================

function handleYearChange(value: any) {
  const year = Number(value);
  if (isNaN(year)) return;
  selectedYear.value = year;
  if (year === 0) {
    // 选择"全部年份"时清除日期筛选
    queryParams.value.createdAtFrom = undefined;
    queryParams.value.createdAtTo = undefined;
  } else {
    queryParams.value.createdAtFrom = `${year}-01-01T00:00:00`;
    queryParams.value.createdAtTo = `${year}-12-31T23:59:59`;
  }
  queryParams.value.pageNum = 1;
  gridApi.reload();
}

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  selectedYear.value = currentYear;
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    createdAtFrom: `${currentYear}-01-01T00:00:00`,
    createdAtTo: `${currentYear}-12-31T23:59:59`,
  };
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd(row: FeeDTO) {
  selectedFee.value = row;
  Object.assign(formData, {
    feeId: row.id,
    amount: row.amount - (row.paidAmount || 0),
    currency: 'CNY',
    paymentMethod: 'BANK_TRANSFER',
    paymentDate: '',
    bankAccount: '',
    transactionNo: '',
    remark: '',
  });
  modalVisible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();
    const paymentRes = await createPayment(formData as CreatePaymentCommand);
    if (paymentRes && paymentRes.id) {
      await confirmPayment(paymentRes.id);
      message.success('收款登记并确认成功');
    } else {
      message.success('收款登记成功');
    }
    modalVisible.value = false;
    gridApi.reload();
  } catch (e: any) {
    if (e?.message) {
      message.error(e.message);
    }
  }
}

async function handleViewPayments(row: FeeDTO) {
  detailLoading.value = true;
  detailModalVisible.value = true;
  currentFee.value = row;
  try {
    const feeDetail = await getFeeDetail(row.id);
    currentFeePayments.value = feeDetail.payments || [];
  } catch (error: any) {
    message.error(error.message || '获取收款明细失败');
    currentFeePayments.value = [];
  } finally {
    detailLoading.value = false;
  }
}

// ==================== 辅助方法 ====================

function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PAID: 'green',
    PARTIAL: 'orange',
    PENDING: 'default',
  };
  return colorMap[status] || 'default';
}

onMounted(() => {
  loadOptions();
});
</script>

<template>
  <Page title="收款管理" description="管理项目收款记录">
    <!-- 统计卡片 -->
    <Row :gutter="16" class="mb-4">
      <Col :xs="12" :sm="6">
        <Card>
          <Statistic title="本月收款" :value="0" prefix="¥" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card>
          <Statistic title="待收款" :value="0" prefix="¥" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card>
          <Statistic title="已开票" :value="0" prefix="¥" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card>
          <Statistic title="收款笔数" :value="0" suffix="笔" />
        </Card>
      </Col>
    </Row>

    <!-- 搜索区域 -->
    <Card class="mb-4">
      <Row :gutter="16">
        <Col :xs="24" :sm="12" :md="4">
          <Select
            v-model:value="selectedYear"
            placeholder="创建年份"
            style="width: 100%"
            :options="yearOptions"
            @change="handleYearChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="5">
          <Input v-model:value="queryParams.feeNo" placeholder="收费编号" allowClear @pressEnter="handleSearch" />
        </Col>
        <Col :xs="24" :sm="12" :md="5">
          <Select
            v-model:value="queryParams.clientId"
            placeholder="客户"
            allowClear
            showSearch
            :filter-option="(input: string, option: any) => option.label?.toLowerCase().includes(input.toLowerCase())"
            style="width: 100%"
          >
            <Select.Option v-for="c in clients" :key="c.id" :value="c.id" :label="c.name">
              {{ c.name }}
            </Select.Option>
          </Select>
        </Col>
        <Col :xs="24" :sm="12" :md="4">
          <Select v-model:value="queryParams.status" placeholder="状态" allowClear style="width: 100%" :options="statusOptions" />
        </Col>
        <Col :xs="24" :sm="24" :md="6">
          <Space>
            <Button type="primary" @click="handleSearch">查询</Button>
            <Button @click="handleReset">重置</Button>
          </Space>
        </Col>
      </Row>
    </Card>

    <!-- 数据表格 -->
    <Card>
      <Grid>
        <template #amount="{ row }">
          {{ formatMoney(row.amount) }}
        </template>
        <template #paidAmount="{ row }">
          {{ formatMoney(row.paidAmount) }}
        </template>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <a v-if="row.status !== 'PAID'" @click="handleAdd(row)">登记收款</a>
            <a v-if="row.status === 'PAID' || row.status === 'PARTIAL'" @click="handleViewPayments(row)">查看明细</a>
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 登记收款弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="登记收款"
      width="600px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <FormItem label="收费项目" name="feeId">
          <Input :value="selectedFee?.feeName" disabled />
        </FormItem>
        <FormItem label="收款金额" name="amount" :rules="[{ required: true, message: '请输入收款金额' }]">
          <InputNumber
            v-model:value="formData.amount"
            :min="0"
            :max="selectedFee ? selectedFee.amount - (selectedFee.paidAmount || 0) : undefined"
            :precision="2"
            style="width: 100%"
            placeholder="请输入"
          />
        </FormItem>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="货币" name="currency" :rules="[{ required: true, message: '请选择货币' }]">
              <Select v-model:value="formData.currency" :options="currencyOptions" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="收款方式" name="paymentMethod" :rules="[{ required: true, message: '请选择收款方式' }]">
              <Select v-model:value="formData.paymentMethod" :options="paymentMethodOptions" />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="收款日期" name="paymentDate" :rules="[{ required: true, message: '请选择收款日期' }]">
          <DatePicker v-model:value="formData.paymentDate" style="width: 100%" />
        </FormItem>
        <FormItem label="银行账户" name="bankAccount">
          <Input v-model:value="formData.bankAccount" placeholder="请输入" />
        </FormItem>
        <FormItem label="交易流水号" name="transactionNo">
          <Input v-model:value="formData.transactionNo" placeholder="请输入" />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea v-model:value="formData.remark" :rows="3" placeholder="请输入" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 收款明细弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="收款明细"
      width="900px"
      :footer="null"
    >
      <div v-if="currentFee" class="mb-4">
        <Row :gutter="16">
          <Col :span="8">
            <strong>收费编号：</strong>{{ currentFee.feeNo }}
          </Col>
          <Col :span="8">
            <strong>收费名称：</strong>{{ currentFee.feeName }}
          </Col>
          <Col :span="8">
            <strong>应收金额：</strong>{{ formatMoney(currentFee.amount) }}
          </Col>
        </Row>
        <Row :gutter="16" class="mt-2">
          <Col :span="8">
            <strong>已收金额：</strong>
            <span class="text-green-600">{{ formatMoney(currentFee.paidAmount) }}</span>
          </Col>
          <Col :span="8">
            <strong>待收金额：</strong>
            <span class="text-orange-500">{{ formatMoney(currentFee.amount - (currentFee.paidAmount || 0)) }}</span>
          </Col>
          <Col :span="8">
            <strong>状态：</strong>
            <Tag :color="getStatusColor(currentFee.status)">{{ currentFee.statusName }}</Tag>
          </Col>
        </Row>
      </div>
      <Table
        :columns="paymentColumns"
        :data-source="currentFeePayments"
        :loading="detailLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatMoney(record.amount) }}
          </template>
          <template v-if="column.key === 'statusName'">
            <Tag :color="record.status === 'CONFIRMED' ? 'green' : record.status === 'PENDING' ? 'orange' : 'default'">
              {{ record.statusName }}
            </Tag>
          </template>
        </template>
      </Table>
    </Modal>
  </Page>
</template>
