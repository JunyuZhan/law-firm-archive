export interface AiUsageLogDTO {
  id: number;
  userId: number;
  userName?: string;
  departmentId?: number;
  departmentName?: string;
  integrationId: number;
  integrationCode: string;
  integrationName?: string;
  modelName?: string;
  requestType: string;
  businessType?: string;
  businessId?: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  promptPrice: number;
  completionPrice: number;
  totalCost: number;
  userCost: number;
  chargeRatio: number;
  success: boolean;
  errorMessage?: string;
  durationMs?: number;
  createdAt?: string;
}

export interface AiUsageQuery {
  userId?: number;
  requestType?: string;
  businessType?: string;
  businessId?: number;
  integrationCode?: string;
  modelName?: string;
  success?: boolean;
  createdAtFrom?: string;
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface AiUsageSummaryDTO {
  userId: number;
  month: string;
  userName?: string;
  departmentId?: number;
  departmentName?: string;
  totalCalls: number;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  totalCost: number;
  userCost: number;
  companyCost: number;
  monthlyTokenQuota?: number | null;
  monthlyCostQuota?: number | null;
  tokenUsagePercent?: number | null;
  costUsagePercent?: number | null;
}

export interface AiModelUsageDTO {
  integrationCode?: string;
  integrationName?: string;
  modelName?: string;
  totalCalls?: number;
  totalTokens?: number;
  totalCost?: number;
  userCost?: number;
}

export interface AiDepartmentSummaryDTO {
  departmentId?: number;
  departmentName?: string;
  totalCalls?: number;
  totalTokens?: number;
  totalCost?: number;
  userCost?: number;
}

export interface AiMonthlyBillDTO {
  id: number;
  billYear: number;
  billMonth: number;
  userId: number;
  userName?: string;
  departmentId?: number;
  departmentName?: string;
  totalCalls: number;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  totalCost: number;
  userCost: number;
  chargeRatio: number;
  deductionStatus: string;
  deductionAmount?: number;
  deductedAt?: string;
  deductedBy?: number;
  deductionRemark?: string;
  salaryDeductionId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SalaryDeductionLinkCommand {
  billIds: number[];
  salaryYear: number;
  salaryMonth: number;
  remark?: string;
}
