import type { CauseOfActionDTO, CauseTreeNodeDTO } from './types';

import { requestClient } from '#/api/request';

/**
 * 案由管理 API
 */

/** 案由类型 */
export type CauseType = 'CIVIL' | 'CRIMINAL' | 'ADMIN';

/** 案由类型选项 */
export interface CauseTypeOption {
  value: CauseType;
  label: string;
  description: string;
}

/** 获取民事案由树 */
export function getCivilCauseTree() {
  return requestClient.get<CauseTreeNodeDTO[]>('/causes/civil/tree');
}

/** 获取刑事罪名树 */
export function getCriminalChargeTree() {
  return requestClient.get<CauseTreeNodeDTO[]>('/causes/criminal/tree');
}

/** 获取行政案由树 */
export function getAdminCauseTree() {
  return requestClient.get<CauseTreeNodeDTO[]>('/causes/admin/tree');
}

/** 获取指定类型的案由树 */
export function getCauseTree(type: CauseType) {
  const pathMap = {
    CIVIL: '/causes/civil/tree',
    CRIMINAL: '/causes/criminal/tree',
    ADMIN: '/causes/admin/tree',
  };
  return requestClient.get<CauseTreeNodeDTO[]>(pathMap[type]);
}

/** 搜索案由 */
export function searchCauses(type: CauseType, keyword: string) {
  return requestClient.get<CauseOfActionDTO[]>('/causes/search', {
    params: { type, keyword },
  });
}

/** 根据代码获取案由名称 */
export function getCauseName(code: string, type: CauseType = 'CIVIL') {
  return requestClient.get<string>('/causes/name', {
    params: { code, type },
  });
}

/** 获取所有案由类型选项 */
export function getCauseTypeOptions(): CauseTypeOption[] {
  return [
    { value: 'CIVIL', label: '民事案由', description: '民事案件相关案由' },
    { value: 'CRIMINAL', label: '刑事罪名', description: '刑事案件相关罪名' },
    { value: 'ADMIN', label: '行政案由', description: '行政案件相关案由' },
  ];
}
