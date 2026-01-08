/**
 * 审批中心 API
 */
import { requestClient } from '#/api/request';

export interface ApprovalDTO {
  id: number;
  approvalNo: string;
  businessType: string;
  businessTypeName?: string;
  businessId: number;
  businessNo?: string;
  businessTitle?: string;
  applicantId: number;
  applicantName?: string;
  approverId: number;
  approverName?: string;
  status: string;
  statusName?: string;
  comment?: string;
  approvedAt?: string;
  priority?: string;
  priorityName?: string;
  urgency?: string;
  urgencyName?: string;
  createdAt?: string;
  updatedAt?: string;
  businessSnapshot?: string;
}

export interface ApprovalQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
  businessType?: string;
  applicantId?: number;
  approverId?: number;
}

export interface ApproveCommand {
  approvalId: number;
  result: 'APPROVED' | 'REJECTED';
  comment?: string;
}

/** 获取审批列表 */
export function getApprovalList(params: ApprovalQuery) {
  return requestClient.get<{ list: ApprovalDTO[]; total: number }>('/workbench/approval/list', { params });
}

/** 获取待审批列表 */
export function getPendingApprovals() {
  return requestClient.get<ApprovalDTO[]>('/workbench/approval/pending');
}

/** 获取我发起的审批 */
export function getMyInitiatedApprovals() {
  return requestClient.get<ApprovalDTO[]>('/workbench/approval/my-initiated');
}

/** 获取我审批过的记录（审批历史） */
export function getMyApprovedHistory() {
  return requestClient.get<ApprovalDTO[]>('/workbench/approval/my-history');
}

/** 获取审批详情 */
export function getApprovalDetail(id: number) {
  return requestClient.get<ApprovalDTO>(`/workbench/approval/${id}`);
}

/** 审批操作（通过/拒绝） */
export function approveApproval(data: ApproveCommand) {
  return requestClient.post('/workbench/approval/approve', data);
}

/** 获取业务审批记录 */
export function getBusinessApprovals(businessType: string, businessId: number) {
  return requestClient.get<ApprovalDTO[]>('/workbench/approval/business', {
    params: { businessType, businessId },
  });
}

/** 获取工作台统计数据 */
export function getWorkbenchStats() {
  return requestClient.get<import('./types').WorkbenchStatsDTO>('/workbench/stats');
}
