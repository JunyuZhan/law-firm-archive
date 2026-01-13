<script setup lang="ts">
import type {
  ApplyOvertimeCommand,
  ApproveOvertimeRequest,
  OvertimeApplicationDTO,
} from '#/api/admin/overtime';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Row,
  Space,
  Table,
  Tag,
  Textarea,
  TimePicker,
} from 'ant-design-vue';

import {
  applyOvertime,
  approveOvertime,
  getMyOvertimeApplications,
  getOvertimeApplicationsByDateRange,
} from '#/api/admin/overtime';

defineOptions({ name: 'OvertimeManagement' });

const { RangePicker } = DatePicker;

// 搜索表单
const searchForm = reactive({
  startDate: '',
  endDate: '',
});

// 状态标签颜色
const statusColorMap: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
};

// 状态文本
const statusTextMap: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
};

// 表格列
const columns = [
  {
    title: '申请编号',
    dataIndex: 'applicationNo',
    key: 'applicationNo',
    width: 120,
  },
  { title: '申请人', dataIndex: 'userName', key: 'userName', width: 100 },
  {
    title: '加班日期',
    dataIndex: 'overtimeDate',
    key: 'overtimeDate',
    width: 120,
  },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 100 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 100 },
  {
    title: '加班时长',
    dataIndex: 'overtimeHours',
    key: 'overtimeHours',
    width: 100,
  },
  { title: '加班原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  {
    title: '审批人',
    dataIndex: 'approverName',
    key: 'approverName',
    width: 100,
  },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<OvertimeApplicationDTO[]>([]);
const loading = ref(false);

// 新增弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const overtimeForm = reactive<ApplyOvertimeCommand>({
  overtimeDate: '',
  startTime: '',
  endTime: '',
  reason: '',
  workContent: '',
});

// 审批弹窗
const approveModalVisible = ref(false);
const approveModalLoading = ref(false);
const currentRecord = ref<null | OvertimeApplicationDTO>(null);
const approveForm = reactive<ApproveOvertimeRequest>({
  approved: true,
  comment: '',
});

// 获取加班列表
async function fetchData() {
  loading.value = true;
  try {
    let data: OvertimeApplicationDTO[];
    data = await (searchForm.startDate && searchForm.endDate
      ? getOvertimeApplicationsByDateRange(
          searchForm.startDate,
          searchForm.endDate,
        )
      : getMyOvertimeApplications());
    tableData.value = data || [];
  } catch (error) {
    console.error('获取加班列表失败:', error);
    message.error('获取加班列表失败');
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
  searchForm.startDate = '';
  searchForm.endDate = '';
  fetchData();
}

// 日期范围变化
function handleDateRangeChange(dates: any) {
  if (dates && dates.length === 2) {
    searchForm.startDate = dates[0].format('YYYY-MM-DD');
    searchForm.endDate = dates[1].format('YYYY-MM-DD');
  } else {
    searchForm.startDate = '';
    searchForm.endDate = '';
  }
}

// 新增加班申请
function handleAdd() {
  Object.assign(overtimeForm, {
    overtimeDate: '',
    startTime: '',
    endTime: '',
    reason: '',
    workContent: '',
  });
  modalVisible.value = true;
}

// 提交加班申请
async function handleSubmit() {
  if (!overtimeForm.overtimeDate) {
    message.warning('请选择加班日期');
    return;
  }
  if (!overtimeForm.startTime) {
    message.warning('请选择开始时间');
    return;
  }
  if (!overtimeForm.endTime) {
    message.warning('请选择结束时间');
    return;
  }

  modalLoading.value = true;
  try {
    await applyOvertime(overtimeForm);
    message.success('提交成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '提交失败');
  } finally {
    modalLoading.value = false;
  }
}

// 审批加班申请
function handleApprove(record: Record<string, any>) {
  currentRecord.value = record as OvertimeApplicationDTO;
  approveForm.approved = true;
  approveForm.comment = '';
  approveModalVisible.value = true;
}

// 拒绝加班申请
function handleReject(record: Record<string, any>) {
  currentRecord.value = record as OvertimeApplicationDTO;
  approveForm.approved = false;
  approveForm.comment = '';
  approveModalVisible.value = true;
}

// 提交审批
async function handleApproveSubmit() {
  if (!currentRecord.value) return;

  approveModalLoading.value = true;
  try {
    await approveOvertime(currentRecord.value.id, approveForm);
    message.success('审批成功');
    approveModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '审批失败');
  } finally {
    approveModalLoading.value = false;
  }
}

// 查看详情
const detailModalVisible = ref(false);
const detailData = ref<null | OvertimeApplicationDTO>(null);

function handleView(record: Record<string, any>) {
  detailData.value = record as OvertimeApplicationDTO;
  detailModalVisible.value = true;
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="加班管理" description="加班申请与审批">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="8" :lg="6">
            <RangePicker style="width: 100%" @change="handleDateRangeChange" />
          </Col>
          <Col :xs="24" :sm="12" :md="16" :lg="18">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">申请加班</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'overtimeHours'">
            <span>{{
              record.overtimeHours ? `${record.overtimeHours} 小时` : '-'
            }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="statusColorMap[record.status || '']">
              {{ statusTextMap[record.status || ''] || record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <template v-if="record.status === 'PENDING'">
                <a @click="handleApprove(record)">通过</a>
                <a style="color: #ff4d4f" @click="handleReject(record)">拒绝</a>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增加班申请弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="申请加班"
      :confirm-loading="modalLoading"
      width="600px"
      @ok="handleSubmit"
    >
      <Form :model="overtimeForm" layout="vertical">
        <FormItem label="加班日期" required>
          <DatePicker
            v-model:value="overtimeForm.overtimeDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="开始时间" required>
          <TimePicker
            v-model:value="overtimeForm.startTime"
            style="width: 100%"
            format="HH:mm"
            value-format="HH:mm"
          />
        </FormItem>
        <FormItem label="结束时间" required>
          <TimePicker
            v-model:value="overtimeForm.endTime"
            style="width: 100%"
            format="HH:mm"
            value-format="HH:mm"
          />
        </FormItem>
        <FormItem label="加班原因">
          <Input
            v-model:value="overtimeForm.reason"
            placeholder="请输入加班原因"
          />
        </FormItem>
        <FormItem label="工作内容">
          <Textarea
            v-model:value="overtimeForm.workContent"
            placeholder="请输入工作内容"
            :rows="4"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 审批弹窗 -->
    <Modal
      v-model:open="approveModalVisible"
      :title="approveForm.approved ? '审批通过' : '审批拒绝'"
      :confirm-loading="approveModalLoading"
      @ok="handleApproveSubmit"
    >
      <Form :model="approveForm" layout="vertical">
        <FormItem label="审批意见">
          <Textarea
            v-model:value="approveForm.comment"
            placeholder="请输入审批意见"
            :rows="4"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="加班申请详情"
      width="600px"
      :footer="null"
    >
      <div v-if="detailData" style="line-height: 2">
        <p><strong>申请编号:</strong> {{ detailData.applicationNo || '-' }}</p>
        <p><strong>申请人:</strong> {{ detailData.userName || '-' }}</p>
        <p><strong>加班日期:</strong> {{ detailData.overtimeDate || '-' }}</p>
        <p><strong>开始时间:</strong> {{ detailData.startTime || '-' }}</p>
        <p><strong>结束时间:</strong> {{ detailData.endTime || '-' }}</p>
        <p>
          <strong>加班时长:</strong> {{ detailData.overtimeHours || '-' }} 小时
        </p>
        <p><strong>加班原因:</strong> {{ detailData.reason || '-' }}</p>
        <p><strong>工作内容:</strong> {{ detailData.workContent || '-' }}</p>
        <p><strong>状态:</strong> {{ detailData.statusName || '-' }}</p>
        <p v-if="detailData.approvalComment">
          <strong>审批意见:</strong> {{ detailData.approvalComment }}
        </p>
      </div>
    </Modal>
  </Page>
</template>
