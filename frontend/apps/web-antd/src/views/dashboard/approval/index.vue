<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Input,
  Select,
  Tabs,
  TabPane,
  Badge,
  Textarea,
  Form,
  FormItem,
  Descriptions,
  DescriptionsItem,
} from 'ant-design-vue';
import type { Key } from 'ant-design-vue/es/table/interface';
import {
  getPendingApprovals,
  getMyInitiatedApprovals,
  getMyApprovedHistory,
  getApprovalDetail,
  approveApproval,
  type ApprovalDTO,
} from '#/api/workbench';
import { requestClient } from '#/api/request';
import { getContractDetail } from '#/api/matter';

defineOptions({ name: 'WorkbenchApproval' });

// 当前 Tab
const activeTab = ref<'pending' | 'initiated' | 'history'>('pending');

// 待审批列表
const pendingList = ref<ApprovalDTO[]>([]);
const pendingLoading = ref(false);

// 我发起的审批
const initiatedList = ref<ApprovalDTO[]>([]);
const initiatedLoading = ref(false);

// 审批历史
const historyList = ref<ApprovalDTO[]>([]);
const historyLoading = ref(false);

// 筛选条件
const filterBusinessType = ref<string | undefined>(undefined);
const filterStatus = ref<string | undefined>(undefined);
const searchKeyword = ref('');

// 选中的行
const selectedRowKeys = ref<Key[]>([]);

// 审批详情弹窗
const detailModalVisible = ref(false);
const currentApproval = ref<ApprovalDTO | null>(null);
const approvalComment = ref('');
const approvalLoading = ref(false);

// 业务类型选项
const businessTypeOptions = [
  { label: '全部', value: undefined },
  { label: '合同审批', value: 'CONTRACT' },
  { label: '用印申请', value: 'SEAL_APPLICATION' },
  { label: '利冲检查', value: 'CONFLICT_CHECK' },
  { label: '利冲豁免', value: 'CONFLICT_EXEMPTION' },
  { label: '费用报销', value: 'EXPENSE' },
  { label: '收款变更', value: 'PAYMENT_AMENDMENT' },
  { label: '项目结案', value: 'MATTER_CLOSE' },
  { label: '转正申请', value: 'REGULARIZATION' },
  { label: '离职申请', value: 'RESIGNATION' },
];

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
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

// 待审批表格列
const pendingColumns = [
  { title: '审批编号', dataIndex: 'approvalNo', key: 'approvalNo', width: 180 },
  { title: '业务类型', dataIndex: 'businessTypeName', key: 'businessTypeName', width: 120 },
  { title: '业务标题', dataIndex: 'businessTitle', key: 'businessTitle', width: 200, ellipsis: true },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '优先级', dataIndex: 'priorityName', key: 'priorityName', width: 80 },
  { title: '紧急程度', dataIndex: 'urgencyName', key: 'urgencyName', width: 100 },
  { title: '申请时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 我发起的审批表格列
const initiatedColumns = [
  { title: '审批编号', dataIndex: 'approvalNo', key: 'approvalNo', width: 180 },
  { title: '业务类型', dataIndex: 'businessTypeName', key: 'businessTypeName', width: 120 },
  { title: '业务标题', dataIndex: 'businessTitle', key: 'businessTitle', width: 200, ellipsis: true },
  { title: '审批人', dataIndex: 'approverName', key: 'approverName', width: 100 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '审批意见', dataIndex: 'comment', key: 'comment', width: 150, ellipsis: true },
  { title: '申请时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '审批时间', dataIndex: 'approvedAt', key: 'approvedAt', width: 160 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
];

// 审批历史表格列
const historyColumns = [
  { title: '审批编号', dataIndex: 'approvalNo', key: 'approvalNo', width: 180 },
  { title: '业务类型', dataIndex: 'businessTypeName', key: 'businessTypeName', width: 120 },
  { title: '业务标题', dataIndex: 'businessTitle', key: 'businessTitle', width: 200, ellipsis: true },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '审批结果', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '审批意见', dataIndex: 'comment', key: 'comment', width: 150, ellipsis: true },
  { title: '申请时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '审批时间', dataIndex: 'approvedAt', key: 'approvedAt', width: 160 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
];

// 加载待审批列表
async function fetchPendingList() {
  pendingLoading.value = true;
  try {
    const data = await getPendingApprovals();
    pendingList.value = data || [];
  } catch (error: any) {
    message.error(error.message || '加载待审批列表失败');
  } finally {
    pendingLoading.value = false;
  }
}

// 加载我发起的审批
async function fetchInitiatedList() {
  initiatedLoading.value = true;
  try {
    const data = await getMyInitiatedApprovals();
    initiatedList.value = data || [];
  } catch (error: any) {
    message.error(error.message || '加载审批列表失败');
  } finally {
    initiatedLoading.value = false;
  }
}

// 加载审批历史
async function fetchHistoryList() {
  historyLoading.value = true;
  try {
    const data = await getMyApprovedHistory();
    historyList.value = data || [];
  } catch (error: any) {
    message.error(error.message || '加载审批历史失败');
  } finally {
    historyLoading.value = false;
  }
}

// 筛选后的待审批列表
const filteredPendingList = computed(() => {
  let list = pendingList.value;
  
  if (filterBusinessType.value) {
    list = list.filter(item => item.businessType === filterBusinessType.value);
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    list = list.filter(item => 
      item.approvalNo?.toLowerCase().includes(keyword) ||
      item.businessTitle?.toLowerCase().includes(keyword) ||
      item.applicantName?.toLowerCase().includes(keyword)
    );
  }
  
  return list;
});

// 筛选后的我发起的审批列表
const filteredInitiatedList = computed(() => {
  let list = initiatedList.value;
  
  if (filterBusinessType.value) {
    list = list.filter(item => item.businessType === filterBusinessType.value);
  }
  
  if (filterStatus.value) {
    list = list.filter(item => item.status === filterStatus.value);
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    list = list.filter(item => 
      item.approvalNo?.toLowerCase().includes(keyword) ||
      item.businessTitle?.toLowerCase().includes(keyword) ||
      item.approverName?.toLowerCase().includes(keyword)
    );
  }
  
  return list;
});

// 筛选后的审批历史列表
const filteredHistoryList = computed(() => {
  let list = historyList.value;
  
  if (filterBusinessType.value) {
    list = list.filter(item => item.businessType === filterBusinessType.value);
  }
  
  if (filterStatus.value) {
    list = list.filter(item => item.status === filterStatus.value);
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    list = list.filter(item => 
      item.approvalNo?.toLowerCase().includes(keyword) ||
      item.businessTitle?.toLowerCase().includes(keyword) ||
      item.applicantName?.toLowerCase().includes(keyword)
    );
  }
  
  return list;
});

// 待审批数量
const pendingCount = computed(() => pendingList.value.length);

// 我发起的审批数量
const initiatedCount = computed(() => initiatedList.value.length);

// 审批历史数量
const historyCount = computed(() => historyList.value.length);

// Tab 切换
function handleTabChange(key: Key) {
  activeTab.value = String(key) as 'pending' | 'initiated' | 'history';
  selectedRowKeys.value = [];
  filterBusinessType.value = undefined;
  filterStatus.value = undefined;
  searchKeyword.value = '';
}

// 查看详情
async function handleViewDetail(record: ApprovalDTO) {
  try {
    const detail = await getApprovalDetail(record.id);
    currentApproval.value = detail;
    approvalComment.value = '';
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 审批通过
async function handleApprove(record: ApprovalDTO) {
  Modal.confirm({
    title: '确认通过',
    content: `确定要通过「${record.businessTitle}」的审批吗？`,
    okText: '通过',
    okType: 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await approveApproval({
          approvalId: record.id,
          result: 'APPROVED',
          comment: '',
        });
        message.success('审批通过');
        fetchPendingList();
      } catch (error: any) {
        message.error(error.message || '审批失败');
      }
    },
  });
}

// 拒绝审批弹窗相关
const rejectModalVisible = ref(false);
const rejectReason = ref('');
const rejectingRecord = ref<ApprovalDTO | null>(null);

// 审批拒绝 - 打开弹窗
function handleReject(record: ApprovalDTO) {
  rejectingRecord.value = record;
  rejectReason.value = '';
  rejectModalVisible.value = true;
}

// 确认拒绝
async function confirmReject() {
  if (!rejectingRecord.value) return;
  
  if (!rejectReason.value.trim()) {
        message.warning('请填写拒绝原因');
    return;
      }
  
      try {
        await approveApproval({
      approvalId: rejectingRecord.value.id,
          result: 'REJECTED',
      comment: rejectReason.value.trim(),
        });
        message.success('已拒绝');
    rejectModalVisible.value = false;
        fetchPendingList();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
}

// 弹窗内审批通过
async function handleModalApprove() {
  if (!currentApproval.value) return;
  
  approvalLoading.value = true;
  try {
    await approveApproval({
      approvalId: currentApproval.value.id,
      result: 'APPROVED',
      comment: approvalComment.value.trim() || undefined,
    });
    message.success('审批通过');
    detailModalVisible.value = false;
    fetchPendingList();
  } catch (error: any) {
    message.error(error.message || '审批失败');
  } finally {
    approvalLoading.value = false;
  }
}

// 弹窗内审批拒绝
async function handleModalReject() {
  if (!currentApproval.value) return;
  
  if (!approvalComment.value.trim()) {
    message.warning('拒绝时必须填写审批意见');
    return;
  }
  
  approvalLoading.value = true;
  try {
    await approveApproval({
      approvalId: currentApproval.value.id,
      result: 'REJECTED',
      comment: approvalComment.value.trim(),
    });
    message.success('已拒绝');
    detailModalVisible.value = false;
    fetchPendingList();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  } finally {
    approvalLoading.value = false;
  }
}

// 批量审批通过
async function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要审批的记录');
    return;
  }
  
  Modal.confirm({
    title: '批量通过',
    content: `确定要批量通过选中的 ${selectedRowKeys.value.length} 条审批吗？`,
    okText: '确定',
    okType: 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await requestClient.post('/workbench/approval/batch-approve', {
          approvalIds: selectedRowKeys.value,
          result: 'APPROVED',
        });
        message.success('批量审批成功');
        selectedRowKeys.value = [];
        fetchPendingList();
      } catch (error: any) {
        message.error(error.message || '批量审批失败');
      }
    },
  });
}

// 批量拒绝弹窗相关
const batchRejectModalVisible = ref(false);
const batchRejectReason = ref('');

// 批量拒绝 - 打开弹窗
function handleBatchReject() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要拒绝的记录');
    return;
  }
  batchRejectReason.value = '';
  batchRejectModalVisible.value = true;
}

// 确认批量拒绝
async function confirmBatchReject() {
  if (!batchRejectReason.value.trim()) {
        message.warning('请填写拒绝原因');
    return;
      }
  
      try {
        await requestClient.post('/workbench/approval/batch-approve', {
          approvalIds: selectedRowKeys.value,
          result: 'REJECTED',
      comment: batchRejectReason.value.trim(),
        });
        message.success('批量拒绝成功');
    batchRejectModalVisible.value = false;
        selectedRowKeys.value = [];
        fetchPendingList();
      } catch (error: any) {
        message.error(error.message || '批量操作失败');
      }
}

// 跳转到业务详情
function goToBusinessDetail(record: ApprovalDTO) {
  const urlMap: Record<string, string> = {
    CONTRACT: `/matter/contract?id=${record.businessId}`,
    SEAL_APPLICATION: `/document/seal-apply?id=${record.businessId}`,
    CONFLICT_CHECK: `/client/conflict?id=${record.businessId}`,
    EXPENSE: `/finance/expense?id=${record.businessId}`,
    PAYMENT_AMENDMENT: `/finance/payment-amendment?id=${record.businessId}`,
    MATTER_CLOSE: `/matter/list?id=${record.businessId}`,
    REGULARIZATION: `/hr/regularization?id=${record.businessId}`,
    RESIGNATION: `/hr/resignation?id=${record.businessId}`,
  };
  
  const url = urlMap[record.businessType];
  if (url) {
    window.open(url, '_blank');
  } else {
    message.info('暂不支持查看该类型业务详情');
  }
}

// 格式化时间
function formatTime(time: string | undefined) {
  if (!time) return '-';
  return time.replace('T', ' ').substring(0, 19);
}

// 判断是否是合同变更审批
function isContractChangeApproval(approval: ApprovalDTO | null): boolean {
  if (!approval) return false;
  return approval.businessType === 'CONTRACT' && 
         approval.businessTitle?.includes('变更申请') === true;
}

// 预览合同变更审批单
async function handlePreviewChangeApprovalForm() {
  if (!currentApproval.value) return;
  
  try {
    // 获取合同详情
    const contract = await getContractDetail(currentApproval.value.businessId);
    
    // 解析变更内容（从 businessSnapshot 中解析）
    const changeContent = currentApproval.value.businessSnapshot || '';
    
    // 生成审批单HTML
    const html = generateChangeApprovalFormHtml({
      approvalNo: currentApproval.value.approvalNo,
      contractNo: contract.contractNo || '',
      contractName: contract.name || '',
      applicantName: currentApproval.value.applicantName || '',
      approverName: currentApproval.value.approverName || '',
      changeContent: changeContent,
      comment: currentApproval.value.comment || '',
      createdAt: currentApproval.value.createdAt || '',
      approvedAt: currentApproval.value.approvedAt || '',
      status: currentApproval.value.status,
    });
    
    // 打开新窗口显示
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      message.error('无法打开预览窗口，请检查浏览器是否阻止了弹出窗口');
      return;
    }
    
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8">
          <title>合同变更审批单 - ${currentApproval.value.approvalNo}</title>
          <style>
            body { 
              font-family: "SimSun", "宋体", serif; 
              padding: 40px; 
              line-height: 1.8; 
              font-size: 14px;
            }
            h2 { 
              text-align: center; 
              margin-bottom: 20px; 
              letter-spacing: 8px;
            }
            table { 
              width: 100%; 
              border-collapse: collapse; 
              margin: 20px 0; 
            }
            td, th { 
              border: 1px solid #333; 
              padding: 10px; 
              vertical-align: top; 
            }
            th { 
              background: #f5f5f5; 
              text-align: center; 
              font-weight: bold; 
              width: 15%; 
            }
            .change-content {
              white-space: pre-wrap;
              line-height: 1.6;
            }
            @media print { 
              body { padding: 20px; } 
              @page { margin: 2cm; }
            }
          </style>
        </head>
        <body>
          ${html}
          <div style="text-align: center; margin-top: 30px;">
            <button onclick="window.print()" style="padding: 8px 16px; cursor: pointer; font-size: 14px;">打印审批单</button>
            <button onclick="window.close()" style="padding: 8px 16px; cursor: pointer; font-size: 14px; margin-left: 10px;">关闭</button>
          </div>
        </body>
      </html>
    `);
    printWindow.document.close();
  } catch (error: any) {
    message.error('生成审批单失败：' + (error.message || '未知错误'));
  }
}

// 生成合同变更审批单HTML
function generateChangeApprovalFormHtml(data: {
  approvalNo: string;
  contractNo: string;
  contractName: string;
  applicantName: string;
  approverName: string;
  changeContent: string;
  comment: string;
  createdAt: string;
  approvedAt: string;
  status: string;
}): string {
  const formatDate = (dateStr: string) => {
    if (!dateStr) return '____年____月____日';
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${year}年${month}月${day}日`;
  };
  
  const statusText = data.status === 'APPROVED' ? '已通过' : 
                     data.status === 'REJECTED' ? '已拒绝' : 
                     data.status === 'PENDING' ? '待审批' : data.status;
  
  return `
    <div style="max-width: 800px; margin: 0 auto;">
      <h2 style="text-align: center; margin-bottom: 5px;">律师事务所</h2>
      <h2 style="text-align: center; margin-bottom: 20px; letter-spacing: 8px;">合同变更审批单</h2>
      
      <table border="1" cellpadding="10" cellspacing="0" style="width: 100%; border-collapse: collapse;">
        <tr>
          <th style="width: 15%; background: #f5f5f5;">审批编号</th>
          <td colspan="3">${data.approvalNo}</td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">合同编号</th>
          <td colspan="3">${data.contractNo}</td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">合同名称</th>
          <td colspan="3">${data.contractName}</td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">申请人</th>
          <td>${data.applicantName}</td>
          <th style="background: #f5f5f5;">申请时间</th>
          <td>${formatDate(data.createdAt)}</td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">审批人</th>
          <td>${data.approverName}</td>
          <th style="background: #f5f5f5;">审批状态</th>
          <td>${statusText}</td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">变更内容</th>
          <td colspan="3" style="height: 200px; vertical-align: top;">
            <div class="change-content">${data.changeContent || '无变更内容'}</div>
          </td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">审批意见</th>
          <td colspan="3" style="height: 100px; vertical-align: top;">
            ${data.comment || (data.status === 'PENDING' ? '待审批' : '无')}
          </td>
        </tr>
        <tr>
          <th style="background: #f5f5f5;">审批时间</th>
          <td colspan="3">${data.approvedAt ? formatDate(data.approvedAt) : '待审批'}</td>
        </tr>
      </table>
    </div>
  `;
}

// 表格选择配置
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: Key[]) => {
    selectedRowKeys.value = keys;
  },
}));

// 初始化
onMounted(() => {
  fetchPendingList();
  fetchInitiatedList();
  fetchHistoryList();
});
</script>

<template>
  <Page title="审批中心" description="集中管理所有业务审批，快速处理待审批事项">
    <Card>
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <TabPane key="pending">
          <template #tab>
            <Badge :count="pendingCount" :offset="[10, 0]">
              <span>待我审批</span>
            </Badge>
          </template>
        </TabPane>
        <TabPane key="initiated">
          <template #tab>
            <Badge :count="initiatedCount" :offset="[10, 0]" :showZero="false">
              <span>我发起的</span>
            </Badge>
          </template>
        </TabPane>
        <TabPane key="history">
          <template #tab>
            <Badge :count="historyCount" :offset="[10, 0]" :showZero="false">
              <span>审批历史</span>
            </Badge>
          </template>
        </TabPane>
      </Tabs>

      <!-- 筛选区域 -->
      <div style="margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap; align-items: center;">
        <Select
          v-model:value="filterBusinessType"
          :options="businessTypeOptions"
          placeholder="业务类型"
          style="width: 150px"
          allowClear
        />
        <Select
          v-if="activeTab === 'initiated' || activeTab === 'history'"
          v-model:value="filterStatus"
          :options="statusOptions"
          placeholder="状态"
          style="width: 120px"
          allowClear
        />
        <Input.Search
          v-model:value="searchKeyword"
          placeholder="搜索编号/标题/姓名"
          style="width: 220px"
          allowClear
        />
        <div style="flex: 1;"></div>
        <template v-if="activeTab === 'pending' && selectedRowKeys.length > 0">
          <Button type="primary" @click="handleBatchApprove">
            批量通过 ({{ selectedRowKeys.length }})
          </Button>
          <Button danger @click="handleBatchReject">
            批量拒绝 ({{ selectedRowKeys.length }})
          </Button>
        </template>
      </div>

      <!-- 待审批列表 -->
      <Table
        v-if="activeTab === 'pending'"
        :columns="pendingColumns"
        :dataSource="filteredPendingList"
        :loading="pendingLoading"
        :rowSelection="rowSelection"
        :rowKey="(record: ApprovalDTO) => record.id"
        :scroll="{ x: 1200 }"
        :pagination="{ showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'priorityName'">
            <Tag :color="priorityColorMap[record.priority] || 'default'">
              {{ record.priorityName || record.priority }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'urgencyName'">
            <Tag v-if="record.urgency === 'URGENT'" color="red">紧急</Tag>
            <span v-else>{{ record.urgencyName || '普通' }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleViewDetail(record as ApprovalDTO)">
                详情
              </Button>
              <Button type="link" size="small" style="color: #52c41a" @click="handleApprove(record as ApprovalDTO)">
                通过
              </Button>
              <Button type="link" size="small" danger @click="handleReject(record as ApprovalDTO)">
                拒绝
              </Button>
            </Space>
          </template>
        </template>
      </Table>

      <!-- 我发起的审批列表 -->
      <Table
        v-if="activeTab === 'initiated'"
        :columns="initiatedColumns"
        :dataSource="filteredInitiatedList"
        :loading="initiatedLoading"
        :rowKey="(record: ApprovalDTO) => record.id"
        :scroll="{ x: 1300 }"
        :pagination="{ showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="statusColorMap[record.status] || 'default'">
              {{ record.statusName || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'approvedAt'">
            {{ formatTime(record.approvedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Button type="link" size="small" @click="handleViewDetail(record as ApprovalDTO)">
              查看
            </Button>
          </template>
        </template>
      </Table>

      <!-- 审批历史列表 -->
      <Table
        v-if="activeTab === 'history'"
        :columns="historyColumns"
        :dataSource="filteredHistoryList"
        :loading="historyLoading"
        :rowKey="(record: ApprovalDTO) => record.id"
        :scroll="{ x: 1300 }"
        :pagination="{ showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="statusColorMap[record.status] || 'default'">
              {{ record.statusName || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'approvedAt'">
            {{ formatTime(record.approvedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Button type="link" size="small" @click="handleViewDetail(record as ApprovalDTO)">
              查看
            </Button>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 审批详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      :title="activeTab === 'pending' ? '审批详情' : '查看审批'"
      :width="600"
      :footer="null"
    >
      <template v-if="currentApproval">
        <Descriptions :column="2" bordered size="small" style="margin-bottom: 16px;">
          <DescriptionsItem label="审批编号" :span="2">
            {{ currentApproval.approvalNo }}
          </DescriptionsItem>
          <DescriptionsItem label="业务类型">
            {{ currentApproval.businessTypeName }}
          </DescriptionsItem>
          <DescriptionsItem label="业务编号">
            {{ currentApproval.businessNo || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="业务标题" :span="2">
            {{ currentApproval.businessTitle }}
          </DescriptionsItem>
          <DescriptionsItem label="申请人">
            {{ currentApproval.applicantName }}
          </DescriptionsItem>
          <DescriptionsItem label="申请时间">
            {{ formatTime(currentApproval.createdAt) }}
          </DescriptionsItem>
          <DescriptionsItem label="审批人">
            {{ currentApproval.approverName }}
          </DescriptionsItem>
          <DescriptionsItem label="状态">
            <Tag :color="statusColorMap[currentApproval.status] || 'default'">
              {{ currentApproval.statusName || currentApproval.status }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="优先级">
            <Tag :color="priorityColorMap[currentApproval.priority || 'MEDIUM']">
              {{ currentApproval.priorityName || '中' }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="紧急程度">
            <Tag v-if="currentApproval.urgency === 'URGENT'" color="red">紧急</Tag>
            <span v-else>普通</span>
          </DescriptionsItem>
          <DescriptionsItem v-if="currentApproval.approvedAt" label="审批时间" :span="2">
            {{ formatTime(currentApproval.approvedAt) }}
          </DescriptionsItem>
          <DescriptionsItem v-if="currentApproval.comment" label="审批意见" :span="2">
            {{ currentApproval.comment }}
          </DescriptionsItem>
        </Descriptions>

        <div style="margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap;">
          <Button type="link" @click="goToBusinessDetail(currentApproval)" style="padding: 0;">
            📄 查看业务详情 →
          </Button>
          <!-- 合同变更审批单预览 -->
          <Button 
            v-if="isContractChangeApproval(currentApproval)"
            type="link" 
            @click="handlePreviewChangeApprovalForm"
            style="padding: 0; color: #1890ff;"
          >
            📋 预览变更审批单
          </Button>
        </div>

        <!-- 待审批时显示审批操作区域 -->
        <template v-if="activeTab === 'pending' && currentApproval.status === 'PENDING'">
          <Form layout="vertical">
            <FormItem label="审批意见">
              <Textarea
                v-model:value="approvalComment"
                placeholder="请输入审批意见（拒绝时必填）"
                :rows="3"
                :maxlength="500"
                showCount
              />
            </FormItem>
          </Form>
          <div style="display: flex; justify-content: flex-end; gap: 12px;">
            <Button @click="detailModalVisible = false">取消</Button>
            <Button danger :loading="approvalLoading" @click="handleModalReject">拒绝</Button>
            <Button type="primary" :loading="approvalLoading" @click="handleModalApprove">通过</Button>
          </div>
        </template>
        
        <!-- 非待审批状态显示关闭按钮 -->
        <template v-else>
          <div style="display: flex; justify-content: flex-end;">
            <Button @click="detailModalVisible = false">关闭</Button>
          </div>
        </template>
      </template>
    </Modal>

    <!-- 拒绝审批弹窗 -->
    <Modal
      v-model:open="rejectModalVisible"
      title="拒绝审批"
      :width="500"
      @ok="confirmReject"
      okText="拒绝"
      okType="danger"
      cancelText="取消"
    >
      <p style="margin-bottom: 12px;">
        确定要拒绝「{{ rejectingRecord?.businessTitle }}」的审批吗？
      </p>
      <Form layout="vertical">
        <FormItem label="拒绝原因（必填）">
          <Textarea
            v-model:value="rejectReason"
            placeholder="请输入拒绝原因"
            :rows="3"
            :maxlength="500"
            showCount
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 批量拒绝弹窗 -->
    <Modal
      v-model:open="batchRejectModalVisible"
      title="批量拒绝"
      :width="500"
      @ok="confirmBatchReject"
      okText="拒绝"
      okType="danger"
      cancelText="取消"
    >
      <p style="margin-bottom: 12px;">
        确定要批量拒绝选中的 {{ selectedRowKeys.length }} 条审批吗？
      </p>
      <Form layout="vertical">
        <FormItem label="拒绝原因（必填）">
          <Textarea
            v-model:value="batchRejectReason"
            placeholder="请输入拒绝原因"
            :rows="3"
            :maxlength="500"
            showCount
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-badge-count) {
  font-size: 12px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
}
</style>

