import type { PageResult } from '../matter/types';

/**
 * 档案借阅模块 API
 */
import { requestClient } from '#/api/request';

// ========== 档案借阅类型定义 ==========
export interface ArchiveBorrowDTO {
  id: number;
  borrowNo: string;
  archiveId: number;
  archiveName?: string;
  archiveNo?: string;
  borrowerId?: number;
  borrowerName?: string;
  borrowDate?: string;
  expectedReturnDate?: string;
  actualReturnDate?: string;
  status: string;
  statusName?: string;
  reason?: string;
  createdAt?: string;
}

export interface CreateBorrowCommand {
  archiveId: number;
  expectedReturnDate: string;
  reason?: string;
}

export interface ReturnArchiveCommand {
  borrowId: number;
  returnDate: string;
  condition?: string;
}

// ========== 档案借阅 API ==========

/** 获取借阅记录列表 */
export function getBorrowList(params: {
  archiveId?: number;
  pageNum?: number;
  pageSize?: number;
  status?: string;
}) {
  return requestClient.get<PageResult<ArchiveBorrowDTO>>(
    '/archive/borrow/list',
    { params },
  );
}

/** 创建借阅申请 */
export function createBorrow(data: CreateBorrowCommand) {
  return requestClient.post<ArchiveBorrowDTO>('/archive/borrow', data);
}

/** 审批通过 */
export function approveBorrow(id: number) {
  return requestClient.post(`/archive/borrow/${id}/approve`);
}

/** 审批拒绝 */
export function rejectBorrow(id: number, reason: string) {
  return requestClient.post(`/archive/borrow/${id}/reject`, { reason });
}

/** 确认借出 */
export function confirmBorrow(id: number) {
  return requestClient.post(`/archive/borrow/${id}/confirm`);
}

/** 归还档案 */
export function returnArchive(data: ReturnArchiveCommand) {
  return requestClient.post('/archive/borrow/return', data);
}

/** 获取逾期借阅列表 */
export function getOverdueBorrows() {
  return requestClient.get<ArchiveBorrowDTO[]>('/archive/borrow/overdue');
}
