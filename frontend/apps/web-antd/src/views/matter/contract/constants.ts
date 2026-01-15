/**
 * 合同类型选项
 */
export const CONTRACT_TYPE_OPTIONS = [
  { label: '委托代理合同', value: 'AGENCY' },
  { label: '法律顾问合同', value: 'CONSULTING' },
  { label: '专项法律服务合同', value: 'SPECIAL' },
];

/**
 * 收费方式选项
 */
export const FEE_TYPE_OPTIONS = [
  { label: '固定收费', value: 'FIXED' },
  { label: '按标的额比例', value: 'PERCENTAGE' },
  { label: '计时收费', value: 'HOURLY' },
  { label: '风险代理', value: 'CONTINGENCY' },
  { label: '协商收费', value: 'NEGOTIATED' },
];

/**
 * 审理阶段选项
 */
export const TRIAL_STAGE_OPTIONS = [
  { label: '一审', value: 'FIRST_INSTANCE' },
  { label: '二审', value: 'SECOND_INSTANCE' },
  { label: '再审', value: 'RETRIAL' },
  { label: '执行', value: 'EXECUTION' },
];

/**
 * 参与人角色选项
 */
export const PARTICIPANT_ROLE_OPTIONS = [
  { label: '主办律师', value: 'PRIMARY_LAWYER' },
  { label: '协办律师', value: 'ASSISTANT_LAWYER' },
  { label: '律师助理', value: 'PARALEGAL' },
  { label: '实习律师', value: 'TRAINEE' },
];

/**
 * 冲突检查状态选项
 */
export const CONFLICT_CHECK_STATUS_OPTIONS = [
  { label: '未检查', value: 'NOT_CHECKED' },
  { label: '无冲突', value: 'NO_CONFLICT' },
  { label: '有冲突', value: 'HAS_CONFLICT' },
  { label: '需人工复核', value: 'MANUAL_REVIEW' },
];

/**
 * 合同状态选项
 */
export const STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING' },
  { label: '已生效', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已到期', value: 'EXPIRED' },
  { label: '已终止', value: 'TERMINATED' },
];

/**
 * 案件类型与字典编码映射
 */
export const CASE_TYPE_TO_DICT_CODE: Record<string, string> = {
  CIVIL: 'civil_trial_stage',
  ADMIN: 'admin_trial_stage',
  CRIMINAL: 'criminal_trial_stage',
};

/**
 * 字段配置类型
 */
export interface FieldConfig {
  label: string;
  required?: boolean;
  defaultValue?: any;
  dependsOn?: string;
  showFor?: string[];
}

/**
 * 默认字段配置
 */
export const DEFAULT_FIELD_CONFIG: Record<string, FieldConfig> = {
  // 基本信息
  clientId: { label: '委托人', required: true },
  totalAmount: { label: '合同金额', required: true },
  claimAmount: { label: '标的额', required: false },
  signDate: { label: '签约日期', required: false },
  paymentDeadline: { label: '付款期限', required: false },
  contractType: { label: '合同类型', required: false },
  feeType: { label: '收费方式', required: false },

  // 案件信息
  caseType: { label: '案件类型', required: false },
  causeOfAction: { label: '案由/罪名', required: false },
  trialStage: { label: '审理阶段', required: false },
  caseNo: { label: '案号', required: false },
  opposingParty: { label: '对方当事人', required: false },
  courtName: { label: '审理法院', required: false },

  // 合同条款
  paymentTerms: { label: '付款条款', required: false },
  liabilityClause: { label: '违约责任', required: false },
  serviceScope: { label: '服务范围', required: false },
  expiryDate: { label: '合同到期日', required: false },
  autoRenew: { label: '自动续签', required: false },
};

/**
 * 合同类型与显示字段映射
 */
export const CONTRACT_TYPE_FIELDS: Record<string, string[]> = {
  FIXED: ['clientId', 'totalAmount', 'signDate', 'paymentTerms', 'expiryDate'],
  PERCENTAGE: [
    'clientId',
    'totalAmount',
    'claimAmount',
    'signDate',
    'paymentTerms',
    'caseType',
    'causeOfAction',
  ],
  HOURLY: ['clientId', 'totalAmount', 'signDate', 'paymentTerms', 'serviceScope'],
  CONTINGENCY: [
    'clientId',
    'claimAmount',
    'signDate',
    'paymentTerms',
    'caseType',
    'causeOfAction',
  ],
  NEGOTIATED: ['clientId', 'totalAmount', 'signDate', 'paymentTerms'],
};
