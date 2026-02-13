package com.lawfirm.application.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.knowledge.command.CreateQualityCheckStandardCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckStandardDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.knowledge.entity.QualityCheckStandard;
import com.lawfirm.domain.knowledge.repository.QualityCheckStandardRepository;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckStandardMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** QualityCheckStandardAppService 单元测试 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QualityCheckStandardAppService 质量标准服务测试")
class QualityCheckStandardAppServiceTest {

  private static final Long TEST_STANDARD_ID = 100L;

  @Mock private QualityCheckStandardRepository standardRepository;

  @Mock private QualityCheckStandardMapper standardMapper;

  @InjectMocks private QualityCheckStandardAppService standardAppService;

  @Nested
  @DisplayName("查询标准测试")
  class QueryStandardTests {

    @Test
    @DisplayName("应该成功获取所有启用的标准")
    void getEnabledStandards_shouldSuccess() {
      // Given
      QualityCheckStandard standard =
          QualityCheckStandard.builder()
              .id(TEST_STANDARD_ID)
              .standardName("测试标准")
              .category(QualityCheckStandard.CATEGORY_CONTRACT)
              .enabled(true)
              .build();

      when(standardMapper.selectEnabledStandards()).thenReturn(List.of(standard));

      // When
      List<QualityCheckStandardDTO> result = standardAppService.getEnabledStandards();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStandardName()).isEqualTo("测试标准");
    }

    @Test
    @DisplayName("应该成功按分类查询标准")
    void getStandardsByCategory_shouldSuccess() {
      // Given
      QualityCheckStandard standard =
          QualityCheckStandard.builder()
              .id(TEST_STANDARD_ID)
              .standardName("合同标准")
              .category(QualityCheckStandard.CATEGORY_CONTRACT)
              .build();

      when(standardMapper.selectByCategory(QualityCheckStandard.CATEGORY_CONTRACT))
          .thenReturn(List.of(standard));

      // When
      List<QualityCheckStandardDTO> result =
          standardAppService.getStandardsByCategory(QualityCheckStandard.CATEGORY_CONTRACT);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("应该成功获取标准详情")
    void getStandardById_shouldSuccess() {
      // Given
      QualityCheckStandard standard =
          QualityCheckStandard.builder()
              .id(TEST_STANDARD_ID)
              .standardName("测试标准")
              .category(QualityCheckStandard.CATEGORY_CONTRACT)
              .description("标准描述")
              .build();

      when(standardRepository.getByIdOrThrow(eq(TEST_STANDARD_ID), anyString()))
          .thenReturn(standard);

      // When
      QualityCheckStandardDTO result = standardAppService.getStandardById(TEST_STANDARD_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getStandardName()).isEqualTo("测试标准");
    }
  }

  @Nested
  @DisplayName("创建标准测试")
  class CreateStandardTests {

    @Test
    @DisplayName("应该成功创建标准")
    void createStandard_shouldSuccess() {
      // Given
      CreateQualityCheckStandardCommand command = new CreateQualityCheckStandardCommand();
      command.setStandardName("新标准");
      command.setCategory(QualityCheckStandard.CATEGORY_CONTRACT);
      command.setDescription("标准描述");
      command.setWeight(BigDecimal.valueOf(1.5));
      command.setEnabled(true);

      when(standardRepository.save(any(QualityCheckStandard.class)))
          .thenAnswer(
              invocation -> {
                QualityCheckStandard standard = invocation.getArgument(0);
                standard.setId(TEST_STANDARD_ID);
                return true;
              });

      // When
      QualityCheckStandardDTO result = standardAppService.createStandard(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getStandardName()).isEqualTo("新标准");
      verify(standardRepository).save(any(QualityCheckStandard.class));
    }

    @Test
    @DisplayName("应该失败当标准名称为空")
    void createStandard_shouldFail_whenNameEmpty() {
      // Given
      CreateQualityCheckStandardCommand command = new CreateQualityCheckStandardCommand();
      command.setStandardName("");

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> standardAppService.createStandard(command));
      assertThat(exception.getMessage()).contains("标准名称不能为空");
    }

    @Test
    @DisplayName("应该使用默认值当未指定")
    void createStandard_shouldUseDefaults() {
      // Given
      CreateQualityCheckStandardCommand command = new CreateQualityCheckStandardCommand();
      command.setStandardName("新标准");

      when(standardRepository.save(any(QualityCheckStandard.class)))
          .thenAnswer(
              invocation -> {
                QualityCheckStandard standard = invocation.getArgument(0);
                standard.setId(TEST_STANDARD_ID);
                return true;
              });

      // When
      QualityCheckStandardDTO result = standardAppService.createStandard(command);

      // Then
      assertThat(result).isNotNull();
      verify(standardRepository)
          .save(
              argThat(
                  standard ->
                      standard.getWeight().compareTo(BigDecimal.ONE) == 0
                          && Boolean.TRUE.equals(standard.getEnabled())
                          && standard.getSortOrder() == 0));
    }
  }

  @Nested
  @DisplayName("更新标准测试")
  class UpdateStandardTests {

    @Test
    @DisplayName("应该成功更新标准")
    void updateStandard_shouldSuccess() {
      // Given
      QualityCheckStandard standard =
          QualityCheckStandard.builder()
              .id(TEST_STANDARD_ID)
              .standardName("原名称")
              .category(QualityCheckStandard.CATEGORY_CONTRACT)
              .enabled(true)
              .build();

      CreateQualityCheckStandardCommand command = new CreateQualityCheckStandardCommand();
      command.setStandardName("新名称");
      command.setCategory(QualityCheckStandard.CATEGORY_DOCUMENT);
      command.setEnabled(false);

      when(standardRepository.getByIdOrThrow(eq(TEST_STANDARD_ID), anyString()))
          .thenReturn(standard);
      when(standardRepository.updateById(any(QualityCheckStandard.class))).thenReturn(true);

      // When
      QualityCheckStandardDTO result = standardAppService.updateStandard(TEST_STANDARD_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(standard.getStandardName()).isEqualTo("新名称");
      assertThat(standard.getCategory()).isEqualTo(QualityCheckStandard.CATEGORY_DOCUMENT);
      assertThat(standard.getEnabled()).isFalse();
      verify(standardRepository).updateById(standard);
    }

    @Test
    @DisplayName("应该失败当标准不存在")
    void updateStandard_shouldFail_whenStandardNotExists() {
      // Given
      CreateQualityCheckStandardCommand command = new CreateQualityCheckStandardCommand();

      when(standardRepository.getByIdOrThrow(eq(TEST_STANDARD_ID), anyString()))
          .thenThrow(new BusinessException("检查标准不存在"));

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> standardAppService.updateStandard(TEST_STANDARD_ID, command));
    }
  }

  @Nested
  @DisplayName("删除标准测试")
  class DeleteStandardTests {

    @Test
    @DisplayName("应该成功删除标准")
    void deleteStandard_shouldSuccess() {
      // Given
      QualityCheckStandard standard =
          QualityCheckStandard.builder().id(TEST_STANDARD_ID).standardName("测试标准").build();

      when(standardRepository.getByIdOrThrow(eq(TEST_STANDARD_ID), anyString()))
          .thenReturn(standard);
      when(standardRepository.removeById(TEST_STANDARD_ID)).thenReturn(true);

      // When
      standardAppService.deleteStandard(TEST_STANDARD_ID);

      // Then
      verify(standardRepository).removeById(TEST_STANDARD_ID);
    }
  }
}
