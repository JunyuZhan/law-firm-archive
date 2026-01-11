import type { PageResult } from '../matter/types';

/**
 * 员工管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface EmployeeDTO {
  id: number;
  userId?: number;
  employeeNo?: string;
  realName?: string;
  email?: string;
  phone?: string;
  departmentId?: number;
  departmentName?: string;
  gender?: string;
  birthDate?: string;
  idCard?: string;
  nationality?: string;
  nativePlace?: string;
  politicalStatus?: string;
  education?: string;
  major?: string;
  graduationSchool?: string;
  graduationDate?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  address?: string;
  lawyerLicenseNo?: string;
  licenseIssueDate?: string;
  licenseExpireDate?: string;
  licenseStatus?: string;
  practiceArea?: string;
  practiceYears?: number;
  position?: string;
  level?: string;
  entryDate?: string;
  probationEndDate?: string;
  regularDate?: string;
  resignationDate?: string;
  resignationReason?: string;
  workStatus?: string;
  workStatusName?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmployeeQuery {
  pageNum?: number;
  pageSize?: number;
  employeeNo?: string;
  realName?: string;
  departmentId?: number;
  workStatus?: string;
  position?: string;
}

export interface CreateEmployeeCommand {
  userId: number;
  employeeNo?: string;
  gender?: string;
  birthDate?: string;
  idCard?: string;
  nationality?: string;
  nativePlace?: string;
  politicalStatus?: string;
  education?: string;
  major?: string;
  graduationSchool?: string;
  graduationDate?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  address?: string;
  lawyerLicenseNo?: string;
  licenseIssueDate?: string;
  licenseExpireDate?: string;
  licenseStatus?: string;
  practiceArea?: string;
  practiceYears?: number;
  position?: string;
  level?: string;
  entryDate?: string;
  probationEndDate?: string;
  workStatus?: string;
  remark?: string;
}

export interface UpdateEmployeeCommand {
  employeeNo?: string;
  gender?: string;
  birthDate?: string;
  idCard?: string;
  nationality?: string;
  nativePlace?: string;
  politicalStatus?: string;
  education?: string;
  major?: string;
  graduationSchool?: string;
  graduationDate?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  address?: string;
  lawyerLicenseNo?: string;
  licenseIssueDate?: string;
  licenseExpireDate?: string;
  licenseStatus?: string;
  practiceArea?: string;
  practiceYears?: number;
  position?: string;
  level?: string;
  entryDate?: string;
  probationEndDate?: string;
  regularDate?: string;
  workStatus?: string;
  remark?: string;
}

// ========== API 函数 ==========

/** 分页查询员工档案 */
export function getEmployeeList(params: EmployeeQuery) {
  return requestClient.get<PageResult<EmployeeDTO>>('/hr/employee', { params });
}

/** 根据ID查询员工档案 */
export function getEmployeeDetail(id: number) {
  return requestClient.get<EmployeeDTO>(`/hr/employee/${id}`);
}

/** 根据用户ID查询员工档案 */
export function getEmployeeByUserId(userId: number) {
  return requestClient.get<EmployeeDTO>(`/hr/employee/user/${userId}`);
}

/** 创建员工档案 */
export function createEmployee(data: CreateEmployeeCommand) {
  return requestClient.post<EmployeeDTO>('/hr/employee', data);
}

/** 更新员工档案 */
export function updateEmployee(id: number, data: UpdateEmployeeCommand) {
  return requestClient.put<EmployeeDTO>(`/hr/employee/${id}`, data);
}

/** 删除员工档案 */
export function deleteEmployee(id: number) {
  return requestClient.delete(`/hr/employee/${id}`);
}
