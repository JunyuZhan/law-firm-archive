import { requestClient } from '#/api/request';

import type {
  LeaveApplication,
  LeaveBalance,
  LeaveQueryParams,
  LeaveType,
  PageResult,
} from './types';

/**
 * 获取请假类型列表
 */
export function getLeaveTypes(): Promise<LeaveType[]> {
  return requestClient.get('/admin/leave/types');
}

/**
 * 获取假期余额
 */
export function getLeaveBalance(params?: { userId?: number; year?: number }): Promise<LeaveBalance[]> {
  return requestClient.get('/admin/leave/balance', { params });
}

/**
 * 获取请假记录列表
 */
export function fetchLeaveList(params: LeaveQueryParams): Promise<PageResult<LeaveApplication>> {
  return requestClient.get('/admin/leave/applications', { params });
}

/**
 * 提交请假申请
 */
export function createLeave(data: { leaveTypeId: number; startDate: string; endDate: string; reason?: string }): Promise<LeaveApplication> {
  return requestClient.post('/admin/leave/applications', data);
}

/**
 * 获取待审批列表
 */
export function getPendingApplications(): Promise<LeaveApplication[]> {
  return requestClient.get('/admin/leave/applications/pending');
}

/**
 * 取消请假
 */
export function cancelLeave(id: number): Promise<void> {
  return requestClient.post(`/admin/leave/applications/${id}/cancel`);
}

/**
 * 审批通过
 */
export function approveLeave(data: { applicationId: number; comment?: string }): Promise<LeaveApplication> {
  return requestClient.post('/admin/leave/applications/approve', { ...data, approved: true });
}

/**
 * 审批拒绝
 */
export function rejectLeave(data: { applicationId: number; comment?: string }): Promise<LeaveApplication> {
  return requestClient.post('/admin/leave/applications/approve', { ...data, approved: false });
}
