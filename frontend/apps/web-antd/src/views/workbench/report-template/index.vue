<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  CreateReportTemplateCommand,
  ReportTemplateDTO,
} from '#/api/workbench/report-template';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createReportTemplate,
  deleteReportTemplate,
  disableReportTemplate,
  enableReportTemplate,
  generateReportByTemplate,
  getReportDataSourceFields,
  getReportDataSources,
  getReportTemplateDetail,
  getReportTemplateList,
  updateReportTemplate,
} from '#/api/workbench/report-template';

defineOptions({ name: 'ReportTemplateManagement' });

// ==================== 数据源选项 ====================
const dataSourceOptions = ref<{ label: string; value: string }[]>([]);

async function loadDataSources() {
  try {
    const res = await getReportDataSources();
    dataSourceOptions.value = (res || []).map((ds: any) => ({
      label: ds.name || ds.code,
      value: ds.code,
    }));
  } catch (error: any) {
    console.error('加载数据源失败', error);
  }
}

// 状态选项
const statusOptions = [
  { label: '启用', value: 'ENABLED', color: 'green' },
  { label: '停用', value: 'DISABLED', color: 'default' },
];

// ==================== 搜索表单配置 ====================
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'keyword',
    label: '关键词',
    component: 'Input',
    componentProps: {
      placeholder: '模板名称',
      allowClear: true,
    },
  },
  {
    fieldName: 'dataSource',
    label: '数据源',
    component: 'Select',
    componentProps: {
      placeholder: '请选择',
      allowClear: true,
      options: dataSourceOptions,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择',
      allowClear: true,
      options: statusOptions,
    },
  },
];

// ==================== 表格配置 ====================
const gridColumns: any[] = [
  {
    title: '模板名称',
    field: 'templateName',
    minWidth: 180,
    showOverflow: true,
  },
  { title: '数据源', field: 'dataSourceName', width: 150 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  {
    title: '系统模板',
    field: 'isSystem',
    width: 100,
    align: 'center',
    slots: { default: 'isSystem' },
  },
  { title: '创建人', field: 'createdByName', width: 100 },
  {
    title: '创建时间',
    field: 'createdAt',
    width: 160,
    slots: { default: 'createdAt' },
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
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  const res = await getReportTemplateList({
    pageNum: params.page,
    pageSize: params.pageSize,
    keyword: params.keyword,
    dataSource: params.dataSource,
    status: params.status,
  });
  // 后端返回 records 字段
  const records = (res as any).records || (res as any).list || [];
  return {
    items: records,
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
const editForm = reactive<CreateReportTemplateCommand & { id?: number }>({
  templateName: '',
  description: '',
  dataSource: '',
  fieldConfig: [],
});
const editLoading = ref(false);
const isEdit = ref(false);

// 字段配置
const availableFields = ref<any[]>([]);
const selectedFields = ref<string[]>([]);

async function loadDataSourceFields(dataSource: string) {
  if (!dataSource) {
    availableFields.value = [];
    return;
  }
  try {
    const res = await getReportDataSourceFields(dataSource);
    availableFields.value = res || [];
  } catch (error: any) {
    console.error('加载字段失败', error);
    availableFields.value = [];
  }
}

function handleCreate() {
  isEdit.value = false;
  Object.assign(editForm, {
    id: undefined,
    templateName: '',
    description: '',
    dataSource: '',
    fieldConfig: [],
  });
  selectedFields.value = [];
  availableFields.value = [];
  editModalVisible.value = true;
}

async function handleEdit(row: ReportTemplateDTO) {
  isEdit.value = true;
  try {
    const detail = await getReportTemplateDetail(row.id);
    Object.assign(editForm, {
      id: detail.id,
      templateName: detail.templateName,
      description: detail.description,
      dataSource: detail.dataSource,
      fieldConfig: detail.fieldConfig || [],
    });
    selectedFields.value = (detail.fieldConfig || []).map((f) => f.field);
    if (detail.dataSource) {
      await loadDataSourceFields(detail.dataSource);
    }
    editModalVisible.value = true;
  } catch (error: any) {
    message.error(`加载详情失败：${error.message || '未知错误'}`);
  }
}

async function handleDataSourceChange(value: string) {
  editForm.dataSource = value;
  editForm.fieldConfig = [];
  selectedFields.value = [];
  await loadDataSourceFields(value);
}

async function handleEditSubmit() {
  if (!editForm.templateName?.trim()) {
    message.warning('请输入模板名称');
    return;
  }
  if (!editForm.dataSource) {
    message.warning('请选择数据源');
    return;
  }
  if (selectedFields.value.length === 0) {
    message.warning('请选择至少一个字段');
    return;
  }

  // 构建字段配置
  editForm.fieldConfig = selectedFields.value.map((field) => {
    const fieldInfo = availableFields.value.find((f) => f.field === field);
    return {
      field,
      label: fieldInfo?.label || field,
      type: fieldInfo?.type || 'STRING',
      visible: true,
    };
  });

  editLoading.value = true;
  try {
    const data: CreateReportTemplateCommand = {
      templateName: editForm.templateName,
      description: editForm.description,
      dataSource: editForm.dataSource,
      fieldConfig: editForm.fieldConfig,
    };

    if (isEdit.value && editForm.id) {
      await updateReportTemplate(editForm.id, data);
      message.success('更新成功');
    } else {
      await createReportTemplate(data);
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

// ==================== 操作方法 ====================
async function handleEnable(row: ReportTemplateDTO) {
  try {
    await enableReportTemplate(row.id);
    message.success('启用成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`启用失败：${error.message || '未知错误'}`);
  }
}

async function handleDisable(row: ReportTemplateDTO) {
  try {
    await disableReportTemplate(row.id);
    message.success('停用成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`停用失败：${error.message || '未知错误'}`);
  }
}

async function handleDelete(row: ReportTemplateDTO) {
  try {
    await deleteReportTemplate(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(`删除失败：${error.message || '未知错误'}`);
  }
}

async function handleGenerate(row: ReportTemplateDTO) {
  try {
    message.loading({ content: '正在生成报表...', key: 'generate' });
    await generateReportByTemplate(row.id, { format: 'EXCEL' });
    message.success({ content: '报表生成成功', key: 'generate' });
  } catch (error: any) {
    message.error({
      content: `生成失败：${error.message || '未知错误'}`,
      key: 'generate',
    });
  }
}

// ==================== 工具方法 ====================
function getStatusColor(status: string) {
  const option = statusOptions.find((o) => o.value === status);
  return option?.color || 'default';
}

function getStatusName(status: string) {
  const option = statusOptions.find((o) => o.value === status);
  return option?.label || status;
}

function formatDateTime(date: string | null | undefined) {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
}

// 初始化
onMounted(() => {
  loadDataSources();
});
</script>

<template>
  <Page
    title="报表模板管理"
    description="创建和管理自定义报表模板，支持灵活配置数据字段"
  >
    <Grid>
      <!-- 工具栏 -->
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">新建模板</Button>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName || getStatusName(row.status) }}
        </Tag>
      </template>

      <!-- 系统模板列 -->
      <template #isSystem="{ row }">
        <Tag v-if="row.isSystem" color="blue">是</Tag>
        <span v-else>-</span>
      </template>

      <!-- 创建时间列 -->
      <template #createdAt="{ row }">
        {{ formatDateTime(row.createdAt) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a v-if="!row.isSystem" @click="handleEdit(row as ReportTemplateDTO)"
            >编辑</a
          >
          <a
            v-if="row.status === 'DISABLED'"
            @click="handleEnable(row as ReportTemplateDTO)"
            >启用</a
          >
          <a
            v-if="row.status === 'ENABLED'"
            @click="handleDisable(row as ReportTemplateDTO)"
            >停用</a
          >
          <a
            v-if="row.status === 'ENABLED'"
            @click="handleGenerate(row as ReportTemplateDTO)"
            >生成</a
          >
          <Popconfirm
            v-if="!row.isSystem"
            title="确定删除此模板？"
            @confirm="handleDelete(row as ReportTemplateDTO)"
          >
            <a style="color: #ff4d4f">删除</a>
          </Popconfirm>
        </Space>
      </template>
    </Grid>

    <!-- 编辑弹窗 -->
    <Modal
      v-model:open="editModalVisible"
      :title="isEdit ? '编辑模板' : '新建模板'"
      :confirm-loading="editLoading"
      width="650px"
      @ok="handleEditSubmit"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <Form.Item label="模板名称" required>
          <Input
            v-model:value="editForm.templateName"
            placeholder="请输入模板名称"
            :maxlength="100"
          />
        </Form.Item>
        <Form.Item label="数据源" required>
          <Select
            :value="editForm.dataSource"
            placeholder="请选择数据源"
            :options="dataSourceOptions"
            :disabled="isEdit"
            @change="(val: any) => handleDataSourceChange(val as string)"
          />
        </Form.Item>
        <Form.Item label="描述">
          <Input.TextArea
            v-model:value="editForm.description"
            :rows="2"
            placeholder="请输入描述"
          />
        </Form.Item>
        <Form.Item label="选择字段" required>
          <Select
            v-model:value="selectedFields"
            mode="multiple"
            placeholder="请选择要显示的字段"
            :options="
              availableFields.map((f) => ({
                label: f.label || f.field,
                value: f.field,
              }))
            "
            style="width: 100%"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>
