package com.lawfirm.application.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.knowledge.command.CreateCaseLibraryCommand;
import com.lawfirm.application.knowledge.dto.CaseLibraryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.CaseLibrary;
import com.lawfirm.domain.knowledge.entity.KnowledgeCollection;
import com.lawfirm.domain.knowledge.repository.CaseCategoryRepository;
import com.lawfirm.domain.knowledge.repository.CaseLibraryRepository;
import com.lawfirm.infrastructure.persistence.mapper.CaseCategoryMapper;
import com.lawfirm.infrastructure.persistence.mapper.CaseLibraryMapper;
import com.lawfirm.infrastructure.persistence.mapper.KnowledgeCollectionMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/** CaseLibraryAppService 单元测试 测试知识库案例管理核心业务逻辑 */
@ExtendWith(MockitoExtension.class)
class CaseLibraryAppServiceTest {

  @Mock private CaseCategoryRepository caseCategoryRepository;

  @Mock private CaseCategoryMapper caseCategoryMapper;

  @Mock private CaseLibraryRepository caseLibraryRepository;

  @Mock private CaseLibraryMapper caseLibraryMapper;

  @Mock private KnowledgeCollectionMapper knowledgeCollectionMapper;

  @InjectMocks private CaseLibraryAppService caseLibraryAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  // ==================== 创建案例测试 ====================

  @Nested
  @DisplayName("创建案例测试")
  class CreateCaseTests {

    @Test
    @DisplayName("成功创建案例")
    void createCase_Success() {
      // Given
      CreateCaseLibraryCommand command = new CreateCaseLibraryCommand();
      command.setTitle("合同纠纷典型案例");
      command.setCategoryId(1L);
      command.setCaseNumber("(2024)京01民终123号");
      command.setCourtName("北京市第一中级人民法院");
      command.setJudgeDate(LocalDate.of(2024, 1, 15));
      command.setCaseType("民事");
      command.setCauseOfAction("合同纠纷");

      when(caseLibraryRepository.save(any(CaseLibrary.class))).thenReturn(true);

      // When
      CaseLibraryDTO result = caseLibraryAppService.createCase(command);

      // Then
      assertNotNull(result);
      assertEquals("合同纠纷典型案例", result.getTitle());
      assertEquals("(2024)京01民终123号", result.getCaseNumber());
      verify(caseLibraryRepository).save(any(CaseLibrary.class));
    }

    @Test
    @DisplayName("标题为空时创建失败")
    void createCase_EmptyTitle() {
      // Given
      CreateCaseLibraryCommand command = new CreateCaseLibraryCommand();
      command.setTitle("");

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> caseLibraryAppService.createCase(command));
      assertEquals("案例标题不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("标题为null时创建失败")
    void createCase_NullTitle() {
      // Given
      CreateCaseLibraryCommand command = new CreateCaseLibraryCommand();
      command.setTitle(null);

      // When & Then
      assertThrows(BusinessException.class, () -> caseLibraryAppService.createCase(command));
    }
  }

  // ==================== 获取案例详情测试 ====================

  @Nested
  @DisplayName("获取案例详情测试")
  class GetCaseTests {

    @Test
    @DisplayName("成功获取案例详情")
    void getCaseById_Success() {
      // Given
      CaseLibrary caseLib =
          CaseLibrary.builder()
              .id(1L)
              .title("劳动争议案例")
              .caseNumber("(2024)沪01民终456号")
              .source(CaseLibrary.SOURCE_EXTERNAL)
              .viewCount(100)
              .collectCount(10)
              .build();

      when(caseLibraryRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(caseLib);
      when(caseLibraryMapper.incrementViewCount(1L)).thenReturn(1);

      // When
      CaseLibraryDTO result = caseLibraryAppService.getCaseById(1L);

      // Then
      assertNotNull(result);
      assertEquals("劳动争议案例", result.getTitle());
      assertEquals("(2024)沪01民终456号", result.getCaseNumber());
      verify(caseLibraryMapper).incrementViewCount(1L);
    }

    @Test
    @DisplayName("案例不存在时抛出异常")
    void getCaseById_NotFound() {
      // Given
      when(caseLibraryRepository.getByIdOrThrow(eq(999L), anyString()))
          .thenThrow(new BusinessException("案例不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> caseLibraryAppService.getCaseById(999L));
    }
  }

  // ==================== 更新案例测试 ====================

  @Nested
  @DisplayName("更新案例测试")
  class UpdateCaseTests {

    @Test
    @DisplayName("成功更新案例")
    void updateCase_Success() {
      // Given
      CaseLibrary existingCase =
          CaseLibrary.builder().id(1L).title("原标题").caseNumber("(2024)京01民终123号").build();

      CreateCaseLibraryCommand command = new CreateCaseLibraryCommand();
      command.setTitle("更新后的标题");
      command.setCourtName("北京市高级人民法院");

      when(caseLibraryRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(existingCase);
      when(caseLibraryRepository.updateById(any(CaseLibrary.class))).thenReturn(true);

      // When
      CaseLibraryDTO result = caseLibraryAppService.updateCase(1L, command);

      // Then
      assertNotNull(result);
      assertEquals("更新后的标题", result.getTitle());
      verify(caseLibraryRepository).updateById(any(CaseLibrary.class));
    }

    @Test
    @DisplayName("部分更新案例")
    void updateCase_PartialUpdate() {
      // Given
      CaseLibrary existingCase =
          CaseLibrary.builder()
              .id(1L)
              .title("原标题")
              .caseNumber("(2024)京01民终123号")
              .courtName("原法院")
              .build();

      CreateCaseLibraryCommand command = new CreateCaseLibraryCommand();
      command.setCourtName("新法院"); // 只更新法院名称

      when(caseLibraryRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(existingCase);
      when(caseLibraryRepository.updateById(any(CaseLibrary.class))).thenReturn(true);

      // When
      CaseLibraryDTO result = caseLibraryAppService.updateCase(1L, command);

      // Then
      assertEquals("原标题", result.getTitle()); // 标题保持不变
      assertEquals("新法院", existingCase.getCourtName()); // 法院已更新
    }
  }

  // ==================== 收藏案例测试 ====================

  @Nested
  @DisplayName("收藏案例测试")
  class CollectCaseTests {

    @Test
    @DisplayName("成功收藏案例")
    void collectCase_Success() {
      // Given
      CaseLibrary caseLib = CaseLibrary.builder().id(1L).title("测试案例").collectCount(5).build();

      when(caseLibraryRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(caseLib);
      when(knowledgeCollectionMapper.insert(any(KnowledgeCollection.class))).thenReturn(1);
      when(caseLibraryMapper.incrementCollectCount(1L)).thenReturn(1);

      // When
      caseLibraryAppService.collectCase(1L);

      // Then
      verify(knowledgeCollectionMapper).insert(any(KnowledgeCollection.class));
      verify(caseLibraryMapper).incrementCollectCount(1L);
    }

    @Test
    @DisplayName("重复收藏案例时抛出异常")
    void collectCase_AlreadyCollected() {
      // Given
      CaseLibrary caseLib = CaseLibrary.builder().id(1L).title("测试案例").build();

      when(caseLibraryRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(caseLib);
      when(knowledgeCollectionMapper.insert(any(KnowledgeCollection.class)))
          .thenThrow(new DuplicateKeyException("Duplicate entry"));

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> caseLibraryAppService.collectCase(1L));
      assertEquals("已收藏该案例", exception.getMessage());
    }

    @Test
    @DisplayName("收藏不存在的案例时抛出异常")
    void collectCase_CaseNotFound() {
      // Given
      when(caseLibraryRepository.getByIdOrThrow(eq(999L), anyString()))
          .thenThrow(new BusinessException("案例不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> caseLibraryAppService.collectCase(999L));
    }
  }

  // ==================== 取消收藏测试 ====================

  @Nested
  @DisplayName("取消收藏测试")
  class UncollectCaseTests {

    @Test
    @DisplayName("成功取消收藏")
    void uncollectCase_Success() {
      // Given
      when(knowledgeCollectionMapper.deleteByUserAndTarget(
              eq(1L), eq(KnowledgeCollection.TYPE_CASE), eq(1L)))
          .thenReturn(1);
      when(caseLibraryMapper.decrementCollectCount(1L)).thenReturn(1);

      // When
      caseLibraryAppService.uncollectCase(1L);

      // Then
      verify(knowledgeCollectionMapper)
          .deleteByUserAndTarget(1L, KnowledgeCollection.TYPE_CASE, 1L);
      verify(caseLibraryMapper).decrementCollectCount(1L);
    }

    @Test
    @DisplayName("取消未收藏的案例不会减少收藏数")
    void uncollectCase_NotCollected() {
      // Given
      when(knowledgeCollectionMapper.deleteByUserAndTarget(
              eq(1L), eq(KnowledgeCollection.TYPE_CASE), eq(1L)))
          .thenReturn(0);

      // When
      caseLibraryAppService.uncollectCase(1L);

      // Then
      verify(caseLibraryMapper, never()).decrementCollectCount(anyLong());
    }
  }

  // ==================== 删除案例测试 ====================

  @Nested
  @DisplayName("删除案例测试")
  class DeleteCaseTests {

    @Test
    @DisplayName("成功删除案例")
    void deleteCase_Success() {
      // Given
      CaseLibrary caseLib = CaseLibrary.builder().id(1L).title("待删除案例").build();

      when(caseLibraryRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(caseLib);
      when(caseLibraryMapper.deleteById(1L)).thenReturn(1);

      // When
      caseLibraryAppService.deleteCase(1L);

      // Then
      verify(caseLibraryMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除不存在的案例时抛出异常")
    void deleteCase_NotFound() {
      // Given
      when(caseLibraryRepository.getByIdOrThrow(eq(999L), anyString()))
          .thenThrow(new BusinessException("案例不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> caseLibraryAppService.deleteCase(999L));
    }
  }
}
