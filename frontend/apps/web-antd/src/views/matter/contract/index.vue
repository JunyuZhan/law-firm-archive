<script setup lang="ts">
import type { ClientDTO } from '#/api/client/types';
import type { CommissionRule } from '#/api/finance/commission-rule';
import type {
  ContractDTO,
  ContractParticipantDTO,
  ContractPaymentScheduleDTO,
  ContractQuery,
  ContractStatistics,
  CreateContractCommand,
  CreateParticipantCommand,
  CreatePaymentScheduleCommand,
} from '#/api/finance/types';
import type { ContractPrintDTO } from '#/api/matter';
import type { DepartmentDTO } from '#/api/system/types';

import { computed, h, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Alert,
  Button,
  Card,
  Cascader,
  Checkbox,
  Col,
  DatePicker,
  Divider,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getClientList, getClientSelectOptions } from '#/api/client';
// UpdateContractCommand 用于类型推断
import {
  createContractFromTemplate,
  createContractParticipant,
  createPaymentSchedule,
  deleteContractParticipant,
  deletePaymentSchedule,
  getContractParticipants,
  getContractPaymentSchedules,
  updateContractParticipant,
  updatePaymentSchedule,
} from '#/api/finance';
import { commissionRuleApi } from '#/api/finance/commission-rule';
import {
  applyContractChange,
  createContract,
  getContractApprovers,
  getContractDetail,
  getContractPrintData,
  getMatterContractList,
  getMatterContractStatistics,
  submitContract,
  updateContract,
  withdrawContract,
} from '#/api/matter';
import { requestClient } from '#/api/request';
import { getDepartmentTreePublic, getDictDataByCode } from '#/api/system';
import { approveApproval, getBusinessApprovals } from '#/api/workbench';
import { UserTreeSelect } from '#/components/UserTreeSelect';
import {
  decodeHtmlEntities,
  formatStructuredForPrint,
  isStructuredContent,
} from '#/views/system/contract-template/utils/print-formatter';
import {
  CASE_CATEGORY_OPTIONS,
  causesToCascaderOptions,
  findCauseNameInAll,
  getCausesByType,
  getCauseTypeByCase,
  needsCauseOfAction,
  preloadAllCauses,
} from '#/composables/useCauseOfAction';
import { getStageDictCode } from '#/composables/useStageDict';

defineOptions({ name: 'MatterContractList' });

// 响应式布局
const { isMobile } = useResponsive();

// 用户信息
const userStore = useUserStore();
const currentUserId = computed(() => Number(userStore.userInfo?.userId) || 0);

// 判断当前用户是否可以操作该合同（创建者或签约人）
function canOperateContract(contract: ContractDTO): boolean {
  const userId = currentUserId.value;
  return contract.createdBy === userId || contract.signerId === userId;
}

// 金额转中文大写
function amountToChinese(num: null | number | undefined): string {
  if (num === undefined || num === null || isNaN(num)) return '';
  if (num === 0) return '零元整';

  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'];
  const units = ['', '拾', '佰', '仟'];
  const bigUnits = ['', '万', '亿', '兆'];

  // 分离整数和小数部分
  const parts = num.toFixed(2).split('.');
  const intPart = parts[0] || '0';
  const decPart = parts[1] || '00';
  let result = '';

  // 处理整数部分
  if (intPart !== '0') {
    let intStr: string = intPart;
    let groupIndex = 0;

    while (intStr.length > 0) {
      const group = intStr.slice(-4);
      intStr = intStr.slice(0, -4);

      let groupResult = '';
      let hasZero = false;

      for (let i = 0; i < group.length; i++) {
        const char = group[i];
        if (char === undefined) continue;
        const digit = Number.parseInt(char, 10);
        const unitIndex = group.length - 1 - i;

        if (digit === 0) {
          hasZero = true;
        } else {
          if (hasZero) {
            groupResult += '零';
            hasZero = false;
          }
          groupResult += (digits[digit] || '') + (units[unitIndex] || '');
        }
      }

      if (groupResult) {
        result = groupResult + (bigUnits[groupIndex] || '') + result;
      } else if (
        result &&
        groupIndex > 0 && // 如果当前组为空但后面有数字，需要添加零
        !result.startsWith('零')
      ) {
        result = `零${result}`;
      }

      groupIndex++;
    }

    result += '元';
  }

  // 处理小数部分
  const jiaoChar = decPart[0];
  const fenChar = decPart[1];
  const jiao = jiaoChar ? Number.parseInt(jiaoChar, 10) : 0;
  const fen = fenChar ? Number.parseInt(fenChar, 10) : 0;

  if (jiao === 0 && fen === 0) {
    result += '整';
  } else {
    if (jiao > 0) {
      result += `${digits[jiao] || ''}角`;
    } else if (result) {
      result += '零';
    }
    if (fen > 0) {
      result += `${digits[fen] || ''}分`;
    }
  }

  return result || '零元整';
}

const router = useRouter();

// 状态
const loading = ref(false);
const saving = ref(false); // 保存中状态
const dataSource = ref<ContractDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const modalTitle = ref('创建合同');
const formRef = ref();
const changeModalVisible = ref(false);
const changeFormRef = ref();
const detailModalVisible = ref(false);
const currentContract = ref<ContractDTO | null>(null);
const activeTab = ref('info');
const paymentSchedules = ref<ContractPaymentScheduleDTO[]>([]);
const participants = ref<ContractParticipantDTO[]>([]);
const scheduleModalVisible = ref(false);
const participantModalVisible = ref(false);
const scheduleFormRef = ref();
const participantFormRef = ref();
const statistics = ref<ContractStatistics | null>(null);

// 合同模板相关
interface ContractTemplateDTO {
  id: number;
  templateNo: string;
  name: string;
  templateType: string;
  templateTypeName: string;
  feeType: string;
  feeTypeName: string;
  content: string;
  clauses: string;
  description: string;
  status: string;
}
const contractTemplates = ref<ContractTemplateDTO[]>([]);
const selectedTemplateId = ref<number | undefined>(undefined);
const selectedTemplate = ref<ContractTemplateDTO | null>(null);
const templatePreviewVisible = ref(false);
const templatePreviewContent = ref('');

// 打印弹窗相关
const printModalVisible = ref(false);
const printOptions = reactive({
  printContract: true, // 打印合同
  printApprovalForm: true, // 打印收案审批表
});
const currentPrintData = ref<ContractPrintDTO | null>(null);

// 审批表编辑弹窗相关
const approvalFormModalVisible = ref(false);
const approvalFormData = reactive({
  contractId: 0,
  caseSummary: '',
});
const savingApprovalForm = ref(false);

// 提成方案相关
const commissionRules = ref<CommissionRule[]>([]);
const selectedCommissionRuleId = ref<number | undefined>(undefined);
const selectedCommissionRule = ref<CommissionRule | null>(null);
const commissionFormData = reactive({
  firmRate: 0,
  leadLawyerRate: 0,
  assistLawyerRate: 0,
  supportStaffRate: 0,
  originatorRate: 0,
});

const changeFormData = reactive<{
  [key: string]: any;
  changeDescription: string;
  changeReason: string;
  contractId?: number;
  effectiveDate?: any;
  expiryDate?: any;
  signDate?: any;
}>({
  changeReason: '',
  changeDescription: '',
});
const clients = ref<ClientDTO[]>([]);
const departments = ref<DepartmentDTO[]>([]);

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

// 选中的年份（0表示全部）
const selectedYear = ref<number>(currentYear);

// 查询参数
const queryParams = reactive<ContractQuery>({
  pageNum: 1,
  pageSize: 10,
  contractNo: undefined,
  name: undefined,
  clientId: undefined,
  contractType: undefined,
  feeType: undefined,
  status: undefined,
  signerId: undefined,
  // 默认筛选当前年份（按创建时间）
  createdAtFrom: `${currentYear}-01-01T00:00:00`,
  createdAtTo: `${currentYear}-12-31T23:59:59`,
});

// 年份变化时更新日期范围
function handleYearChange(value: any) {
  const year = Number(value);
  if (isNaN(year)) return;
  selectedYear.value = year;
  if (year === 0) {
    // 选择"全部年份"时清除日期筛选
    queryParams.createdAtFrom = undefined;
    queryParams.createdAtTo = undefined;
  } else {
    queryParams.createdAtFrom = `${year}-01-01T00:00:00`;
    queryParams.createdAtTo = `${year}-12-31T23:59:59`;
  }
  queryParams.pageNum = 1;
  fetchData();
}

// 表单数据
const formData = reactive<
  {
    arbitrationCommittee?: string;
    assistantNames?: string;
    assistantRate?: number;
    authorizationType?: string;
    criminalCharge?: string;
    defendantName?: string;
    defenseStage?: string | string[];
    disputeResolution?: string;
    effectiveDate?: any;
    expiryDate?: any;
    id?: number;
    // 模板专用扩展字段
    lawyerNames?: string;
    partnerRate?: number;
    paymentDeadline?: string;
    seniorRate?: number;
    serviceHours?: number;
    signDate?: any;
    specialTerms?: string;
  } & Partial<CreateContractCommand>
>({
  name: '',
  clientId: undefined,
  contractType: 'CIVIL_PROXY',
  feeType: 'FIXED',
  totalAmount: undefined,
  currency: 'CNY',
  signDate: dayjs().format('YYYY-MM-DD'), // 默认今天
  effectiveDate: undefined,
  expiryDate: undefined,
  signerId: undefined,
  departmentId: undefined,
  paymentTerms: '',
  remark: '',
  // 扩展字段
  caseType: undefined,
  causeOfAction: undefined,
  trialStage: [] as string[], // 支持多选
  claimAmount: undefined,
  jurisdictionCourt: '',
  opposingParty: '',
  caseSummary: '', // 案情摘要
  conflictCheckStatus: 'NOT_REQUIRED',
  advanceTravelFee: undefined,
  riskRatio: undefined,
  // 模板专用扩展字段
  lawyerNames: '',
  assistantNames: '',
  authorizationType: '一般代理',
  paymentDeadline: '',
  disputeResolution: '1',
  arbitrationCommittee: '',
  specialTerms: '',
  defendantName: '',
  criminalCharge: '',
  defenseStage: [] as string[], // 支持多选
  partnerRate: undefined,
  seniorRate: undefined,
  assistantRate: undefined,
  serviceHours: undefined,
});

// 付款计划表单
const scheduleFormData = reactive<
  Partial<CreatePaymentScheduleCommand> & { id?: number }
>({
  phaseName: '',
  amount: undefined,
  percentage: undefined,
  plannedDate: undefined,
  remark: '',
});

// 参与人表单（用于合同详情页）
const participantFormData = reactive<
  Partial<CreateParticipantCommand> & { id?: number }
>({
  userId: undefined,
  role: 'CO_COUNSEL',
  commissionRate: undefined,
  remark: '',
});

// 创建合同时的参与人列表
interface ContractParticipantInput {
  userId: number | undefined;
  role: string;
  commissionRate?: number;
}

const contractParticipants = ref<ContractParticipantInput[]>([]);

// 表格列（响应式）
const columns = computed(() => {
  const baseColumns = [
    {
      title: '合同编号',
      dataIndex: 'contractNo',
      key: 'contractNo',
      width: 140,
    },
    {
      title: '合同名称',
      dataIndex: 'name',
      key: 'name',
      width: isMobile.value ? 120 : 180,
      ellipsis: true,
      mobileShow: true,
    },
    {
      title: '客户',
      dataIndex: 'clientName',
      key: 'clientName',
      width: 120,
      mobileShow: true,
    },
    {
      title: '合同类型',
      dataIndex: 'contractTypeName',
      key: 'contractTypeName',
      width: 100,
    },
    {
      title: '收费方式',
      dataIndex: 'feeTypeName',
      key: 'feeTypeName',
      width: 100,
    },
    {
      title: '合同金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      mobileShow: true,
    },
    {
      title: '状态',
      dataIndex: 'statusName',
      key: 'statusName',
      width: 90,
      mobileShow: true,
    },
    { title: '签约日期', dataIndex: 'signDate', key: 'signDate', width: 110 },
    {
      title: '操作',
      key: 'action',
      width: isMobile.value ? 120 : 220,
      fixed: 'right' as const,
      mobileShow: true,
    },
  ];

  // 移动端隐藏部分列
  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
});

// 付款计划表格列
const scheduleColumns = [
  { title: '阶段名称', dataIndex: 'phaseName', key: 'phaseName', width: 150 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '比例(%)', dataIndex: 'percentage', key: 'percentage', width: 100 },
  {
    title: '计划日期',
    dataIndex: 'plannedDate',
    key: 'plannedDate',
    width: 120,
  },
  { title: '实际日期', dataIndex: 'actualDate', key: 'actualDate', width: 120 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '操作', key: 'action', width: 150 },
];

// 参与人表格列
const participantColumns = [
  { title: '姓名', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '角色', dataIndex: 'roleName', key: 'roleName', width: 100 },
  {
    title: '提成比例(%)',
    dataIndex: 'commissionRate',
    key: 'commissionRate',
    width: 120,
  },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 150 },
  { title: '操作', key: 'action', width: 150 },
];

// 合同类型选项
const contractTypeOptions = [
  { label: '民事代理', value: 'CIVIL_PROXY' },
  { label: '行政代理', value: 'ADMINISTRATIVE_PROXY' },
  { label: '刑事辩护', value: 'CRIMINAL_DEFENSE' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '非诉案件', value: 'NON_LITIGATION' },
];

// 收费类型选项
const feeTypeOptions = [
  { label: '固定收费', value: 'FIXED' },
  { label: '计时收费', value: 'HOURLY' },
  { label: '风险代理', value: 'CONTINGENCY' },
  { label: '混合收费', value: 'MIXED' },
];

// 审理阶段选项 - 从后端字典加载
const trialStageOptions = ref<{ label: string; value: string }[]>([]);
const trialStageLoading = ref(false);

// 加载审理阶段选项（使用公共映射）
async function loadTrialStageOptions(caseType: string | undefined) {
  if (!caseType) {
    trialStageOptions.value = [];
    return;
  }
  const dictCode = getStageDictCode(caseType);
  trialStageLoading.value = true;
  try {
    const items = await getDictDataByCode(dictCode);
    trialStageOptions.value = items.map((item) => ({
      label: item.label,
      value: item.value,
    }));
  } catch (error) {
    console.warn('加载审理阶段字典失败:', error);
    trialStageOptions.value = [];
  } finally {
    trialStageLoading.value = false;
  }
}

// 利冲审查状态选项
const conflictCheckStatusOptions = [
  { label: '待审查', value: 'PENDING' },
  { label: '已通过', value: 'PASSED' },
  { label: '未通过', value: 'FAILED' },
  { label: '无需审查', value: 'NOT_REQUIRED' },
];

// 参与人角色选项
const participantRoleOptions = [
  { label: '承办律师', value: 'LEAD' },
  { label: '协办律师', value: 'CO_COUNSEL' },
  { label: '案源人', value: 'ORIGINATOR' },
  { label: '律师助理', value: 'PARALEGAL' },
];

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING' },
  { label: '生效中', value: 'ACTIVE' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已终止', value: 'TERMINATED' },
  { label: '已完成', value: 'COMPLETED' },
];

// 案件类型选项（诉讼类）
const caseTypeOptions = CASE_CATEGORY_OPTIONS.filter(
  (opt) => opt.matterType === 'LITIGATION',
);

// 案由级联选择值
const causeValue = ref<string[]>([]);

// 是否显示案由选择
const showCauseSelect = computed(() => {
  return formData.caseType && needsCauseOfAction(formData.caseType);
});

// 案由选项（异步加载）
const causeOptions = ref<any[]>([]);

// 加载案由选项
async function loadCauseOptions() {
  if (!formData.caseType) {
    causeOptions.value = [];
    return;
  }
  const causeType = getCauseTypeByCase(formData.caseType);
  if (!causeType) {
    causeOptions.value = [];
    return;
  }
  const causes = await getCausesByType(causeType);
  causeOptions.value = causesToCascaderOptions(causes);
}

// 刑事罪名级联选择值
const criminalChargeValue = ref<string[]>([]);

// 刑事罪名选项（异步加载）
const criminalChargeOptions = ref<any[]>([]);

// 加载刑事罪名选项
async function loadCriminalChargeOptions() {
  if (formData.caseType !== 'CRIMINAL') {
    criminalChargeOptions.value = [];
    criminalChargeValue.value = [];
    return;
  }
  try {
    const causes = await getCausesByType('CRIMINAL');
    if (causes && causes.length > 0) {
      criminalChargeOptions.value = causesToCascaderOptions(causes);
    } else {
      criminalChargeOptions.value = [];
    }
  } catch (error) {
    console.error('加载刑事罪名选项失败:', error);
    criminalChargeOptions.value = [];
  }
}

// 监听案件类型变化，清空案由和审理阶段，并重新加载选项
watch(
  () => formData.caseType,
  (newCaseType) => {
    formData.causeOfAction = undefined;
    formData.trialStage = []; // 案件类型改变时清空审理阶段
    causeValue.value = [];
    // 如果不是刑事案件，清空刑事案件相关字段
    if (newCaseType !== 'CRIMINAL') {
      formData.criminalCharge = '';
      formData.defendantName = '';
      formData.defenseStage = [];
      criminalChargeValue.value = [];
      criminalChargeOptions.value = [];
    }
    // 从字典加载审理阶段选项
    loadTrialStageOptions(newCaseType);
    // 加载案由选项
    loadCauseOptions();
    // 如果是刑事案件，加载刑事罪名选项
    if (newCaseType === 'CRIMINAL') {
      loadCriminalChargeOptions();
    }
  },
);

// 监听案由级联选择变化
watch(causeValue, (val) => {
  formData.causeOfAction =
    val && val.length > 0 ? val[val.length - 1] : undefined;

  // 如果是刑事案件，自动将案由名称导入到涉嫌罪名字段
  if (formData.caseType === 'CRIMINAL') {
    if (val && val.length > 0) {
      const causeCode = val[val.length - 1];
      if (causeCode) {
        const causeName = findCauseNameInAll(causeCode);
        if (causeName) {
          formData.criminalCharge = causeName;
          // 同步更新刑事罪名级联选择值
          criminalChargeValue.value = val;
        }
      }
    }
  }
});

// 监听刑事罪名级联选择变化
watch(criminalChargeValue, (val) => {
  if (formData.caseType === 'CRIMINAL') {
    if (val && val.length > 0) {
      const chargeCode = val[val.length - 1];
      if (chargeCode) {
        const chargeName = findCauseNameInAll(chargeCode);
        if (chargeName) {
          formData.criminalCharge = chargeName;
        }
      }
    } else {
      formData.criminalCharge = '';
    }
  }
});

// 监听提成方案比例变化，自动更新参与人比例
watch(
  () => [
    commissionFormData.firmRate,
    commissionFormData.leadLawyerRate,
    commissionFormData.assistLawyerRate,
    commissionFormData.supportStaffRate,
    commissionFormData.originatorRate,
  ],
  () => {
    // 如果已选择方案且方案允许修改，同步更新参与人比例
    if (
      selectedCommissionRule.value &&
      selectedCommissionRule.value.allowModify
    ) {
      contractParticipants.value.forEach((p) => {
        if (p.role) {
          const rate = getCommissionRateByRole(p.role);
          if (rate !== undefined) {
            p.commissionRate = rate;
          }
        }
      });
    }
  },
  { deep: true },
);

// 计算提成比例总和
const totalCommissionRate = computed(() => {
  return participants.value.reduce(
    (sum, p) => sum + (p.commissionRate || 0),
    0,
  );
});

// 计算付款计划总额
const totalScheduleAmount = computed(() => {
  return paymentSchedules.value.reduce((sum, s) => sum + (s.amount || 0), 0);
});

// 合同金额大写（自动联动）
const totalAmountChinese = computed(() => {
  return amountToChinese(formData.totalAmount);
});

// 标的金额大写（自动联动）
const claimAmountChinese = computed(() => {
  return amountToChinese(formData.claimAmount);
});

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getMatterContractList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } catch (error: any) {
    message.error(error.message || '加载合同列表失败');
  } finally {
    loading.value = false;
  }
}

// 加载统计数据
async function loadStatistics() {
  try {
    statistics.value = await getMatterContractStatistics();
  } catch (error: any) {
    console.error('加载统计数据失败', error);
  }
}

// 加载选项数据 - 每个API独立处理错误，避免一个失败导致全部失败
async function loadOptions() {
  // 客户列表（只加载已转正的正式客户，用于创建合同）
  try {
    const clientRes = await getClientSelectOptions({
      pageNum: 1,
      pageSize: 1000,
    });
    clients.value = clientRes.list || [];
  } catch (error) {
    console.warn('加载客户列表失败', error);
  }

  // 部门树（使用公共接口，无需特殊权限）
  try {
    const deptRes = await getDepartmentTreePublic();
    departments.value = deptRes || [];
  } catch (error) {
    console.warn('加载部门树失败', error);
  }

  // 合同模板 - 使用不需要权限的接口
  try {
    const templateRes = await requestClient.get<ContractTemplateDTO[]>(
      '/system/contract-template/active',
    );
    contractTemplates.value = templateRes || [];
  } catch (error) {
    console.warn('加载合同模板失败', error);
  }

  // 提成方案（使用公共接口，无需特殊权限）
  try {
    const ruleRes = await commissionRuleApi.getActiveRules();
    commissionRules.value = ruleRes || [];

    // 设置默认提成方案
    const defaultRule = commissionRules.value.find((r) => r.isDefault);
    if (defaultRule) {
      handleCommissionRuleChange(defaultRule.id);
    }
  } catch (error) {
    console.warn('加载提成方案失败', error);
  }

  // 预加载案由数据（民事、刑事、行政）
  try {
    await preloadAllCauses();
  } catch (error) {
    console.warn('预加载案由数据失败', error);
  }
}

// 根据角色获取对应的提成比例
function getCommissionRateByRole(role: string): number | undefined {
  if (!selectedCommissionRule.value) return undefined;

  switch (role) {
    case 'CO_COUNSEL': {
      return selectedCommissionRule.value.assistLawyerRate;
    }
    case 'LEAD': {
      return selectedCommissionRule.value.leadLawyerRate;
    }
    case 'ORIGINATOR': {
      return selectedCommissionRule.value.originatorRate;
    }
    case 'PARALEGAL': {
      return selectedCommissionRule.value.supportStaffRate;
    }
    default: {
      return undefined;
    }
  }
}

// 选择提成方案
function handleCommissionRuleChange(value: any) {
  const ruleId = value as number | undefined;
  selectedCommissionRuleId.value = ruleId;
  if (!ruleId) {
    selectedCommissionRule.value = null;
    Object.assign(commissionFormData, {
      firmRate: 0,
      leadLawyerRate: 0,
      assistLawyerRate: 0,
      supportStaffRate: 0,
      originatorRate: 0,
    });
    // 清空参与人的比例
    contractParticipants.value.forEach((p) => {
      p.commissionRate = undefined;
    });
    return;
  }

  const rule = commissionRules.value.find((r) => r.id === ruleId);
  if (rule) {
    selectedCommissionRule.value = rule;
    Object.assign(commissionFormData, {
      firmRate: rule.firmRate,
      leadLawyerRate: rule.leadLawyerRate,
      assistLawyerRate: rule.assistLawyerRate,
      supportStaffRate: rule.supportStaffRate,
      originatorRate: rule.originatorRate || 0,
    });

    // 根据方案自动初始化参与人列表
    // 如果参与人列表为空，根据方案自动添加有比例的角色的参与人
    if (contractParticipants.value.length === 0) {
      const newParticipants: any[] = [];

      // 主办律师
      if (rule.leadLawyerRate > 0) {
        newParticipants.push({
          userId: undefined,
          role: 'LEAD',
          commissionRate: rule.leadLawyerRate,
        });
      }

      // 协办律师
      if (rule.assistLawyerRate > 0) {
        newParticipants.push({
          userId: undefined,
          role: 'CO_COUNSEL',
          commissionRate: rule.assistLawyerRate,
        });
      }

      // 辅助人员
      if (rule.supportStaffRate > 0) {
        newParticipants.push({
          userId: undefined,
          role: 'PARALEGAL',
          commissionRate: rule.supportStaffRate,
        });
      }

      // 案源人
      if (rule.originatorRate && rule.originatorRate > 0) {
        newParticipants.push({
          userId: undefined,
          role: 'ORIGINATOR',
          commissionRate: rule.originatorRate,
        });
      }

      // 如果方案中所有角色比例都为0，至少添加一个主办律师（比例为0）
      if (newParticipants.length === 0) {
        newParticipants.push({
          userId: undefined,
          role: 'LEAD',
          commissionRate: rule.leadLawyerRate || 0,
        });
      }

      contractParticipants.value.push(...newParticipants);
    } else {
      // 如果已有参与人，更新他们的比例并补充缺失的角色
      const newParticipants: any[] = [];

      // 更新已有参与人的提成比例（根据角色）
      contractParticipants.value.forEach((p) => {
        if (p.role) {
          const rate = getCommissionRateByRole(p.role);
          if (rate !== undefined) {
            p.commissionRate = rate;
          }
        }
      });

      // 检查并补充缺失的角色（如果方案中有比例但参与人列表中还没有）
      // 主办律师
      if (
        rule.leadLawyerRate > 0 &&
        !contractParticipants.value.find((p) => p.role === 'LEAD')
      ) {
        newParticipants.push({
          userId: undefined,
          role: 'LEAD',
          commissionRate: rule.leadLawyerRate,
        });
      }

      // 协办律师
      if (
        rule.assistLawyerRate > 0 &&
        !contractParticipants.value.find((p) => p.role === 'CO_COUNSEL')
      ) {
        newParticipants.push({
          userId: undefined,
          role: 'CO_COUNSEL',
          commissionRate: rule.assistLawyerRate,
        });
      }

      // 辅助人员
      if (
        rule.supportStaffRate > 0 &&
        !contractParticipants.value.find((p) => p.role === 'PARALEGAL')
      ) {
        newParticipants.push({
          userId: undefined,
          role: 'PARALEGAL',
          commissionRate: rule.supportStaffRate,
        });
      }

      // 案源人
      if (
        rule.originatorRate &&
        rule.originatorRate > 0 &&
        !contractParticipants.value.find((p) => p.role === 'ORIGINATOR')
      ) {
        newParticipants.push({
          userId: undefined,
          role: 'ORIGINATOR',
          commissionRate: rule.originatorRate,
        });
      }

      contractParticipants.value.push(...newParticipants);
    }
  }
}

// 字段配置：定义不同模板需要的字段
interface FieldConfig {
  // 基础字段（所有模板都需要）
  basic: string[];
  // 诉讼类字段（民事/行政/刑事）
  litigation: string[];
  // 刑事案件专用字段
  criminal: string[];
  // 非诉项目字段
  nonLitigation: string[];
  // 常年法顾字段
  retainer: string[];
}

const fieldConfig: Record<string, FieldConfig> = {
  // 标准服务合同
  SERVICE: {
    basic: [
      'clientId',
      'totalAmount',
      'signDate',
      'paymentTerms',
      'effectiveDate',
      'expiryDate',
    ],
    litigation: [],
    criminal: [],
    nonLitigation: [],
    retainer: [],
  },
  // 常年法律顾问
  RETAINER: {
    basic: [
      'clientId',
      'totalAmount',
      'signDate',
      'paymentTerms',
      'effectiveDate',
      'expiryDate',
      'serviceHours',
    ],
    litigation: [],
    criminal: [],
    nonLitigation: [],
    retainer: ['serviceHours'],
  },
  // 诉讼代理（民事/行政）
  LITIGATION: {
    basic: [
      'clientId',
      'totalAmount',
      'signDate',
      'paymentTerms',
      'paymentDeadline',
      'effectiveDate',
      'expiryDate',
    ],
    litigation: [
      'caseType',
      'causeOfAction',
      'trialStage',
      'opposingParty',
      'claimAmount',
      'jurisdictionCourt',
      'lawyerNames',
      'assistantNames',
      'authorizationType',
      'disputeResolution',
      'arbitrationCommittee',
      'specialTerms',
    ],
    criminal: [],
    nonLitigation: [],
    retainer: [],
  },
  // 非诉项目
  NON_LITIGATION: {
    basic: [
      'clientId',
      'totalAmount',
      'signDate',
      'paymentTerms',
      'effectiveDate',
      'expiryDate',
    ],
    litigation: [],
    criminal: [],
    nonLitigation: ['partnerRate', 'seniorRate', 'assistantRate'],
    retainer: [],
  },
};

// 根据模板ID获取需要的字段列表
const visibleFields = computed(() => {
  if (!selectedTemplateId.value) {
    return {
      basic: [],
      litigation: [],
      criminal: [],
      nonLitigation: [],
      retainer: [],
    };
  }

  const template = contractTemplates.value.find(
    (t) => t.id === selectedTemplateId.value,
  );
  if (!template) {
    return {
      basic: [],
      litigation: [],
      criminal: [],
      nonLitigation: [],
      retainer: [],
    };
  }

  // 根据模板类型确定字段配置
  const templateType = template.templateType || 'CIVIL_PROXY';
  const fields: FieldConfig = {
    basic: [
      'clientId',
      'totalAmount',
      'signDate',
      'paymentTerms',
      'expiryDate',
    ],
    litigation: [],
    criminal: [],
    nonLitigation: [],
    retainer: [],
  };

  // 根据模板类型设置字段
  switch (templateType) {
    case 'CRIMINAL_DEFENSE':
      // 刑事案件专用字段
      fields.criminal = ['defendantName', 'criminalCharge', 'defenseStage'];
      fields.litigation = ['lawyerNames', 'paymentDeadline', 'specialTerms'];
      break;
    case 'CIVIL_PROXY':
    case 'ADMINISTRATIVE_PROXY':
      // 民事/行政案件字段
      fields.litigation = [
        'caseType',
        'causeOfAction',
        'trialStage',
        'opposingParty',
        'jurisdictionCourt',
        'claimAmount',
        'lawyerNames',
        'paymentDeadline',
        'specialTerms',
      ];
      break;
    case 'NON_LITIGATION':
      // 非诉项目字段
      fields.nonLitigation = ['partnerRate', 'seniorRate', 'assistantRate'];
      break;
    case 'LEGAL_COUNSEL':
      // 法律顾问字段
      fields.retainer = ['serviceHours'];
      break;
  }

  return fields;
});

// 判断字段是否应该显示
function shouldShowField(fieldName: string): boolean {
  const fields = visibleFields.value;
  return (
    fields.basic.includes(fieldName) ||
    fields.litigation.includes(fieldName) ||
    fields.criminal.includes(fieldName) ||
    fields.nonLitigation.includes(fieldName) ||
    fields.retainer.includes(fieldName)
  );
}

// 监听刑事案件字段显示，如果显示且案件类型为CRIMINAL，确保加载数据
watch(
  () => {
    const fields = visibleFields.value;
    return (
      fields.criminal.includes('criminalCharge') &&
      formData.caseType === 'CRIMINAL'
    );
  },
  (shouldLoad) => {
    if (shouldLoad && criminalChargeOptions.value.length === 0) {
      loadCriminalChargeOptions();
    }
  },
);

// 选择模板时自动填充字段
function handleTemplateChange(value: any) {
  const templateId = value as number | undefined;
  selectedTemplateId.value = templateId;

  if (!templateId) {
    selectedTemplate.value = null;
    return;
  }

  const template = contractTemplates.value.find((t) => t.id === templateId);
  if (template) {
    selectedTemplate.value = template;
    // 自动填充合同类型（等于模板类型）和收费方式
    formData.contractType = template.templateType || 'CIVIL_PROXY';
    formData.feeType = template.feeType || 'FIXED';

    // 根据模板类型自动设置案件类型
    switch (template.templateType) {
      case 'CRIMINAL_DEFENSE':
        formData.caseType = 'CRIMINAL';
        loadCriminalChargeOptions();
        break;
      case 'CIVIL_PROXY':
        formData.caseType = 'CIVIL';
        break;
      case 'ADMINISTRATIVE_PROXY':
        formData.caseType = 'ADMINISTRATIVE';
        break;
      case 'STATE_COMP_ADMIN':
        formData.caseType = 'STATE_COMP_ADMIN';
        break;
      case 'STATE_COMP_CRIMINAL':
        formData.caseType = 'STATE_COMP_CRIMINAL';
        break;
      case 'LEGAL_COUNSEL':
        formData.caseType = 'LEGAL_COUNSEL';
        break;
      case 'NON_LITIGATION':
        // 非诉项目不需要 caseType
        break;
    }

    message.success(
      `已应用模板 "${template.name}"，模板内容将在合同创建时导入`,
    );
  }
}

// 预览选中模板的内容
function handlePreviewTemplate() {
  if (!selectedTemplate.value || !selectedTemplate.value.content) {
    message.warning('模板暂无内容');
    return;
  }
  templatePreviewContent.value = formatTemplateContentForPreview(
    selectedTemplate.value.content,
  );
  templatePreviewVisible.value = true;
}

// 格式化模板内容用于预览
function formatTemplateContentForPreview(content: string): string {
  if (!content) return '暂无内容';

  try {
    // 尝试解析结构化内容（JSON格式）
    const parsed = JSON.parse(content);
    if (parsed._structured && parsed.blocks) {
      const blocks = parsed.blocks;
      let result = '';

      // 标题区
      if (blocks.title?.contractName) {
        result += `【标题】\n${blocks.title.contractName}\n\n`;
      }

      // 主体区（甲乙双方）
      if (blocks.parties) {
        if (blocks.parties.partyA) {
          result += `【甲方信息】\n${blocks.parties.partyA}\n\n`;
        }
        if (blocks.parties.partyB) {
          result += `【乙方信息】\n${blocks.parties.partyB}\n\n`;
        }
      }

      // 条款区
      if (blocks.clauses) {
        result += `【合同条款】\n${blocks.clauses}\n\n`;
      }

      // 签署区
      if (blocks.signature) {
        if (blocks.signature.partyASign) {
          result += `【甲方签署】\n${blocks.signature.partyASign}\n\n`;
        }
        if (blocks.signature.partyBSign) {
          result += `【乙方签署】\n${blocks.signature.partyBSign}\n\n`;
        }
        if (blocks.signature.signInfo) {
          result += `【签订信息】\n${blocks.signature.signInfo}\n`;
        }
      }

      return result || content;
    }
  } catch {
    // 不是JSON格式，直接返回原内容
  }

  return content;
}

// 选择客户时自动填充客户信息
function handleClientChange(value: any) {
  formData.clientId = value;
  // 客户数据自动载入（如果需要可以从客户信息中自动填充其他字段）
}

// 打印正式合同（审批后）- 打开打印选项弹窗
async function handlePrintContract() {
  if (!currentContract.value) return;

  try {
    loading.value = true;
    // 获取打印数据
    const printData = await getContractPrintData(currentContract.value.id);
    currentPrintData.value = printData;
    printOptions.printContract = true;
    printOptions.printApprovalForm = true;
    printModalVisible.value = true;
  } catch (error: any) {
    message.error(`获取打印数据失败：${error.message || '未知错误'}`);
  } finally {
    loading.value = false;
  }
}

// 打开审批表编辑弹窗
function handleEditApprovalForm() {
  if (!currentContract.value) return;
  approvalFormData.contractId = currentContract.value.id;
  approvalFormData.caseSummary = currentContract.value.caseSummary || '';
  approvalFormModalVisible.value = true;
}

// 保存审批表（案情摘要）
async function handleSaveApprovalForm() {
  if (!approvalFormData.contractId) return;

  try {
    savingApprovalForm.value = true;
    await updateContract({
      id: approvalFormData.contractId,
      caseSummary: approvalFormData.caseSummary,
    });
    message.success('保存成功');
    approvalFormModalVisible.value = false;

    // 更新当前合同的案情摘要
    if (currentContract.value) {
      currentContract.value.caseSummary = approvalFormData.caseSummary;
    }

    // 刷新列表
    fetchData();
  } catch (error: any) {
    message.error(`保存失败：${error.message || '未知错误'}`);
  } finally {
    savingApprovalForm.value = false;
  }
}

// 单独预览审批表
async function handlePreviewApprovalForm() {
  if (!currentContract.value) return;

  try {
    loading.value = true;
    const printData = await getContractPrintData(currentContract.value.id);

    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      message.error('无法打开预览窗口');
      return;
    }

    // 只生成审批表HTML
    const approvalFormHtml = generateApprovalFormHtml(printData);

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>收案审批表预览 - ${printData.contractNo}</title>
        <style>
          body { font-family: "SimSun", "宋体", serif; padding: 20px; font-size: 14pt; }
          * { font-family: "SimSun", "宋体", serif; }
          @media print {
            body { padding: 0; font-size: 14pt; }
            .no-print { display: none; }
          }
        </style>
      </head>
      <body>
        <div class="no-print" style="margin-bottom: 20px; padding: 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px;">
          <button onclick="window.print()" style="padding: 8px 16px; cursor: pointer; margin-right: 10px;">打印审批表</button>
          <button onclick="window.close()" style="padding: 8px 16px; cursor: pointer;">关闭</button>
        </div>
        ${approvalFormHtml}
      </body>
      </html>
    `);
    printWindow.document.close();
  } catch (error: any) {
    message.error(`获取预览数据失败：${error.message || '未知错误'}`);
  } finally {
    loading.value = false;
  }
}

// 生成审批表HTML
function generateApprovalFormHtml(data: ContractPrintDTO): string {
  // 案由：将代码转换为名称（确保转为字符串进行查找）
  const causeCode = data.causeOfAction ? String(data.causeOfAction) : '';
  let causeOfActionDisplay = '';

  // 对于刑事案件，优先显示罪名名称，避免显示"刑事案件"
  if (data.caseType === 'CRIMINAL') {
    if (causeCode) {
      causeOfActionDisplay =
        findCauseNameInAll(causeCode) || data.causeOfActionName || causeCode;
    } else {
      causeOfActionDisplay = data.causeOfActionName || '';
    }
  } else {
    // 民事和行政案件使用原有逻辑
    causeOfActionDisplay = causeCode
      ? findCauseNameInAll(causeCode) || data.causeOfActionName || causeCode
      : data.causeOfActionName || data.caseTypeName || '';
  }

  return `
    <div style="max-width: 800px; margin: 0 auto; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">
      <h2 style="text-align: center; margin-bottom: 5px; font-family: 'SimSun', '宋体', serif; font-size: 16pt;">${data.firmName || ''}</h2>
      <h2 style="text-align: center; margin-bottom: 10px; letter-spacing: 6px; font-family: 'SimSun', '宋体', serif; font-size: 16pt;">收案审批表</h2>
      <p style="text-align: right; font-size: 11pt; margin-bottom: 10px; color: #333;">合同编号：${data.contractNo || ''}</p>
      
      <table border="1" cellpadding="8" cellspacing="0" style="width: 100%; border-collapse: collapse; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">
        <tr>
          <th style="width: 90px; background: #f5f5f5;">委托人</th>
          <td colspan="3">${data.clientName || ''}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5;">案由</th>
          <td colspan="3">${causeOfActionDisplay}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5;">关联当事人</th>
          <td colspan="3">${data.opposingParty || ''}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5;">委托程序</th>
          <td colspan="3">${data.trialStageName || ''}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5;">有无利益冲突</th>
          <td colspan="3">${data.conflictCheckResult || '待审查'}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5;">代理/辩护费</th>
          <td>${data.totalAmount ? `¥${data.totalAmount.toLocaleString()}` : ''}</td>
          <th style="width: 70px; background: #f5f5f5;">委托时间</th>
          <td style="width: 130px;">${data.signDate || ''}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5;">接待人</th>
          <td>${data.originatorName || data.leadLawyerName || ''}</td>
          <th style="width: 70px; background: #f5f5f5;">办案单位</th>
          <td>${data.jurisdictionCourt || ''}</td>
        </tr>
        <tr>
          <th style="width: 90px; background: #f5f5f5; font-size: 12pt;">案情摘要<br/><span style="font-weight: normal; font-size: 10pt;">（附接待笔录）</span></th>
          <td colspan="3" style="height: 140px; vertical-align: top;">${data.description || '<span style="color: #999;">暂无案情摘要</span>'}</td>
        </tr>
        <tr>
          <th rowspan="2" style="width: 90px; background: #f5f5f5;">审查意见</th>
          <td colspan="3" style="height: 50px; vertical-align: top;">
            <div><strong>接待律师意见：</strong>拟接受委托</div>
            <div style="text-align: right; margin-top: 8px;">
              签名：${data.originatorName || data.signerName || '________'} 日期：${data.signDate || '____年__月__日'}
            </div>
          </td>
        </tr>
        <tr>
          <td colspan="3" style="height: 50px; vertical-align: top;">
            <div><strong>律所领导意见：</strong>${(data.approvals && data.approvals[0]?.comment) || ''}</div>
            <div style="text-align: right; margin-top: 8px;">
              签名：${(data.approvals && data.approvals[0]?.approverName) || '________'} 日期：${(data.approvals && data.approvals[0]?.approvedAt?.slice(0, 10)) || '____年__月__日'}
            </div>
          </td>
        </tr>
      </table>
    </div>
  `;
}

// 执行打印
async function executePrint() {
  if (!currentPrintData.value) return;

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器是否阻止了弹出窗口');
    return;
  }

  const data = currentPrintData.value;
  let htmlContent = '';

  // 公共样式
  // 重要：不要为所有 p 标签添加默认的 text-indent，否则会覆盖编辑器设置的格式
  // wangeditor 编辑器使用内联样式保存格式（如 text-align, text-indent），需要保留这些内联样式
  const commonStyles = `
    body { font-family: "SimSun", "宋体", serif; padding: 40px; line-height: 1.5; font-size: 12pt; }
    * { font-family: "SimSun", "宋体", serif; }
    h1 { text-align: center; font-size: 22pt; margin-bottom: 10px; font-family: "SimSun", "宋体", serif; }
    h2 { text-align: center; font-size: 22pt; margin-bottom: 20px; font-family: "SimSun", "宋体", serif; }
    .page-break { page-break-after: always; }
    table { width: 100%; border-collapse: collapse; margin: 20px 0; }
    td, th { border: 1px solid #333; padding: 8px; vertical-align: top; }
    th { background: #f5f5f5; text-align: center; font-weight: bold; width: 90px; }
    .signature { margin-top: 60px; display: flex; justify-content: space-between; }
    .signature-box { width: 45%; }
    .signature-line { border-bottom: 1px solid #333; height: 40px; margin: 20px 0; }
    .approval-section { margin-top: 20px; }
    .approval-section table { margin-top: 10px; }
    .no-border { border: none !important; }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .header-info { text-align: right; color: #666; margin-bottom: 20px; font-size: 12pt; font-family: "SimSun", "宋体", serif; }
    p, div, span { font-family: "SimSun", "宋体", serif; font-size: 12pt; margin: 4px 0; }
    /* 保留编辑器内联样式的格式 - 内联样式会覆盖这些默认样式 */
    /* 支持 wangeditor 的对齐和缩进格式 */
    p[style*="text-align"], div[style*="text-align"] { /* 保持用户设置的对齐 */ }
    p[style*="text-indent"], div[style*="text-indent"] { /* 保持用户设置的缩进 */ }
    /* 列表样式 */
    ul, ol { padding-left: 2em; margin: 0.5em 0; }
    li { margin: 0.3em 0; }
    /* 加粗、斜体、下划线 */
    strong, b { font-weight: bold; }
    em, i { font-style: italic; }
    u { text-decoration: underline; }
    /* 页码样式 */
    .page-number {
      position: fixed;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      font-family: "SimSun", "宋体", serif;
      font-size: 10pt;
      color: #333;
      text-align: center;
    }
    @media print { 
      body { padding: 15px; } 
      @page { 
        margin: 1.5cm 1.5cm 2cm 1.5cm; 
        size: A4;
        /* 页码样式 - 使用 @bottom-center（现代浏览器支持） */
        @bottom-center {
          content: counter(page);
          font-family: "SimSun", "宋体", serif;
          font-size: 10pt;
          color: #333;
        }
      }
      /* 确保打印时保留内联样式 */
      p[style], div[style], span[style] { /* 内联样式在打印时保持不变 */ }
      /* 审批表一页打印 */
      .approval-form-page { page-break-inside: avoid; }
      /* 打印时隐藏备用页码元素（使用 @page 规则） */
      .page-number { display: none; }
    }
  `;

  // 打印合同
  if (printOptions.printContract) {
    let contractContent = data.contractContent || '';
    let hasSignature = false; // 标记模板是否已包含签署区域

    // 处理合同内容：解码 HTML 实体，处理结构化内容
    if (contractContent) {
      // 先解码 HTML 实体（防止 XSS 过滤导致的乱码）
      contractContent = decodeHtmlEntities(contractContent);

      // 检查是否是结构化内容，如果是则格式化
      if (isStructuredContent(contractContent)) {
        // 准备完整的变量替换数据（包含所有可能用到的变量）
        const signDateStr = data.signDate
          ? new Date(data.signDate).toLocaleDateString('zh-CN', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })
          : '';
        const expiryDateStr = data.expiryDate
          ? new Date(data.expiryDate).toLocaleDateString('zh-CN', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })
          : '完成之日';
        const variables: Record<string, string> = {
          contractNo: data.contractNo || '',
          clientName: data.clientName || '',
          firmName: data.firmName || '',
          signDate: signDateStr,
          matterDescription: data.description || data.opposingParty || '',
          matterName: data.name || '',
          paymentTerms: data.paymentTerms || '一次性支付',
          expiryDate: expiryDateStr,
          clientAddress: data.clientAddress || '',
          clientPhone: data.clientPhone || '',
          firmAddress: data.firmAddress || '',
          firmPhone: data.firmPhone || '',
          firmLegalRep: data.firmLegalRep || '',
        };
        contractContent = formatStructuredForPrint(contractContent, variables);
        // 结构化内容可能已包含签署区域，检查一下
        if (
          contractContent.includes('甲方（签章）') ||
          contractContent.includes('乙方（签章）')
        ) {
          hasSignature = true;
        }
      } else {
        // 非结构化内容（纯文本或 HTML），检查是否包含变量占位符
        // 如果后端没有完全替换变量，手动替换一次
        if (contractContent.includes('${')) {
          const signDateStr = data.signDate
            ? new Date(data.signDate).toLocaleDateString('zh-CN', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })
            : '';
          const expiryDateStr = data.expiryDate
            ? new Date(data.expiryDate).toLocaleDateString('zh-CN', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })
            : '完成之日';

          // 替换所有可能的变量
          contractContent = contractContent
            .replace(/\$\{contractNo\}/g, data.contractNo || '')
            .replace(/\$\{clientName\}/g, data.clientName || '')
            .replace(/\$\{firmName\}/g, data.firmName || '')
            .replace(/\$\{signDate\}/g, signDateStr)
            .replace(
              /\$\{matterDescription\}/g,
              data.description || data.opposingParty || '',
            )
            .replace(/\$\{matterName\}/g, data.name || '')
            .replace(/\$\{paymentTerms\}/g, data.paymentTerms || '一次性支付')
            .replace(/\$\{expiryDate\}/g, expiryDateStr)
            .replace(/\$\{clientAddress\}/g, data.clientAddress || '')
            .replace(/\$\{clientPhone\}/g, data.clientPhone || '')
            .replace(/\$\{firmAddress\}/g, data.firmAddress || '')
            .replace(/\$\{firmPhone\}/g, data.firmPhone || '')
            .replace(/\$\{firmLegalRep\}/g, data.firmLegalRep || '');
        }

        // 检查是否已包含签署区域
        if (
          contractContent.includes('甲方（签章）') ||
          contractContent.includes('甲方（委托人）')
        ) {
          hasSignature = true;
        }
      }
    }

    if (!contractContent) {
      // 如果没有模板内容，生成基本合同信息
      contractContent = `
        <div class="info"><strong>委托人（甲方）：</strong>${data.clientName || ''}</div>
        <div class="info"><strong>受托人（乙方）：</strong>${data.firmName || ''}</div>
        <div class="info"><strong>合同类型：</strong>${data.contractTypeName || ''}</div>
        <div class="info"><strong>收费方式：</strong>${data.feeTypeName || ''}</div>
        <div class="info"><strong>合同金额：</strong>¥${data.totalAmount?.toLocaleString() || ''}</div>
        <div class="info"><strong>签约日期：</strong>${data.signDate || ''}</div>
        ${data.opposingParty ? `<div class="info"><strong>对方当事人：</strong>${data.opposingParty}</div>` : ''}
        ${data.claimAmount ? `<div class="info"><strong>标的金额：</strong>¥${data.claimAmount.toLocaleString()}</div>` : ''}
        ${data.jurisdictionCourt ? `<div class="info"><strong>管辖法院：</strong>${data.jurisdictionCourt}</div>` : ''}
      `;
    }

    // 检查合同内容中是否已包含合同编号（避免重复）
    const contractNoInContent = contractContent.includes(
      `合同编号：${data.contractNo}`,
    );

    htmlContent += `
      <div class="contract-page" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">
        ${!contractNoInContent ? `<div class="header-info">合同编号：${data.contractNo}</div>` : ''}
        <div style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${contractContent}</div>
        ${
          !hasSignature
            ? `
        <div class="signature">
          <div class="signature-box">
            <p><strong>甲方（委托人）：</strong></p>
            <div class="signature-line"></div>
            <p>签字/盖章：</p>
            <div class="signature-line"></div>
            <p>日期：_______年_______月_______日</p>
          </div>
          <div class="signature-box">
            <p><strong>乙方（受托人）：</strong></p>
            <div class="signature-line"></div>
            <p>签字/盖章：</p>
            <div class="signature-line"></div>
            <p>日期：_______年_______月_______日</p>
          </div>
        </div>
        `
            : ''
        }
      </div>
    `;

    if (printOptions.printApprovalForm) {
      htmlContent += '<div class="page-break"></div>';
    }
  }

  // 打印收案审批表
  if (printOptions.printApprovalForm) {
    const signDateStr = data.signDate
      ? new Date(data.signDate).toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        })
      : '____年____月____日';

    // 案由：将代码转换为名称（确保转为字符串进行查找）
    const causeCode = data.causeOfAction ? String(data.causeOfAction) : '';
    let causeOfActionDisplay = '';

    // 对于刑事案件，优先显示罪名名称，避免显示"刑事案件"
    if (data.caseType === 'CRIMINAL') {
      if (causeCode) {
        causeOfActionDisplay =
          findCauseNameInAll(causeCode) || data.causeOfActionName || causeCode;
      } else {
        causeOfActionDisplay = data.causeOfActionName || '';
      }
    } else {
      // 民事和行政案件使用原有逻辑
      causeOfActionDisplay = causeCode
        ? findCauseNameInAll(causeCode) || data.causeOfActionName || causeCode
        : data.causeOfActionName || data.caseTypeName || '';
    }

    // 获取审批信息
    // 接待律师（申请人）意见固定为"拟接受委托"
    // 律所领导意见从审批记录中获取
    const receptionLawyerName = data.originatorName || data.signerName || '';
    let approvalRows = '';

    // 接待律师意见（申请人）
    approvalRows += `
      <tr>
        <th rowspan="3" style="width: 90px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">接待律师意见</th>
        <td colspan="3" style="height: 50px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">拟接受委托</td>
      </tr>
      <tr>
        <td class="no-border text-right" colspan="2" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">签名：</td>
        <td class="no-border" style="width: 130px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${receptionLawyerName}</td>
      </tr>
      <tr>
        <td class="no-border text-right" colspan="2" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">日期：</td>
        <td class="no-border" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${signDateStr}</td>
      </tr>
    `;

    // 律所领导意见（审批人）
    if (data.approvals && data.approvals.length > 0) {
      const leaderApproval = data.approvals[0]; // 第一个审批人是真正的审批人
      const leaderDate = leaderApproval?.approvedAt
        ? new Date(leaderApproval.approvedAt).toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
          })
        : '____年____月____日';

      approvalRows += `
        <tr>
          <th rowspan="3" style="width: 90px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">律所领导意见</th>
          <td colspan="3" style="height: 50px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${leaderApproval?.comment || ''}</td>
        </tr>
        <tr>
          <td class="no-border text-right" colspan="2" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">签名：</td>
          <td class="no-border" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${leaderApproval?.approverName || ''}</td>
        </tr>
        <tr>
          <td class="no-border text-right" colspan="2" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">日期：</td>
          <td class="no-border" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${leaderDate}</td>
        </tr>
      `;
    } else {
      // 预留律所领导签字区域
      approvalRows += `
        <tr>
          <th rowspan="3" style="width: 90px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">律所领导意见</th>
          <td colspan="3" style="height: 50px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;"></td>
        </tr>
        <tr>
          <td class="no-border text-right" colspan="2" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">签名：</td>
          <td class="no-border" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;"></td>
        </tr>
        <tr>
          <td class="no-border text-right" colspan="2" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">日期：</td>
          <td class="no-border" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">____年____月____日</td>
        </tr>
      `;
    }

    htmlContent += `
      <div class="approval-form-page" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">
        <h1 style="font-family: 'SimSun', '宋体', serif; font-size: 16pt; margin-bottom: 5px;">${data.firmName || ''}</h1>
        <h2 style="font-family: 'SimSun', '宋体', serif; font-size: 16pt; margin-bottom: 5px;">收案审批表</h2>
        <p style="text-align: right; font-size: 11pt; margin-bottom: 10px; color: #333;">合同编号：${data.contractNo || ''}</p>
        <table style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">
          <tr>
            <th style="width: 90px;">委托人</th>
            <td colspan="3">${data.clientName || ''}</td>
          </tr>
          <tr>
            <th style="width: 90px;">案由</th>
            <td colspan="3">${causeOfActionDisplay}</td>
          </tr>
          <tr>
            <th style="width: 90px;">关联当事人</th>
            <td colspan="3">${data.opposingParty || ''}</td>
          </tr>
          <tr>
            <th style="width: 90px;">委托程序</th>
            <td colspan="3">${data.trialStageName || ''}</td>
          </tr>
          <tr>
            <th style="width: 90px;">有无利益冲突</th>
            <td colspan="3">${data.conflictCheckResult || '待审查'}</td>
          </tr>
          <tr>
            <th style="width: 90px;">代理/辩护费</th>
            <td>${data.totalAmount ? `¥${data.totalAmount.toLocaleString()}` : ''}</td>
            <th style="width: 70px;">委托时间</th>
            <td style="width: 130px;">${signDateStr}</td>
          </tr>
          <tr>
            <th style="width: 90px;">接待人</th>
            <td>${data.originatorName || data.signerName || ''}</td>
            <th style="width: 70px;">办案单位</th>
            <td>${data.jurisdictionCourt || ''}</td>
          </tr>
          <tr>
            <th style="width: 90px; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">案情摘要<br/><span style="font-weight: normal; font-size: 10pt; font-family: 'SimSun', '宋体', serif;">（附接待笔录）</span></th>
            <td colspan="3" style="height: 140px; vertical-align: top; font-family: 'SimSun', '宋体', serif; font-size: 12pt;">${data.description || ''}</td>
          </tr>
        </table>
        
        <div class="approval-section" style="font-family: 'SimSun', '宋体', serif; font-size: 12pt; margin-top: 10px;">
          <h3 style="text-align: center; margin-bottom: 8px; font-family: 'SimSun', '宋体', serif; font-size: 14pt;">审 查 意 见</h3>
          <table style="font-family: 'SimSun', '宋体', serif; font-size: 12pt;">
            ${approvalRows}
          </table>
        </div>
      </div>
    `;
  }

  // 生成完整 HTML
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>${data.name} - ${data.contractNo}</title>
      <style>${commonStyles}</style>
    </head>
    <body>
      ${htmlContent}
    </body>
    </html>
  `);

  printWindow.document.close();
  printModalVisible.value = false;

  setTimeout(() => {
    printWindow.print();
  }, 500);
}

// 查询
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  selectedYear.value = currentYear;
  Object.assign(queryParams, {
    contractNo: undefined,
    name: undefined,
    clientId: undefined,
    contractType: undefined,
    feeType: undefined,
    status: undefined,
    signerId: undefined,
    createdAtFrom: `${currentYear}-01-01T00:00:00`,
    createdAtTo: `${currentYear}-12-31T23:59:59`,
    pageNum: 1,
  });
  fetchData();
}

// 新增
async function handleAdd() {
  handleResetForm();
  selectedTemplateId.value = undefined;
  selectedTemplate.value = null;

  // 确保选项数据已加载（客户、部门、模板等）
  if (clients.value.length === 0 || contractTemplates.value.length === 0) {
    await loadOptions();
  }

  modalVisible.value = true;
}

// 查看详情
async function handleView(record: ContractDTO) {
  try {
    // 确保提成方案数据已加载
    if (commissionRules.value.length === 0) {
      await loadOptions();
    }

    const detail = await getContractDetail(record.id);
    currentContract.value = detail;
    activeTab.value = 'info';
    detailModalVisible.value = true;
    // 加载付款计划和参与人
    await Promise.all([
      loadPaymentSchedules(record.id),
      loadParticipants(record.id),
    ]);
  } catch (error: any) {
    message.error(error.message || '加载合同详情失败');
  }
}

// 加载付款计划
async function loadPaymentSchedules(contractId: number) {
  try {
    paymentSchedules.value = await getContractPaymentSchedules(contractId);
  } catch (error: any) {
    console.error('加载付款计划失败', error);
  }
}

// 加载参与人
async function loadParticipants(contractId: number) {
  try {
    participants.value = await getContractParticipants(contractId);
  } catch (error: any) {
    console.error('加载参与人失败', error);
  }
}

// 编辑合同
async function handleEdit(record: ContractDTO) {
  try {
    const detail = await getContractDetail(record.id);

    // 编辑时清除模板选择
    selectedTemplateId.value = undefined;

    // 设置案由级联值
    causeValue.value = detail.causeOfAction ? [detail.causeOfAction] : [];

    // 如果是刑事案件，加载刑事罪名选项并设置级联值
    if (detail.caseType === 'CRIMINAL') {
      await loadCriminalChargeOptions();
      // 设置刑事罪名级联值（刑事案件中，案由代码就是罪名代码）
      criminalChargeValue.value = detail.causeOfAction
        ? [detail.causeOfAction]
        : [];
    } else {
      criminalChargeValue.value = [];
    }

    // 填充表单数据
    formData.id = detail.id;
    formData.name = detail.name || '';
    formData.clientId = detail.clientId;
    formData.contractType = detail.contractType || 'CIVIL_PROXY';
    formData.feeType = detail.feeType || 'FIXED';
    formData.totalAmount = detail.totalAmount;
    formData.currency = detail.currency || 'CNY';
    formData.signDate = detail.signDate ? dayjs(detail.signDate) : undefined;
    formData.effectiveDate = detail.effectiveDate
      ? dayjs(detail.effectiveDate)
      : undefined;
    formData.expiryDate = detail.expiryDate
      ? dayjs(detail.expiryDate)
      : undefined;
    formData.signerId = detail.signerId;
    formData.departmentId = detail.departmentId;
    formData.paymentTerms = detail.paymentTerms || '';
    formData.remark = detail.remark || '';
    // 扩展字段
    formData.caseType = detail.caseType;
    formData.causeOfAction = detail.causeOfAction;
    // 加载审理阶段选项（编辑时先加载选项，再填充数据）
    await loadTrialStageOptions(detail.caseType);
    // trialStage 支持多选，后端存储为逗号分隔字符串
    formData.trialStage = detail.trialStage ? detail.trialStage.split(',') : [];
    formData.claimAmount = detail.claimAmount;
    formData.jurisdictionCourt = detail.jurisdictionCourt || '';
    formData.opposingParty = detail.opposingParty || '';
    formData.caseSummary = detail.caseSummary || '';
    formData.conflictCheckStatus = detail.conflictCheckStatus || 'NOT_REQUIRED';
    formData.advanceTravelFee = detail.advanceTravelFee;
    formData.riskRatio = detail.riskRatio;

    // 刑事案件字段（从详情中获取，如果后端返回的话）
    const detailAny = detail as any;
    formData.defendantName = detailAny.defendantName || '';
    formData.criminalCharge = detailAny.criminalCharge || '';
    // defenseStage 支持多选，后端存储为逗号分隔字符串
    formData.defenseStage = detailAny.defenseStage
      ? detailAny.defenseStage.split(',')
      : [];

    // 模板变量字段（从详情中获取，如果后端返回的话）
    formData.lawyerNames = detailAny.lawyerNames || '';
    formData.assistantNames = detailAny.assistantNames || '';
    formData.authorizationType = detailAny.authorizationType || '一般代理';
    formData.paymentDeadline = detailAny.paymentDeadline || '';
    formData.disputeResolution = detailAny.disputeResolution || '1';
    formData.arbitrationCommittee = detailAny.arbitrationCommittee || '';
    formData.specialTerms = detailAny.specialTerms || '';
    formData.partnerRate = detailAny.partnerRate;
    formData.seniorRate = detailAny.seniorRate;
    formData.assistantRate = detailAny.assistantRate;
    formData.serviceHours = detailAny.serviceHours;

    // 提成方案
    selectedCommissionRuleId.value = detail.commissionRuleId;
    if (detail.commissionRuleId) {
      const rule = commissionRules.value.find(
        (r) => r.id === detail.commissionRuleId,
      );
      selectedCommissionRule.value = rule || null;
    } else {
      selectedCommissionRule.value = null;
    }
    Object.assign(commissionFormData, {
      firmRate: detail.firmRate || 0,
      leadLawyerRate: detail.leadLawyerRate || 0,
      assistLawyerRate: detail.assistLawyerRate || 0,
      supportStaffRate: detail.supportStaffRate || 0,
      originatorRate: detail.originatorRate || 0,
    });

    // 加载现有的参与人列表（编辑模式下需要）
    const participants = await getContractParticipants(record.id);
    contractParticipants.value = participants.map((p) => ({
      userId: p.userId,
      role: p.role,
      commissionRate: p.commissionRate,
    }));

    modalTitle.value = '编辑合同';
    modalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '加载合同详情失败');
  }
}

// 保存
async function handleSave() {
  try {
    saving.value = true;

    // 创建合同时必须选择模板
    if (!formData.id && !selectedTemplateId.value) {
      message.error('请先选择合同模板');
      saving.value = false;
      return;
    }

    // 表单验证
    if (!formRef.value) {
      message.error('表单未初始化');
      saving.value = false;
      return;
    }

    try {
      await formRef.value.validate();
    } catch (validateError: any) {
      // 表单验证失败
      if (validateError?.errorFields && validateError.errorFields.length > 0) {
        // Ant Design Vue 会自动显示验证错误
        saving.value = false;
        return;
      }
      // 其他类型的错误
      message.error('表单验证失败，请检查必填字段');
      saving.value = false;
      return;
    }

    if (!formData.clientId) {
      message.error('请选择客户');
      saving.value = false;
      return;
    }
    if (!formData.totalAmount) {
      message.error('请输入合同金额');
      saving.value = false;
      return;
    }

    const baseData: any = {
      name: formData.name || '',
      clientId: formData.clientId,
      contractType: formData.contractType || 'CIVIL_PROXY',
      feeType: formData.feeType || 'FIXED',
      totalAmount: formData.totalAmount || 0,
      currency: formData.currency,
      signDate: formData.signDate,
      effectiveDate: formData.effectiveDate,
      expiryDate: formData.expiryDate,
      signerId: formData.signerId,
      departmentId: formData.departmentId,
      paymentTerms: formData.paymentTerms,
      remark: formData.remark,
      // 扩展字段
      caseType: formData.caseType,
      causeOfAction: formData.causeOfAction,
      // trialStage 多选，转换为逗号分隔字符串存储
      trialStage: Array.isArray(formData.trialStage)
        ? formData.trialStage.join(',')
        : formData.trialStage,
      claimAmount: formData.claimAmount,
      jurisdictionCourt: formData.jurisdictionCourt,
      opposingParty: formData.opposingParty,
      caseSummary: formData.caseSummary,
      conflictCheckStatus: formData.conflictCheckStatus,
      advanceTravelFee: formData.advanceTravelFee,
      riskRatio: formData.riskRatio,
      // 提成分配方案
      commissionRuleId: selectedCommissionRuleId.value,
      firmRate: commissionFormData.firmRate,
      leadLawyerRate: commissionFormData.leadLawyerRate,
      assistLawyerRate: commissionFormData.assistLawyerRate,
      supportStaffRate: commissionFormData.supportStaffRate,
      originatorRate: commissionFormData.originatorRate,
      // 模板变量字段
      lawyerNames: formData.lawyerNames,
      assistantNames: formData.assistantNames,
      authorizationType: formData.authorizationType,
      paymentDeadline: formData.paymentDeadline,
      disputeResolution: formData.disputeResolution,
      arbitrationCommittee: formData.arbitrationCommittee,
      specialTerms: formData.specialTerms,
      defendantName: formData.defendantName,
      criminalCharge: formData.criminalCharge,
      // defenseStage 多选，转换为逗号分隔字符串存储
      defenseStage: Array.isArray(formData.defenseStage)
        ? formData.defenseStage.join(',')
        : formData.defenseStage,
      partnerRate: formData.partnerRate,
      seniorRate: formData.seniorRate,
      assistantRate: formData.assistantRate,
      serviceHours: formData.serviceHours,
      // 金额大写（前端计算）
      totalAmountCN: totalAmountChinese.value,
      totalAmountChinese: totalAmountChinese.value,
      claimAmountChinese: claimAmountChinese.value,
    };

    // 确保提成比例数据正确：如果选择了方案，优先使用 commissionFormData 的值（可能被用户修改过），否则使用方案的值
    if (selectedCommissionRule.value) {
      // 如果 commissionFormData 的值是 undefined 或 null，使用方案的值
      // 否则使用 commissionFormData 的值（可能被用户修改过，如果方案允许修改）
      baseData.firmRate =
        commissionFormData.firmRate !== undefined &&
        commissionFormData.firmRate !== null
          ? commissionFormData.firmRate
          : selectedCommissionRule.value.firmRate;
      baseData.leadLawyerRate =
        commissionFormData.leadLawyerRate !== undefined &&
        commissionFormData.leadLawyerRate !== null
          ? commissionFormData.leadLawyerRate
          : selectedCommissionRule.value.leadLawyerRate;
      baseData.assistLawyerRate =
        commissionFormData.assistLawyerRate !== undefined &&
        commissionFormData.assistLawyerRate !== null
          ? commissionFormData.assistLawyerRate
          : selectedCommissionRule.value.assistLawyerRate;
      baseData.supportStaffRate =
        commissionFormData.supportStaffRate !== undefined &&
        commissionFormData.supportStaffRate !== null
          ? commissionFormData.supportStaffRate
          : selectedCommissionRule.value.supportStaffRate;
      baseData.originatorRate =
        commissionFormData.originatorRate !== undefined &&
        commissionFormData.originatorRate !== null
          ? commissionFormData.originatorRate
          : selectedCommissionRule.value.originatorRate || 0;
    }

    let createdContractId: number | undefined;

    if (formData.id) {
      // 编辑合同
      await updateContract({ id: formData.id, ...baseData });
      message.success('合同更新成功');
    } else if (selectedTemplateId.value) {
      // 基于模板创建合同
      const contractResult = await createContractFromTemplate(
        selectedTemplateId.value,
        baseData,
      );
      createdContractId = contractResult?.id;
      message.success('合同创建成功（基于模板），请提交审批');
    } else {
      // 直接创建合同（不应该走到这里，因为前面已经检查了）
      const contractResult = await createContract(baseData);
      createdContractId = contractResult?.id;
      message.success('合同创建成功，请提交审批');
    }

    // 处理参与人（新建合同时创建，编辑合同时更新）
    if (contractParticipants.value.length > 0) {
      try {
        for (const participant of contractParticipants.value) {
          if (participant.userId) {
            if (formData.id) {
              // 编辑合同时，尝试更新现有参与人或创建新参与人
              const existingParticipants = await getContractParticipants(
                formData.id,
              );
              const existingParticipant = existingParticipants.find(
                (p) =>
                  p.userId === participant.userId &&
                  p.role === participant.role,
              );

              if (existingParticipant) {
                // 更新现有参与人
                await updateContractParticipant(
                  formData.id,
                  existingParticipant.id,
                  {
                    role: participant.role || 'LEAD',
                    commissionRate: participant.commissionRate,
                    remark: '编辑合同时更新',
                  },
                );
              } else {
                // 创建新参与人
                await createContractParticipant(formData.id, {
                  userId: participant.userId,
                  role: participant.role || 'LEAD',
                  commissionRate: participant.commissionRate,
                  remark: '编辑合同时添加',
                });
              }
            } else if (createdContractId) {
              // 新建合同时创建参与人
              await createContractParticipant(createdContractId, {
                userId: participant.userId,
                role: participant.role || 'LEAD',
                commissionRate: participant.commissionRate,
                remark: '创建合同时添加',
              });
            }
          }
        }
      } catch {
        // 参与人处理失败不影响合同保存
      }
    }

    modalVisible.value = false;
    handleResetForm();
    selectedTemplateId.value = undefined;
    await fetchData();
  } catch (error: any) {
    // API 错误
    console.error('保存合同失败:', error);
    const errorMsg =
      error?.response?.data?.message ||
      error?.message ||
      error?.msg ||
      (formData.id ? '更新合同失败' : '创建合同失败');
    message.error(errorMsg);
  } finally {
    saving.value = false;
  }
}

// 添加合同参与人（创建合同时）
function handleAddContractParticipant() {
  // 根据方案自动决定默认角色
  // 优先选择方案中有比例但参与人列表中还没有的角色
  let defaultRole: string | undefined;
  let defaultRate: number | undefined;

  if (selectedCommissionRule.value) {
    const rule = selectedCommissionRule.value;

    // 检查主办律师：如果方案中有比例且参与人列表中还没有主办律师
    if (
      rule.leadLawyerRate > 0 &&
      !contractParticipants.value.find((p) => p.role === 'LEAD')
    ) {
      defaultRole = 'LEAD';
      defaultRate = rule.leadLawyerRate;
    }
    // 检查协办律师：如果方案中有比例且参与人列表中还没有协办律师
    else if (
      rule.assistLawyerRate > 0 &&
      !contractParticipants.value.find((p) => p.role === 'CO_COUNSEL')
    ) {
      defaultRole = 'CO_COUNSEL';
      defaultRate = rule.assistLawyerRate;
    }
    // 检查辅助人员：如果方案中有比例且参与人列表中还没有辅助人员
    else if (
      rule.supportStaffRate > 0 &&
      !contractParticipants.value.find((p) => p.role === 'PARALEGAL')
    ) {
      defaultRole = 'PARALEGAL';
      defaultRate = rule.supportStaffRate;
    }
    // 检查案源人：如果方案中有比例且参与人列表中还没有案源人
    else if (
      rule.originatorRate &&
      rule.originatorRate > 0 &&
      !contractParticipants.value.find((p) => p.role === 'ORIGINATOR')
    ) {
      defaultRole = 'ORIGINATOR';
      defaultRate = rule.originatorRate;
    }
    // 如果所有角色都已添加，默认添加主办律师（即使比例为0）
    else {
      defaultRole = 'LEAD';
      defaultRate = rule.leadLawyerRate || 0;
    }
  } else {
    // 如果没有选择方案，默认添加主办律师
    defaultRole = 'LEAD';
    defaultRate = undefined;
  }

  contractParticipants.value.push({
    userId: undefined,
    role: defaultRole,
    commissionRate: defaultRate,
  });
}

// 参与人角色改变时，自动更新提成比例
function handleParticipantRoleChange(index: number, role: string | undefined) {
  const participant = contractParticipants.value[index];
  if (participant && role) {
    participant.role = role;
    // 如果已选择提成方案，自动从方案中获取对应角色的比例
    const rate = getCommissionRateByRole(role);
    if (rate !== undefined) {
      participant.commissionRate = rate;
    }
  }
}

// 重置表单
function handleResetForm() {
  causeValue.value = [];
  contractParticipants.value = []; // 重置参与人列表
  Object.assign(formData, {
    id: undefined,
    name: '',
    clientId: undefined,
    contractType: 'CIVIL_PROXY',
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
    caseType: undefined,
    causeOfAction: undefined,
    trialStage: [], // 支持多选，重置为空数组
    claimAmount: undefined,
    jurisdictionCourt: '',
    opposingParty: '',
    caseSummary: '',
    conflictCheckStatus: 'NOT_REQUIRED',
    advanceTravelFee: undefined,
    riskRatio: undefined,
  });

  // 重置提成方案为默认
  const defaultRule = commissionRules.value.find((r) => r.isDefault);
  if (defaultRule) {
    handleCommissionRuleChange(defaultRule.id);
  } else {
    selectedCommissionRuleId.value = undefined;
    selectedCommissionRule.value = null;
    Object.assign(commissionFormData, {
      firmRate: 0,
      leadLawyerRate: 0,
      assistLawyerRate: 0,
      supportStaffRate: 0,
      originatorRate: 0,
    });
  }

  modalTitle.value = '创建合同';
}

// 审批人选择相关
interface ApproverOption {
  id: number;
  realName: string;
  departmentName: string;
  position: string;
}
const approverModalVisible = ref(false);
const approverOptions = ref<ApproverOption[]>([]);
const selectedApproverId = ref<number | undefined>(undefined);
const pendingSubmitContract = ref<ContractDTO | null>(null);

// 提交审批
async function handleSubmit(record: ContractDTO) {
  try {
    // 加载可选审批人
    const approvers = await getContractApprovers();
    approverOptions.value = approvers || [];

    // 始终弹出选择审批人的弹窗
    pendingSubmitContract.value = record;
    selectedApproverId.value = undefined;
    approverModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取审批人列表失败');
  }
}

// 确认提交审批（选择审批人后）
async function handleConfirmSubmit() {
  if (!pendingSubmitContract.value) return;

  if (approverOptions.value.length > 0 && !selectedApproverId.value) {
    message.warning('请选择审批人');
    return;
  }

  try {
    await submitContract(
      pendingSubmitContract.value.id,
      selectedApproverId.value,
    );
    message.success('提交审批成功');
    approverModalVisible.value = false;
    pendingSubmitContract.value = null;
    selectedApproverId.value = undefined;
    fetchData();
  } catch (error: any) {
    message.error(error.message || '提交审批失败');
  }
}

// 审批通过
async function handleApprove(record: ContractDTO) {
  try {
    const approvals = await getBusinessApprovals('CONTRACT', record.id);
    const pendingApproval = approvals.find((a) => a.status === 'PENDING');

    if (!pendingApproval) {
      message.warning('未找到待审批的审批单');
      return;
    }

    Modal.confirm({
      title: '确认审批',
      content: `确定要审批通过合同 "${record.name}" 吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await approveApproval({
            approvalId: pendingApproval.id,
            result: 'APPROVED',
            comment: '',
          });
          message.success('审批通过成功');
          fetchData();
        } catch (error: any) {
          message.error(error.message || '审批失败');
        }
      },
    });
  } catch (error: any) {
    message.error(error.message || '获取审批单失败');
  }
}

// 审批拒绝
async function handleReject(record: ContractDTO) {
  try {
    const approvals = await getBusinessApprovals('CONTRACT', record.id);
    const pendingApproval = approvals.find((a) => a.status === 'PENDING');

    if (!pendingApproval) {
      message.warning('未找到待审批的审批单');
      return;
    }

    const rejectReasonRef = ref<string>('');

    Modal.confirm({
      title: '拒绝审批',
      width: 500,
      content: () =>
        h('div', [
          h(
            'p',
            { style: 'margin-bottom: 12px' },
            `确定要拒绝合同 "${record.name}" 吗？`,
          ),
          h(Textarea, {
            value: rejectReasonRef.value,
            placeholder: '请输入拒绝事由（必填）',
            rows: 4,
            'onUpdate:value': (value: string) => {
              rejectReasonRef.value = value;
            },
          }),
        ]),
      okText: '确认拒绝',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        if (!rejectReasonRef.value?.trim()) {
          message.error('拒绝时必须填写拒绝事由');
          throw undefined;
        }
        try {
          await approveApproval({
            approvalId: pendingApproval.id,
            result: 'REJECTED',
            comment: rejectReasonRef.value.trim(),
          });
          message.success('已拒绝');
          fetchData();
        } catch (error: any) {
          message.error(error.message || '操作失败');
        }
      },
    });
  } catch (error: any) {
    message.error(error.message || '获取审批单失败');
  }
}

// 撤回审批（合同创建者或签约人可以撤回待审批的合同）
async function handleWithdraw(record: ContractDTO) {
  Modal.confirm({
    title: '撤回审批',
    content: `确定要撤回合同 "${record.name}" 的审批吗？撤回后合同将恢复为草稿状态，您可以修改后重新提交。`,
    okText: '撤回',
    cancelText: '取消',
    onOk: async () => {
      try {
        await withdrawContract(record.id);
        message.success('已撤回审批，合同已恢复为草稿状态');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '撤回失败');
      }
    },
  });
}

// 申请合同变更
async function handleChange(record: ContractDTO) {
  try {
    const detail = await getContractDetail(record.id);
    Object.assign(changeFormData, {
      contractId: detail.id,
      changeReason: '',
      changeDescription: '',
      name: detail.name || '',
      contractType: detail.contractType || 'CIVIL_PROXY',
      clientId: detail.clientId,
      feeType: detail.feeType || 'FIXED',
      totalAmount: detail.totalAmount,
      currency: detail.currency || 'CNY',
      signDate: detail.signDate ? dayjs(detail.signDate) : undefined,
      effectiveDate: detail.effectiveDate
        ? dayjs(detail.effectiveDate)
        : undefined,
      expiryDate: detail.expiryDate ? dayjs(detail.expiryDate) : undefined,
      signerId: detail.signerId,
      departmentId: detail.departmentId,
      paymentTerms: detail.paymentTerms || '',
      remark: detail.remark || '',
    });
    changeModalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '加载合同详情失败');
  }
}

// 提交变更申请
async function handleSubmitChange() {
  try {
    await changeFormRef.value?.validate();
    if (!changeFormData.changeReason?.trim()) {
      message.error('请输入变更原因');
      return;
    }

    await applyContractChange({
      contractId: changeFormData.contractId!,
      changeReason: changeFormData.changeReason,
      changeDescription: changeFormData.changeDescription,
      name: changeFormData.name,
      contractType: changeFormData.contractType,
      clientId: changeFormData.clientId,
      feeType: changeFormData.feeType,
      totalAmount: changeFormData.totalAmount,
      currency: changeFormData.currency,
      signDate:
        changeFormData.signDate?.format?.('YYYY-MM-DD') ||
        changeFormData.signDate,
      effectiveDate:
        changeFormData.effectiveDate?.format?.('YYYY-MM-DD') ||
        changeFormData.effectiveDate,
      expiryDate:
        changeFormData.expiryDate?.format?.('YYYY-MM-DD') ||
        changeFormData.expiryDate,
      signerId: changeFormData.signerId,
      departmentId: changeFormData.departmentId,
      paymentTerms: changeFormData.paymentTerms,
      remark: changeFormData.remark,
    });
    message.success('变更申请提交成功，等待审批');
    changeModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '提交变更申请失败');
  }
}

// 基于合同创建项目
function handleCreateMatter(record: ContractDTO) {
  if (record.status !== 'ACTIVE') {
    message.warning('只能基于已审批通过的合同创建项目');
    return;
  }
  router.push({ path: '/matter/list', query: { fromContract: record.id } });
}

// 创建客户
function handleCreateClient() {
  router.push({
    path: '/crm/client',
    query: { action: 'create', returnPath: '/matter/contract' },
  });
}

// ========== 付款计划管理 ==========
function handleAddSchedule() {
  Object.assign(scheduleFormData, {
    id: undefined,
    phaseName: '',
    amount: undefined,
    percentage: undefined,
    plannedDate: undefined,
    remark: '',
  });
  scheduleModalVisible.value = true;
}

function handleEditSchedule(record: ContractPaymentScheduleDTO) {
  Object.assign(scheduleFormData, {
    id: record.id,
    phaseName: record.phaseName,
    amount: record.amount,
    percentage: record.percentage,
    plannedDate: record.plannedDate ? dayjs(record.plannedDate) : undefined,
    remark: record.remark,
  });
  scheduleModalVisible.value = true;
}

async function handleSaveSchedule() {
  try {
    await scheduleFormRef.value?.validate();
    if (!currentContract.value) return;

    const data = {
      phaseName: scheduleFormData.phaseName || '',
      amount: scheduleFormData.amount,
      percentage: scheduleFormData.percentage,
      plannedDate:
        typeof scheduleFormData.plannedDate === 'string'
          ? scheduleFormData.plannedDate
          : (scheduleFormData.plannedDate as any)?.format?.('YYYY-MM-DD'),
      remark: scheduleFormData.remark,
    };

    if (scheduleFormData.id) {
      await updatePaymentSchedule(
        currentContract.value.id,
        scheduleFormData.id,
        data,
      );
      message.success('付款计划更新成功');
    } else {
      await createPaymentSchedule(currentContract.value.id, data);
      message.success('付款计划创建成功');
    }
    scheduleModalVisible.value = false;
    await loadPaymentSchedules(currentContract.value.id);
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '保存付款计划失败');
  }
}

async function handleDeleteSchedule(record: ContractPaymentScheduleDTO) {
  if (!currentContract.value) return;
  try {
    await deletePaymentSchedule(currentContract.value.id, record.id);
    message.success('删除成功');
    await loadPaymentSchedules(currentContract.value.id);
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// ========== 参与人管理 ==========
function handleAddParticipant() {
  Object.assign(participantFormData, {
    id: undefined,
    userId: undefined,
    role: 'CO_COUNSEL',
    commissionRate: undefined,
    remark: '',
  });
  participantModalVisible.value = true;
}

function handleEditParticipant(record: ContractParticipantDTO) {
  Object.assign(participantFormData, {
    id: record.id,
    userId: record.userId,
    role: record.role,
    commissionRate: record.commissionRate,
    remark: record.remark,
  });
  participantModalVisible.value = true;
}

async function handleSaveParticipant() {
  try {
    await participantFormRef.value?.validate();
    if (!currentContract.value) return;

    const data = {
      userId: participantFormData.userId!,
      role: participantFormData.role || 'CO_COUNSEL',
      commissionRate: participantFormData.commissionRate,
      remark: participantFormData.remark,
    };

    if (participantFormData.id) {
      await updateContractParticipant(
        currentContract.value.id,
        participantFormData.id,
        data,
      );
      message.success('参与人更新成功');
    } else {
      await createContractParticipant(currentContract.value.id, data);
      message.success('参与人添加成功');
    }
    participantModalVisible.value = false;
    await loadParticipants(currentContract.value.id);
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '保存参与人失败');
  }
}

async function handleDeleteParticipant(record: ContractParticipantDTO) {
  if (!currentContract.value) return;
  try {
    await deleteContractParticipant(currentContract.value.id, record.id);
    message.success('删除成功');
    await loadParticipants(currentContract.value.id);
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PENDING: 'orange',
    ACTIVE: 'green',
    REJECTED: 'red',
    TERMINATED: 'gray',
    COMPLETED: 'blue',
  };
  return colorMap[status] || 'default';
}

// 格式化金额
function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

onMounted(async () => {
  fetchData();
  loadStatistics();
  await loadOptions();
});
</script>

<template>
  <Page
    title="合同管理"
    description="管理项目合同，创建合同后可提交审批，审批通过后可基于合同创建项目"
  >
    <!-- 统计卡片 -->
    <Row :gutter="[16, 16]" style="margin-bottom: 16px" v-if="statistics">
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card>
          <Statistic title="合同总数" :value="statistics.totalCount" />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card>
          <Statistic
            title="生效中"
            :value="statistics.activeCount"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card>
          <Statistic
            title="合同总金额"
            :value="statistics.totalAmount"
            prefix="¥"
            :precision="2"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card>
          <Statistic
            title="待收金额"
            :value="statistics.unpaidAmount"
            prefix="¥"
            :precision="2"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
    </Row>

    <Card>
      <!-- 操作栏 -->
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 16px;
        "
      >
        <Button type="primary" size="large" @click="handleAdd">
          <template #icon><Plus /></template>
          创建合同
        </Button>
      </div>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <Select
              v-model:value="selectedYear"
              placeholder="创建年份"
              style="width: 100%"
              :options="yearOptions"
              @change="handleYearChange"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <Input
              v-model:value="queryParams.contractNo"
              placeholder="合同编号"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <Input
              v-model:value="queryParams.name"
              placeholder="合同名称"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <Select
              v-model:value="queryParams.feeType"
              placeholder="收费方式"
              allow-clear
              style="width: 100%"
              :options="feeTypeOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <Select
              v-model:value="queryParams.status"
              placeholder="合同状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <UserTreeSelect
              v-model:value="queryParams.signerId"
              placeholder="选择签约人"
              style="width: 100%"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
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
        :scroll="{ x: isMobile ? 600 : 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="getStatusColor((record as ContractDTO).status)">
              {{ (record as ContractDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'totalAmount'">
            {{ formatMoney((record as ContractDTO).totalAmount) }}
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record as ContractDTO)">查看</a>
              <template
                v-if="
                  (record as ContractDTO).status === 'DRAFT' &&
                  canOperateContract(record as ContractDTO)
                "
              >
                <a @click="handleEdit(record as ContractDTO)">编辑</a>
                <a @click="handleSubmit(record as ContractDTO)">提交审批</a>
              </template>
              <template
                v-if="
                  (record as ContractDTO).status === 'REJECTED' &&
                  canOperateContract(record as ContractDTO)
                "
              >
                <a @click="handleEdit(record as ContractDTO)">编辑</a>
                <a @click="handleSubmit(record as ContractDTO)">重新提交</a>
              </template>
              <template v-if="(record as ContractDTO).status === 'PENDING'">
                <a @click="handleApprove(record as ContractDTO)">通过</a>
                <a
                  style="color: red"
                  @click="handleReject(record as ContractDTO)"
                  >拒绝</a
                >
                <a
                  v-if="
                    (record as ContractDTO).createdBy === currentUserId ||
                    (record as ContractDTO).signerId === currentUserId
                  "
                  style="color: #faad14"
                  @click="handleWithdraw(record as ContractDTO)"
                  >撤回</a
                >
              </template>
              <template
                v-if="
                  (record as ContractDTO).status === 'ACTIVE' &&
                  canOperateContract(record as ContractDTO)
                "
              >
                <a @click="handleChange(record as ContractDTO)">变更</a>
                <a @click="handleCreateMatter(record as ContractDTO)"
                  >创建项目</a
                >
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 创建/编辑合同弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="isMobile ? '100%' : '900px'"
      :centered="isMobile"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <div style="max-height: 70vh; overflow-y: auto">
        <Form
          ref="formRef"
          :model="formData"
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 17 }"
        >
          <!-- 第一步：选择模板（仅新建时显示） -->
          <template v-if="!formData.id">
            <div
              style="
                padding: 12px 16px;
                margin-bottom: 16px;
                background: #e6f7ff;
                border: 1px solid #91d5ff;
                border-radius: 4px;
              "
            >
              <FormItem label="合同模板" style="margin-bottom: 8px">
                <div style="display: flex; gap: 8px; align-items: center">
                  <Select
                    v-model:value="selectedTemplateId"
                    placeholder="请选择合同模板（必选）"
                    size="large"
                    style="flex: 1"
                    @change="handleTemplateChange"
                    :options="
                      contractTemplates.map((t) => ({
                        label: t.name,
                        value: t.id,
                      }))
                    "
                  />
                  <Button
                    v-if="selectedTemplate"
                    type="link"
                    @click="handlePreviewTemplate"
                  >
                    预览模板
                  </Button>
                </div>
              </FormItem>

              <!-- 选中模板后显示模板信息 -->
              <div
                v-if="selectedTemplate"
                style="
                  padding: 8px 12px;
                  margin-top: 8px;
                  background: #fff;
                  border: 1px solid #d9d9d9;
                  border-radius: 4px;
                "
              >
                <div
                  style="
                    display: flex;
                    flex-wrap: wrap;
                    gap: 16px;
                    font-size: 13px;
                  "
                >
                  <span
                    ><strong>合同类型：</strong
                    >{{
                      selectedTemplate.contractTypeName ||
                      selectedTemplate.contractType
                    }}</span
                  >
                  <span
                    ><strong>收费方式：</strong
                    >{{
                      selectedTemplate.feeTypeName || selectedTemplate.feeType
                    }}</span
                  >
                </div>
                <div
                  v-if="selectedTemplate.description"
                  style="margin-top: 6px; font-size: 12px; color: #666"
                >
                  <strong>说明：</strong>{{ selectedTemplate.description }}
                </div>
                <Alert
                  v-if="selectedTemplate.content"
                  type="success"
                  style="padding: 6px 12px; margin-top: 8px"
                  show-icon
                >
                  <template #message>
                    <span style="font-size: 12px"
                      >✓
                      该模板包含合同正文内容，创建合同时将自动导入模板内容并填充变量</span
                    >
                  </template>
                </Alert>
                <Alert
                  v-else
                  type="warning"
                  style="padding: 6px 12px; margin-top: 8px"
                  show-icon
                >
                  <template #message>
                    <span style="font-size: 12px"
                      >⚠
                      该模板暂无合同正文内容，仅会应用合同类型和收费方式</span
                    >
                  </template>
                </Alert>
              </div>
            </div>
          </template>

          <!-- 基本信息 -->
          <Divider
            orientation="left"
            style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
          >
            基本信息
          </Divider>

          <FormItem
            label="委托人（甲方）"
            name="clientId"
            :rules="[{ required: true, message: '请选择客户' }]"
          >
            <div style="display: flex; gap: 8px">
              <Select
                v-model:value="formData.clientId"
                placeholder="请选择委托人/客户"
                show-search
                allow-clear
                style="flex: 1"
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
                @change="handleClientChange"
              />
              <Button type="link" size="small" @click="handleCreateClient">
                <Plus class="size-4" />新建
              </Button>
            </div>
          </FormItem>

          <FormItem
            label="合同金额"
            name="totalAmount"
            :rules="[{ required: true, message: '请输入合同金额' }]"
          >
            <InputNumber
              v-model:value="formData.totalAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="请输入金额"
              addon-before="¥"
            />
          </FormItem>
          <FormItem label="金额大写">
            <Input
              :value="totalAmountChinese"
              disabled
              style="width: 100%; color: #ad6800; background: #fffbe6"
              placeholder="大写金额自动生成"
            />
          </FormItem>

          <Row
            :gutter="12"
            v-if="
              shouldShowField('signDate') || shouldShowField('paymentDeadline')
            "
          >
            <Col :span="12" v-if="shouldShowField('signDate')">
              <FormItem
                label="签约日期"
                name="signDate"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <DatePicker
                  v-model:value="formData.signDate"
                  style="width: 100%"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  placeholder="默认今天"
                />
              </FormItem>
            </Col>
            <Col :span="12" v-if="shouldShowField('paymentDeadline')">
              <FormItem
                label="付款期限"
                name="paymentDeadline"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <Input
                  v-model:value="formData.paymentDeadline"
                  placeholder="如：合同签订后3日内"
                />
              </FormItem>
            </Col>
          </Row>

          <Row :gutter="12">
            <Col :span="12">
              <FormItem
                label="合同类型"
                name="contractType"
                :label-col="{ span: 14 }"
                :wrapper-col="{ span: 10 }"
              >
                <Select
                  v-model:value="formData.contractType"
                  :options="contractTypeOptions"
                />
              </FormItem>
            </Col>
            <Col :span="12">
              <FormItem
                label="收费方式"
                name="feeType"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <Select
                  v-model:value="formData.feeType"
                  :options="feeTypeOptions"
                />
              </FormItem>
            </Col>
          </Row>

          <!-- 案件信息（诉讼类） -->
          <template
            v-if="
              visibleFields.litigation.length > 0 ||
              visibleFields.criminal.length > 0
            "
          >
            <Divider
              orientation="left"
              style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
            >
              {{
                visibleFields.criminal.length > 0
                  ? '刑事案件信息'
                  : '案件信息（诉讼类）'
              }}
            </Divider>

            <Row
              :gutter="12"
              v-if="
                shouldShowField('caseType') || shouldShowField('trialStage')
              "
            >
              <Col :span="12" v-if="shouldShowField('caseType')">
                <FormItem
                  label="案件类型"
                  name="caseType"
                  :label-col="{ span: 14 }"
                  :wrapper-col="{ span: 10 }"
                >
                  <Select
                    v-model:value="formData.caseType"
                    :options="caseTypeOptions"
                    placeholder="选择"
                    allow-clear
                  />
                </FormItem>
              </Col>
              <Col :span="12" v-if="shouldShowField('trialStage')">
                <FormItem
                  label="审理阶段"
                  name="trialStage"
                  :label-col="{ span: 10 }"
                  :wrapper-col="{ span: 14 }"
                >
                  <Select
                    v-model:value="formData.trialStage"
                    :options="trialStageOptions"
                    mode="multiple"
                    allow-clear
                    placeholder="可多选"
                    :max-tag-count="2"
                  />
                </FormItem>
              </Col>
            </Row>

            <FormItem
              v-if="shouldShowField('causeOfAction') && showCauseSelect"
              label="案由"
              name="causeOfAction"
            >
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

            <FormItem
              v-if="shouldShowField('opposingParty')"
              label="对方当事人（被告/被申请人）"
              name="opposingParty"
            >
              <Input
                v-model:value="formData.opposingParty"
                placeholder="被告/被申请人名称"
              />
            </FormItem>

            <template v-if="shouldShowField('claimAmount')">
              <FormItem label="标的金额" name="claimAmount">
                <InputNumber
                  v-model:value="formData.claimAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  placeholder="请输入金额"
                  addon-before="¥"
                />
              </FormItem>
              <FormItem label="标的金额大写">
                <Input
                  :value="claimAmountChinese"
                  disabled
                  style="width: 100%; color: #ad6800; background: #fffbe6"
                  placeholder="大写金额自动生成"
                />
              </FormItem>
            </template>

            <FormItem
              v-if="shouldShowField('jurisdictionCourt')"
              label="管辖法院"
              name="jurisdictionCourt"
            >
              <Input
                v-model:value="formData.jurisdictionCourt"
                placeholder="如：北京市朝阳区人民法院"
              />
            </FormItem>

            <!-- 案情摘要 - 用于审批表 -->
            <FormItem label="案情摘要" name="caseSummary">
              <Textarea
                v-model:value="formData.caseSummary"
                :rows="4"
                placeholder="请简要描述案件基本情况，该内容将显示在收案审批表的【案情摘要】栏目中"
              />
            </FormItem>

            <!-- 刑事案件信息 -->
            <template v-if="visibleFields.criminal.length > 0">
              <Row :gutter="12">
                <Col :span="12" v-if="shouldShowField('defendantName')">
                  <FormItem
                    label="被告人姓名"
                    name="defendantName"
                    :label-col="{ span: 10 }"
                    :wrapper-col="{ span: 14 }"
                  >
                    <Input
                      v-model:value="formData.defendantName"
                      placeholder="犯罪嫌疑人/被告人姓名"
                    />
                  </FormItem>
                </Col>
                <Col :span="12" v-if="shouldShowField('criminalCharge')">
                  <FormItem
                    label="涉嫌罪名"
                    name="criminalCharge"
                    :label-col="{ span: 10 }"
                    :wrapper-col="{ span: 14 }"
                  >
                    <Cascader
                      v-model:value="criminalChargeValue"
                      :options="criminalChargeOptions"
                      placeholder="请选择罪名"
                      change-on-select
                      :show-search="{
                        filter: (inputValue, path) =>
                          path.some((option) =>
                            option.label
                              .toLowerCase()
                              .includes(inputValue.toLowerCase()),
                          ),
                      }"
                      :display-render="
                        ({ labels }) => labels[labels.length - 1]
                      "
                      style="width: 100%"
                    />
                  </FormItem>
                </Col>
              </Row>

              <FormItem
                v-if="shouldShowField('defenseStage')"
                label="辩护阶段"
                name="defenseStage"
              >
                <Select
                  v-model:value="formData.defenseStage"
                  placeholder="可多选"
                  mode="multiple"
                  allow-clear
                  :max-tag-count="2"
                  :options="[
                    { label: 'A-侦查阶段律师', value: 'A-侦查阶段律师' },
                    {
                      label: 'B-审查起诉阶段辩护人',
                      value: 'B-审查起诉阶段辩护人',
                    },
                    { label: 'C-一审辩护人', value: 'C-一审辩护人' },
                    { label: 'D-二审辩护人', value: 'D-二审辩护人' },
                    {
                      label: 'E-死刑复核阶段辩护人',
                      value: 'E-死刑复核阶段辩护人',
                    },
                    { label: 'F-再审辩护人', value: 'F-再审辩护人' },
                  ]"
                />
              </FormItem>
            </template>
          </template>

          <!-- 律师信息 -->
          <template
            v-if="
              shouldShowField('lawyerNames') ||
              shouldShowField('assistantNames') ||
              shouldShowField('authorizationType')
            "
          >
            <Divider
              orientation="left"
              style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
            >
              律师信息
            </Divider>

            <Row :gutter="12">
              <Col :span="12" v-if="shouldShowField('lawyerNames')">
                <FormItem
                  label="主办律师"
                  name="lawyerNames"
                  :label-col="{ span: 10 }"
                  :wrapper-col="{ span: 14 }"
                >
                  <Input
                    v-model:value="formData.lawyerNames"
                    placeholder="律师姓名，多人用顿号分隔"
                  />
                </FormItem>
              </Col>
              <Col :span="12" v-if="shouldShowField('assistantNames')">
                <FormItem
                  label="律师助理"
                  name="assistantNames"
                  :label-col="{ span: 10 }"
                  :wrapper-col="{ span: 14 }"
                >
                  <Input
                    v-model:value="formData.assistantNames"
                    placeholder="助理姓名（选填）"
                  />
                </FormItem>
              </Col>
            </Row>

            <FormItem
              v-if="shouldShowField('authorizationType')"
              label="代理权限"
              name="authorizationType"
            >
              <Select
                v-model:value="formData.authorizationType"
                :options="[
                  { label: '一般代理', value: '一般代理' },
                  { label: '特别代理', value: '特别代理' },
                ]"
              />
            </FormItem>
          </template>

          <!-- 非诉项目计时收费 -->
          <template v-if="visibleFields.nonLitigation.length > 0">
            <Divider
              orientation="left"
              style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
            >
              计时收费标准
            </Divider>

            <Row :gutter="12">
              <Col :span="8" v-if="shouldShowField('partnerRate')">
                <FormItem label="合伙人" name="partnerRate">
                  <InputNumber
                    v-model:value="formData.partnerRate"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="元/小时"
                    addon-after="元/小时"
                  />
                </FormItem>
              </Col>
              <Col :span="8" v-if="shouldShowField('seniorRate')">
                <FormItem label="资深律师" name="seniorRate">
                  <InputNumber
                    v-model:value="formData.seniorRate"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="元/小时"
                    addon-after="元/小时"
                  />
                </FormItem>
              </Col>
              <Col :span="8" v-if="shouldShowField('assistantRate')">
                <FormItem label="律师助理" name="assistantRate">
                  <InputNumber
                    v-model:value="formData.assistantRate"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="元/小时"
                    addon-after="元/小时"
                  />
                </FormItem>
              </Col>
            </Row>
          </template>

          <!-- 常年法顾服务小时数 -->
          <FormItem
            v-if="shouldShowField('serviceHours')"
            label="服务小时数"
            name="serviceHours"
          >
            <InputNumber
              v-model:value="formData.serviceHours"
              :min="0"
              style="width: 100%"
              placeholder="每月服务小时数"
              addon-after="小时/月"
            />
          </FormItem>

          <!-- 合同期限 -->
          <template
            v-if="
              shouldShowField('effectiveDate') || shouldShowField('expiryDate')
            "
          >
            <Divider
              orientation="left"
              style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
            >
              合同期限
            </Divider>

            <Row :gutter="12">
              <Col :span="12" v-if="shouldShowField('effectiveDate')">
                <FormItem
                  label="生效日期"
                  name="effectiveDate"
                  :label-col="{ span: 14 }"
                  :wrapper-col="{ span: 10 }"
                >
                  <DatePicker
                    v-model:value="formData.effectiveDate"
                    style="width: 100%"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                  />
                </FormItem>
              </Col>
              <Col :span="12" v-if="shouldShowField('expiryDate')">
                <FormItem
                  label="到期日期"
                  name="expiryDate"
                  :label-col="{ span: 10 }"
                  :wrapper-col="{ span: 14 }"
                >
                  <DatePicker
                    v-model:value="formData.expiryDate"
                    style="width: 100%"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                  />
                </FormItem>
              </Col>
            </Row>
          </template>

          <!-- 其他信息 -->
          <Divider
            orientation="left"
            style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
          >
            其他信息
          </Divider>

          <Row :gutter="12">
            <Col :span="12">
              <FormItem
                label="利冲审查"
                name="conflictCheckStatus"
                :label-col="{ span: 14 }"
                :wrapper-col="{ span: 10 }"
              >
                <Select
                  v-model:value="formData.conflictCheckStatus"
                  :options="conflictCheckStatusOptions"
                />
              </FormItem>
            </Col>
            <Col :span="12" v-if="formData.feeType === 'CONTINGENCY'">
              <FormItem
                label="风险代理比例"
                name="riskRatio"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <InputNumber
                  v-model:value="formData.riskRatio"
                  :min="0"
                  :max="100"
                  :precision="1"
                  style="width: 100%"
                  addon-after="%"
                />
              </FormItem>
            </Col>
            <Col :span="12" v-else>
              <FormItem
                label="预支差旅费"
                name="advanceTravelFee"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <InputNumber
                  v-model:value="formData.advanceTravelFee"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  placeholder="0.00"
                />
              </FormItem>
            </Col>
          </Row>

          <FormItem
            v-if="shouldShowField('paymentTerms')"
            label="付款条款"
            name="paymentTerms"
          >
            <Textarea
              v-model:value="formData.paymentTerms"
              :rows="2"
              placeholder="约定付款方式、时间节点等"
            />
          </FormItem>

          <!-- 争议解决 -->
          <template
            v-if="
              shouldShowField('disputeResolution') ||
              shouldShowField('arbitrationCommittee')
            "
          >
            <Divider
              orientation="left"
              style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
            >
              争议解决
            </Divider>

            <Row :gutter="12">
              <Col :span="12" v-if="shouldShowField('disputeResolution')">
                <FormItem
                  label="争议解决方式"
                  name="disputeResolution"
                  :label-col="{ span: 12 }"
                  :wrapper-col="{ span: 12 }"
                >
                  <Select
                    v-model:value="formData.disputeResolution"
                    :options="[
                      { label: '1-受托人住所地法院管辖', value: '1' },
                      { label: '2-仲裁委员会仲裁', value: '2' },
                    ]"
                  />
                </FormItem>
              </Col>
              <Col
                :span="12"
                v-if="
                  shouldShowField('arbitrationCommittee') &&
                  formData.disputeResolution === '2'
                "
              >
                <FormItem
                  label="仲裁委员会"
                  name="arbitrationCommittee"
                  :label-col="{ span: 10 }"
                  :wrapper-col="{ span: 14 }"
                >
                  <Input
                    v-model:value="formData.arbitrationCommittee"
                    placeholder="如：北京"
                  />
                </FormItem>
              </Col>
            </Row>
          </template>

          <FormItem
            v-if="shouldShowField('specialTerms')"
            label="特别约定"
            name="specialTerms"
          >
            <Textarea
              v-model:value="formData.specialTerms"
              :rows="2"
              placeholder="双方的特别约定事项（选填）"
            />
          </FormItem>

          <FormItem label="备注" name="remark">
            <Textarea
              v-model:value="formData.remark"
              :rows="2"
              placeholder="其他需要说明的事项"
            />
          </FormItem>

          <!-- 提成方案 -->
          <Divider
            orientation="left"
            style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
          >
            提成分配方案
          </Divider>

          <FormItem label="选择方案">
            <Select
              v-model:value="selectedCommissionRuleId"
              placeholder="选择提成分配方案"
              allow-clear
              @change="handleCommissionRuleChange"
              :options="
                commissionRules.map((r) => ({
                  label: `${r.ruleName} (律所${r.firmRate}% 主办${r.leadLawyerRate}%)`,
                  value: r.id,
                }))
              "
            />
          </FormItem>

          <div
            v-if="selectedCommissionRule"
            style="
              padding: 12px;
              margin-bottom: 16px;
              background: #fafafa;
              border-radius: 4px;
            "
          >
            <div
              style="
                display: flex;
                justify-content: space-between;
                margin-bottom: 8px;
              "
            >
              <span style="font-size: 12px; color: #666">
                <Tag
                  v-if="selectedCommissionRule.allowModify"
                  color="green"
                  size="small"
                  >可调整</Tag
                >
                <Tag v-else size="small">固定</Tag>
                {{ selectedCommissionRule.description }}
              </span>
              <span style="font-size: 12px; color: #1890ff">
                合计:
                {{
                  (
                    (commissionFormData.firmRate || 0) +
                    (commissionFormData.leadLawyerRate || 0) +
                    (commissionFormData.assistLawyerRate || 0) +
                    (commissionFormData.supportStaffRate || 0)
                  ).toFixed(2)
                }}%
              </span>
            </div>
            <div
              style="
                padding: 4px 0;
                margin-bottom: 8px;
                font-size: 11px;
                color: #999;
              "
            >
              💡
              提示：此比例将自动应用到下方参与人（根据角色匹配），可在参与人部分手动调整
            </div>
            <Row :gutter="8">
              <Col :span="6">
                <div style="margin-bottom: 4px; font-size: 11px; color: #999">
                  律所
                </div>
                <InputNumber
                  v-model:value="commissionFormData.firmRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :disabled="!selectedCommissionRule.allowModify"
                  size="small"
                  addon-after="%"
                  style="width: 100%"
                />
              </Col>
              <Col :span="6">
                <div style="margin-bottom: 4px; font-size: 11px; color: #999">
                  主办律师
                </div>
                <InputNumber
                  v-model:value="commissionFormData.leadLawyerRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :disabled="!selectedCommissionRule.allowModify"
                  size="small"
                  addon-after="%"
                  style="width: 100%"
                />
              </Col>
              <Col :span="6">
                <div style="margin-bottom: 4px; font-size: 11px; color: #999">
                  协办律师
                </div>
                <InputNumber
                  v-model:value="commissionFormData.assistLawyerRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :disabled="!selectedCommissionRule.allowModify"
                  size="small"
                  addon-after="%"
                  style="width: 100%"
                />
              </Col>
              <Col :span="6">
                <div style="margin-bottom: 4px; font-size: 11px; color: #999">
                  辅助人员
                </div>
                <InputNumber
                  v-model:value="commissionFormData.supportStaffRate"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :disabled="!selectedCommissionRule.allowModify"
                  size="small"
                  addon-after="%"
                  style="width: 100%"
                />
              </Col>
            </Row>
          </div>

          <!-- 参与人 -->
          <Divider
            orientation="left"
            style="margin: 8px 0 16px; font-size: 13px; color: #1890ff"
          >
            参与人
          </Divider>

          <div style="margin-bottom: 16px">
            <div
              style="
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 8px;
              "
            >
              <span style="font-size: 12px; color: #666"
                >选择合同参与人（用于提成分配和"我的收款"显示）</span
              >
              <Button
                type="link"
                size="small"
                @click="handleAddContractParticipant"
              >
                <Plus class="size-4" />添加参与人
              </Button>
            </div>

            <div
              v-if="contractParticipants.length === 0"
              style="
                padding: 12px;
                font-size: 12px;
                color: #999;
                text-align: center;
                background: #fafafa;
                border-radius: 4px;
              "
            >
              未添加参与人，将自动添加签约人或当前用户为参与人
            </div>

            <div
              v-for="(participant, index) in contractParticipants"
              :key="index"
              style="
                padding: 12px;
                margin-bottom: 8px;
                background: #fafafa;
                border-radius: 4px;
              "
            >
              <Row :gutter="8" align="middle">
                <Col :span="8">
                  <UserTreeSelect
                    v-model:value="participant.userId"
                    placeholder="选择人员"
                    style="width: 100%"
                  />
                </Col>
                <Col :span="6">
                  <Select
                    v-model:value="participant.role"
                    style="width: 100%"
                    @change="
                      (value: any) =>
                        handleParticipantRoleChange(index, String(value))
                    "
                    :options="[
                      { label: '主办律师', value: 'LEAD' },
                      { label: '协办律师', value: 'CO_COUNSEL' },
                      { label: '案源人', value: 'ORIGINATOR' },
                      { label: '律师助理', value: 'PARALEGAL' },
                    ]"
                  />
                </Col>
                <Col :span="6">
                  <InputNumber
                    v-model:value="participant.commissionRate"
                    :min="0"
                    :max="100"
                    :precision="2"
                    style="width: 100%"
                    placeholder="提成比例"
                    addon-after="%"
                  />
                </Col>
                <Col :span="4">
                  <Button
                    type="link"
                    danger
                    size="small"
                    @click="contractParticipants.splice(index, 1)"
                  >
                    删除
                  </Button>
                </Col>
              </Row>
            </div>
          </div>
        </Form>
      </div>
    </Modal>

    <!-- 合同详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="合同详情"
      :width="isMobile ? '100%' : '1000px'"
      :centered="isMobile"
      :footer="null"
    >
      <!-- 审批通过后显示打印按钮 -->
      <div
        v-if="currentContract && currentContract.status === 'ACTIVE'"
        style="
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 12px;
          margin-bottom: 16px;
          background: #f6ffed;
          border: 1px solid #b7eb8f;
          border-radius: 4px;
        "
      >
        <span style="color: #52c41a"
          >✓ 合同已审批通过，可打印正式合同文本供双方签字</span
        >
        <Space>
          <Button @click="handlePreviewApprovalForm">📋 预览审批表</Button>
          <!-- 只有合同创建人或签约律师可以编辑审批表 -->
          <Button
            v-if="
              currentContract.createdBy === currentUserId ||
              currentContract.signerId === currentUserId
            "
            @click="handleEditApprovalForm"
          >
            ✏️ 编辑审批表
          </Button>
          <Button type="primary" @click="handlePrintContract">
            🖨️ 打印合同
          </Button>
        </Space>
      </div>
      <Tabs v-model:active-key="activeTab">
        <TabPane key="info" tab="基本信息">
          <div v-if="currentContract" style="padding: 16px">
            <Row :gutter="[16, 12]">
              <Col :span="8">
                <strong>合同编号：</strong>{{ currentContract.contractNo }}
              </Col>
              <Col :span="8">
                <strong>合同名称：</strong>{{ currentContract.name }}
              </Col>
              <Col :span="8">
                <strong>状态：</strong
                ><Tag :color="getStatusColor(currentContract.status)">
                  {{ currentContract.statusName }}
                </Tag>
              </Col>
              <Col :span="8">
                <strong>客户：</strong>{{ currentContract.clientName }}
              </Col>
              <Col :span="8">
                <strong>合同类型：</strong
                >{{ currentContract.contractTypeName }}
              </Col>
              <Col :span="8">
                <strong>收费方式：</strong>{{ currentContract.feeTypeName }}
              </Col>
              <Col :span="8">
                <strong>合同金额：</strong
                >{{ formatMoney(currentContract.totalAmount) }}
              </Col>
              <Col :span="8">
                <strong>已收金额：</strong
                >{{ formatMoney(currentContract.paidAmount) }}
              </Col>
              <Col :span="8">
                <strong>待收金额：</strong
                >{{ formatMoney(currentContract.unpaidAmount) }}
              </Col>
              <Col :span="8" v-if="currentContract.trialStage">
                <strong>审理阶段：</strong>{{ currentContract.trialStageName }}
              </Col>
              <Col :span="8" v-if="currentContract.claimAmount">
                <strong>标的金额：</strong
                >{{ formatMoney(currentContract.claimAmount) }}
              </Col>
              <Col :span="8" v-if="currentContract.jurisdictionCourt">
                <strong>管辖法院：</strong
                >{{ currentContract.jurisdictionCourt }}
              </Col>
              <Col :span="8" v-if="currentContract.opposingParty">
                <strong>对方当事人：</strong>{{ currentContract.opposingParty }}
              </Col>
              <Col :span="8">
                <strong>利冲审查：</strong
                >{{ currentContract.conflictCheckStatusName }}
              </Col>
              <Col :span="8" v-if="currentContract.riskRatio">
                <strong>风险代理比例：</strong>{{ currentContract.riskRatio }}%
              </Col>
              <Col :span="8">
                <strong>签约日期：</strong>{{ currentContract.signDate || '-' }}
              </Col>
              <Col :span="8">
                <strong>生效日期：</strong
                >{{ currentContract.effectiveDate || '-' }}
              </Col>
              <Col :span="8">
                <strong>到期日期：</strong
                >{{ currentContract.expiryDate || '-' }}
              </Col>
            </Row>
            <Divider />
            <div v-if="currentContract.caseSummary" style="margin-bottom: 12px">
              <strong>案情摘要：</strong>
              <div
                style="
                  padding: 12px;
                  margin-top: 8px;
                  white-space: pre-wrap;
                  background: #fafafa;
                  border-radius: 4px;
                "
              >
                {{ currentContract.caseSummary }}
              </div>
            </div>
            <div v-if="currentContract.remark">
              <strong>备注：</strong>{{ currentContract.remark }}
            </div>
          </div>
        </TabPane>
        <TabPane key="schedule" tab="付款计划">
          <div
            style="
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 16px;
            "
          >
            <div>
              <span
                >付款计划总额：<strong>{{
                  formatMoney(totalScheduleAmount)
                }}</strong></span
              >
              <span style="margin-left: 16px" v-if="currentContract">
                合同金额：<strong>{{
                  formatMoney(currentContract.totalAmount)
                }}</strong>
                <Tag
                  v-if="totalScheduleAmount === currentContract.totalAmount"
                  color="green"
                  style="margin-left: 8px"
                  >已匹配</Tag
                >
                <Tag v-else color="orange" style="margin-left: 8px"
                  >差额:
                  {{
                    formatMoney(
                      currentContract.totalAmount - totalScheduleAmount,
                    )
                  }}</Tag
                >
              </span>
            </div>
            <Button type="primary" size="small" @click="handleAddSchedule">
              添加付款计划
            </Button>
          </div>
          <Table
            :columns="scheduleColumns"
            :data-source="paymentSchedules"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">
                {{ formatMoney((record as ContractPaymentScheduleDTO).amount) }}
              </template>
              <template v-if="column.key === 'statusName'">
                <Tag>
                  {{ (record as ContractPaymentScheduleDTO).statusName }}
                </Tag>
              </template>
              <template v-if="column.key === 'action'">
                <Space>
                  <a
                    @click="
                      handleEditSchedule(record as ContractPaymentScheduleDTO)
                    "
                    >编辑</a
                  >
                  <Popconfirm
                    title="确定删除？"
                    @confirm="
                      handleDeleteSchedule(record as ContractPaymentScheduleDTO)
                    "
                  >
                    <a style="color: red">删除</a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </TabPane>
        <TabPane key="participant" tab="参与人">
          <Alert
            message="💡 参与人说明"
            description="合同参与人（主办律师、协办律师）会在创建项目时自动复制为项目团队成员，并作为授权委托书的受托人信息。审批通过后仍可添加参与人。"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <div
            style="
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 16px;
            "
          >
            <div>
              <span
                >提成比例总和：<strong>{{ totalCommissionRate }}%</strong></span
              >
              <Tag
                v-if="totalCommissionRate <= 100"
                color="green"
                style="margin-left: 8px"
              >
                正常
              </Tag>
              <Tag v-else color="red" style="margin-left: 8px">超过100%</Tag>
            </div>
            <Button type="primary" size="small" @click="handleAddParticipant">
              添加参与人
            </Button>
          </div>
          <Table
            :columns="participantColumns"
            :data-source="participants"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'roleName'">
                <Tag
                  :color="
                    (record as ContractParticipantDTO).role === 'LEAD'
                      ? 'blue'
                      : 'default'
                  "
                >
                  {{ (record as ContractParticipantDTO).roleName }}
                </Tag>
              </template>
              <template v-if="column.key === 'commissionRate'">
                <span
                  >{{
                    (record as ContractParticipantDTO).commissionRate || 0
                  }}%</span
                >
                <!-- 显示与方案比例的对比 -->
                <span
                  v-if="currentContract"
                  style="margin-left: 8px; font-size: 11px; color: #999"
                >
                  <template
                    v-if="
                      (record as ContractParticipantDTO).role === 'LEAD' &&
                      currentContract.leadLawyerRate
                    "
                  >
                    (方案: {{ currentContract.leadLawyerRate }}%)
                  </template>
                  <template
                    v-else-if="
                      (record as ContractParticipantDTO).role ===
                        'CO_COUNSEL' && currentContract.assistLawyerRate
                    "
                  >
                    (方案: {{ currentContract.assistLawyerRate }}%)
                  </template>
                  <template
                    v-else-if="
                      (record as ContractParticipantDTO).role === 'PARALEGAL' &&
                      currentContract.supportStaffRate
                    "
                  >
                    (方案: {{ currentContract.supportStaffRate }}%)
                  </template>
                </span>
              </template>
              <template v-if="column.key === 'action'">
                <Space>
                  <a
                    @click="
                      handleEditParticipant(record as ContractParticipantDTO)
                    "
                    >编辑</a
                  >
                  <Popconfirm
                    title="确定删除？"
                    @confirm="
                      handleDeleteParticipant(record as ContractParticipantDTO)
                    "
                  >
                    <a style="color: red">删除</a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </TabPane>
      </Tabs>
    </Modal>

    <!-- 付款计划弹窗 -->
    <Modal
      v-model:open="scheduleModalVisible"
      :title="scheduleFormData.id ? '编辑付款计划' : '添加付款计划'"
      width="500px"
      @ok="handleSaveSchedule"
    >
      <Form
        ref="scheduleFormRef"
        :model="scheduleFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem
          label="阶段名称"
          name="phaseName"
          :rules="[{ required: true, message: '请输入阶段名称' }]"
        >
          <Input
            v-model:value="scheduleFormData.phaseName"
            placeholder="如：首付款、尾款"
          />
        </FormItem>
        <FormItem label="金额" name="amount">
          <InputNumber
            v-model:value="scheduleFormData.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
            prefix="¥"
          />
        </FormItem>
        <FormItem label="比例(%)" name="percentage">
          <InputNumber
            v-model:value="scheduleFormData.percentage"
            :min="0"
            :max="100"
            :precision="1"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="计划日期" name="plannedDate">
          <DatePicker
            v-model:value="scheduleFormData.plannedDate"
            style="width: 100%"
            format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea v-model:value="scheduleFormData.remark" :rows="2" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 参与人弹窗 -->
    <Modal
      v-model:open="participantModalVisible"
      :title="participantFormData.id ? '编辑参与人' : '添加参与人'"
      width="500px"
      @ok="handleSaveParticipant"
    >
      <Form
        ref="participantFormRef"
        :model="participantFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem
          label="人员"
          name="userId"
          :rules="[{ required: true, message: '请选择人员' }]"
        >
          <UserTreeSelect
            v-model:value="participantFormData.userId"
            placeholder="选择人员（按部门筛选）"
            :disabled="!!participantFormData.id"
          />
        </FormItem>
        <FormItem label="角色" name="role" :rules="[{ required: true }]">
          <Select
            v-model:value="participantFormData.role"
            :options="participantRoleOptions"
          />
        </FormItem>
        <FormItem label="提成比例(%)" name="commissionRate">
          <InputNumber
            v-model:value="participantFormData.commissionRate"
            :min="0"
            :max="100"
            :precision="1"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea v-model:value="participantFormData.remark" :rows="2" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 合同变更申请弹窗 -->
    <Modal
      v-model:open="changeModalVisible"
      title="合同变更申请"
      :width="isMobile ? '100%' : '800px'"
      :centered="isMobile"
      @ok="handleSubmitChange"
    >
      <Form
        ref="changeFormRef"
        :model="changeFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem
          label="变更原因"
          name="changeReason"
          :rules="[{ required: true, message: '请输入变更原因' }]"
        >
          <Input
            v-model:value="changeFormData.changeReason"
            placeholder="请输入变更原因"
          />
        </FormItem>
        <FormItem label="变更说明" name="changeDescription">
          <Textarea
            v-model:value="changeFormData.changeDescription"
            :rows="2"
            placeholder="请详细说明变更内容"
          />
        </FormItem>
        <Divider>变更内容</Divider>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="合同名称" name="name">
              <Input v-model:value="changeFormData.name" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="合同金额" name="totalAmount">
              <InputNumber
                v-model:value="changeFormData.totalAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
                prefix="¥"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="收费方式" name="feeType">
              <Select
                v-model:value="changeFormData.feeType"
                :options="feeTypeOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="到期日期" name="expiryDate">
              <DatePicker
                v-model:value="changeFormData.expiryDate"
                style="width: 100%"
                format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
        </Row>
        <FormItem
          label="付款条款"
          name="paymentTerms"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 21 }"
        >
          <Textarea v-model:value="changeFormData.paymentTerms" :rows="2" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 模板预览弹窗 -->
    <Modal
      v-model:open="templatePreviewVisible"
      title="模板内容预览"
      width="800px"
      :footer="null"
    >
      <div
        style="
          max-height: 500px;
          padding: 16px;
          overflow-y: auto;
          white-space: pre-wrap;
          background: #f5f5f5;
          border-radius: 4px;
        "
      >
        {{ templatePreviewContent || '暂无内容' }}
      </div>
    </Modal>

    <!-- 打印选项弹窗 -->
    <Modal
      v-model:open="printModalVisible"
      title="🖨️ 打印设置"
      width="500px"
      @ok="executePrint"
      ok-text="开始打印"
      cancel-text="取消"
    >
      <div style="padding: 16px 0">
        <Alert
          message="请选择要打印的内容"
          description="合同审批通过后，您可以打印合同文本供双方签字，同时打印收案审批表用于律所存档。"
          type="info"
          show-icon
          style="margin-bottom: 20px"
        />

        <div style="display: flex; flex-direction: column; gap: 16px">
          <Checkbox v-model:checked="printOptions.printContract">
            <span style="font-size: 15px; font-weight: 500">📄 合同文本</span>
            <div style="margin-top: 4px; font-size: 13px; color: #666">
              打印完整合同内容，包含双方签字盖章区域
            </div>
          </Checkbox>

          <Checkbox v-model:checked="printOptions.printApprovalForm">
            <span style="font-size: 15px; font-weight: 500">📋 收案审批表</span>
            <div style="margin-top: 4px; font-size: 13px; color: #666">
              打印律所内部收案审批表，包含委托人信息、案情摘要、审批意见等
            </div>
          </Checkbox>
        </div>

        <div
          v-if="currentPrintData"
          style="
            padding: 12px;
            margin-top: 20px;
            background: #f5f5f5;
            border-radius: 8px;
          "
        >
          <div style="margin-bottom: 8px; font-weight: 500">📌 合同信息</div>
          <div style="font-size: 13px; color: #666">
            <div>合同编号：{{ currentPrintData.contractNo }}</div>
            <div>合同名称：{{ currentPrintData.name }}</div>
            <div>委托人：{{ currentPrintData.clientName }}</div>
            <div>
              合同金额：¥{{ currentPrintData.totalAmount?.toLocaleString() }}
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <!-- 审批表编辑弹窗 -->
    <Modal
      v-model:open="approvalFormModalVisible"
      title="✏️ 编辑收案审批表"
      width="700px"
      @ok="handleSaveApprovalForm"
      :confirm-loading="savingApprovalForm"
      ok-text="保存"
      cancel-text="取消"
    >
      <div style="padding: 16px 0">
        <Alert
          message="编辑案情摘要"
          description="案情摘要将显示在收案审批表的【案情摘要】栏目中，请简要描述案件基本情况。该内容独立于合同文本。"
          type="info"
          show-icon
          style="margin-bottom: 20px"
        />

        <Form layout="vertical">
          <FormItem label="案情摘要（附接待笔录）">
            <Textarea
              v-model:value="approvalFormData.caseSummary"
              :rows="8"
              placeholder="请简要描述案件基本情况，如：&#10;1. 委托人基本情况&#10;2. 案件起因经过&#10;3. 诉讼请求或法律需求&#10;4. 证据材料概述"
              show-count
              :maxlength="2000"
            />
          </FormItem>
        </Form>

        <div
          style="
            padding: 12px;
            margin-top: 16px;
            background: #fffbe6;
            border: 1px solid #ffe58f;
            border-radius: 4px;
          "
        >
          <div style="font-weight: 500; color: #ad6800">💡 提示</div>
          <div style="margin-top: 8px; font-size: 13px; color: #666">
            案情摘要应包含以下要点：委托人身份信息、案件基本事实、争议焦点、代理目标等。
            保存后可在合同详情中查看，也可通过预览审批表查看最终效果。
          </div>
        </div>
      </div>
    </Modal>

    <!-- 选择审批人弹窗 -->
    <Modal
      v-model:open="approverModalVisible"
      title="选择审批人"
      width="500px"
      @ok="handleConfirmSubmit"
      ok-text="提交审批"
      cancel-text="取消"
    >
      <div style="margin-bottom: 16px">
        <p>
          请选择审批人，合同
          <strong>{{ pendingSubmitContract?.name }}</strong>
          将提交给选中的审批人进行审批。
        </p>
      </div>

      <template v-if="approverOptions.length > 0">
        <Select
          v-model:value="selectedApproverId"
          placeholder="请选择审批人"
          style="width: 100%"
          size="large"
          show-search
          :filter-option="
            (input, option) =>
              (option?.label || '').toLowerCase().includes(input.toLowerCase())
          "
        >
          <template v-for="approver in approverOptions" :key="approver.id">
            <Select.Option :value="approver.id" :label="approver.realName">
              <div
                style="
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                "
              >
                <span>{{ approver.realName }}</span>
                <span style="font-size: 12px; color: #999"
                  >{{ approver.position }} · {{ approver.departmentName }}</span
                >
              </div>
            </Select.Option>
          </template>
        </Select>
      </template>

      <template v-else>
        <div style="padding: 20px; color: #999; text-align: center">
          <p>暂无可选审批人</p>
          <p style="font-size: 12px">
            系统将自动分配审批人，或请联系管理员配置合伙人/主任角色
          </p>
        </div>
      </template>
    </Modal>
  </Page>
</template>

<style scoped>
/* 合同预览内容样式 */
.contract-preview-content {
  flex: 1;
  max-height: 65vh;
  padding: 32px 40px;
  overflow-y: auto;
  font-family: SimSun, '宋体', serif;
  font-size: 14pt;
  line-height: 2;
  color: #333;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgb(0 0 0 / 8%);
}

/* 模拟A4纸张效果 */
.contract-preview-content::before {
  display: block;
  height: 0;
  content: '';
}

/* 富文本元素样式 */
:deep(.contract-preview-content h1),
:deep(.contract-preview-content h2),
:deep(.contract-preview-content h3) {
  margin: 16px 0;
  font-weight: bold;
  color: #000;
  text-align: center;
}

:deep(.contract-preview-content h1) {
  font-size: 22px;
  letter-spacing: 4px;
}

:deep(.contract-preview-content h2) {
  font-size: 18px;
  letter-spacing: 2px;
}

:deep(.contract-preview-content h3) {
  font-size: 16px;
}

:deep(.contract-preview-content p) {
  margin: 8px 0;

  /* 不设置默认 text-align，保留编辑器的内联样式设置 */
}

:deep(.contract-preview-content table) {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;
}

:deep(.contract-preview-content table td),
:deep(.contract-preview-content table th) {
  padding: 8px 12px;
  text-align: left;
  border: 1px solid #333;
}

:deep(.contract-preview-content table th) {
  font-weight: bold;
  background: #f5f5f5;
}

:deep(.contract-preview-content ul),
:deep(.contract-preview-content ol) {
  padding-left: 2em;
  margin: 8px 0;
}

:deep(.contract-preview-content li) {
  margin: 4px 0;
}

:deep(.contract-preview-content strong),
:deep(.contract-preview-content b) {
  font-weight: bold;
}

:deep(.contract-preview-content em),
:deep(.contract-preview-content i) {
  font-style: italic;
}

:deep(.contract-preview-content u) {
  text-decoration: underline;
}

/* 变量占位符样式 */
:deep(.contract-preview-content) span[style*='background'] {
  padding: 2px 4px;
  border-radius: 2px;
}
</style>
