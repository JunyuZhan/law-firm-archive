// 使用 types.ts 中的 PageResult 定义
import type { PageResult } from './types';

import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface DeadlineDTO {
  id: number;
  matterId: number;
  matterNo?: string;
  matterName?: string;
  deadlineType: string;
  deadlineTypeName?: string;
  deadlineName: string;
  baseDate?: string;
  deadlineDate: string;
  reminderDays?: number;
  reminderSent?: boolean;
  reminderSentAt?: string;
  status: string;
  statusName?: string;
  completedAt?: string;
  completedBy?: number;
  completedByName?: string;
  description?: string;
  daysRemaining?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface DeadlineQueryDTO {
  pageNum?: number;
  pageSize?: number;
  matterId?: number;
  deadlineType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}

export interface CreateDeadlineCommand {
  matterId: number;
  deadlineType: string;
  deadlineName: string;
  baseDate?: string;
  deadlineDate: string;
  reminderDays?: number;
  description?: string;
}

export interface UpdateDeadlineCommand {
  id: number;
  deadlineName?: string;
  deadlineDate?: string;
  reminderDays?: number;
  description?: string;
}

// 期限类型选项
export const DEADLINE_TYPE_OPTIONS = [
  { label: '举证期', value: 'EVIDENCE_SUBMISSION' },
  { label: '答辩期', value: 'DEFENSE' },
  { label: '上诉期', value: 'APPEAL' },
  { label: '申请执行期', value: 'EXECUTION_APPLICATION' },
  { label: '开庭日期', value: 'COURT_DATE' },
  { label: '仲裁开庭', value: 'ARBITRATION_DATE' },
  { label: '合同履行期', value: 'CONTRACT_PERFORMANCE' },
  { label: '其他', value: 'OTHER' },
];

// 状态选项
export const DEADLINE_STATUS_OPTIONS = [
  { label: '待处理', value: 'PENDING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已过期', value: 'EXPIRED' },
];

// ==================== API ====================

/** 分页查询期限列表 */
export function getDeadlines(params: DeadlineQueryDTO) {
  return requestClient.get<PageResult<DeadlineDTO>>('/matter/deadlines/list', {
    params,
  });
}

/** 获取期限详情 */
export function getDeadlineById(id: number) {
  return requestClient.get<DeadlineDTO>(`/matter/deadlines/${id}`);
}

/** 根据项目ID查询期限列表 */
export function getDeadlinesByMatterId(matterId: number) {
  return requestClient.get<DeadlineDTO[]>(
    `/matter/deadlines/matter/${matterId}`,
  );
}

/** 创建期限提醒 */
export function createDeadline(data: CreateDeadlineCommand) {
  return requestClient.post<DeadlineDTO>('/matter/deadlines', data);
}

/** 自动创建期限提醒 */
export function autoCreateDeadlines(matterId: number) {
  return requestClient.post<void>(`/matter/deadlines/auto-create/${matterId}`);
}

/** 更新期限提醒 */
export function updateDeadline(data: UpdateDeadlineCommand) {
  return requestClient.put<DeadlineDTO>('/matter/deadlines', data);
}

/** 完成期限 */
export function completeDeadline(id: number) {
  return requestClient.post<DeadlineDTO>(`/matter/deadlines/${id}/complete`);
}

/** 删除期限提醒 */
export function deleteDeadline(id: number) {
  return requestClient.delete<void>(`/matter/deadlines/${id}`);
}
