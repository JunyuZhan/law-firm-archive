package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateDocumentCommand;
import com.lawfirm.application.document.command.UpdateDocumentCommand;
import com.lawfirm.application.document.command.UploadNewVersionCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.DocumentQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.entity.DocumentVersion;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.infrastructure.external.file.ThumbnailService;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import com.lawfirm.infrastructure.persistence.mapper.DocumentVersionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DocumentAppService 单元测试
 *
 * 测试文档应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentAppService 文档服务测试")
class DocumentAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_DEPT_ID = 10L;
    private static final Long TEST_MATTER_ID = 100L;
    private static final Long TEST_DOC_ID = 1000L;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentCategoryRepository categoryRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentVersionMapper versionMapper;

    @Mock
    private MinioService minioService;

    @Mock
    private ThumbnailService thumbnailService;

    @Mock
    private MatterAppService matterAppService;

    @InjectMocks
    private DocumentAppService documentAppService;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Manually inject matterAppService since it uses @Lazy setter injection
        documentAppService.setMatterAppService(matterAppService);
    }

    @Nested
    @DisplayName("分页查询文档测试")
    class ListDocumentsTests {

        @Test
        @DisplayName("应该分页查询文档列表")
        void listDocuments_shouldReturnPagedResult() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

                DocumentQueryDTO query = new DocumentQueryDTO();
                query.setPageNum(1);
                query.setPageSize(10);

                Document doc = createTestDocument(1L, "测试文档.pdf", "pdf");
                Page<Document> page = new Page<>(1, 10);
                page.setRecords(List.of(doc));
                page.setTotal(1);

                // For "ALL" data scope, getAccessibleMatterIds returns null (meaning access to all)
                when(matterAppService.getAccessibleMatterIds(eq("ALL"), eq(TEST_USER_ID), eq(TEST_DEPT_ID)))
                        .thenReturn(null);
                when(documentMapper.selectDocumentPage(any(Page.class), any(), any(),
                        any(), any(), any(), any(), any(), any())).thenReturn(page);
                // 使用lenient，因为只有当文档有categoryId时才会调用此方法
                lenient().when(categoryRepository.listByIds(any())).thenReturn(new ArrayList<>());

                // When
                PageResult<DocumentDTO> result = documentAppService.listDocuments(query);

                // Then
                assertThat(result.getRecords()).hasSize(1);
                assertThat(result.getRecords().get(0).getTitle()).isEqualTo("测试文档.pdf");
            }
        }

        @Test
        @DisplayName("无权限时应返回空结果")
        void listDocuments_shouldReturnEmptyWhenNoPermission() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("SELF");
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

                DocumentQueryDTO query = new DocumentQueryDTO();
                query.setPageNum(1);
                query.setPageSize(10);

                when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                        .thenReturn(Collections.emptyList());

                // When
                PageResult<DocumentDTO> result = documentAppService.listDocuments(query);

                // Then
                assertThat(result.getRecords()).isEmpty();
                assertThat(result.getTotal()).isEqualTo(0);
            }
        }
    }

    @Nested
    @DisplayName("创建文档测试")
    class CreateDocumentTests {

        @Test
        @DisplayName("应该成功创建文档")
        void createDocument_shouldReturnDocumentDTO() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                CreateDocumentCommand command = new CreateDocumentCommand();
                command.setTitle("测试文档.pdf");
                command.setMatterId(TEST_MATTER_ID);
                command.setFileName("test.pdf");
                command.setFilePath("/path/to/test.pdf");
                command.setFileSize(1024L);
                command.setFileType("pdf");
                command.setMimeType("application/pdf");

                doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
                when(documentRepository.save(any(Document.class))).thenReturn(true);
                when(versionMapper.insert(any(DocumentVersion.class))).thenReturn(1);

                // When
                DocumentDTO result = documentAppService.createDocument(command);

                // Then
                assertThat(result.getTitle()).isEqualTo("测试文档.pdf");
                assertThat(result.getVersion()).isEqualTo(1);
                assertThat(result.getIsLatest()).isTrue();
                verify(documentRepository).save(any(Document.class));
            }
        }
    }

    @Nested
    @DisplayName("更新文档测试")
    class UpdateDocumentTests {

        @Test
        @DisplayName("应该成功更新文档")
        void updateDocument_shouldReturnDocumentDTO() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Document doc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");

                UpdateDocumentCommand command = new UpdateDocumentCommand();
                command.setId(TEST_DOC_ID);
                command.setTitle("更新后的文档.pdf");
                command.setSecurityLevel("CONFIDENTIAL");

                when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
                when(documentRepository.updateById(any(Document.class))).thenReturn(true);

                // When
                DocumentDTO result = documentAppService.updateDocument(command);

                // Then
                assertThat(result.getTitle()).isEqualTo("更新后的文档.pdf");
                assertThat(result.getSecurityLevel()).isEqualTo("CONFIDENTIAL");
            }
        }

        @Test
        @DisplayName("文档不存在时应抛出异常")
        void updateDocument_shouldThrowException_whenNotFound() {
            // Given
            UpdateDocumentCommand command = new UpdateDocumentCommand();
            command.setId(999L);

            when(documentRepository.getByIdOrThrow(999L, "文档不存在"))
                    .thenThrow(new BusinessException("文档不存在"));

            // When & Then
            assertThatThrownBy(() -> documentAppService.updateDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("文档不存在");
        }
    }

    @Nested
    @DisplayName("上传新版本测试")
    class UploadNewVersionTests {

        @Test
        @DisplayName("应该成功上传新版本")
        void uploadNewVersion_shouldCreateNewVersion() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                Document oldDoc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
                oldDoc.setVersion(1);

                UploadNewVersionCommand command = new UploadNewVersionCommand();
                command.setDocumentId(TEST_DOC_ID);
                command.setFileName("test_v2.pdf");
                command.setFilePath("/path/to/test_v2.pdf");
                command.setFileSize(2048L);
                command.setChangeNote("更新内容");

                when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(oldDoc);
                when(documentRepository.updateById(any(Document.class))).thenReturn(true);
                when(documentRepository.save(any(Document.class))).thenReturn(true);
                when(versionMapper.insert(any(DocumentVersion.class))).thenReturn(1);

                // When
                DocumentDTO result = documentAppService.uploadNewVersion(command);

                // Then
                assertThat(result.getVersion()).isEqualTo(2);
                assertThat(result.getIsLatest()).isTrue();
                verify(documentRepository).updateById(oldDoc); // 标记旧版本
                verify(documentRepository).save(any(Document.class)); // 保存新版本
            }
        }
    }

    @Nested
    @DisplayName("获取文档详情测试")
    class GetDocumentByIdTests {

        @Test
        @DisplayName("应该获取文档详情")
        void getDocumentById_shouldReturnDocument() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);

            // When
            DocumentDTO result = documentAppService.getDocumentById(TEST_DOC_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TEST_DOC_ID);
            assertThat(result.getTitle()).isEqualTo("测试文档.pdf");
        }

        @Test
        @DisplayName("文档不存在时应抛出异常")
        void getDocumentById_shouldThrowException_whenNotFound() {
            // Given
            when(documentRepository.getByIdOrThrow(999L, "文档不存在"))
                    .thenThrow(new BusinessException("文档不存在"));

            // When & Then
            assertThatThrownBy(() -> documentAppService.getDocumentById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("文档不存在");
        }
    }

    @Nested
    @DisplayName("获取文档版本测试")
    class GetDocumentVersionsTests {

        @Test
        @DisplayName("应该获取文档所有版本")
        void getDocumentVersions_shouldReturnAllVersions() {
            // Given
            Document doc1 = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            doc1.setVersion(1);
            Document doc2 = createTestDocument(TEST_DOC_ID + 1, "测试文档.pdf", "pdf");
            doc2.setVersion(2);
            doc2.setParentDocId(TEST_DOC_ID);

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc1);
            when(documentRepository.findAllVersions(TEST_DOC_ID)).thenReturn(List.of(doc1, doc2));

            // When
            List<DocumentDTO> result = documentAppService.getDocumentVersions(TEST_DOC_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getVersion()).isEqualTo(1);
            assertThat(result.get(1).getVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("删除文档测试")
    class DeleteDocumentTests {

        @Test
        @DisplayName("应该成功删除文档")
        void deleteDocument_shouldDeleteDocument() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(documentRepository.softDelete(TEST_DOC_ID)).thenReturn(true);

            // When
            documentAppService.deleteDocument(TEST_DOC_ID);

            // Then
            verify(documentRepository).softDelete(TEST_DOC_ID);
        }

        @Test
        @DisplayName("文档不存在时应抛出异常")
        void deleteDocument_shouldThrowException_whenNotFound() {
            // Given
            when(documentRepository.getByIdOrThrow(999L, "文档不存在"))
                    .thenThrow(new BusinessException("文档不存在"));

            // When & Then
            assertThatThrownBy(() -> documentAppService.deleteDocument(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("文档不存在");
        }
    }

    @Nested
    @DisplayName("归档文档测试")
    class ArchiveDocumentTests {

        @Test
        @DisplayName("应该成功归档文档")
        void archiveDocument_shouldArchiveDocument() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(documentRepository.updateById(any(Document.class))).thenReturn(true);

            // When
            documentAppService.archiveDocument(TEST_DOC_ID);

            // Then
            assertThat(doc.getStatus()).isEqualTo("ARCHIVED");
            verify(documentRepository).updateById(doc);
        }
    }

    @Nested
    @DisplayName("按案件查询文档测试")
    class GetDocumentsByMatterTests {

        @Test
        @DisplayName("应该获取案件文档列表")
        void getDocumentsByMatter_shouldReturnDocuments() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

                Document doc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");

                when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                        .thenReturn(null);
                when(documentRepository.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(doc));

                // When
                List<DocumentDTO> result = documentAppService.getDocumentsByMatter(TEST_MATTER_ID);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("测试文档.pdf");
            }
        }

        @Test
        @DisplayName("无权限时应抛出异常")
        void getDocumentsByMatter_shouldThrowException_whenNoPermission() {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("SELF");
                mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

                when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                        .thenReturn(List.of(200L)); // 不包含TEST_MATTER_ID

                // When & Then
                assertThatThrownBy(() -> documentAppService.getDocumentsByMatter(TEST_MATTER_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("无权访问该项目的文档");
            }
        }
    }

    @Nested
    @DisplayName("上传文件测试")
    class UploadFileTests {

        @Test
        @DisplayName("应该成功上传文件")
        void uploadFile_shouldUploadFile() throws Exception {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class);
                 MockedStatic<FileValidator> mockedValidator = mockStatic(FileValidator.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                
                // Mock FileValidator to return success ValidationResult
                FileValidator.ValidationResult successResult = FileValidator.ValidationResult.success();
                mockedValidator.when(() -> FileValidator.validate(any(MultipartFile.class))).thenReturn(successResult);

                MultipartFile file = mock(MultipartFile.class);
                when(file.isEmpty()).thenReturn(false);
                when(file.getOriginalFilename()).thenReturn("test.pdf");
                when(file.getContentType()).thenReturn("application/pdf");
                when(file.getSize()).thenReturn(1024L);
                // 使用lenient，因为FileValidator已被mock，可能不会读取InputStream
                lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

                // Mock MinIO uploadFile方法（三个参数：InputStream, String, String）
                when(minioService.uploadFile(any(InputStream.class), anyString(), anyString()))
                        .thenReturn("http://localhost:9000/law-firm/matters/M_100/2026-01/20260127_abc123_test.pdf");
                // 使用lenient，因为缩略图服务只在支持的文件类型时才会调用
                lenient().when(thumbnailService.supportsThumbnail(anyString())).thenReturn(false);
                when(documentRepository.save(any(Document.class))).thenReturn(true);
                when(versionMapper.insert(any(DocumentVersion.class))).thenReturn(1);

                // When
                DocumentDTO result = documentAppService.uploadFile(file, TEST_MATTER_ID, null, "描述", null);

                // Then
                assertThat(result.getFileName()).isEqualTo("test.pdf");
                assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
                verify(minioService).uploadFile(any(InputStream.class), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("文件验证失败时应抛出异常")
        void uploadFile_shouldThrowException_whenValidationFails() {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);

            // 设置文件名为不支持的类型来触发验证失败
            when(file.getOriginalFilename()).thenReturn("test.exe");

            // When & Then
            assertThatThrownBy(() -> documentAppService.uploadFile(file, TEST_MATTER_ID, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("禁止上传可执行文件");
        }

        @Test
        @DisplayName("空文件时应抛出异常")
        void uploadFile_shouldThrowException_whenFileEmpty() {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> documentAppService.uploadFile(file, TEST_MATTER_ID, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("请选择要上传的文件");
        }
    }

    @Nested
    @DisplayName("版本回退测试")
    class RollbackToVersionTests {

        @Test
        @DisplayName("应该成功回退到指定版本")
        void rollbackToVersion_shouldRollback() {
            // Given
            Document currentDoc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            currentDoc.setVersion(2); // 当前版本是2
            currentDoc.setIsLatest(true);

            Document targetDoc = createTestDocument(TEST_DOC_ID + 1, "测试文档.pdf", "pdf");
            targetDoc.setVersion(1); // 回退到版本1
            targetDoc.setIsLatest(false); // 旧版本不是最新的

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(currentDoc);
            when(documentRepository.findAllVersions(TEST_DOC_ID)).thenReturn(List.of(targetDoc, currentDoc));
            when(documentRepository.updateById(any(Document.class))).thenReturn(true);
            when(documentRepository.save(any(Document.class))).thenReturn(true);
            when(versionMapper.insert(any(DocumentVersion.class))).thenReturn(1);

            // When
            DocumentDTO result = documentAppService.rollbackToVersion(TEST_DOC_ID, 1);

            // Then
            assertThat(result.getVersion()).isEqualTo(3); // 新版本号 = 当前最大版本 + 1
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("目标版本不存在时应抛出异常")
        void rollbackToVersion_shouldThrowException_whenVersionNotFound() {
            // Given
            Document currentDoc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            currentDoc.setVersion(3);

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(currentDoc);
            when(documentRepository.findAllVersions(TEST_DOC_ID)).thenReturn(List.of(currentDoc));

            // When & Then
            assertThatThrownBy(() -> documentAppService.rollbackToVersion(TEST_DOC_ID, 2))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("目标版本不存在: v2");
        }
    }

    @Nested
    @DisplayName("移动文档测试")
    class MoveDocumentTests {

        @Test
        @DisplayName("应该成功移动文档")
        void moveDocument_shouldMoveDocument() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "测试文档.pdf", "pdf");
            doc.setDossierItemId(1L);

            when(documentRepository.getById(TEST_DOC_ID)).thenReturn(doc);
            when(documentRepository.updateById(any(Document.class))).thenReturn(true);
            doNothing().when(documentMapper).updateDossierItemDocCount(anyLong(), anyInt());

            // When
            DocumentDTO result = documentAppService.moveDocument(TEST_DOC_ID, 2L);

            // Then
            assertThat(result.getDossierItemId()).isEqualTo(2L);
            verify(documentMapper).updateDossierItemDocCount(eq(1L), eq(-1));
            verify(documentMapper).updateDossierItemDocCount(eq(2L), eq(1));
        }

        @Test
        @DisplayName("文档不存在时应抛出异常")
        void moveDocument_shouldThrowException_whenDocumentNotFound() {
            // Given
            when(documentRepository.getById(TEST_DOC_ID)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> documentAppService.moveDocument(TEST_DOC_ID, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("文档不存在");
        }
    }

    @Nested
    @DisplayName("重新排序文档测试")
    class ReorderDocumentsTests {

        @Test
        @DisplayName("应该成功重新排序文档")
        void reorderDocuments_shouldReorder() {
            // Given
            Document doc1 = createTestDocument(1L, "文档1.pdf", "pdf");
            Document doc2 = createTestDocument(2L, "文档2.pdf", "pdf");

            when(documentRepository.getById(1L)).thenReturn(doc1);
            when(documentRepository.getById(2L)).thenReturn(doc2);
            when(documentRepository.updateById(any(Document.class))).thenReturn(true);

            // When
            documentAppService.reorderDocuments(List.of(2L, 1L)); // 反转顺序

            // Then
            assertThat(doc1.getDisplayOrder()).isEqualTo(2);
            assertThat(doc2.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("空列表时不执行操作")
        void reorderDocuments_shouldDoNothing_whenListEmpty() {
            // When
            documentAppService.reorderDocuments(Collections.emptyList());

            // Then
            verify(documentRepository, never()).updateById(any(Document.class));
        }
    }

    @Nested
    @DisplayName("缩略图测试")
    class ThumbnailTests {

        @Test
        @DisplayName("应该成功生成缩略图")
        void generateThumbnailForDocument_shouldGenerate() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.jpg", "jpg");
            doc.setFilePath("/path/to/test.jpg");

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(thumbnailService.supportsThumbnail("test.jpg")).thenReturn(true);
            when(thumbnailService.generateThumbnailFromUrl(anyString(), anyString()))
                    .thenReturn("/thumbnail/path/test.jpg");
            when(documentRepository.updateById(any(Document.class))).thenReturn(true);

            // When
            String result = documentAppService.generateThumbnailForDocument(TEST_DOC_ID);

            // Then
            assertThat(result).isEqualTo("/thumbnail/path/test.jpg");
        }

        @Test
        @DisplayName("已有缩略图时直接返回")
        void generateThumbnailForDocument_shouldReturnExisting_whenExists() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.jpg", "jpg");
            doc.setThumbnailUrl("/existing/thumbnail.jpg");

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);

            // When
            String result = documentAppService.generateThumbnailForDocument(TEST_DOC_ID);

            // Then
            assertThat(result).isEqualTo("/existing/thumbnail.jpg");
            verify(thumbnailService, never()).generateThumbnailFromUrl(anyString(), anyString());
        }

        @Test
        @DisplayName("不支持缩略图时返回null")
        void generateThumbnailForDocument_shouldReturnNull_whenNotSupported() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.docx", "docx");

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(thumbnailService.supportsThumbnail("test.docx")).thenReturn(false);

            // When
            String result = documentAppService.generateThumbnailForDocument(TEST_DOC_ID);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("批量上传文件测试")
    class UploadFilesTests {

        @Test
        @DisplayName("应该成功批量上传文件")
        void uploadFiles_shouldUploadMultipleFiles() throws Exception {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class);
                 MockedStatic<FileValidator> mockedValidator = mockStatic(FileValidator.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                
                FileValidator.ValidationResult successResult = FileValidator.ValidationResult.success();
                mockedValidator.when(() -> FileValidator.validate(any(MultipartFile.class))).thenReturn(successResult);

                MultipartFile file1 = mock(MultipartFile.class);
                when(file1.isEmpty()).thenReturn(false);
                when(file1.getOriginalFilename()).thenReturn("test1.pdf");
                when(file1.getContentType()).thenReturn("application/pdf");
                when(file1.getSize()).thenReturn(1024L);
                lenient().when(file1.getInputStream()).thenReturn(new ByteArrayInputStream("content1".getBytes()));

                MultipartFile file2 = mock(MultipartFile.class);
                when(file2.isEmpty()).thenReturn(false);
                when(file2.getOriginalFilename()).thenReturn("test2.pdf");
                when(file2.getContentType()).thenReturn("application/pdf");
                when(file2.getSize()).thenReturn(2048L);
                lenient().when(file2.getInputStream()).thenReturn(new ByteArrayInputStream("content2".getBytes()));

                MultipartFile[] files = {file1, file2};

                // Mock MinIO uploadFile方法（三个参数：InputStream, String, String）
                when(minioService.uploadFile(any(InputStream.class), anyString(), anyString()))
                        .thenReturn("http://localhost:9000/law-firm/matters/M_100/2026-01/20260127_abc123_test1.pdf")
                        .thenReturn("http://localhost:9000/law-firm/matters/M_100/2026-01/20260127_def456_test2.pdf");
                lenient().when(thumbnailService.supportsThumbnail(anyString())).thenReturn(false);
                when(documentRepository.save(any(Document.class))).thenReturn(true);
                when(versionMapper.insert(any(DocumentVersion.class))).thenReturn(1);

                // When
                List<DocumentDTO> result = documentAppService.uploadFiles(files, TEST_MATTER_ID, null, "批量上传", null, null);

                // Then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getFileName()).isEqualTo("test1.pdf");
                assertThat(result.get(1).getFileName()).isEqualTo("test2.pdf");
                verify(minioService, times(2)).uploadFile(any(InputStream.class), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("空文件数组时应抛出异常")
        void uploadFiles_shouldThrowException_whenEmptyArray() {
            // When & Then
            assertThatThrownBy(() -> documentAppService.uploadFiles(new MultipartFile[0], TEST_MATTER_ID, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("请选择要上传的文件");
        }

        @Test
        @DisplayName("所有文件都为空时应抛出异常")
        void uploadFiles_shouldThrowException_whenAllFilesEmpty() {
            // Given
            MultipartFile file1 = mock(MultipartFile.class);
            when(file1.isEmpty()).thenReturn(true);
            MultipartFile file2 = mock(MultipartFile.class);
            when(file2.isEmpty()).thenReturn(true);
            MultipartFile[] files = {file1, file2};

            // When & Then
            assertThatThrownBy(() -> documentAppService.uploadFiles(files, TEST_MATTER_ID, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("没有有效的文件");
        }

        @Test
        @DisplayName("上传失败时应清理已上传的文件")
        void uploadFiles_shouldCleanupOnFailure() throws Exception {
            try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class);
                 MockedStatic<FileValidator> mockedValidator = mockStatic(FileValidator.class)) {
                // Given
                mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
                
                FileValidator.ValidationResult successResult = FileValidator.ValidationResult.success();
                mockedValidator.when(() -> FileValidator.validate(any(MultipartFile.class))).thenReturn(successResult);

                MultipartFile file1 = mock(MultipartFile.class);
                when(file1.isEmpty()).thenReturn(false);
                when(file1.getOriginalFilename()).thenReturn("test1.pdf");
                when(file1.getContentType()).thenReturn("application/pdf");
                when(file1.getSize()).thenReturn(1024L);
                lenient().when(file1.getInputStream()).thenReturn(new ByteArrayInputStream("content1".getBytes()));

                MultipartFile file2 = mock(MultipartFile.class);
                when(file2.isEmpty()).thenReturn(false);
                when(file2.getOriginalFilename()).thenReturn("test2.pdf");
                when(file2.getContentType()).thenReturn("application/pdf");
                when(file2.getSize()).thenReturn(2048L);
                lenient().when(file2.getInputStream()).thenReturn(new ByteArrayInputStream("content2".getBytes()));

                MultipartFile[] files = {file1, file2};

                // 第一个文件成功上传，第二个文件失败
                // 注意：uploadFile返回的是MinIO内部URL，但filePath使用的是buildFileUrl生成的URL
                // objectName是动态生成的（包含UUID），所以我们需要mock动态响应
                
                // Mock uploadFile返回MinIO内部URL（实际代码中newFileUrl）
                when(minioService.uploadFile(any(InputStream.class), anyString(), anyString()))
                        .thenAnswer(invocation -> {
                            String objName = invocation.getArgument(1);
                            return "http://localhost:9000/law-firm/" + objName;
                        });
                
                // Mock buildFileUrl返回filePath（实际代码中filePath使用buildFileUrl生成）
                when(minioService.buildFileUrl(anyString()))
                        .thenAnswer(invocation -> {
                            String objName = invocation.getArgument(0);
                            return "http://localhost:9000/law-firm/" + objName;
                        });
                
                // Mock getBucketName（用于设置bucketName字段）
                when(minioService.getBucketName()).thenReturn("law-firm");
                
                lenient().when(thumbnailService.supportsThumbnail(anyString())).thenReturn(false);
                
                // 第一个文件保存成功
                when(documentRepository.save(any(Document.class)))
                        .thenReturn(true)  // 第一个文件
                        .thenThrow(new RuntimeException("Database error")); // 第二个文件失败
                when(versionMapper.insert(any(DocumentVersion.class)))
                        .thenReturn(1)  // 第一个文件的版本
                        .thenThrow(new RuntimeException("Database error")); // 第二个文件失败
                
                // Mock extractObjectName用于从URL提取objectName（清理时使用）
                when(minioService.extractObjectName(anyString()))
                        .thenAnswer(invocation -> {
                            String url = invocation.getArgument(0);
                            if (url != null && url.contains("/law-firm/")) {
                                return url.substring(url.indexOf("/law-firm/") + 10);
                            }
                            return null;
                        });
                
                // Mock deleteFile方法（接收objectName）
                doNothing().when(minioService).deleteFile(anyString());

                // When & Then
                assertThatThrownBy(() -> documentAppService.uploadFiles(files, TEST_MATTER_ID, null, null, null, null))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("批量上传失败");
                
                // 验证清理操作被调用（第一个文件已成功上传，第二个失败时应该清理第一个）
                // 代码会先调用extractObjectName从URL提取objectName，然后调用deleteFile
                // 由于objectName是动态生成的，我们只验证方法被调用了，不验证具体参数
                verify(minioService, atLeastOnce()).extractObjectName(anyString());
                verify(minioService, atLeastOnce()).deleteFile(anyString());
            }
        }
    }

    @Nested
    @DisplayName("获取缩略图URL测试")
    class GetThumbnailUrlTests {

        @Test
        @DisplayName("应该返回缩略图URL")
        void getThumbnailUrl_shouldReturnThumbnailUrl() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.jpg", "jpg");
            doc.setThumbnailUrl("/thumbnail/path/test.jpg");

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(minioService.getBrowserAccessibleUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            String result = documentAppService.getThumbnailUrl(TEST_DOC_ID);

            // Then
            assertThat(result).isEqualTo("/thumbnail/path/test.jpg");
        }

        @Test
        @DisplayName("没有缩略图时应生成并返回")
        void getThumbnailUrl_shouldGenerateWhenNotExists() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.jpg", "jpg");
            doc.setThumbnailUrl(null);
            doc.setFilePath("/path/to/test.jpg");

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(thumbnailService.supportsThumbnail("test.jpg")).thenReturn(true);
            when(thumbnailService.generateThumbnailFromUrl(anyString(), anyString()))
                    .thenReturn("/thumbnail/path/test.jpg");
            when(documentRepository.updateById(any(Document.class))).thenReturn(true);
            when(minioService.getBrowserAccessibleUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            String result = documentAppService.getThumbnailUrl(TEST_DOC_ID);

            // Then
            assertThat(result).isEqualTo("/thumbnail/path/test.jpg");
            verify(thumbnailService).generateThumbnailFromUrl(anyString(), anyString());
        }

        @Test
        @DisplayName("不支持缩略图时应返回null")
        void getThumbnailUrl_shouldReturnNull_whenNotSupported() {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.docx", "docx");
            doc.setThumbnailUrl(null);

            when(documentRepository.getByIdOrThrow(TEST_DOC_ID, "文档不存在")).thenReturn(doc);
            when(thumbnailService.supportsThumbnail("test.docx")).thenReturn(false);

            // When
            String result = documentAppService.getThumbnailUrl(TEST_DOC_ID);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("OnlyOffice保存测试")
    class SaveFromOnlyOfficeTests {

        @Test
        @DisplayName("网络异常时应抛出保存失败异常")
        void saveFromOnlyOffice_shouldThrowException_whenNetworkError() throws Exception {
            // Given
            Document doc = createTestDocument(TEST_DOC_ID, "test.docx", "docx");
            doc.setVersion(1);
            doc.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            when(documentRepository.getById(TEST_DOC_ID)).thenReturn(doc);

            // When & Then - 由于无法模拟URL类的网络行为，单元测试中会抛出网络异常
            assertThatThrownBy(() -> documentAppService.saveFromOnlyOffice(TEST_DOC_ID, "http://invalid-onlyoffice-host/download/url"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("保存文档失败");
        }

        @Test
        @DisplayName("文档不存在时应抛出异常")
        void saveFromOnlyOffice_shouldThrowException_whenDocumentNotFound() {
            // Given
            when(documentRepository.getById(TEST_DOC_ID)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> documentAppService.saveFromOnlyOffice(TEST_DOC_ID, "http://url"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("文档不存在: " + TEST_DOC_ID);
        }
    }

    // ========== 辅助方法 ==========

    private Document createTestDocument(Long id, String title, String fileType) {
        return Document.builder()
                .id(id)
                .docNo("DOC" + System.currentTimeMillis())
                .title(title)
                .fileName(title)
                .filePath("/path/to/" + title)
                .fileSize(1024L)
                .fileType(fileType)
                .mimeType("application/" + fileType)
                .version(1)
                .isLatest(true)
                .securityLevel("INTERNAL")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
