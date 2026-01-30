import type {
  CaseCategoryDTO,
  CaseLibraryDTO,
  CaseLibraryQuery,
  CaseStudyNoteDTO,
  CreateArticleCommand,
  CreateCaseLibraryCommand,
  CreateCaseStudyNoteCommand,
  CreateLawRegulationCommand,
  KnowledgeArticleDTO,
  KnowledgeArticleQuery,
  LawCategoryDTO,
  LawRegulationDTO,
  LawRegulationQuery,
  PageResult,
} from './types';

/**
 * 知识库模块 API
 */
import { requestClient } from '#/api/request';

// ========== 法律法规 API ==========

/** 获取法规分类树 */
export function getLawCategoryTree() {
  return requestClient.get<LawCategoryDTO[]>('/knowledge/law/categories');
}

/** 获取法规列表 */
export function getLawRegulationList(params: LawRegulationQuery) {
  return requestClient.get<PageResult<LawRegulationDTO>>('/knowledge/law', {
    params,
  });
}

/** 获取法规详情 */
export function getLawRegulationDetail(id: number) {
  return requestClient.get<LawRegulationDTO>(`/knowledge/law/${id}`);
}

/** 创建法规 */
export function createLawRegulation(data: CreateLawRegulationCommand) {
  return requestClient.post<LawRegulationDTO>('/knowledge/law', data);
}

/** 更新法规 */
export function updateLawRegulation(
  id: number,
  data: CreateLawRegulationCommand,
) {
  return requestClient.put<LawRegulationDTO>(`/knowledge/law/${id}`, data);
}

/** 删除法规 */
export function deleteLawRegulation(id: number) {
  return requestClient.delete(`/knowledge/law/${id}`);
}

/** 收藏法规 */
export function collectLawRegulation(id: number) {
  return requestClient.post(`/knowledge/law/${id}/collect`);
}

/** 取消收藏法规 */
export function uncollectLawRegulation(id: number) {
  return requestClient.delete(`/knowledge/law/${id}/collect`);
}

/** 获取我的收藏法规 */
export function getMyCollectedRegulations() {
  return requestClient.get<LawRegulationDTO[]>('/knowledge/law/collected');
}

/** 标注法规失效 */
export function markLawRegulationRepealed(id: number, reason?: string) {
  return requestClient.post<LawRegulationDTO>(
    `/knowledge/law/${id}/mark-repealed`,
    null,
    {
      params: { reason },
    },
  );
}

// ========== 知识文章 API ==========

/** 获取文章列表 */
export function getArticleList(params: KnowledgeArticleQuery) {
  return requestClient.get<PageResult<KnowledgeArticleDTO>>(
    '/knowledge/article',
    { params },
  );
}

/** 获取文章详情 */
export function getArticleDetail(id: number) {
  return requestClient.get<KnowledgeArticleDTO>(`/knowledge/article/${id}`);
}

/** 创建文章 */
export function createArticle(data: CreateArticleCommand) {
  return requestClient.post<KnowledgeArticleDTO>('/knowledge/article', data);
}

/** 更新文章 */
export function updateArticle(id: number, data: CreateArticleCommand) {
  return requestClient.put<KnowledgeArticleDTO>(
    `/knowledge/article/${id}`,
    data,
  );
}

/** 删除文章 */
export function deleteArticle(id: number) {
  return requestClient.delete(`/knowledge/article/${id}`);
}

/** 发布文章 */
export function publishArticle(id: number) {
  return requestClient.post<KnowledgeArticleDTO>(
    `/knowledge/article/${id}/publish`,
  );
}

/** 归档文章 */
export function archiveArticle(id: number) {
  return requestClient.post(`/knowledge/article/${id}/archive`);
}

/** 点赞文章 */
export function likeArticle(id: number) {
  return requestClient.post(`/knowledge/article/${id}/like`);
}

/** 获取我的文章 */
export function getMyArticles() {
  return requestClient.get<KnowledgeArticleDTO[]>('/knowledge/article/my');
}

/** 收藏文章 */
export function collectArticle(id: number) {
  return requestClient.post(`/knowledge/article/${id}/collect`);
}

/** 取消收藏文章 */
export function uncollectArticle(id: number) {
  return requestClient.delete(`/knowledge/article/${id}/collect`);
}

/** 获取我的收藏文章 */
export function getMyCollectedArticles() {
  return requestClient.get<KnowledgeArticleDTO[]>(
    '/knowledge/article/collected',
  );
}

// ========== 案例库 API ==========

/** 获取案例分类树 */
export function getCaseCategoryTree() {
  return requestClient.get<CaseCategoryDTO[]>('/knowledge/case/categories');
}

/** 获取案例列表 */
export function getCaseList(params: CaseLibraryQuery) {
  return requestClient.get<PageResult<CaseLibraryDTO>>('/knowledge/case', {
    params,
  });
}

/** 获取案例详情 */
export function getCaseDetail(id: number) {
  return requestClient.get<CaseLibraryDTO>(`/knowledge/case/${id}`);
}

/** 创建案例 */
export function createCase(data: CreateCaseLibraryCommand) {
  return requestClient.post<CaseLibraryDTO>('/knowledge/case', data);
}

/** 更新案例 */
export function updateCase(id: number, data: CreateCaseLibraryCommand) {
  return requestClient.put<CaseLibraryDTO>(`/knowledge/case/${id}`, data);
}

/** 删除案例 */
export function deleteCase(id: number) {
  return requestClient.delete(`/knowledge/case/${id}`);
}

/** 收藏案例 */
export function collectCase(id: number) {
  return requestClient.post(`/knowledge/case/${id}/collect`);
}

/** 取消收藏案例 */
export function uncollectCase(id: number) {
  return requestClient.delete(`/knowledge/case/${id}/collect`);
}

/** 获取我的收藏案例 */
export function getMyCollectedCases() {
  return requestClient.get<CaseLibraryDTO[]>('/knowledge/case/collected');
}

// ========== 案例学习笔记 API ==========

/** 获取我对某案例的学习笔记 */
export function getMyCaseNote(caseId: number) {
  return requestClient.get<CaseStudyNoteDTO>(
    `/knowledge/case-study/note/${caseId}`,
  );
}

/** 获取我的所有学习笔记 */
export function getMyStudyNotes() {
  return requestClient.get<CaseStudyNoteDTO[]>('/knowledge/case-study/note/my');
}

/** 获取某案例的所有学习笔记 */
export function getCaseStudyNotes(caseId: number) {
  return requestClient.get<CaseStudyNoteDTO[]>(
    `/knowledge/case-study/note/case/${caseId}`,
  );
}

/** 保存学习笔记 */
export function saveCaseStudyNote(data: CreateCaseStudyNoteCommand) {
  return requestClient.post<CaseStudyNoteDTO>(
    '/knowledge/case-study/note',
    data,
  );
}

/** 删除学习笔记 */
export function deleteCaseStudyNote(caseId: number) {
  return requestClient.delete(`/knowledge/case-study/note/${caseId}`);
}

// 导出类型
export type * from './types';
