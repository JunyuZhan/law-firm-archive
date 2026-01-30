<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type {
  DocumentTemplateDTO,
  DocumentTemplateQuery,
} from '#/api/document/template';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Space,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteTemplate,
  getTemplateList,
  updateTemplate,
} from '#/api/document/template';

import PreviewModal from './components/PreviewModal.vue';
import TemplateModal from './components/TemplateModal.vue';

defineOptions({ name: 'DocumentTemplate' });

// ==================== 状态定义 ====================

const templateModalRef = ref<InstanceType<typeof TemplateModal>>();
const previewModalRef = ref<InstanceType<typeof PreviewModal>>();

// 搜索条件
const searchForm = ref<DocumentTemplateQuery>({
  name: '',
  templateType: '',
  businessType: '',
  status: '',
});

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '模板名称', field: 'name', minWidth: 180 },
  {
    title: '模板类型',
    field: 'templateType',
    width: 120,
    slots: { default: 'templateType' },
  },
  {
    title: '适用业务',
    field: 'businessTypeName',
    width: 120,
  },
  {
    title: '案件类型',
    field: 'caseTypeName',
    width: 120,
  },
  {
    title: '状态',
    field: 'status',
    width: 80,
    slots: { default: 'status' },
  },
  {
    title: '创建人',
    field: 'creatorName',
    width: 100,
  },
  {
    title: '使用次数',
    field: 'useCount',
    width: 100,
    align: 'right',
  },
  {
    title: '创建时间',
    field: 'createdAt',
    width: 160,
  },
  {
    title: '操作',
    field: 'action',
    width: 220,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData(params: {
  [key: string]: any;
  page: number;
  pageSize: number;
}) {
  const query: DocumentTemplateQuery = {
    pageNum: params.page,
    pageSize: params.pageSize,
    name: searchForm.value.name || undefined,
    templateType: searchForm.value.templateType || undefined,
    businessType: searchForm.value.businessType || undefined,
    status: searchForm.value.status || undefined,
  };
  const res = await getTemplateList(query);
  return {
    items: res.list || [],
    total: res.total || 0,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: '',
    minHeight: 200,
    proxyConfig: {
      ajax: {
        query: async ({
          page,
        }: {
          page: { currentPage: number; pageSize: number };
        }) => {
          return await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
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

// ==================== 操作方法 ====================

function handleAdd() {
  templateModalRef.value?.open();
}

function handleEdit(row: DocumentTemplateDTO) {
  templateModalRef.value?.open(row);
}

function handlePreview(row: DocumentTemplateDTO) {
  previewModalRef.value?.open(row);
}

async function handleToggle(row: DocumentTemplateDTO) {
  try {
    await updateTemplate(row.id, {
      status: row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
    });
    message.success(row.status === 'ACTIVE' ? '已停用' : '已启用');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

function handleDelete(row: DocumentTemplateDTO) {
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

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  searchForm.value = {
    name: '',
    templateType: '',
    businessType: '',
    status: '',
  };
  gridApi.reload();
}

// 模板类型颜色映射
function getTemplateTypeColor(type?: string) {
  const colors: Record<string, string> = {
    WORD: 'blue',
    EXCEL: 'green',
    PDF: 'red',
    HTML: 'purple',
    POWER_OF_ATTORNEY: 'orange',
  };
  return colors[type || ''] || 'default';
}
</script>

<template>
  <Page
    title="文书模板管理"
    description="管理文书模板库，支持变量替换自动生成文书"
  >
    <!-- 搜索栏 -->
    <div
      style="
        padding: 16px;
        margin-bottom: 16px;
        background: #fafafa;
        border-radius: 6px;
      "
    >
      <Space wrap>
        <Input
          v-model:value="searchForm.name"
          placeholder="模板名称"
          style="width: 200px"
          allow-clear
          @press-enter="handleSearch"
        />
        <Select
          v-model:value="searchForm.templateType"
          placeholder="模板类型"
          style="width: 150px"
          allow-clear
        >
          <Select.Option value="WORD">Word文档</Select.Option>
          <Select.Option value="EXCEL">Excel表格</Select.Option>
          <Select.Option value="PDF">PDF文档</Select.Option>
          <Select.Option value="HTML">富文本</Select.Option>
          <Select.Option value="POWER_OF_ATTORNEY">授权委托书</Select.Option>
        </Select>
        <Select
          v-model:value="searchForm.businessType"
          placeholder="适用业务"
          style="width: 150px"
          allow-clear
        >
          <Select.Option value="LITIGATION">诉讼案件</Select.Option>
          <Select.Option value="NON_LITIGATION">非诉项目</Select.Option>
          <Select.Option value="GENERAL">通用</Select.Option>
        </Select>
        <Select
          v-model:value="searchForm.status"
          placeholder="状态"
          style="width: 120px"
          allow-clear
        >
          <Select.Option value="ACTIVE">启用</Select.Option>
          <Select.Option value="DISABLED">停用</Select.Option>
        </Select>
        <Button type="primary" @click="handleSearch">查询</Button>
        <Button @click="handleReset">重置</Button>
      </Space>
    </div>

    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Button type="primary" @click="handleAdd">
          <Plus class="size-4" /> 新增模板
        </Button>
      </template>

      <!-- 模板类型列 -->
      <template #templateType="{ row }">
        <Tag :color="getTemplateTypeColor(row.templateType)">
          {{ row.templateTypeName || row.templateType }}
        </Tag>
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
