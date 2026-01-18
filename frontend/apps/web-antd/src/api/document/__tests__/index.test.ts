import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  CreateDocumentCommand,
  DocumentQuery,
  OnlyOfficeConfig,
  UpdateDocumentCommand,
  UploadNewVersionCommand,
} from '../index';
import {
  batchDownloadDocuments,
  checkDocumentEditSupport,
  createDocument,
  createFolder,
  deleteDocument,
  downloadDocument,
  downloadDocumentsAsZip,
  getDocumentAccessLogs,
  getDocumentDetail,
  getDocumentEditConfig,
  getDocumentList,
  getDocumentPreviewConfig,
  getDocumentPreviewUrl,
  getDocumentThumbnailUrl,
  getDocumentVersions,
  getDocumentsByMatter,
  moveDocument,
  previewDocument,
  reorderDocuments,
  shareDocument,
  updateDocument,
  uploadFile,
  uploadFiles,
  uploadNewVersion,
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

describe('Document API', () => {
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

  describe('getDocumentList', () => {
    it('should fetch document list with query parameters', async () => {
      const query: DocumentQuery = {
        name: 'test',
        matterId: 1,
        pageNum: 1,
        pageSize: 10,
      };
      const mockResponse = {
        list: [
          {
            id: 1,
            name: 'test-doc',
            title: 'Test Document',
            matterId: 1,
          },
        ],
        total: 1,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentList(query);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/document', { params: query });
    });

    it('should handle empty query parameters', async () => {
      const mockResponse = {
        list: [],
        total: 0,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentList({});

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith('/document', { params: {} });
    });
  });

  describe('getDocumentsByMatter', () => {
    it('should fetch documents by matter ID', async () => {
      const matterId = 1;
      const mockResponse = [
        {
          id: 1,
          name: 'doc1',
          matterId: 1,
        },
        {
          id: 2,
          name: 'doc2',
          matterId: 1,
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentsByMatter(matterId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/document/matter/${matterId}`);
    });
  });

  describe('getDocumentDetail', () => {
    it('should fetch document detail by ID', async () => {
      const documentId = 1;
      const mockResponse = {
        id: documentId,
        name: 'test-doc',
        title: 'Test Document',
        matterId: 1,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentDetail(documentId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/document/${documentId}`);
    });
  });

  describe('createDocument', () => {
    it('should create a new document', async () => {
      const command: CreateDocumentCommand = {
        title: 'New Document',
        matterId: 1,
        filePath: '/path/to/file.pdf',
        fileName: 'file.pdf',
        fileSize: 1024,
        fileType: 'pdf',
      };
      const mockResponse = {
        id: 1,
        ...command,
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await createDocument(command);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith('/document', command);
    });
  });

  describe('updateDocument', () => {
    it('should update an existing document', async () => {
      const documentId = 1;
      const command: UpdateDocumentCommand = {
        id: documentId,
        title: 'Updated Title',
        description: 'Updated description',
      };
      const mockResponse = {
        id: documentId,
        ...command,
      };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await updateDocument(documentId, command);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(`/document/${documentId}`, command);
    });
  });

  describe('deleteDocument', () => {
    it('should delete a document', async () => {
      const documentId = 1;

      mockDelete.mockResolvedValueOnce(undefined);

      await deleteDocument(documentId);

      expect(mockDelete).toHaveBeenCalledWith(`/document/${documentId}`);
    });
  });

  describe('uploadNewVersion', () => {
    it('should upload a new version of a document', async () => {
      const documentId = 1;
      const command: UploadNewVersionCommand = {
        documentId,
        filePath: '/path/to/new-version.pdf',
        fileName: 'new-version.pdf',
        fileSize: 2048,
        fileType: 'pdf',
      };
      const mockResponse = {
        id: documentId,
        ...command,
        version: 2,
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await uploadNewVersion(documentId, command);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        `/document/${documentId}/versions`,
        command,
      );
    });
  });

  describe('getDocumentVersions', () => {
    it('should fetch all versions of a document', async () => {
      const documentId = 1;
      const mockResponse = [
        {
          id: documentId,
          version: 1,
          fileName: 'v1.pdf',
        },
        {
          id: documentId,
          version: 2,
          fileName: 'v2.pdf',
        },
      ];

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentVersions(documentId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(`/document/${documentId}/versions`);
    });
  });

  describe('downloadDocument', () => {
    it('should download a document as blob', async () => {
      const documentId = 1;
      const mockBlob = new Blob(['file content'], { type: 'application/pdf' });

      mockGet.mockResolvedValueOnce(mockBlob);

      const result = await downloadDocument(documentId);

      expect(result).toEqual(mockBlob);
      expect(mockGet).toHaveBeenCalledWith(`/document/${documentId}/download`, {
        responseType: 'blob',
      });
    });
  });

  describe('getDocumentPreviewConfig', () => {
    it('should fetch preview config for OnlyOffice', async () => {
      const documentId = 1;
      const mockConfig: OnlyOfficeConfig = {
        supported: true,
        documentId,
        documentServerUrl: 'https://docserver.example.com',
        document: {
          fileType: 'pdf',
          key: 'test-key',
          title: 'Test Document',
          url: 'https://example.com/doc.pdf',
          permissions: {
            comment: true,
            download: true,
            edit: false,
            print: true,
            review: true,
          },
        },
        editorConfig: {
          lang: 'zh-CN',
          mode: 'view',
        },
      };

      mockGet.mockResolvedValueOnce(mockConfig);

      const result = await getDocumentPreviewConfig(documentId);

      expect(result).toEqual(mockConfig);
      expect(mockGet).toHaveBeenCalledWith(`/document/${documentId}/preview`);
    });
  });

  describe('getDocumentEditConfig', () => {
    it('should fetch edit config for OnlyOffice', async () => {
      const documentId = 1;
      const mockConfig: OnlyOfficeConfig = {
        supported: true,
        documentId,
        documentServerUrl: 'https://docserver.example.com',
        document: {
          fileType: 'docx',
          key: 'test-key',
          title: 'Test Document',
          url: 'https://example.com/doc.docx',
          permissions: {
            comment: true,
            download: true,
            edit: true,
            print: true,
            review: true,
          },
        },
        editorConfig: {
          lang: 'zh-CN',
          mode: 'edit',
        },
      };

      mockGet.mockResolvedValueOnce(mockConfig);

      const result = await getDocumentEditConfig(documentId);

      expect(result).toEqual(mockConfig);
      expect(mockGet).toHaveBeenCalledWith(`/document/${documentId}/edit`);
    });
  });

  describe('checkDocumentEditSupport', () => {
    it('should check if document supports editing', async () => {
      const documentId = 1;
      const mockResponse = {
        canEdit: true,
        canPreview: true,
        documentId,
        fileName: 'test.docx',
        fileType: 'docx',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await checkDocumentEditSupport(documentId);

      expect(result).toEqual(mockResponse);
      expect(result.canEdit).toBe(true);
      expect(mockGet).toHaveBeenCalledWith(
        `/document/${documentId}/edit-support`,
      );
    });
  });

  describe('getDocumentPreviewUrl', () => {
    it('should fetch preview URL with signed token', async () => {
      const documentId = 1;
      const mockResponse = {
        documentId,
        expires: Date.now() + 3600000,
        fileName: 'test.pdf',
        fileType: 'pdf',
        mimeType: 'application/pdf',
        previewUrl: 'https://example.com/preview?token=abc123',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentPreviewUrl(documentId);

      expect(result).toEqual(mockResponse);
      expect(result.previewUrl).toBe('https://example.com/preview?token=abc123');
      expect(mockGet).toHaveBeenCalledWith(
        `/document/${documentId}/preview-url`,
      );
    });
  });

  describe('getDocumentThumbnailUrl', () => {
    it('should fetch thumbnail URL', async () => {
      const documentId = 1;
      const mockResponse = {
        documentId,
        fileName: 'test.pdf',
        fileType: 'pdf',
        hasThumbnail: true,
        thumbnailUrl: 'https://example.com/thumbnail.jpg',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentThumbnailUrl(documentId);

      expect(result).toEqual(mockResponse);
      expect(result.hasThumbnail).toBe(true);
      expect(mockGet).toHaveBeenCalledWith(`/document/${documentId}/thumbnail`);
    });
  });

  describe('shareDocument', () => {
    it('should share a document and return share link', async () => {
      const documentId = 1;
      const mockResponse = 'https://example.com/share/abc123';

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await shareDocument(documentId);

      expect(result).toBe(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(`/document/${documentId}/share`);
    });
  });

  describe('previewDocument', () => {
    it('should return preview URL on success', async () => {
      const documentId = 1;
      const mockResponse = {
        documentId,
        previewUrl: 'https://example.com/preview?token=abc123',
        expires: Date.now() + 3600000,
        fileName: 'test.pdf',
        fileType: 'pdf',
        mimeType: 'application/pdf',
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await previewDocument(documentId);

      expect(result).toBe('https://example.com/preview?token=abc123');
    });

    it('should return null on error', async () => {
      const documentId = 1;

      mockGet.mockRejectedValueOnce(new Error('Network error'));

      const result = await previewDocument(documentId);

      expect(result).toBeNull();
    });

    it('should return null when previewUrl is missing', async () => {
      const documentId = 1;
      const mockResponse = {
        documentId,
        previewUrl: null,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await previewDocument(documentId);

      expect(result).toBeNull();
    });
  });

  describe('createFolder', () => {
    it('should create a folder', async () => {
      const folderData = {
        matterId: 1,
        name: 'New Folder',
        parentFolder: '/',
      };

      mockPost.mockResolvedValueOnce(undefined);

      await createFolder(folderData);

      expect(mockPost).toHaveBeenCalledWith('/document/folder', folderData);
    });
  });

  describe('uploadFile', () => {
    it('should upload a single file', async () => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const options = {
        matterId: 1,
        folder: '/test',
        description: 'Test file',
      };
      const mockResponse = {
        id: 1,
        fileName: 'test.pdf',
        fileSize: file.size,
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await uploadFile(file, options);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/document/upload',
        expect.any(FormData),
        {
          headers: { 'Content-Type': 'multipart/form-data' },
        },
      );
    });

    it('should upload file without optional options', async () => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const mockResponse = {
        id: 1,
        fileName: 'test.pdf',
      };

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await uploadFile(file);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/document/upload',
        expect.any(FormData),
        {
          headers: { 'Content-Type': 'multipart/form-data' },
        },
      );
    });
  });

  describe('uploadFiles', () => {
    it('should upload multiple files', async () => {
      const files = [
        new File(['content1'], 'test1.pdf', { type: 'application/pdf' }),
        new File(['content2'], 'test2.pdf', { type: 'application/pdf' }),
      ];
      const options = {
        matterId: 1,
        sourceType: 'USER_UPLOADED',
      };
      const mockResponse = [
        { id: 1, fileName: 'test1.pdf' },
        { id: 2, fileName: 'test2.pdf' },
      ];

      mockPost.mockResolvedValueOnce(mockResponse);

      const result = await uploadFiles(files, options);

      expect(result).toEqual(mockResponse);
      expect(mockPost).toHaveBeenCalledWith(
        '/document/upload/batch',
        expect.any(FormData),
        {
          headers: { 'Content-Type': 'multipart/form-data' },
        },
      );
    });
  });

  describe('getDocumentAccessLogs', () => {
    it('should fetch document access logs', async () => {
      const documentId = 1;
      const params = {
        actionType: 'VIEW',
        pageNum: 1,
        pageSize: 10,
      };
      const mockResponse = {
        list: [
          {
            id: 1,
            actionType: 'VIEW',
            userId: 1,
            userName: 'Test User',
          },
        ],
        total: 1,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentAccessLogs(documentId, params);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/document/${documentId}/access-logs`,
        { params },
      );
    });

    it('should fetch logs without params', async () => {
      const documentId = 1;
      const mockResponse = {
        list: [],
        total: 0,
      };

      mockGet.mockResolvedValueOnce(mockResponse);

      const result = await getDocumentAccessLogs(documentId);

      expect(result).toEqual(mockResponse);
      expect(mockGet).toHaveBeenCalledWith(
        `/document/${documentId}/access-logs`,
        { params: undefined },
      );
    });
  });

  describe('moveDocument', () => {
    it('should move document to target directory', async () => {
      const documentId = 1;
      const targetDossierItemId = 2;
      const mockResponse = {
        id: documentId,
        dossierItemId: targetDossierItemId,
      };

      mockPut.mockResolvedValueOnce(mockResponse);

      const result = await moveDocument(documentId, targetDossierItemId);

      expect(result).toEqual(mockResponse);
      expect(mockPut).toHaveBeenCalledWith(
        `/document/${documentId}/move`,
        null,
        {
          params: { targetDossierItemId },
        },
      );
    });
  });

  describe('reorderDocuments', () => {
    it('should reorder documents', async () => {
      const documentIds = [3, 1, 2];

      mockPut.mockResolvedValueOnce(undefined);

      await reorderDocuments(documentIds);

      expect(mockPut).toHaveBeenCalledWith('/document/reorder', documentIds);
    });
  });

  describe('batchDownloadDocuments', () => {
    it('should download multiple documents as zip', async () => {
      const documentIds = [1, 2, 3];
      const mockBlob = new Blob(['zip content'], { type: 'application/zip' });

      mockPost.mockResolvedValueOnce(mockBlob);

      const result = await batchDownloadDocuments(documentIds);

      expect(result).toEqual(mockBlob);
      expect(mockPost).toHaveBeenCalledWith(
        '/document/batch-download',
        documentIds,
        {
          responseType: 'blob',
        },
      );
    });
  });

  describe('downloadDocumentsAsZip', () => {
    it('should download documents as zip and trigger browser download', async () => {
      const documentIds = [1, 2, 3];
      const filename = 'documents.zip';
      const mockBlob = new Blob(['zip content'], { type: 'application/zip' });

      // Mock URL.createObjectURL and revokeObjectURL
      const createObjectURLSpy = vi
        .spyOn(window.URL, 'createObjectURL')
        .mockReturnValue('blob:http://localhost/test');
      const revokeObjectURLSpy = vi.spyOn(window.URL, 'revokeObjectURL');

      // Mock document.createElement and link methods
      const mockLink = {
        href: '',
        download: '',
        click: vi.fn(),
        remove: vi.fn(),
      };
      const createElementSpy = vi
        .spyOn(document, 'createElement')
        .mockReturnValue(mockLink as any);
      const appendChildSpy = vi.spyOn(document.body, 'append');

      mockPost.mockResolvedValueOnce(mockBlob);

      await downloadDocumentsAsZip(documentIds, filename);

      expect(mockPost).toHaveBeenCalledWith(
        '/document/batch-download',
        documentIds,
        {
          responseType: 'blob',
        },
      );
      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(createElementSpy).toHaveBeenCalledWith('a');
      expect(mockLink.download).toBe(filename);
      expect(mockLink.click).toHaveBeenCalled();
      expect(mockLink.remove).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalled();

      createObjectURLSpy.mockRestore();
      revokeObjectURLSpy.mockRestore();
      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
    });

    it('should use default filename when not provided', async () => {
      const documentIds = [1, 2];
      const mockBlob = new Blob(['zip content'], { type: 'application/zip' });

      const createObjectURLSpy = vi
        .spyOn(window.URL, 'createObjectURL')
        .mockReturnValue('blob:http://localhost/test');
      const revokeObjectURLSpy = vi.spyOn(window.URL, 'revokeObjectURL');

      const mockLink = {
        href: '',
        download: '',
        click: vi.fn(),
        remove: vi.fn(),
      };
      vi.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      vi.spyOn(document.body, 'append');

      mockPost.mockResolvedValueOnce(mockBlob);

      await downloadDocumentsAsZip(documentIds);

      expect(mockLink.download).toMatch(/^documents_\d+\.zip$/);

      createObjectURLSpy.mockRestore();
      revokeObjectURLSpy.mockRestore();
    });
  });
});
