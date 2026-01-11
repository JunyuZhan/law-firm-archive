/**
 * 加班管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface OvertimeApplicationDTO {
  id: number;
  applicationNo?: string;
  userId?: number;
  userName?: string;
  overtimeDate: string;
  startTime: string;
  endTime: string;
  overtimeHours?: number;
  reason?: string;
  workContent?: string;
  status?: string;
  statusName?: string;
  approverId?: number;
  approverName?: string;
  approvedAt?: string;
  approvalComment?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ApplyOvertimeCommand {
  overtimeDate: string;
  startTime: string;
  endTime: string;
  reason?: string;
  workContent?: string;
}

export interface ApproveOvertimeRequest {
  approved: boolean;
  comment?: string;
}

// ========== API 函数 ==========

/** 申请加班 */
export function applyOvertime(data: ApplyOvertimeCommand) {
  return requestClient.post<OvertimeApplicationDTO>(
    '/admin/overtime/apply',
    data,
  );
}

/** 审批加班申请 */
export function approveOvertime(id: number, data: ApproveOvertimeRequest) {
  return requestClient.post<OvertimeApplicationDTO>(
    `/admin/overtime/${id}/approve`,
    data,
  );
}

/** 查询我的加班申请 */
export function getMyOvertimeApplications() {
  return requestClient.get<OvertimeApplicationDTO[]>('/admin/overtime/my');
}

/** 查询指定日期范围的加班申请 */
export function getOvertimeApplicationsByDateRange(
  startDate: string,
  endDate: string,
) {
  return requestClient.get<OvertimeApplicationDTO[]>('/admin/overtime/range', {
    params: { startDate, endDate },
  });
}
