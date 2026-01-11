/**
 * 知识库模块类型定义
 */

// ========== 法律法规 ==========
export interface LawRegulationDTO {
  id: number;
  name: string;
  lawType: string;
  lawTypeName?: string;
  categoryId?: number;
  categoryName?: string;
  issuer?: string;
  issueDate?: string;
  effectiveDate?: string;
  status: string;
  statusName?: string;
  content?: string;
  isRepealed?: boolean;
  repealedDate?: string;
  repealedReason?: string;
  collected?: boolean;
  createdAt?: string;
}

export interface LawRegulationQuery {
  name?: string;
  lawType?: string;
  categoryId?: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateLawRegulationCommand {
  name: string;
  lawType: string;
  categoryId?: number;
  issuer?: string;
  issueDate?: string;
  effectiveDate?: string;
  content?: string;
}

export interface LawCategoryDTO {
  id: number;
  name: string;
  parentId?: number;
  children?: LawCategoryDTO[];
}

// ========== 知识文章 ==========
export interface KnowledgeArticleDTO {
  id: number;
  title: string;
  content?: string;
  category?: string;
  categoryName?: string;
  authorId?: number;
  authorName?: string;
  status: string;
  statusName?: string;
  publishTime?: string;
  views?: number;
  likes?: number;
  collected?: boolean;
  createdAt?: string;
}

export interface KnowledgeArticleQuery {
  title?: string;
  category?: string;
  authorId?: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateArticleCommand {
  title: string;
  content: string;
  category?: string;
}

// ========== 案例库 ==========
export interface CaseLibraryDTO {
  id: number;
  name: string;
  caseType?: string;
  caseTypeName?: string;
  categoryId?: number;
  categoryName?: string;
  court?: string;
  judgmentDate?: string;
  result?: string;
  resultName?: string;
  lawyerId?: number;
  lawyerName?: string;
  referenceValue?: string;
  referenceValueName?: string;
  summary?: string;
  collected?: boolean;
  createdAt?: string;
}

export interface CaseLibraryQuery {
  name?: string;
  caseType?: string;
  categoryId?: number;
  court?: string;
  lawyerId?: number;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateCaseLibraryCommand {
  name: string;
  caseType?: string;
  categoryId?: number;
  court?: string;
  judgmentDate?: string;
  result?: string;
  lawyerId?: number;
  referenceValue?: string;
  summary?: string;
}

export interface CaseCategoryDTO {
  id: number;
  name: string;
  parentId?: number;
  children?: CaseCategoryDTO[];
}

// ========== 通用分页结果 ==========
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
}
