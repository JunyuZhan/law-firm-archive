package com.lawfirm.application.matter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateDeadlineCommand;
import com.lawfirm.application.matter.command.UpdateDeadlineCommand;
import com.lawfirm.application.matter.dto.DeadlineDTO;
import com.lawfirm.application.matter.dto.DeadlineQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Deadline;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.DeadlineRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DeadlineMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DeadlineAppService 单元测试
 *
 * <p>测试期限提醒应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeadlineAppService 期限提醒服务测试")
class DeadlineAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_MATTER_ID = 100L;
  private static final Long TEST_DEADLINE_ID = 1000L;

  @Mock private DeadlineRepository deadlineRepository;

  @Mock private DeadlineMapper deadlineMapper;

  @Mock private MatterRepository matterRepository;

  @Mock private UserRepository userRepository;

  @Mock private NotificationAppService notificationAppService;

  @Mock private MatterAppService matterAppService;

  @InjectMocks private DeadlineAppService deadlineAppService;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    // Manually inject matterAppService since it uses @Lazy setter injection
    deadlineAppService.setMatterAppService(matterAppService);
    // Mock the getBaseMapper chain to return deadlineMapper
    lenient().when(deadlineRepository.getBaseMapper()).thenReturn(deadlineMapper);
  }

  @Nested
  @DisplayName("分页查询期限测试")
  class ListDeadlinesTests {

    @Test
    @DisplayName("应该分页查询期限列表")
    void listDeadlines_shouldReturnPagedResult() {
      // Given
      DeadlineQueryDTO query = new DeadlineQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
      Page<Deadline> page = new Page<>(1, 10);
      page.setRecords(List.of(deadline));
      page.setTotal(1);

      @SuppressWarnings("unchecked")
      Page<Deadline> pageParam1 = any(Page.class);
      when(deadlineMapper.selectDeadlinePage(pageParam1, eq(query))).thenReturn(page);

      // When
      PageResult<DeadlineDTO> result = deadlineAppService.listDeadlines(query);

      // Then
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getDeadlineName()).isEqualTo("举证期限");
    }

    @Test
    @DisplayName("空结果时应返回空分页")
    void listDeadlines_shouldReturnEmptyWhenNoResults() {
      // Given
      DeadlineQueryDTO query = new DeadlineQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Page<Deadline> page = new Page<>(1, 10);
      page.setRecords(new ArrayList<>());
      page.setTotal(0);

      @SuppressWarnings("unchecked")
      Page<Deadline> pageParam2 = any(Page.class);
      when(deadlineMapper.selectDeadlinePage(pageParam2, eq(query))).thenReturn(page);

      // When
      PageResult<DeadlineDTO> result = deadlineAppService.listDeadlines(query);

      // Then
      assertThat(result.getRecords()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("获取期限详情测试")
  class GetDeadlineByIdTests {

    @Test
    @DisplayName("应该获取期限详情")
    void getDeadlineById_shouldReturnDeadline() {
      // Given
      Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
      when(deadlineRepository.getByIdOrThrow(TEST_DEADLINE_ID, "期限提醒不存在")).thenReturn(deadline);

      // When
      DeadlineDTO result = deadlineAppService.getDeadlineById(TEST_DEADLINE_ID);

      // Then
      assertThat(result.getId()).isEqualTo(TEST_DEADLINE_ID);
      assertThat(result.getDeadlineName()).isEqualTo("举证期限");
    }

    @Test
    @DisplayName("期限不存在时应抛出异常")
    void getDeadlineById_shouldThrowException_whenNotFound() {
      // Given
      when(deadlineRepository.getByIdOrThrow(999L, "期限提醒不存在"))
          .thenThrow(new BusinessException("期限提醒不存在"));

      // When & Then
      assertThatThrownBy(() -> deadlineAppService.getDeadlineById(999L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("期限提醒不存在");
    }
  }

  @Nested
  @DisplayName("按项目查询期限测试")
  class GetDeadlinesByMatterIdTests {

    @Test
    @DisplayName("应该获取项目的期限列表")
    void getDeadlinesByMatterId_shouldReturnDeadlines() {
      // Given
      Deadline deadline1 = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
      Deadline deadline2 = createTestDeadline(TEST_DEADLINE_ID + 1, "答辩期限", "REPLY");

      when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在"))
          .thenReturn(createTestMatter(TEST_MATTER_ID));
      when(deadlineRepository.findByMatterId(TEST_MATTER_ID))
          .thenReturn(List.of(deadline1, deadline2));

      // When
      List<DeadlineDTO> result = deadlineAppService.getDeadlinesByMatterId(TEST_MATTER_ID);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getDeadlineName()).isEqualTo("举证期限");
      assertThat(result.get(1).getDeadlineName()).isEqualTo("答辩期限");
    }

    @Test
    @DisplayName("项目不存在时应抛出异常")
    void getDeadlinesByMatterId_shouldThrowException_whenMatterNotFound() {
      // Given
      when(matterRepository.getByIdOrThrow(999L, "项目不存在"))
          .thenThrow(new BusinessException("项目不存在"));

      // When & Then
      assertThatThrownBy(() -> deadlineAppService.getDeadlinesByMatterId(999L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("项目不存在");
    }
  }

  @Nested
  @DisplayName("创建期限测试")
  class CreateDeadlineTests {

    @Test
    @DisplayName("应该成功创建期限")
    void createDeadline_shouldReturnDeadlineDTO() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        CreateDeadlineCommand command = new CreateDeadlineCommand();
        command.setMatterId(TEST_MATTER_ID);
        command.setDeadlineType("EVIDENCE_SUBMISSION");
        command.setDeadlineName("举证期限");
        command.setBaseDate(LocalDate.now());
        command.setDeadlineDate(LocalDate.now().plusDays(15));
        command.setReminderDays(7);

        Matter matter = createTestMatter(TEST_MATTER_ID);

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在")).thenReturn(matter);
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
        when(deadlineMapper.insert(any(Deadline.class))).thenReturn(1);

        // When
        DeadlineDTO result = deadlineAppService.createDeadline(command);

        // Then
        assertThat(result.getDeadlineName()).isEqualTo("举证期限");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(deadlineMapper).insert(any(Deadline.class));
      }
    }

    @Test
    @DisplayName("期限日期早于基准日期时应抛出异常")
    void createDeadline_shouldThrowException_whenDeadlineBeforeBase() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        CreateDeadlineCommand command = new CreateDeadlineCommand();
        command.setMatterId(TEST_MATTER_ID);
        command.setDeadlineName("举证期限");
        command.setBaseDate(LocalDate.now().plusDays(10));
        command.setDeadlineDate(LocalDate.now()); // 早于基准日期

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在"))
            .thenReturn(createTestMatter(TEST_MATTER_ID));
        // 所有权验证在日期验证之前调用
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);

        // When & Then
        assertThatThrownBy(() -> deadlineAppService.createDeadline(command))
            .isInstanceOf(BusinessException.class)
            .hasMessage("期限日期不能早于基准日期");
      }
    }
  }

  @Nested
  @DisplayName("自动创建期限测试")
  class AutoCreateDeadlinesTests {

    @Test
    @DisplayName("应该为诉讼案件自动创建期限")
    void autoCreateDeadlines_shouldCreateForLitigation() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Matter matter = createTestMatter(TEST_MATTER_ID);
        matter.setMatterType("LITIGATION");
        matter.setBusinessType("CIVIL");
        matter.setFilingDate(LocalDate.now());

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在")).thenReturn(matter);
        when(deadlineRepository.findByMatterId(TEST_MATTER_ID)).thenReturn(new ArrayList<>());
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
        when(deadlineMapper.insert(any(Deadline.class))).thenReturn(1);

        // When
        deadlineAppService.autoCreateDeadlines(TEST_MATTER_ID);

        // Then
        // 民事诉讼应该创建举证期限和答辩期限两个期限
        verify(deadlineMapper, atLeast(2)).insert(any(Deadline.class));
      }
    }

    @Test
    @DisplayName("非诉讼案件不应创建期限")
    void autoCreateDeadlines_shouldNotCreateForNonLitigation() {
      // Given
      Matter matter = createTestMatter(TEST_MATTER_ID);
      matter.setMatterType("NON_LITIGATION");

      when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在")).thenReturn(matter);

      // When
      deadlineAppService.autoCreateDeadlines(TEST_MATTER_ID);

      // Then
      verify(deadlineMapper, never()).insert(any(Deadline.class));
    }

    @Test
    @DisplayName("已存在期限时不应重复创建")
    void autoCreateDeadlines_shouldNotCreateWhenExists() {
      // Given
      Matter matter = createTestMatter(TEST_MATTER_ID);
      matter.setMatterType("LITIGATION");
      matter.setFilingDate(LocalDate.now());

      when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在")).thenReturn(matter);
      when(deadlineRepository.findByMatterId(TEST_MATTER_ID))
          .thenReturn(List.of(createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION")));

      // When
      deadlineAppService.autoCreateDeadlines(TEST_MATTER_ID);

      // Then
      verify(deadlineMapper, never()).insert(any(Deadline.class));
    }
  }

  @Nested
  @DisplayName("更新期限测试")
  class UpdateDeadlineTests {

    @Test
    @DisplayName("应该成功更新期限")
    void updateDeadline_shouldReturnDeadlineDTO() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
        deadline.setStatus("ACTIVE");

        UpdateDeadlineCommand command = new UpdateDeadlineCommand();
        command.setId(TEST_DEADLINE_ID);
        command.setDeadlineName("更新后的期限");
        command.setReminderDays(5);

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(deadlineRepository.getByIdOrThrow(TEST_DEADLINE_ID, "期限提醒不存在")).thenReturn(deadline);
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
        when(deadlineMapper.updateById(any(Deadline.class))).thenReturn(1);

        // When
        DeadlineDTO result = deadlineAppService.updateDeadline(command);

        // Then
        assertThat(result.getDeadlineName()).isEqualTo("更新后的期限");
        assertThat(deadline.getReminderDays()).isEqualTo(5);
      }
    }

    @Test
    @DisplayName("非有效状态不能更新")
    void updateDeadline_shouldThrowException_whenNotActive() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
        deadline.setStatus("COMPLETED");

        UpdateDeadlineCommand command = new UpdateDeadlineCommand();
        command.setId(TEST_DEADLINE_ID);

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(deadlineRepository.getByIdOrThrow(TEST_DEADLINE_ID, "期限提醒不存在")).thenReturn(deadline);
        // 所有权验证在状态验证之前调用
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);

        // When & Then
        assertThatThrownBy(() -> deadlineAppService.updateDeadline(command))
            .isInstanceOf(BusinessException.class)
            .hasMessage("只有有效状态的期限才能更新");
      }
    }
  }

  @Nested
  @DisplayName("完成期限测试")
  class CompleteDeadlineTests {

    @Test
    @DisplayName("应该成功完成期限")
    void completeDeadline_shouldCompleteDeadline() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
        deadline.setStatus("ACTIVE");

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(deadlineRepository.getByIdOrThrow(TEST_DEADLINE_ID, "期限提醒不存在")).thenReturn(deadline);
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
        when(deadlineMapper.updateById(any(Deadline.class))).thenReturn(1);

        // When
        DeadlineDTO result = deadlineAppService.completeDeadline(TEST_DEADLINE_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(deadline.getCompletedAt()).isNotNull();
      }
    }

    @Test
    @DisplayName("非有效状态不能完成")
    void completeDeadline_shouldThrowException_whenNotActive() {
      // Given
      Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
      deadline.setStatus("COMPLETED");

      when(deadlineRepository.getByIdOrThrow(TEST_DEADLINE_ID, "期限提醒不存在")).thenReturn(deadline);

      // When & Then
      assertThatThrownBy(() -> deadlineAppService.completeDeadline(TEST_DEADLINE_ID))
          .isInstanceOf(BusinessException.class)
          .hasMessage("只有有效状态的期限才能标记为完成");
    }
  }

  @Nested
  @DisplayName("删除期限测试")
  class DeleteDeadlineTests {

    @Test
    @DisplayName("应该成功删除期限")
    void deleteDeadline_shouldDeleteDeadline() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(deadlineRepository.getByIdOrThrow(TEST_DEADLINE_ID, "期限提醒不存在")).thenReturn(deadline);
        doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
        when(deadlineRepository.softDelete(TEST_DEADLINE_ID)).thenReturn(true);

        // When
        deadlineAppService.deleteDeadline(TEST_DEADLINE_ID);

        // Then
        verify(deadlineRepository).softDelete(TEST_DEADLINE_ID);
      }
    }
  }

  @Nested
  @DisplayName("获取我的期限测试")
  class GetMyUpcomingDeadlinesTests {

    @Test
    @DisplayName("应该获取我的即将到期期限")
    void getMyUpcomingDeadlines_shouldReturnDeadlines() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        when(deadlineRepository.findMyUpcoming(TEST_USER_ID, 7, 10)).thenReturn(List.of(deadline));

        // When
        List<DeadlineDTO> result = deadlineAppService.getMyUpcomingDeadlines(7, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeadlineName()).isEqualTo("举证期限");
      }
    }
  }

  @Nested
  @DisplayName("发送期限提醒测试")
  class SendDeadlineRemindersTests {

    @Test
    @DisplayName("应该发送期限提醒")
    void sendDeadlineReminders_shouldSendReminders() {
      // Given
      Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
      deadline.setReminderSent(false);
      deadline.setDeadlineDate(LocalDate.now().plusDays(3));

      Matter matter = createTestMatter(TEST_MATTER_ID);
      matter.setLeadLawyerId(TEST_USER_ID);

      when(deadlineRepository.findNeedReminder()).thenReturn(List.of(deadline));
      when(matterRepository.getByIdOrThrow(TEST_MATTER_ID, "项目不存在")).thenReturn(matter);
      doNothing()
          .when(notificationAppService)
          .sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());
      when(deadlineMapper.updateById(any(Deadline.class))).thenReturn(1);

      // When
      deadlineAppService.sendDeadlineReminders();

      // Then
      assertThat(deadline.getReminderSent()).isTrue();
      assertThat(deadline.getReminderSentAt()).isNotNull();
      verify(notificationAppService)
          .sendSystemNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());
    }
  }

  @Nested
  @DisplayName("更新过期期限测试")
  class UpdateExpiredDeadlinesTests {

    @Test
    @DisplayName("应该更新过期期限状态")
    void updateExpiredDeadlines_shouldUpdateStatus() {
      // Given
      Deadline deadline = createTestDeadline(TEST_DEADLINE_ID, "举证期限", "EVIDENCE_SUBMISSION");
      deadline.setDeadlineDate(LocalDate.now().minusDays(1)); // 已过期
      deadline.setStatus("ACTIVE");

      when(deadlineRepository.findUpcomingDeadlines()).thenReturn(List.of(deadline));
      when(deadlineMapper.updateById(any(Deadline.class))).thenReturn(1);

      // When
      deadlineAppService.updateExpiredDeadlines();

      // Then
      assertThat(deadline.getStatus()).isEqualTo("EXPIRED");
    }
  }

  // ========== 辅助方法 ==========

  private Deadline createTestDeadline(Long id, String name, String type) {
    return Deadline.builder()
        .id(id)
        .matterId(TEST_MATTER_ID)
        .deadlineType(type)
        .deadlineName(name)
        .baseDate(LocalDate.now())
        .deadlineDate(LocalDate.now().plusDays(15))
        .reminderDays(7)
        .reminderSent(false)
        .status("ACTIVE")
        .createdBy(TEST_USER_ID)
        .build();
  }

  private Matter createTestMatter(Long id) {
    return Matter.builder()
        .id(id)
        .matterNo("M2026001")
        .name("测试项目")
        .matterType("LITIGATION")
        .businessType("CIVIL")
        .status("ACTIVE")
        .build();
  }
}
