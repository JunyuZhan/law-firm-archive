/**
 * 采购管理 API
 */
import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 类型定义 ==========
export interface PurchaseItemDTO {
  id: number;
  requestId: number;
  itemName: string;
  specification?: string;
  unit?: string;
  quantity: number;
  estimatedPrice?: number;
  actualPrice?: number;
  estimatedAmount?: number;
  actualAmount?: number;
  receivedQuantity?: number;
  remarks?: string;
  fullyReceived?: boolean;
}

export interface PurchaseRequestDTO {
  id: number;
  requestNo: string;
  title: string;
  applicantId?: number;
  applicantName?: string;
  departmentId?: number;
  departmentName?: string;
  purchaseType: string;
  purchaseTypeName?: string;
  estimatedAmount?: number;
  actualAmount?: number;
  expectedDate?: string;
  reason?: string;
  status: string;
  statusName?: string;
  approverId?: number;
  approverName?: string;
  approvalDate?: string;
  approvalComment?: string;
  supplierId?: number;
  supplierName?: string;
  remarks?: string;
  createdAt?: string;
  updatedAt?: string;
  items?: PurchaseItemDTO[];
}

export interface PurchaseReceiveDTO {
  id: number;
  receiveNo: string;
  requestId: number;
  requestNo?: string;
  itemId: number;
  itemName?: string;
  quantity: number;
  receiveDate?: string;
  receiverId?: number;
  receiverName?: string;
  location?: string;
  convertToAsset?: boolean;
  assetId?: number;
  assetNo?: string;
  remarks?: string;
  createdAt?: string;
}

export interface PurchaseQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  purchaseType?: string;
  status?: string;
  applicantId?: number;
  departmentId?: number;
}

export interface CreatePurchaseRequestCommand {
  title: string;
  purchaseType?: string;
  expectedDate?: string;
  reason?: string;
  supplierId?: number;
  remarks?: string;
  items: PurchaseItemCommand[];
}

export interface PurchaseItemCommand {
  itemName: string;
  specification?: string;
  unit?: string;
  quantity: number;
  estimatedPrice?: number;
  remarks?: string;
}

export interface PurchaseReceiveCommand {
  requestId: number;
  itemId: number;
  quantity: number;
  receiveDate?: string;
  location?: string;
  convertToAsset?: boolean;
  remarks?: string;
}

// ========== API 函数 ==========

/** 分页查询采购申请 */
export function getPurchaseList(params: PurchaseQuery) {
  return requestClient.get<PageResult<PurchaseRequestDTO>>('/admin/purchases', { params });
}

/** 获取采购申请详情 */
export function getPurchaseDetail(id: number) {
  return requestClient.get<PurchaseRequestDTO>(`/admin/purchases/${id}`);
}

/** 创建采购申请 */
export function createPurchaseRequest(data: CreatePurchaseRequestCommand) {
  return requestClient.post<PurchaseRequestDTO>('/admin/purchases', data);
}

/** 提交采购申请 */
export function submitPurchaseRequest(id: number) {
  return requestClient.post(`/admin/purchases/${id}/submit`);
}

/** 审批采购申请 */
export function approvePurchaseRequest(id: number, approved: boolean, comment?: string) {
  return requestClient.post(`/admin/purchases/${id}/approve`, null, { params: { approved, comment } });
}

/** 开始采购 */
export function startPurchasing(id: number, supplierId: number) {
  return requestClient.post(`/admin/purchases/${id}/start`, null, { params: { supplierId } });
}

/** 采购入库 */
export function receivePurchaseItem(data: PurchaseReceiveCommand) {
  return requestClient.post<PurchaseReceiveDTO>('/admin/purchases/receive', data);
}

/** 取消采购申请 */
export function cancelPurchaseRequest(id: number) {
  return requestClient.post(`/admin/purchases/${id}/cancel`);
}

/** 获取入库记录 */
export function getPurchaseReceiveRecords(id: number) {
  return requestClient.get<PurchaseReceiveDTO[]>(`/admin/purchases/${id}/receives`);
}

/** 获取我的采购申请 */
export function getMyPurchaseRequests() {
  return requestClient.get<PurchaseRequestDTO[]>('/admin/purchases/my');
}

/** 获取待审批的采购申请 */
export function getPendingPurchaseApproval() {
  return requestClient.get<PurchaseRequestDTO[]>('/admin/purchases/pending');
}

/** 获取采购统计 */
export function getPurchaseStatistics() {
  return requestClient.get<Record<string, any>>('/admin/purchases/statistics');
}

