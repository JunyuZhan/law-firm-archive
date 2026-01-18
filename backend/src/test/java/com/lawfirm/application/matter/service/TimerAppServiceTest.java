package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.command.StartTimerCommand;
import com.lawfirm.application.matter.dto.TimerSessionDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.TimerSession;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TimerSessionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TimerAppService 单元测试
 * 测试计时器服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TimerAppService 计时器服务测试")
class TimerAppServiceTest {

    private static final Long TEST_SESSION_ID = 100L;
    private static final Long TEST_MATTER_ID = 200L;
    private static final Long TEST_USER_ID = 300L;

    @Mock
    private TimerSessionRepository timerSessionRepository;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private TimesheetAppService timesheetAppService;

    @InjectMocks
    private TimerAppService timerAppService;

    @Nested
    @DisplayName("开始计时测试")
    class StartTimerTests {

        @Test
        @DisplayName("应该成功开始计时")
        void startTimer_shouldSuccess() {
            // Given
            StartTimerCommand command = new StartTimerCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setWorkType("CONSULTATION");
            command.setWorkContent("咨询工作");
            command.setBillable(true);

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试案件")
                    .build();

            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(null);
            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            when(timerSessionRepository.save(any(TimerSession.class))).thenAnswer(invocation -> {
                TimerSession session = invocation.getArgument(0);
                session.setId(TEST_SESSION_ID);
                return true;
            });

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                TimerSessionDTO result = timerAppService.startTimer(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
                assertThat(result.getStatus()).isEqualTo("RUNNING");
                verify(timerSessionRepository).save(any(TimerSession.class));
            }
        }

        @Test
        @DisplayName("已有正在运行的计时器时应该失败")
        void startTimer_shouldFail_whenAlreadyRunning() {
            // Given
            StartTimerCommand command = new StartTimerCommand();
            command.setMatterId(TEST_MATTER_ID);

            TimerSession runningSession = TimerSession.builder()
                    .id(TEST_SESSION_ID)
                    .status("RUNNING")
                    .build();

            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(runningSession);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When & Then
                BusinessException exception = assertThrows(BusinessException.class,
                        () -> timerAppService.startTimer(command));
                assertThat(exception.getMessage()).contains("正在运行的计时器");
            }
        }
    }

    @Nested
    @DisplayName("暂停计时测试")
    class PauseTimerTests {

        @Test
        @DisplayName("应该成功暂停计时")
        void pauseTimer_shouldSuccess() {
            // Given
            TimerSession session = TimerSession.builder()
                    .id(TEST_SESSION_ID)
                    .matterId(TEST_MATTER_ID)
                    .userId(TEST_USER_ID)
                    .startTime(LocalDateTime.now().minusHours(1))
                    .elapsedSeconds(0L)
                    .status("RUNNING")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试案件")
                    .build();

            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(session);
            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            lenient().when(timerSessionRepository.updateById(any(TimerSession.class))).thenReturn(true);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                TimerSessionDTO result = timerAppService.pauseTimer();

                // Then
                assertThat(result).isNotNull();
                assertThat(session.getStatus()).isEqualTo("PAUSED");
                assertThat(session.getPauseTime()).isNotNull();
            }
        }

        @Test
        @DisplayName("没有正在运行的计时器时应该失败")
        void pauseTimer_shouldFail_whenNoRunning() {
            // Given
            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(null);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When & Then
                BusinessException exception = assertThrows(BusinessException.class,
                        () -> timerAppService.pauseTimer());
                assertThat(exception.getMessage()).contains("没有正在运行的计时器");
            }
        }
    }

    @Nested
    @DisplayName("继续计时测试")
    class ResumeTimerTests {

        @Test
        @DisplayName("应该成功继续计时")
        void resumeTimer_shouldSuccess() {
            // Given
            TimerSession session = TimerSession.builder()
                    .id(TEST_SESSION_ID)
                    .matterId(TEST_MATTER_ID)
                    .userId(TEST_USER_ID)
                    .status("PAUSED")
                    .elapsedSeconds(3600L)
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试案件")
                    .build();

            when(timerSessionRepository.findPausedByUserId(TEST_USER_ID)).thenReturn(session);
            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            lenient().when(timerSessionRepository.updateById(any(TimerSession.class))).thenReturn(true);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                TimerSessionDTO result = timerAppService.resumeTimer();

                // Then
                assertThat(result).isNotNull();
                assertThat(session.getStatus()).isEqualTo("RUNNING");
                assertThat(session.getResumeTime()).isNotNull();
            }
        }

        @Test
        @DisplayName("没有已暂停的计时器时应该失败")
        void resumeTimer_shouldFail_whenNoPaused() {
            // Given
            when(timerSessionRepository.findPausedByUserId(TEST_USER_ID)).thenReturn(null);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When & Then
                BusinessException exception = assertThrows(BusinessException.class,
                        () -> timerAppService.resumeTimer());
                assertThat(exception.getMessage()).contains("没有已暂停的计时器");
            }
        }
    }

    @Nested
    @DisplayName("停止计时测试")
    class StopTimerTests {

        @Test
        @DisplayName("应该成功停止计时并保存工时记录")
        void stopTimer_shouldSuccess() {
            // Given
            TimerSession session = TimerSession.builder()
                    .id(TEST_SESSION_ID)
                    .matterId(TEST_MATTER_ID)
                    .userId(TEST_USER_ID)
                    .workType("CONSULTATION")
                    .workContent("咨询工作")
                    .billable(true)
                    .startTime(LocalDateTime.now().minusHours(2))
                    .elapsedSeconds(0L)
                    .status("RUNNING")
                    .build();

            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(session);
            lenient().when(timerSessionRepository.updateById(any(TimerSession.class))).thenReturn(true);
            lenient().when(timesheetAppService.createTimesheet(any())).thenReturn(null);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                timerAppService.stopTimer();

                // Then
                assertThat(session.getStatus()).isEqualTo("STOPPED");
                verify(timesheetAppService).createTimesheet(any());
            }
        }

        @Test
        @DisplayName("停止已暂停的计时器应该成功")
        void stopTimer_shouldSuccess_whenPaused() {
            // Given
            TimerSession session = TimerSession.builder()
                    .id(TEST_SESSION_ID)
                    .matterId(TEST_MATTER_ID)
                    .userId(TEST_USER_ID)
                    .elapsedSeconds(3600L)
                    .status("PAUSED")
                    .build();

            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(null);
            when(timerSessionRepository.findPausedByUserId(TEST_USER_ID)).thenReturn(session);
            lenient().when(timerSessionRepository.updateById(any(TimerSession.class))).thenReturn(true);
            lenient().when(timesheetAppService.createTimesheet(any())).thenReturn(null);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                timerAppService.stopTimer();

                // Then
                assertThat(session.getStatus()).isEqualTo("STOPPED");
                verify(timesheetAppService).createTimesheet(any());
            }
        }

        @Test
        @DisplayName("没有活动的计时器时应该失败")
        void stopTimer_shouldFail_whenNoActive() {
            // Given
            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(null);
            when(timerSessionRepository.findPausedByUserId(TEST_USER_ID)).thenReturn(null);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When & Then
                BusinessException exception = assertThrows(BusinessException.class,
                        () -> timerAppService.stopTimer());
                assertThat(exception.getMessage()).contains("没有正在运行的计时器");
            }
        }
    }

    @Nested
    @DisplayName("获取计时器状态测试")
    class GetTimerStatusTests {

        @Test
        @DisplayName("应该成功获取正在运行的计时器状态")
        void getTimerStatus_shouldSuccess_whenRunning() {
            // Given
            TimerSession session = TimerSession.builder()
                    .id(TEST_SESSION_ID)
                    .matterId(TEST_MATTER_ID)
                    .startTime(LocalDateTime.now().minusHours(1))
                    .elapsedSeconds(0L)
                    .status("RUNNING")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试案件")
                    .build();

            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(session);
            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                TimerSessionDTO result = timerAppService.getTimerStatus();

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo("RUNNING");
            }
        }

        @Test
        @DisplayName("没有活动计时器时应该返回null")
        void getTimerStatus_shouldReturnNull_whenNoActive() {
            // Given
            when(timerSessionRepository.findRunningByUserId(TEST_USER_ID)).thenReturn(null);
            when(timerSessionRepository.findPausedByUserId(TEST_USER_ID)).thenReturn(null);

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

                // When
                TimerSessionDTO result = timerAppService.getTimerStatus();

                // Then
                assertThat(result).isNull();
            }
        }
    }
}
