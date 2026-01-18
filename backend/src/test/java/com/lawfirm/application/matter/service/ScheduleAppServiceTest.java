package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateScheduleCommand;
import com.lawfirm.application.matter.dto.ScheduleDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.domain.matter.repository.ScheduleRepository;
import com.lawfirm.infrastructure.persistence.mapper.ScheduleMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ScheduleAppService 单元测试
 * 测试日程管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleAppService 日程服务测试")
class ScheduleAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_MATTER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleAppService scheduleAppService;

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
    @DisplayName("创建日程测试")
    class CreateScheduleTests {

        @Test
        @DisplayName("应该成功创建日程")
        void createSchedule_shouldSuccess() {
            // Given
            CreateScheduleCommand command = new CreateScheduleCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setTitle("开庭");
            command.setDescription("案件开庭");
            command.setLocation("法院A");
            command.setScheduleType("COURT");
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
            command.setAllDay(false);
            command.setReminderMinutes(30);

            when(scheduleRepository.save(any(Schedule.class))).thenReturn(true);

            // When
            ScheduleDTO result = scheduleAppService.createSchedule(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("开庭");
            assertThat(result.getScheduleType()).isEqualTo("COURT");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(scheduleRepository).save(any(Schedule.class));
        }

        @Test
        @DisplayName("结束时间早于开始时间应该失败")
        void createSchedule_shouldFail_whenEndTimeBeforeStartTime() {
            // Given
            CreateScheduleCommand command = new CreateScheduleCommand();
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now()); // 早于开始时间

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> scheduleAppService.createSchedule(command));
            assertThat(exception.getMessage()).contains("结束时间不能早于开始时间");
        }

        @Test
        @DisplayName("应该设置默认全天标志")
        void createSchedule_shouldSetDefaultAllDay() {
            // Given
            CreateScheduleCommand command = new CreateScheduleCommand();
            command.setTitle("会议");
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
            command.setAllDay(null); // 不设置

            when(scheduleRepository.save(any(Schedule.class))).thenReturn(true);

            // When
            ScheduleDTO result = scheduleAppService.createSchedule(command);

            // Then
            assertThat(result).isNotNull();
            verify(scheduleRepository).save(argThat(schedule -> 
                    schedule.getAllDay() != null && !schedule.getAllDay()));
        }
    }

    @Nested
    @DisplayName("查询日程测试")
    class QueryScheduleTests {

        @Test
        @DisplayName("应该成功查询日程列表")
        void listSchedules_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("开庭")
                    .userId(TEST_USER_ID)
                    .matterId(TEST_MATTER_ID)
                    .build();

            Page<Schedule> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(schedule));
            page.setTotal(1L);

            when(scheduleMapper.selectSchedulePage(any(Page.class), eq(TEST_USER_ID), any(), any(), any(), any()))
                    .thenReturn(page);

            // When
            List<ScheduleDTO> result = scheduleAppService.listSchedules(
                    TEST_USER_ID, TEST_MATTER_ID, "COURT", null, null, 1, 10);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("开庭");
        }

        @Test
        @DisplayName("应该忽略传入的userId参数，使用当前用户")
        void listSchedules_shouldIgnoreUserIdParameter() {
            // Given
            Page<Schedule> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0L);

            when(scheduleMapper.selectSchedulePage(any(Page.class), eq(TEST_USER_ID), any(), any(), any(), any()))
                    .thenReturn(page);

            // When - 传入不同的userId，但应该使用当前用户ID
            scheduleAppService.listSchedules(OTHER_USER_ID, null, null, null, null, 1, 10);

            // Then - 验证使用的是当前用户ID
            verify(scheduleMapper).selectSchedulePage(any(Page.class), eq(TEST_USER_ID), any(), any(), any(), any());
        }

        @Test
        @DisplayName("应该成功获取日程详情")
        void getScheduleById_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("开庭")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);

            // When
            ScheduleDTO result = scheduleAppService.getScheduleById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("开庭");
        }

        @Test
        @DisplayName("不能查看他人的日程")
        void getScheduleById_shouldFail_whenNotOwner() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .userId(OTHER_USER_ID) // 其他用户的日程
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> scheduleAppService.getScheduleById(1L));
            assertThat(exception.getMessage()).contains("无权操作");
        }

        @Test
        @DisplayName("应该成功获取某天的日程")
        void getSchedulesByDate_shouldSuccess() {
            // Given
            LocalDate date = LocalDate.now();
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("会议")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleRepository.findByUserAndDate(TEST_USER_ID, date))
                    .thenReturn(Collections.singletonList(schedule));

            // When
            List<ScheduleDTO> result = scheduleAppService.getSchedulesByDate(TEST_USER_ID, date);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("会议");
        }

        @Test
        @DisplayName("应该成功获取今天的日程")
        void getMyTodaySchedules_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("今日会议")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleRepository.findByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(Collections.singletonList(schedule));

            // When
            List<ScheduleDTO> result = scheduleAppService.getMyTodaySchedules();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("今日会议");
        }

        @Test
        @DisplayName("应该成功获取近期日程")
        void getMyUpcomingSchedules_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("未来会议")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleMapper.selectUpcomingSchedules(eq(TEST_USER_ID), any(LocalDateTime.class), eq(10)))
                    .thenReturn(Collections.singletonList(schedule));

            // When
            List<ScheduleDTO> result = scheduleAppService.getMyUpcomingSchedules(7, 10);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("未来会议");
        }
    }

    @Nested
    @DisplayName("更新日程测试")
    class UpdateScheduleTests {

        @Test
        @DisplayName("应该成功更新日程")
        void updateSchedule_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("原标题")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);
            when(scheduleRepository.updateById(any(Schedule.class))).thenReturn(true);

            // When
            ScheduleDTO result = scheduleAppService.updateSchedule(
                    1L, "新标题", "新描述", "新地点", null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(schedule.getTitle()).isEqualTo("新标题");
            assertThat(schedule.getDescription()).isEqualTo("新描述");
            assertThat(schedule.getLocation()).isEqualTo("新地点");
            verify(scheduleRepository).updateById(schedule);
        }

        @Test
        @DisplayName("不能更新他人的日程")
        void updateSchedule_shouldFail_whenNotOwner() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .userId(OTHER_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> scheduleAppService.updateSchedule(1L, "新标题", null, null, null, null, null));
            assertThat(exception.getMessage()).contains("无权操作");
        }

        @Test
        @DisplayName("应该只更新非空字段")
        void updateSchedule_shouldOnlyUpdateNonNullFields() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("原标题")
                    .description("原描述")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);
            when(scheduleRepository.updateById(any(Schedule.class))).thenReturn(true);

            // When - 只更新标题
            scheduleAppService.updateSchedule(1L, "新标题", null, null, null, null, null);

            // Then
            assertThat(schedule.getTitle()).isEqualTo("新标题");
            assertThat(schedule.getDescription()).isEqualTo("原描述"); // 未改变
        }
    }

    @Nested
    @DisplayName("删除日程测试")
    class DeleteScheduleTests {

        @Test
        @DisplayName("应该成功删除日程")
        void deleteSchedule_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("待删除日程")
                    .userId(TEST_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);
            when(scheduleRepository.removeById(eq(1L))).thenReturn(true);

            // When
            scheduleAppService.deleteSchedule(1L);

            // Then
            verify(scheduleRepository).removeById(1L);
        }

        @Test
        @DisplayName("不能删除他人的日程")
        void deleteSchedule_shouldFail_whenNotOwner() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .userId(OTHER_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> scheduleAppService.deleteSchedule(1L));
            assertThat(exception.getMessage()).contains("无权操作");
        }
    }

    @Nested
    @DisplayName("取消日程测试")
    class CancelScheduleTests {

        @Test
        @DisplayName("应该成功取消日程")
        void cancelSchedule_shouldSuccess() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .title("待取消日程")
                    .userId(TEST_USER_ID)
                    .status("ACTIVE")
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);
            when(scheduleRepository.updateById(any(Schedule.class))).thenReturn(true);

            // When
            scheduleAppService.cancelSchedule(1L);

            // Then
            assertThat(schedule.getStatus()).isEqualTo("CANCELLED");
            verify(scheduleRepository).updateById(schedule);
        }

        @Test
        @DisplayName("不能取消他人的日程")
        void cancelSchedule_shouldFail_whenNotOwner() {
            // Given
            Schedule schedule = Schedule.builder()
                    .id(1L)
                    .userId(OTHER_USER_ID)
                    .build();

            when(scheduleRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(schedule);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> scheduleAppService.cancelSchedule(1L));
            assertThat(exception.getMessage()).contains("无权操作");
        }
    }
}
