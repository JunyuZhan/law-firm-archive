import type { PageResult } from '../matter/types';

import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface PrepaymentDTO {
  id: number;
  prepaymentNo: string;
  clientId: number;
  clientName?: string;
  contractId?: number;
  contractNo?: string;
  matterId?: number;
  matterNo?: string;
  matterName?: string;
  amount: number;
  usedAmount: number;
  remainingAmount: number;
  currency?: string;
  receiptDate?: string;
  paymentMethod?: string;
  paymentMethodName?: string;
  bankAccount?: string;
  transactionNo?: string;
  status: string;
  statusName?: string;
  confirmerId?: number;
  confirmerName?: string;
  confirmedAt?: string;
  purpose?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
  usages?: PrepaymentUsageDTO[];
}

export interface PrepaymentUsageDTO {
  id: number;
  prepaymentId: number;
  prepaymentNo?: string;
  feeId: number;
  feeNo?: string;
  feeName?: string;
  matterId?: number;
  matterNo?: string;
  matterName?: string;
  amount: number;
  usageTime?: string;
  operatorId?: number;
  operatorName?: string;
  remark?: string;
  createdAt?: string;
}

export interface PrepaymentQueryDTO {
  pageNum?: number;
  pageSize?: number;
  clientId?: number;
  matterId?: number;
  contractId?: number;
  status?: string;
  prepaymentNo?: string;
}

export interface CreatePrepaymentCommand {
  clientId: number;
  contractId?: number;
  matterId?: number;
  amount: number;
  currency?: string;
  receiptDate?: string;
  paymentMethod?: string;
  bankAccount?: string;
  transactionNo?: string;
  purpose?: string;
  remark?: string;
}

export interface UsePrepaymentCommand {
  prepaymentId: number;
  feeId: number;
  amount: number;
  remark?: string;
}

// 状态选项
export const PREPAYMENT_STATUS_OPTIONS = [
  { label: '待确认', value: 'PENDING', color: 'orange' },
  { label: '有效', value: 'ACTIVE', color: 'green' },
  { label: '已用完', value: 'USED', color: 'blue' },
  { label: '已退款', value: 'REFUNDED', color: 'red' },
  { label: '已取消', value: 'CANCELLED', color: 'default' },
];

// 支付方式选项
export const PAYMENT_METHOD_OPTIONS = [
  { label: '银行转账', value: 'BANK' },
  { label: '现金', value: 'CASH' },
  { label: '支票', value: 'CHECK' },
  { label: '其他', value: 'OTHER' },
];

// ==================== API ====================

/** 分页查询预收款 */
export function getPrepaymentList(params: PrepaymentQueryDTO) {
  return requestClient.get<PageResult<PrepaymentDTO>>('/finance/prepayment/list', {
    params,
  });
}

/** 获取预收款详情 */
export function getPrepaymentById(id: number) {
  return requestClient.get<PrepaymentDTO>(`/finance/prepayment/${id}`);
}

/** 创建预收款 */
export function createPrepayment(data: CreatePrepaymentCommand) {
  return requestClient.post<PrepaymentDTO>('/finance/prepayment', data);
}

/** 确认预收款 */
export function confirmPrepayment(id: number) {
  return requestClient.post<PrepaymentDTO>(`/finance/prepayment/${id}/confirm`);
}

/** 使用预收款（核销） */
export function usePrepayment(data: UsePrepaymentCommand) {
  return requestClient.post<PrepaymentUsageDTO>('/finance/prepayment/use', data);
}

/** 查询客户可用预收款 */
export function getAvailablePrepayments(clientId: number) {
  return requestClient.get<PrepaymentDTO[]>(`/finance/prepayment/available/${clientId}`);
}

/** 退款 */
export function refundPrepayment(id: number, remark: string) {
  return requestClient.post<PrepaymentDTO>(`/finance/prepayment/${id}/refund`, null, {
    params: { remark },
  });
}
