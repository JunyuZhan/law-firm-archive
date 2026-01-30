<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import {
  CheckCircleOutlined,
  DownloadOutlined,
  EyeOutlined,
  FileOutlined,
  Plus,
  UploadOutlined,
} from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Card,
  Form,
  FormItem,
  Input,
  List,
  message,
  Modal,
  Space,
  Tabs,
  Tag,
  Upload,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { uploadFile } from '#/api/document';
import { requestClient } from '#/api/request';

defineOptions({ name: 'HrTraining' });

const userStore = useUserStore();
const isAdmin = computed(() => {
  const roles = userStore.userInfo?.roles ?? [];
  // 管理员(ADMIN)、律所主任(DIRECTOR)、行政(ADMIN_STAFF) 可以发布和管理培训通知
  return roles.some((role) =>
    ['ADMIN', 'ADMIN_STAFF', 'DIRECTOR'].includes(String(role).toUpperCase()),
  );
});

// ==================== 类型定义 ====================

interface TrainingNoticeDTO {
  id: number;
  title: string;
  content?: string;
  attachments?: { fileName: string; fileUrl: string }[];
  status: string;
  statusName?: string;
  publishedAt?: string;
  createdAt?: string;
  // 完成情况
  completedCount?: number;
  totalCount?: number;
  myCompleted?: boolean;
  myCertificateUrl?: string;
}

// ==================== 状态定义 ====================

const activeTab = ref('notices');
const modalVisible = ref(false);
const detailVisible = ref(false);
const uploadVisible = ref(false);
const currentRecord = ref<null | TrainingNoticeDTO>(null);
const uploading = ref(false);

const formData = reactive({
  title: '',
  content: '',
  attachments: [] as { fileName: string; fileUrl: string }[],
});

// 待上传的附件文件（先缓存，发布时才上传）
const pendingFiles = ref<File[]>([]);
const publishing = ref(false);

const uploadForm = reactive({
  certificateUrl: '',
  certificateName: '',
});

// 待上传的合格证文件（先缓存，点击确定时才上传）
const pendingCertificateFile = ref<File | null>(null);

// ==================== 表格配置 - 培训通知列表 ====================

const noticeColumns = [
  { title: '培训通知标题', field: 'title', minWidth: 250 },
  { title: '发布时间', field: 'publishedAt', width: 160 },
  {
    title: '完成情况',
    field: 'progress',
    width: 120,
    slots: { default: 'progress' },
  },
  {
    title: '我的状态',
    field: 'myStatus',
    width: 100,
    slots: { default: 'myStatus' },
  },
  {
    title: '操作',
    field: 'action',
    width: 150,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

async function loadNotices({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const res = await requestClient.get<any>('/hr/training-notice', {
    params: { pageNum: page.currentPage, pageSize: page.pageSize },
  });
  return { items: res.list || [], total: res.total || 0 };
}

const [NoticeGrid, noticeGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: noticeColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadNotices } },
  },
});

// ==================== 表格配置 - 完成情况列表（管理员） ====================

const completionColumns = [
  { title: '培训通知', field: 'noticeTitle', minWidth: 200 },
  { title: '律师姓名', field: 'employeeName', width: 100 },
  { title: '部门', field: 'departmentName', width: 120 },
  { title: '上传时间', field: 'uploadedAt', width: 160 },
  {
    title: '合格证',
    field: 'certificate',
    width: 100,
    slots: { default: 'certificate' },
  },
];

async function loadCompletions({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const res = await requestClient.get<any>('/hr/training-notice/completions', {
    params: { pageNum: page.currentPage, pageSize: page.pageSize },
  });
  return { items: res.list || [], total: res.total || 0 };
}

const [CompletionGrid, completionGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: completionColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadCompletions } },
  },
});

// ==================== Tab切换 ====================

function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  // 延迟执行，确保表格已挂载
  nextTick(() => {
    if (key === 'notices' && noticeGridApi.grid?.commitProxy) {
      noticeGridApi.grid.commitProxy('query');
    } else if (key === 'completions' && completionGridApi.grid?.commitProxy) {
      completionGridApi.grid.commitProxy('query');
    }
  });
}

// ==================== 发布培训通知 ====================

function handleAdd() {
  Object.assign(formData, {
    title: '',
    content: '',
    attachments: [],
  });
  pendingFiles.value = [];
  modalVisible.value = true;
}

function handleAttachmentUpload(info: any) {
  const file = info.file.originFileObj || info.file;
  // 只缓存文件，不立即上传
  pendingFiles.value.push(file);
  // 添加到显示列表（暂时用本地预览）
  formData.attachments.push({
    fileName: file.name,
    fileUrl: '', // 发布时才有真正的URL
  });
}

function removeAttachment(index: number) {
  formData.attachments.splice(index, 1);
  pendingFiles.value.splice(index, 1);
}

async function handlePublish() {
  if (!formData.title.trim()) {
    message.error('请输入通知标题');
    return;
  }

  try {
    publishing.value = true;

    // 先上传所有待上传的附件
    const uploadedAttachments: { fileName: string; fileUrl: string }[] = [];
    for (const file of pendingFiles.value) {
      try {
        const res = await uploadFile(file);
        uploadedAttachments.push({
          fileName: file.name,
          fileUrl: res.filePath || '',
        });
      } catch {
        message.error(`附件"${file.name}"上传失败`);
        publishing.value = false;
        return;
      }
    }

    // 发布通知
    await requestClient.post('/hr/training-notice', {
      title: formData.title,
      content: formData.content,
      attachments: uploadedAttachments,
    });

    message.success('发布成功');
    modalVisible.value = false;
    pendingFiles.value = [];
    if (noticeGridApi.grid?.commitProxy) {
      noticeGridApi.grid.commitProxy('query');
    }
  } catch (error: any) {
    message.error(error.message || '发布失败');
  } finally {
    publishing.value = false;
  }
}

// ==================== 查看详情 ====================

async function handleView(row: TrainingNoticeDTO) {
  try {
    const detail = await requestClient.get<TrainingNoticeDTO>(
      `/hr/training-notice/${row.id}`,
    );
    currentRecord.value = detail;
    detailVisible.value = true;
  } catch {
    message.error('加载详情失败');
  }
}

// ==================== 上传合格证 ====================

function handleOpenUpload(row: TrainingNoticeDTO) {
  currentRecord.value = row;
  Object.assign(uploadForm, {
    certificateUrl: '',
    certificateName: '',
  });
  pendingCertificateFile.value = null;
  uploadVisible.value = true;
}

function handleCertificateUpload(info: any) {
  const file = info.file.originFileObj || info.file;
  // 只缓存文件，不立即上传
  pendingCertificateFile.value = file;
  uploadForm.certificateName = file.name;
  uploadForm.certificateUrl = ''; // 确定时才有真正的URL
}

async function handleSubmitCertificate() {
  if (!pendingCertificateFile.value) {
    message.error('请选择合格证文件');
    return;
  }

  try {
    uploading.value = true;

    // 先上传文件
    const res = await uploadFile(pendingCertificateFile.value);
    const certificateUrl = res.filePath || '';

    // 然后提交完成记录
    await requestClient.post(
      `/hr/training-notice/${currentRecord.value?.id}/complete`,
      {
        certificateUrl,
        certificateName: uploadForm.certificateName,
      },
    );

    message.success('上传成功');
    uploadVisible.value = false;
    pendingCertificateFile.value = null;
    if (noticeGridApi.grid?.commitProxy) {
      noticeGridApi.grid.commitProxy('query');
    }
  } catch (error: any) {
    message.error(error.message || '上传失败');
  } finally {
    uploading.value = false;
  }
}

// ==================== 删除通知 ====================

function handleDelete(row: TrainingNoticeDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除"${row.title}"吗？`,
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await requestClient.delete(`/hr/training-notice/${row.id}`);
        message.success('删除成功');
        if (noticeGridApi.grid?.commitProxy) {
          noticeGridApi.grid.commitProxy('query');
        }
      } catch (error: any) {
        message.error(error.message || '删除失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

async function downloadFile(url?: string, fileName?: string) {
  if (!url) return;

  try {
    // 先获取预签名 URL
    const res = await requestClient.get<{
      downloadUrl: string;
      fileName?: string;
    }>('/hr/training-notice/download-url', {
      params: { fileUrl: url, fileName },
    });

    const downloadUrl = res.downloadUrl || url;
    const downloadFileName =
      res.fileName || fileName || url.split('/').pop() || 'download';

    // 使用预签名 URL 直接下载
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = downloadFileName;
    link.target = '_blank';
    document.body.append(link);
    link.click();
    link.remove();
  } catch (error: any) {
    console.error('下载文件失败:', error);
    message.error(`文件下载失败: ${error.message || '未知错误'}`);
  }
}
</script>

<template>
  <Page title="培训管理" description="发布培训通知，查看律师完成情况">
    <Card>
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="notices" tab="培训通知" />
        <Tabs.TabPane v-if="isAdmin" key="completions" tab="完成情况" />
      </Tabs>

      <!-- 培训通知列表 -->
      <template v-if="activeTab === 'notices'">
        <div v-if="isAdmin" style="margin-bottom: 16px">
          <Button type="primary" @click="handleAdd">
            <Plus class="size-4" />发布培训通知
          </Button>
        </div>

        <NoticeGrid>
          <template #progress="{ row }">
            <span>{{ row.completedCount || 0 }}/{{ row.totalCount || 0 }}</span>
          </template>
          <template #myStatus="{ row }">
            <Tag v-if="row.myCompleted" color="success">
              <CheckCircleOutlined /> 已完成
            </Tag>
            <Tag v-else color="default">未完成</Tag>
          </template>
          <template #action="{ row }">
            <Space>
              <a @click="handleView(row)">查看</a>
              <!-- eslint-disable-next-line prettier/prettier -->
              <a v-if="!row.myCompleted" @click="handleOpenUpload(row)"
                >上传合格证</a
              >
              <!-- eslint-disable-next-line prettier/prettier -->
              <a
                v-if="isAdmin"
                style="color: #ff4d4f"
                @click="handleDelete(row)"
                >删除</a
              >
            </Space>
          </template>
        </NoticeGrid>
      </template>

      <!-- 完成情况列表（管理员） -->
      <template v-if="activeTab === 'completions'">
        <CompletionGrid>
          <template #certificate="{ row }">
            <a
              v-if="row.certificateUrl"
              @click="downloadFile(row.certificateUrl, row.certificateName)"
            >
              <Space size="small">
                <EyeOutlined />
                <span>查看</span>
              </Space>
            </a>
            <span v-else>-</span>
          </template>
        </CompletionGrid>
      </template>
    </Card>

    <!-- 发布培训通知弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="发布培训通知"
      width="700px"
      :confirm-loading="publishing"
      ok-text="发布"
      @ok="handlePublish"
    >
      <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <FormItem label="通知标题" required>
          <Input
            v-model:value="formData.title"
            placeholder="如：2026年度律师继续教育培训通知"
          />
        </FormItem>
        <FormItem label="通知内容">
          <Input.TextArea
            v-model:value="formData.content"
            :rows="8"
            placeholder="培训要求、培训网站、注意事项等..."
          />
        </FormItem>
        <FormItem label="附件">
          <Upload
            :before-upload="() => false"
            :show-upload-list="false"
            @change="handleAttachmentUpload"
          >
            <Button><UploadOutlined />上传附件</Button>
          </Upload>
          <List
            v-if="formData.attachments.length > 0"
            size="small"
            style="margin-top: 8px"
          >
            <List.Item v-for="(att, idx) in formData.attachments" :key="idx">
              <span><FileOutlined /> {{ att.fileName }}</span>
              <template #actions>
                <!-- eslint-disable-next-line prettier/prettier -->
                <a style="color: #ff4d4f" @click="removeAttachment(idx)"
                  >删除</a
                >
              </template>
            </List.Item>
          </List>
        </FormItem>
      </Form>
    </Modal>

    <!-- 查看详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      :title="currentRecord?.title"
      width="700px"
      :footer="null"
    >
      <div v-if="currentRecord">
        <div style="margin-bottom: 16px; color: rgb(0 0 0 / 45%)">
          发布时间：{{ currentRecord.publishedAt }}
        </div>
        <div style="line-height: 1.8; white-space: pre-wrap">
          {{ currentRecord.content || '暂无内容' }}
        </div>

        <div
          v-if="currentRecord.attachments?.length"
          style="
            padding-top: 16px;
            margin-top: 24px;
            border-top: 1px solid #f0f0f0;
          "
        >
          <div style="margin-bottom: 8px; font-weight: 500">附件：</div>
          <Space direction="vertical">
            <a
              v-for="(att, idx) in currentRecord.attachments"
              :key="idx"
              @click="downloadFile(att.fileUrl, att.fileName)"
            >
              <DownloadOutlined /> {{ att.fileName }}
            </a>
          </Space>
        </div>
      </div>
    </Modal>

    <!-- 上传合格证弹窗 -->
    <Modal
      v-model:open="uploadVisible"
      title="上传培训合格证"
      width="500px"
      @ok="handleSubmitCertificate"
      :confirm-loading="uploading"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="培训通知">
          <span>{{ currentRecord?.title }}</span>
        </FormItem>
        <FormItem label="合格证文件" required>
          <Upload
            :before-upload="() => false"
            :max-count="1"
            accept=".pdf,.jpg,.jpeg,.png"
            @change="handleCertificateUpload"
          >
            <Button :loading="uploading">
              <UploadOutlined />
              {{ uploadForm.certificateName || '选择文件（PDF/图片）' }}
            </Button>
          </Upload>
          <div
            v-if="uploadForm.certificateName"
            style="margin-top: 8px; color: #52c41a"
          >
            <FileOutlined /> {{ uploadForm.certificateName }}
          </div>
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
