<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  CreatePrepaymentCommand,
  PrepaymentDTO,
} from '#/api/finance/prepayment';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getClientList } from '#/api/client';
import {
  confirmPrepayment,
  createPrepayment,
  getPrepaymentById,
  getPrepaymentList,
  PAYMENT_METHOD_OPTIONS,
  PREPAYMENT_STATUS_OPTIONS,
  refundPrepayment,
} from '#/api/finance/prepayment';

defineOptions({ name: 'PrepaymentManagement' });

// ==================== 客户选项 ====================
const clientOptions = ref<{ label: string; value: number }[]>([]);

async function loadClients(keyword?: string) {
  try {
    const res = await getClientList({ pageNum: 1, pageSize: 50, keyword });
    clientOptions.value = (res.list || []).map(
      (c: { id: number; name: string }) => ({
        label: c.name,
        value: c.id,
      }),
    );
  } catch (error: any) {
    console.error('加载客户列表失败', error);
  }
}

// ==================== 搜索表单配置 ====================
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'prepaymentNo',
    label: '预收款编号',
    component: 'Input',
    componentProps: {
      placeholder: '请输入预收款编号',
      allowClear: true,
    },
  },
  {
    fieldName: 'clientId',
    label: '客户',
    component: 'ApiSelect',
    componentProps: {
      placeholder: '请选择客户',
      allowClear: true,
      showSearch: true,
      filterOption: false,
      api: async () => {
        await loadClients();
        return clientOptions.value;
      },
      immediate: true,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: PREPAYMENT_STATUS_OPTIONS,
    },
  },
];

// ==================== 表格配置 ====================
const gridColumns: any[] = [
  { title: '预收款编号', field: 'prepaymentNo', width: 150 },
  { title: '客户', field: 'clientName', width: 150, showOverflow: true },
  {
    title: '金额',
    field: 'amount',
    width: 120,
    align: 'right',
    slots: { default: 'amount' },
  },
  {
    title: '已用金额',
    field: 'usedAmount',
    width: 120,
    align: 'right',
    slots: { default: 'usedAmount' },
  },
  {
    title: '剩余金额',
    field: 'remainingAmount',
    width: 120,
    align: 'right',
    slots: { default: 'remainingAmount' },
  },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  {
    title: '收款日期',
    field: 'receiptDate',
    width: 120,
    slots: { default: 'receiptDate' },
  },
  { title: '支付方式', field: 'paymentMethodName', width: 100 },
  { title: '用途', field: 'purpose', showOverflow: true },
  {
    title: '操作',
    field: 'action',
    width: 180,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  const res = await getPrepaymentList({
    pageNum: params.page,
    pageSize: params.pageSize,
    prepaymentNo: params.prepaymentNo,
    clientId: params.clientId,
    status: params.status,
  });
  return {
    items: res.list || [],
    total: res.total || 0,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    showCollapseButton: false,
    submitButtonOptions: { content: '查询' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page, form }: { form: any; page: any }) => {
          return await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...form,
          });
        },
      },
    },
    pagerConfig: {
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
    },
  },
});

// ==================== 新建弹窗 ====================
const createModalVisible = ref(false);
const createForm = reactive<CreatePrepaymentCommand>({
  clientId: 0,
  amount: 0,
  paymentMethod: 'BANK',
  receiptDate: dayjs().format('YYYY-MM-DD'),
});
const createLoading = ref(false);

function handleCreate() {
  Object.assign(createForm, {
    clientId: 0,
    amount: 0,
    paymentMethod: 'BANK',
    receiptDate: dayjs().format('YYYY-MM-DD'),
    contractId: undefined,
    matterId: undefined,
    currency: 'CNY',
    bankAccount: '',
    transactionNo: '',
    purpose: '',
    remark: '',
  });
  loadClients();
  createModalVisible.value = true;
}

async function handleCreateSubmit() {
  if (!createForm.clientId) {
    message.warning('请选择客户');
    return;
  }
  if (!createForm.amount || createForm.amount <= 0) {
    message.warning('请输入有效金额');
    return;
  }

  createLoading.value = true;
  try {
    await createPrepayment(createForm);
    message.success('创建成功');
    createModalVisible.value = false;
    gridApi.reload();
  } catch (error: any) {
    message.error(`创建失败：${error.message || '未知错误'}`);
  } finally {
    createLoading.value = false;
  }
}

// ==================== 详情弹窗 ====================
const detailModalVisible = ref(false);
const detailData = ref<PrepaymentDTO | null>(null);
const detailLoading = ref(false);

async function handleViewDetail(row: PrepaymentDTO) {
  detailLoading.value = true;
  detailModalVisible.value = true;
  try {
    const res = await getPrepaymentById(row.id);
    detailData.value = res;
  } catch (error: any) {
    message.error(`加载详情失败：${error.message || '未知错误'}`);
  } finally {
    detailLoading.value = false;
  }
}

// ==================== 操作方法 ====================
async function handleConfirm(row: PrepaymentDTO) {
  try {
    await confirmPrepayment(row.id);
    message.success('确认成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`确认失败：${error.message || '未知错误'}`);
  }
}

async function handleRefund(row: PrepaymentDTO) {
  Modal.confirm({
    title: '确认退款',
    content: '确定要对该预收款进行退款吗？退款后无法恢复。',
    okText: '确认退款',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await refundPrepayment(row.id, '用户申请退款');
        message.success('退款成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(`退款失败：${error.message || '未知错误'}`);
      }
    },
  });
}

// ==================== 工具方法 ====================
function getStatusColor(status: string) {
  const option = PREPAYMENT_STATUS_OPTIONS.find((o) => o.value === status);
  return option?.color || 'default';
}

function getStatusName(status: string) {
  const option = PREPAYMENT_STATUS_OPTIONS.find((o) => o.value === status);
  return option?.label || status;
}

function formatCurrency(amount: number | null | undefined) {
  if (amount === null || amount === undefined) return '¥0.00';
  return `¥${Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function formatDate(date: string | null | undefined) {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD');
}

// 初始化
onMounted(() => {
  loadClients();
});
</script>

<template>
  <Page title="预收款管理" description="管理客户预付款项，支持核销到收费记录">
    <Grid>
      <!-- 工具栏 -->
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">新建预收款</Button>
      </template>

      <!-- 金额列 -->
      <template #amount="{ row }">
        <span style="font-weight: 500">{{ formatCurrency(row.amount) }}</span>
      </template>

      <!-- 已用金额列 -->
      <template #usedAmount="{ row }">
        {{ formatCurrency(row.usedAmount) }}
      </template>

      <!-- 剩余金额列 -->
      <template #remainingAmount="{ row }">
        <span :style="{ color: row.remainingAmount > 0 ? '#52c41a' : '#999' }">
          {{ formatCurrency(row.remainingAmount) }}
        </span>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName || getStatusName(row.status) }}
        </Tag>
      </template>

      <!-- 收款日期列 -->
      <template #receiptDate="{ row }">
        {{ formatDate(row.receiptDate) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleViewDetail(row)">详情</a>
          <a v-if="row.status === 'PENDING'" @click="handleConfirm(row)"
            >确认</a
          >
          <a
            v-if="row.status === 'ACTIVE' && row.usedAmount === 0"
            style="color: #ff4d4f"
            @click="handleRefund(row)"
          >
            退款
          </a>
        </Space>
      </template>
    </Grid>

    <!-- 新建弹窗 -->
    <Modal
      v-model:open="createModalVisible"
      title="新建预收款"
      :confirm-loading="createLoading"
      width="600px"
      @ok="handleCreateSubmit"
    >
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <Form.Item label="客户" required>
          <Select
            v-model:value="createForm.clientId"
            placeholder="请选择客户"
            show-search
            :filter-option="false"
            :options="clientOptions"
            @search="loadClients"
          />
        </Form.Item>
        <Form.Item label="金额" required>
          <InputNumber
            v-model:value="createForm.amount"
            :min="0.01"
            :precision="2"
            :step="100"
            style="width: 100%"
            :formatter="
              (value: any) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
            "
            :parser="(value: any) => value.replace(/¥\s?|(,*)/g, '')"
          />
        </Form.Item>
        <Form.Item label="支付方式">
          <Select
            v-model:value="createForm.paymentMethod"
            :options="PAYMENT_METHOD_OPTIONS"
          />
        </Form.Item>
        <Form.Item label="银行账号">
          <Input
            v-model:value="createForm.bankAccount"
            placeholder="请输入银行账号"
          />
        </Form.Item>
        <Form.Item label="交易流水号">
          <Input
            v-model:value="createForm.transactionNo"
            placeholder="请输入交易流水号"
          />
        </Form.Item>
        <Form.Item label="用途">
          <Input
            v-model:value="createForm.purpose"
            placeholder="请输入用途说明"
          />
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="createForm.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="预收款详情"
      :footer="null"
      width="700px"
    >
      <Spin :spinning="detailLoading">
        <template v-if="detailData">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="预收款编号">
              {{ detailData.prepaymentNo }}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag :color="getStatusColor(detailData.status)">
                {{ detailData.statusName || getStatusName(detailData.status) }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="客户">
              {{ detailData.clientName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="关联合同">
              {{ detailData.contractNo || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="金额">
              <span style="font-size: 16px; font-weight: 600; color: #1890ff">
                {{ formatCurrency(detailData.amount) }}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="已用金额">
              {{ formatCurrency(detailData.usedAmount) }}
            </Descriptions.Item>
            <Descriptions.Item label="剩余金额">
              <span style="color: #52c41a">
                {{ formatCurrency(detailData.remainingAmount) }}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="支付方式">
              {{ detailData.paymentMethodName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="收款日期">
              {{ formatDate(detailData.receiptDate) }}
            </Descriptions.Item>
            <Descriptions.Item label="银行账号">
              {{ detailData.bankAccount || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="交易流水号" :span="2">
              {{ detailData.transactionNo || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="用途" :span="2">
              {{ detailData.purpose || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="备注" :span="2">
              {{ detailData.remark || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="确认人">
              {{ detailData.confirmerName || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="确认时间">
              {{
                detailData.confirmedAt
                  ? dayjs(detailData.confirmedAt).format('YYYY-MM-DD HH:mm')
                  : '-'
              }}
            </Descriptions.Item>
          </Descriptions>

          <!-- 核销记录 -->
          <template v-if="detailData.usages && detailData.usages.length > 0">
            <Divider>核销记录</Divider>
            <Table
              :columns="[
                { title: '收费编号', dataIndex: 'feeNo', key: 'feeNo' },
                { title: '费用名称', dataIndex: 'feeName', key: 'feeName' },
                { title: '核销金额', dataIndex: 'amount', key: 'amount' },
                { title: '核销时间', dataIndex: 'usageTime', key: 'usageTime' },
                {
                  title: '操作人',
                  dataIndex: 'operatorName',
                  key: 'operatorName',
                },
              ]"
              :data-source="detailData.usages"
              :pagination="false"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">
                  {{ formatCurrency(record.amount) }}
                </template>
                <template v-else-if="column.key === 'usageTime'">
                  {{
                    record.usageTime
                      ? dayjs(record.usageTime).format('YYYY-MM-DD HH:mm')
                      : '-'
                  }}
                </template>
              </template>
            </Table>
          </template>
        </template>
      </Spin>
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-descriptions-item-label) {
  width: 100px;
  background-color: #fafafa;
}
</style>
