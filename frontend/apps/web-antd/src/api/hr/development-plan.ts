/**
 * 员工发展计划 API
 */
import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 类型定义 ==========
export interface DevelopmentMilestoneDTO {
  id: number;
  planId: number;
  milestoneName: string;
  description?: string;
  targetDate?: string;
  status?: string;
  statusName?: string;
  completedDate?: string;
  completionNote?: string;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface DevelopmentPlanDTO {
  id: number;
  planNo?: string;
  employeeId: number;
  employeeName?: string;
  planYear: number;
  planTitle: string;
  currentLevelId?: number;
  currentLevelName?: string;
  targetLevelId?: number;
  targetLevelName?: string;
  targetDate?: string;
  careerGoals?: string[];
  skillGoals?: string[];
  performanceGoals?: string[];
  actionPlans?: string[];
  requiredTraining?: string;
  requiredResources?: string;
  mentorId?: number;
  mentorName?: string;
  progressPercentage?: number;
  progressNotes?: string;
  status?: string;
  statusName?: string;
  reviewedBy?: number;
  reviewedByName?: string;
  reviewedAt?: string;
  reviewComment?: string;
  milestones?: DevelopmentMilestoneDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface DevelopmentPlanQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  employeeId?: number;
  planYear?: number;
}

export interface CreateDevelopmentPlanCommand {
  planYear: number;
  planTitle: string;
  targetLevelId?: number;
  targetDate?: string;
  careerGoals?: string[];
  skillGoals?: string[];
  performanceGoals?: string[];
  actionPlans?: string[];
  requiredTraining?: string;
  requiredResources?: string;
  mentorId?: number;
  milestones?: MilestoneItem[];
}

export interface MilestoneItem {
  milestoneName: string;
  description?: string;
  targetDate?: string;
  sortOrder?: number;
}

// ========== API 函数 ==========

/** 分页查询发展规划 */
export function getDevelopmentPlanList(params: DevelopmentPlanQuery) {
  return requestClient.get<PageResult<DevelopmentPlanDTO>>('/hr/development-plan', { params });
}

/** 获取规划详情 */
export function getDevelopmentPlanDetail(id: number) {
  return requestClient.get<DevelopmentPlanDTO>(`/hr/development-plan/${id}`);
}

/** 获取我的当年规划 */
export function getMyCurrentDevelopmentPlan() {
  return requestClient.get<DevelopmentPlanDTO>('/hr/development-plan/my-current');
}

/** 创建发展规划 */
export function createDevelopmentPlan(data: CreateDevelopmentPlanCommand) {
  return requestClient.post<DevelopmentPlanDTO>('/hr/development-plan', data);
}

/** 更新发展规划 */
export function updateDevelopmentPlan(id: number, data: CreateDevelopmentPlanCommand) {
  return requestClient.put<DevelopmentPlanDTO>(`/hr/development-plan/${id}`, data);
}

/** 删除发展规划 */
export function deleteDevelopmentPlan(id: number) {
  return requestClient.delete(`/hr/development-plan/${id}`);
}

/** 提交规划 */
export function submitDevelopmentPlan(id: number) {
  return requestClient.post(`/hr/development-plan/${id}/submit`);
}

/** 审核规划 */
export function reviewDevelopmentPlan(id: number, comment?: string) {
  return requestClient.post(`/hr/development-plan/${id}/review`, null, { params: { comment } });
}

/** 更新里程碑状态 */
export function updateMilestoneStatus(milestoneId: number, status: string, completionNote?: string) {
  return requestClient.post(`/hr/development-plan/milestones/${milestoneId}/status`, null, {
    params: { status, completionNote },
  });
}

