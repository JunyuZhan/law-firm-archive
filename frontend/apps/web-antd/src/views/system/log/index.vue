<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { OperationLogDTO } from '#/api/system/types';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { DeleteOutlined, DownloadOutlined } from '@vben/icons';

import {
  Button,
  Card,
  Col,
  message,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  cleanOldLogs,
  exportOperationLog,
  getLogStatistics,
  getOperationLogList,
} from '#/api/system';

import LogDetailModal from './components/LogDetailModal.vue';

defineOptions({ name: 'SystemLog' });

// ==================== 状态定义 ====================

const logDetailModalRef = ref<InstanceType<typeof LogDetailModal>>();
const statistics = ref<Record<string, any>>({});
const statisticsLoading = ref(false);
const exportLoading = ref(false);
const cleanLoading = ref(false);
const currentQueryParams = ref<Record<string, any>>({});

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

const gridColumns: any[] = [
  { title: '模块', field: 'module', width: 120 },
  { title: '操作', field: 'action', width: 120 },
  { title: '操作人', field: 'operatorName', width: 100 },
  { title: '操作IP', field: 'operatorIp', width: 130 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '耗时(ms)', field: 'duration', width: 100 },
  { title: '操作时间', field: 'operationTime', width: 180 },
  {
    title: '操作',
    field: 'action_btn',
    width: 80,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  // 保存查询条件用于导出
  currentQueryParams.value = {
    module: params.module,
    operatorName: params.operatorName,
    status: params.status,
    startTime: params.startTime,
    endTime: params.endTime,
  };

  const res = await getOperationLogList({
    pageNum: params.page,
    pageSize: params.pageSize,
    ...currentQueryParams.value,
  });
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
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
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
    },
  },
});

// ==================== 操作方法 ====================

function handleDetail(row: OperationLogDTO) {
  logDetailModalRef.value?.open(row);
}

// 加载统计信息
async function loadStatistics() {
  statisticsLoading.value = true;
  try {
    const res = await getLogStatistics();
    statistics.value = res;
  } catch (error: any) {
    console.error('加载统计信息失败', error);
  } finally {
    statisticsLoading.value = false;
  }
}

// 导出日志
async function handleExport() {
  exportLoading.value = true;
  try {
    const res = await exportOperationLog(currentQueryParams.value);

    // 处理返回的 Blob
    let blob: Blob;
    if (res instanceof Blob) {
      blob = res;
    } else if (res?.data instanceof Blob) {
      blob = res.data;
    } else {
      throw new TypeError('导出失败：未知响应格式');
    }

    // 检查是否是错误响应（JSON格式）
    if (blob.type === 'application/json') {
      const text = await blob.text();
      const errorData = JSON.parse(text);
      throw new Error(errorData.message || '导出失败');
    }

    // 创建下载链接
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.xlsx`;
    document.body.append(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

    message.success('导出成功');
  } catch (error: any) {
    console.error('导出失败:', error);
    message.error(error?.message || '导出失败');
  } finally {
    exportLoading.value = false;
  }
}

// 清理历史日志
async function handleClean() {
  cleanLoading.value = true;
  try {
    await cleanOldLogs(90);
    message.success('清理成功');
    gridApi.reload();
    await loadStatistics();
  } catch (error: any) {
    message.error(error.message || '清理失败');
  } finally {
    cleanLoading.value = false;
  }
}

onMounted(() => {
  loadStatistics();
});
</script>

<template>
  <Page title="操作日志" description="查看系统操作日志">
    <!-- 统计卡片 -->
    <Card class="mb-4" :bordered="false">
      <Row :gutter="16">
        <Col :xs="24" :sm="12" :md="6">
          <Statistic
            title="总日志数"
            :value="statistics.totalCount || 0"
            :loading="statisticsLoading"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6">
          <Statistic
            title="成功"
            :value="statistics.successCount || 0"
            :value-style="{ color: '#3f8600' }"
            :loading="statisticsLoading"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6">
          <Statistic
            title="失败"
            :value="statistics.failCount || 0"
            :value-style="{ color: '#cf1322' }"
            :loading="statisticsLoading"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6">
          <Statistic
            title="平均耗时"
            :value="statistics.avgExecutionTime || 0"
            suffix="ms"
            :loading="statisticsLoading"
          />
        </Col>
      </Row>
    </Card>

    <!-- 操作栏 -->
    <Card class="mb-4" :bordered="false">
      <Space>
        <Button type="primary" :loading="exportLoading" @click="handleExport">
          <DownloadOutlined class="mr-1" /> 导出日志
        </Button>
        <Button :loading="statisticsLoading" @click="loadStatistics">
          刷新统计
        </Button>
        <Popconfirm
          title="确定要清理90天前的日志吗？"
          ok-text="确定"
          cancel-text="取消"
          @confirm="handleClean"
        >
          <Button danger :loading="cleanLoading">
            <DeleteOutlined class="mr-1" /> 清理历史日志
          </Button>
        </Popconfirm>
      </Space>
    </Card>

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
