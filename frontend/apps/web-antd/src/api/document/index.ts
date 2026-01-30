import type { PageResult } from '../matter/types';

/**
 * 文档管理模块 API
 */
import { requestClient } from '#/api/request';

// ========== 文档管理类型定义 ==========
export interface DocumentDTO {
  id: number;
  name: string;
  title?: string;
  matterId?: number;
  matterName?: string;
  categoryId?: number;
  categoryName?: string;
  filePath?: string;
  fileName?: string;
  fileSize?: number;
  fileType?: string;
  version?: number;
  uploaderId?: number;
  uploaderName?: string;
  uploadTime?: string;
  description?: string;
  createdAt?: string;
  /** 文件分类（evidence/contract/litigation/other） */
  fileCategory?: string;
  /** 卷宗目录路径 */
  folderPath?: string;
  /** 关联的卷宗目录项ID */
  dossierItemId?: number;
  /** 显示排序顺序 */
  displayOrder?: number;
  /** 缩略图URL（图片和PDF文件） */
  thumbnailUrl?: string;
  /**
   * MinIO桶名称，默认law-firm
   */
  bucketName?: string;
  /**
   * 存储路径：document/{matterId}/{folderPath}/
   */
  storagePath?: string;
  /**
   * 物理文件名：{YYYYMMDD}_{UUID}_{fileName}.{ext}
   */
  physicalName?: string;
  /**
   * 文件Hash值（SHA-256），用于去重和校验
   */
  fileHash?: string;
}

export interface DocumentQuery {
  name?: string;
  matterId?: number;
  categoryId?: number;
  uploaderId?: number;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateDocumentCommand {
  title: string;
  matterId?: number;
  categoryId?: number;
  filePath: string;
  fileName: string;
  fileSize: number;
  fileType?: string;
  mimeType?: string;
  securityLevel?: string;
  stage?: string;
  tags?: string[];
  description?: string;
}

export interface UpdateDocumentCommand {
  id: number;
  title?: string; // 文档标题（重命名）
  categoryId?: number;
  description?: string;
}

export interface UploadNewVersionCommand {
  documentId: number;
  filePath: string;
  fileName: string;
  fileSize: number;
  fileType?: string;
}

// ========== 文档管理 API ==========

/** 获取文档列表 */
export function getDocumentList(params: DocumentQuery) {
  return requestClient.get<PageResult<DocumentDTO>>('/document', { params });
}

/** 获取项目下的所有文档 */
export function getDocumentsByMatter(matterId: number) {
  return requestClient.get<DocumentDTO[]>(`/document/matter/${matterId}`);
}

/** 获取文档详情 */
export function getDocumentDetail(id: number) {
  return requestClient.get<DocumentDTO>(`/document/${id}`);
}

/** 创建文档 */
export function createDocument(data: CreateDocumentCommand) {
  return requestClient.post<DocumentDTO>('/document', data);
}

/** 更新文档 */
export function updateDocument(id: number, data: UpdateDocumentCommand) {
  return requestClient.put<DocumentDTO>(`/document/${id}`, data);
}

/** 删除文档 */
export function deleteDocument(id: number) {
  return requestClient.delete(`/document/${id}`);
}

/** 上传新版本 */
export function uploadNewVersion(id: number, data: UploadNewVersionCommand) {
  return requestClient.post<DocumentDTO>(`/document/${id}/versions`, data);
}

/** 获取文档所有版本 */
export function getDocumentVersions(id: number) {
  return requestClient.get<DocumentDTO[]>(`/document/${id}/versions`);
}

/** 下载文档 */
export function downloadDocument(id: number) {
  return requestClient.get(`/document/${id}/download`, {
    responseType: 'blob',
  });
}

/** 获取文档预览配置（OnlyOffice 只读模式） */
export function getDocumentPreviewConfig(id: number) {
  return requestClient.get<OnlyOfficeConfig>(`/document/${id}/preview`);
}

/** 获取文档编辑配置（OnlyOffice 编辑模式） */
export function getDocumentEditConfig(id: number) {
  return requestClient.get<OnlyOfficeConfig>(`/document/${id}/edit`);
}

/** 检查文档是否支持在线编辑 */
export function checkDocumentEditSupport(id: number) {
  return requestClient.get<{
    canEdit: boolean;
    canPreview: boolean;
    documentId: number;
    fileName: string;
    fileType: string;
  }>(`/document/${id}/edit-support`);
}

/** 获取文档预览 URL（带签名的临时访问链接） */
export function getDocumentPreviewUrl(id: number) {
  return requestClient.get<{
    documentId: number;
    expires: number;
    fileName: string;
    fileType: string;
    mimeType: string;
    previewUrl: string;
  }>(`/document/${id}/preview-url`);
}

/** 获取文档缩略图 URL */
export function getDocumentThumbnailUrl(id: number) {
  return requestClient.get<{
    documentId: number;
    fileName: string;
    fileType: string;
    hasThumbnail: boolean;
    message?: string;
    thumbnailUrl?: string;
  }>(`/document/${id}/thumbnail`);
}

/** 分享文档 */
export function shareDocument(id: number) {
  return requestClient.post<string>(`/document/${id}/share`);
}

/** 预览文档（获取预览URL并返回） */
export async function previewDocument(id: number): Promise<null | string> {
  try {
    const result = await requestClient.get<{
      documentId: number;
      expires: number;
      fileName: string;
      fileType: string;
      mimeType: string;
      previewUrl: string;
    }>(`/document/${id}/preview-url`);
    return result.previewUrl || null;
  } catch {
    return null;
  }
}

/** OnlyOffice 配置类型 */
export interface OnlyOfficeConfig {
  supported: boolean;
  message?: string;
  documentId?: number;
  documentServerUrl?: string;
  apiJsUrl?: string;
  token?: string; // JWT token for OnlyOffice Document Server authentication
  document?: {
    fileType: string;
    key: string;
    permissions?: {
      comment: boolean;
      download: boolean;
      edit: boolean;
      print: boolean;
      review: boolean;
    };
    title: string;
    url: string;
  };
  documentType?: string;
  editorConfig?: {
    callbackUrl?: string;
    customization?: Record<string, any>;
    lang: string;
    mode: string;
    user?: {
      id: string;
      name: string;
    };
  };
  height?: string;
  width?: string;
  type?: string;
}

/** 创建文件夹 */
export function createFolder(data: {
  matterId: number;
  name: string;
  parentFolder: string;
}) {
  return requestClient.post('/document/folder', data);
}

/** 上传文件 */
export function uploadFile(
  file: File,
  options: {
    description?: string;
    dossierItemId?: number;
    folder?: string;
    matterId?: number;
  } = {},
) {
  const formData = new FormData();
  formData.append('file', file);
  if (options.matterId) formData.append('matterId', String(options.matterId));
  if (options.folder) formData.append('folder', options.folder);
  if (options.description) formData.append('description', options.description);
  if (options.dossierItemId)
    formData.append('dossierItemId', String(options.dossierItemId));

  return requestClient.post<DocumentDTO>('/document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/** 批量上传文件 */
export function uploadFiles(
  files: File[],
  options: {
    description?: string;
    dossierItemId?: number;
    folder?: string;
    matterId?: number;
    sourceType?: string; // 文档来源类型：USER_UPLOADED, SIGNED_VERSION 等
  } = {},
) {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));
  if (options.matterId) formData.append('matterId', String(options.matterId));
  if (options.folder) formData.append('folder', options.folder);
  if (options.description) formData.append('description', options.description);
  if (options.dossierItemId)
    formData.append('dossierItemId', String(options.dossierItemId));
  if (options.sourceType) formData.append('sourceType', options.sourceType);

  return requestClient.post<DocumentDTO[]>('/document/upload/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/** 获取文档访问日志 */
export function getDocumentAccessLogs(
  id: number,
  params?: { actionType?: string; pageNum?: number; pageSize?: number },
) {
  return requestClient.get<PageResult<any>>(`/document/${id}/access-logs`, {
    params,
  });
}

/** 移动文件到指定目录 */
export function moveDocument(id: number, targetDossierItemId: number) {
  return requestClient.put<DocumentDTO>(`/document/${id}/move`, null, {
    params: { targetDossierItemId },
  });
}

/** 重新排序文档 */
export function reorderDocuments(documentIds: number[]) {
  return requestClient.put('/document/reorder', documentIds);
}

/** 批量下载文档（打包为 ZIP） */
export function batchDownloadDocuments(ids: number[]) {
  return requestClient.post('/document/batch-download', ids, {
    responseType: 'blob',
  });
}

/** 批量下载并触发浏览器下载 */
export async function downloadDocumentsAsZip(ids: number[], filename?: string) {
  const response = await batchDownloadDocuments(ids);
  const blob = new Blob([response as any], { type: 'application/zip' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || `documents_${Date.now()}.zip`;
  document.body.append(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

// 导出类型
export type * from './types';
