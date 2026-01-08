/**
 * 行政合同查询 API（只读）
 * 用于司法局报备、介绍信等
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2
 */
import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface AdminContractViewDTO {
  id: number;
  contractNo: string;
  name: string;
  signDate: string;
  caseType: string;
  caseTypeName: string;
  causeOfAction?: string;
  causeOfActionName?: string;
  opposingParty?: string;
  totalAmount: number;
  status: string;
  jurisdictionCourt?: string;
  trialStage?: string;
  trialStageName?: string;
  clientId?: number;
  clientName?: string;
  leadLawyerId?: number;
  leadLawyerName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminContractQueryDTO {
  contractNo?: string;
  clientName?: string;
  caseType?: string;
  leadLawyerId?: number;
  signDateFrom?: string;
  signDateTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// ==================== API 函数 ====================

/**
 * 查询已审批合同列表（只读）
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
export function getAdminContractList(params: AdminContractQueryDTO) {
  return requestClient.get<PageResult<AdminContractViewDTO>>('/admin/contract/list', { params });
}

/**
 * 获取合同详情（只读）
 * Requirement: 5.5
 */
export function getAdminContractDetail(id: number) {
  return requestClient.get<AdminContractViewDTO>(`/admin/contract/${id}`);
}

/**
 * 导出司法局报备收案清单
 * Requirements: 6.1, 6.2
 */
export function exportJudicialFiling(year: number, month: number, customFields?: string[]) {
  const params: Record<string, any> = { year, month };
  if (customFields && customFields.length > 0) {
    params.customFields = customFields;
  }
  
  return requestClient.get('/admin/contract/export/judicial-filing', {
    params,
    responseType: 'blob',
    responseReturn: 'body', // 直接返回blob数据，跳过JSON解析
  } as any);
}

/**
 * 下载司法局报备Excel（处理blob响应）
 */
export async function downloadJudicialFilingExcel(year: number, month: number, customFields?: string[]) {
  const response = await exportJudicialFiling(year, month, customFields);
  
  // 创建下载链接
  const blob = new Blob([response as any], { 
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `收案清单_${year}年${month}月.xlsx`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/**
 * 导出合同列表Excel（根据查询条件）
 */
export function exportContractList(params: AdminContractQueryDTO) {
  return requestClient.get('/admin/contract/export/list', {
    params,
    responseType: 'blob',
    responseReturn: 'body', // 直接返回blob数据，跳过JSON解析
  } as any);
}

/**
 * 下载合同列表Excel（处理blob响应）
 */
export async function downloadContractListExcel(params: AdminContractQueryDTO) {
  const response = await exportContractList(params);
  
  // 创建下载链接
  const blob = new Blob([response as any], { 
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  const today = new Date();
  const dateStr = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}${String(today.getDate()).padStart(2, '0')}`;
  link.download = `合同列表_${dateStr}.xlsx`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
