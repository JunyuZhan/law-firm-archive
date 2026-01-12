<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  AnnouncementDTO,
  CreateAnnouncementCommand,
} from '#/api/system/announcement';

import { ref, reactive } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  DatePicker,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  ANNOUNCEMENT_PRIORITY_OPTIONS,
  ANNOUNCEMENT_STATUS_OPTIONS,
  ANNOUNCEMENT_TYPE_OPTIONS,
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncementById,
  getAnnouncementList,
  publishAnnouncement,
  updateAnnouncement,
  withdrawAnnouncement,
} from '#/api/system/announcement';

defineOptions({ name: 'AnnouncementManagement' });

// ==================== 搜索表单配置 ====================
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'type',
    label: '类型',
    component: 'Select',
    componentProps: {
      placeholder: '请选择类型',
      allowClear: true,
      options: ANNOUNCEMENT_TYPE_OPTIONS,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: ANNOUNCEMENT_STATUS_OPTIONS,
    },
  },
];

// ==================== 表格配置 ====================
const gridColumns: any[] = [
  { title: '标题', field: 'title', minWidth: 200, showOverflow: true },
  { title: '类型', field: 'type', width: 100, slots: { default: 'type' } },
  { title: '优先级', field: 'priority', width: 80, slots: { default: 'priority' } },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '发布人', field: 'publisherName', width: 100 },
  { title: '发布时间', field: 'publishedAt', width: 160, slots: { default: 'publishedAt' } },
  { title: '过期时间', field: 'expiredAt', width: 160, slots: { default: 'expiredAt' } },
  { title: '浏览量', field: 'viewCount', width: 80, align: 'center' },
  { title: '操作', field: 'action', width: 200, fixed: 'right', slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: Record<string, any> & { page: number; pageSize: number }) {
  const res = await getAnnouncementList({
    pageNum: params.page,
    pageSize: params.pageSize,
    type: params.type,
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

// ==================== 编辑弹窗 ====================
const editModalVisible = ref(false);
const editForm = reactive<CreateAnnouncementCommand & { id?: number }>({
  title: '',
  content: '',
  type: 'NOTICE',
  priority: 0,
  expiredAt: undefined,
});
const editLoading = ref(false);
const isEdit = ref(false);

function handleCreate() {
  isEdit.value = false;
  Object.assign(editForm, {
    id: undefined,
    title: '',
    content: '',
    type: 'NOTICE',
    priority: 0,
    expiredAt: undefined,
  });
  editModalVisible.value = true;
}

async function handleEdit(row: AnnouncementDTO) {
  isEdit.value = true;
  try {
    const detail = await getAnnouncementById(row.id);
    Object.assign(editForm, {
      id: detail.id,
      title: detail.title,
      content: detail.content,
      type: detail.type,
      priority: detail.priority || 0,
      expiredAt: detail.expiredAt,
    });
    editModalVisible.value = true;
  } catch (error: any) {
    message.error(`加载公告详情失败：${error.message || '未知错误'}`);
  }
}

async function handleEditSubmit() {
  if (!editForm.title?.trim()) {
    message.warning('请输入公告标题');
    return;
  }
  if (!editForm.content?.trim()) {
    message.warning('请输入公告内容');
    return;
  }

  editLoading.value = true;
  try {
    const data: CreateAnnouncementCommand = {
      title: editForm.title,
      content: editForm.content,
      type: editForm.type,
      priority: editForm.priority,
      expiredAt: editForm.expiredAt,
    };

    if (isEdit.value && editForm.id) {
      await updateAnnouncement(editForm.id, data);
      message.success('更新成功');
    } else {
      await createAnnouncement(data);
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

// ==================== 详情弹窗 ====================
const detailModalVisible = ref(false);
const detailData = ref<AnnouncementDTO | null>(null);
const detailLoading = ref(false);

async function handleViewDetail(row: AnnouncementDTO) {
  detailLoading.value = true;
  detailModalVisible.value = true;
  try {
    const res = await getAnnouncementById(row.id);
    detailData.value = res;
  } catch (error: any) {
    message.error(`加载详情失败：${error.message || '未知错误'}`);
  } finally {
    detailLoading.value = false;
  }
}

// ==================== 操作方法 ====================
async function handlePublish(row: AnnouncementDTO) {
  try {
    await publishAnnouncement(row.id);
    message.success('发布成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`发布失败：${error.message || '未知错误'}`);
  }
}

async function handleWithdraw(row: AnnouncementDTO) {
  try {
    await withdrawAnnouncement(row.id);
    message.success('撤回成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`撤回失败：${error.message || '未知错误'}`);
  }
}

async function handleDelete(row: AnnouncementDTO) {
  try {
    await deleteAnnouncement(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`删除失败：${error.message || '未知错误'}`);
  }
}

// ==================== 工具方法 ====================
function getTypeColor(type: string) {
  const option = ANNOUNCEMENT_TYPE_OPTIONS.find((o) => o.value === type);
  return option?.color || 'default';
}

function getTypeName(type: string) {
  const option = ANNOUNCEMENT_TYPE_OPTIONS.find((o) => o.value === type);
  return option?.label || type;
}

function getStatusColor(status: string) {
  const option = ANNOUNCEMENT_STATUS_OPTIONS.find((o) => o.value === status);
  return option?.color || 'default';
}

function getStatusName(status: string) {
  const option = ANNOUNCEMENT_STATUS_OPTIONS.find((o) => o.value === status);
  return option?.label || status;
}

function getPriorityName(priority: number | undefined) {
  if (priority === undefined || priority === null) return '普通';
  const option = ANNOUNCEMENT_PRIORITY_OPTIONS.find((o) => o.value === priority);
  return option?.label || '普通';
}

function getPriorityColor(priority: number | undefined) {
  if (priority === 2) return 'red';
  if (priority === 1) return 'orange';
  return 'default';
}

function formatDateTime(date: string | null | undefined) {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
}
</script>

<template>
  <Page title="公告管理" description="发布和管理系统公告通知">
    <Grid>
      <!-- 工具栏 -->
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">新建公告</Button>
      </template>

      <!-- 类型列 -->
      <template #type="{ row }">
        <Tag :color="getTypeColor(row.type)">
          {{ row.typeName || getTypeName(row.type) }}
        </Tag>
      </template>

      <!-- 优先级列 -->
      <template #priority="{ row }">
        <Tag :color="getPriorityColor(row.priority)">
          {{ row.priorityName || getPriorityName(row.priority) }}
        </Tag>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName || getStatusName(row.status) }}
        </Tag>
      </template>

      <!-- 发布时间列 -->
      <template #publishedAt="{ row }">
        {{ formatDateTime(row.publishedAt) }}
      </template>

      <!-- 过期时间列 -->
      <template #expiredAt="{ row }">
        {{ formatDateTime(row.expiredAt) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleViewDetail(row)">查看</a>
          <a v-if="row.status === 'DRAFT'" @click="handleEdit(row)">编辑</a>
          <a v-if="row.status === 'DRAFT'" @click="handlePublish(row)">发布</a>
          <a
            v-if="row.status === 'PUBLISHED'"
            style="color: #ff4d4f"
            @click="handleWithdraw(row)"
          >
            撤回
          </a>
          <Popconfirm
            v-if="row.status !== 'PUBLISHED'"
            title="确定删除此公告？"
            @confirm="handleDelete(row)"
          >
            <a style="color: #ff4d4f">删除</a>
          </Popconfirm>
        </Space>
      </template>
    </Grid>

    <!-- 编辑弹窗 -->
    <Modal
      v-model:open="editModalVisible"
      :title="isEdit ? '编辑公告' : '新建公告'"
      :confirm-loading="editLoading"
      width="700px"
      @ok="handleEditSubmit"
    >
      <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <Form.Item label="标题" required>
          <Input
            v-model:value="editForm.title"
            placeholder="请输入公告标题"
            :maxlength="100"
            show-count
          />
        </Form.Item>
        <Form.Item label="类型" required>
          <Select
            v-model:value="editForm.type"
            :options="ANNOUNCEMENT_TYPE_OPTIONS"
          />
        </Form.Item>
        <Form.Item label="优先级">
          <Select
            v-model:value="editForm.priority"
            :options="ANNOUNCEMENT_PRIORITY_OPTIONS"
          />
        </Form.Item>
        <Form.Item label="过期时间">
          <DatePicker
            v-model:value="editForm.expiredAt"
            value-format="YYYY-MM-DD HH:mm:ss"
            show-time
            placeholder="选择过期时间（可选）"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="内容" required>
          <Input.TextArea
            v-model:value="editForm.content"
            :rows="8"
            placeholder="请输入公告内容"
            :maxlength="5000"
            show-count
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="公告详情"
      :footer="null"
      width="700px"
    >
      <Spin :spinning="detailLoading">
        <template v-if="detailData">
          <div style="margin-bottom: 16px">
            <Space>
              <Tag :color="getTypeColor(detailData.type)">
                {{ detailData.typeName || getTypeName(detailData.type) }}
              </Tag>
              <Tag :color="getPriorityColor(detailData.priority)">
                {{ detailData.priorityName || getPriorityName(detailData.priority) }}
              </Tag>
              <Tag :color="getStatusColor(detailData.status)">
                {{ detailData.statusName || getStatusName(detailData.status) }}
              </Tag>
            </Space>
          </div>

          <h2 style="margin-bottom: 16px">{{ detailData.title }}</h2>

          <div style="color: #666; margin-bottom: 16px; font-size: 13px">
            <Space split="|">
              <span>发布人：{{ detailData.publisherName || '-' }}</span>
              <span>发布时间：{{ formatDateTime(detailData.publishedAt) }}</span>
              <span>浏览量：{{ detailData.viewCount || 0 }}</span>
            </Space>
          </div>

          <div
            style="
              background: #fafafa;
              padding: 16px;
              border-radius: 4px;
              white-space: pre-wrap;
              line-height: 1.8;
            "
          >
            {{ detailData.content }}
          </div>

          <div
            v-if="detailData.expiredAt"
            style="margin-top: 16px; color: #999; font-size: 12px"
          >
            过期时间：{{ formatDateTime(detailData.expiredAt) }}
          </div>
        </template>
      </Spin>
    </Modal>
  </Page>
</template>
