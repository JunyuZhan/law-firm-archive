import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface ScheduleDTO {
  id: number;
  matterId?: number;
  matterName?: string;
  userId?: number;
  userName?: string;
  title: string;
  description?: string;
  location?: string;
  scheduleType: string;
  scheduleTypeName?: string;
  startTime: string;
  endTime?: string;
  allDay?: boolean;
  reminderMinutes?: number;
  reminderSent?: boolean;
  recurrenceRule?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateScheduleCommand {
  matterId?: number;
  title: string;
  description?: string;
  location?: string;
  scheduleType: string;
  startTime: string;
  endTime?: string;
  allDay?: boolean;
  reminderMinutes?: number;
  recurrenceRule?: string;
}

export interface UpdateScheduleCommand {
  title?: string;
  description?: string;
  location?: string;
  startTime?: string;
  endTime?: string;
  reminderMinutes?: number;
}

// 日程类型选项
export const SCHEDULE_TYPE_OPTIONS = [
  { label: '开庭', value: 'COURT', color: '#f5222d' },
  { label: '会议', value: 'MEETING', color: '#1890ff' },
  { label: '期限', value: 'DEADLINE', color: '#fa8c16' },
  { label: '约见', value: 'APPOINTMENT', color: '#52c41a' },
  { label: '其他', value: 'OTHER', color: '#722ed1' },
];

// 提醒时间选项
export const REMINDER_OPTIONS = [
  { label: '不提醒', value: 0 },
  { label: '提前5分钟', value: 5 },
  { label: '提前15分钟', value: 15 },
  { label: '提前30分钟', value: 30 },
  { label: '提前1小时', value: 60 },
  { label: '提前2小时', value: 120 },
  { label: '提前1天', value: 1440 },
  { label: '提前2天', value: 2880 },
];

// ==================== API ====================

/** 查询日程列表 */
export function getSchedules(params: {
  endTime?: string;
  matterId?: number;
  pageNum?: number;
  pageSize?: number;
  scheduleType?: string;
  startTime?: string;
  userId?: number;
}) {
  return requestClient.get<ScheduleDTO[]>('/schedules', { params });
}

/** 获取日程详情 */
export function getScheduleById(id: number) {
  return requestClient.get<ScheduleDTO>(`/schedules/${id}`);
}

/** 创建日程 */
export function createSchedule(data: CreateScheduleCommand) {
  return requestClient.post<ScheduleDTO>('/schedules', data);
}

/** 更新日程 */
export function updateSchedule(id: number, params: UpdateScheduleCommand) {
  return requestClient.put<ScheduleDTO>(`/schedules/${id}`, null, { params });
}

/** 删除日程 */
export function deleteSchedule(id: number) {
  return requestClient.delete<void>(`/schedules/${id}`);
}

/** 取消日程 */
export function cancelSchedule(id: number) {
  return requestClient.post<void>(`/schedules/${id}/cancel`);
}

/** 获取用户某天的日程 */
export function getSchedulesByUserAndDate(userId: number, date: string) {
  return requestClient.get<ScheduleDTO[]>(
    `/schedules/user/${userId}/date/${date}`,
  );
}

/** 获取我今天的日程 */
export function getMyTodaySchedules() {
  return requestClient.get<ScheduleDTO[]>('/schedules/my/today');
}

/** 获取我近期的日程 */
export function getMyUpcomingSchedules(days: number = 7, limit: number = 10) {
  return requestClient.get<ScheduleDTO[]>('/schedules/my/upcoming', {
    params: { days, limit },
  });
}
