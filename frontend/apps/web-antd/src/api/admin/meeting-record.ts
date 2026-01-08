/**
 * 会议记录 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface MeetingRecordDTO {
  id: number;
  recordNo?: string;
  bookingId?: number;
  roomId: number;
  roomName?: string;
  title: string;
  meetingDate: string;
  startTime: string;
  endTime?: string;
  organizerId?: number;
  organizerName?: string;
  attendees?: string;
  content?: string;
  decisions?: string;
  actionItems?: string;
  attachmentUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateMeetingRecordCommand {
  bookingId?: number;
  roomId: number;
  title: string;
  meetingDate: string;
  startTime: string;
  endTime?: string;
  content?: string;
  decisions?: string;
  actionItems?: string;
  attachmentUrl?: string;
}

// ========== API 函数 ==========

/** 创建会议记录 */
export function createMeetingRecord(data: CreateMeetingRecordCommand) {
  return requestClient.post<MeetingRecordDTO>('/admin/meeting-records', data);
}

/** 根据预约创建会议记录 */
export function createMeetingRecordFromBooking(bookingId: number, data: CreateMeetingRecordCommand) {
  return requestClient.post<MeetingRecordDTO>(`/admin/meeting-records/from-booking/${bookingId}`, data);
}

/** 获取会议记录详情 */
export function getMeetingRecordDetail(id: number) {
  return requestClient.get<MeetingRecordDTO>(`/admin/meeting-records/${id}`);
}

/** 查询会议室的会议记录 */
export function getMeetingRecordsByRoom(roomId: number) {
  return requestClient.get<MeetingRecordDTO[]>(`/admin/meeting-records/room/${roomId}`);
}

/** 查询指定日期范围的会议记录 */
export function getMeetingRecordsByDateRange(startDate: string, endDate: string) {
  return requestClient.get<MeetingRecordDTO[]>('/admin/meeting-records/range', {
    params: { startDate, endDate },
  });
}

