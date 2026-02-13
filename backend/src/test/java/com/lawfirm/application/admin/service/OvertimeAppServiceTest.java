package com.lawfirm.application.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.admin.command.ApplyOvertimeCommand;
import com.lawfirm.application.admin.dto.OvertimeApplicationDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.OvertimeApplication;
import com.lawfirm.domain.admin.repository.OvertimeApplicationRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.OvertimeApplicationMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** OvertimeAppService 单元测试 测试加班管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OvertimeAppService 加班服务测试")
class OvertimeAppServiceTest {

  private static final Long TEST_APPLICATION_ID = 100L;
  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_APPROVER_ID = 2L;

  @Mock private OvertimeApplicationRepository overtimeRepository;

  @Mock private OvertimeApplicationMapper overtimeMapper;

  @Mock private UserRepository userRepository;

  @InjectMocks private OvertimeAppService overtimeAppService;

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
  @DisplayName("申请加班测试")
  class ApplyOvertimeTests {

    @Test
    @DisplayName("应该成功申请加班")
    void applyOvertime_shouldSuccess() {
      // Given
      ApplyOvertimeCommand command = new ApplyOvertimeCommand();
      command.setOvertimeDate(LocalDate.now());
      command.setStartTime(LocalTime.of(18, 0));
      command.setEndTime(LocalTime.of(20, 0));
      command.setReason("项目紧急");
      command.setWorkContent("完成项目文档");

      when(overtimeRepository.save(any(OvertimeApplication.class)))
          .thenAnswer(
              invocation -> {
                OvertimeApplication app = invocation.getArgument(0);
                app.setId(TEST_APPLICATION_ID);
                app.setApplicationNo("OT2024001");
                return true;
              });

      // When
      OvertimeApplicationDTO result = overtimeAppService.applyOvertime(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(OvertimeApplication.STATUS_PENDING);
      assertThat(result.getOvertimeHours()).isEqualByComparingTo(BigDecimal.valueOf(2));
      verify(overtimeRepository).save(any(OvertimeApplication.class));
    }

    @Test
    @DisplayName("应该成功申请跨天加班")
    void applyOvertime_shouldSuccess_whenCrossDay() {
      // Given
      ApplyOvertimeCommand command = new ApplyOvertimeCommand();
      command.setOvertimeDate(LocalDate.now());
      command.setStartTime(LocalTime.of(22, 0));
      command.setEndTime(LocalTime.of(2, 0)); // 跨天
      command.setReason("项目紧急");

      when(overtimeRepository.save(any(OvertimeApplication.class)))
          .thenAnswer(
              invocation -> {
                OvertimeApplication app = invocation.getArgument(0);
                app.setId(TEST_APPLICATION_ID);
                app.setApplicationNo("OT2024001");
                return true;
              });

      // When
      OvertimeApplicationDTO result = overtimeAppService.applyOvertime(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getOvertimeHours()).isGreaterThan(BigDecimal.ZERO);
      verify(overtimeRepository).save(any(OvertimeApplication.class));
    }

    @Test
    @DisplayName("加班时长超过12小时应该失败")
    void applyOvertime_shouldFail_whenHoursTooLong() {
      // Given
      ApplyOvertimeCommand command = new ApplyOvertimeCommand();
      command.setOvertimeDate(LocalDate.now());
      command.setStartTime(LocalTime.of(9, 0));
      command.setEndTime(LocalTime.of(22, 0)); // 13小时，超过12小时
      command.setReason("项目紧急");

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> overtimeAppService.applyOvertime(command));
      assertThat(exception.getMessage()).contains("单次加班时长不能超过12小时");
    }

    @Test
    @DisplayName("加班时长小于等于0应该失败")
    void applyOvertime_shouldFail_whenHoursZero() {
      // Given
      ApplyOvertimeCommand command = new ApplyOvertimeCommand();
      command.setOvertimeDate(LocalDate.now());
      command.setStartTime(LocalTime.of(18, 0));
      command.setEndTime(LocalTime.of(18, 0)); // 相同时间，时长为0
      command.setReason("项目紧急");

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> overtimeAppService.applyOvertime(command));
      assertThat(exception.getMessage()).contains("加班时长必须大于0");
    }
  }

  @Nested
  @DisplayName("审批加班申请测试")
  class ApproveOvertimeTests {

    @Test
    @DisplayName("应该成功审批通过加班申请")
    void approveOvertime_shouldSuccess_whenApproved() {
      // Given
      securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(true);

      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicationNo("OT2024001")
              .userId(TEST_USER_ID)
              .status(OvertimeApplication.STATUS_PENDING)
              .build();

      when(overtimeRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      when(overtimeRepository.updateById(any(OvertimeApplication.class))).thenReturn(true);

      // When
      OvertimeApplicationDTO result =
          overtimeAppService.approveOvertime(TEST_APPLICATION_ID, true, "同意");

      // Then
      assertThat(result).isNotNull();
      assertThat(application.getStatus()).isEqualTo(OvertimeApplication.STATUS_APPROVED);
      assertThat(application.getApproverId()).isEqualTo(TEST_APPROVER_ID);
      assertThat(application.getApprovedAt()).isNotNull();
      verify(overtimeRepository).updateById(application);
    }

    @Test
    @DisplayName("应该成功审批拒绝加班申请")
    void approveOvertime_shouldSuccess_whenRejected() {
      // Given
      securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(true);

      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicationNo("OT2024001")
              .userId(TEST_USER_ID)
              .status(OvertimeApplication.STATUS_PENDING)
              .build();

      when(overtimeRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      when(overtimeRepository.updateById(any(OvertimeApplication.class))).thenReturn(true);

      // When
      OvertimeApplicationDTO result =
          overtimeAppService.approveOvertime(TEST_APPLICATION_ID, false, "不同意");

      // Then
      assertThat(result).isNotNull();
      assertThat(application.getStatus()).isEqualTo(OvertimeApplication.STATUS_REJECTED);
    }

    @Test
    @DisplayName("已处理的申请不能重复审批")
    void approveOvertime_shouldFail_whenAlreadyProcessed() {
      // Given
      securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(true);

      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .status(OvertimeApplication.STATUS_APPROVED) // 已审批
              .build();

      when(overtimeRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> overtimeAppService.approveOvertime(TEST_APPLICATION_ID, true, "同意"));
      assertThat(exception.getMessage()).contains("只有待审批的申请可以审批");
    }

    @Test
    @DisplayName("没有权限不能审批")
    void approveOvertime_shouldFail_whenNoPermission() {
      // Given
      securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(false); // 没有权限

      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .status(OvertimeApplication.STATUS_PENDING)
              .build();

      when(overtimeRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> overtimeAppService.approveOvertime(TEST_APPLICATION_ID, true, "同意"));
      assertThat(exception.getMessage()).contains("权限不足");
    }

    @Test
    @DisplayName("不能审批自己的申请")
    void approveOvertime_shouldFail_whenSelfApproval() {
      // Given
      securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(true);

      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .userId(TEST_USER_ID) // 自己申请的
              .status(OvertimeApplication.STATUS_PENDING)
              .build();

      when(overtimeRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> overtimeAppService.approveOvertime(TEST_APPLICATION_ID, true, "同意"));
      assertThat(exception.getMessage()).contains("不能审批自己的加班申请");
    }
  }

  @Nested
  @DisplayName("查询加班申请测试")
  class QueryOvertimeTests {

    @Test
    @DisplayName("应该成功查询我的加班申请")
    void getMyApplications_shouldSuccess() {
      // Given
      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicationNo("OT2024001")
              .userId(TEST_USER_ID)
              .status(OvertimeApplication.STATUS_PENDING)
              .build();

      when(overtimeMapper.selectByUserId(TEST_USER_ID))
          .thenReturn(Collections.singletonList(application));

      // When
      List<OvertimeApplicationDTO> result = overtimeAppService.getMyApplications();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getApplicationNo()).isEqualTo("OT2024001");
    }

    @Test
    @DisplayName("应该成功查询指定日期范围的加班申请")
    void getApplicationsByDateRange_shouldSuccess() {
      // Given
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);

      OvertimeApplication application =
          OvertimeApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicationNo("OT2024001")
              .userId(TEST_USER_ID)
              .overtimeDate(LocalDate.of(2024, 1, 15))
              .status(OvertimeApplication.STATUS_PENDING)
              .build();

      when(overtimeMapper.selectByDateRange(eq(TEST_USER_ID), eq(startDate), eq(endDate)))
          .thenReturn(Collections.singletonList(application));

      // When
      List<OvertimeApplicationDTO> result =
          overtimeAppService.getApplicationsByDateRange(startDate, endDate);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getApplicationNo()).isEqualTo("OT2024001");
    }
  }
}
