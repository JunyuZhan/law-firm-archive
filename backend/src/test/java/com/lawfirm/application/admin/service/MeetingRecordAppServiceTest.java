package com.lawfirm.application.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.admin.command.CreateMeetingRecordCommand;
import com.lawfirm.application.admin.dto.MeetingRecordDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.MeetingRecord;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import com.lawfirm.domain.admin.repository.MeetingRecordRepository;
import com.lawfirm.domain.admin.repository.MeetingRoomRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.MeetingRecordMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MeetingRecordAppService 单元测试
 * 测试会议记录服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MeetingRecordAppService 会议记录服务测试")
class MeetingRecordAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_RECORD_ID = 100L;
    private static final Long TEST_ROOM_ID = 200L;
    private static final Long TEST_BOOKING_ID = 300L;
    private static final Long OTHER_USER_ID = 999L;

    @Mock
    private MeetingRecordRepository recordRepository;

    @Mock
    private MeetingRecordMapper recordMapper;

    @Mock
    private MeetingRoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MeetingRecordAppService meetingRecordAppService;

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
    @DisplayName("创建会议记录测试")
    class CreateRecordTests {

        @Test
        @DisplayName("应该成功创建会议记录")
        void createRecord_shouldSuccess() throws JsonProcessingException {
            // Given
            CreateMeetingRecordCommand command = new CreateMeetingRecordCommand();
            command.setRoomId(TEST_ROOM_ID);
            command.setTitle("项目会议");
            command.setMeetingDate(java.time.LocalDate.now());
            command.setStartTime(java.time.LocalTime.now());
            command.setEndTime(java.time.LocalTime.now().plusHours(2));
            command.setAttendeeIds(Arrays.asList(1L, 2L));
            command.setContent("会议内容");
            command.setDecisions("会议决定");
            command.setActionItems("行动项");

            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .build();

            when(roomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(objectMapper.writeValueAsString(anyList())).thenReturn("[1,2]");
            when(recordRepository.save(any(MeetingRecord.class))).thenReturn(true);

            // When
            MeetingRecordDTO result = meetingRecordAppService.createRecord(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("项目会议");
            assertThat(result.getOrganizerId()).isEqualTo(TEST_USER_ID);
            verify(recordRepository).save(any(MeetingRecord.class));
        }

        @Test
        @DisplayName("会议室不存在应该失败")
        void createRecord_shouldFail_whenRoomNotFound() {
            // Given
            CreateMeetingRecordCommand command = new CreateMeetingRecordCommand();
            command.setRoomId(TEST_ROOM_ID);

            when(roomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString()))
                    .thenThrow(new BusinessException("会议室不存在"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRecordAppService.createRecord(command));
            assertThat(exception.getMessage()).contains("会议室不存在");
        }

        @Test
        @DisplayName("参会人员序列化失败应该抛出异常")
        void createRecord_shouldFail_whenSerializeFails() throws JsonProcessingException {
            // Given
            CreateMeetingRecordCommand command = new CreateMeetingRecordCommand();
            command.setRoomId(TEST_ROOM_ID);
            command.setAttendeeIds(Arrays.asList(1L, 2L));

            MeetingRoom room = new MeetingRoom();
            when(roomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(objectMapper.writeValueAsString(anyList()))
                    .thenThrow(new JsonProcessingException("Serialization error") {
                    });

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRecordAppService.createRecord(command));
            assertThat(exception.getMessage()).contains("数据格式错误");
        }
    }

    @Nested
    @DisplayName("根据预约创建记录测试")
    class CreateFromBookingTests {

        @Test
        @DisplayName("应该成功根据预约创建会议记录")
        void createRecordFromBooking_shouldSuccess() throws JsonProcessingException {
            // Given
            CreateMeetingRecordCommand command = new CreateMeetingRecordCommand();
            command.setRoomId(TEST_ROOM_ID);
            command.setTitle("项目会议");

            MeetingRoom room = new MeetingRoom();
            when(roomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(objectMapper.writeValueAsString(anyList())).thenReturn("[]");
            when(recordRepository.save(any(MeetingRecord.class))).thenReturn(true);

            // When
            MeetingRecordDTO result = meetingRecordAppService.createRecordFromBooking(TEST_BOOKING_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(command.getBookingId()).isEqualTo(TEST_BOOKING_ID);
        }

        @Test
        @DisplayName("预约已有记录时应该失败")
        void createRecordFromBooking_shouldFail_whenDuplicate() throws JsonProcessingException {
            // Given
            CreateMeetingRecordCommand command = new CreateMeetingRecordCommand();
            command.setRoomId(TEST_ROOM_ID);

            MeetingRoom room = new MeetingRoom();
            when(roomRepository.getByIdOrThrow(eq(TEST_ROOM_ID), anyString())).thenReturn(room);
            when(objectMapper.writeValueAsString(anyList())).thenReturn("[]");
            when(recordRepository.save(any(MeetingRecord.class)))
                    .thenThrow(new DuplicateKeyException("Duplicate key"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRecordAppService.createRecordFromBooking(TEST_BOOKING_ID, command));
            assertThat(exception.getMessage()).contains("已有会议记录");
        }
    }

    @Nested
    @DisplayName("查询会议记录测试")
    class QueryRecordTests {

        @Test
        @DisplayName("应该成功获取会议记录详情")
        void getRecordById_shouldSuccess() {
            // Given
            MeetingRecord record = MeetingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .recordNo("MR2024001")
                    .organizerId(TEST_USER_ID)
                    .title("项目会议")
                    .build();

            MeetingRoom room = MeetingRoom.builder()
                    .id(TEST_ROOM_ID)
                    .name("会议室A")
                    .build();

            User user = new User();
            user.setId(TEST_USER_ID);
            user.setRealName("组织者");

            when(recordRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);
            when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(room);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(user);

            // When
            MeetingRecordDTO result = meetingRecordAppService.getRecordById(TEST_RECORD_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("项目会议");
        }

        @Test
        @DisplayName("非组织者且非参会人员无权查看")
        void getRecordById_shouldFail_whenNoPermission() throws JsonProcessingException {
            // Given
            MeetingRecord record = MeetingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .organizerId(OTHER_USER_ID)
                    .attendees("[999]") // 其他参会人员
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasRole("HR_MANAGER")).thenReturn(false);

            when(recordRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(Collections.singletonList(OTHER_USER_ID));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRecordAppService.getRecordById(TEST_RECORD_ID));
            assertThat(exception.getMessage()).contains("权限不足");
        }

        @Test
        @DisplayName("参会人员可以查看记录")
        void getRecordById_shouldSuccess_whenAttendee() throws JsonProcessingException {
            // Given
            MeetingRecord record = MeetingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .organizerId(OTHER_USER_ID)
                    .attendees("[1,999]") // 当前用户是参会人员
                    .build();

            when(recordRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenAnswer(invocation -> Arrays.asList(TEST_USER_ID, OTHER_USER_ID));
            when(roomRepository.findById(any())).thenReturn(new MeetingRoom());
            when(userRepository.findById(any())).thenReturn(new User());

            // When
            MeetingRecordDTO result = meetingRecordAppService.getRecordById(TEST_RECORD_ID);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("管理员可以查看所有记录")
        void getRecordById_shouldSuccess_whenAdmin() {
            // Given
            MeetingRecord record = MeetingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .organizerId(OTHER_USER_ID)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

            when(recordRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);
            when(roomRepository.findById(any())).thenReturn(new MeetingRoom());
            when(userRepository.findById(any())).thenReturn(new User());

            // When
            MeetingRecordDTO result = meetingRecordAppService.getRecordById(TEST_RECORD_ID);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("按会议室查询记录测试")
    class QueryByRoomTests {

        @Test
        @DisplayName("管理员应该成功查询会议室的记录")
        void getRecordsByRoom_shouldSuccess_whenAdmin() {
            // Given
            MeetingRecord record = MeetingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .roomId(TEST_ROOM_ID)
                    .title("会议1")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

            when(recordMapper.selectByRoomId(TEST_ROOM_ID))
                    .thenReturn(Collections.singletonList(record));
            when(roomRepository.listByIds(anyList())).thenReturn(Collections.emptyList());
            when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

            // When
            List<MeetingRecordDTO> result = meetingRecordAppService.getRecordsByRoom(TEST_ROOM_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("会议1");
        }

        @Test
        @DisplayName("普通用户无权按会议室查询")
        void getRecordsByRoom_shouldFail_whenNotAdmin() {
            // Given
            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasRole("HR_MANAGER")).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRecordAppService.getRecordsByRoom(TEST_ROOM_ID));
            assertThat(exception.getMessage()).contains("权限不足");
        }
    }

    @Nested
    @DisplayName("按日期范围查询记录测试")
    class QueryByDateRangeTests {

        @Test
        @DisplayName("管理员应该成功查询日期范围的记录")
        void getRecordsByDateRange_shouldSuccess_whenAdmin() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            MeetingRecord record = MeetingRecord.builder()
                    .id(TEST_RECORD_ID)
                    .meetingDate(LocalDate.now())
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

            when(recordMapper.selectByDateRange(startDate, endDate))
                    .thenReturn(Collections.singletonList(record));
            when(roomRepository.listByIds(anyList())).thenReturn(Collections.emptyList());
            when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

            // When
            List<MeetingRecordDTO> result = meetingRecordAppService.getRecordsByDateRange(startDate, endDate);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("普通用户无权按日期查询")
        void getRecordsByDateRange_shouldFail_whenNotAdmin() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasRole("HR_MANAGER")).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> meetingRecordAppService.getRecordsByDateRange(startDate, endDate));
            assertThat(exception.getMessage()).contains("权限不足");
        }
    }
}
