import type { PageResult } from '#/api/client/types';
import { requestClient } from '#/api/request';

import type {
  AiDepartmentSummaryDTO,
  AiModelUsageDTO,
  AiMonthlyBillDTO,
  AiUsageLogDTO,
  AiUsageQuery,
  AiUsageSummaryDTO,
  SalaryDeductionLinkCommand,
} from './types';

export function getMyUsageLogs(params: AiUsageQuery) {
  return requestClient.get<PageResult<AiUsageLogDTO>>('/ai/usage/my', {
    params,
  });
}

export function getMyUsageSummary(month?: string) {
  const params = month ? { month } : {};
  return requestClient.get<AiUsageSummaryDTO>('/ai/usage/my/summary', {
    params,
  });
}

export function getMyUsageByModel(month?: string) {
  const params = month ? { month } : {};
  return requestClient.get<AiModelUsageDTO[]>('/ai/usage/my/by-model', {
    params,
  });
}

export function getAllUsersSummary(month?: string) {
  const params = month ? { month } : {};
  return requestClient.get<AiUsageSummaryDTO[]>('/ai/usage/statistics', {
    params,
  });
}

export function getDepartmentSummary(month?: string) {
  const params = month ? { month } : {};
  return requestClient.get<AiDepartmentSummaryDTO[]>(
    '/ai/usage/statistics/department',
    {
      params,
    },
  );
}

export function getMonthlyBills(year: number, month: number) {
  return requestClient.get<AiMonthlyBillDTO[]>('/ai/usage/billing', {
    params: { year, month },
  });
}

export function generateMonthlyBills(year: number, month: number) {
  return requestClient.post<number>('/ai/usage/billing/generate', undefined, {
    params: { year, month },
  });
}

export function linkToSalaryDeduction(data: SalaryDeductionLinkCommand) {
  return requestClient.post<void>('/ai/usage/billing/link-salary', data);
}

export function markBillDeducted(id: number, remark: string) {
  return requestClient.post<void>(`/ai/usage/billing/${id}/deduct`, undefined, {
    params: { remark },
  });
}

export function waiveBill(id: number, reason: string) {
  return requestClient.post<void>(`/ai/usage/billing/${id}/waive`, undefined, {
    params: { reason },
  });
}
