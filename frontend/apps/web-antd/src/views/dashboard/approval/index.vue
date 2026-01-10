<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import { Card, Tabs, TabPane, Badge, Button, Space, Tag, Textarea } from 'ant-design-vue';
import type { Key } from 'ant-design-vue/es/table/interface';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getPendingApprovals,
  getMyInitiatedApprovals,
  getMyApprovedHistory,
  approveApproval,
  type ApprovalDTO,
} from '#/api/workbench';
import { requestClient } from '#/api/request';
import ApprovalDetailModal from '#/views/workbench/approval/components/ApprovalDetailModal.vue';

defineOptions({ name: 'DashboardApproval' });

// ==================== 状态定义 ====================

const activeTab = ref<'pending' | 'initiated' | 'history'>('pending');
const activeCategory = ref<string>('all');
const detailModalRef = ref<InstanceType<typeof ApprovalDetailModal>>();

// 所有数据（用于分类统计）
const allPendingData = ref<ApprovalDTO[]>([]);
const allInitiatedData = ref<ApprovalDTO[]>([]);
const allHistoryData = ref<ApprovalDTO[]>([]);

// 业务类别定义
const businessCategories = [
  { key: 'all', name: '全部', types: [] as string[] },
  { key: 'project', name: '项目业务', types: ['CONTRACT', 'MATTER_CLOSE'] },
  { key: 'finance', name: '财务业务', types: ['EXPENSE', 'PAYMENT_AMENDMENT'] },
  { key: 'admin', name: '行政业务', types: ['SEAL_APPLICATION'] },
  { key: 'risk', name: '风控业务', types: ['CONFLICT_CHECK', 'CONFLICT_EXEMPTION'] },
  { key: 'hr', name: '人事业务', types: ['REGULARIZATION', 'RESIGNATION'] },
];

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

// ==================== 分类统计 ====================

function getCategoryCount(data: ApprovalDTO[], categoryKey: string): number {
  if (categoryKey === 'all') return data.length;
  const category = businessCategories.find(c => c.key === categoryKey);
  if (!category) return 0;
  return data.filter(item => category.types.includes(item.businessType)).length;
}

const pendingStats = computed(() => {
  return businessCategories.map(cat => ({
    ...cat,
    count: getCategoryCount(allPendingData.value, cat.key),
  }));
});

const initiatedStats = computed(() => {
  return businessCategories.map(cat => ({
    ...cat,
    count: getCategoryCount(allInitiatedData.value, cat.key),
  }));
});

const historyStats = computed(() => {
  return businessCategories.map(cat => ({
    ...cat,
    count: getCategoryCount(allHistoryData.value, cat.key),
  }));
});

// ==================== 待审批表格配置 ====================

const pendingColumns = [
  { type: 'checkbox' as const, width: 50 },
  { title: '业务类型', field: 'businessTypeName', width: 100 },
  { title: '业务标题', field: 'businessTitle', minWidth: 200, showOverflow: true },
  { title: '申请人', field: 'applicantName', width: 100 },
  { title: '优先级', field: 'priority', width: 80, slots: { default: 'priority' } },
  { title: '紧急程度', field: 'urgency', width: 80, slots: { default: 'urgency' } },
  { title: '申请时间', field: 'createdAt', width: 150 },
  { title: '操作', field: 'action', width: 160, fixed: 'right' as const, slots: { default: 'action' } },
];

async function loadPendingData() {
    const data = await getPendingApprovals();
  allPendingData.value = data || [];
  
  // 根据分类过滤
  let filteredData = data || [];
  if (activeCategory.value !== 'all') {
    const category = businessCategories.find(c => c.key === activeCategory.value);
    if (category) {
      filteredData = filteredData.filter(item => category.types.includes(item.businessType));
    }
  }
  
  return {
    items: filteredData,
    total: filteredData.length,
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
      enabled: true,
      pageSize: 10,
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

const initiatedColumns = [
  { title: '业务类型', field: 'businessTypeName', width: 100 },
  { title: '业务标题', field: 'businessTitle', minWidth: 200, showOverflow: true },
  { title: '审批人', field: 'approverName', width: 100 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '审批意见', field: 'comment', width: 150, showOverflow: true },
  { title: '申请时间', field: 'createdAt', width: 150 },
  { title: '审批时间', field: 'approvedAt', width: 150 },
  { title: '操作', field: 'action', width: 80, fixed: 'right' as const, slots: { default: 'action' } },
];

async function loadInitiatedData() {
  const data = await getMyInitiatedApprovals();
  allInitiatedData.value = data || [];
  
  let filteredData = data || [];
  if (activeCategory.value !== 'all') {
    const category = businessCategories.find(c => c.key === activeCategory.value);
    if (category) {
      filteredData = filteredData.filter(item => category.types.includes(item.businessType));
    }
  }
  
  return {
    items: filteredData,
    total: filteredData.length,
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
      enabled: true,
      pageSize: 10,
    },
  },
});

// ==================== 审批历史表格配置 ====================

const historyColumns = [
  { title: '业务类型', field: 'businessTypeName', width: 100 },
  { title: '业务标题', field: 'businessTitle', minWidth: 200, showOverflow: true },
  { title: '申请人', field: 'applicantName', width: 100 },
  { title: '结果', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '审批意见', field: 'comment', width: 150, showOverflow: true },
  { title: '审批时间', field: 'approvedAt', width: 150 },
  { title: '操作', field: 'action', width: 80, fixed: 'right' as const, slots: { default: 'action' } },
];

async function loadHistoryData() {
  const data = await getMyApprovedHistory();
  allHistoryData.value = data || [];
  
  let filteredData = data || [];
  if (activeCategory.value !== 'all') {
    const category = businessCategories.find(c => c.key === activeCategory.value);
    if (category) {
      filteredData = filteredData.filter(item => category.types.includes(item.businessType));
    }
  }
  
  return {
    items: filteredData,
    total: filteredData.length,
  };
}

const [HistoryGrid, historyGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: historyColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: loadHistoryData,
      },
    },
    pagerConfig: {
      enabled: true,
      pageSize: 10,
    },
  },
});

// ==================== 操作方法 ====================

// 主 Tab 切换
function handleTabChange(key: Key) {
  activeTab.value = String(key) as 'pending' | 'initiated' | 'history';
  activeCategory.value = 'all';
}

// 分类切换
function handleCategoryChange(key: Key) {
  activeCategory.value = String(key);
  // 刷新当前表格
  if (activeTab.value === 'pending') {
    pendingGridApi.reload();
  } else if (activeTab.value === 'initiated') {
    initiatedGridApi.reload();
  } else {
    historyGridApi.reload();
  }
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
  } else if (activeTab.value === 'initiated') {
    initiatedGridApi.reload();
  } else {
    historyGridApi.reload();
}
}

// 获取当前统计数据
const currentStats = computed(() => {
  if (activeTab.value === 'pending') return pendingStats.value;
  if (activeTab.value === 'initiated') return initiatedStats.value;
  return historyStats.value;
});

// 待审批总数（仅待审批需要红点提醒）
const pendingCount = computed(() => allPendingData.value.length);

// 初始化
onMounted(async () => {
  // 预加载所有数据用于统计
  try {
    const [pending, initiated, history] = await Promise.all([
      getPendingApprovals(),
      getMyInitiatedApprovals(),
      getMyApprovedHistory(),
    ]);
    allPendingData.value = pending || [];
    allInitiatedData.value = initiated || [];
    allHistoryData.value = history || [];
  } catch {
    // 静默失败，表格会自己加载
  }
});
</script>

<template>
  <Page title="审批中心" description="集中管理所有业务审批，快速处理待审批事项" auto-content-height>
    <Card :bordered="false">
      <!-- 主 Tab：待我审批 / 我发起的 / 审批历史 -->
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <TabPane key="pending">
          <template #tab>
            <Badge :count="pendingCount" :offset="[10, 0]">
              <span>待我审批</span>
            </Badge>
          </template>
        </TabPane>
        <TabPane key="initiated" tab="我发起的" />
        <TabPane key="history" tab="审批历史" />
      </Tabs>

      <!-- 分类子 Tab（仅待审批显示数量 Badge） -->
      <Tabs 
        v-model:activeKey="activeCategory" 
        size="small" 
        type="card"
        @change="handleCategoryChange"
        style="margin-bottom: 16px;"
      >
        <TabPane v-for="cat in currentStats" :key="cat.key">
          <template #tab>
            <template v-if="activeTab === 'pending'">
              <Badge :count="cat.count" :offset="[8, 0]" :showZero="cat.key === 'all'" size="small">
                <span>{{ cat.name }}</span>
            </Badge>
          </template>
            <span v-else>{{ cat.name }}</span>
          </template>
        </TabPane>
      </Tabs>

      <!-- 待审批列表 -->
      <PendingGrid v-if="activeTab === 'pending'">
        <template #toolbar-buttons>
          <Space>
            <Button type="primary" @click="handleBatchApprove">批量通过</Button>
            <Button danger @click="handleBatchReject">批量拒绝</Button>
          </Space>
        </template>

        <template #priority="{ row }">
          <Tag :color="priorityColorMap[row.priority] || 'default'">
            {{ row.priorityName || row.priority }}
            </Tag>
          </template>

        <template #urgency="{ row }">
          <Tag v-if="row.urgency === 'URGENT'" color="red">紧急</Tag>
          <span v-else>{{ row.urgencyName || '普通' }}</span>
          </template>

        <template #action="{ row }">
            <Space>
            <a @click="handleViewDetail(row, true)">详情</a>
            <a style="color: #52c41a" @click="handleApprove(row)">通过</a>
            <a style="color: #ff4d4f" @click="handleReject(row)">拒绝</a>
            </Space>
        </template>
      </PendingGrid>

      <!-- 我发起的审批列表 -->
      <InitiatedGrid v-if="activeTab === 'initiated'">
        <template #status="{ row }">
          <Tag :color="statusColorMap[row.status] || 'default'">
            {{ row.statusName || row.status }}
            </Tag>
          </template>

        <template #action="{ row }">
          <a @click="handleViewDetail(row, false)">查看</a>
        </template>
      </InitiatedGrid>

      <!-- 审批历史列表 -->
      <HistoryGrid v-if="activeTab === 'history'">
        <template #status="{ row }">
          <Tag :color="statusColorMap[row.status] || 'default'">
            {{ row.statusName || row.status }}
            </Tag>
          </template>

        <template #action="{ row }">
          <a @click="handleViewDetail(row, false)">查看</a>
        </template>
      </HistoryGrid>
    </Card>

    <!-- 审批详情弹窗（复用 workbench 的组件） -->
    <ApprovalDetailModal ref="detailModalRef" @success="handleModalSuccess" />
  </Page>
</template>

<style scoped>
:deep(.ant-badge-count) {
  font-size: 12px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
}

:deep(.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  padding: 4px 12px;
}
</style>
