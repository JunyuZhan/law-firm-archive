import type { PageResult } from '../matter/types';

/**
 * 劳动合同管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface LaborContractDTO {
  id: number;
  employeeId: number;
  userId?: number;
  employeeName?: string;
  contractNo?: string;
  contractType: string;
  contractTypeName?: string;
  startDate: string;
  endDate?: string;
  probationMonths?: number;
  probationEndDate?: string;
  baseSalary?: number;
  performanceBonus?: number;
  otherAllowance?: number;
  status?: string;
  statusName?: string;
  signDate?: string;
  expireDate?: string;
  renewCount?: number;
  contractFileUrl?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LaborContractQuery {
  pageNum?: number;
  pageSize?: number;
  employeeId?: number;
  contractNo?: string;
  status?: string;
}

export interface CreateLaborContractCommand {
  employeeId: number;
  contractNo?: string;
  contractType: string;
  startDate: string;
  endDate?: string;
  probationMonths?: number;
  probationEndDate?: string;
  baseSalary?: number;
  performanceBonus?: number;
  otherAllowance?: number;
  signDate?: string;
  contractFileUrl?: string;
  remark?: string;
}

export interface UpdateLaborContractCommand {
  contractNo?: string;
  contractType?: string;
  startDate?: string;
  endDate?: string;
  probationMonths?: number;
  probationEndDate?: string;
  baseSalary?: number;
  performanceBonus?: number;
  otherAllowance?: number;
  status?: string;
  signDate?: string;
  expireDate?: string;
  contractFileUrl?: string;
  remark?: string;
}

// ========== API 函数 ==========

/** 分页查询劳动合同 */
export function getLaborContractList(params: LaborContractQuery) {
  return requestClient.get<PageResult<LaborContractDTO>>('/hr/contract', {
    params,
  });
}

/** 根据ID查询劳动合同 */
export function getLaborContractDetail(id: number) {
  return requestClient.get<LaborContractDTO>(`/hr/contract/${id}`);
}

/** 根据员工ID查询所有合同 */
export function getLaborContractsByEmployeeId(employeeId: number) {
  return requestClient.get<LaborContractDTO[]>(
    `/hr/contract/employee/${employeeId}`,
  );
}

/** 创建劳动合同 */
export function createLaborContract(data: CreateLaborContractCommand) {
  return requestClient.post<LaborContractDTO>('/hr/contract', data);
}

/** 更新劳动合同 */
export function updateLaborContract(
  id: number,
  data: UpdateLaborContractCommand,
) {
  return requestClient.put<LaborContractDTO>(`/hr/contract/${id}`, data);
}

/** 删除劳动合同 */
export function deleteLaborContract(id: number) {
  return requestClient.delete(`/hr/contract/${id}`);
}

/** 续签合同 */
export function renewLaborContract(
  id: number,
  newStartDate: string,
  newEndDate: string,
) {
  return requestClient.post<LaborContractDTO>(
    `/hr/contract/${id}/renew`,
    null,
    {
      params: { newStartDate, newEndDate },
    },
  );
}
