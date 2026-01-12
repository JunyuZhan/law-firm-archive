<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DataHandoverDTO } from '#/api/system/types';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Button, message, Modal, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { cancelHandover, confirmHandover, getHandoverList } from '#/api/system';

import CreateHandoverModal from './components/CreateHandoverModal.vue';
import HandoverDetailModal from './components/HandoverDetailModal.vue';

defineOptions({ name: 'SystemDataHandover' });

// ==================== 状态定义 ====================

const createModalRef = ref<InstanceType<typeof CreateHandoverModal>>();
const detailModalRef = ref<InstanceType<typeof HandoverDetailModal>>();

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'handoverType',
    label: '交接类型',
    component: 'Select',
    componentProps: {
      placeholder: '请选择交接类型',
      allowClear: true,
      options: [
        { label: '离职交接', value: 'RESIGNATION' },
        { label: '项目移交', value: 'PROJECT' },
        { label: '客户移交', value: 'CLIENT' },
        { label: '案源移交', value: 'LEAD' },
      ],
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: [
        { label: '待审批', value: 'PENDING_APPROVAL' },
        { label: '审批通过待执行', value: 'APPROVED' },
        { label: '已拒绝', value: 'REJECTED' },
        { label: '已确认', value: 'CONFIRMED' },
        { label: '已取消', value: 'CANCELLED' },
      ],
    },
  },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '交接单号', field: 'handoverNo', width: 140 },
  { title: '移交人', field: 'fromUsername', width: 100 },
  { title: '接收人', field: 'toUsername', width: 100 },
  { title: '交接类型', field: 'handoverTypeName', width: 100 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '项目', field: 'matterCount', width: 60, align: 'center' },
  { title: '客户', field: 'clientCount', width: 60, align: 'center' },
  { title: '案源', field: 'leadCount', width: 60, align: 'center' },
  { title: '任务', field: 'taskCount', width: 60, align: 'center' },
  { title: '提交人', field: 'submittedByName', width: 100 },
  { title: '提交时间', field: 'submittedAt', width: 160 },
  { title: '确认时间', field: 'confirmedAt', width: 160 },
  {
    title: '操作',
    field: 'action',
    width: 180,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  const res = await getHandoverList({
    pageNum: params.page,
    pageSize: params.pageSize,
    handoverType: params.handoverType,
    status: params.status,
  });
  return {
    items: res.list || (res as any).records || [],
    total: res.total,
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
        query: async ({
          page,
          form,
        }: {
          form: Record<string, any>;
          page: { currentPage: number; pageSize: number };
        }) => {
          return await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...form,
          });
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
    },
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// ==================== 操作方法 ====================

// 状态颜色
function getStatusColor(status: string) {
  switch (status) {
    case 'APPROVED': {
      return 'orange';
    }
    case 'CANCELLED': {
      return 'default';
    }
    case 'CONFIRMED': {
      return 'success';
    }
    case 'PENDING_APPROVAL': {
      return 'processing';
    }
    case 'REJECTED': {
      return 'error';
    }
    default: {
      return 'default';
    }
  }
}

// 新建交接
function handleCreate() {
  createModalRef.value?.open();
}

// 查看详情
function handleViewDetail(row: DataHandoverDTO) {
  detailModalRef.value?.open(row);
}

// 确认交接
function handleConfirm(row: DataHandoverDTO) {
  Modal.confirm({
    title: '确认交接',
    content: `确定要执行交接单「${row.handoverNo}」吗？确认后将立即执行数据迁移。`,
    okText: '确认执行',
    cancelText: '取消',
    okType: 'primary',
    onOk: async () => {
      try {
        await confirmHandover(row.id);
        message.success('交接已完成');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '确认失败');
      }
    },
  });
}

// 取消交接
function handleCancel(row: DataHandoverDTO) {
  Modal.confirm({
    title: '取消交接',
    content: `确定要取消交接单「${row.handoverNo}」吗？`,
    okText: '确认取消',
    cancelText: '返回',
    okType: 'danger',
    onOk: async () => {
      try {
        await cancelHandover(row.id, '用户取消');
        message.success('已取消');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '取消失败');
      }
    },
  });
}

// 弹窗成功回调
function handleModalSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page
    title="数据交接"
    description="管理用户离职交接和项目移交"
  >
    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Button type="primary" @click="handleCreate">新建交接</Button>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName }}
        </Tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleViewDetail(row)">详情</a>
          <template v-if="row.status === 'PENDING_APPROVAL'">
            <span style="color: #999">等待审批</span>
            <a style="color: #ff4d4f" @click="handleCancel(row)">取消</a>
          </template>
          <template v-else-if="row.status === 'APPROVED'">
            <a style="color: #52c41a" @click="handleConfirm(row)">确认执行</a>
            <a style="color: #ff4d4f" @click="handleCancel(row)">取消</a>
          </template>
        </Space>
      </template>
    </Grid>

    <!-- 新建交接弹窗 -->
    <CreateHandoverModal ref="createModalRef" @success="handleModalSuccess" />

    <!-- 详情弹窗 -->
    <HandoverDetailModal ref="detailModalRef" @success="handleModalSuccess" />
  </Page>
</template>
