/**
 * 证据管理模块 API
 */
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

/** 批量下载证据文件（打包为 ZIP） */
export function batchDownloadEvidence(ids: number[]) {
  return requestClient.post('/evidence/batch-download', ids, {
    responseType: 'blob',
  });
}

/** 批量下载并触发浏览器下载 */
export async function downloadEvidenceAsZip(ids: number[], filename?: string) {
  const response = await batchDownloadEvidence(ids);
  const blob = new Blob([response as any], { type: 'application/zip' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || `evidence_${Date.now()}.zip`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

// 导出类型
export type * from './types';

