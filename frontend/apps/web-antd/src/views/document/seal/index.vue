<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { message, Tag, Space, Popconfirm, Card, Button, Input, Select, Row, Col } from 'ant-design-vue';
import { Plus } from '@vben/icons';
import { getSealList, deleteSeal, changeSealStatus } from '#/api/document/seal';
import type { SealDTO, SealQuery } from '#/api/document/seal-types';
import SealModal from './components/SealModal.vue';

defineOptions({ name: 'DocumentSeal' });

const sealModalRef = ref<InstanceType<typeof SealModal>>();
const queryParams = ref<SealQuery>({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  sealType: undefined,
  status: undefined,
});

const sealTypeOptions = [
  { label: '公章', value: 'OFFICIAL' },
  { label: '合同章', value: 'CONTRACT' },
  { label: '财务章', value: 'FINANCE' },
  { label: '法人章', value: 'LEGAL' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '可用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
];

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '印章名称', field: 'name', minWidth: 150 },
  { title: '印章类型', field: 'sealTypeName', width: 120 },
  { title: '保管人', field: 'keeperName', width: 100 },
  { title: '使用次数', field: 'useCount', width: 100 },
  { title: '状态', field: 'statusName', width: 100, slots: { default: 'status' } },
  { title: '创建时间', field: 'createdAt', width: 160 },
  { title: '操作', field: 'action', width: 180, fixed: 'right', slots: { default: 'action' } },
];

async function loadData({ page }: { page: { currentPage: number; pageSize: number } }) {
  const params = { ...queryParams.value, pageNum: page.currentPage, pageSize: page.pageSize };
  const res = await getSealList(params);
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

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = { pageNum: 1, pageSize: 10, name: undefined, sealType: undefined, status: undefined };
  gridApi.reload();
}

function handleAdd() {
  sealModalRef.value?.open();
}

function handleEdit(row: SealDTO) {
  sealModalRef.value?.open(row);
}

async function handleDelete(row: SealDTO) {
  try {
    await deleteSeal(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

async function handleChangeStatus(row: SealDTO) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await changeSealStatus(row.id, newStatus);
    message.success('状态变更成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '状态变更失败');
  }
}

function getStatusColor(status: string) {
  return status === 'ACTIVE' ? 'green' : 'default';
}
</script>

<template>
  <Page title="印章管理" description="管理律所印章">
    <Card>
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :span="6">
            <Input v-model:value="queryParams.name" placeholder="印章名称" allowClear @pressEnter="handleSearch" />
          </Col>
          <Col :span="6">
            <Select v-model:value="queryParams.sealType" placeholder="印章类型" allowClear style="width: 100%" :options="sealTypeOptions" />
          </Col>
          <Col :span="6">
            <Select v-model:value="queryParams.status" placeholder="状态" allowClear style="width: 100%" :options="statusOptions" />
          </Col>
          <Col :span="6">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd"><Plus class="size-4" />添加印章</Button>
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
            <a @click="handleChangeStatus(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</a>
            <Popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <a style="color: red">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <SealModal ref="sealModalRef" @success="gridApi.reload()" />
  </Page>
</template>
