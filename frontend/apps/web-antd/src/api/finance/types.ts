/**
 * 财务管理模块类型定义
 */

// ========== 合同管理 ==========
import type { ApprovalDTO } from '#/api/workbench';

export interface ContractDTO {
  id: number;
  contractNo: string;
  name: string;
  clientId: number;
  clientName?: string;
  matterId?: number;
  matterName?: string;
  contractType: string;
  contractTypeName?: string;
  feeType: string;
  feeTypeName?: string;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount?: number;
  currency: string;
  signDate?: string;
  effectiveDate?: string;
  expiryDate?: string;
  status: string;
  statusName?: string;
  signerId?: number;
  signerName?: string;
  departmentId?: number;
  createdBy?: number; // 创建人ID，用于判断操作权限
  departmentName?: string;
  fileUrl?: string;
  paymentTerms?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
  /** 关联的审批单列表（最新的在前） */
  approvals?: ApprovalDTO[];
  /** 当前待审批的审批单（如果有） */
  currentApproval?: ApprovalDTO;
  // 扩展字段
  /** 案件类型 */
  caseType?: string;
  caseTypeName?: string;
  /** 案由代码 */
  causeOfAction?: string;
  causeOfActionName?: string;
  /** 审理阶段 */
  trialStage?: string | string[]; // 支持单选或多选
  trialStageName?: string;
  /** 标的金额 */
  claimAmount?: number;
  /** 管辖法院 */
  jurisdictionCourt?: string;
  /** 对方当事人 */
  opposingParty?: string;
  /** 利冲审查状态 */
  conflictCheckStatus?: string;
  conflictCheckStatusName?: string;
  /** 归档状态 */
  archiveStatus?: string;
  archiveStatusName?: string;
  /** 预支差旅费 */
  advanceTravelFee?: number;
  /** 风险代理比例 */
  riskRatio?: number;
  /** 用印记录 */
  sealRecord?: string;
  /** 付款计划列表 */
  paymentSchedules?: ContractPaymentScheduleDTO[];
  /** 参与人列表 */
  participants?: ContractParticipantDTO[];
  // 提成分配方案
  /** 提成规则ID */
  commissionRuleId?: number;
  /** 律所比例(%) */
  firmRate?: number;
  /** 主办律师比例(%) */
  leadLawyerRate?: number;
  /** 协办律师比例(%) */
  assistLawyerRate?: number;
  /** 辅助人员比例(%) */
  supportStaffRate?: number;
  /** 案源人比例(%) */
  originatorRate?: number;
  /** 案情摘要（用于审批表） */
  caseSummary?: string;
}

/** 合同付款计划 */
export interface ContractPaymentScheduleDTO {
  id: number;
  contractId: number;
  phaseName: string;
  amount?: number;
  percentage?: number;
  plannedDate?: string;
  actualDate?: string;
  status: string;
  statusName?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 合同参与人 */
export interface ContractParticipantDTO {
  id: number;
  contractId: number;
  userId: number;
  userName?: string;
  role: string;
  roleName?: string;
  commissionRate?: number;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ContractQuery {
  contractNo?: string;
  name?: string;
  clientId?: number;
  matterId?: number;
  contractType?: string;
  status?: string;
  signerId?: number;
  feeType?: string;
  departmentId?: number;
  signDateFrom?: string;
  signDateTo?: string;
  effectiveDateFrom?: string;
  effectiveDateTo?: string;
  expiryDateFrom?: string;
  expiryDateTo?: string;
  amountMin?: number;
  amountMax?: number;
  claimAmountMin?: number;
  claimAmountMax?: number;
  trialStage?: string;
  conflictCheckStatus?: string;
  archiveStatus?: string;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateContractCommand {
  name: string;
  clientId: number;
  matterId?: number;
  contractType: string;
  feeType: string;
  totalAmount: number;
  currency?: string;
  signDate?: string;
  effectiveDate?: string;
  expiryDate?: string;
  signerId?: number;
  departmentId?: number;
  fileUrl?: string;
  paymentTerms?: string;
  remark?: string;
  // 扩展字段
  caseType?: string;
  causeOfAction?: string;
  trialStage?: string | string[]; // 支持单选或多选
  claimAmount?: number;
  jurisdictionCourt?: string;
  opposingParty?: string;
  conflictCheckStatus?: string;
  advanceTravelFee?: number;
  riskRatio?: number;
  // 提成分配方案
  commissionRuleId?: number;
  firmRate?: number;
  leadLawyerRate?: number;
  assistLawyerRate?: number;
  supportStaffRate?: number;
  originatorRate?: number;
  /** 案情摘要（用于审批表） */
  caseSummary?: string;
}

export interface UpdateContractCommand extends Partial<CreateContractCommand> {
  id: number;
  archiveStatus?: string;
  sealRecord?: string;
}

/** 创建付款计划命令 */
export interface CreatePaymentScheduleCommand {
  contractId?: number;
  phaseName: string;
  amount?: number;
  percentage?: number;
  plannedDate?: string;
  remark?: string;
}

/** 更新付款计划命令 */
export interface UpdatePaymentScheduleCommand {
  id?: number;
  phaseName?: string;
  amount?: number;
  percentage?: number;
  plannedDate?: string;
  actualDate?: string;
  status?: string;
  remark?: string;
}

/** 创建参与人命令 */
export interface CreateParticipantCommand {
  contractId?: number;
  userId: number;
  role: string;
  commissionRate?: number;
  remark?: string;
}

/** 更新参与人命令 */
export interface UpdateParticipantCommand {
  id?: number;
  role?: string;
  commissionRate?: number;
  remark?: string;
}

/** 合同统计 */
export interface ContractStatistics {
  totalCount: number;
  draftCount: number;
  pendingCount: number;
  activeCount: number;
  completedCount: number;
  terminatedCount: number;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  fixedFeeCount: number;
  hourlyFeeCount: number;
  contingencyFeeCount: number;
  mixedFeeCount: number;
}

// ========== 收费管理 ==========
export interface FeeDTO {
  id: number;
  feeNo: string;
  contractId?: number;
  contractNo?: string;
  matterId?: number;
  matterName?: string;
  clientId: number;
  clientName?: string;
  feeType: string;
  feeTypeName?: string;
  feeName: string;
  amount: number;
  paidAmount: number;
  currency: string;
  plannedDate?: string;
  actualDate?: string;
  status: string;
  statusName?: string;
  responsibleId?: number;
  responsibleName?: string;
  remark?: string;
  createdAt?: string;
  /** 收款记录列表（仅在获取详情时填充） */
  payments?: PaymentDTO[];
}

export interface FeeQuery {
  feeNo?: string;
  contractId?: number;
  matterId?: number;
  clientId?: number;
  feeType?: string;
  status?: string;
  plannedDateFrom?: string;
  plannedDateTo?: string;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateFeeCommand {
  contractId?: number;
  matterId?: number;
  clientId: number;
  feeType: string;
  feeName: string;
  amount: number;
  currency?: string;
  plannedDate?: string;
  responsibleId?: number;
  remark?: string;
}

// ========== 收款管理 ==========
export interface PaymentDTO {
  id: number;
  paymentNo: string;
  feeId?: number;
  feeNo?: string;
  contractId?: number;
  contractNo?: string;
  matterId?: number;
  matterName?: string;
  clientId: number;
  clientName?: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  paymentMethodName?: string;
  paymentDate: string;
  bankAccount?: string;
  transactionNo?: string;
  status: string;
  statusName?: string;
  confirmerId?: number;
  confirmerName?: string;
  remark?: string;
  createdAt?: string;
}

export interface PaymentQuery {
  paymentNo?: string;
  feeId?: number;
  contractId?: number;
  matterId?: number;
  clientId?: number;
  paymentMethod?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreatePaymentCommand {
  feeId?: number;
  contractId?: number;
  matterId?: number;
  clientId: number;
  amount: number;
  currency?: string;
  paymentMethod: string;
  paymentDate: string;
  bankAccount?: string;
  transactionNo?: string;
  remark?: string;
}

// ========== 发票管理 ==========
export interface InvoiceDTO {
  id: number;
  invoiceNo?: string;
  feeId?: number;
  feeNo?: string;
  contractId?: number;
  contractNo?: string;
  clientId: number;
  clientName?: string;
  invoiceType: string;
  invoiceTypeName?: string;
  title: string;
  taxNo?: string;
  amount: number;
  taxRate?: number;
  taxAmount?: number;
  content?: string;
  invoiceDate?: string;
  status: string;
  statusName?: string;
  applicantId?: number;
  applicantName?: string;
  issuerId?: number;
  issuerName?: string;
  fileUrl?: string;
  remark?: string;
  createdAt?: string;
}

export interface InvoiceQuery {
  invoiceNo?: string;
  feeId?: number;
  contractId?: number;
  clientId?: number;
  invoiceType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateInvoiceCommand {
  feeId?: number;
  contractId?: number;
  clientId: number;
  invoiceType: string;
  title: string;
  taxNo?: string;
  amount: number;
  taxRate?: number;
  content?: string;
  invoiceDate?: string;
  remark?: string;
}

export interface InvoiceStatisticsDTO {
  totalAmount: number;
  totalCount: number;
  monthlyAmount: number;
  yearlyAmount: number;
  byClient: Array<{ amount: number; clientName: string; count: number }>;
  byType: Array<{
    amount: number;
    count: number;
    type: string;
    typeName: string;
  }>;
  byStatus: Array<{ count: number; status: string; statusName: string }>;
  trends: Array<{ amount: number; count: number; month: string }>;
}

// ========== 提成管理 ==========
export interface CommissionDTO {
  id: number;
  commissionNo: string;
  paymentId: number;
  paymentNo?: string;
  feeId?: number;
  contractId?: number;
  matterId?: number;
  matterName?: string;
  clientId: number;
  clientName?: string;
  ruleId?: number;
  ruleCode?: string;
  paymentAmount: number;
  firmRetention: number;
  commissionBase: number;
  commissionRate?: number;
  commissionAmount?: number;
  taxAmount?: number;
  managementFee?: number;
  netCommission: number;
  originatorId?: number;
  originatorName?: string;
  originatorCommission?: number;
  status: string;
  statusName?: string;
  approvedBy?: number;
  approvedAt?: string;
  paidBy?: number;
  paidAt?: string;
  remark?: string;
  createdAt?: string;
}

export interface CommissionQuery {
  commissionNo?: string;
  paymentId?: number;
  matterId?: number;
  clientId?: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

// ========== 费用报销 ==========
export interface ExpenseDTO {
  id: number;
  expenseNo: string;
  matterId?: number;
  matterName?: string;
  expenseType: string;
  expenseTypeName?: string;
  amount: number;
  expenseDate: string;
  description?: string;
  applicantId: number;
  applicantName?: string;
  status: string;
  statusName?: string;
  approverId?: number;
  approverName?: string;
  approveTime?: string;
  payTime?: string;
  remark?: string;
  createdAt?: string;
}

export interface ExpenseQuery {
  expenseNo?: string;
  matterId?: number;
  expenseType?: string;
  status?: string;
  applicantId?: number;
  startDate?: string;
  endDate?: string;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateExpenseCommand {
  matterId?: number;
  expenseType: string;
  amount: number;
  expenseDate: string;
  description?: string;
  remark?: string;
}

// ========== 手动计算提成 ==========
export interface ManualCalculateCommissionCommand {
  paymentId: number;
  participants: {
    commissionAmount?: number;
    commissionRate?: number;
    participantId: number;
    remark?: string;
    userId: number;
  }[];
}

// ========== 通用分页结果 ==========
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
}
