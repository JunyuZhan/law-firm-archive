<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { ClientDTO } from '#/api/client/types';
import type {
  CreateInvoiceCommand,
  InvoiceDTO,
  InvoiceQuery,
} from '#/api/finance/types';

import { onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Button,
  Card,
  Col,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getClientSelectOptions } from '#/api/client';
import {
  applyInvoice,
  cancelInvoice,
  getInvoiceDetail,
  getInvoiceList,
  issueInvoice,
} from '#/api/finance';

defineOptions({ name: 'FinanceInvoice' });

// 响应式布局
const { isMobile } = useResponsive();

// 获取当前年份
const currentYear = new Date().getFullYear();

// 年份选项（最近5年 + 全部）
const yearOptions = [
  { label: '全部年份', value: 0 },
  ...Array.from({ length: 5 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i,
  })),
];

// 选中的年份（0表示全部）
const selectedYear = ref<number>(currentYear);

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const formRef = ref();
const detailModalVisible = ref(false);
const currentInvoice = ref<InvoiceDTO | null>(null);
const clients = ref<ClientDTO[]>([]);
const activeTab = ref('all');

const queryParams = ref<InvoiceQuery>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  clientId: undefined,
  // 默认筛选当前年份（按创建时间）
  createdAtFrom: `${currentYear}-01-01T00:00:00`,
  createdAtTo: `${currentYear}-12-31T23:59:59`,
});

const formData = reactive<
  Partial<CreateInvoiceCommand> & { taxAmount?: number; totalAmount?: number }
>({
  clientId: undefined,
  invoiceType: 'GENERAL',
  title: '',
  amount: undefined,
  taxRate: 0,
  taxAmount: 0,
  totalAmount: undefined,
  remark: '',
});

// ==================== 常量选项 ====================

const invoiceTypeOptions = [
  { label: '增值税普通发票', value: 'GENERAL' },
  { label: '增值税专用发票', value: 'SPECIAL' },
  { label: '其他', value: 'OTHER' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '待开票', value: 'APPROVED' },
  { label: '已开票', value: 'ISSUED' },
  { label: '已作废', value: 'CANCELLED' },
];

// ==================== 表格配置 ====================

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    { title: '发票编号', field: 'invoiceNo', width: 130 },
    { title: '客户名称', field: 'clientName', minWidth: isMobile.value ? 100 : 150, mobileShow: true },
    { title: '发票抬头', field: 'title', minWidth: 180, showOverflow: true },
    { title: '发票类型', field: 'invoiceTypeName', width: 100 },
    {
      title: '发票金额',
      field: 'totalAmount',
      width: 120,
      slots: { default: 'totalAmount' },
      mobileShow: true,
    },
    {
      title: '税额',
      field: 'taxAmount',
      width: 100,
      slots: { default: 'taxAmount' },
    },
    { title: '申请日期', field: 'createdAt', width: 120 },
    { title: '开票日期', field: 'invoiceDate', width: 120 },
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
    return baseColumns.filter(col => col.mobileShow === true);
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
  const res = await getInvoiceList(params);
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

// ==================== 加载选项 ====================

async function loadOptions() {
  try {
    const clientRes = await getClientSelectOptions({
      pageNum: 1,
      pageSize: 1000,
    });
    clients.value = clientRes.list;
  } catch (error: any) {
    console.error('加载选项失败:', error);
  }
}

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

// 年份变化时更新日期范围
function handleYearChange(value: any) {
  const year = Number(value);
  if (isNaN(year)) return;
  selectedYear.value = year;
  if (year === 0) {
    queryParams.value.createdAtFrom = undefined;
    queryParams.value.createdAtTo = undefined;
  } else {
    queryParams.value.createdAtFrom = `${year}-01-01T00:00:00`;
    queryParams.value.createdAtTo = `${year}-12-31T23:59:59`;
  }
  queryParams.value.pageNum = 1;
  gridApi.reload();
}

function handleReset() {
  selectedYear.value = currentYear;
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    status: undefined,
    clientId: undefined,
    createdAtFrom: `${currentYear}-01-01T00:00:00`,
    createdAtTo: `${currentYear}-12-31T23:59:59`,
  };
  gridApi.reload();
}

function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  switch (key) {
    case 'all': {
      queryParams.value.status = undefined;

      break;
    }
    case 'approved': {
      queryParams.value.status = 'APPROVED';

      break;
    }
    case 'issued': {
      queryParams.value.status = 'ISSUED';

      break;
    }
    case 'pending': {
      queryParams.value.status = 'PENDING';

      break;
    }
    // No default
  }
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  Object.assign(formData, {
    clientId: undefined,
    invoiceType: 'GENERAL',
    title: '',
    amount: undefined,
    taxRate: 0,
    taxAmount: 0,
    totalAmount: undefined,
    remark: '',
  });
  modalVisible.value = true;
}

async function handleView(row: InvoiceDTO) {
  try {
    const invoice = await getInvoiceDetail(row.id);
    currentInvoice.value = invoice;
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取发票详情失败');
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate();

    if (formData.amount && formData.taxRate) {
      formData.taxAmount = Number(
        ((formData.amount * formData.taxRate) / 100).toFixed(2),
      );
      formData.totalAmount = formData.amount + formData.taxAmount;
    }

    const submitData: CreateInvoiceCommand = {
      clientId: formData.clientId!,
      invoiceType: formData.invoiceType!,
      title: formData.title!,
      amount: formData.amount!,
      taxRate: formData.taxRate,
      remark: formData.remark,
    };

    await applyInvoice(submitData);
    message.success('申请开票成功');
    modalVisible.value = false;
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '申请开票失败');
  }
}

function handleIssue(row: InvoiceDTO) {
  Modal.confirm({
    title: '开具发票',
    content: '请输入发票号码',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const invoiceNo = prompt('请输入发票号码:');
      if (!invoiceNo) {
        message.warning('请输入发票号码');
        return;
      }
      try {
        await issueInvoice(row.id, invoiceNo);
        message.success('开具发票成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '开具发票失败');
      }
    },
  });
}

function handleCancel(row: InvoiceDTO) {
  Modal.confirm({
    title: '作废发票',
    content: '确定要作废这张发票吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const reason = prompt('请输入作废原因:');
      if (!reason) {
        message.warning('请输入作废原因');
        return;
      }
      try {
        await cancelInvoice(row.id, reason);
        message.success('作废发票成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '作废发票失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

function calculateTax() {
  if (formData.amount && formData.taxRate) {
    formData.taxAmount = Number(
      ((formData.amount * formData.taxRate) / 100).toFixed(2),
    );
    formData.totalAmount = formData.amount + formData.taxAmount;
  }
}

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'blue',
    ISSUED: 'green',
    CANCELLED: 'red',
  };
  return colorMap[status] || 'default';
}

function formatAmount(row: InvoiceDTO) {
  const total =
    row.amount && row.taxAmount ? row.amount + row.taxAmount : row.amount || 0;
  return `¥${total.toLocaleString()}`;
}

onMounted(() => {
  loadOptions();
});
</script>

<template>
  <Page title="发票管理" description="管理发票申请与开具">
    <Card>
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="all" tab="全部" />
        <Tabs.TabPane key="pending" tab="待审批" />
        <Tabs.TabPane key="approved" tab="待开票" />
        <Tabs.TabPane key="issued" tab="已开票" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="selectedYear"
              placeholder="创建年份"
              style="width: 100%"
              :options="yearOptions"
              @change="handleYearChange"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="发票状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.clientId"
              placeholder="客户"
              allow-clear
              show-search
              :filter-option="
                (input: string, option: any) =>
                  (option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              style="width: 100%"
              :options="
                clients.map((c) => ({
                  label: c.clientNo ? `[${c.clientNo}] ${c.name}` : c.name,
                  value: c.id,
                }))
              "
            />
          </Col>
          <Col :xs="24" :sm="24" :md="12">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />申请开票
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <template #totalAmount="{ row }">
          {{ formatAmount(row) }}
        </template>
        <template #taxAmount="{ row }">
          ¥{{ row.taxAmount?.toLocaleString() || '0' }}
        </template>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <a @click="handleView(row)">查看</a>
            <a v-if="row.status === 'APPROVED'" @click="handleIssue(row)"
              >开具</a
            >
            <a
              v-if="row.status === 'ISSUED'"
              @click="handleCancel(row)"
              style="color: red"
              >作废</a
            >
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 申请开票弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="申请开票"
      :width="isMobile ? '100%' : '800px'"
      :centered="isMobile"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem
          label="客户"
          name="clientId"
          :rules="[{ required: true, message: '请选择客户' }]"
        >
          <Select
            v-model:value="formData.clientId"
            placeholder="请选择客户"
            show-search
            :filter-option="
              (input: string, option: any) =>
                (option?.label || '')
                  .toLowerCase()
                  .includes(input.toLowerCase())
            "
            :options="
              clients.map((c) => ({
                label: c.clientNo ? `[${c.clientNo}] ${c.name}` : c.name,
                value: c.id,
              }))
            "
          />
        </FormItem>
        <FormItem
          label="发票抬头"
          name="title"
          :rules="[{ required: true, message: '请输入发票抬头' }]"
        >
          <Input v-model:value="formData.title" placeholder="请输入发票抬头" />
        </FormItem>
        <FormItem
          label="发票类型"
          name="invoiceType"
          :rules="[{ required: true, message: '请选择发票类型' }]"
        >
          <Select
            v-model:value="formData.invoiceType"
            :options="invoiceTypeOptions"
          />
        </FormItem>
        <FormItem
          label="发票金额"
          name="amount"
          :rules="[{ required: true, message: '请输入发票金额' }]"
        >
          <InputNumber
            v-model:value="formData.amount"
            placeholder="请输入发票金额"
            :min="0"
            :precision="2"
            style="width: 100%"
            @change="calculateTax"
          />
        </FormItem>
        <FormItem label="税率(%)" name="taxRate">
          <InputNumber
            v-model:value="formData.taxRate"
            placeholder="请输入税率"
            :min="0"
            :max="100"
            :precision="2"
            style="width: 100%"
            @change="calculateTax"
          />
        </FormItem>
        <FormItem label="税额" name="taxAmount">
          <InputNumber
            v-model:value="formData.taxAmount"
            :disabled="true"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="总金额" name="totalAmount">
          <InputNumber
            v-model:value="formData.totalAmount"
            :disabled="true"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea
            v-model:value="formData.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 发票详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="发票详情"
      :width="isMobile ? '100%' : '800px'"
      :centered="isMobile"
      :footer="null"
    >
      <div v-if="currentInvoice" style="padding: 20px">
        <Row :gutter="[16, 16]">
          <Col :span="12">
            <div>
              <strong>发票编号：</strong>{{ currentInvoice.invoiceNo || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>客户名称：</strong>{{ currentInvoice.clientName || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>发票抬头：</strong>{{ currentInvoice.title || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>发票类型：</strong
              >{{ currentInvoice.invoiceTypeName || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>发票金额：</strong>¥{{
                currentInvoice.amount?.toLocaleString() || '0'
              }}
            </div>
          </Col>
          <Col :span="12">
            <div><strong>税率：</strong>{{ currentInvoice.taxRate }}%</div>
          </Col>
          <Col :span="12">
            <div>
              <strong>税额：</strong>¥{{
                currentInvoice.taxAmount?.toLocaleString() || '0'
              }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>总金额：</strong>{{ formatAmount(currentInvoice) }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>状态：</strong>
              <Tag :color="getStatusColor(currentInvoice.status)">
                {{ currentInvoice.statusName }}
              </Tag>
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>申请日期：</strong>{{ currentInvoice.createdAt || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>开票日期：</strong>{{ currentInvoice.invoiceDate || '-' }}
            </div>
          </Col>
          <Col :span="24">
            <div><strong>备注：</strong>{{ currentInvoice.remark || '-' }}</div>
          </Col>
        </Row>
      </div>
    </Modal>
  </Page>
</template>
