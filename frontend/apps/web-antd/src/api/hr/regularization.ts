/**
 * 转正申请管理 API
 */
import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 类型定义 ==========
export interface RegularizationDTO {
  id: number;
  employeeId: number;
  userId?: number;
  employeeName?: string;
  applicationNo?: string;
  probationStartDate?: string;
  probationEndDate?: string;
  applicationDate?: string;
  expectedRegularDate?: string;
  selfEvaluation?: string;
  supervisorEvaluation?: string;
  hrEvaluation?: string;
  status?: string;
  statusName?: string;
  approverId?: number;
  approverName?: string;
  approvedDate?: string;
  comment?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RegularizationQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  employeeId?: number;
  departmentId?: number;
}

export interface CreateRegularizationCommand {
  employeeId: number;
  probationStartDate?: string;
  probationEndDate?: string;
  expectedRegularDate?: string;
  selfEvaluation?: string;
}

export interface ApproveRegularizationCommand {
  approved: boolean;
  comment?: string;
}

// ========== API 函数 ==========

/** 分页查询转正申请 */
export function getRegularizationList(params: RegularizationQuery) {
  return requestClient.get<PageResult<RegularizationDTO>>('/hr/regularization', { params });
}

/** 根据ID查询转正申请 */
export function getRegularizationDetail(id: number) {
  return requestClient.get<RegularizationDTO>(`/hr/regularization/${id}`);
}

/** 根据员工ID查询转正申请 */
export function getRegularizationsByEmployeeId(employeeId: number) {
  return requestClient.get<RegularizationDTO[]>(`/hr/regularization/employee/${employeeId}`);
}

/** 创建转正申请 */
export function createRegularization(data: CreateRegularizationCommand) {
  return requestClient.post<RegularizationDTO>('/hr/regularization', data);
}

/** 审批转正申请 */
export function approveRegularization(id: number, data: ApproveRegularizationCommand) {
  return requestClient.post<RegularizationDTO>(`/hr/regularization/${id}/approve`, data);
}

/** 删除转正申请 */
export function deleteRegularization(id: number) {
  return requestClient.delete(`/hr/regularization/${id}`);
}

