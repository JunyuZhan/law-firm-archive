<script setup lang="ts">
import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { Tag } from 'ant-design-vue';
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getOperationLogList } from '#/api/system';
import type { OperationLogDTO } from '#/api/system/types';
import LogDetailModal from './components/LogDetailModal.vue';

defineOptions({ name: 'SystemLog' });

// ==================== 状态定义 ====================

const logDetailModalRef = ref<InstanceType<typeof LogDetailModal>>();

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'module',
    label: '模块',
    component: 'Input',
    componentProps: {
      placeholder: '请输入模块',
      allowClear: true,
    },
  },
  {
    fieldName: 'operatorName',
    label: '操作人',
    component: 'Input',
    componentProps: {
      placeholder: '请输入操作人',
      allowClear: true,
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
        { label: '成功', value: 'SUCCESS' },
        { label: '失败', value: 'FAILED' },
      ],
    },
  },
  {
    fieldName: 'startTime',
    label: '开始时间',
    component: 'DatePicker',
    componentProps: {
      placeholder: '开始时间',
      showTime: true,
      format: 'YYYY-MM-DD HH:mm:ss',
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      style: { width: '100%' },
    },
  },
  {
    fieldName: 'endTime',
    label: '结束时间',
    component: 'DatePicker',
    componentProps: {
      placeholder: '结束时间',
      showTime: true,
      format: 'YYYY-MM-DD HH:mm:ss',
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      style: { width: '100%' },
    },
  },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '模块', field: 'module', width: 120 },
  { title: '操作', field: 'action', width: 120 },
  { title: '操作人', field: 'operatorName', width: 100 },
  { title: '操作IP', field: 'operatorIp', width: 130 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '耗时(ms)', field: 'duration', width: 100 },
  { title: '操作时间', field: 'operationTime', width: 180 },
  { title: '操作', field: 'action_btn', width: 80, fixed: 'right', slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: { page: number; pageSize: number } & Record<string, any>) {
  const res = await getOperationLogList({
    pageNum: params.page,
    pageSize: params.pageSize,
    module: params.module,
    operatorName: params.operatorName,
    status: params.status,
    startTime: params.startTime,
    endTime: params.endTime,
  });
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    showCollapseButton: true,
    submitButtonOptions: { content: '查询' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page, form }) => {
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
  },
});

// ==================== 操作方法 ====================

function handleDetail(row: OperationLogDTO) {
  logDetailModalRef.value?.open(row);
}
</script>

<template>
  <Page title="操作日志" description="查看系统操作日志" auto-content-height>
    <Grid>
      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="row.status === 'SUCCESS' ? 'green' : 'red'">
          {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
        </Tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <a @click="handleDetail(row)">详情</a>
      </template>
    </Grid>

    <!-- 日志详情弹窗 -->
    <LogDetailModal ref="logDetailModalRef" />
  </Page>
</template>
