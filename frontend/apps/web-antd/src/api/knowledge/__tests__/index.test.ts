import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  CaseLibraryQuery,
  CreateArticleCommand,
  CreateCaseLibraryCommand,
  CreateLawRegulationCommand,
  KnowledgeArticleQuery,
  LawRegulationQuery,
} from '../types';
import {
  archiveArticle,
  collectArticle,
  collectCase,
  collectLawRegulation,
  createArticle,
  createCase,
  createLawRegulation,
  deleteArticle,
  deleteCase,
  deleteLawRegulation,
  getArticleDetail,
  getArticleList,
  getCaseCategoryTree,
  getCaseDetail,
  getCaseList,
  getLawCategoryTree,
  getLawRegulationDetail,
  getLawRegulationList,
  getMyCollectedArticles,
  getMyCollectedCases,
  getMyCollectedRegulations,
  getMyArticles,
  likeArticle,
  markLawRegulationRepealed,
  publishArticle,
  uncollectArticle,
  uncollectCase,
  uncollectLawRegulation,
  updateArticle,
  updateCase,
  updateLawRegulation,
} from '../index';

// Mock requestClient
vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Knowledge API', () => {
  let mockGet: ReturnType<typeof vi.fn>;
  let mockPost: ReturnType<typeof vi.fn>;
  let mockPut: ReturnType<typeof vi.fn>;
  let mockDelete: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    const { requestClient } = await import('#/api/request');
    mockGet = vi.mocked(requestClient.get);
    mockPost = vi.mocked(requestClient.post);
    mockPut = vi.mocked(requestClient.put);
    mockDelete = vi.mocked(requestClient.delete);
    vi.clearAllMocks();
  });

  describe('Law Regulation Management', () => {
    it('should get law category tree', async () => {
      const mockResponse = [
        {
          id: 1,
          name: 'Civil Law',
          children: [],
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getLawCategoryTree();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/law/categories');
    });

    it('should get law regulation list', async () => {
      const params: LawRegulationQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getLawRegulationList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/law', { params });
    });

    it('should get law regulation detail', async () => {
      const regulationId = 1;
      const mockResponse = {
        id: regulationId,
        title: 'Test Regulation',
        content: 'Regulation content',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getLawRegulationDetail(regulationId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/knowledge/law/${regulationId}`);
    });

    it('should create law regulation', async () => {
      const data: CreateLawRegulationCommand = {
        title: 'New Regulation',
        content: 'Regulation content',
        categoryId: 1,
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createLawRegulation(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/knowledge/law', data);
    });

    it('should update law regulation', async () => {
      const regulationId = 1;
      const data: CreateLawRegulationCommand = {
        title: 'Updated Regulation',
        content: 'Updated content',
      } as any;
      const mockResponse = { id: regulationId, ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateLawRegulation(regulationId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/knowledge/law/${regulationId}`, data);
    });

    it('should delete law regulation', async () => {
      const regulationId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteLawRegulation(regulationId);

      expect(mockDelete).toHaveBeenCalledWith(`/knowledge/law/${regulationId}`);
    });

    it('should collect law regulation', async () => {
      const regulationId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await collectLawRegulation(regulationId);

      expect(mockPost).toHaveBeenCalledWith(`/knowledge/law/${regulationId}/collect`);
    });

    it('should uncollect law regulation', async () => {
      const regulationId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await uncollectLawRegulation(regulationId);

      expect(mockDelete).toHaveBeenCalledWith(
        `/knowledge/law/${regulationId}/collect`,
      );
    });

    it('should get my collected regulations', async () => {
      const mockResponse = [
        {
          id: 1,
          title: 'Collected Regulation',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyCollectedRegulations();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/law/collected');
    });

    it('should mark law regulation as repealed', async () => {
      const regulationId = 1;
      const reason = 'Superseded by new law';
      const mockResponse = {
        id: regulationId,
        status: 'REPEALED',
        repealReason: reason,
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await markLawRegulationRepealed(regulationId, reason);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/knowledge/law/${regulationId}/mark-repealed`,
        null,
        { params: { reason } },
      );
    });
  });

  describe('Knowledge Article Management', () => {
    it('should get article list', async () => {
      const params: KnowledgeArticleQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getArticleList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/article', { params });
    });

    it('should get article detail', async () => {
      const articleId = 1;
      const mockResponse = {
        id: articleId,
        title: 'Test Article',
        content: 'Article content',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getArticleDetail(articleId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/knowledge/article/${articleId}`);
    });

    it('should create article', async () => {
      const data: CreateArticleCommand = {
        title: 'New Article',
        content: 'Article content',
        category: 'LEGAL_ANALYSIS',
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createArticle(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/knowledge/article', data);
    });

    it('should update article', async () => {
      const articleId = 1;
      const data: CreateArticleCommand = {
        title: 'Updated Article',
        content: 'Updated content',
      } as any;
      const mockResponse = { id: articleId, ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateArticle(articleId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/knowledge/article/${articleId}`, data);
    });

    it('should delete article', async () => {
      const articleId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteArticle(articleId);

      expect(mockDelete).toHaveBeenCalledWith(`/knowledge/article/${articleId}`);
    });

    it('should publish article', async () => {
      const articleId = 1;
      const mockResponse = {
        id: articleId,
        status: 'PUBLISHED',
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await publishArticle(articleId);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(`/knowledge/article/${articleId}/publish`);
    });

    it('should archive article', async () => {
      const articleId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await archiveArticle(articleId);

      expect(mockPost).toHaveBeenCalledWith(`/knowledge/article/${articleId}/archive`);
    });

    it('should like article', async () => {
      const articleId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await likeArticle(articleId);

      expect(mockPost).toHaveBeenCalledWith(`/knowledge/article/${articleId}/like`);
    });

    it('should get my articles', async () => {
      const mockResponse = [
        {
          id: 1,
          title: 'My Article',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyArticles();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/article/my');
    });

    it('should collect article', async () => {
      const articleId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await collectArticle(articleId);

      expect(mockPost).toHaveBeenCalledWith(`/knowledge/article/${articleId}/collect`);
    });

    it('should uncollect article', async () => {
      const articleId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await uncollectArticle(articleId);

      expect(mockDelete).toHaveBeenCalledWith(
        `/knowledge/article/${articleId}/collect`,
      );
    });

    it('should get my collected articles', async () => {
      const mockResponse = [
        {
          id: 1,
          title: 'Collected Article',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyCollectedArticles();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/article/collected');
    });
  });

  describe('Case Library Management', () => {
    it('should get case category tree', async () => {
      const mockResponse = [
        {
          id: 1,
          name: 'Civil Cases',
          children: [],
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCaseCategoryTree();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/case/categories');
    });

    it('should get case list', async () => {
      const params: CaseLibraryQuery = { pageNum: 1, pageSize: 10 };
      const mockResponse = { list: [], total: 0 };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCaseList(params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/case', { params });
    });

    it('should get case detail', async () => {
      const caseId = 1;
      const mockResponse = {
        id: caseId,
        title: 'Test Case',
        description: 'Case description',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getCaseDetail(caseId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/knowledge/case/${caseId}`);
    });

    it('should create case', async () => {
      const data: CreateCaseLibraryCommand = {
        title: 'New Case',
        description: 'Case description',
        categoryId: 1,
      } as any;
      const mockResponse = { id: 1, ...data };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createCase(data);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/knowledge/case', data);
    });

    it('should update case', async () => {
      const caseId = 1;
      const data: CreateCaseLibraryCommand = {
        title: 'Updated Case',
        description: 'Updated description',
      } as any;
      const mockResponse = { id: caseId, ...data };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateCase(caseId, data);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/knowledge/case/${caseId}`, data);
    });

    it('should delete case', async () => {
      const caseId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteCase(caseId);

      expect(mockDelete).toHaveBeenCalledWith(`/knowledge/case/${caseId}`);
    });

    it('should collect case', async () => {
      const caseId = 1;

      mockPost.mockResolvedValueOnce(undefined);

      await collectCase(caseId);

      expect(mockPost).toHaveBeenCalledWith(`/knowledge/case/${caseId}/collect`);
    });

    it('should uncollect case', async () => {
      const caseId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await uncollectCase(caseId);

      expect(mockDelete).toHaveBeenCalledWith(`/knowledge/case/${caseId}/collect`);
    });

    it('should get my collected cases', async () => {
      const mockResponse = [
        {
          id: 1,
          title: 'Collected Case',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getMyCollectedCases();

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/knowledge/case/collected');
    });
  });
});
