package com.lawfirm.application.workbench.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.command.CreateScheduledReportCommand;
import com.lawfirm.application.workbench.dto.ScheduledReportDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import com.lawfirm.domain.workbench.entity.ScheduledReport;
import com.lawfirm.domain.workbench.entity.ScheduledReportLog;
import com.lawfirm.domain.workbench.repository.ReportTemplateRepository;
import com.lawfirm.domain.workbench.repository.ScheduledReportLogRepository;
import com.lawfirm.domain.workbench.repository.ScheduledReportRepository;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportLogMapper;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

/** ScheduledReportAppService 单元测试 测试定时报表服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledReportAppService 定时报表服务测试")
class ScheduledReportAppServiceTest {

  private static final Long TEST_TASK_ID = 100L;
  private static final Long TEST_TEMPLATE_ID = 200L;
  private static final Long TEST_USER_ID = 300L;

  @Mock private ScheduledReportRepository scheduledReportRepository;

  @Mock private ScheduledReportMapper scheduledReportMapper;

  @Mock private ScheduledReportLogRepository logRepository;

  @Mock private ScheduledReportLogMapper logMapper;

  @Mock private ReportTemplateRepository templateRepository;

  @Mock private CustomReportAppService customReportAppService;

  @Mock private NotificationAppService notificationAppService;

  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private ScheduledReportAppService scheduledReportAppService;

  @Nested
  @DisplayName("创建定时报表任务测试")
  class CreateScheduledReportTests {

    @Test
    @DisplayName("应该成功创建每日执行的定时报表任务")
    void createScheduledReport_shouldSuccess_forDaily() {
      // Given
      CreateScheduledReportCommand command = new CreateScheduledReportCommand();
      command.setTaskName("每日收入报表");
      command.setTemplateId(TEST_TEMPLATE_ID);
      command.setScheduleType("DAILY");
      command.setExecuteTime("09:00");
      command.setOutputFormat("EXCEL");
      command.setNotifyEnabled(true);
      command.setNotifyUserIds(List.of(TEST_USER_ID));

      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).templateName("收入报表模板").build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(scheduledReportRepository.save(any(ScheduledReport.class)))
          .thenAnswer(
              invocation -> {
                ScheduledReport task = invocation.getArgument(0);
                task.setId(TEST_TASK_ID);
                task.setTaskNo("TASK2024001");
                return true;
              });

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getRealName).thenReturn("测试用户");

        // When
        ScheduledReportDTO result = scheduledReportAppService.createScheduledReport(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskName()).isEqualTo("每日收入报表");
        verify(scheduledReportRepository).save(any(ScheduledReport.class));
      }
    }

    @Test
    @DisplayName("每日执行缺少执行时间应该失败")
    void createScheduledReport_shouldFail_whenDailyMissingTime() {
      // Given
      CreateScheduledReportCommand command = new CreateScheduledReportCommand();
      command.setTemplateId(TEST_TEMPLATE_ID);
      command.setScheduleType("DAILY");
      command.setExecuteTime(null);

      ReportTemplate template = ReportTemplate.builder().id(TEST_TEMPLATE_ID).build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> scheduledReportAppService.createScheduledReport(command));
      assertThat(exception.getMessage()).contains("执行时间");
    }

    @Test
    @DisplayName("每周执行缺少星期几应该失败")
    void createScheduledReport_shouldFail_whenWeeklyMissingDay() {
      // Given
      CreateScheduledReportCommand command = new CreateScheduledReportCommand();
      command.setTemplateId(TEST_TEMPLATE_ID);
      command.setScheduleType("WEEKLY");
      command.setExecuteTime("09:00");
      command.setExecuteDayOfWeek(null);

      ReportTemplate template = ReportTemplate.builder().id(TEST_TEMPLATE_ID).build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> scheduledReportAppService.createScheduledReport(command));
      assertThat(exception.getMessage()).contains("星期几");
    }

    @Test
    @DisplayName("自定义调度缺少Cron表达式应该失败")
    void createScheduledReport_shouldFail_whenCronMissingExpression() {
      // Given
      CreateScheduledReportCommand command = new CreateScheduledReportCommand();
      command.setTemplateId(TEST_TEMPLATE_ID);
      command.setScheduleType("CRON");
      command.setCronExpression(null);

      ReportTemplate template = ReportTemplate.builder().id(TEST_TEMPLATE_ID).build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> scheduledReportAppService.createScheduledReport(command));
      assertThat(exception.getMessage()).contains("Cron表达式");
    }
  }

  @Nested
  @DisplayName("更新定时报表任务测试")
  class UpdateScheduledReportTests {

    @Test
    @DisplayName("应该成功更新定时报表任务")
    void updateScheduledReport_shouldSuccess() {
      // Given
      CreateScheduledReportCommand command = new CreateScheduledReportCommand();
      command.setTaskName("更新后的任务名");
      command.setTemplateId(TEST_TEMPLATE_ID);
      command.setScheduleType("DAILY");
      command.setExecuteTime("10:00");

      ScheduledReport task =
          ScheduledReport.builder()
              .id(TEST_TASK_ID)
              .taskNo("TASK2024001")
              .taskName("原任务名")
              .scheduleType("DAILY")
              .executeTime("09:00")
              .build();

      ReportTemplate template = ReportTemplate.builder().id(TEST_TEMPLATE_ID).build();

      when(scheduledReportRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString()))
          .thenReturn(task);
      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      lenient()
          .when(scheduledReportRepository.updateById(any(ScheduledReport.class)))
          .thenReturn(true);
      lenient().when(templateRepository.findById(TEST_TEMPLATE_ID)).thenReturn(template);

      // When
      ScheduledReportDTO result =
          scheduledReportAppService.updateScheduledReport(TEST_TASK_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(task.getTaskName()).isEqualTo("更新后的任务名");
      assertThat(task.getExecuteTime()).isEqualTo("10:00");
    }
  }

  @Nested
  @DisplayName("删除定时报表任务测试")
  class DeleteScheduledReportTests {

    @Test
    @DisplayName("应该成功删除定时报表任务")
    void deleteScheduledReport_shouldSuccess() {
      // Given
      ScheduledReport task =
          ScheduledReport.builder().id(TEST_TASK_ID).taskNo("TASK2024001").build();

      when(scheduledReportRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString()))
          .thenReturn(task);
      lenient().when(scheduledReportRepository.softDelete(TEST_TASK_ID)).thenReturn(true);

      // When
      scheduledReportAppService.deleteScheduledReport(TEST_TASK_ID);

      // Then
      verify(scheduledReportRepository).softDelete(TEST_TASK_ID);
    }
  }

  @Nested
  @DisplayName("修改任务状态测试")
  class ChangeTaskStatusTests {

    @Test
    @DisplayName("应该成功暂停任务")
    void changeTaskStatus_shouldSuccess_pause() {
      // Given
      ScheduledReport task =
          ScheduledReport.builder().id(TEST_TASK_ID).taskNo("TASK2024001").status("ACTIVE").build();

      when(scheduledReportRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString()))
          .thenReturn(task);
      lenient()
          .when(scheduledReportRepository.updateById(any(ScheduledReport.class)))
          .thenReturn(true);

      // When
      scheduledReportAppService.changeTaskStatus(TEST_TASK_ID, "PAUSED");

      // Then
      assertThat(task.getStatus()).isEqualTo("PAUSED");
    }

    @Test
    @DisplayName("恢复任务应该重新计算下次执行时间")
    void changeTaskStatus_shouldRecalculateNextTime_whenResume() {
      // Given
      ScheduledReport task =
          ScheduledReport.builder()
              .id(TEST_TASK_ID)
              .taskNo("TASK2024001")
              .status("PAUSED")
              .scheduleType("DAILY")
              .executeTime("09:00")
              .build();

      when(scheduledReportRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString()))
          .thenReturn(task);
      lenient()
          .when(scheduledReportRepository.updateById(any(ScheduledReport.class)))
          .thenReturn(true);

      // When
      scheduledReportAppService.changeTaskStatus(TEST_TASK_ID, "ACTIVE");

      // Then
      assertThat(task.getStatus()).isEqualTo("ACTIVE");
      assertThat(task.getNextExecuteTime()).isNotNull();
    }
  }

  @Nested
  @DisplayName("查询定时报表任务测试")
  class QueryScheduledReportTests {

    @Test
    @DisplayName("应该成功分页查询定时报表任务")
    void listScheduledReports_shouldSuccess() {
      // Given
      ScheduledReport task =
          ScheduledReport.builder()
              .id(TEST_TASK_ID)
              .taskNo("TASK2024001")
              .taskName("测试任务")
              .templateId(TEST_TEMPLATE_ID)
              .scheduleType("DAILY")
              .executeTime("09:00")
              .status("ACTIVE")
              .reportParameters("{}")
              .outputFormat("EXCEL")
              .build();

      Page<ScheduledReport> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(task));
      page.setTotal(1);

      when(scheduledReportMapper.selectScheduledReportPage(
              ArgumentMatchers.<Page<ScheduledReport>>any(), any(), any(), any()))
          .thenReturn(page);
      lenient().when(templateRepository.findById(any())).thenReturn(new ReportTemplate());

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        var result = scheduledReportAppService.listScheduledReports(1, 10, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
      }
    }

    @Test
    @DisplayName("应该成功根据ID查询定时报表任务")
    void getScheduledReportById_shouldSuccess() {
      // Given
      ScheduledReport task =
          ScheduledReport.builder()
              .id(TEST_TASK_ID)
              .taskNo("TASK2024001")
              .taskName("测试任务")
              .templateId(TEST_TEMPLATE_ID)
              .scheduleType("DAILY")
              .executeTime("09:00")
              .reportParameters("{}")
              .outputFormat("EXCEL")
              .build();

      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).templateName("测试模板").build();

      when(scheduledReportRepository.getByIdOrThrow(eq(TEST_TASK_ID), anyString()))
          .thenReturn(task);
      when(templateRepository.findById(TEST_TEMPLATE_ID)).thenReturn(template);

      // When
      ScheduledReportDTO result = scheduledReportAppService.getScheduledReportById(TEST_TASK_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTaskNo()).isEqualTo("TASK2024001");
    }
  }

  @Nested
  @DisplayName("查询执行记录测试")
  class QueryExecuteLogsTests {

    @Test
    @DisplayName("应该成功分页查询执行记录")
    void listExecuteLogs_shouldSuccess() {
      // Given
      ScheduledReportLog log =
          ScheduledReportLog.builder()
              .id(1L)
              .taskId(TEST_TASK_ID)
              .taskNo("TASK2024001")
              .status("SUCCESS")
              .build();

      Page<ScheduledReportLog> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(log));
      page.setTotal(1);

      when(logMapper.selectLogPage(ArgumentMatchers.<Page<ScheduledReportLog>>any(), any(), any()))
          .thenReturn(page);

      // When
      var result = scheduledReportAppService.listExecuteLogs(TEST_TASK_ID, 1, 10, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
    }
  }
}
