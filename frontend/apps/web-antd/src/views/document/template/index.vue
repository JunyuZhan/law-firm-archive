<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type {
  DocumentTemplateDTO,
  DocumentTemplateQuery,
} from '#/api/document/template-types';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  Col,
  Input,
  message,
  Popconfirm,
  Row,
  Select,
  Space,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createTemplate,
  deleteTemplate,
  getTemplateDetail,
  getTemplateList,
  updateTemplate,
} from '#/api/document/template';

import PreviewModal from './components/PreviewModal.vue';
import TemplateModal from './components/TemplateModal.vue';

defineOptions({ name: 'DocumentTemplate' });

const templateModalRef = ref<InstanceType<typeof TemplateModal>>();
const previewModalRef = ref<InstanceType<typeof PreviewModal>>();

const queryParams = ref<DocumentTemplateQuery>({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  templateType: undefined,
  businessType: undefined,
  status: undefined,
});

const templateTypeOptions = [
  { label: 'Word文档', value: 'WORD' },
  { label: 'Excel表格', value: 'EXCEL' },
  { label: 'PDF文档', value: 'PDF' },
  { label: '富文本', value: 'HTML' },
];

// 业务类型选项（用于模板筛选）
const businessTypeOptions = [
  { label: '诉讼案件', value: 'LITIGATION' },
  { label: '非诉项目', value: 'NON_LITIGATION' },
  { label: '通用', value: 'GENERAL' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
];

const gridColumns: VxeGridProps['columns'] = [
  {
    title: '模板名称',
    field: 'name',
    minWidth: 200,
    slots: { default: 'name' },
  },
  {
    title: '模板类型',
    field: 'templateTypeName',
    width: 100,
    slots: { default: 'templateType' },
  },
  { title: '适用业务', field: 'businessTypeName', width: 100 },
  { title: '创建人', field: 'creatorName', width: 100 },
  { title: '创建时间', field: 'createdAt', width: 160 },
  {
    title: '使用次数',
    field: 'useCount',
    width: 90,
    slots: { default: 'useCount' },
  },
  {
    title: '状态',
    field: 'statusName',
    width: 80,
    slots: { default: 'status' },
  },
  {
    title: '操作',
    field: 'action',
    width: 200,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

async function loadData({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const params = {
    ...queryParams.value,
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  };
  const res = await getTemplateList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
    rowConfig: { keyField: 'id' },
  },
});

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    templateType: undefined,
    businessType: undefined,
    status: undefined,
  };
  gridApi.reload();
}

function handleAdd() {
  templateModalRef.value?.open();
}

function handleEdit(row: DocumentTemplateDTO) {
  templateModalRef.value?.open(row);
}

function handlePreview(row: DocumentTemplateDTO) {
  previewModalRef.value?.open(row);
}

async function handleDelete(row: DocumentTemplateDTO) {
  try {
    await deleteTemplate(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 复制模板
async function handleCopy(row: DocumentTemplateDTO) {
  try {
    const detail = await getTemplateDetail(row.id);
    await createTemplate({
      name: `${detail.name} - 副本`,
      templateType: detail.templateType || 'HTML',
      businessType: detail.businessType,
      content: detail.content || '',
      description: detail.description,
    });
    message.success('复制成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '复制失败');
  }
}

// 切换状态
async function handleToggleStatus(row: DocumentTemplateDTO) {
  try {
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    await updateTemplate(row.id, { status: newStatus });
    message.success(newStatus === 'ACTIVE' ? '已启用' : '已停用');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

function getStatusColor(status: string) {
  return status === 'ACTIVE' ? 'green' : 'default';
}

function getTemplateTypeColor(type: string) {
  const colors: Record<string, string> = {
    WORD: 'blue',
    EXCEL: 'green',
    PDF: 'red',
    HTML: 'purple',
  };
  return colors[type] || 'default';
}
</script>

<template>
  <Page title="文书模板" description="管理文书模板库，支持变量替换自动生成文书">
    <Card>
      <div style="margin-bottom: 16px">
        <Row :gutter="16" style="margin-bottom: 12px">
          <Col :span="5">
            <Input
              v-model:value="queryParams.name"
              placeholder="搜索模板名称"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :span="4">
            <Select
              v-model:value="queryParams.templateType"
              placeholder="模板类型"
              allow-clear
              style="width: 100%"
              :options="templateTypeOptions"
            />
          </Col>
          <Col :span="4">
            <Select
              v-model:value="queryParams.businessType"
              placeholder="适用业务"
              allow-clear
              style="width: 100%"
              :options="businessTypeOptions"
            />
          </Col>
          <Col :span="4">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :span="7">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />新建模板
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <template #name="{ row }">
          <div>
            <a @click="handlePreview(row)" style="font-weight: 500">{{
              row.name
            }}</a>
            <div
              v-if="row.description"
              style="margin-top: 2px; font-size: 12px; color: #999"
            >
              {{ row.description }}
            </div>
          </div>
        </template>
        <template #templateType="{ row }">
          <Tag :color="getTemplateTypeColor(row.templateType)">
            {{ row.templateTypeName || row.templateType }}
          </Tag>
        </template>
        <template #useCount="{ row }">
          <span style="font-weight: 500; color: #1890ff">{{
            row.useCount || 0
          }}</span>
        </template>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <Tooltip title="预览">
              <a @click="handlePreview(row)">预览</a>
            </Tooltip>
            <Tooltip title="编辑">
              <a @click="handleEdit(row)">编辑</a>
            </Tooltip>
            <Tooltip title="复制">
              <a @click="handleCopy(row)">复制</a>
            </Tooltip>
            <Tooltip :title="row.status === 'ACTIVE' ? '停用' : '启用'">
              <a
                @click="handleToggleStatus(row)"
                :style="{
                  color: row.status === 'ACTIVE' ? '#faad14' : '#52c41a',
                }"
              >
                {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
              </a>
            </Tooltip>
            <Popconfirm
              title="确定删除该模板？删除后不可恢复"
              @confirm="handleDelete(row)"
            >
              <a style="color: #ff4d4f">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <TemplateModal ref="templateModalRef" @success="gridApi.reload()" />
    <PreviewModal ref="previewModalRef" />
  </Page>
</template>
