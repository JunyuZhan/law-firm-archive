<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { requestClient } from '#/api/request';

import ContractPreviewModal from './components/ContractPreviewModal.vue';
import ContractTemplateModal from './components/ContractTemplateModal.vue';

defineOptions({ name: 'SystemContractTemplate' });

interface ContractTemplateDTO {
  id: number;
  templateNo: string;
  name: string;
  contractType: string;
  contractTypeName: string;
  feeType: string;
  feeTypeName: string;
  content: string;
  clauses: string;
  description: string;
  status: string;
  sortOrder: number;
  createdAt: string;
}

// ==================== 状态定义 ====================

const templateModalRef = ref<InstanceType<typeof ContractTemplateModal>>();
const previewModalRef = ref<InstanceType<typeof ContractPreviewModal>>();

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '模板编号', field: 'templateNo', width: 100 },
  { title: '模板名称', field: 'name', width: 150 },
  { title: '合同类型', field: 'contractTypeName', width: 100 },
  { title: '收费方式', field: 'feeTypeName', width: 100 },
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
  const list = await requestClient.get<ContractTemplateDTO[]>(
    '/system/contract-template/list',
  );
  return {
    items: list,
    total: list.length,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async () => {
          return await loadData();
        },
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

function handleEdit(row: ContractTemplateDTO) {
  templateModalRef.value?.openEdit(row);
}

function handlePreview(row: ContractTemplateDTO) {
  previewModalRef.value?.open(row);
}

async function handleToggle(row: ContractTemplateDTO) {
  try {
    await requestClient.post(`/system/contract-template/${row.id}/toggle`);
    message.success(row.status === 'ACTIVE' ? '已停用' : '已启用');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

function handleDelete(row: ContractTemplateDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板 "${row.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await requestClient.delete(`/system/contract-template/${row.id}`);
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
    title="合同模板管理"
    description="管理委托合同模板，支持富文本编辑和变量插入"
    auto-content-height
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

    <!-- 编辑弹窗 -->
    <ContractTemplateModal
      ref="templateModalRef"
      @success="handleModalSuccess"
    />

    <!-- 预览弹窗 -->
    <ContractPreviewModal ref="previewModalRef" />
  </Page>
</template>
