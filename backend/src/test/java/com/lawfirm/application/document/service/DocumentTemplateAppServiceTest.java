package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.BatchGenerateDocumentCommand;
import com.lawfirm.application.document.command.CreateDocumentTemplateCommand;
import com.lawfirm.application.document.command.GenerateDocumentCommand;
import com.lawfirm.application.document.command.PreviewTemplateCommand;
import com.lawfirm.application.document.command.UpdateDocumentTemplateCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.DocumentTemplateDTO;
import com.lawfirm.application.document.dto.DocumentTemplateQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.domain.document.repository.DocumentTemplateRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.DocumentTemplateMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DocumentTemplateAppService 单元测试
 * 测试文档模板管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentTemplateAppService 文档模板服务测试")
class DocumentTemplateAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_TEMPLATE_ID = 100L;
    private static final Long TEST_MATTER_ID = 200L;
    private static final Long TEST_CATEGORY_ID = 300L;

    @Mock
    private DocumentTemplateRepository templateRepository;

    @Mock
    private DocumentCategoryRepository categoryRepository;

    @Mock
    private DocumentTemplateMapper templateMapper;

    @Mock
    private TemplateVariableService templateVariableService;

    @Mock
    private DocumentAppService documentAppService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private DocumentTemplateAppService documentTemplateAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("创建模板测试")
    class CreateTemplateTests {

        @Test
        @DisplayName("应该成功创建模板")
        void createTemplate_shouldSuccess() {
            // Given
            CreateDocumentTemplateCommand command = new CreateDocumentTemplateCommand();
            command.setName("合同模板");
            command.setCategoryId(TEST_CATEGORY_ID);
            command.setTemplateType("CONTRACT");
            command.setFileName("contract.docx");
            command.setFilePath("/templates/contract.docx");
            command.setFileSize(1024L);

            when(templateRepository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> {
                DocumentTemplate template = invocation.getArgument(0);
                template.setId(TEST_TEMPLATE_ID);
                return template;
            });

            // When
            DocumentTemplateDTO result = documentTemplateAppService.createTemplate(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("合同模板");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getUseCount()).isEqualTo(0);
            verify(templateRepository).save(any(DocumentTemplate.class));
        }
    }

    @Nested
    @DisplayName("更新模板测试")
    class UpdateTemplateTests {

        @Test
        @DisplayName("应该成功更新模板")
        void updateTemplate_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("原名称")
                    .status("ACTIVE")
                    .build();

            UpdateDocumentTemplateCommand command = new UpdateDocumentTemplateCommand();
            command.setName("新名称");
            command.setDescription("新描述");

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            lenient().when(templateRepository.updateById(any(DocumentTemplate.class))).thenReturn(true);

            // When
            DocumentTemplateDTO result = documentTemplateAppService.updateTemplate(TEST_TEMPLATE_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(template.getName()).isEqualTo("新名称");
            assertThat(template.getDescription()).isEqualTo("新描述");
        }
    }

    @Nested
    @DisplayName("删除模板测试")
    class DeleteTemplateTests {

        @Test
        @DisplayName("应该成功删除模板")
        void deleteTemplate_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("模板1")
                    .build();

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            lenient().when(templateRepository.removeById(TEST_TEMPLATE_ID)).thenReturn(true);

            // When
            documentTemplateAppService.deleteTemplate(TEST_TEMPLATE_ID);

            // Then
            verify(templateRepository).removeById(TEST_TEMPLATE_ID);
        }
    }

    @Nested
    @DisplayName("使用模板测试")
    class UseTemplateTests {

        @Test
        @DisplayName("应该成功使用模板")
        void useTemplate_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("模板1")
                    .status("ACTIVE")
                    .build();

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            lenient().doNothing().when(templateRepository).incrementUseCount(TEST_TEMPLATE_ID);

            // When
            documentTemplateAppService.useTemplate(TEST_TEMPLATE_ID);

            // Then
            verify(templateRepository).incrementUseCount(TEST_TEMPLATE_ID);
        }

        @Test
        @DisplayName("已停用的模板不能使用")
        void useTemplate_shouldFail_whenInactive() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .status("INACTIVE")
                    .build();

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> documentTemplateAppService.useTemplate(TEST_TEMPLATE_ID));
            assertThat(exception.getMessage()).contains("已停用");
        }
    }

    @Nested
    @DisplayName("生成文档测试")
    class GenerateDocumentTests {

        @Test
        @DisplayName("应该成功从模板生成文档")
        void generateDocument_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("合同模板")
                    .status("ACTIVE")
                    .categoryId(TEST_CATEGORY_ID)
                    .content("模板内容：{{matter.name}}")
                    .build();

            GenerateDocumentCommand command = new GenerateDocumentCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);
            command.setMatterId(TEST_MATTER_ID);
            command.setDocumentName("生成的文档");

            Map<String, Object> variables = new HashMap<>();
            variables.put("matter.name", "案件1");

            DocumentDTO documentDTO = new DocumentDTO();
            documentDTO.setId(400L);
            documentDTO.setTitle("生成的文档");

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(templateVariableService.collectVariables(TEST_MATTER_ID)).thenReturn(variables);
            when(templateVariableService.replaceVariables(anyString(), anyMap())).thenReturn("模板内容：案件1");
            try {
                doReturn("http://minio/documents/file.docx").when(minioService).uploadBytes(any(byte[].class), anyString(), anyString());
            } catch (Exception e) {
                // ignore
            }
            lenient().doNothing().when(templateRepository).incrementUseCount(TEST_TEMPLATE_ID);
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(400L);
                return doc;
            });
            when(documentAppService.getDocumentById(400L)).thenReturn(documentDTO);

            // When
            DocumentDTO result = documentTemplateAppService.generateDocument(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("生成的文档");
            verify(templateRepository).incrementUseCount(TEST_TEMPLATE_ID);
        }

        @Test
        @DisplayName("已停用的模板不能生成文档")
        void generateDocument_shouldFail_whenInactive() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .status("INACTIVE")
                    .build();

            GenerateDocumentCommand command = new GenerateDocumentCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> documentTemplateAppService.generateDocument(command));
            assertThat(exception.getMessage()).contains("已停用");
        }

        @Test
        @DisplayName("模板内容为空应该失败")
        void generateDocument_shouldFail_whenContentEmpty() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .status("ACTIVE")
                    .content(null)
                    .filePath(null)
                    .build();

            GenerateDocumentCommand command = new GenerateDocumentCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);
            command.setMatterId(TEST_MATTER_ID);

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(templateVariableService.collectVariables(TEST_MATTER_ID)).thenReturn(Collections.emptyMap());
            lenient().when(minioService.extractObjectName(any())).thenReturn(null);
            try {
                lenient().doReturn(null).when(minioService).downloadFileAsBytes(anyString());
            } catch (Exception e) {
                // ignore
            }

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> documentTemplateAppService.generateDocument(command));
            assertThat(exception.getMessage()).contains("模板内容为空");
        }
    }

    @Nested
    @DisplayName("批量生成文档测试")
    class BatchGenerateDocumentsTests {

        @Test
        @DisplayName("应该成功批量生成文档")
        void batchGenerateDocuments_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .status("ACTIVE")
                    .content("模板内容")
                    .build();

            BatchGenerateDocumentCommand command = new BatchGenerateDocumentCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);
            command.setMatterIds(Arrays.asList(200L, 201L));

            DocumentDTO doc1 = new DocumentDTO();
            doc1.setId(400L);
            DocumentDTO doc2 = new DocumentDTO();
            doc2.setId(401L);

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(templateVariableService.collectVariables(anyLong())).thenReturn(Collections.emptyMap());
            when(templateVariableService.replaceVariables(anyString(), anyMap())).thenReturn("生成的内容");
            try {
                doReturn("http://minio/file.docx").when(minioService).uploadBytes(any(byte[].class), anyString(), anyString());
            } catch (Exception e) {
                // ignore
            }
            lenient().doNothing().when(templateRepository).incrementUseCount(TEST_TEMPLATE_ID);
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(400L + invocation.getArgument(0, Document.class).getMatterId() - 200L);
                return doc;
            });
            when(documentAppService.getDocumentById(anyLong())).thenAnswer(invocation -> {
                DocumentDTO dto = new DocumentDTO();
                dto.setId(invocation.getArgument(0));
                return dto;
            });

            // When
            List<DocumentDTO> result = documentTemplateAppService.batchGenerateDocuments(command);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("预览模板测试")
    class PreviewTemplateTests {

        @Test
        @DisplayName("应该成功预览模板")
        void previewTemplate_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("合同模板")
                    .content("模板内容：{{matter.name}}")
                    .variables("matter.name,client.name")
                    .build();

            PreviewTemplateCommand command = new PreviewTemplateCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);
            command.setMatterId(TEST_MATTER_ID);

            Map<String, Object> variables = new HashMap<>();
            variables.put("matter.name", "案件1");

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(templateVariableService.collectVariables(TEST_MATTER_ID)).thenReturn(variables);
            when(templateVariableService.replaceVariables(anyString(), anyMap())).thenReturn("模板内容：案件1");

            // When
            Map<String, Object> result = documentTemplateAppService.previewTemplate(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("templateName")).isEqualTo("合同模板");
            assertThat(result.get("previewContent")).isNotNull();
        }
    }

    @Nested
    @DisplayName("查询模板测试")
    class QueryTemplateTests {

        @Test
        @DisplayName("应该成功查询模板列表")
        void listTemplates_shouldSuccess() {
            // Given
            DocumentTemplateQueryDTO query = new DocumentTemplateQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("模板1")
                    .status("ACTIVE")
                    .build();

            Page<DocumentTemplate> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(template));
            page.setTotal(1);

            when(templateMapper.selectTemplatePage(any(Page.class), any(), any(), any(), any())).thenReturn(page);

            // When
            PageResult<DocumentTemplateDTO> result = documentTemplateAppService.listTemplates(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getName()).isEqualTo("模板1");
        }

        @Test
        @DisplayName("应该成功获取模板详情")
        void getTemplateById_shouldSuccess() {
            // Given
            DocumentTemplate template = DocumentTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("模板1")
                    .build();

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);

            // When
            DocumentTemplateDTO result = documentTemplateAppService.getTemplateById(TEST_TEMPLATE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("模板1");
        }
    }
}
