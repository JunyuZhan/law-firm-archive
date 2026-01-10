/**
 * 客户管理模块 API
 */
import { requestClient } from '#/api/request';

import type {
  ApplyConflictCheckCommand,
  ClientDTO,
  ClientQuery,
  ConflictCheckDTO,
  ConflictCheckQuery,
  CreateClientCommand,
  CreateLeadCommand,
  LeadDTO,
  LeadQuery,
  PageResult,
  UpdateClientCommand,
} from './types';

// ========== 客户管理 API ==========

/** 获取客户列表 */
export function getClientList(params: ClientQuery) {
  return requestClient.get<PageResult<ClientDTO>>('/client/list', { params });
}

/** 获取客户详情 */
export function getClientDetail(id: number) {
  return requestClient.get<ClientDTO>(`/client/${id}`);
}

/** 创建客户 */
export function createClient(data: CreateClientCommand) {
  return requestClient.post<ClientDTO>('/client', data);
}

/** 更新客户 */
export function updateClient(data: UpdateClientCommand) {
  return requestClient.put<ClientDTO>('/client', data);
}

/** 删除客户 */
export function deleteClient(id: number) {
  return requestClient.delete(`/client/${id}`);
}

/** 修改客户状态 */
export function changeClientStatus(id: number, status: string) {
  return requestClient.put(`/client/${id}/status`, { status });
}

/** 潜在客户转正式 */
export function convertToFormal(id: number) {
  return requestClient.post(`/client/${id}/convert`);
}

/** 导出客户 */
export function exportClients(params: ClientQuery) {
  return requestClient.get('/client/export', {
    params,
    responseType: 'blob',
  });
}

/** 批量导入客户 */
export function importClients(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<{ success: number; failure: number; errors?: string[] }>('/client/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/** 批量删除客户 */
export function batchDeleteClients(ids: number[]) {
  return requestClient.delete('/client/batch', { data: { ids } });
}

// ========== 利冲审查 API ==========

/** 获取利冲审查列表 */
export function getConflictCheckList(params: ConflictCheckQuery) {
  return requestClient.get<PageResult<ConflictCheckDTO>>('/client/conflict-check/list', { params });
}

/** 获取利冲审查详情 */
export function getConflictCheckDetail(id: number) {
  return requestClient.get<ConflictCheckDTO>(`/client/conflict-check/${id}`);
}

/** 申请利冲审查（简化版，手动申请） */
export function applyConflictCheck(data: ApplyConflictCheckCommand) {
  return requestClient.post<ConflictCheckDTO>('/client/conflict-check/apply', data);
}

/** 审核利冲审查（通过） */
export function approveConflictCheck(id: number, comment?: string) {
  return requestClient.post(`/client/conflict-check/${id}/approve`, { comment });
}

/** 审核利冲审查（拒绝） */
export function rejectConflictCheck(id: number, comment?: string) {
  return requestClient.post(`/client/conflict-check/${id}/reject`, { comment });
}

/** 审核利冲审查 */
export function reviewConflictCheck(id: number, data: { approved: boolean; comment?: string }) {
  if (data.approved) {
    return approveConflictCheck(id, data.comment);
  } else {
    return rejectConflictCheck(id, data.comment);
  }
}

/** 申请利益冲突豁免 */
export function applyExemption(data: { conflictCheckId: number; exemptionReason: string; exemptionDescription?: string }) {
  return requestClient.post<ConflictCheckDTO>('/client/conflict-check/exemption/apply', data);
}

/** 批准豁免申请 */
export function approveExemption(id: number, comment?: string) {
  return requestClient.post(`/client/conflict-check/exemption/${id}/approve`, { comment });
}

/** 拒绝豁免申请 */
export function rejectExemption(id: number, comment?: string) {
  return requestClient.post(`/client/conflict-check/exemption/${id}/reject`, { comment });
}

/** 冲突候选项 */
export interface ConflictCandidate {
  clientId: number;
  clientNo: string;
  clientName: string;
  clientType: string;
  matchScore: number;
  matchType: 'EXACT' | 'CONTAINS' | 'SIMILAR';
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  riskReason: string;
}

/** 快速利冲检索结果 */
export interface QuickConflictCheckResult {
  hasConflict: boolean;
  conflictDetail?: string;
  candidates: ConflictCandidate[];
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
  riskSummary: string;
}

/** 快速利冲检索（增强版，返回候选列表和风险评估） */
export function quickConflictCheck(data: { clientName: string; opposingParty: string }) {
  return requestClient.post<QuickConflictCheckResult>('/client/conflict-check/quick', data);
}

// ========== 案源管理 API ==========

/** 获取案源列表 */
export function getLeadList(params: LeadQuery) {
  return requestClient.get<PageResult<LeadDTO>>('/client/lead', { params });
}

/** 获取案源详情 */
export function getLeadDetail(id: number) {
  return requestClient.get<LeadDTO>(`/client/lead/${id}`);
}

/** 创建案源 */
export function createLead(data: CreateLeadCommand) {
  return requestClient.post<LeadDTO>('/client/lead', data);
}

/** 更新案源 */
export function updateLead(id: number, data: Partial<CreateLeadCommand>) {
  return requestClient.put<LeadDTO>(`/client/lead/${id}`, data);
}

/** 删除案源 */
export function deleteLead(id: number) {
  return requestClient.delete(`/client/lead/${id}`);
}

/** 转化案源为客户 */
export function convertLeadToClient(id: number, data?: { clientId?: number; matterId?: number }) {
  return requestClient.post<LeadDTO>(`/client/lead/${id}/convert`, data || {});
}

/** 跟进案源 */
export function followUpLead(id: number, data: { content: string; nextFollowUpTime?: string }) {
  return requestClient.post(`/client/lead/${id}/follow-up`, data);
}

// 导出类型
export type * from './types';
