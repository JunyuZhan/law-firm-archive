package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.BookMeetingCommand;
import com.lawfirm.application.admin.command.CreateMeetingRoomCommand;
import com.lawfirm.application.admin.dto.MeetingBookingDTO;
import com.lawfirm.application.admin.dto.MeetingBookingQueryDTO;
import com.lawfirm.application.admin.dto.MeetingRoomDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import com.lawfirm.domain.admin.repository.MeetingBookingRepository;
import com.lawfirm.domain.admin.repository.MeetingRoomRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingBookingMapper;
import com.lawfirm.infrastructure.persistence.mapper.MeetingRoomMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * MeetingRoomAppService 单元测试
 * 测试会议室管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingRoomAppService 会议室服务测试")
class MeetingRoomAppServiceTest {

    private static final Long TEST_ROOM_ID = 100L;
    private static final Long TEST_BOOKING_ID = 200L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private MeetingRoomRepository meetingRoomRepository;

    @Mock
    private MeetingRoomMapper meetingRoomMapper;

    @Mock
    private MeetingBookingRepository meetingBookingRepository;

    @Mock
    private MeetingBookingMapper meetingBookingMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MeetingRoomAppService meetingRoomAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        
        // 使用真实的ObjectMapper实例
        try {
            java.lang.reflect.Field field = MeetingRoomAppService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(meetingRoomAppService, new ObjectMapper());
        } catch (Exception e) {
            // Ignore
        }
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("查询会议室测试")
    class QueryRoomTests {

        @Test
        @DisplayName("应该成功获取所有会议室")
        void listRooms_shouldSuccess() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .code("ROOM-A")
                    .status(MeetingRoom.STATUS_AVAILABLE)
                    .enabled(true)
                    .build();

            when(meetingRoomMapper.selectEnabledRooms()).thenReturn(Collections.singletonList(room));

            // When
            List<MeetingRoomDTO> result = meetingRoomAppService.listRooms();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("会议室A");
        }

        @Test
        @DisplayName("应该成功获取可用会议室")
        void listAvailableRooms_shouldSuccess() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .code("ROOM-A")
                    .status(MeetingRoom.STATUS_AVAILABLE)
                    .enabled(true)
                    .build();

            when(meetingRoomMapper.selectAvailableRooms()).thenReturn(Collections.singletonList(room));

            // When
            List<MeetingRoomDTO> result = meetingRoomAppService.listAvailableRooms();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("会议室A");
        }
    }

    @Nested
    @DisplayName("创建会议室测试")
    class CreateRoomTests {

        @Test
        @DisplayName("应该成功创建会议室")
        void createRoom_shouldSuccess() {
            // Given
            CreateMeetingRoomCommand command = new CreateMeetingRoomCommand();
            command.setName("新会议室");
            command.setCode("ROOM-NEW");
            command.setLocation("3楼");
            command.setCapacity(20);
            command.setEquipment("投影仪,白板");
            command.setDescription("大型会议室");

            when(meetingRoomMapper.selectByCode("ROOM-NEW")).thenReturn(null);
            when(meetingRoomRepository.save(any(MeetingRoom.class))).thenAnswer(invocation -> {
                MeetingRoom room = invocation.getArgument(0);
                room.setId(TEST_ROOM_ID);
                return true;
            });

            // When
            MeetingRoomDTO result = meetingRoomAppService.createRoom(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新会议室");
            assertThat(result.getStatus()).isEqualTo(MeetingRoom.STATUS_AVAILABLE);
            assertThat(result.getEnabled()).isTrue();
            verify(meetingRoomRepository).save(any(MeetingRoom.class));
        }

        @Test
        @DisplayName("会议室编码已存在应该失败")
        void createRoom_shouldFail_whenCodeExists() {
            // Given
            CreateMeetingRoomCommand command = new CreateMeetingRoomCommand();
            command.setName("新会议室");
            command.setCode("ROOM-EXISTS");

            MeetingRoom existing = MeetingRoom.builder()
                    .id(999L)
                    .code("ROOM-EXISTS")
                    .build();

            when(meetingRoomMapper.selectByCode("ROOM-EXISTS")).thenReturn(existing);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRoomAppService.createRoom(command));
            assertThat(exception.getMessage()).contains("会议室编码已存在");
        }

        @Test
        @DisplayName("未提供编码应该自动生成")
        void createRoom_shouldAutoGenerateCode() {
            // Given
            CreateMeetingRoomCommand command = new CreateMeetingRoomCommand();
            command.setName("新会议室");
            // 不设置code

            when(meetingRoomMapper.selectByCode(anyString())).thenReturn(null);
            when(meetingRoomRepository.save(any(MeetingRoom.class))).thenAnswer(invocation -> {
                MeetingRoom room = invocation.getArgument(0);
                room.setId(TEST_ROOM_ID);
                return true;
            });

            // When
            MeetingRoomDTO result = meetingRoomAppService.createRoom(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isNotNull();
            assertThat(result.getCode()).startsWith("ROOM-");
        }
    }

    @Nested
    @DisplayName("更新会议室测试")
    class UpdateRoomTests {

        @Test
        @DisplayName("应该成功更新会议室")
        void updateRoom_shouldSuccess() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("原名称")
                    .code("ROOM-A")
                    .capacity(10)
                    .build();

            CreateMeetingRoomCommand command = new CreateMeetingRoomCommand();
            command.setName("新名称");
            command.setCapacity(20);

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingRoomRepository.updateById(any(MeetingRoom.class))).thenReturn(true);

            // When
            MeetingRoomDTO result = meetingRoomAppService.updateRoom(TEST_ROOM_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(room.getName()).isEqualTo("新名称");
            assertThat(room.getCapacity()).isEqualTo(20);
            verify(meetingRoomRepository).updateById(room);
        }

        @Test
        @DisplayName("更新时编码重复应该失败")
        void updateRoom_shouldFail_whenCodeExists() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .code("ROOM-A")
                    .build();

            MeetingRoom existing = MeetingRoom.builder()
                    .id(999L)
                    .code("ROOM-B")
                    .build();

            CreateMeetingRoomCommand command = new CreateMeetingRoomCommand();
            command.setCode("ROOM-B");

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingRoomMapper.selectByCode("ROOM-B")).thenReturn(existing);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRoomAppService.updateRoom(TEST_ROOM_ID, command));
            assertThat(exception.getMessage()).contains("会议室编码已存在");
        }
    }

    @Nested
    @DisplayName("删除会议室测试")
    class DeleteRoomTests {

        @Test
        @DisplayName("应该成功删除没有预约的会议室")
        void deleteRoom_shouldSuccess() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .enabled(true)
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingBookingMapper.countConflicting(eq(TEST_ROOM_ID), any(), any(), any())).thenReturn(0);
            when(meetingRoomRepository.updateById(any(MeetingRoom.class))).thenReturn(true);

            // When
            meetingRoomAppService.deleteRoom(TEST_ROOM_ID);

            // Then
            assertThat(room.getEnabled()).isFalse();
            assertThat(room.getStatus()).isEqualTo(MeetingRoom.STATUS_MAINTENANCE);
            verify(meetingRoomRepository).updateById(room);
        }

        @Test
        @DisplayName("有有效预约的会议室不能删除")
        void deleteRoom_shouldFail_whenHasBookings() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingBookingMapper.countConflicting(eq(TEST_ROOM_ID), any(), any(), any())).thenReturn(5);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRoomAppService.deleteRoom(TEST_ROOM_ID));
            assertThat(exception.getMessage()).contains("有效预约");
        }
    }

    @Nested
    @DisplayName("更新会议室状态测试")
    class UpdateRoomStatusTests {

        @Test
        @DisplayName("应该成功更新会议室状态")
        void updateRoomStatus_shouldSuccess() {
            // Given
            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .status(MeetingRoom.STATUS_AVAILABLE)
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingRoomRepository.updateById(any(MeetingRoom.class))).thenReturn(true);

            // When
            meetingRoomAppService.updateRoomStatus(TEST_ROOM_ID, MeetingRoom.STATUS_MAINTENANCE);

            // Then
            assertThat(room.getStatus()).isEqualTo(MeetingRoom.STATUS_MAINTENANCE);
            verify(meetingRoomRepository).updateById(room);
        }
    }

    @Nested
    @DisplayName("会议预约测试")
    class BookingTests {

        @Test
        @DisplayName("应该成功预约会议")
        void bookMeeting_shouldSuccess() {
            // Given
            BookMeetingCommand command = new BookMeetingCommand();
            command.setRoomId(TEST_ROOM_ID);
            command.setTitle("项目会议");
            command.setStartTime(LocalDateTime.now().plusDays(1).withHour(14));
            command.setEndTime(LocalDateTime.now().plusDays(1).withHour(16));
            command.setAttendeeIds(Collections.singletonList(TEST_USER_ID));

            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .status(MeetingRoom.STATUS_AVAILABLE)
                    .enabled(true)
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingBookingMapper.countConflicting(eq(TEST_ROOM_ID), any(), any(), any())).thenReturn(0);
            when(meetingBookingRepository.save(any(MeetingBooking.class))).thenAnswer(invocation -> {
                MeetingBooking booking = invocation.getArgument(0);
                booking.setId(TEST_BOOKING_ID);
                booking.setBookingNo("MT2024001");
                return true;
            });

            // When
            MeetingBookingDTO result = meetingRoomAppService.bookMeeting(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("项目会议");
            verify(meetingBookingRepository).save(any(MeetingBooking.class));
        }

        @Test
        @DisplayName("时间冲突不能预约")
        void bookMeeting_shouldFail_whenTimeConflict() {
            // Given
            BookMeetingCommand command = new BookMeetingCommand();
            command.setRoomId(TEST_ROOM_ID);
            command.setStartTime(LocalDateTime.now().plusDays(1).withHour(14));
            command.setEndTime(LocalDateTime.now().plusDays(1).withHour(16));

            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .status(MeetingRoom.STATUS_AVAILABLE)
                    .enabled(true)
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(meetingRoomMapper.selectById(TEST_ROOM_ID)).thenReturn(room);
            when(meetingBookingMapper.countConflicting(eq(TEST_ROOM_ID), any(), any(), any())).thenReturn(1); // 有冲突

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRoomAppService.bookMeeting(command));
            assertThat(exception.getMessage()).contains("已被预约");
        }

        @Test
        @DisplayName("已禁用的会议室不能预约")
        void bookMeeting_shouldFail_whenRoomDisabled() {
            // Given
            BookMeetingCommand command = new BookMeetingCommand();
            command.setRoomId(TEST_ROOM_ID);

            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .enabled(false) // 已禁用
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRoomAppService.bookMeeting(command));
            assertThat(exception.getMessage()).contains("会议室已禁用");
        }

        @Test
        @DisplayName("结束时间早于开始时间应该失败")
        void bookMeeting_shouldFail_whenEndBeforeStart() {
            // Given
            BookMeetingCommand command = new BookMeetingCommand();
            command.setRoomId(TEST_ROOM_ID);
            command.setStartTime(LocalDateTime.now().plusDays(1).withHour(16));
            command.setEndTime(LocalDateTime.now().plusDays(1).withHour(14)); // 早于开始时间

            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .enabled(true)
                    .build();

            when(meetingRoomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRoomAppService.bookMeeting(command));
            assertThat(exception.getMessage()).contains("开始时间");
        }
    }

    @Nested
    @DisplayName("查询预约测试")
    class QueryBookingTests {

        @Test
        @DisplayName("应该成功分页查询会议预约")
        void listBookings_shouldSuccess() {
            // Given
            MeetingBooking booking = MeetingBooking.builder()
                    .id(TEST_BOOKING_ID)
                    .bookingNo("MT2024001")
                    .roomId(TEST_ROOM_ID)
                    .title("项目会议")
                    .build();

            Page<MeetingBooking> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(booking));
            page.setTotal(1L);

            when(meetingBookingMapper.selectBookingPage(any(Page.class), any(), any(), any(), any(), any()))
                    .thenReturn(page);
            when(meetingRoomRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

            MeetingBookingQueryDTO query = new MeetingBookingQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<MeetingBookingDTO> result = meetingRoomAppService.listBookings(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }
    }
}
