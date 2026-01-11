/**
 * 客户管理模块类型定义
 */

// ========== 客户管理 ==========

/**
 * 客户简要信息（用于下拉选择）
 * 公共接口返回，不包含敏感信息
 */
export interface ClientSimpleDTO {
  id: number;
  clientNo: string;
  name: string;
  clientType: string;
  status: string;
}

export interface ClientDTO {
  id: number;
  clientNo: string;
  name: string;
  clientType: string;
  clientTypeName?: string;
  creditCode?: string;
  idCard?: string;
  legalRepresentative?: string;
  registeredAddress?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
  industry?: string;
  source?: string;
  level?: string;
  levelName?: string;
  category?: string;
  categoryName?: string;
  status: string;
  statusName?: string;
  originatorId?: number;
  originatorName?: string;
  responsibleLawyerId?: number;
  responsibleLawyerName?: string;
  firstCooperationDate?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ClientQuery {
  name?: string;
  clientType?: string;
  level?: string;
  status?: string;
  originatorId?: number;
  responsibleLawyerId?: number;
  keyword?: string; // 关键字搜索（名称、联系人、电话）
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateClientCommand {
  name: string;
  clientType: string;
  creditCode?: string;
  idCard?: string;
  legalRepresentative?: string;
  registeredAddress?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
  industry?: string;
  source?: string;
  level?: string;
  category?: string;
  originatorId?: number;
  responsibleLawyerId?: number;
  firstCooperationDate?: string; // 首次合作日期
  remark?: string;
}

export interface UpdateClientCommand extends Partial<CreateClientCommand> {
  id: number;
}

// ========== 利冲审查 ==========
export interface ConflictCheckDTO {
  id: number;
  checkNo: string;
  clientName: string;
  opposingParty: string;
  matterName?: string;
  checkType: string;
  checkTypeName?: string;
  status: string;
  statusName?: string;
  result?: string;
  resultName?: string;
  applicantId: number;
  applicantName: string;
  applyTime: string;
  reviewerId?: number;
  reviewerName?: string;
  reviewTime?: string;
  reviewComment?: string;
  conflictDetails?: string;
  createdAt?: string;
}

export interface ConflictCheckQuery {
  checkNo?: string;
  clientName?: string;
  status?: string;
  result?: string;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface ApplyConflictCheckCommand {
  clientName: string;
  opposingParty: string;
  matterName?: string;
  checkType: string;
  remark?: string;
}

// ========== 案源管理 ==========
export interface LeadDTO {
  id: number;
  leadNo: string;
  clientName: string;
  contactPerson?: string;
  contactPhone?: string;
  source: string;
  sourceName?: string;
  matterType?: string;
  matterTypeName?: string;
  estimatedAmount?: number;
  status: string;
  statusName?: string;
  followUpUserId?: number;
  followUpUserName?: string;
  nextFollowUpTime?: string;
  remark?: string;
  createdAt?: string;
}

export interface LeadQuery {
  clientName?: string;
  source?: string;
  status?: string;
  followUpUserId?: number;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateLeadCommand {
  clientName: string;
  contactPerson?: string;
  contactPhone?: string;
  source: string;
  matterType?: string;
  estimatedAmount?: number;
  followUpUserId?: number;
  remark?: string;
}

// ========== 通用分页结果 ==========
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number; // 总页数（后端返回）
}
