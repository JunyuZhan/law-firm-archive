import type {
  CreateMatterCommand,
  CreateTaskCommand,
  CreateTimesheetCommand,
  MatterDTO,
  MatterQuery,
  MatterSimpleDTO,
  MatterTimelineDTO,
  PageResult,
  TaskDTO,
  TaskQuery,
  TimesheetDTO,
  TimesheetQuery,
  UpdateMatterCommand,
} from './types';

/**
 * 项目管理模块 API
 */
import { requestClient } from '#/api/request';

// ========== 项目/案件管理 API ==========

/** 获取项目列表 */
export function getMatterList(params: MatterQuery) {
  return requestClient.get<PageResult<MatterDTO>>('/matter/list', { params });
}

/**
 * 获取项目选择列表（公共接口，无需 matter:list 权限）
 * 用于下拉选择框，所有登录用户都可以访问
 * 返回精简数据，不包含金额、对方信息等敏感信息
 */
export function getMatterSelectOptions(params?: MatterQuery) {
  return requestClient.get<PageResult<MatterSimpleDTO>>(
    '/matter/select-options',
    { params },
  );
}

/** 获取我的项目 */
export function getMyMatters(params: MatterQuery) {
  return requestClient.get<PageResult<MatterDTO>>('/matter/my', { params });
}

/** 获取项目详情 */
export function getMatterDetail(id: number) {
  return requestClient.get<MatterDTO>(`/matter/${id}`);
}

/** 创建项目 */
export function createMatter(data: CreateMatterCommand) {
  return requestClient.post<MatterDTO>('/matter', data);
}

/** 更新项目 */
export function updateMatter(data: UpdateMatterCommand) {
  return requestClient.put<MatterDTO>('/matter', data);
}

/** 删除项目 */
export function deleteMatter(id: number) {
  return requestClient.delete(`/matter/${id}`);
}

/** 修改项目状态 */
export function changeMatterStatus(id: number, status: string) {
  return requestClient.put(`/matter/${id}/status`, { status });
}

/** 添加团队成员 */
export function addParticipant(
  matterId: number,
  data: {
    commissionRate?: number;
    isOriginator?: boolean;
    role: string;
    userId: number;
  },
) {
  return requestClient.post(`/matter/${matterId}/participant`, data);
}

/** 移除团队成员 */
export function removeParticipant(matterId: number, userId: number) {
  return requestClient.delete(`/matter/${matterId}/participant/${userId}`);
}

/** 获取结案审批人列表 */
export function getMatterCloseApprovers() {
  return requestClient.get<
    Array<{
      departmentName: string;
      id: number;
      position: string;
      realName: string;
      recommended: boolean;
    }>
  >('/matter/close/approvers');
}

/** 申请结案 */
export function applyCloseMatter(
  id: number,
  data: {
    approverId?: number; // 审批人ID
    closingDate: string; // 结案日期
    closingReason: string; // 结案原因：WIN/LOSE/SETTLEMENT/WITHDRAWAL/COMPLETED/OTHER
    outcome?: string; // 判决/调解结果描述
    summary?: string; // 结案总结
  },
) {
  return requestClient.post(`/matter/${id}/close/apply`, data);
}

/** 审批结案 */
export function approveCloseMatter(
  id: number,
  data: { approved: boolean; comment?: string },
) {
  return requestClient.post(`/matter/${id}/close/approve`, data);
}

/** 生成结案报告 */
export function generateCloseReport(id: number) {
  return requestClient.get<string>(`/matter/${id}/close/report`);
}

/** 获取项目时间线 */
export function getMatterTimeline(id: number) {
  return requestClient.get<MatterTimelineDTO[]>(`/matter/${id}/timeline`);
}

// ========== 合同管理 API（在项目管理模块中） ==========

/** 合同查询参数 */
export interface MatterContractQuery {
  pageNum?: number;
  pageSize?: number;
  contractNo?: string;
  name?: string;
  clientId?: number;
  status?: string;
  createdAtFrom?: string;
  createdAtTo?: string;
  signDateFrom?: string;
  signDateTo?: string;
}

/** 获取合同列表（律师有权限） */
export function getMatterContractList(params: MatterContractQuery) {
  return requestClient.get<PageResult<any>>('/matter/contract/list', {
    params,
  });
}

/** 获取我的合同 */
export function getMyContracts(params: MatterContractQuery) {
  return requestClient.get<PageResult<any>>('/matter/contract/my', { params });
}

/** 获取合同统计信息 */
export function getMatterContractStatistics() {
  return requestClient.get<any>('/matter/contract/statistics');
}

/** 创建合同 */
export function createContract(data: any) {
  return requestClient.post<any>('/matter/contract', data);
}

/** 获取合同详情 */
export function getContractDetail(id: number) {
  return requestClient.get<any>(`/matter/contract/${id}`);
}

/** 获取合同打印数据 */
export function getContractPrintData(id: number) {
  return requestClient.get<ContractPrintDTO>(
    `/matter/contract/${id}/print-data`,
  );
}

/** 合同打印数据类型 */
export interface ContractPrintDTO {
  id: number;
  contractNo: string;
  name: string;
  contractType: string;
  contractTypeName: string;
  status: string;
  statusName: string;
  // 委托人信息
  clientId: number;
  clientName: string;
  clientType: string;
  clientTypeName: string;
  clientAddress: string;
  clientPhone: string;
  clientIdNumber: string;
  // 律所信息
  firmName: string;
  firmAddress: string;
  firmPhone: string;
  firmLegalRep: string;
  // 案件信息
  caseType: string;
  caseTypeName: string;
  causeOfAction: string;
  causeOfActionName: string;
  trialStage: string;
  trialStageName: string;
  opposingParty: string;
  jurisdictionCourt: string;
  claimAmount: number;
  description: string;
  // 费用信息
  feeType: string;
  feeTypeName: string;
  totalAmount: number;
  // 时间信息
  signDate: string;
  effectiveDate: string;
  expiryDate: string;
  // 人员信息
  signerId: number;
  signerName: string;
  leadLawyerName: string;
  assistLawyerNames: string;
  originatorName: string;
  // 利冲信息
  conflictCheckStatus: string;
  conflictCheckStatusName: string;
  conflictCheckResult: string;
  // 审批信息
  approvals: Array<{
    approvedAt: string;
    approverName: string;
    approverRole: string;
    comment: string;
    status: string;
    statusName: string;
  }>;
  // 模板内容
  contractContent: string;
}

/** 获取已审批的合同列表（用于创建项目时选择） */
export function getApprovedContracts() {
  return requestClient.get<any[]>('/matter/contract/approved');
}

/** 基于合同创建项目 */
export function createMatterFromContract(
  contractId: number,
  data: CreateMatterCommand,
) {
  return requestClient.post<MatterDTO>(
    `/matter/from-contract/${contractId}`,
    data,
  );
}

/** 更新合同（在项目管理模块中） */
export function updateContract(data: { [key: string]: any; id: number }) {
  const { id, ...updateData } = data;
  return requestClient.put<any>(`/matter/contract/${id}`, updateData);
}

/** 提交合同审批 */
export function submitContract(id: number, approverId?: number) {
  return requestClient.post(`/finance/contract/${id}/submit`, null, {
    params: approverId ? { approverId } : undefined,
  });
}

/** 获取可选审批人列表 */
export function getContractApprovers() {
  return requestClient.get<
    Array<{
      departmentName: string;
      id: number;
      position: string;
      realName: string;
    }>
  >('/matter/contract/approvers');
}

/** 审批合同（通过） */
export function approveContract(id: number) {
  return requestClient.post(`/finance/contract/${id}/approve`);
}

/** 审批合同（拒绝） */
export function rejectContract(id: number, reason: string) {
  return requestClient.post(`/finance/contract/${id}/reject`, { reason });
}

/** 申请合同变更 */
export function applyContractChange(data: {
  changeDescription?: string;
  changeReason: string;
  clientId?: number;
  contractId: number;
  contractType?: string;
  currency?: string;
  departmentId?: number;
  effectiveDate?: string;
  expiryDate?: string;
  feeType?: string;
  fileUrl?: string;
  matterId?: number;
  name?: string;
  paymentTerms?: string;
  remark?: string;
  signDate?: string;
  signerId?: number;
  totalAmount?: number;
}) {
  return requestClient.post('/matter/contract/change', data);
}

// ========== 工时管理 API ==========

/** 获取工时列表 */
export function getTimesheetList(params: TimesheetQuery) {
  return requestClient.get<PageResult<TimesheetDTO>>('/timesheets', { params });
}

/** 获取我的工时 */
export function getMyTimesheets(params: TimesheetQuery) {
  return requestClient.get<PageResult<TimesheetDTO>>('/timesheets/my', {
    params,
  });
}

/** 获取工时详情 */
export function getTimesheetDetail(id: number) {
  return requestClient.get<TimesheetDTO>(`/timesheets/${id}`);
}

/** 创建工时记录 */
export function createTimesheet(data: CreateTimesheetCommand) {
  return requestClient.post<TimesheetDTO>('/timesheets', data);
}

/** 更新工时记录 */
export function updateTimesheet(
  id: number,
  data: Partial<CreateTimesheetCommand>,
) {
  return requestClient.put<TimesheetDTO>(`/timesheets/${id}`, data);
}

/** 删除工时记录 */
export function deleteTimesheet(id: number) {
  return requestClient.delete(`/timesheets/${id}`);
}

/** 提交工时审核 */
export function submitTimesheet(id: number) {
  return requestClient.post(`/timesheets/${id}/submit`);
}

/** 审核工时（通过） */
export function approveTimesheet(id: number, comment?: string) {
  return requestClient.post<TimesheetDTO>(`/timesheets/${id}/approve`, null, {
    params: comment ? { comment } : undefined,
  });
}

/** 审核工时（拒绝） */
export function rejectTimesheet(id: number, comment?: string) {
  return requestClient.post<TimesheetDTO>(`/timesheets/${id}/reject`, null, {
    params: comment ? { comment } : undefined,
  });
}

/** 审核工时（统一接口，根据approved字段调用approve或reject） */
export function reviewTimesheet(
  id: number,
  data: { approved: boolean; comment?: string },
) {
  return data.approved
    ? approveTimesheet(id, data.comment)
    : rejectTimesheet(id, data.comment);
}

/** 工时统计 */
export function getTimesheetStats(params: {
  endDate?: string;
  matterId?: number;
  startDate?: string;
  userId?: number;
}) {
  return requestClient.get('/timesheets/stats', { params });
}

// ========== 任务管理 API ==========

/** 获取任务列表 */
export function getTaskList(params: TaskQuery) {
  return requestClient.get<PageResult<TaskDTO>>('/tasks', { params });
}

/** 获取我的待办任务 */
export function getMyTodoTasks() {
  return requestClient.get<TaskDTO[]>('/tasks/my/todo');
}

/** 获取即将到期的任务 */
export function getUpcomingTasks(days: number = 7) {
  return requestClient.get<TaskDTO[]>(`/tasks/upcoming?days=${days}`);
}

/** 获取逾期任务 */
export function getOverdueTasks() {
  return requestClient.get<TaskDTO[]>('/tasks/overdue');
}

/** 获取任务详情 */
export function getTaskDetail(id: number) {
  return requestClient.get<TaskDTO>(`/tasks/${id}`);
}

/** 创建任务 */
export function createTask(data: CreateTaskCommand) {
  return requestClient.post<TaskDTO>('/tasks', data);
}

/** 更新任务 */
export function updateTask(id: number, data: Partial<CreateTaskCommand>) {
  return requestClient.put<TaskDTO>(`/tasks/${id}`, data);
}

/** 删除任务 */
export function deleteTask(id: number) {
  return requestClient.delete(`/tasks/${id}`);
}

/** 修改任务状态 */
export function changeTaskStatus(id: number, status: string) {
  return requestClient.put<TaskDTO>(`/tasks/${id}/status`, null, {
    params: { status },
  });
}

/** 更新任务进度 */
export function updateTaskProgress(id: number, progress: number) {
  return requestClient.put<TaskDTO>(`/tasks/${id}/progress`, null, {
    params: { progress },
  });
}

/** 验收任务（通过） */
export function approveTask(id: number) {
  return requestClient.post<TaskDTO>(`/tasks/${id}/review/approve`);
}

/** 验收任务（退回） */
export function rejectTask(id: number, comment: string) {
  return requestClient.post<TaskDTO>(`/tasks/${id}/review/reject`, null, {
    params: { comment },
  });
}

/** 获取案件任务统计 */
export function getMatterTaskStats(matterId: number) {
  return requestClient.get<number[]>(`/tasks/stats/matter/${matterId}`);
}

// 导出期限提醒和日程API
export * from './deadline';
export * from './schedule';

// 导出类型
export type * from './types';
