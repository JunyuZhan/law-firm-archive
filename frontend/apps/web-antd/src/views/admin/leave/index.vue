<script setup lang="ts">
import type { LeaveApplication, LeaveType } from '#/api/hr/types';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  approveLeave,
  cancelLeave,
  createLeave,
  fetchLeaveList,
  getLeaveTypes,
  rejectLeave,
} from '#/api/hr/leave';

defineOptions({ name: 'AdminLeave' });

const loading = ref(false);
const dataSource = ref<LeaveApplication[]>([]);
const total = ref(0);
const activeTab = ref('my');
const leaveTypes = ref<LeaveType[]>([]);

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as string | undefined,
});

const modalVisible = ref(false);
const detailVisible = ref(false);
const approveVisible = ref(false);
const currentRecord = ref<LeaveApplication | null>(null);

const formData = ref({
  leaveType: undefined as string | undefined,
  startTime: undefined as any,
  endTime: undefined as any,
  reason: '',
});

const approveForm = ref({
  approved: 1 as number,
  comment: '',
});

const columns = [
  { title: '申请人', dataIndex: 'realName', key: 'realName', width: 100 },
  {
    title: '请假类型',
    dataIndex: 'leaveTypeName',
    key: 'leaveTypeName',
    width: 100,
  },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '请假天数', dataIndex: 'leaveDays', key: 'leaveDays', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

const statusMap: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'processing', text: '待审批' },
  APPROVED: { color: 'success', text: '已通过' },
  REJECTED: { color: 'error', text: '已拒绝' },
  CANCELLED: { color: 'default', text: '已取消' },
};

async function loadData() {
  loading.value = true;
  try {
    const res = await fetchLeaveList({
      ...queryParams.value,
      applicationType: activeTab.value,
    });
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadLeaveTypes() {
  try {
    const res = await getLeaveTypes();
    leaveTypes.value = res || [];
  } catch (error) {
    console.error('加载请假类型失败', error);
  }
}

function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  queryParams.value.pageNum = 1;
  loadData();
}

function handleSearch() {
  queryParams.value.pageNum = 1;
  loadData();
}

function handleTableChange(pagination: any) {
  queryParams.value.pageNum = pagination.current;
  queryParams.value.pageSize = pagination.pageSize;
  loadData();
}

function handleAdd() {
  formData.value = {
    leaveType: undefined,
    startTime: undefined,
    endTime: undefined,
    reason: '',
  };
  modalVisible.value = true;
}

async function handleSubmit() {
  if (
    !formData.value.leaveType ||
    !formData.value.startTime ||
    !formData.value.endTime
  ) {
    message.error('请填写必填项');
    return;
  }
  try {
    await createLeave({
      leaveTypeId: Number(formData.value.leaveType),
      startDate: formData.value.startTime.format('YYYY-MM-DD HH:mm:ss'),
      endDate: formData.value.endTime.format('YYYY-MM-DD HH:mm:ss'),
      reason: formData.value.reason,
    });
    message.success('申请提交成功');
    modalVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '提交失败');
  }
}

async function handleView(record: LeaveApplication) {
  currentRecord.value = record;
  detailVisible.value = true;
}

async function handleCancel(id: number) {
  try {
    await cancelLeave(id);
    message.success('已取消申请');
    loadData();
  } catch (error: any) {
    message.error(error.message || '取消失败');
  }
}

function openApprove(record: LeaveApplication) {
  currentRecord.value = record;
  approveForm.value = { approved: 1, comment: '' };
  approveVisible.value = true;
}

async function submitApproval() {
  if (!currentRecord.value) return;
  try {
    if (approveForm.value.approved === 1) {
      await approveLeave({
        applicationId: currentRecord.value.id,
        comment: approveForm.value.comment,
      });
      message.success('已通过申请');
    } else {
      await rejectLeave({
        applicationId: currentRecord.value.id,
        comment: approveForm.value.comment,
      });
      message.success('已拒绝申请');
    }
    approveVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '审批失败');
  }
}

function formatDateTime(time?: string) {
  return time ? dayjs(time).format('YYYY-MM-DD') : '-';
}

onMounted(() => {
  loadData();
  loadLeaveTypes();
});
</script>

<template>
  <Page title="请假管理" description="管理请假申请">
    <Card>
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="my" tab="我的申请" />
        <Tabs.TabPane key="pending" tab="待审批" />
        <Tabs.TabPane key="all" tab="全部申请" />
      </Tabs>

      <div
        style="
          display: flex;
          justify-content: space-between;
          margin-bottom: 16px;
        "
      >
        <Space>
          <Input
            v-model:value="queryParams.keyword"
            placeholder="搜索申请人"
            style="width: 200px"
            allow-clear
            @press-enter="handleSearch"
          />
          <Select
            v-model:value="queryParams.status"
            placeholder="状态"
            style="width: 120px"
            allow-clear
            :options="
              Object.entries(statusMap).map(([k, v]) => ({
                label: v.text,
                value: k,
              }))
            "
            @change="handleSearch"
          />
          <Button @click="handleSearch">查询</Button>
        </Space>
        <Button type="primary" @click="handleAdd"><Plus />申请请假</Button>
      </div>

      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
        }"
        :scroll="{ x: 1000 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template
            v-if="column.key === 'startDate' || column.key === 'endDate'"
          >
            {{
              formatDateTime(String((record as any)[(column as any).dataIndex]))
            }}
          </template>
          <template v-else-if="column.key === 'leaveDays'">
            {{ record.leaveDays }} 天
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{ statusMap[record.status]?.text || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record as LeaveApplication)">查看</a>
              <Popconfirm
                v-if="
                  (record as LeaveApplication).status === 'PENDING' &&
                  activeTab === 'my'
                "
                title="确定要取消此申请吗？"
                @confirm="handleCancel((record as LeaveApplication).id)"
              >
                <a style="color: #ff4d4f">取消</a>
              </Popconfirm>
              <a
                v-if="
                  (record as LeaveApplication).status === 'PENDING' &&
                  activeTab === 'pending'
                "
                @click="openApprove(record as LeaveApplication)"
                >审批</a
              >
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 申请弹窗 -->
    <Modal v-model:open="modalVisible" title="申请请假" @ok="handleSubmit">
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="请假类型" required>
          <Select
            v-model:value="formData.leaveType"
            placeholder="选择请假类型"
            :options="leaveTypes.map((t) => ({ label: t.name, value: t.code }))"
          />
        </FormItem>
        <FormItem label="开始时间" required>
          <DatePicker
            v-model:value="formData.startTime"
            show-time
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="结束时间" required>
          <DatePicker
            v-model:value="formData.endTime"
            show-time
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="请假事由">
          <Textarea
            v-model:value="formData.reason"
            :rows="3"
            placeholder="请说明请假原因"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal v-model:open="detailVisible" title="请假详情" :footer="null">
      <Descriptions v-if="currentRecord" :column="2" bordered size="small">
        <DescriptionsItem label="申请人">
          {{ currentRecord.realName }}
        </DescriptionsItem>
        <DescriptionsItem label="请假类型">
          {{ currentRecord.leaveTypeName }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusMap[currentRecord.status]?.color">
            {{ statusMap[currentRecord.status]?.text }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="请假天数">
          {{ currentRecord.leaveDays }} 天
        </DescriptionsItem>
        <DescriptionsItem label="开始日期">
          {{ formatDateTime(currentRecord.startDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="结束日期">
          {{ formatDateTime(currentRecord.endDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="请假事由" :span="2">
          {{ currentRecord.reason || '-' }}
        </DescriptionsItem>
        <DescriptionsItem v-if="currentRecord.approverName" label="审批人">
          {{ currentRecord.approverName }}
        </DescriptionsItem>
        <DescriptionsItem v-if="currentRecord.approveComment" label="审批意见">
          {{ currentRecord.approveComment }}
        </DescriptionsItem>
      </Descriptions>
    </Modal>

    <!-- 审批弹窗 -->
    <Modal
      v-model:open="approveVisible"
      title="审批请假申请"
      @ok="submitApproval"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="审批结果">
          <Select
            v-model:value="approveForm.approved"
            :options="[
              { label: '通过', value: 1 },
              { label: '拒绝', value: 0 },
            ]"
          />
        </FormItem>
        <FormItem label="审批意见">
          <Textarea
            v-model:value="approveForm.comment"
            :rows="3"
            placeholder="请输入审批意见"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
