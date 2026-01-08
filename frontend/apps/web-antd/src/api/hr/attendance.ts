import { requestClient } from '#/api/request';

import type { AttendanceQueryParams, AttendanceRecord, PageResult } from './types';

/**
 * 获取考勤列表
 */
export function fetchAttendanceList(params: AttendanceQueryParams): Promise<PageResult<AttendanceRecord>> {
  return requestClient.get('/admin/attendance', { params });
}

/**
 * 签到
 */
export function checkIn(data?: { location?: string; remark?: string }): Promise<AttendanceRecord> {
  return requestClient.post('/admin/attendance/check-in', data || {});
}

/**
 * 签退
 */
export function checkOut(data?: { location?: string; remark?: string }): Promise<AttendanceRecord> {
  return requestClient.post('/admin/attendance/check-out', data || {});
}

/**
 * 获取今日考勤
 */
export function getTodayAttendance(): Promise<AttendanceRecord> {
  return requestClient.get('/admin/attendance/today');
}

/**
 * 获取月度考勤统计
 */
export function getMonthlyStatistics(params: { userId?: number; year: number; month: number }): Promise<any> {
  return requestClient.get('/admin/attendance/statistics/monthly', { params });
}
