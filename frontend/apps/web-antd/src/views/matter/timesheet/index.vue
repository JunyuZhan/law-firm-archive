<script setup lang="ts">
import type {
  CreateTimesheetCommand,
  MatterDTO,
  TimesheetDTO,
} from '#/api/matter/types';
import type { UserDTO } from '#/api/system/types';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

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
  Popconfirm,
  Row,
  Select,
  Space,
  Switch,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createTimesheet,
  deleteTimesheet,
  getMatterList,
  getTimesheetList,
  reviewTimesheet,
  submitTimesheet,
  updateTimesheet,
} from '#/api/matter';
import { getUserSelectOptions } from '#/api/system';
import { useResponsive } from '#/hooks/useResponsive';

defineOptions({ name: 'MatterTimesheet' });

// 响应式布局
const { isMobile } = useResponsive();

const userStore = useUserStore();

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const modalTitle = ref('新增工时');
const formRef = ref();
const matters = ref<MatterDTO[]>([]);
const users = ref<UserDTO[]>([]);
const activeTab = ref('all');

// 表单数据
const formData = reactive<Partial<CreateTimesheetCommand> & { id?: number }>({
  id: undefined,
  matterId: undefined,
  workDate: undefined,
  hours: 1,
  workType: 'RESEARCH',
  workContent: '',
  billable: true,
});

// 查询参数
const queryParams = reactive({
  matterId: undefined as number | undefined,
  userId: undefined as number | undefined,
  workType: undefined as string | undefined,
  status: undefined as string | undefined,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
});

// ==================== 常量选项 ====================

const workTypeOptions = [
  { label: '法律研究', value: 'RESEARCH' },
  { label: '文书起草', value: 'DRAFTING' },
  { label: '客户沟通', value: 'COMMUNICATION' },
  { label: '出庭', value: 'COURT' },
  { label: '会议', value: 'MEETING' },
  { label: '其他', value: 'OTHER' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已审核', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
];

// ==================== 表格配置 ====================

// 表格列 - 根据标签页和屏幕尺寸动态显示
function getGridColumns() {
  const baseColumns: any[] = [
    {
      title: '项目',
      field: 'matterName',
      width: isMobile.value ? 120 : 180,
      showOverflow: true,
      mobileShow: true,
    },
  ];

  if (activeTab.value === 'all' && !isMobile.value) {
    baseColumns.push({ title: '律师', field: 'userName', width: 100 });
  }

  const allColumns = [
    ...baseColumns,
    { title: '工作日期', field: 'workDate', width: 110, mobileShow: true },
    { title: '工时', field: 'hours', width: 80, mobileShow: true },
    {
      title: '工作类型',
      field: 'workType',
      width: 100,
      slots: { default: 'workType' },
    },
    { title: '工作描述', field: 'workContent', width: 180, showOverflow: true },
    {
      title: '可计费',
      field: 'billable',
      width: 70,
      slots: { default: 'billable' },
    },
    {
      title: '状态',
      field: 'status',
      width: 90,
      slots: { default: 'status' },
      mobileShow: true,
    },
    { title: '创建时间', field: 'createdAt', width: 150 },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 100 : 180,
      fixed: 'right' as const,
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  if (isMobile.value) {
    return allColumns.filter((col) => col.mobileShow === true);
  }
  return allColumns;
}

// 加载数据
async function loadData(params: { page: number; pageSize: number }) {
  let res;
  if (activeTab.value === 'my') {
    const currentUserId = userStore.userInfo?.userId;
    res = await getTimesheetList({
      pageNum: params.page,
      pageSize: params.pageSize,
      matterId: queryParams.matterId,
      userId: currentUserId ? Number(currentUserId) : undefined,
      workType: queryParams.workType,
      status: queryParams.status,
      startDate: queryParams.startDate,
      endDate: queryParams.endDate,
    });
  } else {
    res = await getTimesheetList({
      pageNum: params.page,
      pageSize: params.pageSize,
      matterId: queryParams.matterId,
      userId: queryParams.userId,
      workType: queryParams.workType,
      status: queryParams.status,
      startDate: queryParams.startDate,
      endDate: queryParams.endDate,
    });
  }
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: getGridColumns(),
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

// 监听响应式变化，更新列配置
watch([isMobile, activeTab], () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

// ==================== 数据加载 ====================

async function loadMatters() {
  try {
    const res = await getMatterList({ pageNum: 1, pageSize: 1000 });
    matters.value = res.list;
  } catch (error) {
    console.error('加载项目失败:', error);
  }
}

async function loadUsers() {
  try {
    // 使用公共接口，无需特殊权限
    const res = await getUserSelectOptions({ pageNum: 1, pageSize: 500 });
    users.value = res.list;
  } catch (error) {
    console.error('加载用户列表失败:', error);
  }
}

// 计算属性
const matterOptions = computed(() =>
  matters.value.map((m) => ({
    label: `[${m.matterNo}] ${m.name}`,
    value: m.id,
  })),
);

const userOptions = computed(() =>
  users.value.map((u) => ({
    label: u.realName || u.username,
    value: u.id,
  })),
);

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.matterId = undefined;
  queryParams.userId = undefined;
  queryParams.workType = undefined;
  queryParams.status = undefined;
  queryParams.startDate = undefined;
  queryParams.endDate = undefined;
  gridApi.reload();
}

function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  queryParams.userId = undefined;
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  modalTitle.value = '新增工时';
  Object.assign(formData, {
    id: undefined,
    matterId: undefined,
    workDate: undefined,
    hours: 1,
    workType: 'RESEARCH',
    workContent: '',
    billable: true,
  });
  modalVisible.value = true;
}

function handleEdit(row: TimesheetDTO) {
  modalTitle.value = '编辑工时';
  Object.assign(formData, {
    id: row.id,
    matterId: row.matterId,
    workDate: row.workDate,
    hours: row.hours,
    workType: row.workType,
    workContent: row.workContent,
    billable: row.billable,
  });
  modalVisible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();

    if (formData.id) {
      await updateTimesheet(
        formData.id,
        formData as Partial<CreateTimesheetCommand>,
      );
      message.success('更新成功');
    } else {
      await createTimesheet(formData as CreateTimesheetCommand);
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

async function handleDelete(row: TimesheetDTO) {
  try {
    await deleteTimesheet(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '删除失败');
  }
}

function handleSubmit(row: TimesheetDTO) {
  Modal.confirm({
    title: '确认提交',
    content: '确定要提交这条工时记录进行审核吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await submitTimesheet(row.id);
        message.success('提交成功');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '提交失败');
      }
    },
  });
}

function handleReview(row: TimesheetDTO, approved: boolean) {
  Modal.confirm({
    title: approved ? '确认通过' : '确认拒绝',
    content: approved
      ? '确定要通过这条工时记录吗？'
      : '确定要拒绝这条工时记录吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      if (approved) {
        try {
          await reviewTimesheet(row.id, { approved, comment: undefined });
          message.success('审核通过');
          gridApi.reload();
        } catch (error: unknown) {
          const err = error as { message?: string };
          message.error(err.message || '操作失败');
        }
      }
    },
  });
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
  };
  return colorMap[status] || 'default';
}

function getWorkTypeName(workType: string) {
  return workTypeOptions.find((o) => o.value === workType)?.label || workType;
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadMatters();
  loadUsers();
});
</script>

<template>
  <Page title="工时管理" description="管理项目工时记录">
    <Card>
      <!-- 标签页切换 -->
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="all" tab="全部工时" />
        <Tabs.TabPane key="my" tab="我的工时" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="5">
            <Select
              v-model:value="queryParams.matterId"
              placeholder="所属项目"
              allow-clear
              show-search
              :virtual="matterOptions.length > 50"
              :list-height="256"
              :filter-option="
                (input: string, option: any) =>
                  (option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              style="width: 100%"
              :options="matterOptions"
            />
          </Col>
          <Col v-if="activeTab === 'all'" :xs="24" :sm="12" :md="4">
            <Select
              v-model:value="queryParams.userId"
              placeholder="筛选律师"
              allow-clear
              show-search
              :virtual="userOptions.length > 50"
              :list-height="256"
              :filter-option="
                (input: string, option: any) =>
                  (option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              style="width: 100%"
              :options="userOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="4">
            <Select
              v-model:value="queryParams.workType"
              placeholder="工作类型"
              allow-clear
              style="width: 100%"
              :options="workTypeOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="4">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="activeTab === 'all' ? 7 : 11">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
        <Row :gutter="16" style="margin-top: 16px">
          <Col :xs="24" :sm="12" :md="5">
            <DatePicker
              v-model:value="queryParams.startDate"
              placeholder="开始日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="5">
            <DatePicker
              v-model:value="queryParams.endDate"
              placeholder="结束日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </Col>
        </Row>
      </div>

      <Grid>
        <!-- 工具栏按钮 -->
        <template #toolbar-buttons>
          <Button type="primary" @click="handleAdd">新增工时</Button>
        </template>

        <!-- 工作类型列 -->
        <template #workType="{ row }">
          {{ getWorkTypeName(row.workType) }}
        </template>

        <!-- 可计费列 -->
        <template #billable="{ row }">
          <Tag :color="row.billable ? 'green' : 'default'">
            {{ row.billable ? '是' : '否' }}
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
            <template v-if="row.status === 'DRAFT'">
              <a @click="handleSubmit(row)">提交</a>
            </template>
            <template v-if="row.status === 'PENDING'">
              <a @click="handleReview(row, true)">通过</a>
              <a @click="handleReview(row, false)" style="color: #ff4d4f"
                >拒绝</a
              >
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
      :width="isMobile ? '100%' : '700px'"
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
          label="所属项目"
          name="matterId"
          :rules="[{ required: true, message: '请选择所属项目' }]"
        >
          <Select
            v-model:value="formData.matterId"
            placeholder="请选择所属项目"
            show-search
            :virtual="matterOptions.length > 50"
            :list-height="256"
            :filter-option="
              (input: string, option: any) =>
                (option?.label || '')
                  .toLowerCase()
                  .includes(input.toLowerCase())
            "
            :options="matterOptions"
          />
        </FormItem>
        <FormItem
          label="工作日期"
          name="workDate"
          :rules="[{ required: true, message: '请选择工作日期' }]"
        >
          <DatePicker
            v-model:value="formData.workDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem
          label="工时(小时)"
          name="hours"
          :rules="[{ required: true, message: '请输入工时' }]"
        >
          <InputNumber
            v-model:value="formData.hours"
            :min="0.1"
            :max="24"
            :precision="1"
            style="width: 100%"
            placeholder="请输入工时"
          />
        </FormItem>
        <FormItem
          label="工作类型"
          name="workType"
          :rules="[{ required: true, message: '请选择工作类型' }]"
        >
          <Select
            v-model:value="formData.workType"
            :options="workTypeOptions"
          />
        </FormItem>
        <FormItem label="可计费" name="billable">
          <Switch v-model:checked="formData.billable" />
        </FormItem>
        <FormItem
          label="工作描述"
          name="workContent"
          :rules="[{ required: false, message: '请输入工作描述' }]"
        >
          <Textarea
            v-model:value="formData.workContent"
            :rows="4"
            placeholder="请输入工作描述"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
