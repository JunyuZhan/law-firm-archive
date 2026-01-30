package com.lawfirm.application.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.admin.command.GoOutCommand;
import com.lawfirm.application.admin.dto.GoOutRecordDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.GoOutRecord;
import com.lawfirm.domain.admin.repository.GoOutRecordRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.GoOutRecordMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/** GoOutAppService 单元测试 测试外出管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoOutAppService 外出服务测试")
class GoOutAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_RECORD_ID = 100L;
  private static final Long OTHER_USER_ID = 999L;

  @Mock private GoOutRecordRepository goOutRepository;

  @Mock private GoOutRecordMapper goOutMapper;

  @Mock private UserRepository userRepository;

  @InjectMocks private GoOutAppService goOutAppService;

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
  @DisplayName("外出登记测试")
  class RegisterGoOutTests {

    @Test
    @DisplayName("应该成功登记外出")
    void registerGoOut_shouldSuccess() {
      // Given
      GoOutCommand command = new GoOutCommand();
      command.setOutTime(LocalDateTime.now());
      command.setExpectedReturnTime(LocalDateTime.now().plusHours(2));
      command.setLocation("客户公司");
      command.setReason("拜访客户");
      command.setCompanions("张三");

      when(goOutRepository.save(any(GoOutRecord.class))).thenReturn(true);

      // When
      GoOutRecordDTO result = goOutAppService.registerGoOut(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getLocation()).isEqualTo("客户公司");
      assertThat(result.getStatus()).isEqualTo(GoOutRecord.STATUS_OUT);
      verify(goOutRepository).save(any(GoOutRecord.class));
    }

    @Test
    @DisplayName("外出时间晚于预计返回时间应该失败")
    void registerGoOut_shouldFail_whenOutTimeAfterReturnTime() {
      // Given
      GoOutCommand command = new GoOutCommand();
      command.setOutTime(LocalDateTime.now().plusHours(2));
      command.setExpectedReturnTime(LocalDateTime.now()); // 早于外出时间

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> goOutAppService.registerGoOut(command));
      assertThat(exception.getMessage()).contains("外出时间不能晚于预计返回时间");
    }

    @Test
    @DisplayName("已有未返回记录时应该失败")
    void registerGoOut_shouldFail_whenDuplicateKey() {
      // Given
      GoOutCommand command = new GoOutCommand();
      command.setOutTime(LocalDateTime.now());
      command.setExpectedReturnTime(LocalDateTime.now().plusHours(2));

      when(goOutRepository.save(any(GoOutRecord.class)))
          .thenThrow(new DuplicateKeyException("Duplicate key"));

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> goOutAppService.registerGoOut(command));
      assertThat(exception.getMessage()).contains("未返回的外出记录");
    }
  }

  @Nested
  @DisplayName("登记返回测试")
  class RegisterReturnTests {

    @Test
    @DisplayName("应该成功登记返回")
    void registerReturn_shouldSuccess() {
      // Given
      GoOutRecord record =
          GoOutRecord.builder()
              .id(TEST_RECORD_ID)
              .recordNo("GO240118ABCD")
              .userId(TEST_USER_ID)
              .status(GoOutRecord.STATUS_OUT)
              .build();

      when(goOutRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);
      when(goOutRepository.updateById(any(GoOutRecord.class))).thenReturn(true);

      // When
      GoOutRecordDTO result = goOutAppService.registerReturn(TEST_RECORD_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(record.getStatus()).isEqualTo(GoOutRecord.STATUS_RETURNED);
      assertThat(record.getActualReturnTime()).isNotNull();
      verify(goOutRepository).updateById(record);
    }

    @Test
    @DisplayName("非外出中状态的记录不能登记返回")
    void registerReturn_shouldFail_whenNotOut() {
      // Given
      GoOutRecord record =
          GoOutRecord.builder()
              .id(TEST_RECORD_ID)
              .status(GoOutRecord.STATUS_RETURNED) // 已返回
              .build();

      when(goOutRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> goOutAppService.registerReturn(TEST_RECORD_ID));
      assertThat(exception.getMessage()).contains("不是外出中状态");
    }

    @Test
    @DisplayName("普通用户不能登记他人的返回")
    void registerReturn_shouldFail_whenNotOwner() {
      // Given
      GoOutRecord record =
          GoOutRecord.builder()
              .id(TEST_RECORD_ID)
              .userId(OTHER_USER_ID) // 其他用户
              .status(GoOutRecord.STATUS_OUT)
              .build();

      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString()))
          .thenReturn(false);

      when(goOutRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> goOutAppService.registerReturn(TEST_RECORD_ID));
      assertThat(exception.getMessage()).contains("权限不足");
    }

    @Test
    @DisplayName("管理员可以登记他人的返回")
    void registerReturn_shouldSuccess_whenAdmin() {
      // Given
      GoOutRecord record =
          GoOutRecord.builder()
              .id(TEST_RECORD_ID)
              .userId(OTHER_USER_ID)
              .status(GoOutRecord.STATUS_OUT)
              .build();

      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER"))
          .thenReturn(true);

      when(goOutRepository.getByIdOrThrow(eq(TEST_RECORD_ID), anyString())).thenReturn(record);
      when(goOutRepository.updateById(any(GoOutRecord.class))).thenReturn(true);

      // When
      GoOutRecordDTO result = goOutAppService.registerReturn(TEST_RECORD_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(record.getStatus()).isEqualTo(GoOutRecord.STATUS_RETURNED);
    }
  }

  @Nested
  @DisplayName("查询外出记录测试")
  class QueryRecordsTests {

    @Test
    @DisplayName("应该成功查询我的外出记录")
    void getMyRecords_shouldSuccess() {
      // Given
      GoOutRecord record =
          GoOutRecord.builder().id(TEST_RECORD_ID).userId(TEST_USER_ID).location("客户公司").build();

      User user = new User();
      user.setId(TEST_USER_ID);
      user.setRealName("测试用户");

      when(goOutMapper.selectByUserId(TEST_USER_ID)).thenReturn(Collections.singletonList(record));
      when(userRepository.listByIds(anyList())).thenReturn(Collections.singletonList(user));

      // When
      List<GoOutRecordDTO> result = goOutAppService.getMyRecords();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getLocation()).isEqualTo("客户公司");
    }

    @Test
    @DisplayName("应该成功查询指定日期范围的外出记录")
    void getRecordsByDateRange_shouldSuccess() {
      // Given
      LocalDate startDate = LocalDate.now().minusDays(7);
      LocalDate endDate = LocalDate.now();

      GoOutRecord record = GoOutRecord.builder().id(TEST_RECORD_ID).userId(TEST_USER_ID).build();

      when(goOutMapper.selectByDateRange(eq(TEST_USER_ID), eq(startDate), eq(endDate)))
          .thenReturn(Collections.singletonList(record));
      when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      // When
      List<GoOutRecordDTO> result = goOutAppService.getRecordsByDateRange(startDate, endDate);

      // Then
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("应该成功查询当前外出的记录")
    void getCurrentOut_shouldSuccess() {
      // Given
      GoOutRecord record =
          GoOutRecord.builder()
              .id(TEST_RECORD_ID)
              .userId(TEST_USER_ID)
              .status(GoOutRecord.STATUS_OUT)
              .build();

      when(goOutMapper.selectCurrentOut(TEST_USER_ID))
          .thenReturn(Collections.singletonList(record));
      when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      // When
      List<GoOutRecordDTO> result = goOutAppService.getCurrentOut();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo(GoOutRecord.STATUS_OUT);
    }

    @Test
    @DisplayName("空记录列表应该返回空列表")
    void getMyRecords_shouldReturnEmpty_whenNoRecords() {
      // Given
      when(goOutMapper.selectByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

      // When
      List<GoOutRecordDTO> result = goOutAppService.getMyRecords();

      // Then
      assertThat(result).isEmpty();
    }
  }
}
