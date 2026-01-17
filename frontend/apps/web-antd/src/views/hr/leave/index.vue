<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { LeaveApplication, LeaveBalance, LeaveType } from '#/api/hr/types';

import { onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  FormItem,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveLeave,
  cancelLeave,
  createLeave,
  fetchLeaveList,
  getLeaveBalance,
  getLeaveTypes,
  rejectLeave,
} from '#/api/hr/leave';

defineOptions({ name: 'LeaveManagement' });

// 响应式布局
const { isMobile } = useResponsive();

const { RangePicker } = DatePicker;

// ==================== 状态定义 ====================

const searchForm = reactive({
  status: undefined as string | undefined,
  leaveTypeId: undefined as number | undefined,
});

const leaveTypes = ref<LeaveType[]>([]);
const leaveBalances = ref<LeaveBalance[]>([]);
const modalVisible = ref(false);
const modalLoading = ref(false);
const leaveForm = reactive({
  leaveTypeId: undefined as number | undefined,
  startDate: '',
  endDate: '',
  leaveDays: 1,
  reason: '',
});

// ==================== 常量选项 ====================

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
];

const statusColorMap: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
  CANCELLED: 'default',
};

const statusTextMap: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
};

// ==================== 表格配置 ====================

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    { title: '请假类型', field: 'leaveTypeName', width: 100, mobileShow: true },
    { title: '开始日期', field: 'startDate', width: 120, mobileShow: true },
    { title: '结束日期', field: 'endDate', width: 120 },
    { title: '请假天数', field: 'leaveDays', width: 100, mobileShow: true },
    { title: '请假原因', field: 'reason', minWidth: 200, showOverflow: true },
    {
      title: '状态',
      field: 'status',
      width: 100,
      slots: { default: 'status' },
      mobileShow: true,
    },
    { title: '审批人', field: 'approverName', width: 100 },
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
    ...searchForm,
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  };
  const res = await fetchLeaveList(params);
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

// ==================== 数据加载 ====================

async function fetchLeaveTypesData() {
  try {
    leaveTypes.value = await getLeaveTypes();
  } catch {
    leaveTypes.value = [];
  }
}

async function fetchLeaveBalanceData() {
  try {
    leaveBalances.value = await getLeaveBalance();
  } catch {
    leaveBalances.value = [];
  }
}

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  Object.assign(searchForm, { status: undefined, leaveTypeId: undefined });
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  Object.assign(leaveForm, {
    leaveTypeId: undefined,
    startDate: '',
    endDate: '',
    leaveDays: 1,
    reason: '',
  });
  modalVisible.value = true;
}

async function handleSubmit() {
  if (!leaveForm.leaveTypeId) {
    message.warning('请选择请假类型');
    return;
  }
  if (!leaveForm.startDate || !leaveForm.endDate) {
    message.warning('请选择请假日期');
    return;
  }
  if (!leaveForm.reason) {
    message.warning('请填写请假原因');
    return;
  }

  modalLoading.value = true;
  try {
    await createLeave({
      leaveTypeId: leaveForm.leaveTypeId,
      startDate: leaveForm.startDate,
      endDate: leaveForm.endDate,
      reason: leaveForm.reason,
    });
    message.success('提交成功');
    modalVisible.value = false;
    gridApi.reload();
    fetchLeaveBalanceData();
  } catch (error: any) {
    message.error(error?.message || '提交失败');
  } finally {
    modalLoading.value = false;
  }
}

async function handleCancel(row: LeaveApplication) {
  try {
    await cancelLeave(row.id);
    message.success('取消成功');
    gridApi.reload();
    fetchLeaveBalanceData();
  } catch (error: any) {
    message.error(error?.message || '取消失败');
  }
}

async function handleApprove(row: LeaveApplication) {
  try {
    await approveLeave({ applicationId: row.id });
    message.success('审批通过');
    gridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '审批失败');
  }
}

async function handleReject(row: LeaveApplication) {
  try {
    await rejectLeave({ applicationId: row.id });
    message.success('已拒绝');
    gridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '操作失败');
  }
}

function handleDateRangeChange(dates: any) {
  if (dates && dates.length === 2) {
    leaveForm.startDate = dates[0].format('YYYY-MM-DD');
    leaveForm.endDate = dates[1].format('YYYY-MM-DD');
    const start = dates[0].valueOf();
    const end = dates[1].valueOf();
    leaveForm.leaveDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;
  } else {
    leaveForm.startDate = '';
    leaveForm.endDate = '';
    leaveForm.leaveDays = 1;
  }
}

onMounted(() => {
  fetchLeaveTypesData();
  fetchLeaveBalanceData();
});
</script>

<template>
  <Page title="请假管理" description="请假申请、审批与假期余额查询">
    <div class="space-y-4 p-4">
      <!-- 假期余额卡片 -->
      <Card title="假期余额">
        <div class="flex flex-wrap gap-6">
          <div
            v-for="balance in leaveBalances"
            :key="balance.leaveTypeId"
            class="text-center"
          >
            <div class="text-sm text-gray-500">{{ balance.leaveTypeName }}</div>
            <div class="text-2xl font-bold text-blue-500">
              {{ balance.remainingDays }}
            </div>
            <div class="text-xs text-gray-400">
              剩余 / {{ balance.totalDays }} 天
            </div>
          </div>
        </div>
      </Card>

      <!-- 搜索区域 -->
      <Card>
        <Row :gutter="[16, 12]">
          <Col :xs="12" :sm="12" :md="6">
            <Select
              v-model:value="searchForm.leaveTypeId"
              placeholder="请假类型"
              allow-clear
              style="width: 100%"
            >
              <Select.Option
                v-for="type in leaveTypes"
                :key="type.id"
                :value="type.id"
              >
                {{ type.name }}
              </Select.Option>
            </Select>
          </Col>
          <Col :xs="12" :sm="12" :md="6">
            <Select
              v-model:value="searchForm.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="12">
            <Space :wrap="isMobile">
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />申请请假
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <!-- 表格区域 -->
      <Card title="请假记录">
        <Grid>
          <template #status="{ row }">
            <Tag :color="statusColorMap[row.status]">
              {{ statusTextMap[row.status] }}
            </Tag>
          </template>
          <template #action="{ row }">
            <Space>
              <a v-if="row.status === 'PENDING'" @click="handleCancel(row)"
                >取消</a
              >
              <a v-if="row.status === 'PENDING'" @click="handleApprove(row)"
                >通过</a
              >
              <a
                v-if="row.status === 'PENDING'"
                style="color: red"
                @click="handleReject(row)"
                >拒绝</a
              >
            </Space>
          </template>
        </Grid>
      </Card>

      <!-- 新增请假弹窗 -->
      <Modal
        v-model:open="modalVisible"
        title="申请请假"
        :width="isMobile ? '100%' : '520px'"
        :centered="isMobile"
        :confirm-loading="modalLoading"
        @ok="handleSubmit"
      >
        <Form :model="leaveForm" layout="vertical">
          <FormItem label="请假类型" required>
            <Select
              v-model:value="leaveForm.leaveTypeId"
              placeholder="请选择请假类型"
            >
              <Select.Option
                v-for="type in leaveTypes"
                :key="type.id"
                :value="type.id"
              >
                {{ type.name }}
              </Select.Option>
            </Select>
          </FormItem>
          <FormItem label="请假日期" required>
            <RangePicker style="width: 100%" @change="handleDateRangeChange" />
          </FormItem>
          <FormItem label="请假天数">
            <InputNumber
              v-model:value="leaveForm.leaveDays"
              :min="0.5"
              :step="0.5"
              style="width: 100%"
            />
          </FormItem>
          <FormItem label="请假原因" required>
            <Textarea
              v-model:value="leaveForm.reason"
              placeholder="请输入请假原因"
              :rows="4"
            />
          </FormItem>
        </Form>
      </Modal>
    </div>
  </Page>
</template>
