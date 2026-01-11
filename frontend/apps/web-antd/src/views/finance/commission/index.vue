<script setup lang="ts">
import type { ClientDTO } from '#/api/client/types';
import type {
  CommissionDTO,
  CommissionQuery,
  ContractParticipantDTO,
} from '#/api/finance/types';
import type { MatterSimpleDTO } from '#/api/matter/types';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';

import { getClientSelectOptions } from '#/api/client';
import {
  approveCommission,
  batchApproveCommission,
  batchIssueCommission,
  getCommissionDetail,
  getCommissionList,
  getContractDetail,
  getContractParticipants,
  getPendingCommissionPayments,
  issueCommission,
  manualCalculateCommission,
} from '#/api/finance';
import { getMatterSelectOptions } from '#/api/matter';

defineOptions({ name: 'FinanceCommission' });

const TabPane = Tabs.TabPane;

// 状态
const loading = ref(false);
const dataSource = ref<CommissionDTO[]>([]);
const total = ref(0);
const detailModalVisible = ref(false);
const currentCommission = ref<CommissionDTO | null>(null);
const clients = ref<ClientDTO[]>([]);
const matters = ref<MatterSimpleDTO[]>([]);
const activeTab = ref('all');
const selectedRowKeys = ref<number[]>([]);

// 待计算提成相关状态
const pendingCommissionsTab = ref('commissions'); // 'commissions' 或 'pending'
const pendingDataSource = ref<any[]>([]);
const pendingTotal = ref(0);
const pendingLoading = ref(false);

// 查询参数
const queryParams = reactive<CommissionQuery>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  clientId: undefined,
  matterId: undefined,
});

// 表格列
const columns = [
  {
    title: '提成编号',
    dataIndex: 'commissionNo',
    key: 'commissionNo',
    width: 130,
  },
  {
    title: '项目名称',
    dataIndex: 'matterName',
    key: 'matterName',
    width: 180,
    ellipsis: true,
  },
  { title: '客户名称', dataIndex: 'clientName', key: 'clientName', width: 150 },
  {
    title: '收款金额',
    dataIndex: 'paymentAmount',
    key: 'paymentAmount',
    width: 120,
  },
  {
    title: '提成比例',
    dataIndex: 'commissionRate',
    key: 'commissionRate',
    width: 100,
  },
  {
    title: '提成金额',
    dataIndex: 'commissionAmount',
    key: 'commissionAmount',
    width: 120,
  },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已发放', value: 'PAID' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getCommissionList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } catch (error: any) {
    message.error(error.message || '加载提成列表失败');
  } finally {
    loading.value = false;
  }
}

// 加载选项
async function loadOptions() {
  try {
    const [clientRes, matterRes] = await Promise.all([
      getClientSelectOptions({ pageNum: 1, pageSize: 1000 }),
      getMatterSelectOptions({ pageNum: 1, pageSize: 1000 }),
    ]);
    clients.value = clientRes.list;
    matters.value = matterRes.list;
  } catch (error: any) {
    console.error('加载选项失败:', error);
  }
}

// 加载待计算提成的收款记录
async function loadPendingCommissions() {
  pendingLoading.value = true;
  try {
    const payments = await getPendingCommissionPayments();
    pendingDataSource.value = payments.map((payment: any) => ({
      id: payment.id,
      feeId: payment.feeId,
      contractId: payment.contractId,
      matterId: payment.matterId,
      matterName: payment.matterName,
      clientId: payment.clientId,
      clientName: payment.clientName,
      feeNo: payment.paymentNo, // 使用收款编号作为显示
      amount: payment.amount,
      paidAmount: payment.amount, // 已确认的收款，已收金额等于收款金额
      payment, // 保存完整的payment对象，用于后续计算提成
    }));
    pendingTotal.value = pendingDataSource.value.length;
  } catch (error: any) {
    message.error(error.message || '加载待计算提成记录失败');
  } finally {
    pendingLoading.value = false;
  }
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryParams.status = undefined;
  queryParams.clientId = undefined;
  queryParams.matterId = undefined;
  queryParams.pageNum = 1;
  fetchData();
}

// 查看详情
async function handleView(record: CommissionDTO) {
  try {
    const commission = await getCommissionDetail(record.id);
    currentCommission.value = commission;
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取提成详情失败');
  }
}

// 审批提成
function handleApprove(record: CommissionDTO) {
  Modal.confirm({
    title: '审批提成',
    content: '确定要审批通过这笔提成吗？',
    okText: '通过',
    cancelText: '拒绝',
    onOk: async () => {
      try {
        await approveCommission(record.id, true);
        message.success('审批通过成功');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '审批失败');
      }
    },
    onCancel: async () => {
      const comment = prompt('请输入拒绝原因:');
      if (comment) {
        try {
          await approveCommission(record.id, false, comment);
          message.success('审批拒绝成功');
          fetchData();
        } catch (error: any) {
          message.error(error.message || '审批失败');
        }
      }
    },
  });
}

// 发放提成
function handleIssue(record: CommissionDTO) {
  Modal.confirm({
    title: '确认发放',
    content: '确定要确认这笔提成已发放吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await issueCommission(record.id);
        message.success('确认发放成功');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '确认发放失败');
      }
    },
  });
}

// 批量审批
function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要审批的提成记录');
    return;
  }
  Modal.confirm({
    title: '批量审批',
    content: `确定要审批通过选中的 ${selectedRowKeys.value.length} 条提成记录吗？`,
    okText: '通过',
    cancelText: '拒绝',
    onOk: async () => {
      try {
        await batchApproveCommission(selectedRowKeys.value, true);
        message.success('批量审批通过成功');
        selectedRowKeys.value = [];
        fetchData();
      } catch (error: any) {
        message.error(error.message || '批量审批失败');
      }
    },
    onCancel: async () => {
      const comment = prompt('请输入拒绝原因:');
      if (comment) {
        try {
          await batchApproveCommission(selectedRowKeys.value, false, comment);
          message.success('批量审批拒绝成功');
          selectedRowKeys.value = [];
          fetchData();
        } catch (error: any) {
          message.error(error.message || '批量审批失败');
        }
      }
    },
  });
}

// 批量发放
function handleBatchIssue() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要发放的提成记录');
    return;
  }
  Modal.confirm({
    title: '批量发放',
    content: `确定要确认选中的 ${selectedRowKeys.value.length} 条提成记录已发放吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await batchIssueCommission(selectedRowKeys.value);
        message.success('批量确认发放成功');
        selectedRowKeys.value = [];
        fetchData();
      } catch (error: any) {
        message.error(error.message || '批量发放失败');
      }
    },
  });
}

// 主Tab切换
function handleMainTabChange(key: number | string) {
  pendingCommissionsTab.value = String(key);
  if (key === 'pending') {
    loadPendingCommissions(); // 加载待计算提成的收款记录
  } else {
    fetchData(); // 加载提成记录
  }
}

// Tab切换
function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  switch (key) {
    case 'all': {
      queryParams.status = undefined;

      break;
    }
    case 'approved': {
      queryParams.status = 'APPROVED';

      break;
    }
    case 'paid': {
      queryParams.status = 'PAID';

      break;
    }
    case 'pending': {
      queryParams.status = 'PENDING';

      break;
    }
    // No default
  }
  queryParams.pageNum = 1;
  fetchData();
}

// 待计算提成相关状态
const commissionFormModalVisible = ref(false);
const currentFee = ref<any>(null);
const commissionFormData = ref<
  Array<{
    commissionAmount?: number;
    commissionRate?: number;
    originalRate?: number;
    participantId: number;
    remark?: string;
    role: string;
    roleName: string;
    userId: number;
    userName?: string;
  }>
>([]);
const commissionSaving = ref(false);

// 选择收款记录进行提成计算
async function handleSelectPayment(record: any) {
  try {
    // record 包含 payment 对象
    const payment = record.payment || record;
    const paymentAmount = payment.amount || 0;

    // 获取合同信息
    if (!payment.contractId) {
      message.error('该收款记录没有关联合同，无法计算提成');
      return;
    }

    // 获取合同详情（包含提成比例）
    const contract = await getContractDetail(payment.contractId);

    // 保存到 currentFee 用于显示
    currentFee.value = {
      id: payment.feeId,
      feeNo: payment.paymentNo,
      contractId: payment.contractId,
      matterId: payment.matterId,
      matterName: payment.matterName,
      clientId: payment.clientId,
      clientName: payment.clientName,
      amount: paymentAmount,
      payments: [payment], // 保存payment对象
      contract, // 保存合同信息
    };

    // 获取合同参与人
    const participants = await getContractParticipants(payment.contractId);
    if (!participants || participants.length === 0) {
      message.warning('该合同没有参与人，无法计算提成');
      return;
    }

    // 根据参与人角色，匹配合同上对应的提成比例
    function getCommissionRateByRole(role: string): number | undefined {
      switch (role) {
        case 'CO_COUNSEL': {
          return contract.assistLawyerRate;
        }
        case 'LEAD': {
          return contract.leadLawyerRate;
        }
        case 'ORIGINATOR': {
          return contract.originatorRate;
        }
        case 'PARALEGAL': {
          return contract.supportStaffRate;
        }
        default: {
          return undefined;
        }
      }
    }

    // 初始化提成表单数据
    // 优先使用参与人表中的比例（用户创建合同时实际设置的），其次使用合同表的默认比例
    commissionFormData.value = participants.map((p: ContractParticipantDTO) => {
      const contractDefaultRate = getCommissionRateByRole(p.role);
      // 优先使用参与人的比例（p.commissionRate），这是用户创建合同时设置的实际比例
      const finalRate =
        p.commissionRate !== undefined && p.commissionRate !== null
          ? p.commissionRate
          : contractDefaultRate || 0;

      return {
        participantId: p.id,
        userId: p.userId,
        userName: p.userName,
        role: p.role,
        roleName: getRoleName(p.role),
        commissionRate: finalRate,
        originalRate: p.commissionRate, // 参与人的实际比例
        commissionAmount:
          finalRate > 0
            ? Number(((paymentAmount * finalRate) / 100).toFixed(2))
            : undefined,
        remark: `合同参与人设置为 ${p.commissionRate !== undefined && p.commissionRate !== null ? `${p.commissionRate}%` : '无比例'}`,
      };
    });

    commissionFormModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
  }
}

// 获取角色名称
function getRoleName(role: string): string {
  const roleMap: Record<string, string> = {
    LEAD: '主办律师',
    CO_COUNSEL: '协办律师',
    ORIGINATOR: '案源人',
    PARALEGAL: '律师助理',
  };
  return roleMap[role] || role;
}

// 计算提成金额（根据比例）
function calculateCommissionAmount(index: number) {
  const item = commissionFormData.value[index];
  if (!item) return;
  if (item.commissionRate && currentFee.value) {
    const payment = currentFee.value.payments?.find(
      (p: any) => p.status === 'CONFIRMED',
    );
    if (payment) {
      item.commissionAmount = Number(
        ((payment.amount * item.commissionRate) / 100).toFixed(2),
      );
    }
  }
}

// 保存手动计算的提成
async function handleSaveCommission() {
  if (!currentFee.value) {
    message.error('收费记录不存在');
    return;
  }

  // 验证数据
  const validParticipants = commissionFormData.value.filter(
    (p) =>
      p.commissionRate &&
      p.commissionRate > 0 &&
      p.commissionAmount &&
      p.commissionAmount > 0,
  );

  if (validParticipants.length === 0) {
    message.error('请至少设置一个参与人的提成');
    return;
  }

  commissionSaving.value = true;
  try {
    const payment = currentFee.value.payments?.[0];
    if (!payment || !payment.id) {
      message.error('未找到收款记录');
      return;
    }

    await manualCalculateCommission({
      paymentId: payment.id,
      participants: validParticipants.map((p) => ({
        participantId: p.participantId,
        userId: p.userId,
        commissionRate: p.commissionRate,
        commissionAmount: p.commissionAmount,
        remark: p.remark,
      })),
    });

    message.success('提成计算成功');
    commissionFormModalVisible.value = false;

    // 刷新数据
    loadPendingCommissions(); // 刷新待计算提成列表
    fetchData(); // 刷新提成记录列表

    // 切换到提成记录Tab
    pendingCommissionsTab.value = 'commissions';
    activeTab.value = 'all';
  } catch (error: any) {
    message.error(error.message || '计算提成失败');
  } finally {
    commissionSaving.value = false;
  }
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'blue',
    PAID: 'green',
  };
  return colorMap[status] || 'default';
}

// 行选择
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (number | string)[]) => {
    selectedRowKeys.value = keys as number[];
  },
}));

onMounted(() => {
  fetchData();
  loadOptions();
});
</script>

<template>
  <Page title="提成管理" description="管理律师提成计算与发放">
    <Card>
      <Tabs
        v-model:active-key="pendingCommissionsTab"
        @change="handleMainTabChange"
      >
        <TabPane key="commissions" tab="提成记录">
          <Tabs
            v-model:active-key="activeTab"
            @change="handleTabChange"
            style="margin-bottom: 16px"
          >
            <TabPane key="all" tab="全部" />
            <TabPane key="pending" tab="待审批" />
            <TabPane key="approved" tab="已审批" />
            <TabPane key="paid" tab="已发放" />
          </Tabs>

          <!-- 提成记录的搜索栏 -->
          <div style="margin-bottom: 16px">
            <Row :gutter="[16, 16]">
              <Col :xs="24" :sm="12" :md="6">
                <Select
                  v-model:value="queryParams.status"
                  placeholder="提成状态"
                  allow-clear
                  style="width: 100%"
                  :options="statusOptions"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Select
                  v-model:value="queryParams.clientId"
                  placeholder="客户"
                  allow-clear
                  show-search
                  :filter-option="
                    (input, option) =>
                      (option?.label || '')
                        .toLowerCase()
                        .includes(input.toLowerCase())
                  "
                  style="width: 100%"
                  :options="
                    clients.map((c) => ({
                      label: c.clientNo ? `[${c.clientNo}] ${c.name}` : c.name,
                      value: c.id,
                    }))
                  "
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Select
                  v-model:value="queryParams.matterId"
                  placeholder="项目"
                  allow-clear
                  show-search
                  :filter-option="
                    (input, option) =>
                      (option?.label || '')
                        .toLowerCase()
                        .includes(input.toLowerCase())
                  "
                  style="width: 100%"
                  :options="
                    matters.map((m) => ({
                      label: `[${m.matterNo}] ${m.name}`,
                      value: m.id,
                    }))
                  "
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Space wrap>
                  <Button type="primary" @click="handleSearch">查询</Button>
                  <Button @click="handleReset">重置</Button>
                  <Button
                    v-if="selectedRowKeys.length > 0 && activeTab === 'pending'"
                    type="primary"
                    @click="handleBatchApprove"
                  >
                    批量审批
                  </Button>
                  <Button
                    v-if="
                      selectedRowKeys.length > 0 && activeTab === 'approved'
                    "
                    type="primary"
                    @click="handleBatchIssue"
                  >
                    批量发放
                  </Button>
                </Space>
              </Col>
            </Row>
          </div>

          <!-- 提成记录表格 -->
          <Table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :row-selection="rowSelection"
            :pagination="{
              current: queryParams.pageNum,
              pageSize: queryParams.pageSize,
              total,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (page, size) => {
                queryParams.pageNum = page;
                queryParams.pageSize = size;
                fetchData();
              },
            }"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'statusName'">
                <Tag :color="getStatusColor((record as CommissionDTO).status)">
                  {{ (record as CommissionDTO).statusName }}
                </Tag>
              </template>
              <template v-if="column.key === 'paymentAmount'">
                ¥{{
                  (record as CommissionDTO).paymentAmount?.toLocaleString() ||
                  '0'
                }}
              </template>
              <template v-if="column.key === 'commissionRate'">
                {{
                  (record as CommissionDTO).commissionRate
                    ? `${(record as CommissionDTO).commissionRate}%`
                    : '-'
                }}
              </template>
              <template v-if="column.key === 'commissionAmount'">
                ¥{{
                  (
                    record as CommissionDTO
                  ).commissionAmount?.toLocaleString() || '0'
                }}
              </template>
              <template v-if="column.key === 'action'">
                <Space>
                  <a @click="handleView(record as CommissionDTO)">查看</a>
                  <template
                    v-if="(record as CommissionDTO).status === 'PENDING'"
                  >
                    <a @click="handleApprove(record as CommissionDTO)">审批</a>
                  </template>
                  <template
                    v-if="(record as CommissionDTO).status === 'APPROVED'"
                  >
                    <a @click="handleIssue(record as CommissionDTO)">发放</a>
                  </template>
                </Space>
              </template>
            </template>
          </Table>
        </TabPane>
        <TabPane key="pending" tab="待计算提成">
          <!-- 待计算提成的搜索栏 -->
          <div style="margin-bottom: 16px">
            <Row :gutter="[16, 16]">
              <Col :xs="24" :sm="12" :md="6">
                <Select
                  placeholder="客户"
                  allow-clear
                  show-search
                  :filter-option="
                    (input, option) =>
                      (option?.label || '')
                        .toLowerCase()
                        .includes(input.toLowerCase())
                  "
                  style="width: 100%"
                  :options="
                    clients.map((c) => ({
                      label: c.clientNo ? `[${c.clientNo}] ${c.name}` : c.name,
                      value: c.id,
                    }))
                  "
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Select
                  placeholder="项目"
                  allow-clear
                  show-search
                  :filter-option="
                    (input, option) =>
                      (option?.label || '')
                        .toLowerCase()
                        .includes(input.toLowerCase())
                  "
                  style="width: 100%"
                  :options="
                    matters.map((m) => ({
                      label: `[${m.matterNo}] ${m.name}`,
                      value: m.id,
                    }))
                  "
                />
              </Col>
              <Col :xs="24" :sm="12" :md="6">
                <Space wrap>
                  <Button type="primary">查询</Button>
                  <Button>重置</Button>
                </Space>
              </Col>
            </Row>
          </div>

          <!-- 待计算提成表格 -->
          <Table
            :columns="[
              {
                title: '收费编号',
                dataIndex: 'feeNo',
                key: 'feeNo',
                width: 130,
              },
              {
                title: '项目名称',
                dataIndex: 'matterName',
                key: 'matterName',
                width: 150,
                ellipsis: true,
              },
              {
                title: '客户名称',
                dataIndex: 'clientName',
                key: 'clientName',
                width: 120,
              },
              {
                title: '收费金额',
                dataIndex: 'amount',
                key: 'amount',
                width: 100,
              },
              {
                title: '已收金额',
                dataIndex: 'paidAmount',
                key: 'paidAmount',
                width: 100,
              },
              { title: '操作', key: 'action', width: 120 },
            ]"
            :data-source="pendingDataSource"
            :loading="pendingLoading"
            :pagination="{ pageSize: 10 }"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template
                v-if="column.key === 'amount' || column.key === 'paidAmount'"
              >
                ¥{{ (record[column.key] || 0).toLocaleString() }}
              </template>
              <template v-if="column.key === 'action'">
                <Button type="link" @click="handleSelectPayment(record)">
                  计算提成
                </Button>
              </template>
            </template>
          </Table>
        </TabPane>
      </Tabs>
    </Card>

    <!-- 提成详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="提成详情"
      width="800px"
      :footer="null"
    >
      <div v-if="currentCommission" style="padding: 20px">
        <Row :gutter="[16, 16]">
          <Col :span="12">
            <div>
              <strong>提成编号：</strong
              >{{ currentCommission.commissionNo || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>项目名称：</strong
              >{{ currentCommission.matterName || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>客户名称：</strong
              >{{ currentCommission.clientName || '-' }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>收款金额：</strong>¥{{
                currentCommission.paymentAmount?.toLocaleString() || '0'
              }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>提成比例：</strong
              >{{
                currentCommission.commissionRate
                  ? `${currentCommission.commissionRate}%`
                  : '-'
              }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>提成金额：</strong>¥{{
                currentCommission.commissionAmount?.toLocaleString() || '0'
              }}
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>状态：</strong>
              <Tag :color="getStatusColor(currentCommission.status)">
                {{ currentCommission.statusName }}
              </Tag>
            </div>
          </Col>
          <Col :span="12">
            <div>
              <strong>创建时间：</strong
              >{{ currentCommission.createdAt || '-' }}
            </div>
          </Col>
          <Col :span="24">
            <div>
              <strong>备注：</strong>{{ currentCommission.remark || '-' }}
            </div>
          </Col>
        </Row>
      </div>
    </Modal>

    <!-- 提成计算表单弹窗 -->
    <Modal
      v-model:open="commissionFormModalVisible"
      title="手动计算提成"
      width="900px"
      :confirm-loading="commissionSaving"
      @ok="handleSaveCommission"
      @cancel="commissionFormModalVisible = false"
    >
      <div v-if="currentFee" style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :span="12">
            <div><strong>收费编号：</strong>{{ currentFee.feeNo }}</div>
          </Col>
          <Col :span="12">
            <div><strong>项目名称：</strong>{{ currentFee.matterName }}</div>
          </Col>
          <Col :span="12">
            <div><strong>客户名称：</strong>{{ currentFee.clientName }}</div>
          </Col>
          <Col :span="12">
            <div>
              <strong>收款金额：</strong>¥{{
                (
                  currentFee.payments?.find(
                    (p: any) => p.status === 'CONFIRMED',
                  )?.amount || 0
                ).toLocaleString()
              }}
            </div>
          </Col>
        </Row>
      </div>

      <div
        style="
          padding: 12px;
          margin-bottom: 16px;
          background: #f5f5f5;
          border-radius: 4px;
        "
      >
        <div style="margin-bottom: 8px; font-size: 12px; color: #666">
          说明：
        </div>
        <div style="font-size: 12px; color: #666">
          1. 提成比例已自动载入合同参与人设定的比例，财务可以修改<br />
          2. 修改提成比例后，提成金额会自动计算（收款金额 × 提成比例）<br />
          3. 也可以直接修改提成金额（财务最终结算时使用）
        </div>
      </div>

      <Table
        :columns="[
          {
            title: '参与人',
            dataIndex: 'userName',
            key: 'userName',
            width: 120,
          },
          { title: '角色', dataIndex: 'roleName', key: 'roleName', width: 100 },
          {
            title: '提成比例(%)',
            dataIndex: 'commissionRate',
            key: 'commissionRate',
            width: 120,
          },
          {
            title: '提成金额(元)',
            dataIndex: 'commissionAmount',
            key: 'commissionAmount',
            width: 150,
          },
          { title: '备注', dataIndex: 'remark', key: 'remark' },
        ]"
        :data-source="commissionFormData"
        :pagination="false"
        row-key="participantId"
        size="middle"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'commissionRate'">
            <div style="display: flex; gap: 8px; align-items: center">
              <InputNumber
                v-model:value="record.commissionRate"
                :min="0"
                :max="100"
                :precision="2"
                style="flex: 1"
                addon-after="%"
                @change="calculateCommissionAmount(index)"
              />
              <span
                v-if="
                  record.originalRate != null &&
                  record.originalRate !== record.commissionRate
                "
                style="font-size: 11px; color: #52c41a"
              >
                (原: {{ record.originalRate }}%)
              </span>
            </div>
          </template>
          <template v-else-if="column.key === 'commissionAmount'">
            <InputNumber
              v-model:value="record.commissionAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
              addon-after="元"
            />
          </template>
          <template v-else-if="column.key === 'remark'">
            <Input
              v-model:value="record.remark"
              placeholder="可选"
              style="width: 100%"
            />
          </template>
        </template>
      </Table>
    </Modal>
  </Page>
</template>
