<script setup lang="ts">
import type { Key } from 'ant-design-vue/es/table/interface';

import type { ApprovalDTO } from '#/api/workbench';

import { computed, h, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  TabPane,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { requestClient } from '#/api/request';
import {
  approveApproval,
  getMyApprovedHistory,
  getMyInitiatedApprovals,
  getPendingApprovals,
} from '#/api/workbench';
import ApprovalDetailModal from '#/views/workbench/approval/components/ApprovalDetailModal.vue';

defineOptions({ name: 'DashboardApproval' });

// ==================== 状态定义 ====================

const activeTab = ref<'history' | 'initiated' | 'pending'>('pending');
const activeCategory = ref<string>('all');
const detailModalRef = ref<InstanceType<typeof ApprovalDetailModal>>();

// 筛选条件
const filterParams = ref({
  keyword: '',
  businessType: undefined as string | undefined,
  priority: undefined as string | undefined,
  urgency: undefined as string | undefined,
  dateRange: [] as any[],
});

// 所有数据（用于分类统计）
const allPendingData = ref<ApprovalDTO[]>([]);
const allInitiatedData = ref<ApprovalDTO[]>([]);
const allHistoryData = ref<ApprovalDTO[]>([]);

// 业务类别定义
const businessCategories = [
  { key: 'all', name: '全部', types: [] as string[] },
  { key: 'project', name: '项目业务', types: ['CONTRACT', 'MATTER_CLOSE'] },
  { key: 'finance', name: '财务业务', types: ['EXPENSE', 'PAYMENT_AMENDMENT'] },
  {
    key: 'admin',
    name: '行政业务',
    types: ['SEAL_APPLICATION', 'LETTER_APPLICATION'],
  },
  {
    key: 'risk',
    name: '风控业务',
    types: ['CONFLICT_CHECK', 'CONFLICT_EXEMPTION'],
  },
  { key: 'hr', name: '人事业务', types: ['REGULARIZATION', 'RESIGNATION'] },
];

// 业务类型选项（用于下拉筛选）
const businessTypeOptions = [
  { label: '合同审批', value: 'CONTRACT' },
  { label: '项目结案', value: 'MATTER_CLOSE' },
  { label: '报销审批', value: 'EXPENSE' },
  { label: '付款修正', value: 'PAYMENT_AMENDMENT' },
  { label: '用印申请', value: 'SEAL_APPLICATION' },
  { label: '出函申请', value: 'LETTER_APPLICATION' },
  { label: '利冲检索', value: 'CONFLICT_CHECK' },
  { label: '利冲豁免', value: 'CONFLICT_EXEMPTION' },
  { label: '转正申请', value: 'REGULARIZATION' },
  { label: '离职申请', value: 'RESIGNATION' },
];

// 优先级选项
const priorityOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];

// 紧急程度选项
const urgencyOptions = [
  { label: '紧急', value: 'URGENT' },
  { label: '普通', value: 'NORMAL' },
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
  const category = businessCategories.find((c) => c.key === categoryKey);
  if (!category) return 0;
  return data.filter((item) => category.types.includes(item.businessType))
    .length;
}

const pendingStats = computed(() => {
  return businessCategories.map((cat) => ({
    ...cat,
    count: getCategoryCount(allPendingData.value, cat.key),
  }));
});

const initiatedStats = computed(() => {
  return businessCategories.map((cat) => ({
    ...cat,
    count: getCategoryCount(allInitiatedData.value, cat.key),
  }));
});

const historyStats = computed(() => {
  return businessCategories.map((cat) => ({
    ...cat,
    count: getCategoryCount(allHistoryData.value, cat.key),
  }));
});

// ==================== 待审批表格配置 ====================

const pendingColumns = [
  { type: 'checkbox' as const, width: 50 },
  { title: '业务类型', field: 'businessTypeName', width: 100 },
  {
    title: '业务标题',
    field: 'businessTitle',
    minWidth: 200,
    showOverflow: true,
  },
  { title: '申请人', field: 'applicantName', width: 100 },
  {
    title: '优先级',
    field: 'priority',
    width: 80,
    slots: { default: 'priority' },
  },
  {
    title: '紧急程度',
    field: 'urgency',
    width: 80,
    slots: { default: 'urgency' },
  },
  { title: '申请时间', field: 'createdAt', width: 150 },
  {
    title: '操作',
    field: 'action',
    width: 160,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

// 通用筛选函数
function applyFilters(data: ApprovalDTO[]): ApprovalDTO[] {
  let filteredData = data;

  // 关键字搜索（业务标题、审批编号、申请人）
  if (filterParams.value.keyword) {
    const keyword = filterParams.value.keyword.toLowerCase();
    filteredData = filteredData.filter(
      (item) =>
        item.businessTitle?.toLowerCase().includes(keyword) ||
        item.approvalNo?.toLowerCase().includes(keyword) ||
        item.applicantName?.toLowerCase().includes(keyword),
    );
  }

  // 业务类型筛选
  if (filterParams.value.businessType) {
    filteredData = filteredData.filter(
      (item) => item.businessType === filterParams.value.businessType,
    );
  }

  // 优先级筛选
  if (filterParams.value.priority) {
    filteredData = filteredData.filter(
      (item) => item.priority === filterParams.value.priority,
    );
  }

  // 紧急程度筛选
  if (filterParams.value.urgency) {
    filteredData = filteredData.filter(
      (item) => item.urgency === filterParams.value.urgency,
    );
  }

  // 时间范围筛选
  if (filterParams.value.dateRange?.length === 2) {
    const [start, end] = filterParams.value.dateRange;
    if (start && end) {
      const startDate = dayjs(start).startOf('day');
      const endDate = dayjs(end).endOf('day');
      filteredData = filteredData.filter((item) => {
        const itemDate = dayjs(item.createdAt);
        return itemDate.isAfter(startDate) && itemDate.isBefore(endDate);
      });
    }
  }

  // 分类筛选
  if (activeCategory.value !== 'all') {
    const category = businessCategories.find(
      (c) => c.key === activeCategory.value,
    );
    if (category) {
      filteredData = filteredData.filter((item) =>
        category.types.includes(item.businessType),
      );
    }
  }

  return filteredData;
}

async function loadPendingData() {
  const data = await getPendingApprovals();
  allPendingData.value = data || [];

  const filteredData = applyFilters(data || []);

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
  {
    title: '业务标题',
    field: 'businessTitle',
    minWidth: 200,
    showOverflow: true,
  },
  { title: '审批人', field: 'approverName', width: 100 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '审批意见', field: 'comment', width: 150, showOverflow: true },
  { title: '申请时间', field: 'createdAt', width: 150 },
  { title: '审批时间', field: 'approvedAt', width: 150 },
  {
    title: '操作',
    field: 'action',
    width: 80,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

async function loadInitiatedData() {
  const data = await getMyInitiatedApprovals();
  allInitiatedData.value = data || [];

  const filteredData = applyFilters(data || []);

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
  {
    title: '业务标题',
    field: 'businessTitle',
    minWidth: 200,
    showOverflow: true,
  },
  { title: '申请人', field: 'applicantName', width: 100 },
  { title: '结果', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '审批意见', field: 'comment', width: 150, showOverflow: true },
  { title: '审批时间', field: 'approvedAt', width: 150 },
  {
    title: '操作',
    field: 'action',
    width: 80,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

async function loadHistoryData() {
  const data = await getMyApprovedHistory();
  allHistoryData.value = data || [];

  const filteredData = applyFilters(data || []);

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
  activeTab.value = String(key) as 'history' | 'initiated' | 'pending';
  activeCategory.value = 'all';
}

// 分类切换
function handleCategoryChange(key: Key) {
  activeCategory.value = String(key);
  // 刷新当前表格
  reloadCurrentGrid();
}

// 重新加载当前表格
function reloadCurrentGrid() {
  if (activeTab.value === 'pending') {
    pendingGridApi.reload();
  } else if (activeTab.value === 'initiated') {
    initiatedGridApi.reload();
  } else {
    historyGridApi.reload();
  }
}

// 筛选搜索
function handleSearch() {
  reloadCurrentGrid();
}

// 重置筛选
function handleReset() {
  filterParams.value = {
    keyword: '',
    businessType: undefined,
    priority: undefined,
    urgency: undefined,
    dateRange: [],
  };
  activeCategory.value = 'all';
  reloadCurrentGrid();
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
    content: () =>
      h('div', {}, [
        h('p', { style: 'margin-bottom: 8px;' }, '请填写拒绝原因（必填）：'),
        h(Textarea, {
          value: rejectReason.value,
          'onUpdate:value': (val: string) => {
            rejectReason.value = val;
          },
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
        throw undefined;
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
    content: () =>
      h('div', {}, [
        h(
          'p',
          { style: 'margin-bottom: 8px;' },
          `确定要批量拒绝选中的 ${records.length} 条审批吗？`,
        ),
        h('p', { style: 'margin-bottom: 8px;' }, '请填写拒绝原因（必填）：'),
        h(Textarea, {
          value: rejectReason.value,
          'onUpdate:value': (val: string) => {
            rejectReason.value = val;
          },
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
        throw undefined;
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
  <Page
    title="审批中心"
    description="集中管理所有业务审批，快速处理待审批事项"
  >
    <Card :bordered="false">
      <!-- 筛选区域 -->
      <div class="filter-section">
        <Row :gutter="[12, 12]" align="middle">
          <Col :xs="24" :sm="12" :md="6" :lg="5" :xl="4">
            <Input
              v-model:value="filterParams.keyword"
              placeholder="搜索标题/编号/申请人"
              allow-clear
              @press-enter="handleSearch"
            >
              <template #prefix>
                <span style="color: #999">🔍</span>
              </template>
            </Input>
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="4" :xl="3">
            <Select
              v-model:value="filterParams.businessType"
              placeholder="业务类型"
              allow-clear
              style="width: 100%"
              :options="businessTypeOptions"
              @change="handleSearch"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3" :xl="3">
            <Select
              v-model:value="filterParams.priority"
              placeholder="优先级"
              allow-clear
              style="width: 100%"
              :options="priorityOptions"
              @change="handleSearch"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3" :xl="3">
            <Select
              v-model:value="filterParams.urgency"
              placeholder="紧急程度"
              allow-clear
              style="width: 100%"
              :options="urgencyOptions"
              @change="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="5" :xl="5">
            <DatePicker.RangePicker
              v-model:value="filterParams.dateRange"
              style="width: 100%"
              :placeholder="['开始日期', '结束日期']"
              @change="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="4" :xl="6">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 主 Tab：待我审批 / 我发起的 / 审批历史 -->
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
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
        v-model:active-key="activeCategory"
        size="small"
        type="card"
        @change="handleCategoryChange"
        style="margin-bottom: 16px"
      >
        <TabPane v-for="cat in currentStats" :key="cat.key">
          <template #tab>
            <template v-if="activeTab === 'pending'">
              <Badge
                :count="cat.count"
                :offset="[8, 0]"
                :show-zero="cat.key === 'all'"
                size="small"
              >
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
.filter-section {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

:deep(.ant-badge-count) {
  min-width: 18px;
  height: 18px;
  font-size: 12px;
  line-height: 18px;
}

:deep(.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  padding: 4px 12px;
}
</style>
