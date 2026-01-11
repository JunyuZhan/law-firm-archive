<script setup lang="ts">
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Button,
  Space,
  Tag,
  Tooltip,
  Empty,
  Popconfirm,
} from 'ant-design-vue';
import {
  Eye,
  ExternalLink,
  Trash,
  Inbox,
  BookOpenText,
  SvgDownloadIcon,
} from '@vben/icons';
import type { VbenFormSchema } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getDocumentList,
  deleteDocument,
  downloadDocument,
  previewDocument,
} from '#/api/document';
import type { DocumentDTO } from '#/api/document';
import { useUserStore } from '@vben/stores';

defineOptions({ name: 'MyDocuments' });

const router = useRouter();
const userStore = useUserStore();

// ==================== 常量选项 ====================

// 文件类型映射
const fileTypeMap: Record<string, { color: string; text: string }> = {
  pdf: { color: 'red', text: 'PDF' },
  doc: { color: 'blue', text: 'Word' },
  docx: { color: 'blue', text: 'Word' },
  xls: { color: 'green', text: 'Excel' },
  xlsx: { color: 'green', text: 'Excel' },
  ppt: { color: 'orange', text: 'PPT' },
  pptx: { color: 'orange', text: 'PPT' },
  txt: { color: 'default', text: '文本' },
  jpg: { color: 'purple', text: '图片' },
  jpeg: { color: 'purple', text: '图片' },
  png: { color: 'purple', text: '图片' },
};

// 文档范围选项
const docScopeOptions = [
  { label: '仅个人文书', value: 'personal' },
  { label: '全部文书', value: 'all' },
];

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'name',
    label: '文档名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入文档名称',
      allowClear: true,
    },
  },
  {
    fieldName: 'docScope',
    label: '文档范围',
    component: 'Select',
    componentProps: {
      placeholder: '选择范围',
      options: docScopeOptions,
    },
    defaultValue: 'personal',  // 默认只显示个人文书
  },
];

// ==================== 表格配置 ====================

const gridColumns = [
  { title: '文档名称', field: 'title', width: 300, showOverflow: true, slots: { default: 'title' } },
  { title: '类型', field: 'fileType', width: 80, slots: { default: 'fileType' } },
  { title: '来源', field: 'matterName', width: 150, showOverflow: true, slots: { default: 'source' } },
  { title: '大小', field: 'fileSize', width: 100, slots: { default: 'fileSize' } },
  { title: '创建时间', field: 'createdAt', width: 160, slots: { default: 'createdAt' } },
  { title: '操作', field: 'action', width: 150, fixed: 'right' as const, slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: { page: number; pageSize: number } & Record<string, any>) {
  const userId = userStore.userInfo?.userId || userStore.userInfo?.id;
  if (!userId) {
    return { items: [], total: 0 };
  }
  
  const queryParams: any = {
    pageNum: params.page,
    pageSize: params.pageSize,
    title: params.name,
    createdBy: userId,  // 使用 createdBy 查询当前用户创建的文档
  };
  
  const res = await getDocumentList(queryParams);
  let list = res.list || [];
  let total = res.total || 0;
  
  // 根据文档范围过滤
  // 默认或选择 'personal' 时，只显示个人文书（不关联项目的）
  const docScope = params.docScope || 'personal';
  if (docScope === 'personal') {
    list = list.filter(doc => !doc.matterId);
    total = list.length;  // 前端过滤后需要重新计算总数
  }
  
  return {
    items: list,
    total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    submitButtonOptions: { content: '搜索' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page, form }: { page: any; form: any }) => {
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
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// ==================== 辅助方法 ====================

function getFileTypeTag(fileType: string | undefined) {
  if (!fileType) return { color: 'default', text: '未知' };
  const type = fileType.toLowerCase().replace('.', '');
  return fileTypeMap[type] || { color: 'default', text: fileType.toUpperCase() };
}

function formatFileSize(bytes: number | undefined): string {
  if (!bytes) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`;
}

// ==================== 操作方法 ====================

async function handlePreview(row: DocumentDTO) {
  try {
    const url = await previewDocument(row.id);
    if (url) {
      window.open(url, '_blank');
    } else {
      message.warning('暂不支持预览此类型文件');
    }
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '预览失败');
  }
}

async function handleDownload(row: DocumentDTO) {
  try {
    const blob = await downloadDocument(row.id);
    const url = window.URL.createObjectURL(blob as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = row.fileName || row.title || '文档';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    message.success('下载成功');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '下载失败');
  }
}

async function handleDelete(row: DocumentDTO) {
  try {
    await deleteDocument(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '删除失败');
  }
}

function goToMatterDossier(row: DocumentDTO) {
  if (row.matterId) {
    router.push(`/matter/detail/${row.matterId}?tab=dossier`);
  }
}

function goToCompose() {
  router.push('/document/compose');
}

</script>

<template>
  <Page title="我的文书" description="查看和管理您的个人文书（不关联项目的文书），项目文书请到对应项目卷宗中查看" auto-content-height>
    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Button type="primary" @click="goToCompose">
          <template #icon><BookOpenText /></template>
          制作文书
        </Button>
      </template>

      <!-- 文档名称列 -->
      <template #title="{ row }">
        <div style="display: flex; gap: 8px; align-items: center">
          <BookOpenText style=" flex-shrink: 0;width: 16px; height: 16px; color: #1890ff" />
          <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
            {{ row.title || row.fileName || '未命名文档' }}
          </span>
        </div>
      </template>

      <!-- 文件类型列 -->
      <template #fileType="{ row }">
        <Tag :color="getFileTypeTag(row.fileType).color">
          {{ getFileTypeTag(row.fileType).text }}
        </Tag>
      </template>

      <!-- 来源列 -->
      <template #source="{ row }">
        <template v-if="row.matterId">
          <a style="display: inline-flex; gap: 4px; align-items: center; color: #1890ff" @click="goToMatterDossier(row)">
            <Inbox style="width: 14px; height: 14px" />
            {{ row.matterName || `项目#${row.matterId}` }}
          </a>
        </template>
        <template v-else>
          <Tag color="blue">个人文书</Tag>
        </template>
      </template>

      <!-- 文件大小列 -->
      <template #fileSize="{ row }">
        {{ formatFileSize(row.fileSize) }}
      </template>

      <!-- 创建时间列 -->
      <template #createdAt="{ row }">
        {{ row.createdAt?.substring(0, 16).replace('T', ' ') || '-' }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <Tooltip title="预览">
            <Button type="link" size="small" @click="handlePreview(row)">
              <template #icon><Eye /></template>
            </Button>
          </Tooltip>
          <Tooltip title="下载">
            <Button type="link" size="small" @click="handleDownload(row)">
              <template #icon><SvgDownloadIcon /></template>
            </Button>
          </Tooltip>
          <Tooltip v-if="row.matterId" title="查看卷宗">
            <Button type="link" size="small" @click="goToMatterDossier(row)">
              <template #icon><ExternalLink /></template>
            </Button>
          </Tooltip>
          <Popconfirm
            title="确定要删除这份文书吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete(row)"
          >
            <Tooltip title="删除">
              <Button type="link" size="small" danger>
                <template #icon><Trash /></template>
              </Button>
            </Tooltip>
          </Popconfirm>
        </Space>
      </template>

      <!-- 空状态 -->
      <template #empty>
        <Empty description="暂无文书">
          <Button type="primary" @click="goToCompose">
            <template #icon><BookOpenText /></template>
            立即制作文书
          </Button>
        </Empty>
      </template>
    </Grid>
  </Page>
</template>
