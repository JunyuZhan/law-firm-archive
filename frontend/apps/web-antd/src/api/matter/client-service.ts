/**
 * 项目客户服务 API
 * 管理项目数据推送到客户服务系统
 */
import { requestClient } from '#/api/request';

// ===================== 类型定义 =====================

/** 推送记录 */
export interface PushRecordDTO {
  id: number;
  matterId: number;
  matterName?: string;
  clientId: number;
  clientName?: string;
  pushType: string;
  scopes: string[];
  dataSnapshot?: Record<string, unknown>;
  externalId?: string;
  externalUrl?: string;
  status: string;
  errorMessage?: string;
  expiresAt?: string;
  createdAt: string;
  creatorId?: number;
  creatorName?: string;
}

/** 推送配置 */
export interface PushConfigDTO {
  matterId: number;
  clientId: number;
  enabled: boolean;
  scopes: string[];
  autoPushOnUpdate: boolean;
  validDays: number;
}

/** 推送请求 */
export interface PushRequest {
  matterId: number;
  clientId?: number;
  scopes: string[];
  validDays?: number;
  documentIds?: number[];
}

/** 授权范围选项 */
export interface ScopeOption {
  value: string;
  label: string;
  description: string;
}

// ===================== API 接口 =====================

/**
 * 推送项目数据到客户服务系统
 */
export function pushMatterData(request: PushRequest) {
  return requestClient.post<PushRecordDTO>(
    '/matter/client-service/push',
    request,
  );
}

/**
 * 获取推送记录列表
 */
export function getPushRecords(params: {
  matterId: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}) {
  return requestClient.get<{ list: PushRecordDTO[]; total: number }>(
    '/matter/client-service/records',
    { params },
  );
}

/**
 * 获取推送记录详情
 */
export function getPushRecordById(id: number) {
  return requestClient.get<PushRecordDTO>(
    `/matter/client-service/records/${id}`,
  );
}

/**
 * 获取最近一次成功推送
 */
export function getLatestPush(matterId: number) {
  return requestClient.get<PushRecordDTO>('/matter/client-service/latest', {
    params: { matterId },
  });
}

/**
 * 获取推送配置
 */
export function getPushConfig(matterId: number, clientId: number) {
  return requestClient.get<PushConfigDTO>('/matter/client-service/config', {
    params: { matterId, clientId },
  });
}

/**
 * 更新推送配置
 */
export function updatePushConfig(
  matterId: number,
  config: Partial<PushConfigDTO>,
) {
  return requestClient.put<PushConfigDTO>(
    '/matter/client-service/config',
    config,
    {
      params: { matterId },
    },
  );
}

/**
 * 获取推送统计
 */
export function getPushStatistics(matterId: number) {
  return requestClient.get<{
    totalPushCount: number;
    lastPushTime?: string;
    lastPushStatus?: string;
  }>('/matter/client-service/statistics', {
    params: { matterId },
  });
}

/**
 * 获取可推送的数据范围选项
 */
export function getScopeOptions() {
  return requestClient.get<ScopeOption[]>('/matter/client-service/scopes');
}

// ===================== 客户上传文件 API =====================

/** 客户上传文件 */
export interface ClientFileDTO {
  id: number;
  matterId: number;
  matterName?: string;
  clientId: number;
  clientName?: string;
  fileName: string;
  originalFileName?: string;
  fileSize?: number;
  fileType?: string;
  fileCategory?: string;
  fileCategoryName?: string;
  description?: string;
  externalFileId: string;
  externalFileUrl: string;
  uploadedBy?: string;
  uploadedAt?: string;
  status: string;
  statusName?: string;
  localDocumentId?: number;
  targetDossierId?: number;
  targetDossierName?: string;
  syncedAt?: string;
  syncedBy?: number;
  syncedByName?: string;
  errorMessage?: string;
  createdAt: string;
}

/** 同步文件请求 */
export interface ClientFileSyncRequest {
  fileId: number;
  targetDossierId: number;
  targetFileName?: string;
  documentCategory?: string;
  remark?: string;
}

/**
 * 获取客户文件列表
 */
export function getClientFiles(params: {
  matterId: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}) {
  return requestClient.get<{ list: ClientFileDTO[]; total: number }>(
    '/matter/client-files',
    { params },
  );
}

/**
 * 获取待同步的客户文件列表
 */
export function getPendingClientFiles(matterId: number) {
  return requestClient.get<ClientFileDTO[]>('/matter/client-files/pending', {
    params: { matterId },
  });
}

/**
 * 获取待同步文件数量
 */
export function countPendingClientFiles(matterId: number) {
  return requestClient.get<{ count: number }>(
    '/matter/client-files/pending/count',
    {
      params: { matterId },
    },
  );
}

/**
 * 同步文件到卷宗
 */
export function syncClientFile(request: ClientFileSyncRequest) {
  return requestClient.post<ClientFileDTO>(
    '/matter/client-files/sync',
    request,
  );
}

/**
 * 批量同步文件
 */
export function batchSyncClientFiles(requests: ClientFileSyncRequest[]) {
  return requestClient.post<ClientFileDTO[]>(
    '/matter/client-files/sync/batch',
    requests,
  );
}

/**
 * 忽略文件（不同步）
 */
export function ignoreClientFile(fileId: number) {
  return requestClient.post<void>(`/matter/client-files/${fileId}/ignore`);
}
