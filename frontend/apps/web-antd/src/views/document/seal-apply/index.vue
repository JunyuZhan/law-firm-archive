<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Row,
  Col,
  Popconfirm,
  Tag,
  Tabs,
  InputNumber,
  Textarea,
} from 'ant-design-vue';
import {
  getSealApplicationList,
  createSealApplication,
  approveSealApplication,
  rejectSealApplication,
  registerSealUsage,
  cancelSealApplication,
  getPendingSealApplications,
  getSealApplicationApprovers,
  getPendingForKeeper,
  getProcessedByKeeper,
  checkIsKeeper,
} from '#/api/document/seal';
import { getSealList } from '#/api/document/seal';
import {
  getPendingApprovals,
  approveApproval,
  type ApprovalDTO,
} from '#/api/workbench';
import { useRouter } from 'vue-router';
import { useUserStore } from '@vben/stores';
import { usePermission } from '#/hooks/usePermission';
import type {
  SealApplicationDTO,
  SealApplicationQuery,
  CreateSealApplicationCommand,
  SealDTO,
} from '#/api/document/seal-types';

defineOptions({ name: 'DocumentSealApply' });

const router = useRouter();
const userStore = useUserStore();
const { hasPermission } = usePermission();

// 用户角色和权限
const isKeeper = ref(false); // 是否是印章保管人
const hasApprovalPermission = ref(false); // 是否有审批权限
const hasManagePermission = ref(false); // 是否有印章管理权限（doc:seal:list）
const isApplicant = ref(true); // 是否是申请人（所有用户都可以申请）

// 状态
const loading = ref(false);
const dataSource = ref<SealApplicationDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const formRef = ref();
const activeTab = ref('my');
const seals = ref<SealDTO[]>([]);
const approveModalVisible = ref(false);
const useModalVisible = ref(false);
const currentApplication = ref<SealApplicationDTO | null>(null);
const approveComment = ref('');
// 审批中心待审批列表（用于"待审批"tab）
const pendingApprovals = ref<ApprovalDTO[]>([]);
const pendingApprovalsLoading = ref(false);
// 拒绝审批相关
const rejectModalVisible = ref(false);
const rejectReason = ref('');
const currentApprovalId = ref<number | null>(null);
// 可选审批人列表
const approvers = ref<Array<{ id: number; realName: string; departmentName: string; position: string }>>([]);
const approversLoading = ref(false);

// 查询参数
const queryParams = reactive<SealApplicationQuery>({
  pageNum: 1,
  pageSize: 10,
  applicationNo: undefined,
  sealId: undefined,
  applicantId: undefined,
  status: undefined,
});

// 表单数据
const formData = reactive<CreateSealApplicationCommand>({
  sealId: 0,
  documentName: '',
  documentType: 'CONTRACT',
  copies: 1,
  usePurpose: '',
  matterId: undefined,
  expectedUseDate: undefined,
  approverId: 0, // 审批人ID（必填）
});

// 表格列
const columns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo', width: 150 },
  { title: '印章名称', dataIndex: 'sealName', key: 'sealName', width: 120 },
  { title: '用印文件', dataIndex: 'documentName', key: 'documentName', width: 180, ellipsis: true },
  { title: '用印目的', dataIndex: 'usePurpose', key: 'usePurpose', width: 150, ellipsis: true },
  { title: '份数', dataIndex: 'copies', key: 'copies', width: 80 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '申请时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已用印', value: 'USED' },
  { label: '已取消', value: 'CANCELLED' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    if (activeTab.value === 'pending') {
      // 待审批 - 从审批中心获取
      await fetchPendingApprovals();
      return;
    } else if (activeTab.value === 'my') {
      // 我的申请 - 查询当前用户的申请
      queryParams.applicantId = userStore.userInfo?.userId;
      queryParams.status = undefined;
      const res = await getSealApplicationList(queryParams);
      dataSource.value = res.list;
      total.value = res.total;
    } else if (activeTab.value === 'keeper-pending') {
      // 保管人待办理 - 审批通过且我是保管人
      const res = await getPendingForKeeper();
      dataSource.value = res;
      total.value = res.length;
    } else if (activeTab.value === 'keeper-processed') {
      // 保管人已办理 - 已用印且我是保管人
      const res = await getProcessedByKeeper();
      dataSource.value = res;
      total.value = res.length;
    } else if (activeTab.value === 'my-approvals') {
      // 我的审批 - 查询我审批过的申请
      // 这里需要从审批中心获取，暂时使用全部申请过滤
      queryParams.status = undefined;
      const res = await getSealApplicationList(queryParams);
      // 需要根据审批记录过滤，暂时显示全部
      dataSource.value = res.list;
      total.value = res.total;
    } else {
      // 全部申请
      queryParams.applicantId = undefined;
      queryParams.status = undefined;
      const res = await getSealApplicationList(queryParams);
      dataSource.value = res.list;
      total.value = res.total;
    }
  } catch (error: any) {
    message.error(error.message || '加载申请列表失败');
  } finally {
    loading.value = false;
  }
}

// 从审批中心加载待审批列表
async function fetchPendingApprovals() {
  pendingApprovalsLoading.value = true;
  try {
    const allPending = await getPendingApprovals();
    // 筛选出用印申请的审批
    pendingApprovals.value = allPending.filter(
      (item) => item.businessType === 'SEAL_APPLICATION'
    );
    // 转换为SealApplicationDTO格式显示
    dataSource.value = pendingApprovals.value.map((approval) => ({
      id: approval.businessId,
      applicationNo: approval.businessNo || approval.approvalNo,
      documentName: approval.businessTitle || '',
      status: approval.status,
      statusName: approval.statusName || '',
      applicantName: approval.applicantName || '',
      createdAt: approval.createdAt,
      // 审批相关信息
      approvalId: approval.id,
      approverName: approval.approverName,
    })) as any;
    total.value = pendingApprovals.value.length;
  } catch (error: any) {
    message.error(error.message || '加载待审批列表失败');
  } finally {
    pendingApprovalsLoading.value = false;
  }
}

// 加载印章列表
async function loadSeals() {
  try {
    const res = await getSealList({ pageNum: 1, pageSize: 1000, status: 'ACTIVE' });
    seals.value = res.list;
  } catch (error: any) {
    console.error('加载印章列表失败:', error);
  }
}

// 加载可选审批人列表
async function loadApprovers() {
  approversLoading.value = true;
  try {
    const res = await getSealApplicationApprovers();
    approvers.value = res;
  } catch (error: any) {
    console.error('加载审批人列表失败:', error);
    message.error('加载审批人列表失败');
  } finally {
    approversLoading.value = false;
  }
}

// Tab切换
function handleTabChange(key: string) {
  activeTab.value = key;
  queryParams.pageNum = 1;
  fetchData();
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryParams.applicationNo = undefined;
  queryParams.sealId = undefined;
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  fetchData();
}

// 新增申请
async function handleAdd() {
  Object.assign(formData, {
    sealId: 0,
    documentName: '',
    documentType: 'CONTRACT',
    copies: 1,
    usePurpose: '',
    matterId: undefined,
    expectedUseDate: undefined,
    approverId: 0,
  });
  // 加载可选审批人列表
  await loadApprovers();
  modalVisible.value = true;
}

// 保存申请
async function handleSave() {
  try {
    await formRef.value?.validate();
    
    if (!formData.sealId) {
      message.error('请选择印章');
      return;
    }
    
    if (!formData.approverId) {
      message.error('请选择审批人');
      return;
    }
    
    await createSealApplication(formData);
    message.success('申请提交成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    if (error?.errorFields) {
      return;
    }
    message.error(error.message || '提交失败');
  }
}

// 审批通过
function handleApprove(record: SealApplicationDTO) {
  // 如果是从审批中心获取的待审批列表，使用审批中心接口
  if (activeTab.value === 'pending' && (record as any).approvalId) {
    handleApproveFromCenter(record as any, true);
    return;
  }
  
  currentApplication.value = record;
  approveComment.value = '';
  approveModalVisible.value = true;
}

// 从审批中心审批
async function handleApproveFromCenter(record: any, approved: boolean) {
  const approvalId = record.approvalId;
  if (!approvalId) {
    message.error('审批记录不存在');
    return;
  }
  
  if (!approved) {
    // 拒绝需要填写原因 - 打开拒绝弹窗
    currentApprovalId.value = approvalId;
    rejectReason.value = '';
    rejectModalVisible.value = true;
    return;
  }
  
  // 通过审批
  Modal.confirm({
    title: '确认通过',
    content: `确定要通过「${record.documentName || record.applicationNo}」的审批吗？`,
    okText: '通过',
    okType: 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await approveApproval({
          approvalId,
          result: 'APPROVED',
          comment: '',
        });
        message.success('审批通过');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '审批失败');
      }
    },
  });
}

// 确认拒绝审批
async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请填写拒绝原因');
    return;
  }
  
  if (!currentApprovalId.value) {
    message.error('审批记录不存在');
    return;
  }
  
  try {
    await approveApproval({
      approvalId: currentApprovalId.value,
      result: 'REJECTED',
      comment: rejectReason.value.trim(),
    });
    message.success('已拒绝');
    rejectModalVisible.value = false;
    rejectReason.value = '';
    currentApprovalId.value = null;
    fetchData();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 保存审批（用于直接审批接口，保留向后兼容）
async function handleApproveSave(approved: boolean) {
  try {
    if (currentApplication.value) {
      if (approved) {
        await approveSealApplication(currentApplication.value.id, approveComment.value);
        message.success('审批通过');
      } else {
        await rejectSealApplication(currentApplication.value.id, approveComment.value);
        message.success('已拒绝');
      }
      approveModalVisible.value = false;
      fetchData();
    }
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 跳转到审批中心
function goToApprovalCenter() {
  router.push('/workbench/approval');
}

// 登记用印
function handleUse(record: SealApplicationDTO) {
  currentApplication.value = record;
  useModalVisible.value = true;
}

// 保存用印
async function handleUseSave() {
  try {
    if (currentApplication.value) {
      await registerSealUsage(currentApplication.value.id, approveComment.value);
      message.success('用印登记成功');
      useModalVisible.value = false;
      fetchData();
    }
  } catch (error: any) {
    message.error(error.message || '登记失败');
  }
}

// 取消申请
function handleCancel(record: SealApplicationDTO) {
  Modal.confirm({
    title: '确认取消',
    content: `确定要取消申请 "${record.applicationNo}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await cancelSealApplication(record.id);
        message.success('取消成功');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '取消失败');
      }
    },
  });
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'blue',
    REJECTED: 'red',
    USED: 'green',
    CANCELLED: 'default',
  };
  return colorMap[status] || 'default';
}

// 检查用户角色和权限
async function checkUserRole() {
  try {
    // 检查是否是保管人
    isKeeper.value = await checkIsKeeper();
    // 检查是否有审批权限
    hasApprovalPermission.value = hasPermission('approval:approve');
    // 检查是否有印章管理权限（用于显示"全部申请"tab）
    hasManagePermission.value = hasPermission('doc:seal:list');
  } catch (error: any) {
    console.error('检查用户角色失败:', error);
  }
}

onMounted(async () => {
  await checkUserRole();
  // 根据权限设置默认tab（如果当前tab不可见，切换到"我的申请"）
  if (activeTab.value === 'pending' && !hasApprovalPermission.value) {
    activeTab.value = 'my';
  } else if (activeTab.value === 'all' && !hasManagePermission.value) {
    activeTab.value = 'my';
  } else if ((activeTab.value === 'keeper-pending' || activeTab.value === 'keeper-processed') && !isKeeper.value) {
    activeTab.value = 'my';
  }
  await fetchData();
  await loadSeals();
});
</script>

<template>
  <Page title="用印申请" description="管理用印申请流程">
    <Card>
      <!-- Tab切换 -->
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="my" tab="我的申请" />
        <Tabs.TabPane v-if="hasApprovalPermission" key="pending" tab="待审批" />
        <Tabs.TabPane v-if="isKeeper" key="keeper-pending" tab="保管人待办理" />
        <Tabs.TabPane v-if="isKeeper" key="keeper-processed" tab="保管人已办理" />
        <Tabs.TabPane v-if="hasManagePermission" key="all" tab="全部申请" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :span="6">
            <Input
              v-model:value="queryParams.applicationNo"
              placeholder="申请编号"
              allowClear
            />
          </Col>
          <Col :span="6">
            <Select
              v-model:value="queryParams.sealId"
              placeholder="印章"
              allowClear
              showSearch
              style="width: 100%"
              :filterOption="(input: string, option: any) => (option?.label || '').toLowerCase().includes(input.toLowerCase())"
              :options="seals.map(s => ({ label: s.name, value: s.id }))"
            />
          </Col>
          <Col :span="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allowClear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :span="6">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">新建申请</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 待审批tab提示 -->
      <div v-if="activeTab === 'pending' && hasApprovalPermission" style="margin-bottom: 16px">
        <Button type="link" @click="goToApprovalCenter">
          前往审批中心统一处理 →
        </Button>
        <span style=" margin-left: 8px;color: #999">
          建议在审批中心统一处理所有待审批事项，支持批量审批
        </span>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="activeTab === 'pending' ? pendingApprovalsLoading : loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => {
            queryParams.pageNum = page;
            queryParams.pageSize = size;
            fetchData();
          },
        }"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="getStatusColor((record as SealApplicationDTO).status)">
              {{ (record as SealApplicationDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <template v-if="(record as SealApplicationDTO).status === 'PENDING'">
                <template v-if="activeTab === 'pending'">
                  <a @click="handleApprove(record as SealApplicationDTO)">审批</a>
                </template>
                <template v-else-if="activeTab === 'my'">
                  <a @click="handleCancel(record as SealApplicationDTO)">取消</a>
                </template>
              </template>
              <template v-if="(record as SealApplicationDTO).status === 'APPROVED'">
                <template v-if="activeTab === 'keeper-pending' || activeTab === 'keeper-processed'">
                  <a @click="handleUse(record as SealApplicationDTO)">登记用印</a>
                </template>
                <template v-else-if="activeTab === 'my'">
                  <span style="color: #999">等待保管人登记用印</span>
                </template>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新建申请弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建用印申请"
      width="600px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="印章" name="sealId" :rules="[{ required: true, message: '请选择印章' }]">
          <Select
            v-model:value="formData.sealId"
            placeholder="请选择印章"
            showSearch
            style="width: 100%"
            :filterOption="(input: string, option: any) => (option?.label || '').toLowerCase().includes(input.toLowerCase())"
            :options="seals.map(s => ({ label: s.name, value: s.id }))"
          />
        </FormItem>
        <FormItem label="用印文件" name="documentName" :rules="[{ required: true, message: '请输入用印文件名称' }]">
          <Input v-model:value="formData.documentName" placeholder="请输入用印文件名称" />
        </FormItem>
        <FormItem label="文件类型" name="documentType">
          <Select v-model:value="formData.documentType" placeholder="请选择文件类型">
            <Select.Option value="CONTRACT">合同</Select.Option>
            <Select.Option value="AGREEMENT">协议</Select.Option>
            <Select.Option value="LETTER">函件</Select.Option>
            <Select.Option value="CERTIFICATE">证明</Select.Option>
            <Select.Option value="OTHER">其他</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="用印目的" name="usePurpose">
          <Textarea v-model:value="formData.usePurpose" :rows="2" placeholder="请输入用印目的" />
        </FormItem>
        <FormItem label="用印份数" name="copies">
          <InputNumber v-model:value="formData.copies" :min="1" style="width: 100%" />
        </FormItem>
        <FormItem label="审批人" name="approverId" :rules="[{ required: true, message: '请选择审批人' }]">
          <Select
            v-model:value="formData.approverId"
            placeholder="请选择审批人"
            showSearch
            :loading="approversLoading"
            style="width: 100%"
            :filterOption="(input: string, option: any) => {
              const label = option?.label || '';
              return label.toLowerCase().includes(input.toLowerCase());
            }"
            :options="approvers.map(a => ({
              label: `${a.realName}（${a.departmentName} - ${a.position}）`,
              value: a.id,
            }))"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 审批弹窗 -->
    <Modal
      v-model:open="approveModalVisible"
      title="审批用印申请"
      width="500px"
      @ok="handleApproveSave(true)"
    >
      <div style="margin-bottom: 16px">
        <p><strong>申请编号：</strong>{{ currentApplication?.applicationNo }}</p>
        <p><strong>印章名称：</strong>{{ currentApplication?.sealName }}</p>
        <p><strong>用印文件：</strong>{{ currentApplication?.documentName }}</p>
        <p><strong>用印目的：</strong>{{ currentApplication?.usePurpose || '-' }}</p>
        <p><strong>用印份数：</strong>{{ currentApplication?.copies }}</p>
      </div>
      <FormItem label="审批意见">
        <Textarea v-model:value="approveComment" :rows="3" placeholder="请输入审批意见（可选）" />
      </FormItem>
      <template #footer>
        <Space>
          <Button @click="approveModalVisible = false">取消</Button>
          <Button danger @click="handleApproveSave(false)">拒绝</Button>
          <Button type="primary" @click="handleApproveSave(true)">通过</Button>
        </Space>
      </template>
    </Modal>

    <!-- 登记用印弹窗 -->
    <Modal
      v-model:open="useModalVisible"
      title="登记用印"
      width="500px"
      @ok="handleUseSave"
    >
      <div style="margin-bottom: 16px">
        <p><strong>申请编号：</strong>{{ currentApplication?.applicationNo }}</p>
        <p><strong>印章名称：</strong>{{ currentApplication?.sealName }}</p>
        <p><strong>用印文件：</strong>{{ currentApplication?.documentName }}</p>
        <p><strong>用印份数：</strong>{{ currentApplication?.copies }}</p>
      </div>
      <FormItem label="用印备注">
        <Textarea v-model:value="approveComment" :rows="3" placeholder="请输入用印备注（可选）" />
      </FormItem>
    </Modal>

    <!-- 拒绝审批弹窗（用于审批中心） -->
    <Modal
      v-model:open="rejectModalVisible"
      title="拒绝审批"
      width="500px"
      @ok="handleRejectConfirm"
      okText="拒绝"
      okType="danger"
    >
      <div style="margin-bottom: 16px">
        <p style=" margin-bottom: 8px;color: #999;">请填写拒绝原因（必填）：</p>
        <Textarea
          v-model:value="rejectReason"
          :rows="4"
          placeholder="请输入拒绝原因"
          :maxlength="500"
          show-count
        />
      </div>
    </Modal>
  </Page>
</template>
