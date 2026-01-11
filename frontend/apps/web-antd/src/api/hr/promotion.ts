import type { PageResult } from '../matter/types';

/**
 * 晋升管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface CareerLevelDTO {
  id: number;
  levelCode: string;
  levelName: string;
  levelOrder: number;
  category: string;
  categoryName?: string;
  description?: string;
  minWorkYears?: number;
  minMatterCount?: number;
  minRevenue?: number;
  requiredCertificates?: string[];
  otherRequirements?: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryRange?: string;
  status?: string;
  statusName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PromotionReviewDTO {
  id: number;
  applicationId: number;
  reviewerId?: number;
  reviewerName?: string;
  reviewerRole: string;
  reviewerRoleName?: string;
  scoreDetails?: Record<string, any>;
  totalScore?: number;
  reviewOpinion: string;
  reviewOpinionName?: string;
  reviewComment?: string;
  reviewTime?: string;
  createdAt?: string;
}

export interface PromotionApplicationDTO {
  id: number;
  applicationNo?: string;
  employeeId: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  currentLevelId?: number;
  currentLevelName?: string;
  targetLevelId: number;
  targetLevelName?: string;
  applyReason?: string;
  achievements?: string;
  selfEvaluation?: string;
  attachments?: string[];
  status?: string;
  statusName?: string;
  reviewScore?: number;
  reviewResult?: string;
  reviewResultName?: string;
  reviewComment?: string;
  approvedBy?: number;
  approvedByName?: string;
  approvedAt?: string;
  approvalComment?: string;
  effectiveDate?: string;
  applyDate?: string;
  reviews?: PromotionReviewDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PromotionLevelQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
  status?: string;
}

export interface PromotionApplicationQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  employeeId?: number;
  departmentId?: number;
}

export interface CreateCareerLevelCommand {
  levelCode: string;
  levelName: string;
  levelOrder: number;
  category: string;
  description?: string;
  minWorkYears?: number;
  minMatterCount?: number;
  minRevenue?: number;
  requiredCertificates?: string[];
  otherRequirements?: string;
  salaryMin?: number;
  salaryMax?: number;
}

export interface CreatePromotionCommand {
  targetLevelId: number;
  applyReason?: string;
  achievements?: string;
  selfEvaluation?: string;
  attachments?: string[];
}

export interface SubmitReviewCommand {
  applicationId: number;
  reviewerRole: string;
  scoreDetails?: Record<string, any>;
  totalScore?: number;
  reviewOpinion: string;
  reviewComment?: string;
}

// ========== 职级管理 API ==========

/** 分页查询职级 */
export function getCareerLevelList(params: PromotionLevelQuery) {
  return requestClient.get<PageResult<CareerLevelDTO>>('/hr/promotion/levels', {
    params,
  });
}

/** 获取职级详情 */
export function getCareerLevelDetail(id: number) {
  return requestClient.get<CareerLevelDTO>(`/hr/promotion/levels/${id}`);
}

/** 按类别获取职级列表 */
export function getCareerLevelsByCategory(category: string) {
  return requestClient.get<CareerLevelDTO[]>(
    `/hr/promotion/levels/category/${category}`,
  );
}

/** 创建职级 */
export function createCareerLevel(data: CreateCareerLevelCommand) {
  return requestClient.post<CareerLevelDTO>('/hr/promotion/levels', data);
}

/** 更新职级 */
export function updateCareerLevel(id: number, data: CreateCareerLevelCommand) {
  return requestClient.put<CareerLevelDTO>(`/hr/promotion/levels/${id}`, data);
}

/** 删除职级 */
export function deleteCareerLevel(id: number) {
  return requestClient.delete(`/hr/promotion/levels/${id}`);
}

/** 启用职级 */
export function enableCareerLevel(id: number) {
  return requestClient.post(`/hr/promotion/levels/${id}/enable`);
}

/** 停用职级 */
export function disableCareerLevel(id: number) {
  return requestClient.post(`/hr/promotion/levels/${id}/disable`);
}

// ========== 晋升申请 API ==========

/** 分页查询晋升申请 */
export function getPromotionApplicationList(params: PromotionApplicationQuery) {
  return requestClient.get<PageResult<PromotionApplicationDTO>>(
    '/hr/promotion/applications',
    { params },
  );
}

/** 获取晋升申请详情 */
export function getPromotionApplicationDetail(id: number) {
  return requestClient.get<PromotionApplicationDTO>(
    `/hr/promotion/applications/${id}`,
  );
}

/** 提交晋升申请 */
export function submitPromotionApplication(data: CreatePromotionCommand) {
  return requestClient.post<PromotionApplicationDTO>(
    '/hr/promotion/applications',
    data,
  );
}

/** 取消晋升申请 */
export function cancelPromotionApplication(id: number) {
  return requestClient.post(`/hr/promotion/applications/${id}/cancel`);
}

/** 提交评审 */
export function submitPromotionReview(data: SubmitReviewCommand) {
  return requestClient.post('/hr/promotion/applications/review', data);
}

/** 最终审批-通过 */
export function approvePromotionApplication(
  id: number,
  comment?: string,
  effectiveDate?: string,
) {
  return requestClient.post(`/hr/promotion/applications/${id}/approve`, null, {
    params: { comment, effectiveDate },
  });
}

/** 最终审批-拒绝 */
export function rejectPromotionApplication(id: number, comment?: string) {
  return requestClient.post(`/hr/promotion/applications/${id}/reject`, null, {
    params: { comment },
  });
}

/** 统计待审批数量 */
export function countPendingPromotionApplications() {
  return requestClient.get<number>('/hr/promotion/applications/pending-count');
}
