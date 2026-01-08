import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface NotificationDTO {
  id: number;
  title: string;
  content: string;
  type: string;
  typeName: string;
  senderId?: number;
  senderName?: string;
  receiverId: number;
  isRead: boolean;
  readAt?: string;
  businessType?: string;
  businessId?: number;
  createdAt: string;
}

export interface NotificationQueryDTO {
  pageNum?: number;
  pageSize?: number;
  type?: string;
  isRead?: boolean;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// ==================== API ====================

/** 获取我的通知列表 */
export function getMyNotifications(params?: NotificationQueryDTO) {
  return requestClient.get<PageResult<NotificationDTO>>('/system/notification', { params });
}

/** 获取未读数量 */
export function getUnreadCount() {
  return requestClient.get<number>('/system/notification/unread-count');
}

/** 标记为已读 */
export function markAsRead(id: number) {
  return requestClient.post<void>(`/system/notification/${id}/read`);
}

/** 全部标记为已读 */
export function markAllAsRead() {
  return requestClient.post<void>('/system/notification/read-all');
}

/** 删除通知 */
export function deleteNotification(id: number) {
  return requestClient.delete<void>(`/system/notification/${id}`);
}
