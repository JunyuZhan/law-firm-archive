/**
 * 项目管理模块类型定义
 */

// ========== 项目/案件管理 ==========

/**
 * 项目简要信息（用于下拉选择）
 * 公共接口返回，不包含敏感信息
 */
export interface MatterSimpleDTO {
  id: number;
  matterNo: string;
  name: string;
  matterType: string;
  matterTypeName?: string;
  caseType?: string;
  caseTypeName?: string;
  status: string;
  statusName?: string;
  clientName?: string;
  contractNo?: string;
  leadLawyerName?: string;
  causeOfAction?: string;
  causeOfActionName?: string;
  opposingParty?: string;
  filingDate?: string;
  createdAt?: string;
}

export interface MatterClientDTO {
  id?: number;
  matterId?: number;
  clientId: number;
  clientName?: string;
  clientType?: string;
  clientRole: string;
  clientRoleName?: string;
  isPrimary: boolean;
}

export interface MatterDTO {
  id: number;
  matterNo: string;
  name: string;
  matterType: string;
  matterTypeName?: string;
  caseType?: string;
  caseTypeName?: string;
  litigationStage?: string;
  litigationStageName?: string;
  causeOfAction?: string;
  causeOfActionName?: string;
  businessType?: string;
  clientId: number;
  clientName?: string;
  clients?: MatterClientDTO[]; // 多客户列表
  opposingParty?: string;
  opposingLawyerName?: string;
  opposingLawyerLicenseNo?: string;
  opposingLawyerFirm?: string;
  opposingLawyerPhone?: string;
  opposingLawyerEmail?: string;
  description?: string;
  status: string;
  statusName?: string;
  originatorId?: number;
  originatorName?: string;
  leadLawyerId?: number;
  leadLawyerName?: string;
  departmentId?: number;
  departmentName?: string;
  feeType?: string;
  feeTypeName?: string;
  estimatedFee?: number;
  actualFee?: number;
  filingDate?: string;
  expectedClosingDate?: string;
  actualClosingDate?: string;
  claimAmount?: number;
  outcome?: string;
  contractId?: number;
  contractNo?: string;
  contractAmount?: number;
  remark?: string;
  conflictStatus?: string;
  participants?: MatterParticipantDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface MatterParticipantDTO {
  id: number;
  matterId: number;
  userId: number;
  userName?: string;
  role: string;
  roleName?: string;
  commissionRate?: number;
  isOriginator?: boolean;
}

export interface MatterQuery {
  matterNo?: string;
  name?: string;
  matterType?: string;
  clientId?: number;
  status?: string;
  leadLawyerId?: number;
  myMatters?: boolean;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface MatterClientCommand {
  clientId: number;
  clientRole: string; // PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人, APPELLANT-上诉人, APPELLEE-被上诉人, EXECUTION_APPLICANT-申请执行人, EXECUTION_RESPONDENT-被执行人, SUSPECT-犯罪嫌疑人, DEFENDANT_CRIMINAL-被告人, RETRIAL_APPLICANT-再审申请人, RETRIAL_RESPONDENT-再审被申请人
  isPrimary: boolean;
}

export interface CreateMatterCommand {
  name: string;
  matterType: string;
  caseType?: string;
  litigationStage?: string;
  causeOfAction?: string;
  businessType?: string;
  clientId: number;
  clients?: MatterClientCommand[]; // 多客户列表
  opposingParty?: string;
  opposingLawyerName?: string;
  opposingLawyerLicenseNo?: string;
  opposingLawyerFirm?: string;
  opposingLawyerPhone?: string;
  opposingLawyerEmail?: string;
  description?: string;
  originatorId?: number;
  leadLawyerId?: number;
  departmentId?: number;
  feeType?: string;
  estimatedFee?: number;
  filingDate?: string;
  expectedClosingDate?: string;
  claimAmount?: number;
  contractId?: number;
  remark?: string;
  participants?: MatterParticipantCommand[];
}

export interface MatterParticipantCommand {
  userId: number;
  role: string;
  commissionRate?: number;
  isOriginator?: boolean;
}

export interface UpdateMatterCommand extends Partial<CreateMatterCommand> {
  id: number;
}

// ========== 工时管理 ==========
export interface TimesheetDTO {
  id: number;
  matterId: number;
  matterName?: string;
  matterNo?: string;
  userId: number;
  userName?: string;
  workDate: string;
  hours: number;
  hourlyRate?: number;
  amount?: number;
  workType: string;
  workTypeName?: string;
  workContent?: string;
  status: string;
  statusName?: string;
  billable: boolean;
  createdAt?: string;
}

export interface TimesheetQuery {
  matterId?: number;
  userId?: number;
  workType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateTimesheetCommand {
  matterId: number;
  workDate: string;
  hours: number;
  workType: string;
  workContent: string;
  billable?: boolean;
}

// ========== 任务管理 ==========
export interface TaskDTO {
  id: number;
  matterId: number;
  matterName?: string;
  title: string;
  description?: string;
  assigneeId?: number;
  assigneeName?: string;
  priority: string;
  priorityName?: string;
  status: string;
  statusName?: string;
  dueDate?: string;
  completedAt?: string;
  createdAt?: string;
  createdBy?: number;
  reviewStatus?: string;
  reviewComment?: string;
  reviewedAt?: string;
  reviewedBy?: number;
}

export interface TaskQuery {
  matterId?: number;
  assigneeId?: number;
  status?: string;
  priority?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateTaskCommand {
  matterId: number;
  title: string;
  description?: string;
  assigneeId?: number;
  priority?: string;
  dueDate?: string;
}

// ========== 项目时间线 ==========
export interface MatterTimelineDTO {
  eventId: string;
  eventType: string;
  eventTypeName?: string;
  title: string;
  description?: string;
  eventTime: string;
  operatorId?: number;
  operatorName?: string;
  relatedId?: number;
  relatedType?: string;
}

// ========== 通用分页结果 ==========
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number; // 总页数（后端返回）
}
