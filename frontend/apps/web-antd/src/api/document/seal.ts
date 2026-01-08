/**
 * 印章管理模块 API
 */
import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 印章管理类型定义 ==========
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

// ========== 印章申请类型定义 ==========
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

// ========== 印章管理 API ==========

/** 获取印章列表 */
export function getSealList(params: SealQuery) {
  return requestClient.get<PageResult<SealDTO>>('/document/seal', { params });
}

/** 获取印章详情 */
export function getSealDetail(id: number) {
  return requestClient.get<SealDTO>(`/document/seal/${id}`);
}

/** 创建印章 */
export function createSeal(data: CreateSealCommand) {
  return requestClient.post<SealDTO>('/document/seal', data);
}

/** 更新印章 */
export function updateSeal(id: number, data: UpdateSealCommand) {
  return requestClient.put<SealDTO>(`/document/seal/${id}`, data);
}

/** 变更印章状态 */
export function changeSealStatus(id: number, status: string) {
  return requestClient.put(`/document/seal/${id}/status`, null, { params: { status } });
}

/** 删除印章 */
export function deleteSeal(id: number) {
  return requestClient.delete(`/document/seal/${id}`);
}

// ========== 印章申请 API ==========

/** 获取用印申请列表 */
export function getSealApplicationList(params: SealApplicationQuery) {
  return requestClient.get<PageResult<SealApplicationDTO>>('/document/seal-application', { params });
}

/** 获取申请详情 */
export function getSealApplicationDetail(id: number) {
  return requestClient.get<SealApplicationDTO>(`/document/seal-application/${id}`);
}

/** 创建用印申请 */
export function createSealApplication(data: CreateSealApplicationCommand) {
  return requestClient.post<SealApplicationDTO>('/document/seal-application', data);
}

/** 审批通过 */
export function approveSealApplication(id: number, comment?: string) {
  return requestClient.post<SealApplicationDTO>(`/document/seal-application/${id}/approve`, null, { params: { comment } });
}

/** 审批拒绝 */
export function rejectSealApplication(id: number, comment?: string) {
  return requestClient.post<SealApplicationDTO>(`/document/seal-application/${id}/reject`, null, { params: { comment } });
}

/** 登记用印 */
export function registerSealUsage(id: number, remark?: string) {
  return requestClient.post<SealApplicationDTO>(`/document/seal-application/${id}/use`, null, { params: { remark } });
}

/** 取消申请 */
export function cancelSealApplication(id: number) {
  return requestClient.post(`/document/seal-application/${id}/cancel`);
}

/** 获取待审批列表 */
export function getPendingSealApplications() {
  return requestClient.get<SealApplicationDTO[]>('/document/seal-application/pending');
}

/** 获取可选审批人列表 */
export function getSealApplicationApprovers(applicantId?: number) {
  return requestClient.get<Array<{
    id: number;
    realName: string;
    departmentName: string;
    position: string;
  }>>('/document/seal-application/approvers', {
    params: applicantId ? { applicantId } : undefined,
  });
}

/** 获取保管人待办理的申请（审批通过且印章的保管人是当前用户） */
export function getPendingForKeeper() {
  return requestClient.get<SealApplicationDTO[]>('/document/seal-application/keeper/pending');
}

/** 获取保管人已办理的申请（已用印且印章的保管人是当前用户） */
export function getProcessedByKeeper() {
  return requestClient.get<SealApplicationDTO[]>('/document/seal-application/keeper/processed');
}

/** 检查当前用户是否是任何印章的保管人 */
export function checkIsKeeper() {
  return requestClient.get<boolean>('/document/seal-application/keeper/check');
}

// 导出类型
export type * from './seal-types';

