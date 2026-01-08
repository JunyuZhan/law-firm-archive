<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Button,
  Space,
  Tag,
  Select,
  Form,
  FormItem,
  DatePicker,
  Input,
  Textarea,
  Row,
  Col,
  Tabs,
  Popconfirm,
} from 'ant-design-vue';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getTaskList,
  createTask,
  updateTask,
  deleteTask,
  changeTaskStatus,
  approveTask,
  rejectTask,
} from '#/api/matter';
import { getMatterList } from '#/api/matter';
import { useUserStore } from '@vben/stores';
import type { TaskDTO, CreateTaskCommand } from '#/api/matter/types';
import type { MatterDTO } from '#/api/matter/types';
import { UserTreeSelect } from '#/components/UserTreeSelect';

defineOptions({ name: 'MatterTask' });

const userStore = useUserStore();

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const modalTitle = ref('新增任务');
const formRef = ref();
const matters = ref<MatterDTO[]>([]);
const activeTab = ref('all');
const reviewModalVisible = ref(false);
const reviewComment = ref('');
const reviewingTask = ref<TaskDTO | null>(null);

// 表单数据
const formData = reactive<Partial<CreateTaskCommand> & { id?: number }>({
  id: undefined,
  matterId: undefined,
  title: '',
  description: '',
  assigneeId: undefined,
  priority: 'MEDIUM',
  dueDate: undefined,
});

// ==================== 常量选项 ====================

const priorityOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待处理', value: 'TODO' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '待验收', value: 'PENDING_REVIEW' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
];

// ==================== 表格配置 ====================

const gridColumns = [
  { title: '任务标题', field: 'title', width: 200, showOverflow: true },
  { title: '所属项目', field: 'matterName', width: 180, showOverflow: true },
  { title: '负责人', field: 'assigneeName', width: 100 },
  { title: '优先级', field: 'priority', width: 100, slots: { default: 'priority' } },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '截止日期', field: 'dueDate', width: 120 },
  { title: '创建时间', field: 'createdAt', width: 160 },
  { title: '操作', field: 'action', width: 200, fixed: 'right' as const, slots: { default: 'action' } },
];

// 查询参数
const queryParams = reactive({
  matterId: undefined as number | undefined,
  status: undefined as string | undefined,
  priority: undefined as string | undefined,
});

// 加载数据
async function loadData(params: { page: number; pageSize: number }) {
  const res = await getTaskList({
    pageNum: params.page,
    pageSize: params.pageSize,
    matterId: queryParams.matterId,
    status: queryParams.status,
    priority: queryParams.priority,
  });
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page }: { page: any }) => {
          return await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
          });
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
    },
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// ==================== 数据加载 ====================

async function loadOptions() {
  try {
    const matterRes = await getMatterList({ pageNum: 1, pageSize: 1000 });
    matters.value = matterRes.list;
  } catch (error) {
    console.error('加载选项失败:', error);
  }
}

// 计算属性：项目选项
const matterOptions = computed(() => 
  matters.value.map(m => ({ 
    label: `[${m.matterNo}] ${m.name}`, 
    value: m.id 
  }))
);

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.matterId = undefined;
  queryParams.status = undefined;
  queryParams.priority = undefined;
  gridApi.reload();
}

// Tab切换
function handleTabChange(key: string | number) {
  activeTab.value = String(key);
  if (key === 'all') {
    queryParams.status = undefined;
  } else if (key === 'pending') {
    queryParams.status = 'TODO';
  } else if (key === 'in_progress') {
    queryParams.status = 'IN_PROGRESS';
  } else if (key === 'pending_review') {
    queryParams.status = 'PENDING_REVIEW';
  } else if (key === 'completed') {
    queryParams.status = 'COMPLETED';
  }
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  modalTitle.value = '新增任务';
  Object.assign(formData, {
    id: undefined,
    matterId: undefined,
    title: '',
    description: '',
    assigneeId: undefined,
    priority: 'MEDIUM',
    dueDate: undefined,
  });
  modalVisible.value = true;
}

function handleEdit(row: TaskDTO) {
  modalTitle.value = '编辑任务';
  Object.assign(formData, {
    id: row.id,
    matterId: row.matterId,
    title: row.title,
    description: row.description,
    assigneeId: row.assigneeId,
    priority: row.priority,
    dueDate: row.dueDate,
  });
  modalVisible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();
    
    if (formData.id) {
      await updateTask(formData.id, formData as Partial<CreateTaskCommand>);
      message.success('更新成功');
    } else {
      await createTask(formData as CreateTaskCommand);
      message.success('创建成功');
    }
    modalVisible.value = false;
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { errorFields?: any; message?: string };
    if (err?.errorFields) return;
    message.error(err.message || '操作失败');
  }
}

function handleDelete(row: TaskDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除任务 "${row.title}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteTask(row.id);
        message.success('删除成功');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

function handleComplete(row: TaskDTO) {
  Modal.confirm({
    title: '确认完成',
    content: `确定要完成任务 "${row.title}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await changeTaskStatus(row.id, 'COMPLETED');
        message.success('任务已完成');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '操作失败');
      }
    },
  });
}

// ==================== 验收操作 ====================

function handleReview(task: TaskDTO) {
  reviewingTask.value = task;
  reviewComment.value = '';
  reviewModalVisible.value = true;
}

async function handleApprove() {
  if (!reviewingTask.value) return;
  try {
    await approveTask(reviewingTask.value.id);
    message.success('任务验收通过');
    reviewModalVisible.value = false;
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '验收失败');
  }
}

async function handleReject() {
  if (!reviewingTask.value) return;
  if (!reviewComment.value.trim()) {
    message.error('请输入退回意见');
    return;
  }
  try {
    await rejectTask(reviewingTask.value.id, reviewComment.value);
    message.success('任务已退回');
    reviewModalVisible.value = false;
    reviewComment.value = '';
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '退回失败');
  }
}

// ==================== 辅助方法 ====================

function getPriorityColor(priority: string) {
  const colorMap: Record<string, string> = {
    LOW: 'default',
    MEDIUM: 'blue',
    HIGH: 'orange',
    URGENT: 'red',
  };
  return colorMap[priority] || 'default';
}

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    TODO: 'orange',
    IN_PROGRESS: 'blue',
    PENDING_REVIEW: 'purple',
    COMPLETED: 'green',
    CANCELLED: 'gray',
  };
  return colorMap[status] || 'default';
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadOptions();
});
</script>

<template>
  <Page title="任务管理" description="管理项目任务" auto-content-height>
    <Card>
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="all" tab="全部任务" />
        <Tabs.TabPane key="pending" tab="待处理" />
        <Tabs.TabPane key="in_progress" tab="进行中" />
        <Tabs.TabPane key="pending_review" tab="待验收" />
        <Tabs.TabPane key="completed" tab="已完成" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.matterId"
              placeholder="所属项目"
              allowClear
              showSearch
              :filterOption="(input: string, option: any) => (option?.label || '').toLowerCase().includes(input.toLowerCase())"
              style="width: 100%"
              :options="matterOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="任务状态"
              allowClear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.priority"
              placeholder="优先级"
              allowClear
              style="width: 100%"
              :options="priorityOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <!-- 工具栏按钮 -->
        <template #toolbar-buttons>
          <Button type="primary" @click="handleAdd">新增任务</Button>
        </template>

        <!-- 优先级列 -->
        <template #priority="{ row }">
          <Tag :color="getPriorityColor(row.priority)">
            {{ row.priorityName }}
          </Tag>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">
            {{ row.statusName }}
          </Tag>
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <Space>
            <a @click="handleEdit(row)">编辑</a>
            <template v-if="row.status === 'PENDING_REVIEW' && row.createdBy === userStore.userInfo?.userId">
              <a @click="handleReview(row)" style="color: #1890ff">验收</a>
            </template>
            <template v-else-if="row.status !== 'COMPLETED' && row.status !== 'PENDING_REVIEW'">
              <a @click="handleComplete(row)">完成</a>
            </template>
            <Popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <a style="color: #ff4d4f">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="700px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="所属项目" name="matterId" :rules="[{ required: true, message: '请选择所属项目' }]">
          <Select
            v-model:value="formData.matterId"
            placeholder="请选择所属项目"
            showSearch
            :filterOption="(input: string, option: any) => (option?.label || '').toLowerCase().includes(input.toLowerCase())"
            :options="matterOptions"
          />
        </FormItem>
        <FormItem label="任务标题" name="title" :rules="[{ required: true, message: '请输入任务标题' }]">
          <Input v-model:value="formData.title" placeholder="请输入任务标题" />
        </FormItem>
        <FormItem label="负责人" name="assigneeId">
          <UserTreeSelect
            v-model:value="formData.assigneeId"
            placeholder="选择负责人（按部门筛选）"
          />
        </FormItem>
        <FormItem label="优先级" name="priority">
          <Select v-model:value="formData.priority" :options="priorityOptions" />
        </FormItem>
        <FormItem label="截止日期" name="dueDate">
          <DatePicker
            v-model:value="formData.dueDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="任务描述" name="description">
          <Textarea v-model:value="formData.description" :rows="4" placeholder="请输入任务描述" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 验收弹窗 -->
    <Modal
      v-model:open="reviewModalVisible"
      title="任务验收"
      width="600px"
      :footer="null"
    >
      <div v-if="reviewingTask">
        <p><strong>任务标题：</strong>{{ reviewingTask.title }}</p>
        <p v-if="reviewingTask.description" style="margin-top: 8px">
          <strong>任务描述：</strong>{{ reviewingTask.description }}
        </p>
        <p v-if="reviewingTask.reviewComment" style="margin-top: 8px; color: #ff4d4f">
          <strong>退回意见：</strong>{{ reviewingTask.reviewComment }}
        </p>
        <FormItem label="退回意见" style="margin-top: 16px">
          <Textarea
            v-model:value="reviewComment"
            :rows="4"
            placeholder="如果退回，请填写退回意见"
          />
        </FormItem>
        <div style="text-align: right; margin-top: 16px">
          <Space>
            <Button @click="reviewModalVisible = false">取消</Button>
            <Button type="primary" danger @click="handleReject" :disabled="!reviewComment.trim()">
              退回
            </Button>
            <Button type="primary" @click="handleApprove">通过</Button>
          </Space>
        </div>
      </div>
    </Modal>
  </Page>
</template>
