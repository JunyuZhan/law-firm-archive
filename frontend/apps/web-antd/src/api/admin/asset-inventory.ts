/**
 * 资产盘点 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface AssetInventoryDTO {
  id: number;
  inventoryNo?: string;
  inventoryDate: string;
  inventoryType: string;
  inventoryTypeName?: string;
  departmentId?: number;
  departmentName?: string;
  location?: string;
  status?: string;
  statusName?: string;
  totalCount?: number;
  actualCount?: number;
  surplusCount?: number;
  shortageCount?: number;
  remark?: string;
  details?: AssetInventoryDetailDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AssetInventoryDetailDTO {
  id: number;
  inventoryId: number;
  assetId: number;
  assetNo?: string;
  assetName?: string;
  expectedStatus?: string;
  actualStatus?: string;
  expectedLocation?: string;
  actualLocation?: string;
  expectedUserId?: number;
  expectedUserName?: string;
  actualUserId?: number;
  actualUserName?: string;
  discrepancyType?: string;
  discrepancyTypeName?: string;
  discrepancyDesc?: string;
  remark?: string;
}

export interface CreateAssetInventoryCommand {
  inventoryDate: string;
  inventoryType: string;
  departmentId?: number;
  location?: string;
  remark?: string;
  assetIds?: number[];
}

export interface UpdateInventoryDetailRequest {
  actualStatus?: string;
  actualLocation?: string;
  actualUserId?: number;
  discrepancyDesc?: string;
}

// ========== API 函数 ==========

/** 创建资产盘点 */
export function createAssetInventory(data: CreateAssetInventoryCommand) {
  return requestClient.post<AssetInventoryDTO>('/admin/asset-inventories', data);
}

/** 更新盘点明细 */
export function updateInventoryDetail(detailId: number, data: UpdateInventoryDetailRequest) {
  return requestClient.put(`/admin/asset-inventories/details/${detailId}`, data);
}

/** 完成盘点 */
export function completeAssetInventory(id: number) {
  return requestClient.post<AssetInventoryDTO>(`/admin/asset-inventories/${id}/complete`);
}

/** 获取盘点详情 */
export function getAssetInventoryDetail(id: number) {
  return requestClient.get<AssetInventoryDTO>(`/admin/asset-inventories/${id}`);
}

/** 查询进行中的盘点 */
export function getInProgressInventories() {
  return requestClient.get<AssetInventoryDTO[]>('/admin/asset-inventories/in-progress');
}

