<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { ClientDTO } from '#/api/client/types';
import type {
  CreatePaymentCommand,
  FeeDTO,
  FeeQuery,
  PaymentDTO,
} from '#/api/finance/types';
import type {
  MatchCandidateDTO,
  OcrResultDTO,
  ReconciliationResultDTO,
} from '#/api/ocr';

import { onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Divider,
  Form,
  FormItem,
  Input,
  InputNumber,
  List,
  ListItem,
  ListItemMeta,
  message,
  Modal,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Textarea,
  Upload,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getClientSelectOptions } from '#/api/client';
import {
  confirmPayment,
  createPayment,
  getFeeDetail,
  getFeeList,
} from '#/api/finance';
import {
  matchPayment,
  OCR_DISABLED,
  OCR_DISABLED_MESSAGE,
  recognizeBankReceipt,
} from '#/api/ocr';

defineOptions({ name: 'FinancePayment' });

// 响应式布局
const { isMobile } = useResponsive();

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

// ==================== OCR智能识别状态 ====================
const ocrModalVisible = ref(false);
const ocrLoading = ref(false);
const ocrResult = ref<null | OcrResultDTO>(null);
const matchResult = ref<null | ReconciliationResultDTO>(null);
const matchLoading = ref(false);
const ocrStep = ref<'match' | 'result' | 'upload'>('upload');

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

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    { title: '收费编号', field: 'feeNo', width: 130 },
    {
      title: '项目',
      field: 'matterName',
      minWidth: isMobile.value ? 120 : 200,
      showOverflow: true,
      mobileShow: true,
    },
    { title: '客户', field: 'clientName', width: 150, mobileShow: true },
    { title: '收费类型', field: 'feeTypeName', width: 100 },
    { title: '收费名称', field: 'feeName', width: 150 },
    {
      title: '应收金额',
      field: 'amount',
      width: 120,
      slots: { default: 'amount' },
      mobileShow: true,
    },
    {
      title: '已收金额',
      field: 'paidAmount',
      width: 120,
      slots: { default: 'paidAmount' },
    },
    { title: '计划日期', field: 'plannedDate', width: 120 },
    { title: '实际日期', field: 'actualDate', width: 120 },
    {
      title: '状态',
      field: 'statusName',
      width: 100,
      slots: { default: 'status' },
      mobileShow: true,
    },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 100 : 150,
      fixed: 'right',
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
}

async function loadData({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const params = {
    ...queryParams.value,
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  };
  const res = await getFeeList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: getGridColumns(),
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

// 收款明细表格列
const paymentColumns = [
  { title: '收款编号', dataIndex: 'paymentNo', key: 'paymentNo', width: 130 },
  { title: '收款金额', dataIndex: 'amount', key: 'amount', width: 120 },
  {
    title: '收款方式',
    dataIndex: 'paymentMethodName',
    key: 'paymentMethodName',
    width: 100,
  },
  {
    title: '收款日期',
    dataIndex: 'paymentDate',
    key: 'paymentDate',
    width: 120,
  },
  {
    title: '交易流水号',
    dataIndex: 'transactionNo',
    key: 'transactionNo',
    width: 150,
  },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
];

// ==================== 加载选项 ====================

async function loadOptions() {
  const clientRes = await getClientSelectOptions({
    pageNum: 1,
    pageSize: 1000,
  });
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
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message);
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

// ==================== OCR智能识别操作 ====================

function openOcrModal() {
  ocrModalVisible.value = true;
  ocrStep.value = 'upload';
  ocrResult.value = null;
  matchResult.value = null;
}

async function handleOcrUpload(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;

  ocrLoading.value = true;
  ocrStep.value = 'result';

  try {
    const result = await recognizeBankReceipt(file);
    ocrResult.value = result;

    if (result.success && result.amount) {
      // 自动进行智能匹配
      matchLoading.value = true;
      ocrStep.value = 'match';

      const matchRes = await matchPayment({
        amount: result.amount,
        payerName: result.payerName,
        transactionDate: result.transactionDate,
        transactionNo: result.transactionNo,
      });
      matchResult.value = matchRes;
    }
  } catch (error: any) {
    message.error(error?.message || 'OCR识别失败');
    ocrStep.value = 'upload';
  } finally {
    ocrLoading.value = false;
    matchLoading.value = false;
  }
}

function handleSelectMatch(candidate: MatchCandidateDTO) {
  // 选择匹配的收费项目，自动填充收款表单
  const fee = {
    id: candidate.feeId,
    feeNo: candidate.feeNo,
    feeName: candidate.feeName || '',
    matterId: candidate.matterId,
    matterName: candidate.matterName,
    matterNo: candidate.matterNo,
    clientId: candidate.clientId,
    clientName: candidate.clientName,
    amount: candidate.expectedAmount,
    paidAmount: candidate.expectedAmount - candidate.unpaidAmount,
    status: 'PENDING',
    statusName: '待收款',
    feeType: 'SERVICE',
    currency: 'CNY',
  } as unknown as FeeDTO;

  selectedFee.value = fee;

  // 填充表单数据
  Object.assign(formData, {
    feeId: candidate.feeId,
    amount: ocrResult.value?.amount || candidate.unpaidAmount,
    currency: 'CNY',
    paymentMethod: 'BANK_TRANSFER',
    paymentDate: ocrResult.value?.transactionDate || '',
    bankAccount: ocrResult.value?.payeeAccount || '',
    transactionNo: ocrResult.value?.transactionNo || '',
    remark:
      ocrResult.value?.remark ||
      `银行: ${ocrResult.value?.bankName || ''}, 付款方: ${ocrResult.value?.payerName || ''}`,
  });

  ocrModalVisible.value = false;
  modalVisible.value = true;
  message.success('已自动填充收款信息，请核对后确认');
}

function formatMatchScore(score: number) {
  return Math.round(score * 100);
}

function getScoreColor(score: number) {
  if (score >= 0.85) return 'green';
  if (score >= 0.5) return 'orange';
  return 'red';
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
          <Input
            v-model:value="queryParams.feeNo"
            placeholder="收费编号"
            allow-clear
            @press-enter="handleSearch"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="5">
          <Select
            v-model:value="queryParams.clientId"
            placeholder="客户"
            allow-clear
            show-search
            :filter-option="
              (input: string, option: any) =>
                option.label?.toLowerCase().includes(input.toLowerCase())
            "
            style="width: 100%"
          >
            <Select.Option
              v-for="c in clients"
              :key="c.id"
              :value="c.id"
              :label="c.name"
            >
              {{ c.name }}
            </Select.Option>
          </Select>
        </Col>
        <Col :xs="24" :sm="12" :md="4">
          <Select
            v-model:value="queryParams.status"
            placeholder="状态"
            allow-clear
            style="width: 100%"
            :options="statusOptions"
          />
        </Col>
        <Col :xs="24" :sm="24" :md="10">
          <Space wrap>
            <Button type="primary" @click="handleSearch">查询</Button>
            <Button @click="handleReset">重置</Button>
            <Button
              v-if="!OCR_DISABLED"
              type="primary"
              ghost
              @click="openOcrModal"
            >
              <template #icon>
                <IconifyIcon icon="ant-design:scan-outlined" />
              </template>
              OCR识别
            </Button>
            <Button
              v-else
              type="default"
              disabled
              :title="OCR_DISABLED_MESSAGE"
            >
              <template #icon>
                <IconifyIcon icon="ant-design:scan-outlined" />
              </template>
              OCR识别（暂不可用）
            </Button>
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
            <a
              v-if="row.status !== 'PAID'"
              v-access:code="'fee:payment'"
              @click="handleAdd(row)"
              >登记收款</a
            >
            <a
              v-if="row.status === 'PAID' || row.status === 'PARTIAL'"
              @click="handleViewPayments(row)"
              >查看明细</a
            >
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 登记收款弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="登记收款"
      :width="isMobile ? '100%' : '600px'"
      :centered="isMobile"
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
        <FormItem
          label="收款金额"
          name="amount"
          :rules="[{ required: true, message: '请输入收款金额' }]"
        >
          <InputNumber
            v-model:value="formData.amount"
            :min="0"
            :max="
              selectedFee
                ? selectedFee.amount - (selectedFee.paidAmount || 0)
                : undefined
            "
            :precision="2"
            style="width: 100%"
            placeholder="请输入"
          />
        </FormItem>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="货币"
              name="currency"
              :rules="[{ required: true, message: '请选择货币' }]"
            >
              <Select
                v-model:value="formData.currency"
                :options="currencyOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              label="收款方式"
              name="paymentMethod"
              :rules="[{ required: true, message: '请选择收款方式' }]"
            >
              <Select
                v-model:value="formData.paymentMethod"
                :options="paymentMethodOptions"
              />
            </FormItem>
          </Col>
        </Row>
        <FormItem
          label="收款日期"
          name="paymentDate"
          :rules="[{ required: true, message: '请选择收款日期' }]"
        >
          <DatePicker
            v-model:value="formData.paymentDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="银行账户" name="bankAccount">
          <Input v-model:value="formData.bankAccount" placeholder="请输入" />
        </FormItem>
        <FormItem label="交易流水号" name="transactionNo">
          <Input v-model:value="formData.transactionNo" placeholder="请输入" />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea
            v-model:value="formData.remark"
            :rows="3"
            placeholder="请输入"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- OCR智能识别弹窗 -->
    <Modal
      v-model:open="ocrModalVisible"
      title="银行回单智能识别"
      :width="isMobile ? '100%' : '800px'"
      :centered="isMobile"
      :footer="null"
    >
      <!-- 步骤1: 上传图片 -->
      <div v-if="ocrStep === 'upload'" class="py-8 text-center">
        <Upload.Dragger
          :show-upload-list="false"
          :before-upload="() => false"
          accept="image/*"
          @change="handleOcrUpload"
        >
          <p class="ant-upload-drag-icon">
            <IconifyIcon
              icon="ant-design:scan-outlined"
              style="font-size: 48px; color: #1890ff"
            />
          </p>
          <p class="ant-upload-text">点击或拖拽银行回单图片到此处</p>
          <p class="ant-upload-hint">
            支持 JPG、PNG、GIF 格式图片，自动识别回单信息并智能匹配待收款项目
          </p>
        </Upload.Dragger>
      </div>

      <!-- 步骤2: 识别结果 -->
      <div v-if="ocrStep === 'result' || ocrStep === 'match'">
        <Spin :spinning="ocrLoading" tip="正在识别银行回单...">
          <div v-if="ocrResult">
            <Alert
              v-if="ocrResult.success"
              type="success"
              show-icon
              :message="`识别成功 (置信度: ${Math.round((ocrResult.confidence || 0) * 100)}%)`"
              class="mb-4"
            />
            <Alert
              v-else
              type="error"
              show-icon
              :message="ocrResult.errorMessage || '识别失败'"
              class="mb-4"
            />

            <Card title="识别结果" size="small" class="mb-4">
              <Row :gutter="16">
                <Col :span="12">
                  <div class="mb-2">
                    <strong>银行名称：</strong>{{ ocrResult.bankName || '-' }}
                  </div>
                  <div class="mb-2">
                    <strong>交易金额：</strong
                    ><span class="text-lg font-bold text-red-500"
                      >¥{{ ocrResult.amount?.toLocaleString() || '-' }}</span
                    >
                  </div>
                  <div class="mb-2">
                    <strong>交易日期：</strong
                    >{{ ocrResult.transactionDate || '-' }}
                  </div>
                  <div class="mb-2">
                    <strong>交易流水号：</strong
                    >{{ ocrResult.transactionNo || '-' }}
                  </div>
                </Col>
                <Col :span="12">
                  <div class="mb-2">
                    <strong>付款方：</strong>{{ ocrResult.payerName || '-' }}
                  </div>
                  <div class="mb-2">
                    <strong>付款账号：</strong
                    >{{ ocrResult.payerAccount || '-' }}
                  </div>
                  <div class="mb-2">
                    <strong>收款方：</strong>{{ ocrResult.payeeName || '-' }}
                  </div>
                  <div class="mb-2">
                    <strong>收款账号：</strong
                    >{{ ocrResult.payeeAccount || '-' }}
                  </div>
                </Col>
              </Row>
              <div v-if="ocrResult.remark" class="mt-2">
                <strong>摘要/备注：</strong>{{ ocrResult.remark }}
              </div>
            </Card>
          </div>
        </Spin>

        <!-- 步骤3: 智能匹配结果 -->
        <Spin :spinning="matchLoading" tip="正在智能匹配待收款项目...">
          <div v-if="matchResult && ocrStep === 'match'">
            <Divider>智能匹配结果</Divider>

            <Alert
              v-if="matchResult.hasRecommended && matchResult.canAutoReconcile"
              type="success"
              show-icon
              message="找到高匹配度项目，建议自动核销"
              class="mb-4"
            />
            <Alert
              v-else-if="matchResult.hasRecommended"
              type="info"
              show-icon
              message="找到相似项目，请确认后核销"
              class="mb-4"
            />
            <Alert
              v-else-if="matchResult.candidates.length === 0"
              type="warning"
              show-icon
              message="未找到匹配的待收款项目，请手动选择"
              class="mb-4"
            />

            <List
              v-if="matchResult.candidates.length > 0"
              :data-source="matchResult.candidates"
              item-layout="horizontal"
              size="small"
            >
              <template #renderItem="{ item }">
                <ListItem>
                  <template #actions>
                    <Button
                      type="primary"
                      size="small"
                      @click="handleSelectMatch(item)"
                    >
                      选择并填充
                    </Button>
                  </template>
                  <ListItemMeta>
                    <template #title>
                      <Space>
                        <Tag :color="getScoreColor(item.score)">
                          匹配度 {{ formatMatchScore(item.score) }}%
                        </Tag>
                        <span>{{ item.feeName }}</span>
                        <span class="text-gray-400">{{ item.feeNo }}</span>
                      </Space>
                    </template>
                    <template #description>
                      <div>
                        <span class="mr-4"
                          >项目: {{ item.matterName || '-' }}</span
                        >
                        <span class="mr-4"
                          >客户: {{ item.clientName || '-' }}</span
                        >
                        <span class="mr-4"
                          >待收:
                          <strong class="text-orange-500"
                            >¥{{ item.unpaidAmount?.toLocaleString() }}</strong
                          ></span
                        >
                      </div>
                      <div
                        v-if="item.matchReasons?.length"
                        class="mt-1 text-xs text-green-600"
                      >
                        <IconifyIcon
                          icon="ant-design:check-circle-outlined"
                          class="mr-1"
                        />
                        {{ item.matchReasons.join('、') }}
                      </div>
                    </template>
                  </ListItemMeta>
                </ListItem>
              </template>
            </List>

            <div class="mt-4 text-center">
              <Button @click="ocrStep = 'upload'">重新上传</Button>
            </div>
          </div>
        </Spin>
      </div>
    </Modal>

    <!-- 收款明细弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="收款明细"
      :width="isMobile ? '100%' : '900px'"
      :centered="isMobile"
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
            <span class="text-green-600">{{
              formatMoney(currentFee.paidAmount)
            }}</span>
          </Col>
          <Col :span="8">
            <strong>待收金额：</strong>
            <span class="text-orange-500">{{
              formatMoney(currentFee.amount - (currentFee.paidAmount || 0))
            }}</span>
          </Col>
          <Col :span="8">
            <strong>状态：</strong>
            <Tag :color="getStatusColor(currentFee.status)">
              {{ currentFee.statusName }}
            </Tag>
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
            <Tag
              :color="
                record.status === 'CONFIRMED'
                  ? 'green'
                  : record.status === 'PENDING'
                    ? 'orange'
                    : 'default'
              "
            >
              {{ record.statusName }}
            </Tag>
          </template>
        </template>
      </Table>
    </Modal>
  </Page>
</template>
