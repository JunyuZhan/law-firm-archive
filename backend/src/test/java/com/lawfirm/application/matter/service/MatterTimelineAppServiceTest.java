package com.lawfirm.application.matter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.matter.dto.MatterTimelineDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DeadlineMapper;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.LetterApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.PaymentMapper;
import com.lawfirm.infrastructure.persistence.mapper.ScheduleMapper;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** MatterTimelineAppService 单元测试 测试项目时间线服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatterTimelineAppService 项目时间线服务测试")
class MatterTimelineAppServiceTest {

  private static final Long TEST_MATTER_ID = 100L;
  private static final Long TEST_USER_ID = 200L;
  private static final Long TEST_DEPT_ID = 300L;

  @Mock private MatterRepository matterRepository;

  @Mock private MatterAppService matterAppService;

  @Mock private TaskMapper taskMapper;

  @Mock private TimesheetMapper timesheetMapper;

  @Mock private FinanceContractMapper financeContractMapper;

  @Mock private PaymentMapper paymentMapper;

  @Mock private ScheduleMapper scheduleMapper;

  @Mock private DeadlineMapper deadlineMapper;

  @Mock private EvidenceMapper evidenceMapper;

  @Mock private LetterApplicationMapper letterApplicationMapper;

  @Mock private UserRepository userRepository;

  @InjectMocks private MatterTimelineAppService matterTimelineAppService;

  @Nested
  @DisplayName("获取项目时间线测试")
  class GetMatterTimelineTests {

    @Test
    @DisplayName("应该成功获取项目时间线")
    void getMatterTimeline_shouldSuccess() {
      // Given
      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .name("测试项目")
              .status("ACTIVE")
              .createdAt(LocalDateTime.now().minusDays(10))
              .createdBy(TEST_USER_ID)
              .updatedAt(LocalDateTime.now().minusDays(5))
              .updatedBy(TEST_USER_ID)
              .build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("ALL");
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
        when(matterAppService.getAccessibleMatterIds("ALL", TEST_USER_ID, TEST_DEPT_ID))
            .thenReturn(null);

        // Mock 各种 mapper 返回空列表
        when(taskMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(timesheetMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(paymentMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deadlineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(evidenceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(letterApplicationMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(financeContractMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(null);

        // When
        List<MatterTimelineDTO> result = matterTimelineAppService.getMatterTimeline(TEST_MATTER_ID);

        // Then
        assertThat(result).isNotNull();
        // 应该包含项目创建事件和状态更新事件
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
      }
    }

    @Test
    @DisplayName("无权访问的项目应该抛出异常")
    void getMatterTimeline_shouldFail_whenNoPermission() {
      // Given
      Matter matter = Matter.builder().id(TEST_MATTER_ID).build();

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("DEPT");
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
        // 返回不包含当前项目ID的列表
        when(matterAppService.getAccessibleMatterIds("DEPT", TEST_USER_ID, TEST_DEPT_ID))
            .thenReturn(List.of(999L, 1000L));

        // When & Then
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> matterTimelineAppService.getMatterTimeline(TEST_MATTER_ID));
        assertThat(exception.getMessage()).contains("无权访问");
      }
    }

    @Test
    @DisplayName("应该包含任务完成事件")
    void getMatterTimeline_shouldIncludeTaskCompleted() {
      // Given
      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .createdAt(LocalDateTime.now().minusDays(10))
              .createdBy(TEST_USER_ID)
              .build();

      Task task =
          Task.builder()
              .id(1L)
              .matterId(TEST_MATTER_ID)
              .title("测试任务")
              .status("COMPLETED")
              .completedAt(LocalDateTime.now().minusDays(5))
              .assigneeId(TEST_USER_ID)
              .deleted(false)
              .build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);
      when(taskMapper.selectList(any())).thenReturn(Collections.singletonList(task));

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("ALL");
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
        when(matterAppService.getAccessibleMatterIds("ALL", TEST_USER_ID, TEST_DEPT_ID))
            .thenReturn(null);

        // Mock 其他 mapper
        when(timesheetMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(paymentMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deadlineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(evidenceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(letterApplicationMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(financeContractMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(null);

        // When
        List<MatterTimelineDTO> result = matterTimelineAppService.getMatterTimeline(TEST_MATTER_ID);

        // Then
        assertThat(result).isNotNull();
        boolean hasTaskEvent =
            result.stream().anyMatch(e -> "TASK_COMPLETED".equals(e.getEventType()));
        assertThat(hasTaskEvent).isTrue();
      }
    }

    @Test
    @DisplayName("应该包含合同签署事件")
    void getMatterTimeline_shouldIncludeContractSigned() {
      // Given
      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .createdAt(LocalDateTime.now().minusDays(10))
              .createdBy(TEST_USER_ID)
              .build();

      Contract contract =
          Contract.builder()
              .id(1L)
              .matterId(TEST_MATTER_ID)
              .name("测试合同")
              .signDate(LocalDate.now().minusDays(3))
              .signerId(TEST_USER_ID)
              .build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);
      when(financeContractMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(contract);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("ALL");
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
        when(matterAppService.getAccessibleMatterIds("ALL", TEST_USER_ID, TEST_DEPT_ID))
            .thenReturn(null);

        // Mock 其他 mapper
        when(taskMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(timesheetMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(paymentMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deadlineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(evidenceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(letterApplicationMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        List<MatterTimelineDTO> result = matterTimelineAppService.getMatterTimeline(TEST_MATTER_ID);

        // Then
        assertThat(result).isNotNull();
        boolean hasContractEvent =
            result.stream().anyMatch(e -> "CONTRACT_SIGNED".equals(e.getEventType()));
        assertThat(hasContractEvent).isTrue();
      }
    }

    @Test
    @DisplayName("应该包含收款记录事件")
    void getMatterTimeline_shouldIncludePaymentReceived() {
      // Given
      Matter matter =
          Matter.builder()
              .id(TEST_MATTER_ID)
              .createdAt(LocalDateTime.now().minusDays(10))
              .createdBy(TEST_USER_ID)
              .build();

      Payment payment =
          Payment.builder()
              .id(1L)
              .matterId(TEST_MATTER_ID)
              .amount(new BigDecimal("10000.00"))
              .paymentDate(LocalDate.now().minusDays(2))
              .status("CONFIRMED")
              .createdBy(TEST_USER_ID)
              .deleted(false)
              .build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);
      when(paymentMapper.selectList(any())).thenReturn(Collections.singletonList(payment));

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("ALL");
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
        when(matterAppService.getAccessibleMatterIds("ALL", TEST_USER_ID, TEST_DEPT_ID))
            .thenReturn(null);

        // Mock 其他 mapper
        when(taskMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(timesheetMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(deadlineMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(evidenceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(letterApplicationMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(financeContractMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(null);

        // When
        List<MatterTimelineDTO> result = matterTimelineAppService.getMatterTimeline(TEST_MATTER_ID);

        // Then
        assertThat(result).isNotNull();
        boolean hasPaymentEvent =
            result.stream().anyMatch(e -> "PAYMENT_RECEIVED".equals(e.getEventType()));
        assertThat(hasPaymentEvent).isTrue();
      }
    }
  }
}
