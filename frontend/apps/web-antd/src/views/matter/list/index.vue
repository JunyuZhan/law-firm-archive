<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { ClientDTO } from '#/api/client/types';
import type {
  CreateMatterCommand,
  MatterDTO,
  UpdateMatterCommand,
} from '#/api/matter/types';
import type { DepartmentDTO } from '#/api/system/types';

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Cascader,
  Col,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  SelectOption,
  Space,
  Tag,
  Textarea,
  TreeSelect,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getClientList } from '#/api/client';
import {
  createContract,
  createMatter,
  createMatterFromContract,
  getApprovedContracts,
  getMatterDetail,
  getMatterList,
  updateMatter,
} from '#/api/matter';
import { getDepartmentTreePublic, getDictDataByCode } from '#/api/system';
import { UserTreeSelect } from '#/components/UserTreeSelect';
import {
  causesToCascaderOptions,
  getCaseCategoryByMatterType,
  getCausesByType,
  getCauseTypeByCase,
  MATTER_TYPE_OPTIONS,
  needsCauseOfAction,
} from '#/constants/causes';

defineOptions({ name: 'MatterList' });

const router = useRouter();
const route = useRoute();

// 获取当前年份
const currentYear = new Date().getFullYear();

// 年份选项（最近5年 + 全部）
const yearOptions = [
  { label: '全部年份', value: 0 },
  ...Array.from({ length: 5 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i,
  })),
];

// 状态
const modalVisible = ref(false);
const modalTitle = ref('新增项目');
const formRef = ref();
const clients = ref<ClientDTO[]>([]);
const departments = ref<DepartmentDTO[]>([]);

// 合同相关
const contractModalVisible = ref(false);
const contractFormRef = ref();
const approvedContracts = ref<any[]>([]);
const createFromContract = ref(false);
const selectedContractId = ref<number | undefined>(undefined);
const contractFormData = reactive({
  name: '',
  clientId: undefined as number | undefined,
  contractType: 'SERVICE',
  feeType: 'FIXED',
  totalAmount: undefined as number | undefined,
  currency: 'CNY',
  signDate: undefined as string | undefined,
  effectiveDate: undefined as string | undefined,
  expiryDate: undefined as string | undefined,
  signerId: undefined as number | undefined,
  departmentId: undefined as number | undefined,
  paymentTerms: '',
  remark: '',
});

// 查询参数已由 useVbenVxeGrid 管理

// 客户角色选项
const clientRoleOptions = [
  { label: '原告', value: 'PLAINTIFF' },
  { label: '被告', value: 'DEFENDANT' },
  { label: '第三人', value: 'THIRD_PARTY' },
  { label: '申请人', value: 'APPLICANT' },
  { label: '被申请人', value: 'RESPONDENT' },
];

// 多客户选择数据
interface SelectedClient {
  clientId: number | undefined;
  clientRole: string;
  isPrimary: boolean;
}
const selectedClients = ref<SelectedClient[]>([
  { clientId: undefined, clientRole: 'PLAINTIFF', isPrimary: true },
]);

// 代理阶段选项 - 从后端字典加载
const litigationStageOptions = ref<{ label: string; value: string }[]>([]);
const litigationStageLoading = ref(false);

// 案件类型与字典编码的映射
const caseTypeToDictCode: Record<string, string> = {
  CIVIL: 'litigation_stage_civil',
  CRIMINAL: 'litigation_stage_criminal',
  ADMINISTRATIVE: 'litigation_stage_administrative',
  LABOR_ARBITRATION: 'litigation_stage_labor_arbitration',
  COMMERCIAL_ARBITRATION: 'litigation_stage_commercial_arbitration',
  ENFORCEMENT: 'litigation_stage_enforcement',
};

// 加载代理阶段选项
async function loadLitigationStageOptions(caseType: string | undefined) {
  if (!caseType) {
    litigationStageOptions.value = [];
    return;
  }
  const dictCode = caseTypeToDictCode[caseType] || 'litigation_stage_default';
  litigationStageLoading.value = true;
  try {
    const items = await getDictDataByCode(dictCode);
    litigationStageOptions.value = items.map((item) => ({
      label: item.label,
      value: item.value,
    }));
  } catch (error) {
    console.warn('加载代理阶段字典失败:', error);
    litigationStageOptions.value = [];
  } finally {
    litigationStageLoading.value = false;
  }
}

// 表单数据
const formData = reactive<Partial<CreateMatterCommand> & { id?: number }>({
  id: undefined,
  name: '',
  matterType: 'LITIGATION',
  caseType: undefined,
  litigationStage: undefined,
  causeOfAction: undefined,
  businessType: undefined,
  clientId: undefined,
  opposingParty: '',
  opposingLawyerName: '',
  opposingLawyerLicenseNo: '',
  opposingLawyerFirm: '',
  opposingLawyerPhone: '',
  opposingLawyerEmail: '',
  description: '',
  originatorId: undefined,
  leadLawyerId: undefined,
  departmentId: undefined,
  feeType: 'HOURLY',
  estimatedFee: undefined,
  filingDate: undefined,
  expectedClosingDate: undefined,
  claimAmount: undefined,
  contractId: undefined,
  remark: '',
});

// 案由级联选择值
const causeValue = ref<string[]>([]);

// 根据项目大类获取案件类型选项
const caseTypeOptions = computed(() => {
  return getCaseCategoryByMatterType(formData.matterType || 'LITIGATION');
});

// 是否显示案由选择
const showCauseSelect = computed(() => {
  return formData.caseType && needsCauseOfAction(formData.caseType);
});

// 案由选项
const causeOptions = computed(() => {
  if (!formData.caseType) return [];
  const causeType = getCauseTypeByCase(formData.caseType);
  if (!causeType) return [];
  const causes = getCausesByType(causeType);
  return causesToCascaderOptions(causes);
});

// 监听项目大类变化，清空案件类型和案由
watch(
  () => formData.matterType,
  () => {
    formData.caseType = undefined;
    formData.causeOfAction = undefined;
    causeValue.value = [];
  },
);

// 监听案件类型变化，清空案由和代理阶段，并重新加载代理阶段选项
watch(
  () => formData.caseType,
  (newCaseType) => {
    formData.causeOfAction = undefined;
    formData.litigationStage = undefined;
    causeValue.value = [];
    // 从字典加载代理阶段选项
    loadLitigationStageOptions(newCaseType);
  },
);

// 监听案由级联选择变化
watch(causeValue, (val) => {
  formData.causeOfAction =
    val && val.length > 0 ? val[val.length - 1] : undefined;
});

// ==================== 常量选项 ====================

// 项目类型选项（使用常量）
const matterTypeOptions = MATTER_TYPE_OPTIONS;

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待处理', value: 'PENDING' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已结案', value: 'CLOSED' },
  { label: '已暂停', value: 'SUSPENDED' },
];

// 收费类型选项
const feeTypeOptions = [
  { label: '计时收费', value: 'HOURLY' },
  { label: '固定收费', value: 'FIXED' },
  { label: '风险代理', value: 'CONTINGENCY' },
  { label: '混合收费', value: 'MIXED' },
];

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'year',
    label: '创建年份',
    component: 'Select',
    defaultValue: currentYear,
    componentProps: {
      placeholder: '请选择年份',
      options: yearOptions,
    },
  },
  {
    fieldName: 'matterNo',
    label: '项目编号',
    component: 'Input',
    componentProps: {
      placeholder: '请输入项目编号',
      allowClear: true,
    },
  },
  {
    fieldName: 'name',
    label: '项目名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入项目名称',
      allowClear: true,
    },
  },
  {
    fieldName: 'matterType',
    label: '项目类型',
    component: 'Select',
    componentProps: {
      placeholder: '请选择项目类型',
      allowClear: true,
      options: MATTER_TYPE_OPTIONS,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: statusOptions.filter((o) => o.value !== undefined),
    },
  },
  {
    fieldName: 'clientId',
    label: '客户',
    component: 'Select',
    componentProps: {
      placeholder: '请选择客户',
      allowClear: true,
      showSearch: true,
      filterOption: (input: string, option: any) =>
        (option?.label || '').toLowerCase().includes(input.toLowerCase()),
      options: computed(() =>
        clients.value.map((c) => ({
          label: c.clientNo ? `[${c.clientNo}] ${c.name}` : c.name,
          value: c.id,
        })),
      ),
    },
  },
  {
    fieldName: 'leadLawyerId',
    label: '主办律师',
    component: 'UserTreeSelect',
    componentProps: {
      placeholder: '选择主办律师',
    },
  },
];

// ==================== 表格配置 ====================

const gridColumns = [
  { title: '类型', field: 'matterTypeName', width: 100 },
  { title: '案件类型', field: 'caseTypeName', width: 100 },
  { title: '合同编号', field: 'contractNo', width: 130 },
  { title: '客户', field: 'clientName', width: 150 },
  { title: '项目编号', field: 'matterNo', width: 130 },
  { title: '主办律师', field: 'leadLawyerName', width: 100 },
  {
    title: '合同金额',
    field: 'contractAmount',
    width: 120,
    slots: { default: 'contractAmount' },
  },
  { title: '创建时间', field: 'createdAt', width: 160 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  {
    title: '操作',
    field: 'action',
    width: 200,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  // 处理年份筛选参数
  let createdAtFrom: string | undefined;
  let createdAtTo: string | undefined;
  const year = params.year;
  if (year && year !== 0) {
    createdAtFrom = `${year}-01-01T00:00:00`;
    createdAtTo = `${year}-12-31T23:59:59`;
  }

  const res = await getMatterList({
    pageNum: params.page,
    pageSize: params.pageSize,
    matterNo: params.matterNo,
    name: params.name,
    matterType: params.matterType,
    clientId: params.clientId,
    status: params.status,
    leadLawyerId: params.leadLawyerId,
    createdAtFrom,
    createdAtTo,
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
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// 加载选项数据 - 每个API独立处理错误，避免一个失败导致全部失败
async function loadOptions() {
  // 客户列表
  try {
    const clientRes = await getClientList({ pageNum: 1, pageSize: 1000 });
    clients.value = clientRes?.list || [];
  } catch (error: any) {
    // 403权限错误静默处理，其他错误记录警告
    if (error?.response?.status !== 403) {
      console.warn('加载客户列表失败', error);
    }
    clients.value = [];
  }

  // 部门树 - 使用公共接口，无需特殊权限
  try {
    const deptRes = await getDepartmentTreePublic();
    departments.value = deptRes || [];
  } catch (error: any) {
    // 错误静默处理
    const status = error?.response?.status || error?.status;
    if (status !== 403) {
      console.warn('加载部门树失败', error);
    }
    departments.value = [];
  }
}

// 搜索和重置已由 useVbenVxeGrid 处理

// 创建合同
async function handleCreateContract() {
  contractModalVisible.value = true;
  // 加载已审批的合同列表（用于参考）
  try {
    const res = await getApprovedContracts();
    approvedContracts.value = res;
  } catch (error: any) {
    console.error('加载已审批合同失败', error);
  }
}

// 保存合同
async function handleSaveContract() {
  try {
    await contractFormRef.value?.validate();
    const contractData = {
      name: contractFormData.name,
      clientId: contractFormData.clientId,
      contractType: contractFormData.contractType,
      feeType: contractFormData.feeType,
      totalAmount: contractFormData.totalAmount,
      currency: contractFormData.currency || 'CNY',
      signDate: contractFormData.signDate,
      effectiveDate: contractFormData.effectiveDate,
      expiryDate: contractFormData.expiryDate,
      signerId: contractFormData.signerId,
      departmentId: contractFormData.departmentId,
      paymentTerms: contractFormData.paymentTerms,
      remark: contractFormData.remark,
    };
    await createContract(contractData);
    message.success('合同创建成功，请提交审批');
    contractModalVisible.value = false;
    // 重置表单
    Object.assign(contractFormData, {
      name: '',
      clientId: undefined,
      contractType: 'SERVICE',
      feeType: 'FIXED',
      totalAmount: undefined,
      currency: 'CNY',
      signDate: undefined,
      effectiveDate: undefined,
      expiryDate: undefined,
      signerId: undefined,
      departmentId: undefined,
      paymentTerms: '',
      remark: '',
    });
  } catch (error: any) {
    if (error?.errorFields) {
      return;
    }
    message.error(error.message || '创建合同失败');
  }
}

// 新增项目（必须从合同创建）
async function handleAdd() {
  // 先加载已审批的合同列表
  try {
    const res = await getApprovedContracts();
    approvedContracts.value = res;
    if (res.length === 0) {
      message.warning('暂无可用的已审批合同，请先创建并审批合同');
      return;
    }

    modalTitle.value = '创建项目';
    createFromContract.value = true; // 必须从合同创建
    selectedContractId.value = undefined;
    causeValue.value = [];
    resetSelectedClients();
    Object.assign(formData, {
      id: undefined,
      name: '',
      matterType: 'LITIGATION',
      caseType: undefined,
      causeOfAction: undefined,
      businessType: undefined,
      clientId: undefined,
      opposingParty: '',
      opposingLawyerName: '',
      opposingLawyerLicenseNo: '',
      opposingLawyerFirm: '',
      opposingLawyerPhone: '',
      opposingLawyerEmail: '',
      description: '',
      originatorId: undefined,
      leadLawyerId: undefined,
      departmentId: undefined,
      feeType: 'HOURLY',
      estimatedFee: undefined,
      filingDate: undefined,
      expectedClosingDate: undefined,
      claimAmount: undefined,
      contractId: undefined,
      remark: '',
    });
    modalVisible.value = true;
  } catch (error: any) {
    message.error(`加载合同列表失败：${error.message || ''}`);
  }
}

// 编辑
function handleEdit(record: MatterDTO) {
  modalTitle.value = '编辑项目';
  // 设置案由级联值
  causeValue.value = record.causeOfAction ? [record.causeOfAction] : [];
  // 加载代理阶段选项（编辑时需要先加载选项，再填充数据）
  loadLitigationStageOptions(record.caseType);

  // 加载多客户数据
  if (record.clients && record.clients.length > 0) {
    selectedClients.value = record.clients.map((c) => ({
      clientId: c.clientId,
      clientRole: c.clientRole || 'PLAINTIFF',
      isPrimary: c.isPrimary || false,
    }));
  } else {
    // 向后兼容：如果没有clients列表，使用单个clientId
    selectedClients.value = [
      {
        clientId: record.clientId,
        clientRole: 'PLAINTIFF',
        isPrimary: true,
      },
    ];
  }

  Object.assign(formData, {
    id: record.id,
    name: record.name,
    matterType: record.matterType,
    caseType: record.caseType,
    litigationStage: record.litigationStage,
    causeOfAction: record.causeOfAction,
    businessType: record.businessType,
    clientId: record.clientId,
    opposingParty: record.opposingParty,
    opposingLawyerName: record.opposingLawyerName,
    opposingLawyerLicenseNo: record.opposingLawyerLicenseNo,
    opposingLawyerFirm: record.opposingLawyerFirm,
    opposingLawyerPhone: record.opposingLawyerPhone,
    opposingLawyerEmail: record.opposingLawyerEmail,
    description: record.description,
    originatorId: record.originatorId,
    leadLawyerId: record.leadLawyerId,
    departmentId: record.departmentId,
    feeType: record.feeType,
    estimatedFee: record.estimatedFee,
    filingDate: record.filingDate,
    expectedClosingDate: record.expectedClosingDate,
    claimAmount: record.claimAmount,
    contractId: record.contractId,
    remark: record.remark,
  });
  modalVisible.value = true;
}

// 保存
async function handleSave() {
  try {
    await formRef.value?.validate();

    // 验证客户选择
    const hasEmptyClient = selectedClients.value.some((c) => !c.clientId);
    if (hasEmptyClient || selectedClients.value.length === 0) {
      message.error('请完整填写客户信息');
      return;
    }

    // 构建客户列表数据
    const clientsData = selectedClients.value.map((c) => ({
      clientId: c.clientId!,
      clientRole: c.clientRole,
      isPrimary: c.isPrimary,
    }));

    // 设置主要客户ID（向后兼容）
    const primaryClient = selectedClients.value.find((c) => c.isPrimary);
    const primaryClientId =
      primaryClient?.clientId || selectedClients.value[0]?.clientId;

    if (formData.id) {
      const updateData: UpdateMatterCommand = {
        id: formData.id,
        ...formData,
        clientId: primaryClientId!,
        clients: clientsData,
      } as UpdateMatterCommand;
      await updateMatter(updateData);
      message.success('更新成功');
    } else {
      // 如果是从合同创建
      if (createFromContract.value && selectedContractId.value) {
        const createData: CreateMatterCommand = {
          ...formData,
          clientId: primaryClientId!,
          clients: clientsData,
        } as CreateMatterCommand;
        await createMatterFromContract(selectedContractId.value, createData);
        message.success('基于合同创建项目成功');
      } else {
        const createData: CreateMatterCommand = {
          ...formData,
          clientId: primaryClientId!,
          clients: clientsData,
        } as CreateMatterCommand;
        await createMatter(createData);
        message.success('创建成功');
      }
    }
    modalVisible.value = false;
    resetSelectedClients();
    gridApi.reload();
  } catch (error: any) {
    if (error?.errorFields) {
      // 表单验证失败
      return;
    }
    message.error(error.message || '操作失败');
  }
}

// 选择合同后自动填充项目信息
function handleContractSelect(contractId: number) {
  const contract = approvedContracts.value.find((c) => c.id === contractId);
  if (contract) {
    // 自动填充合同中的所有相关信息
    formData.name = contract.name || '';
    formData.clientId = Number(contract.clientId);
    formData.feeType = contract.feeType;
    formData.estimatedFee = contract.totalAmount;
    formData.departmentId = contract.departmentId
      ? Number(contract.departmentId)
      : undefined;
    formData.contractId = contractId;
    selectedContractId.value = contractId;

    // 填充案件类型和案由
    formData.matterType =
      contract.contractType === 'LITIGATION' ? 'LITIGATION' : 'NON_LITIGATION';

    if (contract.caseType) {
      formData.caseType = contract.caseType;
    }

    if (contract.causeOfAction) {
      formData.causeOfAction = contract.causeOfAction;
      causeValue.value = [contract.causeOfAction];
    }

    // 填充对方当事人信息
    if (contract.opposingParty) {
      formData.opposingParty = contract.opposingParty;
    }

    // 填充标的金额
    if (contract.claimAmount) {
      formData.claimAmount = contract.claimAmount;
    }

    // 填充主办律师（从合同参与人中找LEAD角色）
    if (contract.participants && contract.participants.length > 0) {
      const leadLawyer = contract.participants.find(
        (p: any) => p.role === 'LEAD',
      );
      if (leadLawyer) {
        formData.leadLawyerId = leadLawyer.userId;
      }
      // 找案源人
      const originator = contract.participants.find(
        (p: any) => p.role === 'ORIGINATOR',
      );
      if (originator) {
        formData.originatorId = originator.userId;
      }
    }

    // 填充签署人作为主办律师（如果没有从参与人中找到）
    if (!formData.leadLawyerId && contract.signerId) {
      formData.leadLawyerId = contract.signerId;
    }

    message.success('已自动填充合同信息，请检查并补充其他必要信息');
  }
}

// 创建客户
function handleCreateClient() {
  // 跳转到客户创建页面，并传递回调参数
  router.push({
    path: '/crm/client',
    query: {
      action: 'create',
      returnPath: '/matter/list',
      returnQuery: JSON.stringify({ modal: 'create' }),
    },
  });
}

// 添加客户
function addClient() {
  selectedClients.value.push({
    clientId: undefined,
    clientRole: 'PLAINTIFF',
    isPrimary: false,
  });
}

// 删除客户
function removeClient(index: number) {
  const removed = selectedClients.value.splice(index, 1)[0];
  // 如果删除的是主要客户，将第一个设为主要
  if (removed?.isPrimary && selectedClients.value.length > 0) {
    selectedClients.value[0]!.isPrimary = true;
  }
}

// 设置主要客户
function setPrimaryClient(index: number) {
  selectedClients.value.forEach((c, i) => {
    c.isPrimary = i === index;
  });
}

// 验证客户
function validateClients(_rule: any, _value: any): Promise<void> {
  return new Promise((resolve, reject) => {
    if (selectedClients.value.length === 0) {
      reject('请至少选择一个客户');
      return;
    }
    const hasEmpty = selectedClients.value.some((c) => !c.clientId);
    if (hasEmpty) {
      reject('请填写完整的客户信息');
      return;
    }
    resolve();
  });
}

// 重置多客户选择
function resetSelectedClients() {
  selectedClients.value = [
    { clientId: undefined, clientRole: 'PLAINTIFF', isPrimary: true },
  ];
}

// 查看详情
function handleView(record: MatterDTO) {
  router.push(`/matter/detail/${record.id}`);
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PENDING: 'orange',
    ACTIVE: 'blue',
    SUSPENDED: 'gray',
    CLOSED: 'green',
    ARCHIVED: 'purple',
  };
  return colorMap[status] || 'default';
}

// 归档项目
async function handleArchive(record: MatterDTO) {
  // 检查项目状态是否允许归档
  if (record.status !== 'CLOSED' && record.status !== 'ARCHIVED') {
    Modal.confirm({
      title: '项目未结案',
      content: `项目 "${record.name}" 尚未结案，是否先申请结案？只有已结案的项目才能创建档案。`,
      okText: '前往结案',
      cancelText: '取消',
      onOk: () => {
        // 跳转到项目详情页
        router.push(`/matter/detail/${record.id}`);
      },
    });
    return;
  }

  // 已结案项目，跳转到档案列表页面并打开归档向导
  Modal.confirm({
    title: '创建档案',
    content: `确定要为项目 "${record.name}" 创建档案吗？系统将收集项目所有相关数据（合同、文档、审批记录等）。`,
    okText: '创建档案',
    cancelText: '取消',
    onOk: () => {
      // 跳转到档案列表页面，传递项目ID
      router.push({
        path: '/archive/list',
        query: { matterId: record.id },
      });
    },
  });
}

// 格式化金额
function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

// 处理从客户创建页面返回
function handleReturnFromClient() {
  const route = router.currentRoute.value;
  if (route.query.action === 'created' && route.query.clientId) {
    // 客户创建成功，自动选择新创建的客户
    const clientId = Number(route.query.clientId);
    formData.clientId = clientId;
    // 重新加载客户列表
    loadOptions();
    // 打开创建项目弹窗
    modalVisible.value = true;
    // 清除查询参数
    router.replace({ path: route.path, query: {} });
    message.success('客户创建成功，已自动选择');
  }
}

onMounted(async () => {
  await loadOptions();

  // 检查是否有查询参数 id，如果有则打开编辑弹窗
  const id = route.query.id;
  if (id) {
    const matterId = Number(id);
    if (!isNaN(matterId)) {
      try {
        const matter = await getMatterDetail(matterId);
        handleEdit(matter);
        // 清除查询参数，避免刷新时重复打开
        router.replace({ path: '/matter/list', query: {} });
      } catch (error: any) {
        message.error(error.message || '加载项目详情失败');
      }
    }
  }

  // 检查是否从客户创建页面返回
  handleReturnFromClient();
});
</script>

<template>
  <Page title="项目管理" description="管理案件和项目信息">
    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Space>
          <Button
            v-access:code="'matter:create'"
            type="primary"
            @click="handleAdd"
          >
            新增项目
          </Button>
          <Button
            v-access:code="['contract:create', 'matter:contract:create']"
            @click="handleCreateContract"
          >
            创建合同
          </Button>
        </Space>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName }}
        </Tag>
      </template>

      <!-- 合同金额列 -->
      <template #contractAmount="{ row }">
        {{ formatMoney(row.contractAmount) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleView(row)">详情</a>
          <a
            v-if="!['ARCHIVED', 'CLOSED', 'PENDING_CLOSE'].includes(row.status)"
            v-access:code="'matter:edit'"
            @click="handleEdit(row)"
            >编辑</a
          >
          <a
            v-if="row.status !== 'ARCHIVED' && row.status !== 'CLOSED'"
            v-access:code="'archive:create'"
            @click="handleArchive(row)"
            style="color: #722ed1"
          >
            归档
          </a>
        </Space>
      </template>
    </Grid>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="900px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <!-- 合同选择（新增时必填） -->
        <div
          v-if="!formData.id"
          style="
            padding: 12px;
            margin-bottom: 16px;
            background: #e6f7ff;
            border: 1px solid #91d5ff;
            border-radius: 4px;
          "
        >
          <FormItem
            label="关联合同"
            name="contractId"
            :rules="[{ required: true, message: '请选择要关联的合同' }]"
            style="margin-bottom: 0"
          >
            <Select
              v-model:value="selectedContractId"
              placeholder="请选择已审批的合同（必选）"
              show-search
              style="width: 100%"
              :filter-option="
                (input, option) =>
                  (option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              :options="
                approvedContracts.map((c) => ({
                  label: `${c.contractNo} - ${c.clientName || ''} (${c.name})`,
                  value: c.id,
                }))
              "
              @change="(val: any) => val && handleContractSelect(val)"
            />
            <div style="margin-top: 8px; font-size: 12px; color: #666">
              📋
              选择合同后将自动填充客户、收费方式等信息。一个合同可以创建多个项目。
            </div>
          </FormItem>
        </div>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="项目名称"
              name="name"
              :rules="[{ required: true, message: '请输入项目名称' }]"
            >
              <Input
                v-model:value="formData.name"
                placeholder="请输入项目名称"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              label="项目大类"
              name="matterType"
              :rules="[{ required: true, message: '请选择项目大类' }]"
            >
              <Select
                v-model:value="formData.matterType"
                :options="matterTypeOptions"
                placeholder="请选择项目大类"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="案件类型" name="caseType">
              <Select
                v-model:value="formData.caseType"
                :options="caseTypeOptions"
                placeholder="请选择案件类型"
                allow-clear
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              v-if="formData.matterType === 'LITIGATION'"
              label="代理阶段"
              name="litigationStage"
            >
              <Select
                v-model:value="formData.litigationStage"
                :options="litigationStageOptions"
                placeholder="请选择代理阶段"
                allow-clear
              />
            </FormItem>
            <FormItem v-else label="业务类型" name="businessType">
              <Input
                v-model:value="formData.businessType"
                placeholder="请输入业务类型"
              />
            </FormItem>
          </Col>
        </Row>
        <Row v-if="showCauseSelect" :gutter="16">
          <Col :span="24">
            <FormItem label="案由" name="causeOfAction">
              <Cascader
                v-model:value="causeValue"
                :options="causeOptions"
                placeholder="请选择案由"
                change-on-select
                :show-search="{
                  filter: (inputValue, path) =>
                    path.some((option) =>
                      option.label
                        .toLowerCase()
                        .includes(inputValue.toLowerCase()),
                    ),
                }"
                :display-render="({ labels }) => labels[labels.length - 1]"
                style="width: 100%"
              />
            </FormItem>
          </Col>
        </Row>
        <!-- 多客户选择区域 -->
        <Row :gutter="16">
          <Col :span="24">
            <FormItem
              label="代理客户"
              :rules="[{ required: true, validator: validateClients }]"
            >
              <div style="display: flex; flex-direction: column; gap: 8px">
                <!-- 已添加的客户列表 -->
                <div
                  v-for="(client, index) in selectedClients"
                  :key="index"
                  style="
                    display: flex;
                    gap: 8px;
                    align-items: center;
                    padding: 8px;
                    background: #f5f5f5;
                    border-radius: 4px;
                  "
                >
                  <Tag
                    :color="client.isPrimary ? 'blue' : 'default'"
                    style="margin: 0"
                  >
                    {{ client.isPrimary ? '主要' : '共同' }}
                  </Tag>
                  <Select
                    v-model:value="client.clientId"
                    placeholder="请选择客户"
                    show-search
                    style="flex: 2"
                    :filter-option="
                      (input, option) =>
                        (option?.label || '')
                          .toLowerCase()
                          .includes(input.toLowerCase())
                    "
                    :options="
                      clients.map((c) => ({
                        label: c.clientNo
                          ? `[${c.clientNo}] ${c.name}`
                          : c.name,
                        value: c.id,
                      }))
                    "
                  />
                  <Select
                    v-model:value="client.clientRole"
                    placeholder="客户角色"
                    style="width: 120px"
                    :options="clientRoleOptions"
                  />
                  <Button
                    v-if="!client.isPrimary"
                    type="text"
                    size="small"
                    @click="setPrimaryClient(index)"
                  >
                    设为主要
                  </Button>
                  <Button
                    type="text"
                    danger
                    size="small"
                    @click="removeClient(index)"
                    :disabled="selectedClients.length <= 1"
                  >
                    删除
                  </Button>
                </div>
                <!-- 添加客户按钮 -->
                <div style="display: flex; gap: 8px">
                  <Button type="dashed" @click="addClient" style="flex: 1">
                    <template #icon><Plus class="size-4" /></template>
                    添加共同委托人
                  </Button>
                  <Button
                    type="link"
                    size="small"
                    @click="handleCreateClient"
                    style="padding: 0; white-space: nowrap"
                  >
                    <template #icon><Plus class="size-4" /></template>
                    创建新客户
                  </Button>
                </div>
              </div>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="24">
            <FormItem label="对方当事人" name="opposingParty">
              <Input
                v-model:value="formData.opposingParty"
                placeholder="请输入对方当事人（多个请用逗号分隔）"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="主办律师" name="leadLawyerId">
              <UserTreeSelect
                v-model:value="formData.leadLawyerId"
                placeholder="选择主办律师（按部门筛选）"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="案源人" name="originatorId">
              <UserTreeSelect
                v-model:value="formData.originatorId"
                placeholder="选择案源人（按部门筛选）"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="收费方式" name="feeType">
              <Select
                v-model:value="formData.feeType"
                :options="feeTypeOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="预估费用" name="estimatedFee">
              <InputNumber
                v-model:value="formData.estimatedFee"
                :min="0"
                :precision="2"
                style="width: 100%"
                :formatter="
                  (value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                "
                :parser="(value) => value.replace(/¥\s?|(,*)/g, '')"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="所属部门" name="departmentId">
              <TreeSelect
                v-model:value="formData.departmentId"
                :tree-data="departments"
                :field-names="{
                  label: 'name',
                  value: 'id',
                  children: 'children',
                }"
                placeholder="请选择部门"
                allow-clear
                style="width: 100%"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="预计结案日期" name="expectedClosingDate">
              <DatePicker
                v-model:value="formData.expectedClosingDate"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="对方律师姓名" name="opposingLawyerName">
              <Input
                v-model:value="formData.opposingLawyerName"
                placeholder="请输入对方律师姓名"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="对方律师执业证号" name="opposingLawyerLicenseNo">
              <Input
                v-model:value="formData.opposingLawyerLicenseNo"
                placeholder="请输入执业证号"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="对方律师律所" name="opposingLawyerFirm">
              <Input
                v-model:value="formData.opposingLawyerFirm"
                placeholder="请输入律所名称"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="对方律师电话" name="opposingLawyerPhone">
              <Input
                v-model:value="formData.opposingLawyerPhone"
                placeholder="请输入联系电话"
              />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="项目描述" name="description">
          <Textarea
            v-model:value="formData.description"
            :rows="3"
            placeholder="请输入项目描述"
          />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea
            v-model:value="formData.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 创建合同弹窗 -->
    <Modal
      v-model:open="contractModalVisible"
      title="创建合同"
      width="800px"
      @ok="handleSaveContract"
    >
      <Form
        ref="contractFormRef"
        :model="contractFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="合同名称"
              name="name"
              :rules="[{ required: true, message: '请输入合同名称' }]"
            >
              <Input
                v-model:value="contractFormData.name"
                placeholder="请输入合同名称"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              label="客户"
              name="clientId"
              :rules="[{ required: true, message: '请选择客户' }]"
            >
              <Select
                v-model:value="contractFormData.clientId"
                placeholder="请选择客户"
                show-search
                allow-clear
                :filter-option="
                  (input, option) =>
                    (option?.label || '')
                      .toLowerCase()
                      .includes(input.toLowerCase())
                "
                :options="
                  clients.map((c) => ({
                    label: c.clientNo ? `[${c.clientNo}] ${c.name}` : c.name,
                    value: c.id,
                  }))
                "
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="合同类型"
              name="contractType"
              :rules="[{ required: true, message: '请选择合同类型' }]"
            >
              <Select v-model:value="contractFormData.contractType">
                <SelectOption value="SERVICE">服务合同</SelectOption>
                <SelectOption value="RETAINER">常年法顾</SelectOption>
                <SelectOption value="LITIGATION">诉讼代理</SelectOption>
                <SelectOption value="NON_LITIGATION">非诉项目</SelectOption>
              </Select>
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              label="收费方式"
              name="feeType"
              :rules="[{ required: true, message: '请选择收费方式' }]"
            >
              <Select v-model:value="contractFormData.feeType">
                <SelectOption value="FIXED">固定收费</SelectOption>
                <SelectOption value="HOURLY">计时收费</SelectOption>
                <SelectOption value="CONTINGENCY">风险代理</SelectOption>
                <SelectOption value="MIXED">混合收费</SelectOption>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="合同金额"
              name="totalAmount"
              :rules="[{ required: true, message: '请输入合同金额' }]"
            >
              <InputNumber
                v-model:value="contractFormData.totalAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
                :formatter="
                  (value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                "
                :parser="(value) => value.replace(/¥\s?|(,*)/g, '')"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="币种" name="currency">
              <Select v-model:value="contractFormData.currency">
                <SelectOption value="CNY">人民币</SelectOption>
                <SelectOption value="USD">美元</SelectOption>
                <SelectOption value="EUR">欧元</SelectOption>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="签约日期" name="signDate">
              <DatePicker
                v-model:value="contractFormData.signDate"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="生效日期" name="effectiveDate">
              <DatePicker
                v-model:value="contractFormData.effectiveDate"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="到期日期" name="expiryDate">
              <DatePicker
                v-model:value="contractFormData.expiryDate"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="签约人" name="signerId">
              <UserTreeSelect
                v-model:value="contractFormData.signerId"
                placeholder="选择签约人（按部门筛选）"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="所属部门" name="departmentId">
              <TreeSelect
                v-model:value="contractFormData.departmentId"
                :tree-data="departments"
                :field-names="{
                  label: 'name',
                  value: 'id',
                  children: 'children',
                }"
                placeholder="请选择部门"
                allow-clear
                style="width: 100%"
              />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="付款条款" name="paymentTerms">
          <Textarea
            v-model:value="contractFormData.paymentTerms"
            :rows="3"
            placeholder="请输入付款条款"
          />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea
            v-model:value="contractFormData.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
