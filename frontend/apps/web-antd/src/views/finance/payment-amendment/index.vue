<script setup lang="ts">
import { ref, reactive, onMounted, h, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Select,
  Row,
  Col,
  Tabs,
  TabPane,
  Statistic,
  Textarea,
  Badge,
  Descriptions,
  DescriptionsItem,
} from 'ant-design-vue';
import {
  getPaymentAmendmentList,
  getPaymentAmendmentDetail,
  approvePaymentAmendment,
  rejectPaymentAmendment,
  type PaymentAmendmentDTO,
} from '#/api/finance/payment-amendment';

defineOptions({ name: 'PaymentAmendment' });

// 状态
const loading = ref(false);
const activeTab = ref('all');
const amendments = ref<PaymentAmendmentDTO[]>([]);
const total = ref(0);

// 查询参数
const queryParams = reactive({
  status: undefined as string | undefined,
  pageNum: 1,
  pageSize: 15,
});

// 统计数据
const stats = computed(() => {
  const pending = amendments.value.filter(a => a.status === 'PENDING').length;
  const approved = amendments.value.filter(a => a.status === 'APPROVED').length;
  const rejected = amendments.value.filter(a => a.status === 'REJECTED').length;
  return { pending, approved, rejected, total: total.value };
});

// 表格列
const columns = [
  { title: '收款编号', dataIndex: 'paymentNo', key: 'paymentNo', width: 140 },
  { title: '原金额', dataIndex: 'originalAmount', key: 'originalAmount', width: 120 },
  { title: '新金额', dataIndex: 'newAmount', key: 'newAmount', width: 120 },
  { title: '差额', dataIndex: 'amountDiff', key: 'amountDiff', width: 100 },
  { title: '变更原因', dataIndex: 'reason', key: 'reason', width: 200, ellipsis: true },
  { title: '申请人', dataIndex: 'requestedByName', key: 'requestedByName', width: 100 },
  { title: '申请时间', dataIndex: 'requestedAt', key: 'requestedAt', width: 160 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '审批人', dataIndex: 'approvedByName', key: 'approvedByName', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getPaymentAmendmentList(queryParams);
    amendments.value = res.records || [];
    total.value = res.total || 0;
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
  } finally {
    loading.value = false;
  }
}

// Tab切换
function handleTabChange(key: string) {
  if (key === 'all') {
    queryParams.status = undefined;
  } else if (key === 'pending') {
    queryParams.status = 'PENDING';
  } else if (key === 'completed') {
    queryParams.status = undefined; // 需要在后端支持多状态查询
  }
  queryParams.pageNum = 1;
  fetchData();
}

// 分页变化
function handleTableChange(pagination: any) {
  queryParams.pageNum = pagination.current;
  queryParams.pageSize = pagination.pageSize;
  fetchData();
}

// 查看详情
async function handleView(record: PaymentAmendmentDTO) {
  try {
    const detail = await getPaymentAmendmentDetail(record.id);
    Modal.info({
      title: '变更申请详情',
      width: 600,
      content: h(Descriptions, { column: 2, bordered: true, size: 'small' }, () => [
        h(DescriptionsItem, { label: '收款编号' }, () => detail.paymentNo || '-'),
        h(DescriptionsItem, { label: '状态' }, () => h(Tag, { color: getStatusColor(detail.status) }, () => detail.statusName)),
        h(DescriptionsItem, { label: '原金额' }, () => `¥${detail.originalAmount?.toLocaleString()}`),
        h(DescriptionsItem, { label: '新金额' }, () => `¥${detail.newAmount?.toLocaleString()}`),
        h(DescriptionsItem, { label: '差额', span: 2 }, () => {
          const diff = detail.amountDiff;
          const color = diff > 0 ? '#52c41a' : diff < 0 ? '#f5222d' : '#000';
          return h('span', { style: { color } }, `${diff > 0 ? '+' : ''}¥${diff?.toLocaleString()}`);
        }),
        h(DescriptionsItem, { label: '变更原因', span: 2 }, () => detail.reason),
        h(DescriptionsItem, { label: '申请人' }, () => detail.requestedByName || '-'),
        h(DescriptionsItem, { label: '申请时间' }, () => detail.requestedAt || '-'),
        h(DescriptionsItem, { label: '审批人' }, () => detail.approvedByName || '-'),
        h(DescriptionsItem, { label: '审批时间' }, () => detail.approvedAt || '-'),
        detail.rejectReason ? h(DescriptionsItem, { label: '拒绝原因', span: 2 }, () => detail.rejectReason) : null,
      ].filter(Boolean)),
    });
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 审批通过
function handleApprove(record: PaymentAmendmentDTO) {
  Modal.confirm({
    title: '确认审批',
    content: `确定要批准收款变更申请吗？\n原金额：¥${record.originalAmount?.toLocaleString()}\n新金额：¥${record.newAmount?.toLocaleString()}`,
    okText: '确认批准',
    cancelText: '取消',
    onOk: async () => {
      try {
        await approvePaymentAmendment(record.id);
        message.success('审批通过');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '审批失败');
      }
    },
  });
}

// 审批拒绝
function handleReject(record: PaymentAmendmentDTO) {
  const rejectReasonRef = ref<string>('');
  
  Modal.confirm({
    title: '拒绝变更申请',
    width: 500,
    content: () => {
      return h('div', [
        h('p', { style: 'margin-bottom: 12px' }, `确定要拒绝收款变更申请吗？`),
        h('p', { style: 'margin-bottom: 12px; color: #666' }, 
          `原金额：¥${record.originalAmount?.toLocaleString()} → 新金额：¥${record.newAmount?.toLocaleString()}`),
        h(Textarea, {
          value: rejectReasonRef.value,
          placeholder: '请输入拒绝原因（必填）',
          rows: 4,
          'onUpdate:value': (value: string) => {
            rejectReasonRef.value = value;
          },
        }),
      ]);
    },
    okText: '确认拒绝',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!rejectReasonRef.value?.trim()) {
        message.error('请输入拒绝原因');
        return Promise.reject();
      }
      try {
        await rejectPaymentAmendment(record.id, rejectReasonRef.value.trim());
        message.success('已拒绝');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
  };
  return colorMap[status] || 'default';
}

// 格式化金额
function formatAmount(amount: number) {
  if (amount === null || amount === undefined) return '-';
  return `¥${amount.toLocaleString()}`;
}

// 格式化差额
function formatDiff(diff: number) {
  if (diff === null || diff === undefined) return '-';
  const prefix = diff > 0 ? '+' : '';
  const color = diff > 0 ? '#52c41a' : diff < 0 ? '#f5222d' : '#000';
  return { text: `${prefix}¥${diff.toLocaleString()}`, color };
}

// 获取当前数据源（根据tab过滤）
const currentDataSource = computed(() => {
  if (activeTab.value === 'pending') {
    return amendments.value.filter(a => a.status === 'PENDING');
  }
  if (activeTab.value === 'completed') {
    return amendments.value.filter(a => a.status === 'APPROVED' || a.status === 'REJECTED');
  }
  return amendments.value;
});

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="收款变更审批" description="审批已锁定收款记录的变更申请">
    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px;">
      <Col :span="6">
        <Card size="small">
          <Statistic title="待审批" :value="stats.pending" :value-style="{ color: '#faad14' }" />
        </Card>
      </Col>
      <Col :span="6">
        <Card size="small">
          <Statistic title="已批准" :value="stats.approved" :value-style="{ color: '#52c41a' }" />
        </Card>
      </Col>
      <Col :span="6">
        <Card size="small">
          <Statistic title="已拒绝" :value="stats.rejected" :value-style="{ color: '#f5222d' }" />
        </Card>
      </Col>
      <Col :span="6">
        <Card size="small">
          <Statistic title="总申请" :value="stats.total" />
        </Card>
      </Col>
    </Row>

    <Card>
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <TabPane key="all">
          <template #tab>
            <span>全部申请 <Badge :count="stats.total" :number-style="{ backgroundColor: '#999' }" /></span>
          </template>
        </TabPane>
        <TabPane key="pending">
          <template #tab>
            <span>待审批 <Badge :count="stats.pending" :number-style="{ backgroundColor: '#faad14' }" /></span>
          </template>
        </TabPane>
        <TabPane key="completed">
          <template #tab>
            <span>已处理 <Badge :count="stats.approved + stats.rejected" :number-style="{ backgroundColor: '#52c41a' }" /></span>
          </template>
        </TabPane>
      </Tabs>

      <Table
        :columns="columns"
        :data-source="currentDataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total: total,
          showTotal: (t: number) => `共 ${t} 条`,
          showSizeChanger: true,
        }"
        row-key="id"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'originalAmount'">
            {{ formatAmount((record as PaymentAmendmentDTO).originalAmount) }}
          </template>
          <template v-if="column.key === 'newAmount'">
            {{ formatAmount((record as PaymentAmendmentDTO).newAmount) }}
          </template>
          <template v-if="column.key === 'amountDiff'">
            <span :style="{ color: formatDiff((record as PaymentAmendmentDTO).amountDiff).color }">
              {{ formatDiff((record as PaymentAmendmentDTO).amountDiff).text }}
            </span>
          </template>
          <template v-if="column.key === 'statusName'">
            <Tag :color="getStatusColor((record as PaymentAmendmentDTO).status)">
              {{ (record as PaymentAmendmentDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record as PaymentAmendmentDTO)">查看</a>
              <template v-if="(record as PaymentAmendmentDTO).status === 'PENDING'">
                <a style="color: #52c41a" @click="handleApprove(record as PaymentAmendmentDTO)">通过</a>
                <a style="color: red" @click="handleReject(record as PaymentAmendmentDTO)">拒绝</a>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
