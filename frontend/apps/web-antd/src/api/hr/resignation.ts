import type { PageResult } from '../matter/types';

/**
 * 离职申请管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface ResignationDTO {
  id: number;
  employeeId: number;
  userId?: number;
  employeeName?: string;
  applicationNo?: string;
  resignationType: string;
  resignationTypeName?: string;
  resignationDate: string;
  lastWorkDate: string;
  reason?: string;
  handoverPersonId?: number;
  handoverPersonName?: string;
  handoverStatus?: string;
  handoverStatusName?: string;
  handoverNote?: string;
  status?: string;
  statusName?: string;
  approverId?: number;
  approverName?: string;
  approvedDate?: string;
  comment?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ResignationQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  employeeId?: number;
  departmentId?: number;
}

export interface CreateResignationCommand {
  employeeId: number;
  resignationType: string;
  resignationDate: string;
  lastWorkDate: string;
  reason?: string;
  handoverPersonId?: number;
  handoverNote?: string;
}

export interface ApproveResignationCommand {
  approved: boolean;
  comment?: string;
}

// ========== API 函数 ==========

/** 分页查询离职申请 */
export function getResignationList(params: ResignationQuery) {
  return requestClient.get<PageResult<ResignationDTO>>('/hr/resignation', {
    params,
  });
}

/** 根据ID查询离职申请 */
export function getResignationDetail(id: number) {
  return requestClient.get<ResignationDTO>(`/hr/resignation/${id}`);
}

/** 根据员工ID查询离职申请 */
export function getResignationsByEmployeeId(employeeId: number) {
  return requestClient.get<ResignationDTO[]>(
    `/hr/resignation/employee/${employeeId}`,
  );
}

/** 创建离职申请 */
export function createResignation(data: CreateResignationCommand) {
  return requestClient.post<ResignationDTO>('/hr/resignation', data);
}

/** 审批离职申请 */
export function approveResignation(
  id: number,
  data: ApproveResignationCommand,
) {
  return requestClient.post<ResignationDTO>(
    `/hr/resignation/${id}/approve`,
    data,
  );
}

/** 完成交接 */
export function completeResignationHandover(id: number, handoverNote?: string) {
  return requestClient.post<ResignationDTO>(
    `/hr/resignation/${id}/complete-handover`,
    null,
    {
      params: { handoverNote },
    },
  );
}

/** 删除离职申请 */
export function deleteResignation(id: number) {
  return requestClient.delete(`/hr/resignation/${id}`);
}
