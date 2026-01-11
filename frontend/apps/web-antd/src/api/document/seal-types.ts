/**
 * 印章管理模块类型定义
 */

export interface SealDTO {
  id: number;
  name: string;
  sealType?: string;
  sealTypeName?: string;
  keeperId?: number;
  keeperName?: string;
  imageUrl?: string;
  useCount?: number;
  status: string;
  statusName?: string;
  description?: string;
  createdAt?: string;
}

export interface SealQuery {
  name?: string;
  sealType?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateSealCommand {
  name: string;
  sealType: string;
  keeperId?: number;
  imageUrl?: string;
  description?: string;
}

export interface UpdateSealCommand {
  name?: string;
  keeperId?: number;
  keeperName?: string;
  imageUrl?: string;
  description?: string;
}

export interface SealApplicationDTO {
  id: number;
  applicationNo: string;
  sealId: number;
  sealName?: string;
  sealType?: string;
  matterId?: number;
  matterName?: string;
  documentName?: string;
  documentType?: string;
  copies: number;
  usePurpose?: string;
  expectedUseDate?: string;
  actualUseDate?: string;
  applicantId?: number;
  applicantName?: string;
  departmentId?: number;
  departmentName?: string;
  status: string;
  statusName?: string;
  approvedBy?: number;
  approvedByName?: string;
  approvedAt?: string;
  approvalComment?: string;
  usedBy?: number;
  usedByName?: string;
  usedAt?: string;
  useRemark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SealApplicationQuery {
  applicationNo?: string;
  sealId?: number;
  applicantId?: number;
  matterId?: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateSealApplicationCommand {
  sealId: number;
  matterId?: number;
  documentName: string;
  documentType?: string;
  copies?: number;
  usePurpose?: string;
  expectedUseDate?: string;
  approverId: number; // 审批人ID（必填）
}
