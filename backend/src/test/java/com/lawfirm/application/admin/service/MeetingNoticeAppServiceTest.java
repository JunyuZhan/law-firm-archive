package com.lawfirm.application.admin.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import com.lawfirm.domain.admin.repository.MeetingBookingRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingBookingMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MeetingNoticeAppService 单元测试
 * 测试会议通知服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingNoticeAppService 会议通知服务测试")
class MeetingNoticeAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_BOOKING_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;

    @Mock
    private MeetingBookingRepository bookingRepository;

    @Mock
    private MeetingBookingMapper bookingMapper;

    @InjectMocks
    private MeetingNoticeAppService meetingNoticeAppService;

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
    @DisplayName("发送会议通知测试")
    class SendNoticeTests {

        @Test
        @DisplayName("应该成功发送会议通知")
        void sendMeetingNotice_shouldSuccess() {
            // Given
            MeetingBooking booking = MeetingBooking.builder()
                    .id(TEST_BOOKING_ID)
                    .bookingNo("MT2024001")
                    .title("项目会议")
                    .organizerId(TEST_USER_ID)
                    .reminderSent(false)
                    .build();

            when(bookingRepository.getByIdOrThrow(eq(TEST_BOOKING_ID), anyString())).thenReturn(booking);
            when(bookingRepository.updateById(any(MeetingBooking.class))).thenReturn(true);

            // When
            meetingNoticeAppService.sendMeetingNotice(TEST_BOOKING_ID);

            // Then
            assertThat(booking.getReminderSent()).isTrue();
            assertThat(booking.getUpdatedBy()).isEqualTo(TEST_USER_ID);
            assertThat(booking.getUpdatedAt()).isNotNull();
            verify(bookingRepository).updateById(booking);
        }

        @Test
        @DisplayName("已发送的通知应该直接返回")
        void sendMeetingNotice_shouldReturn_whenAlreadySent() {
            // Given
            MeetingBooking booking = MeetingBooking.builder()
                    .id(TEST_BOOKING_ID)
                    .organizerId(TEST_USER_ID)
                    .reminderSent(true) // 已发送
                    .build();

            when(bookingRepository.getByIdOrThrow(eq(TEST_BOOKING_ID), anyString())).thenReturn(booking);

            // When
            meetingNoticeAppService.sendMeetingNotice(TEST_BOOKING_ID);

            // Then - 不应该更新
            verify(bookingRepository, never()).updateById(any(MeetingBooking.class));
        }

        @Test
        @DisplayName("非组织者且非管理员不能发送通知")
        void sendMeetingNotice_shouldFail_whenNoPermission() {
            // Given
            MeetingBooking booking = MeetingBooking.builder()
                    .id(TEST_BOOKING_ID)
                    .organizerId(OTHER_USER_ID) // 其他用户组织
                    .reminderSent(false)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasRole("MEETING_MANAGER")).thenReturn(false);

            when(bookingRepository.getByIdOrThrow(eq(TEST_BOOKING_ID), anyString())).thenReturn(booking);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingNoticeAppService.sendMeetingNotice(TEST_BOOKING_ID));
            assertThat(exception.getMessage()).contains("权限不足");
        }

        @Test
        @DisplayName("管理员可以发送任何会议的通知")
        void sendMeetingNotice_shouldSuccess_whenAdmin() {
            // Given
            MeetingBooking booking = MeetingBooking.builder()
                    .id(TEST_BOOKING_ID)
                    .organizerId(OTHER_USER_ID)
                    .reminderSent(false)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

            when(bookingRepository.getByIdOrThrow(eq(TEST_BOOKING_ID), anyString())).thenReturn(booking);
            when(bookingRepository.updateById(any(MeetingBooking.class))).thenReturn(true);

            // When
            meetingNoticeAppService.sendMeetingNotice(TEST_BOOKING_ID);

            // Then
            assertThat(booking.getReminderSent()).isTrue();
            verify(bookingRepository).updateById(booking);
        }
    }

    @Nested
    @DisplayName("批量发送通知测试")
    class BatchSendNoticeTests {

        @Test
        @DisplayName("应该成功批量发送即将开始的会议通知")
        void sendUpcomingMeetingNotices_shouldSuccess() {
            // Given
            MeetingBooking booking1 = MeetingBooking.builder()
                    .id(1L)
                    .bookingNo("MT001")
                    .title("会议1")
                    .reminderSent(false)
                    .build();
            MeetingBooking booking2 = MeetingBooking.builder()
                    .id(2L)
                    .bookingNo("MT002")
                    .title("会议2")
                    .reminderSent(false)
                    .build();

            when(bookingMapper.selectUpcomingMeetings(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(booking1, booking2));
            lenient().when(bookingRepository.updateBatchById(anyList())).thenReturn(true);

            // When
            int sentCount = meetingNoticeAppService.sendUpcomingMeetingNotices(30);

            // Then
            assertThat(sentCount).isEqualTo(2);
            assertThat(booking1.getReminderSent()).isTrue();
            assertThat(booking2.getReminderSent()).isTrue();
            verify(bookingRepository).updateBatchById(anyList());
        }

        @Test
        @DisplayName("没有即将开始的会议应该返回0")
        void sendUpcomingMeetingNotices_shouldReturnZero_whenNoMeetings() {
            // Given
            when(bookingMapper.selectUpcomingMeetings(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            int sentCount = meetingNoticeAppService.sendUpcomingMeetingNotices(30);

            // Then
            assertThat(sentCount).isEqualTo(0);
            verify(bookingRepository, never()).updateBatchById(anyList());
        }

        @Test
        @DisplayName("已发送通知的会议应该跳过")
        void sendUpcomingMeetingNotices_shouldSkip_whenAlreadySent() {
            // Given
            MeetingBooking booking1 = MeetingBooking.builder()
                    .id(1L)
                    .reminderSent(false) // 未发送
                    .build();
            MeetingBooking booking2 = MeetingBooking.builder()
                    .id(2L)
                    .reminderSent(true) // 已发送
                    .build();

            when(bookingMapper.selectUpcomingMeetings(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(booking1, booking2));
            lenient().when(bookingRepository.updateBatchById(anyList())).thenReturn(true);

            // When
            int sentCount = meetingNoticeAppService.sendUpcomingMeetingNotices(30);

            // Then - 只发送booking1
            assertThat(sentCount).isEqualTo(1);
            assertThat(booking1.getReminderSent()).isTrue();
            assertThat(booking2.getReminderSent()).isTrue(); // 保持原状态
        }
    }
}
