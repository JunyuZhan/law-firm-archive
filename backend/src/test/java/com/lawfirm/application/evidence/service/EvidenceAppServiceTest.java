package com.lawfirm.application.evidence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.evidence.command.CreateCrossExamCommand;
import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.command.UpdateEvidenceCommand;
import com.lawfirm.application.evidence.dto.EvidenceCrossExamDTO;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceQueryDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.entity.EvidenceCrossExam;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.file.FileTypeService;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceCrossExamMapper;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** EvidenceAppService 单元测试 测试证据管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvidenceAppService 证据服务测试")
class EvidenceAppServiceTest {

  private static final Long TEST_EVIDENCE_ID = 100L;
  private static final Long TEST_MATTER_ID = 200L;
  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_DEPT_ID = 10L;

  @Mock private EvidenceRepository evidenceRepository;

  @Mock private EvidenceMapper evidenceMapper;

  @Mock private EvidenceCrossExamMapper crossExamMapper;

  @Mock private FileTypeService fileTypeService;

  @Mock private MatterRepository matterRepository;

  @Mock private MatterAppService matterAppService;

  @InjectMocks private EvidenceAppService evidenceAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
    securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");

    // 通过反射设置matterAppService
    try {
      java.lang.reflect.Field field = EvidenceAppService.class.getDeclaredField("matterAppService");
      field.setAccessible(true);
      field.set(evidenceAppService, matterAppService);
    } catch (Exception e) {
      // Ignore
    }
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("查询证据测试")
  class QueryEvidenceTests {

    @Test
    @DisplayName("应该成功分页查询证据")
    void listEvidence_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder()
              .id(TEST_EVIDENCE_ID)
              .evidenceNo("EVD2024001")
              .matterId(TEST_MATTER_ID)
              .name("测试证据")
              .status("ACTIVE")
              .build();

      Page<Evidence> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(evidence));
      page.setTotal(1L);

      when(matterAppService.getAccessibleMatterIds("ALL", TEST_USER_ID, TEST_DEPT_ID))
          .thenReturn(null); // null表示可以访问所有项目
      @SuppressWarnings("unchecked")
      Page<Evidence> pageParam = any(Page.class);
      when(evidenceMapper.selectEvidencePage(pageParam, any(), any(), any(), any(), any(), any()))
          .thenReturn(page);

      EvidenceQueryDTO query = new EvidenceQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<EvidenceDTO> result = evidenceAppService.listEvidence(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("没有权限访问时应该返回空结果")
    void listEvidence_shouldReturnEmpty_whenNoPermission() {
      // Given
      when(matterAppService.getAccessibleMatterIds("ALL", TEST_USER_ID, TEST_DEPT_ID))
          .thenReturn(Collections.emptyList()); // 空列表表示没有权限

      EvidenceQueryDTO query = new EvidenceQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<EvidenceDTO> result = evidenceAppService.listEvidence(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("创建证据测试")
  class CreateEvidenceTests {

    @Test
    @DisplayName("应该成功创建证据")
    void createEvidence_shouldSuccess() {
      // Given
      CreateEvidenceCommand command = new CreateEvidenceCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setName("新证据");
      command.setEvidenceType("DOCUMENT");
      command.setSource("CLIENT");
      command.setGroupName("证据组1");
      command.setIsOriginal(true);
      command.setOriginalCount(1);
      command.setCopyCount(0);

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(evidenceRepository.getMaxSortOrder(eq(TEST_MATTER_ID), eq("证据组1"))).thenReturn(5);
      when(evidenceRepository.save(any(Evidence.class)))
          .thenAnswer(
              invocation -> {
                Evidence evidence = invocation.getArgument(0);
                evidence.setId(TEST_EVIDENCE_ID);
                evidence.setEvidenceNo("EVD2024001");
                return true;
              });

      // When
      EvidenceDTO result = evidenceAppService.createEvidence(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo("新证据");
      assertThat(result.getStatus()).isEqualTo("ACTIVE");
      assertThat(result.getCrossExamStatus()).isEqualTo("PENDING");
      verify(evidenceRepository).save(any(Evidence.class));
    }

    @Test
    @DisplayName("已归档项目不能创建证据")
    void createEvidence_shouldFail_whenMatterArchived() {
      // Given
      CreateEvidenceCommand command = new CreateEvidenceCommand();
      command.setMatterId(TEST_MATTER_ID);

      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .status("ARCHIVED") // 已归档
              .build();

      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> evidenceAppService.createEvidence(command));
      assertThat(exception.getMessage()).contains("已归档");
    }
  }

  @Nested
  @DisplayName("获取证据详情测试")
  class GetEvidenceTests {

    @Test
    @DisplayName("应该成功获取证据详情")
    void getEvidenceById_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder()
              .id(TEST_EVIDENCE_ID)
              .evidenceNo("EVD2024001")
              .matterId(TEST_MATTER_ID)
              .name("测试证据")
              .build();

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      // dataScope是"ALL"时，validateMatterAccess直接返回，不需要mock
      // 使用lenient，因为crossExamMapper可能不会被调用（如果getEvidenceById方法已修改）
      lenient()
          .when(crossExamMapper.selectByEvidenceId(TEST_EVIDENCE_ID))
          .thenReturn(Collections.emptyList());

      // When
      EvidenceDTO result = evidenceAppService.getEvidenceById(TEST_EVIDENCE_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEvidenceNo()).isEqualTo("EVD2024001");
    }
  }

  @Nested
  @DisplayName("更新证据测试")
  class UpdateEvidenceTests {

    @Test
    @DisplayName("应该成功更新证据")
    void updateEvidence_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder().id(TEST_EVIDENCE_ID).matterId(TEST_MATTER_ID).name("原名称").build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      UpdateEvidenceCommand command = new UpdateEvidenceCommand();
      command.setName("新名称");
      command.setDescription("新描述");

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(evidenceRepository.updateById(any(Evidence.class))).thenReturn(true);

      // When
      EvidenceDTO result = evidenceAppService.updateEvidence(TEST_EVIDENCE_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(evidence.getName()).isEqualTo("新名称");
      assertThat(evidence.getDescription()).isEqualTo("新描述");
      verify(evidenceRepository).updateById(evidence);
    }

    @Test
    @DisplayName("已归档项目不能更新证据")
    void updateEvidence_shouldFail_whenMatterArchived() {
      // Given
      Evidence evidence = Evidence.builder().id(TEST_EVIDENCE_ID).matterId(TEST_MATTER_ID).build();

      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .status("ARCHIVED") // 已归档
              .build();

      UpdateEvidenceCommand command = new UpdateEvidenceCommand();

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> evidenceAppService.updateEvidence(TEST_EVIDENCE_ID, command));
      assertThat(exception.getMessage()).contains("已归档");
    }
  }

  @Nested
  @DisplayName("删除证据测试")
  class DeleteEvidenceTests {

    @Test
    @DisplayName("应该成功删除证据")
    void deleteEvidence_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder().id(TEST_EVIDENCE_ID).matterId(TEST_MATTER_ID).name("测试证据").build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(evidenceRepository.removeById(TEST_EVIDENCE_ID)).thenReturn(true);

      // When
      evidenceAppService.deleteEvidence(TEST_EVIDENCE_ID);

      // Then
      verify(evidenceRepository).removeById(TEST_EVIDENCE_ID);
    }
  }

  @Nested
  @DisplayName("调整排序测试")
  class SortOrderTests {

    @Test
    @DisplayName("应该成功调整证据排序")
    void updateSortOrder_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder().id(TEST_EVIDENCE_ID).matterId(TEST_MATTER_ID).sortOrder(1).build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(evidenceRepository.updateById(any(Evidence.class))).thenReturn(true);

      // When
      evidenceAppService.updateSortOrder(TEST_EVIDENCE_ID, 5);

      // Then
      assertThat(evidence.getSortOrder()).isEqualTo(5);
      verify(evidenceRepository).updateById(evidence);
    }

    @Test
    @DisplayName("排序号不能为空")
    void updateSortOrder_shouldFail_whenSortOrderNull() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> evidenceAppService.updateSortOrder(TEST_EVIDENCE_ID, null));
      assertThat(exception.getMessage()).contains("排序号不能为空");
    }

    @Test
    @DisplayName("排序号不能小于0")
    void updateSortOrder_shouldFail_whenSortOrderNegative() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> evidenceAppService.updateSortOrder(TEST_EVIDENCE_ID, -1));
      assertThat(exception.getMessage()).contains("排序号不能为空");
    }

    @Test
    @DisplayName("排序号不能超过9999")
    void updateSortOrder_shouldFail_whenSortOrderTooLarge() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> evidenceAppService.updateSortOrder(TEST_EVIDENCE_ID, 10000));
      assertThat(exception.getMessage()).contains("排序号不能超过9999");
    }
  }

  @Nested
  @DisplayName("批量调整分组测试")
  class BatchUpdateGroupTests {

    @Test
    @DisplayName("应该成功批量调整分组")
    void batchUpdateGroup_shouldSuccess() {
      // Given
      Evidence evidence1 =
          Evidence.builder().id(1L).matterId(TEST_MATTER_ID).groupName("原分组1").build();
      Evidence evidence2 =
          Evidence.builder().id(2L).matterId(TEST_MATTER_ID).groupName("原分组2").build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      when(evidenceRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(evidence1);
      when(evidenceRepository.getByIdOrThrow(eq(2L), anyString())).thenReturn(evidence2);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      lenient().when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(evidenceRepository.updateBatchById(anyList())).thenReturn(true);

      // When
      evidenceAppService.batchUpdateGroup(Arrays.asList(1L, 2L), "新分组");

      // Then
      assertThat(evidence1.getGroupName()).isEqualTo("新分组");
      assertThat(evidence2.getGroupName()).isEqualTo("新分组");
      verify(evidenceRepository).updateBatchById(anyList());
    }

    @Test
    @DisplayName("空列表应该失败")
    void batchUpdateGroup_shouldFail_whenEmptyList() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> evidenceAppService.batchUpdateGroup(Collections.emptyList(), "新分组"));
      assertThat(exception.getMessage()).contains("请选择要分组的证据");
    }
  }

  @Nested
  @DisplayName("质证管理测试")
  class CrossExamTests {

    @Test
    @DisplayName("应该成功添加质证记录")
    void addCrossExam_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder()
              .id(TEST_EVIDENCE_ID)
              .matterId(TEST_MATTER_ID)
              .crossExamStatus("PENDING")
              .build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      CreateCrossExamCommand command = new CreateCrossExamCommand();
      command.setEvidenceId(TEST_EVIDENCE_ID);
      command.setExamParty("原告");
      command.setAuthenticityOpinion("认可");
      command.setLegalityOpinion("认可");
      command.setRelevanceOpinion("认可");

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(crossExamMapper.selectByEvidenceIdAndParty(eq(TEST_EVIDENCE_ID), eq("原告")))
          .thenReturn(null);
      when(crossExamMapper.insert(any(EvidenceCrossExam.class))).thenReturn(1);
      when(evidenceRepository.updateById(any(Evidence.class))).thenReturn(true);

      // When
      EvidenceCrossExamDTO result = evidenceAppService.addCrossExam(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(evidence.getCrossExamStatus()).isEqualTo("IN_PROGRESS");
      verify(crossExamMapper).insert(any(EvidenceCrossExam.class));
    }

    @Test
    @DisplayName("该方已有质证记录应该失败")
    void addCrossExam_shouldFail_whenExists() {
      // Given
      Evidence evidence = Evidence.builder().id(TEST_EVIDENCE_ID).matterId(TEST_MATTER_ID).build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      CreateCrossExamCommand command = new CreateCrossExamCommand();
      command.setEvidenceId(TEST_EVIDENCE_ID);
      command.setExamParty("原告");

      EvidenceCrossExam existing = EvidenceCrossExam.builder().id(1L).examParty("原告").build();

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      lenient().when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(crossExamMapper.selectByEvidenceIdAndParty(eq(TEST_EVIDENCE_ID), eq("原告")))
          .thenReturn(existing);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> evidenceAppService.addCrossExam(command));
      assertThat(exception.getMessage()).contains("该方已有质证记录");
    }

    @Test
    @DisplayName("应该成功完成质证")
    void completeCrossExam_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder()
              .id(TEST_EVIDENCE_ID)
              .matterId(TEST_MATTER_ID)
              .crossExamStatus("IN_PROGRESS")
              .build();

      Matter matter = Matter.builder().id(TEST_MATTER_ID).status("ACTIVE").build();

      when(evidenceRepository.getByIdOrThrow(eq(TEST_EVIDENCE_ID), anyString()))
          .thenReturn(evidence);
      doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(matter);
      when(evidenceRepository.updateById(any(Evidence.class))).thenReturn(true);

      // When
      evidenceAppService.completeCrossExam(TEST_EVIDENCE_ID);

      // Then
      assertThat(evidence.getCrossExamStatus()).isEqualTo("COMPLETED");
      verify(evidenceRepository).updateById(evidence);
    }
  }

  @Nested
  @DisplayName("按案件查询测试")
  class MatterEvidenceTests {

    @Test
    @DisplayName("应该成功获取案件证据列表")
    void getEvidenceByMatter_shouldSuccess() {
      // Given
      Evidence evidence =
          Evidence.builder().id(TEST_EVIDENCE_ID).matterId(TEST_MATTER_ID).name("测试证据").build();

      // dataScope是"ALL"时，validateMatterAccess直接返回，不需要mock
      when(evidenceRepository.findByMatterId(TEST_MATTER_ID))
          .thenReturn(Collections.singletonList(evidence));

      // When
      List<EvidenceDTO> result = evidenceAppService.getEvidenceByMatter(TEST_MATTER_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("测试证据");
    }

    @Test
    @DisplayName("应该成功获取案件证据分组")
    void getEvidenceGroups_shouldSuccess() {
      // Given
      // dataScope是"ALL"时，validateMatterAccess直接返回，不需要mock matterRepository
      when(evidenceRepository.findGroupsByMatterId(TEST_MATTER_ID))
          .thenReturn(Arrays.asList("分组1", "分组2"));

      // When
      List<String> result = evidenceAppService.getEvidenceGroups(TEST_MATTER_ID);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result).contains("分组1", "分组2");
    }
  }
}
