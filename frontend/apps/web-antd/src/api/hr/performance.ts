import type { PageResult } from '../matter/types';

/**
 * 绩效管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface PerformanceTaskDTO {
  id: number;
  name: string;
  periodType: string;
  periodTypeName?: string;
  year: number;
  period?: number;
  startDate: string;
  endDate: string;
  selfEvalDeadline?: string;
  peerEvalDeadline?: string;
  supervisorEvalDeadline?: string;
  status: string;
  statusName?: string;
  description?: string;
  remarks?: string;
  createdAt?: string;
  totalEmployees?: number;
  completedCount?: number;
}

export interface PerformanceIndicatorDTO {
  id: number;
  name: string;
  code?: string;
  category: string;
  categoryName?: string;
  description?: string;
  weight?: number;
  maxScore?: number;
  scoringCriteria?: string;
  applicableRole?: string;
  applicableRoleName?: string;
  sortOrder?: number;
  status?: string;
  remarks?: string;
}

export interface PerformanceScoreDTO {
  id?: number;
  evaluationId?: number;
  indicatorId: number;
  indicatorName?: string;
  indicatorCategory?: string;
  weight?: number;
  maxScore?: number;
  score: number;
  comment?: string;
}

export interface PerformanceEvaluationDTO {
  id: number;
  taskId: number;
  taskName?: string;
  employeeId: number;
  employeeName?: string;
  evaluatorId?: number;
  evaluatorName?: string;
  evaluationType: string;
  evaluationTypeName?: string;
  totalScore?: number;
  grade?: string;
  gradeName?: string;
  comment?: string;
  strengths?: string;
  improvements?: string;
  evaluatedAt?: string;
  status?: string;
  statusName?: string;
  scores?: PerformanceScoreDTO[];
}

export interface PerformanceTaskQuery {
  pageNum?: number;
  pageSize?: number;
  year?: number;
  periodType?: string;
  status?: string;
}

export interface CreatePerformanceTaskCommand {
  name: string;
  periodType: string;
  year: number;
  period?: number;
  startDate: string;
  endDate: string;
  selfEvalDeadline?: string;
  peerEvalDeadline?: string;
  supervisorEvalDeadline?: string;
  description?: string;
  remarks?: string;
}

export interface CreateIndicatorCommand {
  name: string;
  code?: string;
  category: string;
  description?: string;
  weight?: number;
  maxScore?: number;
  scoringCriteria?: string;
  applicableRole?: string;
  sortOrder?: number;
  status?: string;
  remarks?: string;
}

export interface SubmitEvaluationCommand {
  taskId: number;
  employeeId: number;
  evaluationType: string;
  scores: PerformanceScoreDTO[];
  comment?: string;
  strengths?: string;
  improvements?: string;
}

// ========== 考核任务 API ==========

/** 分页查询考核任务 */
export function getPerformanceTaskList(params: PerformanceTaskQuery) {
  return requestClient.get<PageResult<PerformanceTaskDTO>>(
    '/hr/performance/tasks',
    { params },
  );
}

/** 获取考核任务详情 */
export function getPerformanceTaskDetail(id: number) {
  return requestClient.get<PerformanceTaskDTO>(`/hr/performance/tasks/${id}`);
}

/** 创建考核任务 */
export function createPerformanceTask(data: CreatePerformanceTaskCommand) {
  return requestClient.post<PerformanceTaskDTO>('/hr/performance/tasks', data);
}

/** 启动考核任务 */
export function startPerformanceTask(id: number) {
  return requestClient.post(`/hr/performance/tasks/${id}/start`);
}

/** 完成考核任务 */
export function completePerformanceTask(id: number) {
  return requestClient.post(`/hr/performance/tasks/${id}/complete`);
}

/** 获取考核任务统计 */
export function getPerformanceTaskStatistics(id: number) {
  return requestClient.get<Record<string, any>>(
    `/hr/performance/tasks/${id}/statistics`,
  );
}

// ========== 考核指标 API ==========

/** 查询考核指标列表 */
export function getPerformanceIndicatorList(
  category?: string,
  applicableRole?: string,
) {
  return requestClient.get<PerformanceIndicatorDTO[]>(
    '/hr/performance/indicators',
    {
      params: { category, applicableRole },
    },
  );
}

/** 创建考核指标 */
export function createPerformanceIndicator(data: CreateIndicatorCommand) {
  return requestClient.post<PerformanceIndicatorDTO>(
    '/hr/performance/indicators',
    data,
  );
}

/** 更新考核指标 */
export function updatePerformanceIndicator(
  id: number,
  data: CreateIndicatorCommand,
) {
  return requestClient.put<PerformanceIndicatorDTO>(
    `/hr/performance/indicators/${id}`,
    data,
  );
}

/** 删除考核指标 */
export function deletePerformanceIndicator(id: number) {
  return requestClient.delete(`/hr/performance/indicators/${id}`);
}

// ========== 绩效评价 API ==========

/** 提交绩效评价 */
export function submitPerformanceEvaluation(data: SubmitEvaluationCommand) {
  return requestClient.post<PerformanceEvaluationDTO>(
    '/hr/performance/evaluations',
    data,
  );
}

/** 获取员工的评价记录 */
export function getEmployeeEvaluations(taskId: number, employeeId: number) {
  return requestClient.get<PerformanceEvaluationDTO[]>(
    '/hr/performance/evaluations',
    {
      params: { taskId, employeeId },
    },
  );
}

/** 获取我待评价的记录 */
export function getMyPendingEvaluations() {
  return requestClient.get<PerformanceEvaluationDTO[]>(
    '/hr/performance/evaluations/pending',
  );
}

/** 获取评价详情 */
export function getPerformanceEvaluationDetail(id: number) {
  return requestClient.get<PerformanceEvaluationDTO>(
    `/hr/performance/evaluations/${id}`,
  );
}
