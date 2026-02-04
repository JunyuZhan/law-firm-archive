<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { LetterTemplateDTO } from '#/api/admin';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteTemplate,
  getAllTemplates,
  toggleTemplateStatus,
} from '#/api/admin';

import PreviewModal from './components/PreviewModal.vue';
import TemplateModal from './components/TemplateModal.vue';

defineOptions({ name: 'SystemLetterTemplate' });

// ==================== 状态定义 ====================

const templateModalRef = ref<InstanceType<typeof TemplateModal>>();
const previewModalRef = ref<InstanceType<typeof PreviewModal>>();

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '模板编号', field: 'templateNo', width: 120 },
  { title: '模板名称', field: 'name', width: 150 },
  { title: '函件类型', field: 'letterTypeName', width: 100 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '描述', field: 'description', minWidth: 150 },
  { title: '创建时间', field: 'createdAt', width: 160 },
  {
    title: '操作',
    field: 'action',
    width: 200,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData() {
  const list = await getAllTemplates();
  return {
    items: list,
    total: list.length,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    // 移除高度限制，让表格完整显示所有数据
    height: '',
    minHeight: 200,
    proxyConfig: {
      ajax: {
        query: loadData,
      },
    },
    pagerConfig: {
      enabled: false,
    },
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// ==================== 操作方法 ====================

function handleAdd() {
  templateModalRef.value?.openCreate();
}

function handleEdit(row: LetterTemplateDTO) {
  templateModalRef.value?.openEdit(row);
}

function handlePreview(row: LetterTemplateDTO) {
  previewModalRef.value?.open(row);
}

async function handleToggle(row: LetterTemplateDTO) {
  try {
    await toggleTemplateStatus(row.id);
    message.success(row.status === 'ACTIVE' ? '已停用' : '已启用');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

function handleDelete(row: LetterTemplateDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板 "${row.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await deleteTemplate(row.id);
        message.success('删除成功');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

function handleModalSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page
    title="出函模板管理"
    description="管理律师出函/介绍信模板，支持富文本编辑和变量插入"
  >
    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Button type="primary" @click="handleAdd">
          <Plus class="size-4" /> 新增模板
        </Button>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="row.status === 'ACTIVE' ? 'green' : 'default'">
          {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
        </Tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handlePreview(row)">预览</a>
          <a @click="handleEdit(row)">编辑</a>
          <a @click="handleToggle(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </a>
          <a style="color: #ff4d4f" @click="handleDelete(row)">删除</a>
        </Space>
      </template>
    </Grid>

    <!-- 模板弹窗 -->
    <TemplateModal ref="templateModalRef" @success="handleModalSuccess" />

    <!-- 预览弹窗 -->
    <PreviewModal ref="previewModalRef" />
  </Page>
</template>
