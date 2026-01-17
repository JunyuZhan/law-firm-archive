<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type {
  KnowledgeArticleDTO,
  KnowledgeArticleQuery,
} from '#/api/knowledge/types';

import { ref, watch } from 'vue';

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
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  archiveArticle,
  collectArticle,
  deleteArticle,
  getArticleList,
  likeArticle,
  publishArticle,
  uncollectArticle,
} from '#/api/knowledge';

import ArticleModal from './components/ArticleModal.vue';

defineOptions({ name: 'KnowledgeArticle' });

// 响应式布局
const { isMobile } = useResponsive();

const articleModalRef = ref<InstanceType<typeof ArticleModal>>();
const queryParams = ref<KnowledgeArticleQuery>({
  pageNum: 1,
  pageSize: 10,
  title: undefined,
  category: undefined,
  status: undefined,
});

const categoryOptions = [
  { label: '法律法规', value: 'LAW' },
  { label: '实务经验', value: 'PRACTICE' },
  { label: '文书模板', value: 'TEMPLATE' },
  { label: '行业动态', value: 'NEWS' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' },
];

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    {
      title: '文章标题',
      field: 'title',
      minWidth: isMobile.value ? 150 : 250,
      mobileShow: true,
    },
    { title: '分类', field: 'categoryName', width: 120, mobileShow: true },
    { title: '作者', field: 'authorName', width: 100 },
    { title: '发布时间', field: 'publishTime', width: 160 },
    { title: '浏览量', field: 'views', width: 80 },
    { title: '点赞数', field: 'likes', width: 80 },
    {
      title: '状态',
      field: 'statusName',
      width: 100,
      slots: { default: 'status' },
      mobileShow: true,
    },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 100 : 220,
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
  const res = await getArticleList(params);
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

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    category: undefined,
    status: undefined,
  };
  gridApi.reload();
}

function handleAdd() {
  articleModalRef.value?.open();
}

function handleEdit(row: KnowledgeArticleDTO) {
  articleModalRef.value?.open(row);
}

async function handleDelete(row: KnowledgeArticleDTO) {
  try {
    await deleteArticle(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

async function handlePublish(row: KnowledgeArticleDTO) {
  try {
    await publishArticle(row.id);
    message.success('发布成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '发布失败');
  }
}

async function handleArchive(row: KnowledgeArticleDTO) {
  try {
    await archiveArticle(row.id);
    message.success('归档成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '归档失败');
  }
}

async function handleLike(row: KnowledgeArticleDTO) {
  try {
    await likeArticle(row.id);
    message.success('点赞成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

async function handleCollect(row: KnowledgeArticleDTO) {
  try {
    if (row.collected) {
      await uncollectArticle(row.id);
      message.success('已取消收藏');
    } else {
      await collectArticle(row.id);
      message.success('收藏成功');
    }
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'orange',
    PUBLISHED: 'green',
    ARCHIVED: 'default',
  };
  return colorMap[status] || 'default';
}
</script>

<template>
  <Page title="知识文章" description="管理知识库文章">
    <Card>
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="8" :lg="6">
            <Input
              v-model:value="queryParams.title"
              placeholder="文章标题"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6">
            <Select
              v-model:value="queryParams.category"
              placeholder="文章分类"
              allow-clear
              style="width: 100%"
              :options="categoryOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="24" :lg="6">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />发布文章
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <a @click="handleEdit(row)">编辑</a>
            <a v-if="row.status === 'DRAFT'" @click="handlePublish(row)"
              >发布</a
            >
            <a v-if="row.status === 'PUBLISHED'" @click="handleArchive(row)"
              >归档</a
            >
            <a v-if="row.status === 'PUBLISHED'" @click="handleLike(row)"
              >点赞</a
            >
            <a @click="handleCollect(row)">{{
              row.collected ? '取消收藏' : '收藏'
            }}</a>
            <Popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <a style="color: red">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <ArticleModal ref="articleModalRef" @success="gridApi.reload()" />
  </Page>
</template>
