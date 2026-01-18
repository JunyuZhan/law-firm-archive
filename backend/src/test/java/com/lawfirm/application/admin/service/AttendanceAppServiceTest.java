package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CheckInCommand;
import com.lawfirm.application.admin.dto.AttendanceDTO;
import com.lawfirm.application.admin.dto.AttendanceQueryDTO;
import com.lawfirm.application.admin.dto.MonthlyAttendanceStatisticsDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Attendance;
import com.lawfirm.domain.admin.repository.AttendanceRepository;
import com.lawfirm.infrastructure.persistence.mapper.AttendanceMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AttendanceAppService 单元测试
 * 测试考勤管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceAppService 考勤服务测试")
class AttendanceAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_ATTENDANCE_ID = 100L;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceMapper attendanceMapper;

    @InjectMocks
    private AttendanceAppService attendanceAppService;

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
    @DisplayName("查询考勤记录测试")
    class QueryAttendanceTests {

        @Test
        @DisplayName("应该成功分页查询考勤记录")
        void listAttendance_shouldSuccess() {
            // Given
            Attendance attendance = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            Page<Attendance> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(attendance));
            page.setTotal(1L);

            when(attendanceMapper.selectAttendancePage(any(Page.class), any(), any(), any(), any()))
                    .thenReturn(page);

            AttendanceQueryDTO query = new AttendanceQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<AttendanceDTO> result = attendanceAppService.listAttendance(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("签到测试")
    class CheckInTests {

        @Test
        @DisplayName("应该成功正常签到")
        void checkIn_shouldSuccess_whenNormal() {
            // Given
            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            Attendance attendance = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(LocalDateTime.now().with(LocalTime.of(8, 50))) // 早于9点
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
                Attendance att = invocation.getArgument(0);
                att.setId(TEST_ATTENDANCE_ID);
                return true;
            });

            // When
            AttendanceDTO result = attendanceAppService.checkIn(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(Attendance.STATUS_NORMAL);
            verify(attendanceRepository).save(any(Attendance.class));
        }

        @Test
        @DisplayName("应该成功迟到签到")
        void checkIn_shouldSuccess_whenLate() {
            // Given
            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            LocalDateTime lateTime = LocalDate.now().atTime(9, 30);

            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
                Attendance att = invocation.getArgument(0);
                att.setId(TEST_ATTENDANCE_ID);
                att.setStatus(Attendance.STATUS_LATE);
                return true;
            });

            // 模拟当前时间晚于9点
            try (MockedStatic<LocalDateTime> mockedDateTime = mockStatic(LocalDateTime.class);
                 MockedStatic<LocalDate> mockedDate = mockStatic(LocalDate.class)) {
                mockedDateTime.when(LocalDateTime::now).thenReturn(lateTime);
                mockedDate.when(LocalDate::now).thenReturn(LocalDate.now());

                // When
                AttendanceDTO result = attendanceAppService.checkIn(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(Attendance.STATUS_LATE);
            }
        }

        @Test
        @DisplayName("重复签到应该抛出异常")
        void checkIn_shouldFail_whenDuplicate() {
            // Given
            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            Attendance existing = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(LocalDateTime.now().minusHours(1)) // 已有签到时间
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            when(attendanceRepository.save(any(Attendance.class)))
                    .thenThrow(new DuplicateKeyException("Duplicate key"));
            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(existing);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> attendanceAppService.checkIn(command));
            assertThat(exception.getMessage()).contains("今日已签到");
        }
    }

    @Nested
    @DisplayName("签退测试")
    class CheckOutTests {

        @Test
        @DisplayName("应该成功签退")
        void checkOut_shouldSuccess() {
            // Given
            Attendance attendance = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(LocalDateTime.now().with(LocalTime.of(9, 0)))
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(attendance);
            when(attendanceRepository.updateById(any(Attendance.class))).thenReturn(true);

            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            // When
            AttendanceDTO result = attendanceAppService.checkOut(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(attendance.getCheckOutTime()).isNotNull();
            assertThat(attendance.getWorkHours()).isNotNull();
            verify(attendanceRepository).updateById(attendance);
        }

        @Test
        @DisplayName("未签到不能签退")
        void checkOut_shouldFail_whenNotCheckedIn() {
            // Given
            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(null);

            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> attendanceAppService.checkOut(command));
            assertThat(exception.getMessage()).contains("请先签到");
        }

        @Test
        @DisplayName("已签退不能重复签退")
        void checkOut_shouldFail_whenAlreadyCheckedOut() {
            // Given
            Attendance attendance = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(LocalDateTime.now().with(LocalTime.of(9, 0)))
                    .checkOutTime(LocalDateTime.now().with(LocalTime.of(18, 0))) // 已签退
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(attendance);

            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> attendanceAppService.checkOut(command));
            assertThat(exception.getMessage()).contains("今日已签退");
        }

        @Test
        @DisplayName("应该计算加班时长")
        void checkOut_shouldCalculateOvertime() {
            // Given
            Attendance attendance = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(LocalDateTime.now().with(LocalTime.of(9, 0)))
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(attendance);
            when(attendanceRepository.updateById(any(Attendance.class))).thenReturn(true);

            CheckInCommand command = new CheckInCommand();
            command.setLocation("公司");
            command.setDevice("手机");

            // 模拟当前时间晚于18点
            try (MockedStatic<LocalDateTime> mockedDateTime = mockStatic(LocalDateTime.class)) {
                LocalDateTime overtimeTime = LocalDateTime.now().with(LocalTime.of(20, 0));
                mockedDateTime.when(LocalDateTime::now).thenReturn(overtimeTime);

                // When
                AttendanceDTO result = attendanceAppService.checkOut(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(attendance.getOvertimeHours()).isNotNull();
                assertThat(attendance.getOvertimeHours().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("获取今日考勤测试")
    class GetTodayAttendanceTests {

        @Test
        @DisplayName("应该成功获取今日考勤")
        void getTodayAttendance_shouldSuccess() {
            // Given
            Attendance attendance = Attendance.builder()
                    .id(TEST_ATTENDANCE_ID)
                    .userId(TEST_USER_ID)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(LocalDateTime.now())
                    .status(Attendance.STATUS_NORMAL)
                    .build();

            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(attendance);

            // When
            AttendanceDTO result = attendanceAppService.getTodayAttendance();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_ATTENDANCE_ID);
        }

        @Test
        @DisplayName("今日未签到应该返回null")
        void getTodayAttendance_shouldReturnNull_whenNotCheckedIn() {
            // Given
            when(attendanceMapper.selectByUserAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                    .thenReturn(null);

            // When
            AttendanceDTO result = attendanceAppService.getTodayAttendance();

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("月度统计测试")
    class MonthlyStatisticsTests {

        @Test
        @DisplayName("应该成功获取月度考勤统计")
        void getMonthlyStatistics_shouldSuccess() {
            // Given
            Integer year = 2024;
            Integer month = 1;

            // 模拟统计数据: [status, count]
            Object[] stat1 = new Object[]{Attendance.STATUS_NORMAL, 20L};
            Object[] stat2 = new Object[]{Attendance.STATUS_LATE, 3L};
            Object[] stat3 = new Object[]{Attendance.STATUS_EARLY, 1L};
            Object[] stat4 = new Object[]{Attendance.STATUS_ABSENT, 2L};

            when(attendanceMapper.countMonthlyAttendance(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(stat1, stat2, stat3, stat4));

            // When
            MonthlyAttendanceStatisticsDTO result = attendanceAppService.getMonthlyStatistics(TEST_USER_ID, year, month);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getYear()).isEqualTo(year);
            assertThat(result.getMonth()).isEqualTo(month);
            assertThat(result.getNormalDays()).isEqualTo(20);
            assertThat(result.getLateDays()).isEqualTo(3);
            assertThat(result.getEarlyDays()).isEqualTo(1);
            assertThat(result.getAbsentDays()).isEqualTo(2);
        }

        @Test
        @DisplayName("应该使用当前用户作为默认值")
        void getMonthlyStatistics_shouldUseCurrentUser() {
            // Given
            Integer year = 2024;
            Integer month = 1;

            when(attendanceMapper.countMonthlyAttendance(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // When
            MonthlyAttendanceStatisticsDTO result = attendanceAppService.getMonthlyStatistics(null, year, month);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        }
    }
}
