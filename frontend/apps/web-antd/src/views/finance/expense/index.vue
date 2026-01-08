<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Button,
  Tag,
  Row,
  Col,
  Statistic,
  Form,
  FormItem,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Empty,
} from 'ant-design-vue';
import { Plus } from '@vben/icons';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { requestClient } from '#/api/request';

defineOptions({ name: 'ExpenseReimbursement' });

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
  const approved = expenses.value.filter(e => e.status === 'APPROVED').reduce((sum, e) => sum + e.amount, 0);
  const pending = expenses.value.filter(e => e.status === 'PENDING').reduce((sum, e) => sum + e.amount, 0);
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

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '报销单号', field: 'expenseNo', width: 140 },
  { title: '费用类型', field: 'expenseTypeName', width: 100 },
  { title: '金额', field: 'amount', width: 120, slots: { default: 'amount' } },
  { title: '关联项目', field: 'matterName', minWidth: 150, showOverflow: true },
  { title: '费用日期', field: 'expenseDate', width: 110 },
  { title: '说明', field: 'description', minWidth: 200, showOverflow: true },
  { title: '状态', field: 'statusName', width: 100, slots: { default: 'status' } },
  { title: '提交时间', field: 'createdAt', width: 110 },
  { title: '操作', field: 'action', width: 100, fixed: 'right', slots: { default: 'action' } },
];

async function loadData() {
  try {
    const res = await requestClient.get<ExpenseRecord[]>('/finance/my/expenses');
    expenses.value = res || [];
    return { items: res || [], total: (res || []).length };
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
    return { items: [], total: 0 };
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: { enabled: false },
    proxyConfig: { ajax: { query: loadData } },
  },
});

// ==================== CRUD 操作 ====================

function handleAdd() {
  Object.assign(formData, {
    expenseType: 'TRAVEL',
    amount: undefined,
    matterId: undefined,
    expenseDate: undefined,
    description: '',
  });
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
      expenseDate: formData.expenseDate?.format?.('YYYY-MM-DD') || formData.expenseDate,
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
    <Row :gutter="16" style="margin-bottom: 16px;">
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic title="报销记录数" :value="stats.count" suffix="条" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic title="报销总额" :value="stats.total" prefix="¥" :precision="2" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic title="已审批" :value="stats.approved" prefix="¥" :precision="2" :value-style="{ color: '#52c41a' }" />
        </Card>
      </Col>
      <Col :xs="12" :sm="6">
        <Card size="small">
          <Statistic title="待审批" :value="stats.pending" prefix="¥" :precision="2" :value-style="{ color: '#faad14' }" />
        </Card>
      </Col>
    </Row>

    <Card>
      <div style="margin-bottom: 16px;">
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
          <a v-if="row.status === 'PENDING'" style="color: red" @click="handleCancel(row)">撤销</a>
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
      width="500px"
      @ok="handleSubmit"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <FormItem label="费用类型" name="expenseType" :rules="[{ required: true, message: '请选择费用类型' }]">
          <Select v-model:value="formData.expenseType" :options="expenseTypeOptions" />
        </FormItem>
        <FormItem label="金额" name="amount" :rules="[{ required: true, message: '请输入金额' }]">
          <InputNumber
            v-model:value="formData.amount"
            :min="0"
            :precision="2"
            prefix="¥"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="费用日期" name="expenseDate" :rules="[{ required: true, message: '请选择费用日期' }]">
          <DatePicker v-model:value="formData.expenseDate" style="width: 100%" />
        </FormItem>
        <FormItem label="费用说明" name="description" :rules="[{ required: true, message: '请输入费用说明' }]">
          <Input.TextArea v-model:value="formData.description" :rows="3" placeholder="请描述费用用途" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
