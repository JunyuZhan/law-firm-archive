<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { OcrResultDTO } from '#/api/ocr';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Spin,
  Statistic,
  Tag,
  Tooltip,
  Upload,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { recognizeInvoice } from '#/api/ocr';
import { requestClient } from '#/api/request';

defineOptions({ name: 'ExpenseReimbursement' });

// 响应式布局
const { isMobile } = useResponsive();

interface ExpenseRecord {
  id: number;
  expenseNo: string;
  expenseType: string;
  expenseTypeName: string;
  amount: number;
  matterId?: number;
  matterName?: string;
  expenseDate: string;
  description: string;
  status: string;
  statusName: string;
  approverName?: string;
  approvedAt?: string;
  rejectReason?: string;
  createdAt: string;
}

// ==================== 状态定义 ====================

const expenses = ref<ExpenseRecord[]>([]);
const modalVisible = ref(false);
const formRef = ref();
const ocrLoading = ref(false);
const ocrResult = ref<null | OcrResultDTO>(null);

const formData = reactive({
  expenseType: 'TRAVEL',
  amount: undefined as number | undefined,
  matterId: undefined as number | undefined,
  expenseDate: undefined as any,
  description: '',
});

// 统计数据
const stats = computed(() => {
  const total = expenses.value.reduce((sum, e) => sum + e.amount, 0);
  const approved = expenses.value
    .filter((e) => e.status === 'APPROVED')
    .reduce((sum, e) => sum + e.amount, 0);
  const pending = expenses.value
    .filter((e) => e.status === 'PENDING')
    .reduce((sum, e) => sum + e.amount, 0);
  const count = expenses.value.length;
  return { total, approved, pending, count };
});

// ==================== 常量选项 ====================

const expenseTypeOptions = [
  { label: '差旅费', value: 'TRAVEL' },
  { label: '交通费', value: 'TRANSPORT' },
  { label: '餐饮费', value: 'MEAL' },
  { label: '住宿费', value: 'ACCOMMODATION' },
  { label: '办公费', value: 'OFFICE' },
  { label: '诉讼费', value: 'LITIGATION' },
  { label: '其他', value: 'OTHER' },
];

// ==================== 表格配置 ====================

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    { title: '报销单号', field: 'expenseNo', width: 140 },
    { title: '费用类型', field: 'expenseTypeName', width: 100, mobileShow: true },
    { title: '金额', field: 'amount', width: 120, slots: { default: 'amount' }, mobileShow: true },
    { title: '关联项目', field: 'matterName', minWidth: 150, showOverflow: true },
    { title: '费用日期', field: 'expenseDate', width: 110 },
    { title: '说明', field: 'description', minWidth: 200, showOverflow: true },
    {
      title: '状态',
      field: 'statusName',
      width: 100,
      slots: { default: 'status' },
      mobileShow: true,
    },
    { title: '提交时间', field: 'createdAt', width: 110 },
    {
      title: '操作',
      field: 'action',
      width: 100,
      fixed: 'right',
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];
  
  if (isMobile.value) {
    return baseColumns.filter(col => col.mobileShow === true);
  }
  return baseColumns;
}

async function loadData() {
  try {
    const res = await requestClient.get<ExpenseRecord[]>(
      '/finance/my/expenses',
    );
    expenses.value = res || [];
    return { items: res || [], total: (res || []).length };
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
    return { items: [], total: 0 };
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: getGridColumns(),
    // 移除高度限制，让表格完整显示所有数据
    height: '',
    minHeight: 200,
    pagerConfig: { enabled: false },
    proxyConfig: { ajax: { query: loadData } },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

// ==================== OCR识别操作 ====================

async function handleInvoiceOcr(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;

  ocrLoading.value = true;
  try {
    const result = await recognizeInvoice(file);
    ocrResult.value = result;

    if (result.success) {
      // 自动填充表单
      if (result.totalAmount) {
        formData.amount = result.totalAmount;
      } else if (result.invoiceAmount) {
        formData.amount = result.invoiceAmount;
      }

      if (result.invoiceDate) {
        formData.expenseDate = dayjs(result.invoiceDate);
      }

      // 根据发票内容自动推断费用类型
      const rawText = (
        result.rawText ||
        result.data?.raw_text ||
        ''
      ).toLowerCase();
      if (
        rawText.includes('餐') ||
        rawText.includes('饮') ||
        rawText.includes('食')
      ) {
        formData.expenseType = 'MEAL';
      } else if (
        rawText.includes('住宿') ||
        rawText.includes('酒店') ||
        rawText.includes('宾馆')
      ) {
        formData.expenseType = 'ACCOMMODATION';
      } else if (
        rawText.includes('交通') ||
        rawText.includes('出租') ||
        rawText.includes('打车')
      ) {
        formData.expenseType = 'TRANSPORT';
      } else if (
        rawText.includes('机票') ||
        rawText.includes('火车') ||
        rawText.includes('高铁')
      ) {
        formData.expenseType = 'TRAVEL';
      }

      // 填充描述
      const parts = [];
      if (result.invoiceType) parts.push(result.invoiceType);
      if (result.sellerName) parts.push(`销售方: ${result.sellerName}`);
      if (result.invoiceNo) parts.push(`发票号: ${result.invoiceNo}`);
      if (parts.length > 0) {
        formData.description = parts.join(' | ');
      }

      message.success(`发票识别成功！已自动填充报销信息`);
    } else {
      message.error(result.errorMessage || '发票识别失败');
    }
  } catch (error: any) {
    message.error(error?.message || '发票识别失败');
  } finally {
    ocrLoading.value = false;
  }
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  Object.assign(formData, {
    expenseType: 'TRAVEL',
    amount: undefined,
    matterId: undefined,
    expenseDate: undefined,
    description: '',
  });
  ocrResult.value = null;
  modalVisible.value = true;
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();

    if (!formData.amount || formData.amount <= 0) {
      message.error('请输入有效金额');
      return;
    }

    await requestClient.post('/finance/expense/apply', {
      expenseType: formData.expenseType,
      amount: formData.amount,
      matterId: formData.matterId,
      expenseDate:
        formData.expenseDate?.format?.('YYYY-MM-DD') || formData.expenseDate,
      description: formData.description,
    });

    message.success('报销申请提交成功');
    modalVisible.value = false;
    gridApi.reload();
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '提交失败');
  }
}

async function handleCancel(row: ExpenseRecord) {
  Modal.confirm({
    title: '确认撤销',
    content: `确定要撤销报销单 "${row.expenseNo}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await requestClient.post(`/finance/expense/${row.id}/cancel`);
        message.success('已撤销');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '撤销失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
    CANCELLED: 'gray',
    PAID: 'blue',
  };
  return colorMap[status] || 'default';
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <Page title="费用报销" description="提交和查看您的费用报销申请">
    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic title="报销记录数" :value="stats.count" suffix="条" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="报销总额"
            :value="stats.total"
            prefix="¥"
            :precision="2"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="已审批"
            :value="stats.approved"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic
            title="待审批"
            :value="stats.pending"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
    </Row>

    <Card>
      <div style="margin-bottom: 16px">
        <Button type="primary" @click="handleAdd">
          <Plus class="size-4" /> 新增报销
        </Button>
      </div>

      <Grid>
        <template #amount="{ row }">
          {{ formatMoney(row.amount) }}
        </template>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #action="{ row }">
          <a
            v-if="row.status === 'PENDING'"
            style="color: red"
            @click="handleCancel(row)"
            >撤销</a
          >
          <span v-else>-</span>
        </template>
        <template #empty>
          <Empty description="暂无报销记录" />
        </template>
      </Grid>
    </Card>

    <!-- 新增报销弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新增报销"
      :width="isMobile ? '100%' : '500px'"
      :centered="isMobile"
      @ok="handleSubmit"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <!-- OCR智能识别区域 -->
        <div class="mb-4 rounded border border-green-200 bg-green-50 p-3">
          <div class="mb-2 flex items-center">
            <IconifyIcon
              icon="ant-design:scan-outlined"
              class="mr-2 text-green-600"
            />
            <span class="font-medium text-green-700">发票智能识别</span>
            <span class="ml-2 text-xs text-gray-500">上传发票自动填充</span>
          </div>
          <Spin :spinning="ocrLoading" size="small">
            <Upload
              :show-upload-list="false"
              :before-upload="() => false"
              accept="image/*"
              @change="handleInvoiceOcr"
            >
              <Tooltip title="上传发票/票据照片，自动识别金额、日期等信息">
                <Button
                  :loading="ocrLoading"
                  :disabled="ocrLoading"
                  size="small"
                  type="primary"
                  ghost
                >
                  <template #icon>
                    <IconifyIcon icon="ant-design:file-text-outlined" />
                  </template>
                  拍照识别发票
                </Button>
              </Tooltip>
            </Upload>
          </Spin>
          <div v-if="ocrResult?.success" class="mt-2 text-xs text-green-600">
            ✓ 已识别: {{ ocrResult.invoiceType || '票据' }}
            <span v-if="ocrResult.totalAmount"
              >¥{{ ocrResult.totalAmount }}</span
            >
          </div>
        </div>

        <FormItem
          label="费用类型"
          name="expenseType"
          :rules="[{ required: true, message: '请选择费用类型' }]"
        >
          <Select
            v-model:value="formData.expenseType"
            :options="expenseTypeOptions"
          />
        </FormItem>
        <FormItem
          label="金额"
          name="amount"
          :rules="[{ required: true, message: '请输入金额' }]"
        >
          <InputNumber
            v-model:value="formData.amount"
            :min="0"
            :precision="2"
            prefix="¥"
            style="width: 100%"
          />
        </FormItem>
        <FormItem
          label="费用日期"
          name="expenseDate"
          :rules="[{ required: true, message: '请选择费用日期' }]"
        >
          <DatePicker
            v-model:value="formData.expenseDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem
          label="费用说明"
          name="description"
          :rules="[{ required: true, message: '请输入费用说明' }]"
        >
          <Input.TextArea
            v-model:value="formData.description"
            :rows="3"
            placeholder="请描述费用用途"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
