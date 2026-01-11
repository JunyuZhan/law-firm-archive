<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SupplierDTO, SupplierQuery } from '#/api/admin/supplier';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  Col,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeSupplierStatus,
  deleteSupplier,
  getSupplierList,
  getSupplierStatistics,
} from '#/api/admin/supplier';

import SupplierModal from './components/SupplierModal.vue';

defineOptions({ name: 'AdminSupplier' });

const supplierModalRef = ref<InstanceType<typeof SupplierModal>>();
const statistics = ref({ total: 0, active: 0, inactive: 0 });

const queryParams = ref<SupplierQuery>({
  keyword: undefined,
  supplierType: undefined,
  status: undefined,
  rating: undefined,
});

const supplierTypeOptions = [
  { label: '服务商', value: 'SERVICE' },
  { label: '供应商', value: 'SUPPLIER' },
  { label: '合作伙伴', value: 'PARTNER' },
];

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
];

const ratingOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];

const statusColorMap: Record<string, string> = {
  ACTIVE: 'green',
  INACTIVE: 'red',
};
const ratingColorMap: Record<string, string> = {
  A: 'green',
  B: 'blue',
  C: 'orange',
  D: 'red',
};

const gridColumns: VxeGridProps['columns'] = [
  { title: '供应商编号', field: 'supplierNo', width: 120 },
  { title: '供应商名称', field: 'name', minWidth: 150 },
  { title: '类型', field: 'supplierTypeName', width: 100 },
  { title: '联系人', field: 'contactPerson', width: 100 },
  { title: '联系电话', field: 'contactPhone', width: 120 },
  { title: '联系邮箱', field: 'contactEmail', width: 150 },
  { title: '评级', field: 'rating', width: 80, slots: { default: 'rating' } },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '创建时间', field: 'createdAt', width: 160 },
  {
    title: '操作',
    field: 'action',
    width: 180,
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
  const res = await getSupplierList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

async function loadStatistics() {
  try {
    const stats = await getSupplierStatistics();
    statistics.value = {
      total: stats.total || 0,
      active: stats.active || 0,
      inactive: stats.inactive || 0,
    };
  } catch (error) {
    console.error('获取统计数据失败:', error);
  }
}

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = {
    keyword: undefined,
    supplierType: undefined,
    status: undefined,
    rating: undefined,
  };
  gridApi.reload();
}

function handleAdd() {
  supplierModalRef.value?.open();
}

function handleEdit(row: SupplierDTO) {
  supplierModalRef.value?.open(row);
}

function handleDelete(row: SupplierDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除供应商"${row.name}"吗？`,
    onOk: async () => {
      try {
        await deleteSupplier(row.id);
        message.success('删除成功');
        gridApi.reload();
        loadStatistics();
      } catch (error: any) {
        message.error(error.message || '删除失败');
      }
    },
  });
}

function handleChangeStatus(row: SupplierDTO) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  const action = newStatus === 'ACTIVE' ? '启用' : '停用';
  Modal.confirm({
    title: `确认${action}`,
    content: `确定要${action}供应商"${row.name}"吗？`,
    onOk: async () => {
      try {
        await changeSupplierStatus(row.id, newStatus);
        message.success(`${action}成功`);
        gridApi.reload();
        loadStatistics();
      } catch (error: any) {
        message.error(error.message || `${action}失败`);
      }
    },
  });
}

function handleSuccess() {
  gridApi.reload();
  loadStatistics();
}

onMounted(() => {
  loadStatistics();
});
</script>

<template>
  <Page title="供应商管理" description="管理供应商信息">
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :span="8">
        <Card>
          <Statistic title="供应商总数" :value="statistics.total" suffix="家" />
        </Card>
      </Col>
      <Col :span="8">
        <Card>
          <Statistic
            title="启用"
            :value="statistics.active"
            suffix="家"
            :value-style="{ color: '#3f8600' }"
          />
        </Card>
      </Col>
      <Col :span="8">
        <Card>
          <Statistic
            title="停用"
            :value="statistics.inactive"
            suffix="家"
            :value-style="{ color: '#cf1322' }"
          />
        </Card>
      </Col>
    </Row>

    <Card>
      <div style="margin-bottom: 16px">
        <Space wrap>
          <Input
            v-model:value="queryParams.keyword"
            placeholder="搜索供应商名称/联系人/电话"
            style="width: 200px"
            allow-clear
            @press-enter="handleSearch"
          />
          <Select
            v-model:value="queryParams.supplierType"
            placeholder="供应商类型"
            style="width: 120px"
            allow-clear
            :options="supplierTypeOptions"
          />
          <Select
            v-model:value="queryParams.status"
            placeholder="状态"
            style="width: 100px"
            allow-clear
            :options="statusOptions"
          />
          <Select
            v-model:value="queryParams.rating"
            placeholder="评级"
            style="width: 100px"
            allow-clear
            :options="ratingOptions"
          />
          <Button type="primary" @click="handleSearch">搜索</Button>
          <Button @click="handleReset">重置</Button>
          <Button type="primary" @click="handleAdd">
            <Plus class="size-4" />新增供应商
          </Button>
        </Space>
      </div>

      <Grid>
        <template #rating="{ row }">
          <Tag v-if="row.rating" :color="ratingColorMap[row.rating]">
            {{ row.ratingName || row.rating }}
          </Tag>
          <span v-else>-</span>
        </template>
        <template #status="{ row }">
          <Tag :color="statusColorMap[row.status || '']">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </Tag>
        </template>
        <template #action="{ row }">
          <Space>
            <a @click="handleEdit(row)">编辑</a>
            <a @click="handleChangeStatus(row)">{{
              row.status === 'ACTIVE' ? '停用' : '启用'
            }}</a>
            <a style="color: #ff4d4f" @click="handleDelete(row)">删除</a>
          </Space>
        </template>
      </Grid>
    </Card>

    <SupplierModal ref="supplierModalRef" @success="handleSuccess" />
  </Page>
</template>
