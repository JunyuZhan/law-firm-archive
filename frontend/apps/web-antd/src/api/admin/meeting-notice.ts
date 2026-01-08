/**
 * 会议通知 API
 */
import { requestClient } from '#/api/request';

// ========== API 函数 ==========

/** 发送会议通知 */
export function sendMeetingNotice(bookingId: number) {
  return requestClient.post(`/admin/meeting-notices/${bookingId}/send`);
}

/** 批量发送即将开始的会议通知 */
export function sendUpcomingMeetingNotices(minutesBefore: number = 30) {
  return requestClient.post<number>('/admin/meeting-notices/send-upcoming', null, {
    params: { minutesBefore },
  });
}

