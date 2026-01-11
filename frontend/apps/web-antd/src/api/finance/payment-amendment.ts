/**
 * 收款变更申请 API
 *
 * Requirements: 3.5
 */
import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface PaymentAmendmentDTO {
  id: number;
  paymentId: number;
  paymentNo?: string;
  originalAmount: number;
  newAmount: number;
  amountDiff: number;
  reason: string;
  requestedBy: number;
  requestedByName?: string;
  requestedAt: string;
  approvedBy?: number;
  approvedByName?: string;
  approvedAt?: string;
  status: string;
  statusName: string;
  rejectReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentAmendmentCommand {
  paymentId: number;
  newAmount: number;
  reason: string;
}

// 本地分页结果类型（不导出，避免与其他模块冲突）
interface AmendmentPageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// ==================== API 函数 ====================

/**
 * 申请修改已锁定的收款记录
 */
export function createPaymentAmendment(data: CreatePaymentAmendmentCommand) {
  return requestClient.post<PaymentAmendmentDTO>(
    '/finance/payment-amendment',
    data,
  );
}

/**
 * 审批通过变更申请
 */
export function approvePaymentAmendment(id: number, comment?: string) {
  return requestClient.post<PaymentAmendmentDTO>(
    `/finance/payment-amendment/${id}/approve`,
    null,
    {
      params: { comment },
    },
  );
}

/**
 * 拒绝变更申请
 */
export function rejectPaymentAmendment(id: number, rejectReason: string) {
  return requestClient.post<PaymentAmendmentDTO>(
    `/finance/payment-amendment/${id}/reject`,
    null,
    {
      params: { rejectReason },
    },
  );
}

/**
 * 分页查询变更申请列表
 */
export function getPaymentAmendmentList(params: {
  pageNum?: number;
  pageSize?: number;
  status?: string;
}) {
  return requestClient.get<AmendmentPageResult<PaymentAmendmentDTO>>(
    '/finance/payment-amendment/list',
    { params },
  );
}

/**
 * 获取变更申请详情
 */
export function getPaymentAmendmentDetail(id: number) {
  return requestClient.get<PaymentAmendmentDTO>(
    `/finance/payment-amendment/${id}`,
  );
}

/**
 * 查询收款记录的变更历史
 */
export function getPaymentAmendmentHistory(paymentId: number) {
  return requestClient.get<PaymentAmendmentDTO[]>(
    `/finance/payment-amendment/payment/${paymentId}/history`,
  );
}
