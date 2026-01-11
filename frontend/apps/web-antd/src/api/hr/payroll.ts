import type { PageResult } from '../matter/types';

/**
 * 工资管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========

export interface PayrollSheetDTO {
  id: number;
  payrollNo?: string;
  payrollYear?: number;
  payrollMonth?: number;
  status?: string;
  statusName?: string;
  totalEmployees?: number;
  totalGrossAmount?: number | string;
  totalDeductionAmount?: number | string;
  totalNetAmount?: number | string;
  confirmedCount?: number;
  submittedAt?: string;
  submittedBy?: number;
  submittedByName?: string;
  financeConfirmedAt?: string;
  financeConfirmedBy?: number;
  financeConfirmedByName?: string;
  issuedAt?: string;
  issuedBy?: number;
  issuedByName?: string;
  paymentMethod?: string;
  paymentMethodName?: string;
  paymentVoucherUrl?: string;
  remark?: string;
  autoConfirmDeadline?: string;
  items?: PayrollItemDTO[];
  rejectedItems?: PayrollItemDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PayrollItemDTO {
  id: number;
  payrollSheetId?: number;
  employeeId?: number;
  userId?: number;
  employeeNo?: string;
  employeeName?: string;
  grossAmount?: number | string;
  deductionAmount?: number | string;
  netAmount?: number | string;
  confirmStatus?: string;
  confirmStatusName?: string;
  confirmedAt?: string;
  confirmComment?: string;
  confirmDeadline?: string; // 确认截止时间（ISO格式）
  incomes?: PayrollIncomeDTO[];
  deductions?: PayrollDeductionDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PayrollIncomeDTO {
  id: number;
  payrollItemId?: number;
  incomeType?: string;
  incomeTypeName?: string;
  amount?: number | string;
  remark?: string;
  sourceType?: string;
  sourceTypeName?: string;
  sourceId?: number;
}

export interface PayrollDeductionDTO {
  id: number;
  payrollItemId?: number;
  deductionType?: string;
  deductionTypeName?: string;
  amount?: number | string;
  remark?: string;
  sourceType?: string;
  sourceTypeName?: string;
}

export interface PayrollSheetQuery {
  pageNum?: number;
  pageSize?: number;
  payrollYear?: number;
  payrollMonth?: number;
  status?: string;
  payrollNo?: string;
}

export interface CreatePayrollSheetCommand {
  payrollYear: number;
  payrollMonth: number;
  generateType?: 'AUTO' | 'MANUAL';
  autoConfirmDeadline?: string; // 自动确认截止时间（ISO格式）
}

export interface UpdatePayrollItemCommand {
  payrollItemId: number;
  grossAmount?: number; // 应发工资（可手动设置，考虑预支、欠款等因素）
  confirmDeadline?: string; // 确认截止时间（ISO格式）
  incomes?: PayrollIncomeItem[];
  deductions?: PayrollDeductionItem[];
  remark?: string;
}

export interface PayrollIncomeItem {
  id?: number;
  incomeType: string;
  amount: number;
  remark?: string;
  sourceType?: string;
  sourceId?: number;
}

export interface PayrollDeductionItem {
  id?: number;
  deductionType: string;
  amount: number;
  remark?: string;
  sourceType?: string;
}

export interface ConfirmPayrollCommand {
  payrollItemId: number;
  confirmStatus: 'CONFIRMED' | 'REJECTED';
  confirmComment?: string;
}

export interface AddPayrollItemCommand {
  employeeId: number;
  autoLoad?: boolean; // 是否自动载入基本工资和提成数据
}

export interface IssuePayrollCommand {
  payrollSheetId: number;
  paymentMethod: 'BANK_TRANSFER' | 'CASH' | 'OTHER';
  paymentVoucherUrl?: string;
  remark?: string;
}

// ========== API 方法 ==========

/**
 * 创建工资表
 */
export function createPayrollSheet(command: CreatePayrollSheetCommand) {
  return requestClient.post<PayrollSheetDTO>('/hr/payroll', command);
}

/**
 * 分页查询工资表
 */
export function getPayrollSheetList(query: PayrollSheetQuery) {
  return requestClient.get<PageResult<PayrollSheetDTO>>('/hr/payroll', {
    params: query,
  });
}

/**
 * 查询工资表详情
 */
export function getPayrollSheetDetail(id: number) {
  return requestClient.get<PayrollSheetDTO>(`/hr/payroll/${id}`);
}

/**
 * 查询工资表的所有员工工资明细列表
 */
export function getPayrollItemsBySheetId(sheetId: number) {
  return requestClient.get<PayrollItemDTO[]>(`/hr/payroll/${sheetId}/items`);
}

/**
 * 根据年月查询员工工资明细列表
 */
export function getPayrollItemsByYearMonth(year: number, month: number) {
  return requestClient.get<PayrollItemDTO[]>(`/hr/payroll/items`, {
    params: { year, month },
  });
}

/**
 * 为工资表添加员工工资明细
 */
export function addPayrollItem(
  sheetId: number,
  command: AddPayrollItemCommand,
) {
  return requestClient.post<PayrollItemDTO>(
    `/hr/payroll/${sheetId}/add-item`,
    command,
  );
}

/**
 * 更新工资明细
 */
export function updatePayrollItem(
  itemId: number,
  command: UpdatePayrollItemCommand,
) {
  return requestClient.put<PayrollItemDTO>(
    `/hr/payroll/item/${itemId}`,
    command,
  );
}

/**
 * 根据年月和员工ID更新或创建工资明细（用于没有工资表时也能编辑）
 */
export function updatePayrollItemByEmployee(
  year: number,
  month: number,
  employeeId: number,
  command: UpdatePayrollItemCommand,
) {
  return requestClient.put<PayrollItemDTO>(
    `/hr/payroll/item/by-employee`,
    command,
    {
      params: { year, month, employeeId },
    },
  );
}

/**
 * 提交工资表
 */
export function submitPayrollSheet(id: number) {
  return requestClient.post<void>(`/hr/payroll/${id}/submit`);
}

/**
 * 员工确认工资表
 */
export function confirmPayrollItem(command: ConfirmPayrollCommand) {
  return requestClient.post<void>('/hr/payroll/item/confirm', command);
}

/**
 * 财务确认工资表
 */
export function financeConfirmPayrollSheet(id: number) {
  return requestClient.post<void>(`/hr/payroll/${id}/finance-confirm`);
}

/**
 * 发放工资
 */
export function issuePayroll(id: number, command: IssuePayrollCommand) {
  return requestClient.post<void>(`/hr/payroll/${id}/issue`, command);
}

/**
 * 查询我的工资表
 */
export function getMyPayrollSheets(year?: number, month?: number) {
  return requestClient.get<PayrollSheetDTO[]>('/hr/payroll/my', {
    params: { year, month },
  });
}

/**
 * 查询我的工资明细
 */
export function getMyPayrollSheetDetail(id: number) {
  return requestClient.get<PayrollSheetDTO>(`/hr/payroll/my/${id}`);
}

/**
 * 导出工资表为Excel（仅已审批通过的工资表可以导出）
 */
export function exportPayrollSheet(id: number) {
  return requestClient.get<Blob>(`/hr/payroll/${id}/export`, {
    responseType: 'blob',
  });
}
