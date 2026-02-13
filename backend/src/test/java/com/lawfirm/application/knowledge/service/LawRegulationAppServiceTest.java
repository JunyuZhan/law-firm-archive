package com.lawfirm.application.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.knowledge.command.CreateLawRegulationCommand;
import com.lawfirm.application.knowledge.dto.LawCategoryDTO;
import com.lawfirm.application.knowledge.dto.LawRegulationDTO;
import com.lawfirm.application.knowledge.dto.LawRegulationQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.KnowledgeCollection;
import com.lawfirm.domain.knowledge.entity.LawCategory;
import com.lawfirm.domain.knowledge.entity.LawRegulation;
import com.lawfirm.domain.knowledge.repository.LawCategoryRepository;
import com.lawfirm.domain.knowledge.repository.LawRegulationRepository;
import com.lawfirm.infrastructure.persistence.mapper.KnowledgeCollectionMapper;
import com.lawfirm.infrastructure.persistence.mapper.LawCategoryMapper;
import com.lawfirm.infrastructure.persistence.mapper.LawRegulationMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** LawRegulationAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LawRegulationAppService 法规服务测试")
class LawRegulationAppServiceTest {

  private static final Long TEST_REGULATION_ID = 100L;
  private static final Long TEST_CATEGORY_ID = 10L;
  private static final Long TEST_USER_ID = 1L;

  @Mock private LawCategoryRepository lawCategoryRepository;

  @Mock private LawCategoryMapper lawCategoryMapper;

  @Mock private LawRegulationRepository lawRegulationRepository;

  @Mock private LawRegulationMapper lawRegulationMapper;

  @Mock private KnowledgeCollectionMapper knowledgeCollectionMapper;

  @InjectMocks private LawRegulationAppService regulationAppService;

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
  @DisplayName("查询分类树测试")
  class QueryCategoryTreeTests {

    @Test
    @DisplayName("应该成功获取分类树")
    void getCategoryTree_shouldSuccess() {
      // Given
      LawCategory category1 =
          LawCategory.builder().id(1L).name("分类1").parentId(0L).level(1).build();

      LawCategory category2 =
          LawCategory.builder().id(2L).name("分类2").parentId(1L).level(2).build();

      when(lawCategoryMapper.selectAllCategories()).thenReturn(List.of(category1, category2));

      // When
      List<LawCategoryDTO> result = regulationAppService.getCategoryTree();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("分类1");
    }
  }

  @Nested
  @DisplayName("查询法规测试")
  class QueryRegulationTests {

    @Test
    @DisplayName("应该成功分页查询法规")
    void listRegulations_shouldSuccess() {
      // Given
      LawRegulation regulation =
          LawRegulation.builder()
              .id(TEST_REGULATION_ID)
              .title("测试法规")
              .categoryId(TEST_CATEGORY_ID)
              .status(LawRegulation.STATUS_EFFECTIVE)
              .viewCount(10)
              .collectCount(5)
              .build();

      Page<LawRegulation> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(regulation));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<LawRegulation> pageParam = any(Page.class);
      when(lawRegulationMapper.selectRegulationPage(pageParam, any(), any(), any()))
          .thenReturn(page);

      LawCategory category = LawCategory.builder().id(TEST_CATEGORY_ID).name("测试分类").build();
      when(lawCategoryRepository.getById(TEST_CATEGORY_ID)).thenReturn(category);
      when(knowledgeCollectionMapper.countByUserAndTarget(
              eq(TEST_USER_ID), eq(KnowledgeCollection.TYPE_LAW), eq(TEST_REGULATION_ID)))
          .thenReturn(0);

      LawRegulationQueryDTO query = new LawRegulationQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<LawRegulationDTO> result = regulationAppService.listRegulations(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("应该成功获取法规详情")
    void getRegulationById_shouldSuccess() {
      // Given
      LawRegulation regulation =
          LawRegulation.builder()
              .id(TEST_REGULATION_ID)
              .title("测试法规")
              .categoryId(TEST_CATEGORY_ID)
              .status(LawRegulation.STATUS_EFFECTIVE)
              .viewCount(10)
              .collectCount(5)
              .build();

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenReturn(regulation);
      when(lawRegulationMapper.incrementViewCount(TEST_REGULATION_ID)).thenReturn(1);

      LawCategory category = LawCategory.builder().id(TEST_CATEGORY_ID).name("测试分类").build();
      when(lawCategoryRepository.getById(TEST_CATEGORY_ID)).thenReturn(category);
      when(knowledgeCollectionMapper.countByUserAndTarget(
              eq(TEST_USER_ID), eq(KnowledgeCollection.TYPE_LAW), eq(TEST_REGULATION_ID)))
          .thenReturn(0);

      // When
      LawRegulationDTO result = regulationAppService.getRegulationById(TEST_REGULATION_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("测试法规");
      verify(lawRegulationMapper).incrementViewCount(TEST_REGULATION_ID);
    }
  }

  @Nested
  @DisplayName("创建法规测试")
  class CreateRegulationTests {

    @Test
    @DisplayName("应该成功创建法规")
    void createRegulation_shouldSuccess() {
      // Given
      CreateLawRegulationCommand command = new CreateLawRegulationCommand();
      command.setTitle("新法规");
      command.setCategoryId(TEST_CATEGORY_ID);
      command.setDocNumber("DOC-001");
      command.setStatus(LawRegulation.STATUS_EFFECTIVE);

      when(lawRegulationRepository.save(any(LawRegulation.class)))
          .thenAnswer(
              invocation -> {
                LawRegulation regulation = invocation.getArgument(0);
                regulation.setId(TEST_REGULATION_ID);
                return true;
              });

      LawCategory category = LawCategory.builder().id(TEST_CATEGORY_ID).name("测试分类").build();
      when(lawCategoryRepository.getById(TEST_CATEGORY_ID)).thenReturn(category);

      // When
      LawRegulationDTO result = regulationAppService.createRegulation(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("新法规");
      verify(lawRegulationRepository).save(any(LawRegulation.class));
    }

    @Test
    @DisplayName("应该失败当标题为空")
    void createRegulation_shouldFail_whenTitleEmpty() {
      // Given
      CreateLawRegulationCommand command = new CreateLawRegulationCommand();
      command.setTitle("");

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> regulationAppService.createRegulation(command));
      assertThat(exception.getMessage()).contains("法规标题不能为空");
    }

    @Test
    @DisplayName("应该使用默认状态当未指定")
    void createRegulation_shouldUseDefaultStatus() {
      // Given
      CreateLawRegulationCommand command = new CreateLawRegulationCommand();
      command.setTitle("新法规");
      command.setCategoryId(TEST_CATEGORY_ID);

      when(lawRegulationRepository.save(any(LawRegulation.class)))
          .thenAnswer(
              invocation -> {
                LawRegulation regulation = invocation.getArgument(0);
                regulation.setId(TEST_REGULATION_ID);
                return true;
              });

      LawCategory category = LawCategory.builder().id(TEST_CATEGORY_ID).name("测试分类").build();
      when(lawCategoryRepository.getById(TEST_CATEGORY_ID)).thenReturn(category);

      // When
      LawRegulationDTO result = regulationAppService.createRegulation(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(LawRegulation.STATUS_EFFECTIVE);
    }
  }

  @Nested
  @DisplayName("更新法规测试")
  class UpdateRegulationTests {

    @Test
    @DisplayName("应该成功更新法规")
    void updateRegulation_shouldSuccess() {
      // Given
      LawRegulation regulation =
          LawRegulation.builder()
              .id(TEST_REGULATION_ID)
              .title("原标题")
              .categoryId(TEST_CATEGORY_ID)
              .status(LawRegulation.STATUS_EFFECTIVE)
              .build();

      CreateLawRegulationCommand command = new CreateLawRegulationCommand();
      command.setTitle("新标题");
      command.setStatus(LawRegulation.STATUS_AMENDED);

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenReturn(regulation);
      when(lawRegulationRepository.updateById(any(LawRegulation.class))).thenReturn(true);

      LawCategory category = LawCategory.builder().id(TEST_CATEGORY_ID).name("测试分类").build();
      when(lawCategoryRepository.getById(TEST_CATEGORY_ID)).thenReturn(category);

      // When
      LawRegulationDTO result = regulationAppService.updateRegulation(TEST_REGULATION_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(regulation.getTitle()).isEqualTo("新标题");
      assertThat(regulation.getStatus()).isEqualTo(LawRegulation.STATUS_AMENDED);
      verify(lawRegulationRepository).updateById(regulation);
    }

    @Test
    @DisplayName("应该失败当法规不存在")
    void updateRegulation_shouldFail_whenRegulationNotExists() {
      // Given
      CreateLawRegulationCommand command = new CreateLawRegulationCommand();

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenThrow(new BusinessException("法规不存在"));

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> regulationAppService.updateRegulation(TEST_REGULATION_ID, command));
    }
  }

  @Nested
  @DisplayName("删除法规测试")
  class DeleteRegulationTests {

    @Test
    @DisplayName("应该成功删除法规")
    void deleteRegulation_shouldSuccess() {
      // Given
      LawRegulation regulation =
          LawRegulation.builder().id(TEST_REGULATION_ID).title("测试法规").build();

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenReturn(regulation);
      when(lawRegulationMapper.deleteById(TEST_REGULATION_ID)).thenReturn(1);

      // When
      regulationAppService.deleteRegulation(TEST_REGULATION_ID);

      // Then
      verify(lawRegulationMapper).deleteById(TEST_REGULATION_ID);
    }
  }

  @Nested
  @DisplayName("收藏法规测试")
  class CollectRegulationTests {

    @Test
    @DisplayName("应该成功收藏法规")
    void collectRegulation_shouldSuccess() {
      // Given
      LawRegulation regulation = LawRegulation.builder().id(TEST_REGULATION_ID).build();

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenReturn(regulation);
      when(knowledgeCollectionMapper.countByUserAndTarget(
              eq(TEST_USER_ID), eq(KnowledgeCollection.TYPE_LAW), eq(TEST_REGULATION_ID)))
          .thenReturn(0);
      when(knowledgeCollectionMapper.insert(any(KnowledgeCollection.class))).thenReturn(1);
      when(lawRegulationMapper.incrementCollectCount(TEST_REGULATION_ID)).thenReturn(1);

      // When
      regulationAppService.collectRegulation(TEST_REGULATION_ID);

      // Then
      verify(knowledgeCollectionMapper).insert(any(KnowledgeCollection.class));
      verify(lawRegulationMapper).incrementCollectCount(TEST_REGULATION_ID);
    }

    @Test
    @DisplayName("应该失败当已收藏")
    void collectRegulation_shouldFail_whenAlreadyCollected() {
      // Given
      LawRegulation regulation = LawRegulation.builder().id(TEST_REGULATION_ID).build();

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenReturn(regulation);
      when(knowledgeCollectionMapper.countByUserAndTarget(
              eq(TEST_USER_ID), eq(KnowledgeCollection.TYPE_LAW), eq(TEST_REGULATION_ID)))
          .thenReturn(1);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> regulationAppService.collectRegulation(TEST_REGULATION_ID));
      assertThat(exception.getMessage()).contains("已收藏该法规");
    }
  }

  @Nested
  @DisplayName("取消收藏测试")
  class UncollectRegulationTests {

    @Test
    @DisplayName("应该成功取消收藏")
    void uncollectRegulation_shouldSuccess() {
      // Given
      when(knowledgeCollectionMapper.deleteByUserAndTarget(
              eq(TEST_USER_ID), eq(KnowledgeCollection.TYPE_LAW), eq(TEST_REGULATION_ID)))
          .thenReturn(1);
      when(lawRegulationMapper.decrementCollectCount(TEST_REGULATION_ID)).thenReturn(1);

      // When
      regulationAppService.uncollectRegulation(TEST_REGULATION_ID);

      // Then
      verify(lawRegulationMapper).decrementCollectCount(TEST_REGULATION_ID);
    }

    @Test
    @DisplayName("应该忽略当未收藏")
    void uncollectRegulation_shouldIgnore_whenNotCollected() {
      // Given
      when(knowledgeCollectionMapper.deleteByUserAndTarget(
              eq(TEST_USER_ID), eq(KnowledgeCollection.TYPE_LAW), eq(TEST_REGULATION_ID)))
          .thenReturn(0);

      // When
      regulationAppService.uncollectRegulation(TEST_REGULATION_ID);

      // Then
      verify(lawRegulationMapper, never()).decrementCollectCount(any());
    }
  }

  @Nested
  @DisplayName("标注失效测试")
  class MarkAsRepealedTests {

    @Test
    @DisplayName("应该成功标注法规失效")
    void markAsRepealed_shouldSuccess() {
      // Given
      LawRegulation regulation =
          LawRegulation.builder()
              .id(TEST_REGULATION_ID)
              .title("测试法规")
              .summary("原始摘要")
              .status(LawRegulation.STATUS_EFFECTIVE)
              .categoryId(null)
              .build();

      when(lawRegulationRepository.getByIdOrThrow(eq(TEST_REGULATION_ID), anyString()))
          .thenReturn(regulation);
      when(lawRegulationRepository.updateById(any(LawRegulation.class))).thenReturn(true);
      lenient().when(lawCategoryRepository.getById(any())).thenReturn(null);

      // When
      LawRegulationDTO result = regulationAppService.markAsRepealed(TEST_REGULATION_ID, "已废止");

      // Then
      assertThat(result).isNotNull();
      assertThat(regulation.getStatus()).isEqualTo(LawRegulation.STATUS_REPEALED);
      assertThat(regulation.getSummary()).contains("失效原因");
      verify(lawRegulationRepository).updateById(regulation);
    }
  }
}
