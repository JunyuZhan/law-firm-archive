import type { PageResult } from './types';

import { requestClient } from '#/api/request';

// ========== 培训管理类型定义 ==========
export interface TrainingDTO {
  id: number;
  name: string;
  trainingType?: string;
  trainingTypeName?: string;
  trainer?: string;
  trainerId?: number;
  trainingTime?: string;
  location?: string;
  description?: string;
  credits?: number;
  maxParticipants?: number;
  currentParticipants?: number;
  status: string;
  statusName?: string;
  createdAt?: string;
  // 新增字段
  isRequired?: boolean; // 是否必修
  enrollmentType?: string; // 报名类型: VOLUNTARY/MANDATORY/ASSIGNED
  enrollmentTypeName?: string;
  trainingPlanId?: number; // 关联年度方案
  attachments?: AttachmentDTO[]; // 附件列表
}

export interface AttachmentDTO {
  fileName: string;
  fileUrl: string;
  fileSize?: number;
  fileType?: string;
}

export interface TrainingRecordDTO {
  id: number;
  trainingId: number;
  trainingName?: string;
  userId: number;
  userName?: string;
  enrollTime?: string;
  attendStatus?: string;
  attendStatusName?: string;
  credits?: number;
}

export interface TrainingQuery {
  keyword?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateTrainingCommand {
  name: string;
  trainingType?: string;
  trainerId?: number;
  trainingTime?: string;
  location?: string;
  description?: string;
  credits?: number;
  maxParticipants?: number;
  // 新增字段
  isRequired?: boolean;
  enrollmentType?: string;
  trainingPlanId?: number;
  attachments?: AttachmentDTO[];
}

// ========== 培训管理 API ==========

/** 获取培训列表 */
export function getTrainingList(params: TrainingQuery) {
  return requestClient.get<PageResult<TrainingDTO>>('/hr/training/list', {
    params,
  });
}

/** 获取可报名的培训列表 */
export function getAvailableTrainings() {
  return requestClient.get<TrainingDTO[]>('/hr/training/available');
}

/** 获取培训详情 */
export function getTrainingDetail(id: number) {
  return requestClient.get<TrainingDTO>(`/hr/training/${id}`);
}

/** 创建培训计划 */
export function createTraining(data: CreateTrainingCommand) {
  return requestClient.post<TrainingDTO>('/hr/training', data);
}

/** 发布培训 */
export function publishTraining(id: number) {
  return requestClient.post(`/hr/training/${id}/publish`);
}

/** 取消培训 */
export function cancelTraining(id: number) {
  return requestClient.post(`/hr/training/${id}/cancel`);
}

/** 报名培训 */
export function enrollTraining(id: number) {
  return requestClient.post(`/hr/training/${id}/enroll`);
}

/** 取消报名 */
export function cancelEnrollment(id: number) {
  return requestClient.post(`/hr/training/${id}/cancel-enrollment`);
}

/** 获取我的培训记录 */
export function getMyTrainingRecords() {
  return requestClient.get<TrainingRecordDTO[]>('/hr/training/my-records');
}

/** 获取我的学分统计 */
export function getMyTotalCredits() {
  return requestClient.get<number>('/hr/training/my-credits');
}

/** 获取培训参与者列表 */
export function getTrainingParticipants(id: number) {
  return requestClient.get<TrainingRecordDTO[]>(
    `/hr/training/${id}/participants`,
  );
}
