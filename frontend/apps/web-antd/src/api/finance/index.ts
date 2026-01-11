import type {
  CommissionDTO,
  CommissionQuery,
  ContractDTO,
  ContractParticipantDTO,
  ContractPaymentScheduleDTO,
  ContractQuery,
  ContractStatistics,
  CreateContractCommand,
  CreateExpenseCommand,
  CreateFeeCommand,
  CreateInvoiceCommand,
  CreateParticipantCommand,
  CreatePaymentCommand,
  CreatePaymentScheduleCommand,
  ExpenseDTO,
  ExpenseQuery,
  FeeDTO,
  FeeQuery,
  InvoiceDTO,
  InvoiceQuery,
  InvoiceStatisticsDTO,
  PageResult,
  PaymentDTO,
  UpdateContractCommand,
  UpdateParticipantCommand,
  UpdatePaymentScheduleCommand,
} from './types';

/**
 * 财务管理模块 API
 */
import { requestClient } from '#/api/request';

// ========== 合同管理 API ==========

/** 获取合同列表 */
export function getContractList(params: ContractQuery) {
  return requestClient.get<PageResult<ContractDTO>>('/finance/contract/list', {
    params,
  });
}

/** 获取我的合同 */
export function getMyContracts(params: ContractQuery) {
  return requestClient.get<PageResult<ContractDTO>>('/matter/contract/my', {
    params,
  });
}

/** 获取合同详情 */
export function getContractDetail(id: number) {
  return requestClient.get<ContractDTO>(`/finance/contract/${id}`);
}

/** 创建合同 */
export function createContract(data: CreateContractCommand) {
  return requestClient.post<ContractDTO>('/finance/contract', data);
}

/** 更新合同 */
export function updateContract(data: UpdateContractCommand) {
  return requestClient.put<ContractDTO>('/finance/contract', data);
}

/** 删除合同 */
export function deleteContract(id: number) {
  return requestClient.delete(`/finance/contract/${id}`);
}

/** 提交审批 */
export function submitContract(id: number) {
  return requestClient.post(`/finance/contract/${id}/submit`);
}

/** 审批通过 */
export function approveContract(id: number) {
  return requestClient.post(`/finance/contract/${id}/approve`);
}

/** 审批拒绝 */
export function rejectContract(id: number, reason: string) {
  return requestClient.post(`/finance/contract/${id}/reject`, { reason });
}

/** 终止合同 */
export function terminateContract(id: number, reason: string) {
  return requestClient.post(`/finance/contract/${id}/terminate`, { reason });
}

/** 完成合同 */
export function completeContract(id: number) {
  return requestClient.post(`/finance/contract/${id}/complete`);
}

// ========== 合同付款计划 API (matter模块) ==========

/** 获取合同付款计划列表 */
export function getContractPaymentSchedules(contractId: number) {
  return requestClient.get<ContractPaymentScheduleDTO[]>(
    `/matter/contract/${contractId}/payment-schedules`,
  );
}

/** 创建付款计划 */
export function createPaymentSchedule(
  contractId: number,
  data: CreatePaymentScheduleCommand,
) {
  return requestClient.post<ContractPaymentScheduleDTO>(
    `/matter/contract/${contractId}/payment-schedules`,
    data,
  );
}

/** 更新付款计划 */
export function updatePaymentSchedule(
  contractId: number,
  scheduleId: number,
  data: UpdatePaymentScheduleCommand,
) {
  return requestClient.put<ContractPaymentScheduleDTO>(
    `/matter/contract/${contractId}/payment-schedules/${scheduleId}`,
    data,
  );
}

/** 删除付款计划 */
export function deletePaymentSchedule(contractId: number, scheduleId: number) {
  return requestClient.delete(
    `/matter/contract/${contractId}/payment-schedules/${scheduleId}`,
  );
}

// ========== 合同参与人 API (matter模块) ==========

/** 获取合同参与人列表 */
export function getContractParticipants(contractId: number) {
  return requestClient.get<ContractParticipantDTO[]>(
    `/matter/contract/${contractId}/participants`,
  );
}

/** 创建参与人 */
export function createContractParticipant(
  contractId: number,
  data: CreateParticipantCommand,
) {
  return requestClient.post<ContractParticipantDTO>(
    `/matter/contract/${contractId}/participants`,
    data,
  );
}

/** 更新参与人 */
export function updateContractParticipant(
  contractId: number,
  participantId: number,
  data: UpdateParticipantCommand,
) {
  return requestClient.put<ContractParticipantDTO>(
    `/matter/contract/${contractId}/participants/${participantId}`,
    data,
  );
}

/** 删除参与人 */
export function deleteContractParticipant(
  contractId: number,
  participantId: number,
) {
  return requestClient.delete(
    `/matter/contract/${contractId}/participants/${participantId}`,
  );
}

// ========== 合同统计 API (matter模块) ==========

/** 获取合同统计 */
export function getContractStatistics(params?: {
  departmentId?: number;
  endDate?: string;
  startDate?: string;
}) {
  return requestClient.get<ContractStatistics>('/matter/contract/statistics', {
    params,
  });
}

// ========== 合同模板 API (matter模块) ==========

/** 基于模板创建合同 */
export function createContractFromTemplate(
  templateId: number,
  data: CreateContractCommand,
) {
  return requestClient.post<ContractDTO>(
    `/matter/contract/from-template/${templateId}`,
    data,
  );
}

/** 预览模板内容 */
export function previewTemplateContent(
  templateId: number,
  data: CreateContractCommand,
) {
  return requestClient.post<string>(
    `/matter/contract/template/${templateId}/preview`,
    data,
  );
}

// ========== 收费管理 API ==========

/** 获取收费列表 */
export function getFeeList(params: FeeQuery) {
  return requestClient.get<PageResult<FeeDTO>>('/finance/fee/list', { params });
}

/** 获取收费详情 */
export function getFeeDetail(id: number) {
  return requestClient.get<FeeDTO>(`/finance/fee/${id}`);
}

/** 创建收费 */
export function createFee(data: CreateFeeCommand) {
  return requestClient.post<FeeDTO>('/finance/fee', data);
}

/** 更新收费 */
export function updateFee(id: number, data: Partial<CreateFeeCommand>) {
  return requestClient.put<FeeDTO>(`/finance/fee/${id}`, data);
}

/** 删除收费 */
export function deleteFee(id: number) {
  return requestClient.delete(`/finance/fee/${id}`);
}

// ========== 收款管理 API ==========
// 注意：收款接口在FeeController中，路径为 /finance/fee/payment

/** 创建收款 */
export function createPayment(data: CreatePaymentCommand) {
  return requestClient.post<PaymentDTO>('/finance/fee/payment', data);
}

/** 确认收款 */
export function confirmPayment(id: number) {
  return requestClient.post(`/finance/fee/payment/${id}/confirm`);
}

/** 取消收款 */
export function cancelPayment(id: number) {
  return requestClient.post(`/finance/fee/payment/${id}/cancel`);
}

// ========== 发票管理 API ==========

/** 获取发票列表 */
export function getInvoiceList(params: InvoiceQuery) {
  return requestClient.get<PageResult<InvoiceDTO>>('/finance/invoice/list', {
    params,
  });
}

/** 获取发票详情 */
export function getInvoiceDetail(id: number) {
  return requestClient.get<InvoiceDTO>(`/finance/invoice/${id}`);
}

/** 申请开票 */
export function applyInvoice(data: CreateInvoiceCommand) {
  return requestClient.post<InvoiceDTO>('/finance/invoice/apply', data);
}

/** 开具发票 */
export function issueInvoice(id: number, invoiceNo: string) {
  return requestClient.post(`/finance/invoice/${id}/issue`, { invoiceNo });
}

/** 作废发票 */
export function cancelInvoice(id: number, reason: string) {
  return requestClient.post(`/finance/invoice/${id}/cancel`, { reason });
}

/** 获取发票统计 */
export function getInvoiceStatistics() {
  return requestClient.get<InvoiceStatisticsDTO>('/finance/invoice/statistics');
}

// ========== 提成管理 API ==========
// 注意：提成接口路径为 /finance/commission（前端API基础路径已包含/api）

/** 获取提成列表 */
export function getCommissionList(params: CommissionQuery) {
  return requestClient.get<PageResult<CommissionDTO>>('/finance/commission', {
    params,
  });
}

/** 获取提成详情 */
export function getCommissionDetail(id: number) {
  return requestClient.get<CommissionDTO>(`/finance/commission/detail/${id}`);
}

/** 获取待计算提成的收款记录列表 */
export function getPendingCommissionPayments() {
  return requestClient.get<PaymentDTO[]>(
    '/finance/commission/pending-payments',
  );
}

/** 计算提成 */
export function calculateCommission(paymentId: number) {
  return requestClient.post<CommissionDTO[]>(
    `/finance/commission/calculate/${paymentId}`,
  );
}

/** 手动计算提成 */
export function manualCalculateCommission(data: {
  participants: Array<{
    commissionAmount?: number;
    commissionRate?: number;
    participantId: number;
    remark?: string;
    userId: number;
  }>;
  paymentId: number;
}) {
  return requestClient.post<CommissionDTO[]>(
    `/finance/commission/manual-calculate`,
    data,
  );
}

/** 审批提成 */
export function approveCommission(
  id: number,
  approved: boolean,
  comment?: string,
) {
  return requestClient.post<CommissionDTO>(
    `/finance/commission/${id}/approve`,
    null,
    {
      params: { approved, comment },
    },
  );
}

/** 批量审批提成 */
export function batchApproveCommission(
  ids: number[],
  approved: boolean,
  comment?: string,
) {
  return requestClient.post('/finance/commission/batch-approve', ids, {
    params: { approved, comment },
  });
}

/** 确认提成已发放 */
export function issueCommission(id: number) {
  return requestClient.post<CommissionDTO>(`/finance/commission/${id}/issue`);
}

/** 批量确认提成发放 */
export function batchIssueCommission(ids: number[]) {
  return requestClient.post('/finance/commission/batch-issue', ids);
}

/** 查询用户提成总额 */
export function getUserCommissionTotal(userId: number) {
  return requestClient.get<number>(`/finance/commission/users/${userId}/total`);
}

/** 获取全所提成汇总 */
export function getCommissionSummary(startDate?: string, endDate?: string) {
  return requestClient.get('/finance/commission/summary', {
    params: { startDate, endDate },
  });
}

/** 生成提成报表 */
export function getCommissionReport(
  startDate?: string,
  endDate?: string,
  userId?: number,
) {
  return requestClient.get('/finance/commission/report', {
    params: { startDate, endDate, userId },
  });
}

// ========== 费用报销 API ==========
// 注意：费用报销接口路径为 /finance/expense（baseURL已包含/api）

/** 获取费用报销列表 */
export function getExpenseList(params: ExpenseQuery) {
  return requestClient.get<PageResult<ExpenseDTO>>('/finance/expense', {
    params,
  });
}

/** 获取费用报销详情 */
export function getExpenseDetail(id: number) {
  return requestClient.get<ExpenseDTO>(`/finance/expense/${id}`);
}

/** 创建费用报销申请 */
export function createExpense(data: CreateExpenseCommand) {
  return requestClient.post<ExpenseDTO>('/finance/expense', data);
}

/** 审批费用报销 */
export function approveExpense(
  id: number,
  data: { approved: boolean; comment?: string },
) {
  return requestClient.post(`/finance/expense/${id}/approve`, data);
}

/** 确认支付 */
export function payExpense(id: number, paymentMethod: string) {
  return requestClient.post(`/finance/expense/${id}/pay`, null, {
    params: { paymentMethod },
  });
}

/** 成本归集 */
export function allocateCost(data: { expenseId: number; matterId: number }) {
  return requestClient.post('/finance/expense/allocate-cost', data);
}

/** 查询项目成本归集记录 */
export function getMatterCosts(matterId: number) {
  return requestClient.get(`/finance/expense/matters/${matterId}/costs`);
}

/** 获取项目总成本 */
export function getMatterTotalCost(matterId: number) {
  return requestClient.get<number>(
    `/finance/expense/matters/${matterId}/total-cost`,
  );
}

/** 成本分摊 */
export function splitCost(data: {
  expenseId: number;
  matterIds: number[];
  ratios?: Record<number, number>;
  splitMethod: 'EQUAL' | 'MANUAL' | 'RATIO';
}) {
  return requestClient.post('/finance/expense/split-cost', data);
}

/** 查询项目成本分摊记录 */
export function getMatterSplits(matterId: number) {
  return requestClient.get(`/finance/expense/matters/${matterId}/splits`);
}

/** 删除费用报销 */
export function deleteExpense(id: number) {
  return requestClient.delete(`/finance/expense/${id}`);
}

// 导出提成规则管理API
export * from './commission-rule';

// 导出类型
export type * from './types';
