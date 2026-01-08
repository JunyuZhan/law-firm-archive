/**
 * 外出管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface GoOutRecordDTO {
  id: number;
  recordNo?: string;
  userId?: number;
  userName?: string;
  outTime: string;
  expectedReturnTime?: string;
  actualReturnTime?: string;
  location?: string;
  reason: string;
  companions?: string;
  status?: string;
  statusName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GoOutCommand {
  outTime: string;
  expectedReturnTime?: string;
  location?: string;
  reason: string;
  companions?: string;
}

// ========== API 函数 ==========

/** 外出登记 */
export function registerGoOut(data: GoOutCommand) {
  return requestClient.post<GoOutRecordDTO>('/admin/go-out/register', data);
}

/** 登记返回 */
export function registerReturn(id: number) {
  return requestClient.post<GoOutRecordDTO>(`/admin/go-out/${id}/return`);
}

/** 查询我的外出记录 */
export function getMyGoOutRecords() {
  return requestClient.get<GoOutRecordDTO[]>('/admin/go-out/my');
}

/** 查询指定日期范围的外出记录 */
export function getGoOutRecordsByDateRange(startDate: string, endDate: string) {
  return requestClient.get<GoOutRecordDTO[]>('/admin/go-out/range', {
    params: { startDate, endDate },
  });
}

/** 查询当前外出的记录 */
export function getCurrentGoOut() {
  return requestClient.get<GoOutRecordDTO[]>('/admin/go-out/current');
}

