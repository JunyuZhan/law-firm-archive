<script setup lang="ts">
import { ref } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Button,
  Space,
  Input,
  Select,
  Row,
  Col,
  Popconfirm,
  Tag,
} from 'ant-design-vue';
import { Plus } from '@vben/icons';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getLeadList, deleteLead, convertLeadToClient } from '#/api/client';
import type { LeadDTO, LeadQuery } from '#/api/client/types';
import LeadModal from './components/LeadModal.vue';

defineOptions({ name: 'CrmLead' });

// ==================== 状态定义 ====================

const leadModalRef = ref<InstanceType<typeof LeadModal>>();
const queryParams = ref<LeadQuery>({
  pageNum: 1,
  pageSize: 10,
  clientName: undefined,
  source: undefined,
  status: undefined,
});

// ==================== 常量选项 ====================

const sourceOptions = [
  { label: '全部', value: undefined },
  { label: '转介绍', value: 'REFERRAL' },
  { label: '网络', value: 'ONLINE' },
  { label: '电话咨询', value: 'PHONE' },
  { label: '上门咨询', value: 'WALK_IN' },
  { label: '其他', value: 'OTHER' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '新建', value: 'NEW' },
  { label: '跟进中', value: 'FOLLOWING' },
  { label: '已转化', value: 'CONVERTED' },
  { label: '已流失', value: 'LOST' },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '案源编号', field: 'leadNo', width: 130 },
  { title: '客户名称', field: 'clientName', minWidth: 150 },
  { title: '联系人', field: 'contactPerson', width: 100 },
  { title: '联系电话', field: 'contactPhone', width: 130 },
  { title: '来源', field: 'sourceName', width: 100 },
  { title: '预估金额', field: 'estimatedAmount', width: 120, slots: { default: 'estimatedAmount' } },
  { title: '状态', field: 'statusName', width: 100, slots: { default: 'status' } },
  { title: '跟进人', field: 'followUpUserName', width: 100 },
  { title: '创建时间', field: 'createdAt', width: 160 },
  { title: '操作', field: 'action', width: 180, fixed: 'right', slots: { default: 'action' } },
];

async function loadData({ page }: { page: { currentPage: number; pageSize: number } }) {
  const params = { ...queryParams.value, pageNum: page.currentPage, pageSize: page.pageSize };
  const res = await getLeadList(params);
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

// ==================== 操作方法 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = { pageNum: 1, pageSize: 10, clientName: undefined, source: undefined, status: undefined };
  gridApi.reload();
}

function handleAdd() {
  leadModalRef.value?.open();
}

function handleEdit(row: LeadDTO) {
  leadModalRef.value?.open(row);
}

async function handleDelete(row: LeadDTO) {
  try {
    await deleteLead(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

function handleConvert(row: LeadDTO) {
  Modal.confirm({
    title: '确认转化',
    content: `确定要将案源 "${row.clientName}" 转化为正式客户吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await convertLeadToClient(row.id);
        message.success('转化成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '转化失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    NEW: 'blue',
    FOLLOWING: 'orange',
    CONVERTED: 'green',
    LOST: 'red',
  };
  return colorMap[status] || 'default';
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}
</script>

<template>
  <Page title="案源管理" description="管理案源线索和跟进">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="6">
            <Input
              v-model:value="queryParams.clientName"
              placeholder="客户名称"
              allowClear
              @pressEnter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.source"
              placeholder="来源"
              allowClear
              style="width: 100%"
              :options="sourceOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allowClear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />新增案源
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <!-- 预估金额列 -->
        <template #estimatedAmount="{ row }">
          {{ formatMoney(row.estimatedAmount) }}
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <Space>
            <a @click="handleEdit(row)">编辑</a>
            <template v-if="row.status !== 'CONVERTED'">
              <a @click="handleConvert(row)">转化</a>
            </template>
            <Popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <a style="color: red">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <LeadModal ref="leadModalRef" @success="gridApi.reload()" />
  </Page>
</template>
