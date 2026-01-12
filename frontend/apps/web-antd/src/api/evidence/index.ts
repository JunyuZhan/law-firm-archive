/**
 * 证据管理模块 API
 */
import { useAccessStore } from '@vben/stores';

import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 证据管理类型定义 ==========
export interface EvidenceDTO {
  id: number;
  evidenceNo: string;
  name: string;
  matterId?: number;
  matterName?: string;
  evidenceType?: string;
  evidenceTypeName?: string;
  source?: string;
  groupName?: string;
  provePurpose?: string;
  description?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  pageStart?: number;
  pageEnd?: number;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  fileSizeDisplay?: string;
  fileType?: string;  // 文件类型：image, pdf, word, excel, video, audio, other
  thumbnailUrl?: string;  // 缩略图URL（仅图片文件）
  submitterId?: number;
  submitterName?: string;
  submitTime?: string;
  status?: string;
  statusName?: string;
  sortOrder?: number;
  createdAt?: string;
}

export interface EvidenceQuery {
  evidenceNo?: string;
  name?: string;
  matterId?: number;
  evidenceType?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateEvidenceCommand {
  name: string;
  matterId: number;
  evidenceType: string;
  source?: string;
  groupName?: string;
  provePurpose?: string;
  description?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  pageStart?: number;
  pageEnd?: number;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  fileType?: string;
  thumbnailUrl?: string;
  /** 关联卷宗文件ID，引用 doc_document.id */
  documentId?: number;
}

export interface UpdateEvidenceCommand {
  name?: string;
  evidenceType?: string;
  source?: string;
  groupName?: string;
  provePurpose?: string;
  description?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  pageStart?: number;
  pageEnd?: number;
}

// ========== 证据管理 API ==========

/** 获取证据列表 */
export function getEvidenceList(params: EvidenceQuery) {
  return requestClient.get<PageResult<EvidenceDTO>>('/evidence', { params });
}

/** 获取证据详情 */
export function getEvidenceDetail(id: number) {
  return requestClient.get<EvidenceDTO>(`/evidence/${id}`);
}

/** 创建证据 */
export function createEvidence(data: CreateEvidenceCommand) {
  return requestClient.post<EvidenceDTO>('/evidence', data);
}

/** 更新证据 */
export function updateEvidence(id: number, data: UpdateEvidenceCommand) {
  return requestClient.put<EvidenceDTO>(`/evidence/${id}`, data);
}

/** 删除证据 */
export function deleteEvidence(id: number) {
  return requestClient.delete(`/evidence/${id}`);
}

/** 调整排序 */
export function updateEvidenceSort(id: number, sortOrder: number) {
  return requestClient.put(`/evidence/${id}/sort`, null, { params: { sortOrder } });
}

/** 批量调整分组 */
export function batchUpdateGroup(ids: number[], groupName: string) {
  return requestClient.post(`/evidence/batch-group`, ids, { params: { groupName } });
}

/** 按案件获取证据列表 */
export function getEvidenceByMatter(matterId: number) {
  return requestClient.get<EvidenceDTO[]>(`/evidence/matter/${matterId}`);
}

/** 获取案件的证据分组 */
export function getEvidenceGroups(matterId: number) {
  return requestClient.get<string[]>(`/evidence/matter/${matterId}/groups`);
}

/** 上传证据文件 */
export function uploadEvidenceFile(file: File) {
  return requestClient.upload<{ 
    fileUrl: string; 
    fileName: string; 
    fileSize: number;
    fileType: string;
    thumbnailUrl: string | null;
    canPreview: boolean;
  }>('/evidence/upload', { file });
}

/** 获取文件预览URL（预签名URL） */
export function getEvidencePreviewUrl(id: number) {
  return requestClient.get<{ fileUrl: string; fileName: string; fileType: string; canPreview: boolean }>(`/evidence/${id}/preview`);
}

/** 获取文件下载URL（预签名URL） */
export function getEvidenceDownloadUrl(id: number) {
  return requestClient.get<{ downloadUrl: string; fileName: string }>(`/evidence/${id}/download-url`);
}

/** 获取 OnlyOffice 预览URL（Docker 容器可访问） */
export function getEvidenceOnlyOfficeUrl(id: number) {
  return requestClient.get<{ fileUrl: string; fileName: string; fileType: string }>(`/evidence/${id}/onlyoffice-url`);
}

/** 获取文件缩略图URL */
export function getEvidenceThumbnailUrl(id: number) {
  return requestClient.get<{ thumbnailUrl: string | null; fileType: string }>(`/evidence/${id}/thumbnail`);
}

/** 获取文件文本内容 */
export function getEvidenceFileContent(id: number) {
  return requestClient.get<{ content: string; supported: boolean }>(`/evidence/${id}/content`);
}

// ========== 证据清单导出 ==========

export interface EvidenceExportItem {
  id?: number;
  name?: string;
  evidenceType?: string;
  evidenceTypeName?: string;
  provePurpose?: string;
  pageStart?: number;
  pageEnd?: number;
  source?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  listOrder?: number;
}

/** 导出证据清单（GET方式，导出全部） */
export function exportEvidenceListAll(matterId: number, format: 'word' | 'pdf' = 'word') {
  // 使用 window.open 打开下载链接
  const baseUrl = import.meta.env.VITE_GLOB_API_URL || '/api';
  window.open(`${baseUrl}/evidence/matter/${matterId}/export?format=${format}`, '_blank');
}

/** 导出证据清单（POST方式，指定证据项） */
export async function exportEvidenceList(matterId: number, items: EvidenceExportItem[], format: 'word' | 'pdf' = 'word') {
  const response = await requestClient.post(`/evidence/matter/${matterId}/export?format=${format}`, items, {
    responseType: 'blob',
  });
  
  // 创建下载链接
  const blob = new Blob([response as any], { 
    type: format === 'word' 
      ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' 
      : 'application/pdf' 
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `证据清单_${new Date().toISOString().slice(0, 10)}.${format === 'word' ? 'docx' : 'pdf'}`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/** 获取当前用户的访问令牌 */
export async function getAccessToken(): Promise<string | null> {
  const accessStore = useAccessStore();
  return accessStore.accessToken || null;
}

/** 直接下载证据清单（带 token 授权） */
export async function downloadEvidenceListDirect(
  matterId: number, 
  items: EvidenceExportItem[], 
  format: 'word' | 'pdf' = 'word',
  _token?: string  // token 参数保留但不使用，因为 requestClient 会自动带上
) {
  const response = await requestClient.post(`/evidence/matter/${matterId}/export?format=${format}`, items, {
    responseType: 'blob',
  });
  
  // 创建下载链接
  const blob = new Blob([response as any], { 
    type: format === 'word' 
      ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' 
      : 'application/pdf' 
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `证据清单_${new Date().toISOString().slice(0, 10)}.${format === 'word' ? 'docx' : 'pdf'}`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/** 批量下载证据文件为 ZIP */
export async function downloadEvidenceAsZip(ids: number[], fileName?: string) {
  const response = await requestClient.post('/evidence/batch-download', ids, {
    responseType: 'blob',
  });
  
  // 创建下载链接
  const blob = new Blob([response as any], { type: 'application/zip' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || `证据材料_${new Date().toISOString().slice(0, 10)}.zip`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

// ========== 证据清单管理 API ==========

export interface EvidenceListDTO {
  id: number;
  listNo: string;
  matterId: number;
  name: string;
  listType?: string;
  listTypeName?: string;
  evidenceIds?: string;
  evidenceIdList?: number[];
  evidences?: EvidenceDTO[];
  fileUrl?: string;
  fileName?: string;
  status?: string;
  statusName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateEvidenceListCommand {
  matterId: number;
  name: string;
  listType?: string;
  evidenceIds?: number[];
}

export interface EvidenceListCompareResult {
  list1: EvidenceListDTO;
  list2: EvidenceListDTO;
  addedIds: number[];
  removedIds: number[];
  commonIds: number[];
  addedEvidences?: EvidenceDTO[];
  removedEvidences?: EvidenceDTO[];
}

/** 获取证据清单列表 */
export function getEvidenceLists(params: { matterId?: number; listType?: string; pageNum?: number; pageSize?: number }) {
  return requestClient.get<PageResult<EvidenceListDTO>>('/evidence/list', { params });
}

/** 获取证据清单详情 */
export function getEvidenceListDetail(id: number) {
  return requestClient.get<EvidenceListDTO>(`/evidence/list/${id}`);
}

/** 创建证据清单 */
export function createEvidenceList(data: CreateEvidenceListCommand) {
  return requestClient.post<EvidenceListDTO>('/evidence/list', data);
}

/** 更新证据清单 */
export function updateEvidenceList(id: number, params: { name?: string; listType?: string }, evidenceIds?: number[]) {
  return requestClient.put<EvidenceListDTO>(`/evidence/list/${id}`, evidenceIds, { params });
}

/** 删除证据清单 */
export function deleteEvidenceList(id: number) {
  return requestClient.delete(`/evidence/list/${id}`);
}

/** 按案件获取证据清单列表 */
export function getEvidenceListsByMatter(matterId: number) {
  return requestClient.get<EvidenceListDTO[]>(`/evidence/list/matter/${matterId}`);
}

/** 获取证据清单历史 */
export function getEvidenceListHistory(matterId: number) {
  return requestClient.get<EvidenceListDTO[]>(`/evidence/list/matter/${matterId}/history`);
}

/** 对比两个证据清单 */
export function compareEvidenceLists(listId1: number, listId2: number) {
  return requestClient.get<EvidenceListCompareResult>('/evidence/list/compare', { params: { listId1, listId2 } });
}

/** 生成证据清单文件 */
export function generateEvidenceListFile(id: number, format: 'docx' | 'pdf' = 'docx') {
  return requestClient.post<string>(`/evidence/list/${id}/generate`, null, { params: { format } });
}

/** 导出证据清单为Word */
export async function exportEvidenceListToWord(id: number) {
  const response = await requestClient.get(`/evidence/list/${id}/export/word`, {
    responseType: 'blob',
  });
  
  const blob = new Blob([response as any], { 
    type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `证据清单_${new Date().toISOString().slice(0, 10)}.docx`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/** 导出证据清单为PDF */
export async function exportEvidenceListToPdf(id: number) {
  const response = await requestClient.get(`/evidence/list/${id}/export/pdf`, {
    responseType: 'blob',
  });
  
  const blob = new Blob([response as any], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `证据清单_${new Date().toISOString().slice(0, 10)}.pdf`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

// 证据清单类型选项
export const EVIDENCE_LIST_TYPE_OPTIONS = [
  { value: 'SUBMISSION', label: '提交清单' },
  { value: 'EXCHANGE', label: '交换清单' },
  { value: 'COURT', label: '庭审清单' },
];

// 导出类型
export type * from './types';

