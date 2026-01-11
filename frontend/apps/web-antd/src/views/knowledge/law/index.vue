<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { LawRegulationDTO } from '#/api/knowledge/types';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  message,
  Modal,
  Row,
  Space,
  Tag,
  Tree,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  collectLawRegulation,
  deleteLawRegulation,
  getLawCategoryTree,
  getLawRegulationList,
  markLawRegulationRepealed,
  uncollectLawRegulation,
} from '#/api/knowledge';

import LawModal from './components/LawModal.vue';

defineOptions({ name: 'KnowledgeLaw' });

const lawModalRef = ref<InstanceType<typeof LawModal>>();
const categories = ref<any[]>([]);
const selectedCategoryId = ref<number | undefined>();

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'name',
    label: '法规名称',
    component: 'Input',
    componentProps: { placeholder: '请输入法规名称', allowClear: true },
  },
  {
    fieldName: 'lawType',
    label: '法规类型',
    component: 'Select',
    componentProps: {
      placeholder: '请选择法规类型',
      allowClear: true,
      options: [
        { label: '法律', value: 'LAW' },
        { label: '行政法规', value: 'REGULATION' },
        { label: '部门规章', value: 'RULE' },
        { label: '司法解释', value: 'INTERPRETATION' },
        { label: '地方性法规', value: 'LOCAL' },
      ],
    },
  },
  {
    fieldName: 'status',
    label: '效力状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择效力状态',
      allowClear: true,
      options: [
        { label: '现行有效', value: 'EFFECTIVE' },
        { label: '已修订', value: 'AMENDED' },
        { label: '已废止', value: 'REPEALED' },
      ],
    },
  },
];

const gridColumns: VxeGridProps['columns'] = [
  { title: '法规名称', field: 'name', width: 200 },
  { title: '法规类型', field: 'lawTypeName', width: 120 },
  { title: '分类', field: 'categoryName', width: 120 },
  { title: '发布机关', field: 'issuer', width: 150 },
  { title: '发布日期', field: 'issueDate', width: 120 },
  { title: '实施日期', field: 'effectiveDate', width: 120 },
  {
    title: '效力状态',
    field: 'status',
    width: 100,
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

async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  const res = await getLawRegulationList({
    pageNum: params.page,
    pageSize: params.pageSize,
    name: params.name,
    lawType: params.lawType,
    status: params.status,
    categoryId: selectedCategoryId.value,
  });
  return { items: res.list, total: res.total };
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
        query: async ({
          page,
          form,
        }: {
          form: Record<string, any>;
          page: { currentPage: number; pageSize: number };
        }) =>
          await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...form,
          }),
      },
    },
    pagerConfig: { pageSize: 10, pageSizes: [10, 20, 50, 100] },
    toolbarConfig: { slots: { buttons: 'toolbar-buttons' } },
  },
});

async function loadCategories() {
  try {
    categories.value = await getLawCategoryTree();
  } catch (error) {
    console.error(error);
  }
}

function onCategorySelect(selectedKeys: any[]) {
  selectedCategoryId.value =
    selectedKeys.length > 0 ? selectedKeys[0] : undefined;
  gridApi.reload();
}

function getStatusColor(status: string) {
  return (
    { EFFECTIVE: 'green', AMENDED: 'orange', REPEALED: 'red' }[status] ||
    'default'
  );
}

function handleAdd() {
  lawModalRef.value?.openCreate();
}
function handleEdit(row: LawRegulationDTO) {
  lawModalRef.value?.openEdit(row);
}

function handleDelete(row: LawRegulationDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除法规 "${row.name}" 吗？`,
    onOk: async () => {
      await deleteLawRegulation(row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
}

async function handleCollect(row: LawRegulationDTO) {
  if (row.collected) {
    await uncollectLawRegulation(row.id);
    message.success('已取消收藏');
  } else {
    await collectLawRegulation(row.id);
    message.success('收藏成功');
  }
  gridApi.reload();
}

function handleMarkRepealed(row: LawRegulationDTO) {
  Modal.confirm({
    title: '标注失效',
    content: '确定要将此法规标注为已废止吗？',
    onOk: async () => {
      await markLawRegulationRepealed(row.id);
      message.success('标注成功');
      gridApi.reload();
    },
  });
}

function handleModalSuccess() {
  gridApi.reload();
}

onMounted(() => {
  loadCategories();
});
</script>

<template>
  <Page title="法规库" description="查询和管理法律法规">
    <Row :gutter="16">
      <Col :span="5">
        <Card
          title="法规分类"
          style="height: calc(100vh - 200px); overflow-y: auto"
        >
          <Tree
            :tree-data="categories"
            :field-names="{ children: 'children', title: 'name', key: 'id' }"
            :selected-keys="selectedCategoryId ? [selectedCategoryId] : []"
            @select="onCategorySelect"
          />
        </Card>
      </Col>
      <Col :span="19">
        <Grid>
          <template #toolbar-buttons>
            <Button type="primary" @click="handleAdd"> 新增法规 </Button>
          </template>
          <template #status="{ row }">
            <Tag :color="getStatusColor(row.status)">
              {{ row.statusName }}
            </Tag>
          </template>
          <template #action="{ row }">
            <Space>
              <a @click="handleEdit(row)">编辑</a>
              <a @click="handleCollect(row)">{{
                row.collected ? '取消收藏' : '收藏'
              }}</a>
              <a style="color: #ff4d4f" @click="handleDelete(row)">删除</a>
              <a @click="handleMarkRepealed(row)">标注失效</a>
            </Space>
          </template>
        </Grid>
      </Col>
    </Row>
    <LawModal ref="lawModalRef" @success="handleModalSuccess" />
  </Page>
</template>
