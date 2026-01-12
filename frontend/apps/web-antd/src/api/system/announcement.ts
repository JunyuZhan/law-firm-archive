import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface AnnouncementDTO {
  id: number;
  title: string;
  content: string;
  type: string;
  typeName?: string;
  priority?: number;
  priorityName?: string;
  status: string;
  statusName?: string;
  publisherId?: number;
  publisherName?: string;
  publishedAt?: string;
  expiredAt?: string;
  viewCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AnnouncementQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
  type?: string;
}

export interface CreateAnnouncementCommand {
  title: string;
  content: string;
  type: string;
  priority?: number;
  expiredAt?: string;
}

// 本地分页结果类型
interface AnnouncementPageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// 公告类型选项
export const ANNOUNCEMENT_TYPE_OPTIONS = [
  { label: '通知', value: 'NOTICE', color: 'blue' },
  { label: '公告', value: 'ANNOUNCEMENT', color: 'green' },
  { label: '紧急', value: 'URGENT', color: 'red' },
  { label: '系统', value: 'SYSTEM', color: 'purple' },
];

// 公告状态选项
export const ANNOUNCEMENT_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT', color: 'default' },
  { label: '已发布', value: 'PUBLISHED', color: 'green' },
  { label: '已撤回', value: 'WITHDRAWN', color: 'orange' },
  { label: '已过期', value: 'EXPIRED', color: 'red' },
];

// 优先级选项
export const ANNOUNCEMENT_PRIORITY_OPTIONS = [
  { label: '普通', value: 0 },
  { label: '重要', value: 1 },
  { label: '紧急', value: 2 },
];

// ==================== API ====================

/** 分页查询公告 */
export function getAnnouncementList(params: AnnouncementQuery) {
  return requestClient.get<AnnouncementPageResult<AnnouncementDTO>>(
    '/system/announcement',
    { params },
  );
}

/** 获取有效公告列表（首页展示用） */
export function getValidAnnouncements(limit: number = 10) {
  return requestClient.get<AnnouncementDTO[]>('/system/announcement/valid', {
    params: { limit },
  });
}

/** 获取公告详情 */
export function getAnnouncementById(id: number) {
  return requestClient.get<AnnouncementDTO>(`/system/announcement/${id}`);
}

/** 创建公告 */
export function createAnnouncement(data: CreateAnnouncementCommand) {
  return requestClient.post<AnnouncementDTO>('/system/announcement', data);
}

/** 更新公告 */
export function updateAnnouncement(
  id: number,
  data: CreateAnnouncementCommand,
) {
  return requestClient.put<AnnouncementDTO>(`/system/announcement/${id}`, data);
}

/** 发布公告 */
export function publishAnnouncement(id: number) {
  return requestClient.post<AnnouncementDTO>(
    `/system/announcement/${id}/publish`,
  );
}

/** 撤回公告 */
export function withdrawAnnouncement(id: number) {
  return requestClient.post<void>(`/system/announcement/${id}/withdraw`);
}

/** 删除公告 */
export function deleteAnnouncement(id: number) {
  return requestClient.delete<void>(`/system/announcement/${id}`);
}
