/**
 * 节假日服务API
 * 提供工作日计算、诉讼期限计算等功能
 */

import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface WorkdayCheckResult {
  date: string;
  isWorkday: boolean;
  typeName: string;
}

export interface HolidayCheckResult {
  date: string;
  isHoliday: boolean;
  typeName: string;
}

export interface AddWorkdaysResult {
  startDate: string;
  workdays: number;
  resultDate: string;
  isWorkday: boolean;
}

export interface DeadlineResult {
  startDate: string;
  days: number;
  workdaysOnly: boolean;
  deadline: string;
  isWorkday: boolean;
  deadlineTypeName: string;
  explanation: string;
}

export interface CountWorkdaysResult {
  startDate: string;
  endDate: string;
  workdayCount: number;
}

export interface OffDaysResult {
  startDate: string;
  endDate: string;
  offDays: string[];
  count: number;
}

export interface ReminderDateResult {
  targetDate: string;
  workdaysBefore: number;
  reminderDate: string;
}

export interface SyncResult {
  year: number;
  syncedCount: number;
  message: string;
}

export interface SyncStatusResult {
  year: number;
  synced: boolean;
}

// ==================== API方法 ====================

/**
 * 判断是否工作日
 */
export function isWorkday(date: string) {
  return requestClient.get<WorkdayCheckResult>('/system/holiday/is-workday', {
    params: { date },
  });
}

/**
 * 判断是否法定节假日
 */
export function isHoliday(date: string) {
  return requestClient.get<HolidayCheckResult>('/system/holiday/is-holiday', {
    params: { date },
  });
}

/**
 * 计算N个工作日后的日期
 * @param startDate 起始日期
 * @param workdays 工作日数量（正数向后，负数向前）
 */
export function addWorkdays(startDate: string, workdays: number) {
  return requestClient.get<AddWorkdaysResult>('/system/holiday/add-workdays', {
    params: { startDate, workdays },
  });
}

/**
 * 计算诉讼期限截止日期
 * @param startDate 起算日期（如判决书送达日）
 * @param days 期限天数
 * @param workdaysOnly 是否按工作日计算（默认false，即按自然日计算但节假日顺延）
 */
export function calculateDeadline(
  startDate: string,
  days: number,
  workdaysOnly = false,
) {
  return requestClient.get<DeadlineResult>('/system/holiday/deadline', {
    params: { startDate, days, workdaysOnly },
  });
}

/**
 * 计算两个日期间的工作日数量
 */
export function countWorkdays(startDate: string, endDate: string) {
  return requestClient.get<CountWorkdaysResult>(
    '/system/holiday/count-workdays',
    {
      params: { startDate, endDate },
    },
  );
}

/**
 * 获取范围内的休息日
 */
export function getOffDays(startDate: string, endDate: string) {
  return requestClient.get<OffDaysResult>('/system/holiday/off-days', {
    params: { startDate, endDate },
  });
}

/**
 * 计算提醒日期（目标日期前N个工作日）
 */
export function calculateReminderDate(
  targetDate: string,
  workdaysBefore: number,
) {
  return requestClient.get<ReminderDateResult>(
    '/system/holiday/reminder-date',
    {
      params: { targetDate, workdaysBefore },
    },
  );
}

/**
 * 同步节假日数据（管理员）
 */
export function syncHolidays(year: number) {
  return requestClient.post<SyncResult>(`/system/holiday/sync/${year}`);
}

/**
 * 检查数据同步状态
 */
export function checkSyncStatus(year: number) {
  return requestClient.get<SyncStatusResult>(
    `/system/holiday/sync-status/${year}`,
  );
}
