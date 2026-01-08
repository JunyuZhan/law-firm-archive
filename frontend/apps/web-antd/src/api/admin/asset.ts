/**
 * 资产管理 API
 */
import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 类型定义 ==========
export interface AssetDTO {
  id: number;
  assetNo: string;
  name: string;
  category: string;
  categoryName?: string;
  brand?: string;
  model?: string;
  specification?: string;
  serialNumber?: string;
  purchaseDate?: string;
  purchasePrice?: number;
  supplier?: string;
  warrantyExpireDate?: string;
  usefulLife?: number;
  location?: string;
  currentUserId?: number;
  currentUserName?: string;
  departmentId?: number;
  departmentName?: string;
  status: string;
  statusName?: string;
  imageUrl?: string;
  remarks?: string;
  createdAt?: string;
  updatedAt?: string;
  inWarranty?: boolean;
}

export interface AssetRecordDTO {
  id: number;
  assetId: number;
  assetNo: string;
  assetName: string;
  recordType: string;
  recordTypeName?: string;
  operatorId?: number;
  operatorName?: string;
  fromUserId?: number;
  fromUserName?: string;
  toUserId?: number;
  toUserName?: string;
  operateDate?: string;
  expectedReturnDate?: string;
  actualReturnDate?: string;
  reason?: string;
  maintenanceCost?: number;
  approvalStatus?: string;
  approverId?: number;
  approverName?: string;
  approvalComment?: string;
  remarks?: string;
  createdAt?: string;
}

export interface AssetQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
  status?: string;
  departmentId?: number;
}

export interface CreateAssetCommand {
  name: string;
  category: string;
  brand?: string;
  model?: string;
  specification?: string;
  serialNumber?: string;
  purchaseDate?: string;
  purchasePrice?: number;
  supplier?: string;
  warrantyExpireDate?: string;
  usefulLife?: number;
  location?: string;
  departmentId?: number;
  imageUrl?: string;
  remarks?: string;
}

export interface AssetReceiveCommand {
  assetId: number;
  userId?: number;
  expectedReturnDate?: string;
  reason?: string;
  remarks?: string;
}

// ========== API 函数 ==========

/** 分页查询资产列表 */
export function getAssetList(params: AssetQuery) {
  return requestClient.get<PageResult<AssetDTO>>('/admin/assets', { params });
}

/** 获取资产详情 */
export function getAssetDetail(id: number) {
  return requestClient.get<AssetDTO>(`/admin/assets/${id}`);
}

/** 创建资产 */
export function createAsset(data: CreateAssetCommand) {
  return requestClient.post<AssetDTO>('/admin/assets', data);
}

/** 更新资产 */
export function updateAsset(id: number, data: CreateAssetCommand) {
  return requestClient.put<AssetDTO>(`/admin/assets/${id}`, data);
}

/** 删除资产 */
export function deleteAsset(id: number) {
  return requestClient.delete(`/admin/assets/${id}`);
}

/** 资产领用 */
export function receiveAsset(data: AssetReceiveCommand) {
  return requestClient.post('/admin/assets/receive', data);
}

/** 资产归还 */
export function returnAsset(id: number, remarks?: string) {
  return requestClient.post(`/admin/assets/${id}/return`, null, { params: { remarks } });
}

/** 资产报废 */
export function scrapAsset(id: number, reason: string) {
  return requestClient.post(`/admin/assets/${id}/scrap`, null, { params: { reason } });
}

/** 获取资产操作记录 */
export function getAssetRecords(id: number) {
  return requestClient.get<AssetRecordDTO[]>(`/admin/assets/${id}/records`);
}

/** 获取我领用的资产 */
export function getMyAssets() {
  return requestClient.get<AssetDTO[]>('/admin/assets/my');
}

/** 获取闲置资产 */
export function getIdleAssets() {
  return requestClient.get<AssetDTO[]>('/admin/assets/idle');
}

/** 获取资产统计 */
export function getAssetStatistics() {
  return requestClient.get<Record<string, any>>('/admin/assets/statistics');
}

