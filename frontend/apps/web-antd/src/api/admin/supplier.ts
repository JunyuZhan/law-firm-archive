import type { PageResult } from '../matter/types';

/**
 * 供应商管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface SupplierDTO {
  id: number;
  supplierNo?: string;
  name: string;
  supplierType?: string;
  supplierTypeName?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
  address?: string;
  creditCode?: string;
  bankName?: string;
  bankAccount?: string;
  supplyScope?: string;
  rating?: string;
  ratingName?: string;
  status?: string;
  statusName?: string;
  remarks?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SupplierQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  supplierType?: string;
  status?: string;
  rating?: string;
}

export interface CreateSupplierCommand {
  name: string;
  supplierType?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
  address?: string;
  creditCode?: string;
  bankName?: string;
  bankAccount?: string;
  supplyScope?: string;
  rating?: string;
  remarks?: string;
}

// ========== API 函数 ==========

/** 分页查询供应商列表 */
export function getSupplierList(params: SupplierQuery) {
  return requestClient.get<PageResult<SupplierDTO>>('/admin/suppliers', {
    params,
  });
}

/** 获取供应商详情 */
export function getSupplierDetail(id: number) {
  return requestClient.get<SupplierDTO>(`/admin/suppliers/${id}`);
}

/** 创建供应商 */
export function createSupplier(data: CreateSupplierCommand) {
  return requestClient.post<SupplierDTO>('/admin/suppliers', data);
}

/** 更新供应商 */
export function updateSupplier(id: number, data: CreateSupplierCommand) {
  return requestClient.put<SupplierDTO>(`/admin/suppliers/${id}`, data);
}

/** 删除供应商 */
export function deleteSupplier(id: number) {
  return requestClient.delete(`/admin/suppliers/${id}`);
}

/** 启用/停用供应商 */
export function changeSupplierStatus(id: number, status: string) {
  return requestClient.put(`/admin/suppliers/${id}/status`, null, {
    params: { status },
  });
}

/** 获取供应商统计 */
export function getSupplierStatistics() {
  return requestClient.get<Record<string, any>>('/admin/suppliers/statistics');
}
