<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type {
  CaseCategoryDTO,
  CaseLibraryDTO,
  CaseLibraryQuery,
} from '#/api/knowledge';

import { onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { useResponsive } from '#/hooks/useResponsive';

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
  TreeSelect,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  collectCase,
  deleteCase,
  getCaseCategoryTree,
  getCaseList,
  uncollectCase,
} from '#/api/knowledge';

import CaseDetailModal from './components/CaseDetailModal.vue';
import CaseModal from './components/CaseModal.vue';

defineOptions({ name: 'KnowledgeCase' });

// 响应式布局
const { isMobile } = useResponsive();

const caseModalRef = ref<InstanceType<typeof CaseModal>>();
const caseDetailModalRef = ref<InstanceType<typeof CaseDetailModal>>();
const categories = ref<CaseCategoryDTO[]>([]);

const queryParams = ref<CaseLibraryQuery>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  caseType: undefined,
  categoryId: undefined,
});

const caseTypeOptions = [
  { label: '民事', value: 'CIVIL' },
  { label: '刑事', value: 'CRIMINAL' },
  { label: '行政', value: 'ADMINISTRATIVE' },
  { label: '破产', value: 'BANKRUPTCY' },
  { label: '知识产权', value: 'IP' },
  { label: '仲裁', value: 'ARBITRATION' },
  { label: '执行', value: 'ENFORCEMENT' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '专项服务', value: 'SPECIAL_SERVICE' },
];

const referenceValueMap: Record<string, { color: string; text: string }> = {
  HIGH: { color: 'red', text: '高' },
  MEDIUM: { color: 'orange', text: '中' },
  LOW: { color: 'blue', text: '低' },
};

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    {
      title: '案例名称',
      field: 'name',
      minWidth: isMobile.value ? 120 : 200,
      mobileShow: true,
    },
    { title: '案由类型', field: 'caseTypeName', width: 100, mobileShow: true },
    { title: '审理法院', field: 'court', width: 150 },
    {
      title: '判决日期',
      field: 'judgmentDate',
      width: 110,
      slots: { default: 'judgmentDate' },
    },
    { title: '案件结果', field: 'resultName', width: 90 },
    { title: '经办律师', field: 'lawyerName', width: 100 },
    {
      title: '参考价值',
      field: 'referenceValue',
      width: 90,
      slots: { default: 'referenceValue' },
      mobileShow: true,
    },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 100 : 180,
      fixed: 'right',
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
}

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
  const res = await getCaseList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: getGridColumns(),
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

async function loadCategories() {
  try {
    const res = await getCaseCategoryTree();
    categories.value = res || [];
  } catch (error) {
    console.error('加载分类失败', error);
  }
}

function convertToTreeData(data: CaseCategoryDTO[]): any[] {
  return data.map((item) => ({
    title: item.name,
    value: item.id,
    children: item.children ? convertToTreeData(item.children) : undefined,
  }));
}

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    name: '',
    caseType: undefined,
    categoryId: undefined,
  };
  gridApi.reload();
}

function handleAdd() {
  caseModalRef.value?.open();
}

function handleEdit(row: CaseLibraryDTO) {
  caseModalRef.value?.open(row);
}

function handleView(row: CaseLibraryDTO) {
  caseDetailModalRef.value?.open(row);
}

async function handleDelete(row: CaseLibraryDTO) {
  try {
    await deleteCase(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

async function handleCollect(row: CaseLibraryDTO) {
  try {
    if (row.collected) {
      await uncollectCase(row.id);
      message.success('已取消收藏');
    } else {
      await collectCase(row.id);
      message.success('收藏成功');
    }
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

onMounted(() => {
  loadCategories();
});
</script>

<template>
  <Page title="案例库" description="管理典型案例">
    <Card>
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="6" :lg="4">
            <Select
              v-model:value="queryParams.caseType"
              placeholder="案由类型"
              style="width: 100%"
              allow-clear
              :options="caseTypeOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="4">
            <TreeSelect
              v-model:value="queryParams.categoryId"
              placeholder="案例分类"
              style="width: 100%"
              allow-clear
              :tree-data="convertToTreeData(categories)"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="4">
            <Input
              v-model:value="queryParams.name"
              placeholder="搜索案例"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="12">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />添加案例
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <template #judgmentDate="{ row }">
          {{ formatDate(row.judgmentDate) }}
        </template>
        <template #referenceValue="{ row }">
          <Tag
            :color="referenceValueMap[row.referenceValue]?.color || 'default'"
          >
            {{
              row.referenceValueName ||
              referenceValueMap[row.referenceValue]?.text
            }}
          </Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <a @click="handleCollect(row)">{{
              row.collected ? '取消收藏' : '收藏'
            }}</a>
            <a @click="handleView(row)">查看</a>
            <a @click="handleEdit(row)">编辑</a>
            <Popconfirm title="确定删除此案例？" @confirm="handleDelete(row)">
              <a style="color: #ff4d4f">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <CaseModal ref="caseModalRef" @success="gridApi.reload()" />
    <CaseDetailModal ref="caseDetailModalRef" />
  </Page>
</template>
