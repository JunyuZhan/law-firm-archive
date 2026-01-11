import type { PageResult } from '../matter/types';
import type { DocumentDTO } from './types';

/**
 * 文档模板模块 API
 */
import { requestClient } from '#/api/request';

// ========== 文档模板类型定义 ==========
export interface DocumentTemplateDTO {
  id: number;
  name: string;
  categoryId?: number;
  categoryName?: string;
  templateType?: string;
  templateTypeName?: string;
  businessType?: string;
  businessTypeName?: string;
  content?: string;
  variables?: string[];
  creatorId?: number;
  creatorName?: string;
  useCount?: number;
  status: string;
  statusName?: string;
  description?: string;
  createdAt?: string;
}

export interface DocumentTemplateQuery {
  name?: string;
  categoryId?: number;
  templateType?: string;
  businessType?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateDocumentTemplateCommand {
  name: string;
  categoryId?: number;
  templateType: string;
  businessType?: string;
  content: string;
  description?: string;
}

export interface UpdateDocumentTemplateCommand {
  name?: string;
  categoryId?: number;
  templateType?: string;
  description?: string;
  status?: string;
}

export interface GenerateDocumentCommand {
  templateId: number;
  matterId?: number; // 可选，不传则为个人文书
  variables?: Record<string, any>;
  fileName?: string;
  dossierItemId?: number; // 卷宗目录项ID
}

export interface PreviewTemplateCommand {
  templateId: number;
  matterId?: number; // 可选，用于预览时自动填充项目相关变量
  variables?: Record<string, any>;
}

// ========== 文档模板 API ==========

/** 获取模板列表（需要 doc:template:list 权限） */
export function getTemplateList(params: DocumentTemplateQuery) {
  return requestClient.get<PageResult<DocumentTemplateDTO>>(
    '/document/template',
    { params },
  );
}

/** 获取启用的模板列表（公共接口，用于文书制作选择模板） */
export function getActiveTemplateList(params?: DocumentTemplateQuery) {
  return requestClient.get<PageResult<DocumentTemplateDTO>>(
    '/document/template/active',
    { params },
  );
}

/** 获取模板详情 */
export function getTemplateDetail(id: number) {
  return requestClient.get<DocumentTemplateDTO>(`/document/template/${id}`);
}

/** 创建模板 */
export function createTemplate(data: CreateDocumentTemplateCommand) {
  return requestClient.post<DocumentTemplateDTO>('/document/template', data);
}

/** 更新模板 */
export function updateTemplate(
  id: number,
  data: UpdateDocumentTemplateCommand,
) {
  return requestClient.put<DocumentTemplateDTO>(
    `/document/template/${id}`,
    data,
  );
}

/** 删除模板 */
export function deleteTemplate(id: number) {
  return requestClient.delete(`/document/template/${id}`);
}

/** 使用模板 */
export function useTemplate(id: number) {
  return requestClient.post(`/document/template/${id}/use`);
}

/** 从模板生成文档 */
export function generateDocument(data: GenerateDocumentCommand) {
  return requestClient.post<DocumentDTO>('/document/template/generate', data);
}

/** 预览模板 */
export function previewTemplate(data: PreviewTemplateCommand) {
  return requestClient.post<Record<string, any>>(
    '/document/template/preview',
    data,
  );
}

// 导出类型
export type * from './template-types';
