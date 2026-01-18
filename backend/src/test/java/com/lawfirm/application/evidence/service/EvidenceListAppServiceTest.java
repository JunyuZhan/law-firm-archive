package com.lawfirm.application.evidence.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.evidence.command.CreateEvidenceListCommand;
import com.lawfirm.application.evidence.dto.EvidenceListCompareResult;
import com.lawfirm.application.evidence.dto.EvidenceListDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import com.lawfirm.domain.evidence.repository.EvidenceListRepository;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.document.EvidenceListDocumentGenerator;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceListMapper;
import com.lawfirm.application.matter.service.MatterAppService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EvidenceListAppService 单元测试
 * 测试证据清单管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvidenceListAppService 证据清单服务测试")
class EvidenceListAppServiceTest {

    private static final Long TEST_LIST_ID = 100L;
    private static final Long TEST_MATTER_ID = 200L;
    private static final Long TEST_EVIDENCE_ID_1 = 300L;
    private static final Long TEST_EVIDENCE_ID_2 = 301L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_DEPT_ID = 10L;

    @Mock
    private EvidenceListRepository listRepository;

    @Mock
    private EvidenceListMapper listMapper;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private EvidenceListDocumentGenerator documentGenerator;

    @Mock
    private MinioService minioService;

    @Mock
    private MatterAppService matterAppService;

    @InjectMocks
    private EvidenceListAppService evidenceListAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() throws Exception {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
        securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");

        // 通过反射设置matterAppService
        java.lang.reflect.Field field = EvidenceListAppService.class.getDeclaredField("matterAppService");
        field.setAccessible(true);
        field.set(evidenceListAppService, matterAppService);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("创建证据清单测试")
    class CreateListTests {

        @Test
        @DisplayName("应该成功创建证据清单")
        void createList_shouldSuccess() throws JsonProcessingException {
            // Given
            CreateEvidenceListCommand command = new CreateEvidenceListCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setName("证据清单1");
            command.setListType(EvidenceList.TYPE_SUBMISSION);
            command.setEvidenceIds(Arrays.asList(TEST_EVIDENCE_ID_1, TEST_EVIDENCE_ID_2));

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .status("ACTIVE")
                    .build();

            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null); // ALL权限返回null
            lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
            when(objectMapper.writeValueAsString(anyList())).thenReturn("[300,301]");
            when(listRepository.save(any(EvidenceList.class))).thenReturn(true);

            // When
            EvidenceListDTO result = evidenceListAppService.createList(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("证据清单1");
            assertThat(result.getListType()).isEqualTo(EvidenceList.TYPE_SUBMISSION);
            assertThat(result.getStatus()).isEqualTo(EvidenceList.STATUS_DRAFT);
            verify(listRepository).save(any(EvidenceList.class));
        }

        @Test
        @DisplayName("已归档项目不能创建清单")
        void createList_shouldFail_whenMatterArchived() {
            // Given
            CreateEvidenceListCommand command = new CreateEvidenceListCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setName("证据清单1");

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .status("ARCHIVED")
                    .build();

            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evidenceListAppService.createList(command));
            assertThat(exception.getMessage()).contains("已归档");
        }
    }

    @Nested
    @DisplayName("查询证据清单测试")
    class QueryListTests {

        @Test
        @DisplayName("应该成功分页查询证据清单")
        void listEvidenceLists_shouldSuccess() {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .listNo("EL2024001")
                    .matterId(TEST_MATTER_ID)
                    .name("证据清单1")
                    .build();

            Page<EvidenceList> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(list));
            page.setTotal(1L);

            when(listMapper.selectListPage(any(Page.class), eq(TEST_MATTER_ID), any()))
                    .thenReturn(page);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);

            // When
            PageResult<EvidenceListDTO> result = evidenceListAppService.listEvidenceLists(
                    TEST_MATTER_ID, null, 1, 10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getName()).isEqualTo("证据清单1");
        }

        @Test
        @DisplayName("应该成功获取清单详情")
        void getListById_shouldSuccess() throws JsonProcessingException {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .listNo("EL2024001")
                    .matterId(TEST_MATTER_ID)
                    .name("证据清单1")
                    .evidenceIds("[300,301]")
                    .build();

            Evidence evidence1 = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_1)
                    .evidenceNo("EV001")
                    .name("证据1")
                    .build();
            Evidence evidence2 = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_2)
                    .evidenceNo("EV002")
                    .name("证据2")
                    .build();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenAnswer(invocation -> {
                        String json = invocation.getArgument(0);
                        if ("[300,301]".equals(json)) {
                            return Arrays.asList(TEST_EVIDENCE_ID_1, TEST_EVIDENCE_ID_2);
                        } else if ("[300]".equals(json)) {
                            return Collections.singletonList(TEST_EVIDENCE_ID_1);
                        } else if ("[]".equals(json) || json == null || json.isEmpty()) {
                            return Collections.emptyList();
                        }
                        return Collections.emptyList();
                    });
            when(evidenceRepository.listByIds(anyList())).thenReturn(Arrays.asList(evidence1, evidence2));

            // When
            EvidenceListDTO result = evidenceListAppService.getListById(TEST_LIST_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("证据清单1");
            assertThat(result.getEvidences()).hasSize(2);
        }

        @Test
        @DisplayName("无权访问项目时应该失败")
        void getListById_shouldFail_whenNoPermission() {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .build();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("DEPT");
            when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList()); // 无权限

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evidenceListAppService.getListById(TEST_LIST_ID));
            assertThat(exception.getMessage()).contains("权限不足");
        }
    }

    @Nested
    @DisplayName("更新证据清单测试")
    class UpdateListTests {

        @Test
        @DisplayName("应该成功更新证据清单")
        void updateList_shouldSuccess() throws JsonProcessingException {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .name("原名称")
                    .status("ACTIVE")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .status("ACTIVE")
                    .build();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
            when(objectMapper.writeValueAsString(anyList())).thenReturn("[300]");
            when(listRepository.updateById(any(EvidenceList.class))).thenReturn(true);

            // When
            EvidenceListDTO result = evidenceListAppService.updateList(
                    TEST_LIST_ID, "新名称", null, Collections.singletonList(TEST_EVIDENCE_ID_1));

            // Then
            assertThat(result).isNotNull();
            assertThat(list.getName()).isEqualTo("新名称");
            verify(listRepository).updateById(list);
        }
    }

    @Nested
    @DisplayName("删除证据清单测试")
    class DeleteListTests {

        @Test
        @DisplayName("应该成功删除证据清单")
        void deleteList_shouldSuccess() {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .name("待删除清单")
                    .status("ACTIVE")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .status("ACTIVE")
                    .build();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
            when(listRepository.removeById(TEST_LIST_ID)).thenReturn(true);

            // When
            evidenceListAppService.deleteList(TEST_LIST_ID);

            // Then
            verify(listRepository).removeById(TEST_LIST_ID);
        }
    }

    @Nested
    @DisplayName("生成清单文件测试")
    class GenerateFileTests {

        @Test
        @DisplayName("应该成功生成Word格式清单文件")
        void generateListFile_shouldSuccess_whenWordFormat() throws Exception {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .name("证据清单1")
                    .evidenceIds("[300,301]")
                    .build();

            Evidence evidence1 = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_1)
                    .name("证据1")
                    .build();
            Evidence evidence2 = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_2)
                    .name("证据2")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("案件1")
                    .build();

            byte[] documentBytes = "test document".getBytes();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenAnswer(invocation -> {
                        String json = invocation.getArgument(0);
                        if ("[300,301]".equals(json)) {
                            return Arrays.asList(TEST_EVIDENCE_ID_1, TEST_EVIDENCE_ID_2);
                        } else if ("[300]".equals(json)) {
                            return Collections.singletonList(TEST_EVIDENCE_ID_1);
                        } else if ("[]".equals(json) || json == null || json.isEmpty()) {
                            return Collections.emptyList();
                        }
                        return Collections.emptyList();
                    });
            when(evidenceRepository.listByIds(anyList())).thenReturn(Arrays.asList(evidence1, evidence2));
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
            when(documentGenerator.generateWordDocument(any(), any(), anyList())).thenReturn(documentBytes);
            when(minioService.uploadFile(any(ByteArrayInputStream.class), anyString(), anyString(), anyString()))
                    .thenReturn("http://minio/evidence-list/file.docx");
            when(listRepository.updateById(any(EvidenceList.class))).thenReturn(true);

            // When
            String fileUrl = evidenceListAppService.generateListFile(TEST_LIST_ID, "docx");

            // Then
            assertThat(fileUrl).isNotNull();
            assertThat(fileUrl).contains("evidence-list");
            verify(documentGenerator).generateWordDocument(any(), any(), anyList());
            verify(minioService).uploadFile(any(ByteArrayInputStream.class), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("应该成功生成PDF格式清单文件")
        void generateListFile_shouldSuccess_whenPdfFormat() throws Exception {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .name("证据清单1")
                    .evidenceIds("[300]")
                    .build();

            Evidence evidence = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_1)
                    .name("证据1")
                    .build();

            byte[] documentBytes = "test pdf".getBytes();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(Collections.singletonList(TEST_EVIDENCE_ID_1));
            when(evidenceRepository.listByIds(anyList())).thenReturn(Collections.singletonList(evidence));
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(new Matter());
            when(documentGenerator.generatePdfDocument(any(), any(), anyList())).thenReturn(documentBytes);
            when(minioService.uploadFile(any(ByteArrayInputStream.class), anyString(), anyString(), anyString()))
                    .thenReturn("http://minio/evidence-list/file.pdf");
            when(listRepository.updateById(any(EvidenceList.class))).thenReturn(true);

            // When
            String fileUrl = evidenceListAppService.generateListFile(TEST_LIST_ID, "pdf");

            // Then
            assertThat(fileUrl).isNotNull();
            verify(documentGenerator).generatePdfDocument(any(), any(), anyList());
        }

        @Test
        @DisplayName("清单中没有证据时应该失败")
        void generateListFile_shouldFail_whenNoEvidence() throws JsonProcessingException {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .evidenceIds("[]")
                    .build();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            lenient().when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(Collections.emptyList());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> evidenceListAppService.generateListFile(TEST_LIST_ID, "docx"));
            assertThat(exception.getMessage()).contains("清单中没有证据");
        }
    }

    @Nested
    @DisplayName("对比清单测试")
    class CompareListsTests {

        @Test
        @DisplayName("应该成功对比两个清单")
        void compareLists_shouldSuccess() throws JsonProcessingException {
            // Given
            EvidenceList list1 = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .listNo("EL001")
                    .matterId(TEST_MATTER_ID)
                    .evidenceIds("[300]")
                    .build();

            EvidenceList list2 = EvidenceList.builder()
                    .id(101L)
                    .listNo("EL002")
                    .matterId(TEST_MATTER_ID)
                    .evidenceIds("[300,301]")
                    .build();

            Evidence evidence1 = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_1)
                    .name("证据1")
                    .build();
            Evidence evidence2 = Evidence.builder()
                    .id(TEST_EVIDENCE_ID_2)
                    .name("证据2")
                    .build();

            when(listRepository.getByIdOrThrow(eq(TEST_LIST_ID), anyString())).thenReturn(list1);
            when(listRepository.getByIdOrThrow(eq(101L), anyString())).thenReturn(list2);
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);
            when(objectMapper.readValue(eq("[300]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(Collections.singletonList(TEST_EVIDENCE_ID_1));
            when(objectMapper.readValue(eq("[300,301]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(Arrays.asList(TEST_EVIDENCE_ID_1, TEST_EVIDENCE_ID_2));
            when(evidenceRepository.listByIds(Collections.singletonList(TEST_EVIDENCE_ID_2)))
                    .thenReturn(Collections.singletonList(evidence2));

            // When
            EvidenceListCompareResult result = evidenceListAppService.compareLists(TEST_LIST_ID, 101L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAddedEvidences()).hasSize(1);
            // 当没有删除的证据时，service返回null而不是空列表
            assertThat(result.getRemovedEvidences()).isNull();
            assertThat(result.getCommonEvidenceIds()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("按案件查询清单测试")
    class QueryByMatterTests {

        @Test
        @DisplayName("应该成功获取案件的所有清单")
        void getListsByMatter_shouldSuccess() {
            // Given
            EvidenceList list = EvidenceList.builder()
                    .id(TEST_LIST_ID)
                    .matterId(TEST_MATTER_ID)
                    .name("清单1")
                    .build();

            when(listMapper.selectByMatterId(TEST_MATTER_ID))
                    .thenReturn(Collections.singletonList(list));
            lenient().when(matterAppService.getAccessibleMatterIds(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);

            // When
            List<EvidenceListDTO> result = evidenceListAppService.getListsByMatter(TEST_MATTER_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("清单1");
        }
    }
}
