import type { PageResult } from '../matter/types';
import type {
  ApproveMigrateRequest,
  ArchiveCheckResult,
  ArchiveDataSnapshot,
  ArchiveDataSource,
  ArchiveDTO,
  ArchiveQuery,
  CreateArchiveCommand,
  MigrateRequest,
  StoreArchiveCommand,
} from './types';

/**
 * 档案管理模块 API
 */
import { requestClient } from '#/api/request';

// 重新导出类型
export type {
  ApproveMigrateRequest,
  ArchiveCheckResult,
  ArchiveDataSnapshot,
  ArchiveDataSource,
  ArchiveDTO,
  ArchiveQuery,
  CreateArchiveCommand,
  MigrateRequest,
  StoreArchiveCommand,
};

// ========== 档案管理 API ==========

/** 获取档案列表 */
export function getArchiveList(params: ArchiveQuery) {
  return requestClient.get<PageResult<ArchiveDTO>>('/archive/list', { params });
}

/** 获取档案详情 */
export function getArchiveDetail(id: number) {
  return requestClient.get<ArchiveDTO>(`/archive/${id}`);
}

/** 获取待归档案件列表 */
export function getPendingMatters() {
  return requestClient.get<any[]>('/archive/pending-matters');
}

/** 归档预检查 */
export function checkArchiveRequirements(matterId: number) {
  return requestClient.get<ArchiveCheckResult>(`/archive/check/${matterId}`);
}

/** 预览归档数据 */
export function previewArchiveData(matterId: number) {
  return requestClient.get<ArchiveDataSnapshot>(`/archive/preview/${matterId}`);
}

/** 获取可用的归档数据源配置 */
export function getArchiveDataSources() {
  return requestClient.get<ArchiveDataSource[]>('/archive/data-sources');
}

/** 创建档案 */
export function createArchive(data: CreateArchiveCommand) {
  return requestClient.post<ArchiveDTO>('/archive', data);
}

/** 提交入库审批 */
export function submitStoreApproval(archiveId: number) {
  return requestClient.post(`/archive/${archiveId}/submit-store`);
}

/** 审批入库 */
export function approveStore(
  archiveId: number,
  approved: boolean,
  comment?: string,
) {
  return requestClient.post(`/archive/${archiveId}/approve-store`, {
    approved,
    comment,
  });
}

/** 档案入库（实际入库操作） */
export function storeArchive(data: StoreArchiveCommand) {
  return requestClient.post('/archive/store', data);
}

/** 申请迁移档案 */
export function applyMigrateArchive(id: number, data: MigrateRequest) {
  return requestClient.post(`/archive/${id}/apply-migrate`, data);
}

/** 审批迁移档案 */
export function approveMigrateArchive(id: number, data: ApproveMigrateRequest) {
  return requestClient.post(`/archive/${id}/approve-migrate`, data);
}

/** 申请销毁档案（兼容旧接口，内部调用迁移） */
export function applyDestroyArchive(id: number, reason: string) {
  return requestClient.post(`/archive/${id}/apply-destroy`, { reason });
}

/** 审批销毁档案（兼容旧接口，内部调用迁移审批） */
export function approveDestroyArchive(
  id: number,
  approved: boolean,
  comment?: string,
) {
  return requestClient.post(`/archive/${id}/approve-destroy`, {
    approved,
    comment,
  });
}

/** 获取即将到期的档案 */
export function getExpiringArchives(days: number = 90) {
  return requestClient.get<ArchiveDTO[]>(`/archive/expiring`, {
    params: { days },
  });
}

/** 按库位查看档案 */
export function getArchivesByLocation(locationId: number) {
  return requestClient.get<ArchiveDTO[]>(`/archive/location/${locationId}`);
}

/** 设置档案保管期限 */
export function setRetentionPeriod(id: number, retentionPeriod: string) {
  return requestClient.put<ArchiveDTO>(`/archive/${id}/retention-period`, {
    retentionPeriod,
  });
}

/** 销毁登记 */
export function registerDestroyArchive(
  id: number,
  data: { destroyLocation: string; destroyMethod: string; witness: string },
) {
  return requestClient.post<ArchiveDTO>(
    `/archive/${id}/register-destroy`,
    data,
  );
}

/** 下载卷宗封面 */
export function downloadArchiveCover(id: number) {
  return requestClient.get(`/archive/${id}/cover`, {
    responseType: 'blob',
  });
}

/** 重新生成卷宗封面 */
export function regenerateArchiveCover(id: number) {
  return requestClient.post<ArchiveDTO>(`/archive/${id}/regenerate-cover`);
}

// 导出类型
export type * from './types';
