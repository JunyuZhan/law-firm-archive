import type {
  MeetingBooking,
  MeetingBookingQueryParams,
  MeetingRoom,
  MeetingRoomQueryParams,
  PageResult,
} from './types';

import { requestClient } from '#/api/request';

/**
 * 获取所有会议室列表
 */
export function fetchMeetingRoomList(
  params?: MeetingRoomQueryParams,
): Promise<MeetingRoom[]> {
  return requestClient.get('/admin/meeting-room', { params });
}

/**
 * 获取可用会议室
 */
export function getAvailableMeetingRooms(): Promise<MeetingRoom[]> {
  return requestClient.get('/admin/meeting-room/available');
}

/**
 * 创建会议室
 */
export function createMeetingRoom(data: {
  capacity?: number;
  equipment?: string;
  location?: string;
  name: string;
}): Promise<MeetingRoom> {
  return requestClient.post('/admin/meeting-room', data);
}

/**
 * 更新会议室
 */
export function updateMeetingRoom(
  id: number,
  data: {
    capacity?: number;
    equipment?: string;
    location?: string;
    name?: string;
  },
): Promise<MeetingRoom> {
  return requestClient.put(`/admin/meeting-room/${id}`, data);
}

/**
 * 删除会议室
 */
export function deleteMeetingRoom(id: number): Promise<void> {
  return requestClient.delete(`/admin/meeting-room/${id}`);
}

/**
 * 更新会议室状态
 */
export function updateMeetingRoomStatus(
  id: number,
  status: string,
): Promise<void> {
  return requestClient.put(`/admin/meeting-room/${id}/status`, null, {
    params: { status },
  });
}

/**
 * 获取预约列表
 */
export function fetchBookingList(
  params: MeetingBookingQueryParams,
): Promise<PageResult<MeetingBooking>> {
  return requestClient.get('/admin/meeting-room/bookings', { params });
}

/**
 * 预约会议室
 */
export function bookMeetingRoom(data: {
  endTime: string;
  participants?: number[];
  roomId: number;
  startTime: string;
  title: string;
}): Promise<MeetingBooking> {
  return requestClient.post('/admin/meeting-room/bookings', data);
}

/**
 * 取消预约
 */
export function cancelBooking(id: number): Promise<void> {
  return requestClient.post(`/admin/meeting-room/bookings/${id}/cancel`);
}

/**
 * 获取会议室某日预约情况
 */
export function getRoomDayBookings(
  roomId: number,
  date: string,
): Promise<MeetingBooking[]> {
  return requestClient.get(`/admin/meeting-room/${roomId}/bookings/day`, {
    params: { date },
  });
}

/**
 * 获取我的会议预约
 */
export function getMyBookings(): Promise<MeetingBooking[]> {
  return requestClient.get('/admin/meeting-room/bookings/my');
}

/**
 * 获取会议室日程视图
 */
export function getMeetingRoomSchedule(
  roomId: number,
  startDate: string,
  endDate: string,
): Promise<MeetingBooking[]> {
  return requestClient.get(`/admin/meeting-room/${roomId}/schedule`, {
    params: { startDate, endDate },
  });
}

/**
 * 获取所有会议室日程视图
 */
export function getAllRoomsSchedule(
  startDate: string,
  endDate: string,
): Promise<Record<number, MeetingBooking[]>> {
  return requestClient.get('/admin/meeting-room/schedule/all', {
    params: { startDate, endDate },
  });
}
