<script setup lang="ts">
import { ref, onMounted, h } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import { Card, Tabs, TabPane, Badge, Button, Space, Tag, Textarea } from 'ant-design-vue';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getPendingApprovals,
  getMyInitiatedApprovals,
  approveApproval,
  type ApprovalDTO,
} from '#/api/workbench';
import { requestClient } from '#/api/request';
import ApprovalDetailModal from './components/ApprovalDetailModal.vue';

defineOptions({ name: 'WorkbenchApproval' });

// ==================== 状态定义 ====================

const activeTab = ref<'pending' | 'initiated'>('pending');
const detailModalRef = ref<InstanceType<typeof ApprovalDetailModal>>();
const pendingCount = ref(0);
const initiatedCount = ref(0);

// 状态颜色映射
const statusColorMap: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
  CANCELLED: 'default',
};

// 优先级颜色映射
const priorityColorMap: Record<string, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'default',
};

// ==================== 待审批表格配置 ====================

const pendingColumns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 50 },
  { title: '审批编号', field: 'approvalNo', width: 180 },
  { title: '业务类型', field: 'businessTypeName', width: 120 },
  { title: '业务标题', field: 'businessTitle', width: 200, showOverflow: true },
  { title: '申请人', field: 'applicantName', width: 100 },
  { title: '优先级', field: 'priority', width: 80, slots: { default: 'priority' } },
  { title: '紧急程度', field: 'urgency', width: 100, slots: { default: 'urgency' } },
  { title: '申请时间', field: 'createdAt', width: 160 },
  { title: '操作', field: 'action', width: 180, fixed: 'right', slots: { default: 'action' } },
];

async function loadPendingData() {
  const data = await getPendingApprovals();
  pendingCount.value = data?.length || 0;
  return {
    items: data || [],
    total: data?.length || 0,
  };
}

const [PendingGrid, pendingGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: pendingColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: loadPendingData,
      },
    },
    pagerConfig: {
      enabled: false,
    },
    checkboxConfig: {
      highlight: true,
    },
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// ==================== 我发起的审批表格配置 ====================

const initiatedColumns: VxeGridProps['columns'] = [
  { title: '审批编号', field: 'approvalNo', width: 180 },
  { title: '业务类型', field: 'businessTypeName', width: 120 },
  { title: '业务标题', field: 'businessTitle', width: 200, showOverflow: true },
  { title: '审批人', field: 'approverName', width: 100 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '审批意见', field: 'comment', width: 150, showOverflow: true },
  { title: '申请时间', field: 'createdAt', width: 160 },
  { title: '审批时间', field: 'approvedAt', width: 160 },
  { title: '操作', field: 'action', width: 100, fixed: 'right', slots: { default: 'action' } },
];

async function loadInitiatedData() {
  const data = await getMyInitiatedApprovals();
  initiatedCount.value = data?.length || 0;
  return {
    items: data || [],
    total: data?.length || 0,
  };
}

const [InitiatedGrid, initiatedGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: initiatedColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: loadInitiatedData,
      },
    },
    pagerConfig: {
      enabled: false,
    },
  },
});

// ==================== 操作方法 ====================

// Tab 切换
function handleTabChange(key: string | number) {
  activeTab.value = String(key) as 'pending' | 'initiated';
}

// 查看详情
function handleViewDetail(row: ApprovalDTO, isPending: boolean = false) {
  detailModalRef.value?.open(row, isPending);
}

// 审批通过
function handleApprove(row: ApprovalDTO) {
  Modal.confirm({
    title: '确认通过',
    content: `确定要通过「${row.businessTitle}」的审批吗？`,
    okText: '通过',
    okType: 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await approveApproval({
          approvalId: row.id,
          result: 'APPROVED',
          comment: '',
        });
        message.success('审批通过');
        pendingGridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '审批失败');
      }
    },
  });
}

// 审批拒绝
function handleReject(row: ApprovalDTO) {
  const rejectReason = ref('');
  
  Modal.confirm({
    title: '拒绝审批',
    content: () => h('div', {}, [
      h('p', { style: 'margin-bottom: 8px;' }, '请填写拒绝原因（必填）：'),
      h(Textarea, {
        value: rejectReason.value,
        'onUpdate:value': (val: string) => { rejectReason.value = val; },
        placeholder: '请输入拒绝原因',
        rows: 3,
        maxlength: 500,
        showCount: true,
      }),
    ]),
    okText: '拒绝',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      if (!rejectReason.value.trim()) {
        message.warning('请填写拒绝原因');
        return Promise.reject();
      }
      try {
        await approveApproval({
          approvalId: row.id,
          result: 'REJECTED',
          comment: rejectReason.value.trim(),
        });
        message.success('已拒绝');
        pendingGridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '操作失败');
        return Promise.reject();
      }
    },
  });
}

// 批量审批通过
async function handleBatchApprove() {
  const records = pendingGridApi.grid?.getCheckboxRecords() || [];
  if (records.length === 0) {
    message.warning('请先选择要审批的记录');
    return;
  }
  
  Modal.confirm({
    title: '批量通过',
    content: `确定要批量通过选中的 ${records.length} 条审批吗？`,
    okText: '确定',
    okType: 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await requestClient.post('/workbench/approval/batch-approve', {
          approvalIds: records.map((r: ApprovalDTO) => r.id),
          result: 'APPROVED',
        });
        message.success('批量审批成功');
        pendingGridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '批量审批失败');
      }
    },
  });
}

// 批量拒绝
async function handleBatchReject() {
  const records = pendingGridApi.grid?.getCheckboxRecords() || [];
  if (records.length === 0) {
    message.warning('请先选择要拒绝的记录');
    return;
  }
  
  const rejectReason = ref('');
  
  Modal.confirm({
    title: '批量拒绝',
    content: () => h('div', {}, [
      h('p', { style: 'margin-bottom: 8px;' }, `确定要批量拒绝选中的 ${records.length} 条审批吗？`),
      h('p', { style: 'margin-bottom: 8px;' }, '请填写拒绝原因（必填）：'),
      h(Textarea, {
        value: rejectReason.value,
        'onUpdate:value': (val: string) => { rejectReason.value = val; },
        placeholder: '请输入拒绝原因',
        rows: 3,
        maxlength: 500,
        showCount: true,
      }),
    ]),
    okText: '拒绝',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      if (!rejectReason.value.trim()) {
        message.warning('请填写拒绝原因');
        return Promise.reject();
      }
      try {
        await requestClient.post('/workbench/approval/batch-approve', {
          approvalIds: records.map((r: ApprovalDTO) => r.id),
          result: 'REJECTED',
          comment: rejectReason.value.trim(),
        });
        message.success('批量拒绝成功');
        pendingGridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '批量操作失败');
        return Promise.reject();
      }
    },
  });
}

// 弹窗成功回调
function handleModalSuccess() {
  if (activeTab.value === 'pending') {
    pendingGridApi.reload();
  } else {
    initiatedGridApi.reload();
  }
}

// 初始化
onMounted(() => {
  // 加载两个列表的数量
  loadPendingData();
  loadInitiatedData();
});
</script>

<template>
  <Page title="审批中心" description="集中管理所有业务审批，快速处理待审批事项" auto-content-height>
    <Card :bordered="false">
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <TabPane key="pending">
          <template #tab>
            <Badge :count="pendingCount" :offset="[10, 0]">
              <span>待我审批</span>
            </Badge>
          </template>
          
          <PendingGrid>
            <!-- 工具栏按钮 -->
            <template #toolbar-buttons>
              <Space>
                <Button type="primary" @click="handleBatchApprove">批量通过</Button>
                <Button danger @click="handleBatchReject">批量拒绝</Button>
              </Space>
            </template>

            <!-- 优先级列 -->
            <template #priority="{ row }">
              <Tag :color="priorityColorMap[row.priority] || 'default'">
                {{ row.priorityName || row.priority }}
              </Tag>
            </template>

            <!-- 紧急程度列 -->
            <template #urgency="{ row }">
              <Tag v-if="row.urgency === 'URGENT'" color="red">紧急</Tag>
              <span v-else>{{ row.urgencyName || '普通' }}</span>
            </template>

            <!-- 操作列 -->
            <template #action="{ row }">
              <Space>
                <a @click="handleViewDetail(row, true)">详情</a>
                <a style="color: #52c41a" @click="handleApprove(row)">通过</a>
                <a style="color: #ff4d4f" @click="handleReject(row)">拒绝</a>
              </Space>
            </template>
          </PendingGrid>
        </TabPane>

        <TabPane key="initiated">
          <template #tab>
            <Badge :count="initiatedCount" :offset="[10, 0]" :showZero="false">
              <span>我发起的</span>
            </Badge>
          </template>
          
          <InitiatedGrid>
            <!-- 状态列 -->
            <template #status="{ row }">
              <Tag :color="statusColorMap[row.status] || 'default'">
                {{ row.statusName || row.status }}
              </Tag>
            </template>

            <!-- 操作列 -->
            <template #action="{ row }">
              <a @click="handleViewDetail(row, false)">查看</a>
            </template>
          </InitiatedGrid>
        </TabPane>
      </Tabs>
    </Card>

    <!-- 审批详情弹窗 -->
    <ApprovalDetailModal ref="detailModalRef" @success="handleModalSuccess" />
  </Page>
</template>

<style scoped>
:deep(.ant-badge-count) {
  min-width: 18px;
  height: 18px;
  font-size: 12px;
  line-height: 18px;
}
</style>
