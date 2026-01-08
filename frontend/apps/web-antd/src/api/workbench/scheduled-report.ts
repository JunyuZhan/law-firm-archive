/**
 * 定时报表 API
 */
import { requestClient } from '#/api/request';

import type { PageResult } from '../matter/types';

// ========== 类型定义 ==========
export interface ScheduledReportDTO {
  id: number;
  taskNo?: string;
  taskName: string;
  description?: string;
  templateId: number;
  templateName?: string;
  scheduleType: string;
  scheduleTypeName?: string;
  cronExpression?: string;
  executeTime?: string;
  executeDayOfWeek?: number;
  executeDayOfMonth?: number;
  scheduleDescription?: string;
  reportParameters?: Record<string, any>;
  outputFormat?: string;
  notifyEnabled?: boolean;
  notifyEmails?: string[];
  notifyUserIds?: number[];
  status?: string;
  statusName?: string;
  lastExecuteTime?: string;
  lastExecuteStatus?: string;
  lastExecuteStatusName?: string;
  nextExecuteTime?: string;
  totalExecuteCount?: number;
  successCount?: number;
  failCount?: number;
  createdBy?: number;
  createdByName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ScheduledReportLogDTO {
  id: number;
  taskId: number;
  taskNo?: string;
  executeTime?: string;
  status?: string;
  statusName?: string;
  reportId?: number;
  fileUrl?: string;
  fileSize?: number;
  fileSizeDisplay?: string;
  durationMs?: number;
  durationDisplay?: string;
  errorMessage?: string;
  notifyStatus?: string;
  notifyStatusName?: string;
  notifyResult?: string;
  createdAt?: string;
}

export interface ScheduledReportQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
}

export interface CreateScheduledReportCommand {
  taskName: string;
  description?: string;
  templateId: number;
  scheduleType: string;
  cronExpression?: string;
  executeTime?: string;
  executeDayOfWeek?: number;
  executeDayOfMonth?: number;
  reportParameters?: Record<string, any>;
  outputFormat?: string;
  notifyEnabled?: boolean;
  notifyEmails?: string[];
  notifyUserIds?: number[];
}

export interface ScheduledReportLogQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
}

// ========== API 函数 ==========

/** 分页查询定时报表任务 */
export function getScheduledReportList(params: ScheduledReportQuery) {
  return requestClient.get<PageResult<ScheduledReportDTO>>('/workbench/scheduled-report', { params });
}

/** 获取定时任务详情 */
export function getScheduledReportDetail(id: number) {
  return requestClient.get<ScheduledReportDTO>(`/workbench/scheduled-report/${id}`);
}

/** 创建定时报表任务 */
export function createScheduledReport(data: CreateScheduledReportCommand) {
  return requestClient.post<ScheduledReportDTO>('/workbench/scheduled-report', data);
}

/** 更新定时报表任务 */
export function updateScheduledReport(id: number, data: CreateScheduledReportCommand) {
  return requestClient.put<ScheduledReportDTO>(`/workbench/scheduled-report/${id}`, data);
}

/** 删除定时报表任务 */
export function deleteScheduledReport(id: number) {
  return requestClient.delete(`/workbench/scheduled-report/${id}`);
}

/** 启用任务 */
export function enableScheduledReport(id: number) {
  return requestClient.post(`/workbench/scheduled-report/${id}/enable`);
}

/** 暂停任务 */
export function pauseScheduledReport(id: number) {
  return requestClient.post(`/workbench/scheduled-report/${id}/pause`);
}

/** 立即执行任务 */
export function executeScheduledReportNow(id: number) {
  return requestClient.post<any>(`/workbench/scheduled-report/${id}/execute`);
}

/** 查询执行记录 */
export function getScheduledReportLogs(id: number, params: ScheduledReportLogQuery) {
  return requestClient.get<PageResult<ScheduledReportLogDTO>>(`/workbench/scheduled-report/${id}/logs`, { params });
}

