<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  CreateScheduledReportCommand,
  ScheduledReportDTO,
  ScheduledReportLogDTO,
} from '#/api/workbench/scheduled-report';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  TimePicker,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getReportTemplateList } from '#/api/workbench/report-template';
import {
  createScheduledReport,
  deleteScheduledReport,
  enableScheduledReport,
  executeScheduledReportNow,
  getScheduledReportDetail,
  getScheduledReportList,
  getScheduledReportLogs,
  pauseScheduledReport,
  updateScheduledReport,
} from '#/api/workbench/scheduled-report';

defineOptions({ name: 'ScheduledReportManagement' });

// ==================== 状态选项 ====================
const statusOptions = [
  { label: '启用', value: 'ENABLED', color: 'green' },
  { label: '暂停', value: 'PAUSED', color: 'orange' },
  { label: '已停用', value: 'DISABLED', color: 'default' },
];

// ==================== 搜索表单配置 ====================
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'keyword',
    label: '关键词',
    component: 'Input',
    componentProps: {
      placeholder: '任务名称',
      allowClear: true,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择',
      allowClear: true,
      options: statusOptions,
    },
  },
];

// ==================== 表格配置 ====================
const gridColumns: any[] = [
  { title: '任务名称', field: 'taskName', minWidth: 180, showOverflow: true },
  { title: '报表模板', field: 'templateName', width: 150, showOverflow: true },
  { title: '执行周期', field: 'scheduleDescription', width: 150 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '上次执行', field: 'lastExecuteTime', width: 160, slots: { default: 'lastExecuteTime' } },
  { title: '执行结果', field: 'lastExecuteStatus', width: 100, slots: { default: 'lastExecuteStatus' } },
  { title: '下次执行', field: 'nextExecuteTime', width: 160, slots: { default: 'nextExecuteTime' } },
  { title: '操作', field: 'action', width: 250, fixed: 'right', slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: Record<string, any> & { page: number; pageSize: number }) {
  const res = await getScheduledReportList({
    pageNum: params.page,
    pageSize: params.pageSize,
    keyword: params.keyword,
    status: params.status,
  });
  // 后端返回 records 字段
  const records = (res as any).records || (res as any).list || [];
  return {
    items: records,
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

// ==================== 模板选项 ====================
const templateOptions = ref<{ label: string; value: number }[]>([]);

async function loadTemplates() {
  try {
    const res = await getReportTemplateList({
      pageNum: 1,
      pageSize: 100,
      status: 'ENABLED',
    });
    const records = (res as any).records || (res as any).list || [];
    templateOptions.value = records.map((t: any) => ({
      label: t.templateName,
      value: t.id,
    }));
  } catch (error: any) {
    console.error('加载模板失败', error);
  }
}

// ==================== 编辑弹窗 ====================
const editModalVisible = ref(false);
const editForm = reactive<CreateScheduledReportCommand & { id?: number }>({
  taskName: '',
  description: '',
  templateId: 0,
  scheduleType: 'DAILY',
  executeTime: '08:00',
  executeDayOfWeek: 1,
  executeDayOfMonth: 1,
  outputFormat: 'EXCEL',
  notifyEnabled: false,
  notifyEmails: [],
});
const editLoading = ref(false);
const isEdit = ref(false);

// 执行周期选项
const scheduleTypeOptions = [
  { label: '每天', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '每月', value: 'MONTHLY' },
];

// 星期选项
const weekDayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

function handleCreate() {
  isEdit.value = false;
  Object.assign(editForm, {
    id: undefined,
    taskName: '',
    description: '',
    templateId: 0,
    scheduleType: 'DAILY',
    executeTime: '08:00',
    executeDayOfWeek: 1,
    executeDayOfMonth: 1,
    outputFormat: 'EXCEL',
    notifyEnabled: false,
    notifyEmails: [],
  });
  editModalVisible.value = true;
}

async function handleEdit(row: ScheduledReportDTO) {
  isEdit.value = true;
  try {
    const detail = await getScheduledReportDetail(row.id);
    Object.assign(editForm, {
      id: detail.id,
      taskName: detail.taskName,
      description: detail.description,
      templateId: detail.templateId,
      scheduleType: detail.scheduleType,
      executeTime: detail.executeTime,
      executeDayOfWeek: detail.executeDayOfWeek,
      executeDayOfMonth: detail.executeDayOfMonth,
      outputFormat: detail.outputFormat || 'EXCEL',
      notifyEnabled: detail.notifyEnabled,
      notifyEmails: detail.notifyEmails || [],
    });
    editModalVisible.value = true;
  } catch (error: any) {
    message.error(`加载详情失败：${error.message || '未知错误'}`);
  }
}

async function handleEditSubmit() {
  if (!editForm.taskName?.trim()) {
    message.warning('请输入任务名称');
    return;
  }
  if (!editForm.templateId) {
    message.warning('请选择报表模板');
    return;
  }

  editLoading.value = true;
  try {
    const data: CreateScheduledReportCommand = {
      taskName: editForm.taskName,
      description: editForm.description,
      templateId: editForm.templateId,
      scheduleType: editForm.scheduleType,
      executeTime: editForm.executeTime,
      executeDayOfWeek: editForm.executeDayOfWeek,
      executeDayOfMonth: editForm.executeDayOfMonth,
      outputFormat: editForm.outputFormat,
      notifyEnabled: editForm.notifyEnabled,
      notifyEmails: editForm.notifyEmails,
    };

    if (isEdit.value && editForm.id) {
      await updateScheduledReport(editForm.id, data);
      message.success('更新成功');
    } else {
      await createScheduledReport(data);
      message.success('创建成功');
    }
    editModalVisible.value = false;
    gridApi.reload();
  } catch (error: any) {
    message.error(`操作失败：${error.message || '未知错误'}`);
  } finally {
    editLoading.value = false;
  }
}

// ==================== 执行记录弹窗 ====================
const logsModalVisible = ref(false);
const logsData = ref<ScheduledReportLogDTO[]>([]);
const logsLoading = ref(false);
const logsTotal = ref(0);
const logsPage = ref(1);
const currentTaskId = ref<number | null>(null);

const logColumns = [
  { title: '执行时间', dataIndex: 'executeTime', key: 'executeTime', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '耗时', dataIndex: 'durationDisplay', key: 'durationDisplay', width: 100 },
  { title: '文件大小', dataIndex: 'fileSizeDisplay', key: 'fileSizeDisplay', width: 100 },
  { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
];

async function handleViewLogs(row: ScheduledReportDTO) {
  currentTaskId.value = row.id;
  logsPage.value = 1;
  logsModalVisible.value = true;
  await loadLogs();
}

async function loadLogs() {
  if (!currentTaskId.value) return;
  logsLoading.value = true;
  try {
    const res = await getScheduledReportLogs(currentTaskId.value, {
      pageNum: logsPage.value,
      pageSize: 10,
    });
    const records = (res as any).records || (res as any).list || [];
    logsData.value = records;
    logsTotal.value = res.total || 0;
  } catch (error: any) {
    message.error(`加载执行记录失败：${error.message || '未知错误'}`);
  } finally {
    logsLoading.value = false;
  }
}

function handleLogsPageChange(page: number) {
  logsPage.value = page;
  loadLogs();
}

// ==================== 操作方法 ====================
async function handleEnable(row: ScheduledReportDTO) {
  try {
    await enableScheduledReport(row.id);
    message.success('启用成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`启用失败：${error.message || '未知错误'}`);
  }
}

async function handlePause(row: ScheduledReportDTO) {
  try {
    await pauseScheduledReport(row.id);
    message.success('暂停成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`暂停失败：${error.message || '未知错误'}`);
  }
}

async function handleExecuteNow(row: ScheduledReportDTO) {
  try {
    message.loading({ content: '正在执行...', key: 'execute' });
    await executeScheduledReportNow(row.id);
    message.success({ content: '执行成功', key: 'execute' });
    gridApi.reload();
  } catch (error: any) {
    message.error({
      content: `执行失败：${error.message || '未知错误'}`,
      key: 'execute',
    });
  }
}

async function handleDelete(row: ScheduledReportDTO) {
  try {
    await deleteScheduledReport(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`删除失败：${error.message || '未知错误'}`);
  }
}

// ==================== 工具方法 ====================
function getStatusColor(status: string) {
  const option = statusOptions.find((o) => o.value === status);
  return option?.color || 'default';
}

function getStatusName(status: string) {
  const option = statusOptions.find((o) => o.value === status);
  return option?.label || status;
}

function getExecuteStatusColor(status: string) {
  if (status === 'SUCCESS') return 'green';
  if (status === 'FAILED') return 'red';
  if (status === 'RUNNING') return 'blue';
  return 'default';
}

function getExecuteStatusName(status: string) {
  if (status === 'SUCCESS') return '成功';
  if (status === 'FAILED') return '失败';
  if (status === 'RUNNING') return '执行中';
  return status || '-';
}

function formatDateTime(date: string | null | undefined) {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
}

// 初始化
onMounted(() => {
  loadTemplates();
});
</script>

<template>
  <Page
    title="定时报表管理"
    description="配置定时生成报表任务，支持每天、每周、每月执行"
  >
    <Grid>
      <!-- 工具栏 -->
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">新建任务</Button>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName || getStatusName(row.status) }}
        </Tag>
      </template>

      <!-- 上次执行时间列 -->
      <template #lastExecuteTime="{ row }">
        {{ formatDateTime(row.lastExecuteTime) }}
      </template>

      <!-- 执行结果列 -->
      <template #lastExecuteStatus="{ row }">
        <Tag
          v-if="row.lastExecuteStatus"
          :color="getExecuteStatusColor(row.lastExecuteStatus)"
        >
          {{ row.lastExecuteStatusName || getExecuteStatusName(row.lastExecuteStatus) }}
        </Tag>
        <span v-else>-</span>
      </template>

      <!-- 下次执行时间列 -->
      <template #nextExecuteTime="{ row }">
        {{ formatDateTime(row.nextExecuteTime) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleEdit(row as ScheduledReportDTO)">编辑</a>
          <a
            v-if="row.status === 'PAUSED' || row.status === 'DISABLED'"
            @click="handleEnable(row as ScheduledReportDTO)"
          >
            启用
          </a>
          <a
            v-if="row.status === 'ENABLED'"
            @click="handlePause(row as ScheduledReportDTO)"
          >
            暂停
          </a>
          <a @click="handleExecuteNow(row as ScheduledReportDTO)">立即执行</a>
          <a @click="handleViewLogs(row as ScheduledReportDTO)">记录</a>
          <Popconfirm
            title="确定删除此任务？"
            @confirm="handleDelete(row as ScheduledReportDTO)"
          >
            <a style="color: #ff4d4f">删除</a>
          </Popconfirm>
        </Space>
      </template>
    </Grid>

    <!-- 编辑弹窗 -->
    <Modal
      v-model:open="editModalVisible"
      :title="isEdit ? '编辑任务' : '新建任务'"
      :confirm-loading="editLoading"
      width="600px"
      @ok="handleEditSubmit"
    >
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <Form.Item label="任务名称" required>
          <Input
            v-model:value="editForm.taskName"
            placeholder="请输入任务名称"
            :maxlength="100"
          />
        </Form.Item>
        <Form.Item label="报表模板" required>
          <Select
            v-model:value="editForm.templateId"
            placeholder="请选择报表模板"
            :options="templateOptions"
          />
        </Form.Item>
        <Form.Item label="执行周期" required>
          <Select
            v-model:value="editForm.scheduleType"
            :options="scheduleTypeOptions"
          />
        </Form.Item>
        <Form.Item v-if="editForm.scheduleType === 'WEEKLY'" label="执行日">
          <Select
            v-model:value="editForm.executeDayOfWeek"
            :options="weekDayOptions"
          />
        </Form.Item>
        <Form.Item v-if="editForm.scheduleType === 'MONTHLY'" label="执行日">
          <InputNumber
            v-model:value="editForm.executeDayOfMonth"
            :min="1"
            :max="28"
            placeholder="每月几号"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="执行时间">
          <TimePicker
            v-model:value="editForm.executeTime"
            value-format="HH:mm"
            format="HH:mm"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="描述">
          <Input.TextArea
            v-model:value="editForm.description"
            :rows="2"
            placeholder="请输入描述"
          />
        </Form.Item>
        <Form.Item label="启用通知">
          <Switch v-model:checked="editForm.notifyEnabled" />
        </Form.Item>
        <Form.Item v-if="editForm.notifyEnabled" label="通知邮箱">
          <Select
            v-model:value="editForm.notifyEmails"
            mode="tags"
            placeholder="输入邮箱后回车添加"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 执行记录弹窗 -->
    <Modal
      v-model:open="logsModalVisible"
      title="执行记录"
      :footer="null"
      width="800px"
    >
      <Spin :spinning="logsLoading">
        <Table
          :columns="logColumns"
          :data-source="logsData"
          :pagination="{
            current: logsPage,
            pageSize: 10,
            total: logsTotal,
            showTotal: (t: number) => `共 ${t} 条`,
            onChange: handleLogsPageChange,
          }"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'executeTime'">
              {{ formatDateTime(record.executeTime) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="getExecuteStatusColor(record.status)">
                {{ record.statusName || getExecuteStatusName(record.status) }}
              </Tag>
            </template>
          </template>
        </Table>
      </Spin>
    </Modal>
  </Page>
</template>
