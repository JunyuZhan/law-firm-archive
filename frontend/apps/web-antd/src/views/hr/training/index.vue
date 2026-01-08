<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Button,
  Space,
  Tag,
  Input,
  Select,
  Form,
  FormItem,
  Row,
  Col,
  Tabs,
  InputNumber,
  DatePicker,
  Textarea,
} from 'ant-design-vue';
import { Plus } from '@vben/icons';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getTrainingList,
  createTraining,
  publishTraining,
  cancelTraining,
  enrollTraining,
  cancelEnrollment,
  getMyTotalCredits,
} from '#/api/hr/training';
import type { TrainingDTO, TrainingQuery, CreateTrainingCommand } from '#/api/hr/training';
import { UserTreeSelect } from '#/components/UserTreeSelect';

defineOptions({ name: 'HrTraining' });

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const formRef = ref();
const activeTab = ref('upcoming');
const myCredits = ref(0);

const queryParams = ref<TrainingQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  status: undefined,
});

const formData = reactive<CreateTrainingCommand & { id?: number }>({
  id: undefined,
  name: '',
  trainingType: '',
  trainerId: undefined,
  trainingTime: '',
  location: '',
  description: '',
  credits: undefined,
  maxParticipants: undefined,
});

// ==================== 常量选项 ====================

const trainingTypeOptions = [
  { label: '内部培训', value: 'INTERNAL' },
  { label: '外部培训', value: 'EXTERNAL' },
  { label: '在线培训', value: 'ONLINE' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '进行中', value: 'ONGOING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '培训名称', field: 'name', minWidth: 200 },
  { title: '培训类型', field: 'trainingTypeName', width: 120 },
  { title: '讲师', field: 'trainer', width: 100 },
  { title: '培训时间', field: 'trainingTime', width: 160 },
  { title: '培训地点', field: 'location', width: 150 },
  { title: '学分', field: 'credits', width: 80 },
  { title: '参与人数', field: 'currentParticipants', width: 100 },
  { title: '状态', field: 'statusName', width: 100, slots: { default: 'status' } },
  { title: '操作', field: 'action', width: 180, fixed: 'right', slots: { default: 'action' } },
];

async function loadData({ page }: { page: { currentPage: number; pageSize: number } }) {
  // 根据tab设置状态筛选
  let status = queryParams.value.status;
  if (activeTab.value === 'upcoming') {
    status = 'PUBLISHED';
  } else if (activeTab.value === 'ongoing') {
    status = 'ONGOING';
  } else if (activeTab.value === 'completed') {
    status = 'COMPLETED';
  }
  
  const params = { ...queryParams.value, status, pageNum: page.currentPage, pageSize: page.pageSize };
  const res = await getTrainingList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

// ==================== 数据加载 ====================

async function loadMyCredits() {
  try {
    const credits = await getMyTotalCredits();
    myCredits.value = credits;
  } catch (error: any) {
    console.error('加载学分失败:', error);
  }
}

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = { pageNum: 1, pageSize: 10, keyword: undefined, status: undefined };
  gridApi.reload();
}

function handleTabChange(key: string | number) {
  activeTab.value = String(key);
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  Object.assign(formData, {
    id: undefined,
    name: '',
    trainingType: '',
    trainerId: undefined,
    trainingTime: '',
    location: '',
    description: '',
    credits: undefined,
    maxParticipants: undefined,
  });
  modalVisible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();
    await createTraining(formData);
    message.success('创建成功');
    modalVisible.value = false;
    gridApi.reload();
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '操作失败');
  }
}

async function handlePublish(row: TrainingDTO) {
  try {
    await publishTraining(row.id);
    message.success('发布成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '发布失败');
  }
}

function handleCancel(row: TrainingDTO) {
  Modal.confirm({
    title: '确认取消',
    content: `确定要取消培训 "${row.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await cancelTraining(row.id);
        message.success('取消成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '取消失败');
      }
    },
  });
}

async function handleEnroll(row: TrainingDTO) {
  try {
    await enrollTraining(row.id);
    message.success('报名成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '报名失败');
  }
}

function handleCancelEnrollment(row: TrainingDTO) {
  Modal.confirm({
    title: '确认取消报名',
    content: `确定要取消报名培训 "${row.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await cancelEnrollment(row.id);
        message.success('取消报名成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '取消报名失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PUBLISHED: 'blue',
    ONGOING: 'green',
    COMPLETED: 'success',
    CANCELLED: 'red',
  };
  return colorMap[status] || 'default';
}

onMounted(() => {
  loadMyCredits();
});
</script>

<template>
  <Page title="培训管理" description="管理员工培训计划">
    <Card>
      <!-- 我的学分 -->
      <div style="margin-bottom: 16px; padding: 12px; background: #f0f2f5; border-radius: 4px;">
        <Space>
          <span style="font-weight: bold;">我的总学分：</span>
          <span style="font-size: 20px; color: #1890ff;">{{ myCredits }}</span>
        </Space>
      </div>

      <!-- Tab切换 -->
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="upcoming" tab="即将开始" />
        <Tabs.TabPane key="ongoing" tab="进行中" />
        <Tabs.TabPane key="completed" tab="已完成" />
        <Tabs.TabPane key="all" tab="全部" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="6">
            <Input
              v-model:value="queryParams.keyword"
              placeholder="搜索培训名称"
              allowClear
              @pressEnter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allowClear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="12">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />新建培训
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <a v-if="row.status === 'DRAFT'" @click="handlePublish(row)">发布</a>
            <template v-if="row.status === 'PUBLISHED'">
              <a @click="handleEnroll(row)">报名</a>
              <a @click="handleCancel(row)">取消</a>
            </template>
            <a v-if="row.status === 'ONGOING'" @click="handleCancelEnrollment(row)">取消报名</a>
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 新增弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建培训"
      width="700px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="培训名称" name="name" :rules="[{ required: true, message: '请输入培训名称' }]">
          <Input v-model:value="formData.name" placeholder="请输入培训名称" />
        </FormItem>
        <FormItem label="培训类型" name="trainingType">
          <Select v-model:value="formData.trainingType" :options="trainingTypeOptions" placeholder="请选择培训类型" />
        </FormItem>
        <FormItem label="讲师" name="trainerId">
          <UserTreeSelect
            v-model:value="formData.trainerId"
            placeholder="选择讲师（按部门筛选）"
          />
        </FormItem>
        <FormItem label="培训时间" name="trainingTime">
          <DatePicker
            v-model:value="formData.trainingTime"
            placeholder="请选择培训时间"
            style="width: 100%"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </FormItem>
        <FormItem label="培训地点" name="location">
          <Input v-model:value="formData.location" placeholder="请输入培训地点" />
        </FormItem>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="学分" name="credits">
              <InputNumber v-model:value="formData.credits" :min="0" style="width: 100%" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="最大参与人数" name="maxParticipants">
              <InputNumber v-model:value="formData.maxParticipants" :min="1" style="width: 100%" />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="培训描述" name="description">
          <Textarea v-model:value="formData.description" :rows="4" placeholder="请输入培训描述" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
