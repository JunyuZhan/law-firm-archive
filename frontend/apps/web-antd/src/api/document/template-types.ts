/**
 * 文档模板模块类型定义
 */

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
  matterId?: number;  // 可选，不传则为个人文书
  variables?: Record<string, any>;
  fileName?: string;
  dossierItemId?: number;  // 卷宗目录项ID
}

export interface PreviewTemplateCommand {
  templateId: number;
  matterId?: number;  // 可选，用于预览时自动填充项目相关变量
  variables?: Record<string, any>;
}

