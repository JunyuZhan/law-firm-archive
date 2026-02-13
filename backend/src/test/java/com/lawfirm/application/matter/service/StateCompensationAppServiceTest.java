package com.lawfirm.application.matter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.matter.command.CreateStateCompensationCommand;
import com.lawfirm.application.matter.command.UpdateStateCompensationCommand;
import com.lawfirm.application.matter.dto.StateCompensationDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterStateCompensation;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.MatterStateCompensationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** StateCompensationAppService 单元测试 测试国家赔偿案件服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StateCompensationAppService 国家赔偿服务测试")
class StateCompensationAppServiceTest {

  private static final Long TEST_COMPENSATION_ID = 100L;
  private static final Long TEST_MATTER_ID = 200L;
  private static final Long TEST_USER_ID = 300L;

  @Mock private MatterStateCompensationRepository stateCompensationRepository;

  @Mock private MatterRepository matterRepository;

  @Mock private MatterAppService matterAppService;

  @InjectMocks private StateCompensationAppService stateCompensationAppService;

  @Nested
  @DisplayName("创建国家赔偿信息测试")
  class CreateTests {

    @Test
    @DisplayName("应该成功创建行政赔偿信息")
    void create_shouldSuccess_forAdminCompensation() {
      // Given
      CreateStateCompensationCommand command = new CreateStateCompensationCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setObligorOrgName("测试机关");
      command.setObligorOrgType("ADMIN_ORGAN");
      command.setCaseSource("ILLEGAL_ADMIN_PUNISHMENT");
      command.setApplicationDate(LocalDate.now());
      command.setClaimAmount(new BigDecimal("100000.00"));

      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .matterNo("M2024001")
              .name("测试案件")
              .caseType("STATE_COMP_ADMIN")
              .build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(stateCompensationRepository.existsByMatterId(TEST_MATTER_ID)).thenReturn(false);
      when(stateCompensationRepository.save(any(MatterStateCompensation.class)))
          .thenAnswer(
              invocation -> {
                MatterStateCompensation entity = invocation.getArgument(0);
                entity.setId(TEST_COMPENSATION_ID);
                return true;
              });

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        StateCompensationDTO result = stateCompensationAppService.create(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
        verify(stateCompensationRepository).save(any(MatterStateCompensation.class));
      }
    }

    @Test
    @DisplayName("应该成功创建刑事赔偿信息")
    void create_shouldSuccess_forCriminalCompensation() {
      // Given
      CreateStateCompensationCommand command = new CreateStateCompensationCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setObligorOrgName("测试机关");
      command.setCriminalCaseTerminated(true);
      command.setCriminalCaseNo("CR2024001");

      Matter matter = Matter.builder().id(TEST_MATTER_ID).caseType("STATE_COMP_CRIMINAL").build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(stateCompensationRepository.existsByMatterId(TEST_MATTER_ID)).thenReturn(false);
      when(stateCompensationRepository.save(any(MatterStateCompensation.class)))
          .thenAnswer(
              invocation -> {
                MatterStateCompensation entity = invocation.getArgument(0);
                entity.setId(TEST_COMPENSATION_ID);
                return true;
              });

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        StateCompensationDTO result = stateCompensationAppService.create(command);

        // Then
        assertThat(result).isNotNull();
      }
    }

    @Test
    @DisplayName("案件类型不正确应该失败")
    void create_shouldFail_whenInvalidCaseType() {
      // Given
      CreateStateCompensationCommand command = new CreateStateCompensationCommand();
      command.setMatterId(TEST_MATTER_ID);

      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .caseType("CIVIL") // 非国家赔偿类型
              .build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> stateCompensationAppService.create(command));
      assertThat(exception.getMessage()).contains("国家赔偿类型");
    }

    @Test
    @DisplayName("刑事赔偿未确认刑事诉讼终结应该失败")
    void create_shouldFail_whenCriminalNotTerminated() {
      // Given
      CreateStateCompensationCommand command = new CreateStateCompensationCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setCriminalCaseTerminated(false);

      Matter matter = Matter.builder().id(TEST_MATTER_ID).caseType("STATE_COMP_CRIMINAL").build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> stateCompensationAppService.create(command));
      assertThat(exception.getMessage()).contains("刑事诉讼已终结");
    }

    @Test
    @DisplayName("已存在国家赔偿信息应该失败")
    void create_shouldFail_whenAlreadyExists() {
      // Given
      CreateStateCompensationCommand command = new CreateStateCompensationCommand();
      command.setMatterId(TEST_MATTER_ID);

      Matter matter = Matter.builder().id(TEST_MATTER_ID).caseType("STATE_COMP_ADMIN").build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      when(stateCompensationRepository.existsByMatterId(TEST_MATTER_ID)).thenReturn(true);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> stateCompensationAppService.create(command));
      assertThat(exception.getMessage()).contains("已存在国家赔偿信息");
    }
  }

  @Nested
  @DisplayName("更新国家赔偿信息测试")
  class UpdateTests {

    @Test
    @DisplayName("应该成功更新国家赔偿信息")
    void update_shouldSuccess() {
      // Given
      UpdateStateCompensationCommand command = new UpdateStateCompensationCommand();
      command.setId(TEST_COMPENSATION_ID);
      command.setObligorOrgName("更新后的机关");
      command.setApprovedAmount(new BigDecimal("80000.00"));

      MatterStateCompensation entity =
          MatterStateCompensation.builder()
              .id(TEST_COMPENSATION_ID)
              .matterId(TEST_MATTER_ID)
              .obligorOrgName("原机关")
              .criminalCaseTerminated(true)
              .build();

      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .matterNo("M2024001")
              .name("测试案件")
              .caseType("STATE_COMP_ADMIN")
              .build();

      when(stateCompensationRepository.getByIdOrThrow(eq(TEST_COMPENSATION_ID), anyString()))
          .thenReturn(entity);
      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      lenient()
          .when(stateCompensationRepository.updateById(any(MatterStateCompensation.class)))
          .thenReturn(true);

      // When
      StateCompensationDTO result = stateCompensationAppService.update(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(entity.getObligorOrgName()).isEqualTo("更新后的机关");
    }
  }

  @Nested
  @DisplayName("删除国家赔偿信息测试")
  class DeleteTests {

    @Test
    @DisplayName("应该成功删除国家赔偿信息")
    void delete_shouldSuccess() {
      // Given
      MatterStateCompensation entity =
          MatterStateCompensation.builder()
              .id(TEST_COMPENSATION_ID)
              .matterId(TEST_MATTER_ID)
              .build();

      when(stateCompensationRepository.getByIdOrThrow(eq(TEST_COMPENSATION_ID), anyString()))
          .thenReturn(entity);
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      lenient().when(stateCompensationRepository.removeById(TEST_COMPENSATION_ID)).thenReturn(true);

      // When
      stateCompensationAppService.delete(TEST_COMPENSATION_ID);

      // Then
      verify(stateCompensationRepository).removeById(TEST_COMPENSATION_ID);
    }

    @Test
    @DisplayName("应该成功根据案件ID删除国家赔偿信息")
    void deleteByMatterId_shouldSuccess() {
      // Given
      lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
      lenient().when(stateCompensationRepository.deleteByMatterId(TEST_MATTER_ID)).thenReturn(true);

      // When
      stateCompensationAppService.deleteByMatterId(TEST_MATTER_ID);

      // Then
      verify(stateCompensationRepository).deleteByMatterId(TEST_MATTER_ID);
    }
  }

  @Nested
  @DisplayName("查询国家赔偿信息测试")
  class QueryTests {

    @Test
    @DisplayName("应该成功根据案件ID获取国家赔偿信息")
    void getByMatterId_shouldSuccess() {
      // Given
      Matter matter = Matter.builder().id(TEST_MATTER_ID).matterNo("M2024001").name("测试案件").build();

      MatterStateCompensation entity =
          MatterStateCompensation.builder()
              .id(TEST_COMPENSATION_ID)
              .matterId(TEST_MATTER_ID)
              .obligorOrgName("测试机关")
              .build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(stateCompensationRepository.findByMatterId(TEST_MATTER_ID)).thenReturn(entity);

      // When
      StateCompensationDTO result = stateCompensationAppService.getByMatterId(TEST_MATTER_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
    }

    @Test
    @DisplayName("案件不存在国家赔偿信息应该返回null")
    void getByMatterId_shouldReturnNull_whenNotExists() {
      // Given
      Matter matter = Matter.builder().id(TEST_MATTER_ID).build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(stateCompensationRepository.findByMatterId(TEST_MATTER_ID)).thenReturn(null);

      // When
      StateCompensationDTO result = stateCompensationAppService.getByMatterId(TEST_MATTER_ID);

      // Then
      assertThat(result).isNull();
    }
  }
}
