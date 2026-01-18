package com.lawfirm.interfaces.rest.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.service.DocAccessLogService;
import com.lawfirm.application.document.service.DocumentAppService;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.DocAccessLog;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.onlyoffice.OnlyOfficeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DocumentController 单元测试
 * 测试文档预览和编辑相关接口
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentController 文档控制器测试")
class DocumentControllerTest {

    private static final Long TEST_DOCUMENT_ID = 1L;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USERNAME = "admin";

    @Mock
    private DocumentAppService documentAppService;

    @Mock
    private DocAccessLogService accessLogService;

    @Mock
    private OnlyOfficeService onlyOfficeService;

    @Mock
    private MinioService minioService;

    private MockMvc mockMvc;
    private DocumentController controller;
    private ObjectMapper objectMapper;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        controller = new DocumentController(
                documentAppService,
                accessLogService,
                onlyOfficeService,
                minioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        // Mock SecurityUtils
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn(TEST_USERNAME);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("文档预览配置接口测试")
    class GetPreviewConfigTests {

        @Test
        @DisplayName("获取支持预览的文档配置 - 成功")
        void shouldReturnPreviewConfigForSupportedFile() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.docx", "http://minio:9000/law-firm/test.docx");
            String proxyUrl = "http://backend:8080/api/document/" + TEST_DOCUMENT_ID + "/file-proxy?token=test&expires=123456";
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);
            when(onlyOfficeService.isPreviewSupported("test.docx")).thenReturn(true);
            when(onlyOfficeService.buildFileUrlForDocument(TEST_DOCUMENT_ID)).thenReturn(proxyUrl);
            Map<String, Object> previewConfig = new HashMap<>();
            previewConfig.put("document", Map.of("key", "test-key"));
            when(onlyOfficeService.generateViewConfig(
                    anyString(), eq("test.docx"), eq(TEST_USER_ID), eq(TEST_USERNAME)))
                    .thenReturn(previewConfig);

            // When & Then
            mockMvc.perform(get("/document/{id}/preview", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.supported").value(true))
                    .andExpect(jsonPath("$.data.documentId").value(TEST_DOCUMENT_ID));

            verify(accessLogService).logAccess(eq(TEST_DOCUMENT_ID), eq(DocAccessLog.ACTION_VIEW), any());
        }

        @Test
        @DisplayName("获取不支持预览的文档配置 - 返回不支持信息")
        void shouldReturnNotSupportedForUnsupportedFile() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.zip", "http://minio:9000/law-firm/test.zip");
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);
            when(onlyOfficeService.isPreviewSupported("test.zip")).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/document/{id}/preview", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.supported").value(false))
                    .andExpect(jsonPath("$.data.message").value("该文件类型不支持在线预览"))
                    .andExpect(jsonPath("$.data.fileName").value("test.zip"));
        }

        @Test
        @DisplayName("文档不存在 - 返回错误")
        void shouldReturnErrorWhenDocumentNotFound() throws Exception {
            // Given
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/document/{id}/preview", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("文档不存在"));
        }
    }

    @Nested
    @DisplayName("文档编辑配置接口测试")
    class GetEditConfigTests {

        @Test
        @DisplayName("获取支持编辑的文档配置 - 成功")
        void shouldReturnEditConfigForSupportedFile() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.docx", "http://minio:9000/law-firm/test.docx");
            doc.setVersion(1);
            String proxyUrl = "http://backend:8080/api/document/" + TEST_DOCUMENT_ID + "/file-proxy?token=test&expires=123456";
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);
            when(onlyOfficeService.isSupported("test.docx")).thenReturn(true);
            when(onlyOfficeService.buildFileUrlForDocument(TEST_DOCUMENT_ID)).thenReturn(proxyUrl);
            Map<String, Object> editConfig = new HashMap<>();
            editConfig.put("document", Map.of("key", "test-key"));
            when(onlyOfficeService.generateEditConfig(
                    anyString(), eq("test.docx"), contains("doc_" + TEST_DOCUMENT_ID), eq(TEST_USER_ID), eq(TEST_USERNAME), anyString()))
                    .thenReturn(editConfig);

            // When & Then
            mockMvc.perform(get("/document/{id}/edit", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.supported").value(true))
                    .andExpect(jsonPath("$.data.documentId").value(TEST_DOCUMENT_ID));

            verify(accessLogService).logAccess(eq(TEST_DOCUMENT_ID), eq("EDIT"), any());
        }

        @Test
        @DisplayName("获取不支持编辑的文档配置 - 返回不支持信息")
        void shouldReturnNotSupportedForNonEditableFile() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.pdf", "http://minio:9000/law-firm/test.pdf");
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);
            when(onlyOfficeService.isSupported("test.pdf")).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/document/{id}/edit", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.supported").value(false))
                    .andExpect(jsonPath("$.data.message").value("该文件类型不支持在线编辑"))
                    .andExpect(jsonPath("$.data.fileName").value("test.pdf"));
        }
    }

    @Nested
    @DisplayName("OnlyOffice回调接口测试")
    class OnlyOfficeCallbackTests {

        @Test
        @DisplayName("回调状态2（保存文档）- 成功处理")
        void shouldHandleCallbackWithStatus2() throws Exception {
            // Given
            Map<String, Object> payload = Map.of(
                    "status", 2,
                    "url", "http://onlyoffice/download/url"
            );
            doNothing().when(documentAppService).saveFromOnlyOffice(eq(TEST_DOCUMENT_ID), anyString());

            // When & Then
            mockMvc.perform(post("/document/{id}/callback", TEST_DOCUMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0));

            verify(documentAppService).saveFromOnlyOffice(eq(TEST_DOCUMENT_ID), eq("http://onlyoffice/download/url"));
        }

        @Test
        @DisplayName("回调状态6（强制保存）- 成功处理")
        void shouldHandleCallbackWithStatus6() throws Exception {
            // Given
            Map<String, Object> payload = Map.of(
                    "status", 6,
                    "url", "http://onlyoffice/download/url"
            );
            doNothing().when(documentAppService).saveFromOnlyOffice(eq(TEST_DOCUMENT_ID), anyString());

            // When & Then
            mockMvc.perform(post("/document/{id}/callback", TEST_DOCUMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0));

            verify(documentAppService).saveFromOnlyOffice(eq(TEST_DOCUMENT_ID), eq("http://onlyoffice/download/url"));
        }

        @Test
        @DisplayName("回调状态4（无变化）- 不需要保存")
        void shouldHandleCallbackWithStatus4() throws Exception {
            // Given
            Map<String, Object> payload = Map.of("status", 4);

            // When & Then
            mockMvc.perform(post("/document/{id}/callback", TEST_DOCUMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0));

            verify(documentAppService, never()).saveFromOnlyOffice(anyLong(), anyString());
        }

        @Test
        @DisplayName("回调处理异常 - 返回错误")
        void shouldReturnErrorOnCallbackException() throws Exception {
            // Given
            Map<String, Object> payload = Map.of(
                    "status", 2,
                    "url", "http://onlyoffice/download/url"
            );
            doThrow(new RuntimeException("保存失败"))
                    .when(documentAppService).saveFromOnlyOffice(anyLong(), anyString());

            // When & Then
            mockMvc.perform(post("/document/{id}/callback", TEST_DOCUMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(1))
                    .andExpect(jsonPath("$.message").value("保存失败"));
        }
    }

    @Nested
    @DisplayName("编辑支持检查接口测试")
    class CheckEditSupportTests {

        @Test
        @DisplayName("检查可编辑文档 - 返回支持")
        void shouldReturnTrueForEditableDocument() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.docx", "http://minio:9000/law-firm/test.docx");
            doc.setFileType("docx");
            doc.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);
            when(onlyOfficeService.isSupported("test.docx")).thenReturn(true);
            when(onlyOfficeService.isPreviewSupported("test.docx")).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/document/{id}/edit-support", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.documentId").value(TEST_DOCUMENT_ID))
                    .andExpect(jsonPath("$.data.fileName").value("test.docx"))
                    .andExpect(jsonPath("$.data.canEdit").value(true))
                    .andExpect(jsonPath("$.data.canPreview").value(true));
        }

        @Test
        @DisplayName("检查不可编辑但可预览文档 - 返回部分支持")
        void shouldReturnPartialSupportForPdf() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.pdf", "http://minio:9000/law-firm/test.pdf");
            doc.setFileType("pdf");
            doc.setMimeType("application/pdf");
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);
            when(onlyOfficeService.isSupported("test.pdf")).thenReturn(false);
            when(onlyOfficeService.isPreviewSupported("test.pdf")).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/document/{id}/edit-support", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.canEdit").value(false))
                    .andExpect(jsonPath("$.data.canPreview").value(true));
        }
    }

    @Nested
    @DisplayName("预览URL生成接口测试")
    class GetPreviewUrlTests {

        @Test
        @DisplayName("生成预览URL - 成功")
        void shouldGeneratePreviewUrl() throws Exception {
            // Given
            DocumentDTO doc = createMockDocument("test.docx", "http://minio:9000/law-firm/test.docx");
            doc.setFileType("docx");
            doc.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            when(documentAppService.getDocumentById(TEST_DOCUMENT_ID)).thenReturn(doc);

            // When & Then
            mockMvc.perform(get("/document/{id}/preview-url", TEST_DOCUMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.documentId").value(TEST_DOCUMENT_ID))
                    .andExpect(jsonPath("$.data.fileName").value("test.docx"))
                    .andExpect(jsonPath("$.data.previewUrl").exists())
                    .andExpect(jsonPath("$.data.expires").exists());
        }
    }

    @Nested
    @DisplayName("Token验证相关测试")
    class TokenValidationTests {

        @Test
        @DisplayName("生成访问token - 格式正确")
        void shouldGenerateValidAccessToken() {
            // Given
            long expires = System.currentTimeMillis() + 7200 * 1000;

            // When
            String token = DocumentController.generateAccessToken(TEST_DOCUMENT_ID, expires);

            // Then
            assertThat(token).isNotNull();
            assertThat(token.length()).isEqualTo(32); // SHA-256 hex substring
        }

        @Test
        @DisplayName("相同输入生成相同token")
        void shouldGenerateSameTokenForSameInput() {
            // Given
            long expires = System.currentTimeMillis() + 7200 * 1000;

            // When
            String token1 = DocumentController.generateAccessToken(TEST_DOCUMENT_ID, expires);
            String token2 = DocumentController.generateAccessToken(TEST_DOCUMENT_ID, expires);

            // Then
            assertThat(token1).isEqualTo(token2);
        }

        @Test
        @DisplayName("不同文档ID生成不同token")
        void shouldGenerateDifferentTokenForDifferentDocumentId() {
            // Given
            long expires = System.currentTimeMillis() + 7200 * 1000;

            // When
            String token1 = DocumentController.generateAccessToken(1L, expires);
            String token2 = DocumentController.generateAccessToken(2L, expires);

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    /**
     * 创建模拟文档对象
     */
    private DocumentDTO createMockDocument(String fileName, String filePath) {
        DocumentDTO doc = new DocumentDTO();
        doc.setId(TEST_DOCUMENT_ID);
        doc.setFileName(fileName);
        doc.setFilePath(filePath);
        doc.setFileType(fileName.substring(fileName.lastIndexOf('.') + 1));
        return doc;
    }
}
