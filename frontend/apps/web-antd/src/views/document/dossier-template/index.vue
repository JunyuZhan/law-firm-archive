<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';

import { ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  CASE_TYPE_OPTIONS,
  deleteDossierTemplate,
  getAllDossierTemplates,
} from '#/api/document/dossier';
import { useResponsive } from '#/hooks/useResponsive';

import DirectoryConfigModal from './components/DirectoryConfigModal.vue';
import DossierTemplateModal from './components/DossierTemplateModal.vue';

defineOptions({ name: 'DossierTemplate' });

interface DossierTemplate {
  id: number;
  name: string;
  caseType: string;
  description?: string;
  isDefault: boolean;
}

// 响应式布局
const { isMobile } = useResponsive();

// 响应式列配置
function getGridColumns(): VxeGridProps<DossierTemplate>['columns'] {
  const baseColumns: any[] = [
    {
      title: '模板名称',
      field: 'name',
      minWidth: isMobile.value ? 120 : 150,
      mobileShow: true,
    },
    {
      title: '案件类型',
      field: 'caseType',
      width: isMobile.value ? 100 : 120,
      formatter: ({ cellValue }: { cellValue: string }) => {
        return (
          CASE_TYPE_OPTIONS.find((o) => o.value === cellValue)?.label ||
          cellValue
        );
      },
      mobileShow: true,
    },
    {
      title: '描述',
      field: 'description',
      minWidth: 200,
    },
    {
      title: '默认',
      field: 'isDefault',
      width: 80,
      align: 'center' as const,
      slots: { default: 'default_slot' },
      mobileShow: true,
    },
    {
      title: '操作',
      width: isMobile.value ? 140 : 220,
      fixed: 'right' as const,
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: getGridColumns(),
    height: 'auto',
    minHeight: 200,
    showOverflow: true,
    proxyConfig: {
      ajax: {
        query: async () => {
          const data = await getAllDossierTemplates();
          return { items: data };
        },
      },
    },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

const directoryModalRef = ref<InstanceType<typeof DirectoryConfigModal>>();
const templateModalRef = ref<InstanceType<typeof DossierTemplateModal>>();

// 新增模板
const handleAdd = () => {
  templateModalRef.value?.openCreate();
};

// 配置目录
const handleEditItems = (row: DossierTemplate) => {
  directoryModalRef.value?.open(row.id, row.name);
};

const handleDelete = (row: DossierTemplate) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板 "${row.name}" 吗？此操作将同时删除其关联的所有目录定义。`,
    onOk: async () => {
      await deleteDossierTemplate(row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
};
</script>

<template>
  <Page title="卷宗模板管理" description="管理不同案件类型的标准卷宗目录结构">
    <template #extra>
      <Button type="primary" @click="handleAdd">
        <Plus class="size-4" /> 新增模板
      </Button>
    </template>

    <Grid>
      <template #default_slot="{ row }">
        <Tag :color="row.isDefault ? 'green' : 'default'">
          {{ row.isDefault ? '是' : '否' }}
        </Tag>
      </template>

      <template #action="{ row }">
        <Space :size="isMobile ? 4 : 8">
          <Button
            type="link"
            :size="isMobile ? 'small' : 'small'"
            @click="handleEditItems(row)"
          >
            {{ isMobile ? '配置' : '配置目录' }}
          </Button>
          <Button
            type="link"
            :size="isMobile ? 'small' : 'small'"
            @click="handleDelete(row)"
            danger
          >
            删除
          </Button>
        </Space>
      </template>
    </Grid>

    <!-- 目录配置弹窗 -->
    <DirectoryConfigModal ref="directoryModalRef" @success="gridApi.reload()" />

    <!-- 模板新增/编辑弹窗 -->
    <DossierTemplateModal ref="templateModalRef" @success="gridApi.reload()" />
  </Page>
</template>
