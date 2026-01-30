/**
 * 卷宗管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========

/** 卷宗目录模板 */
export interface DossierTemplate {
  id: number;
  name: string;
  caseType: string;
  description?: string;
  isDefault: boolean;
  createdAt?: string;
}

/** 卷宗目录模板项 */
export interface DossierTemplateItem {
  id: number;
  templateId: number;
  parentId: number;
  name: string;
  itemType: 'FILE' | 'FOLDER';
  fileCategory?: string;
  sortOrder: number;
  required: boolean;
  description?: string;
}

/** 项目卷宗目录项 */
export interface MatterDossierItem {
  id: number;
  matterId: number;
  parentId: number;
  name: string;
  itemType: 'FILE' | 'FOLDER';
  fileCategory?: string;
  sortOrder: number;
  documentCount: number;
  createdAt?: string;
  updatedAt?: string;
}

// ========== 卷宗目录 API ==========

/** 获取项目卷宗目录 */
export function getMatterDossierItems(matterId: number) {
  return requestClient.get<MatterDossierItem[]>(`/matter/${matterId}/dossier`);
}

/** 初始化项目卷宗目录 */
export function initMatterDossier(matterId: number) {
  return requestClient.post<MatterDossierItem[]>(
    `/matter/${matterId}/dossier/init`,
  );
}

/** 添加自定义目录项 */
export function addDossierItem(
  matterId: number,
  params: {
    itemType?: string;
    name: string;
    parentId?: number;
  },
) {
  return requestClient.post<MatterDossierItem>(
    `/matter/${matterId}/dossier/item`,
    params,
  );
}

/** 更新目录项 */
export function updateDossierItem(
  matterId: number,
  itemId: number,
  params: {
    name?: string;
    sortOrder?: number;
  },
) {
  return requestClient.put<MatterDossierItem>(
    `/matter/${matterId}/dossier/item/${itemId}`,
    params,
  );
}

/** 删除目录项 */
export function deleteDossierItem(matterId: number, itemId: number) {
  return requestClient.delete(`/matter/${matterId}/dossier/item/${itemId}`);
}

/** 调整目录项排序 */
export function reorderDossierItems(matterId: number, itemIds: number[]) {
  return requestClient.put(`/matter/${matterId}/dossier/reorder`, itemIds);
}

// ========== 自动归档 API ==========

/** 重新生成授权委托书结果 */
export interface RegeneratePOAResult {
  success: boolean;
  message: string;
  templateUsed: boolean;
  templateName?: string;
  hint?: string;
}

/** 重新生成授权委托书（强制覆盖已有版本） */
export function regeneratePowerOfAttorney(matterId: number) {
  return requestClient.post<RegeneratePOAResult>(
    `/matter/${matterId}/dossier/regenerate/power-of-attorney`,
  );
}

/** 触发自动归档 */
export function triggerAutoArchive(matterId: number, contractId?: number) {
  const params = contractId ? `?contractId=${contractId}` : '';
  return requestClient.post<{ message: string }>(
    `/matter/${matterId}/dossier/auto-archive${params}`,
  );
}

// ========== 卷宗模板 API ==========

/** 获取所有卷宗模板 */
export function getAllDossierTemplates() {
  return requestClient.get<DossierTemplate[]>('/dossier/template');
}

/** 创建卷宗模板 */
export function createDossierTemplate(
  data: Omit<DossierTemplate, 'id' | 'isDefault'>,
) {
  return requestClient.post<DossierTemplate>('/dossier/template', data);
}

/** 更新卷宗模板 */
export function updateDossierTemplate(
  id: number,
  data: Partial<DossierTemplate>,
) {
  return requestClient.put<DossierTemplate>(`/dossier/template/${id}`, data);
}

/** 删除卷宗模板 */
export function deleteDossierTemplate(id: number) {
  return requestClient.delete(`/dossier/template/${id}`);
}

/** 获取模板目录项 */
export function getDossierTemplateItems(templateId: number) {
  return requestClient.get<DossierTemplateItem[]>(
    `/dossier/template/${templateId}/items`,
  );
}

/** 添加模板目录项 */
export function addDossierTemplateItem(
  templateId: number,
  data: Partial<DossierTemplateItem>,
) {
  return requestClient.post<DossierTemplateItem>(
    `/dossier/template/${templateId}/items`,
    data,
  );
}

/** 更新模板目录项 */
export function updateDossierTemplateItem(
  itemId: number,
  data: Partial<DossierTemplateItem>,
) {
  return requestClient.put<DossierTemplateItem>(
    `/dossier/template/items/${itemId}`,
    data,
  );
}

/** 删除模板目录项 */
export function deleteDossierTemplateItem(itemId: number) {
  return requestClient.delete(`/dossier/template/items/${itemId}`);
}

// ========== 文件分类常量 ==========

export const FILE_CATEGORY_OPTIONS = [
  { label: '证据材料', value: 'EVIDENCE' },
  { label: '诉讼文书', value: 'PLEADING' },
  { label: '合同文件', value: 'CONTRACT' },
  { label: '往来函件', value: 'CORRESPONDENCE' },
  { label: '会见记录', value: 'MEETING' },
  { label: '裁判文书', value: 'JUDGMENT' },
  { label: '工作日志', value: 'WORKLOG' },
  { label: '其他', value: 'OTHER' },
];

export const CASE_TYPE_OPTIONS = [
  { label: '刑事案件', value: 'CRIMINAL' },
  { label: '民事案件', value: 'CIVIL' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '其他非诉', value: 'NON_LITIGATION' },
];
