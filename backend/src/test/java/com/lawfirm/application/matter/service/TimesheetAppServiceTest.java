package com.lawfirm.application.matter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateTimesheetCommand;
import com.lawfirm.application.matter.dto.TimesheetDTO;
import com.lawfirm.application.matter.dto.TimesheetQueryDTO;
import com.lawfirm.common.constant.TimesheetStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.HourlyRate;
import com.lawfirm.domain.matter.entity.Timesheet;
import com.lawfirm.domain.matter.repository.HourlyRateRepository;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TimesheetRepository;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

/** TimesheetAppService 单元测试 测试工时管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetAppService 工时服务测试")
class TimesheetAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_DEPT_ID = 1L;
  private static final Long TEST_MATTER_ID = 100L;

  @Mock private TimesheetRepository timesheetRepository;

  @Mock private HourlyRateRepository hourlyRateRepository;

  @Mock private TimesheetMapper timesheetMapper;

  @Mock private MatterRepository matterRepository;

  @Mock private MatterParticipantRepository matterParticipantRepository;

  @Mock private com.lawfirm.infrastructure.persistence.mapper.DepartmentMapper departmentMapper;

  @Mock private com.lawfirm.domain.system.repository.UserRepository userRepository;

  @InjectMocks private TimesheetAppService timesheetAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);
    securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建工时记录测试")
  class CreateTimesheetTests {

    @Test
    @DisplayName("应该成功创建工时记录")
    void createTimesheet_shouldSuccess() {
      // Given
      CreateTimesheetCommand command = new CreateTimesheetCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setWorkDate(LocalDate.now());
      command.setHours(BigDecimal.valueOf(8));
      command.setWorkType("LEGAL_RESEARCH");
      command.setWorkContent("法律研究");
      command.setBillable(true);
      command.setHourlyRate(BigDecimal.valueOf(500));

      when(timesheetRepository.save(any(Timesheet.class))).thenReturn(true);

      // When
      TimesheetDTO result = timesheetAppService.createTimesheet(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getHours()).isEqualByComparingTo(BigDecimal.valueOf(8));
      assertThat(result.getWorkType()).isEqualTo("LEGAL_RESEARCH");
      assertThat(result.getBillable()).isTrue();
      assertThat(result.getStatus()).isEqualTo(TimesheetStatus.DRAFT);
      verify(timesheetRepository).save(any(Timesheet.class));
    }

    @Test
    @DisplayName("应该自动获取小时费率")
    void createTimesheet_shouldAutoGetHourlyRate() {
      // Given
      CreateTimesheetCommand command = new CreateTimesheetCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setWorkDate(LocalDate.now());
      command.setHours(BigDecimal.valueOf(8));
      command.setWorkType("LEGAL_RESEARCH");
      command.setWorkContent("法律研究");
      command.setBillable(true);
      command.setHourlyRate(null); // 不指定费率

      HourlyRate rate = new HourlyRate();
      rate.setRate(BigDecimal.valueOf(600));
      when(hourlyRateRepository.findCurrentRate(eq(TEST_USER_ID), any(LocalDate.class)))
          .thenReturn(rate);
      when(timesheetRepository.save(any(Timesheet.class))).thenReturn(true);

      // When
      TimesheetDTO result = timesheetAppService.createTimesheet(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getHourlyRate()).isEqualByComparingTo(BigDecimal.valueOf(600));
      // 金额应该是 8 * 600 = 4800
      assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(4800));
    }

    @Test
    @DisplayName("不可计费工时金额应为零")
    void createTimesheet_shouldSetZeroAmount_whenNotBillable() {
      // Given
      CreateTimesheetCommand command = new CreateTimesheetCommand();
      command.setMatterId(TEST_MATTER_ID);
      command.setWorkDate(LocalDate.now());
      command.setHours(BigDecimal.valueOf(8));
      command.setWorkType("ADMIN");
      command.setWorkContent("行政工作");
      command.setBillable(false);
      command.setHourlyRate(BigDecimal.valueOf(500));

      when(timesheetRepository.save(any(Timesheet.class))).thenReturn(true);

      // When
      TimesheetDTO result = timesheetAppService.createTimesheet(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getBillable()).isFalse();
      assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("更新工时记录测试")
  class UpdateTimesheetTests {

    @Test
    @DisplayName("应该成功更新工时记录")
    void updateTimesheet_shouldSuccess() {
      // Given
      Timesheet timesheet =
          Timesheet.builder()
              .id(1L)
              .timesheetNo("TS2024001")
              .userId(TEST_USER_ID)
              .status(TimesheetStatus.DRAFT)
              .hours(BigDecimal.valueOf(8))
              .hourlyRate(BigDecimal.valueOf(500))
              .amount(BigDecimal.valueOf(4000))
              .billable(true)
              .build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);
      when(timesheetRepository.updateById(any(Timesheet.class))).thenReturn(true);

      // When
      TimesheetDTO result =
          timesheetAppService.updateTimesheet(
              1L, LocalDate.now(), BigDecimal.valueOf(10), "LEGAL_RESEARCH", "更新内容", true);

      // Then
      assertThat(result).isNotNull();
      assertThat(timesheet.getHours()).isEqualByComparingTo(BigDecimal.valueOf(10));
      assertThat(timesheet.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000)); // 10 * 500
      verify(timesheetRepository).updateById(timesheet);
    }

    @Test
    @DisplayName("非草稿状态不能修改")
    void updateTimesheet_shouldFail_whenNotDraft() {
      // Given
      Timesheet timesheet =
          Timesheet.builder().id(1L).userId(TEST_USER_ID).status(TimesheetStatus.APPROVED).build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> timesheetAppService.updateTimesheet(1L, null, null, null, null, null));
      assertThat(exception.getMessage()).contains("草稿状态");
    }

    @Test
    @DisplayName("只能修改自己的工时记录")
    void updateTimesheet_shouldFail_whenNotOwner() {
      // Given
      Timesheet timesheet =
          Timesheet.builder()
              .id(1L)
              .userId(999L) // 其他用户
              .status(TimesheetStatus.DRAFT)
              .build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> timesheetAppService.updateTimesheet(1L, null, null, null, null, null));
      assertThat(exception.getMessage()).contains("自己的工时记录");
    }
  }

  @Nested
  @DisplayName("删除工时记录测试")
  class DeleteTimesheetTests {

    @Test
    @DisplayName("应该成功删除草稿状态的工时记录")
    void deleteTimesheet_shouldSuccess() {
      // Given
      Timesheet timesheet =
          Timesheet.builder()
              .id(1L)
              .timesheetNo("TS2024001")
              .userId(TEST_USER_ID)
              .status(TimesheetStatus.DRAFT)
              .build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);
      when(timesheetRepository.removeById(eq(1L))).thenReturn(true);

      // When
      timesheetAppService.deleteTimesheet(1L);

      // Then
      verify(timesheetRepository).removeById(1L);
    }

    @Test
    @DisplayName("已审批的工时记录不能删除")
    void deleteTimesheet_shouldFail_whenApproved() {
      // Given
      Timesheet timesheet =
          Timesheet.builder().id(1L).userId(TEST_USER_ID).status(TimesheetStatus.APPROVED).build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> timesheetAppService.deleteTimesheet(1L));
      assertThat(exception.getMessage()).contains("草稿状态");
    }
  }

  @Nested
  @DisplayName("提交工时测试")
  class SubmitTimesheetTests {

    @Test
    @DisplayName("应该成功提交草稿状态的工时")
    void submitTimesheet_shouldSuccess() {
      // Given
      Timesheet timesheet =
          Timesheet.builder().id(1L).timesheetNo("TS2024001").status(TimesheetStatus.DRAFT).build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);
      when(timesheetRepository.updateById(any(Timesheet.class))).thenReturn(true);

      // When
      TimesheetDTO result = timesheetAppService.submitTimesheet(1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(timesheet.getStatus()).isEqualTo(TimesheetStatus.SUBMITTED);
      assertThat(timesheet.getSubmittedAt()).isNotNull();
      verify(timesheetRepository).updateById(timesheet);
    }

    @Test
    @DisplayName("已提交的工时不能再次提交")
    void submitTimesheet_shouldFail_whenAlreadySubmitted() {
      // Given
      Timesheet timesheet = Timesheet.builder().id(1L).status(TimesheetStatus.SUBMITTED).build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> timesheetAppService.submitTimesheet(1L));
      assertThat(exception.getMessage()).contains("草稿状态");
    }

    @Test
    @DisplayName("应该成功批量提交工时")
    void batchSubmit_shouldSuccess() {
      // Given
      Timesheet timesheet1 =
          Timesheet.builder().id(1L).userId(TEST_USER_ID).status(TimesheetStatus.DRAFT).build();
      Timesheet timesheet2 =
          Timesheet.builder().id(2L).userId(TEST_USER_ID).status(TimesheetStatus.DRAFT).build();

      when(timesheetRepository.listByIds(Arrays.asList(1L, 2L)))
          .thenReturn(Arrays.asList(timesheet1, timesheet2));
      when(timesheetRepository.updateById(any(Timesheet.class))).thenReturn(true);

      // When
      TimesheetAppService.BatchSubmitResult result =
          timesheetAppService.batchSubmit(Arrays.asList(1L, 2L));

      // Then
      assertThat(result.getSuccessCount()).isEqualTo(2);
      assertThat(result.getSuccessIds()).containsExactly(1L, 2L);
      assertThat(result.getFailureCount()).isEqualTo(0);
      assertThat(timesheet1.getStatus()).isEqualTo(TimesheetStatus.SUBMITTED);
      assertThat(timesheet2.getStatus()).isEqualTo(TimesheetStatus.SUBMITTED);
    }

    @Test
    @DisplayName("批量提交时如果有一个失败应该全部失败")
    void batchSubmit_shouldFail_whenOneInvalid() {
      // Given
      Timesheet timesheet1 =
          Timesheet.builder().id(1L).userId(TEST_USER_ID).status(TimesheetStatus.DRAFT).build();
      Timesheet timesheet2 =
          Timesheet.builder()
              .id(2L)
              .userId(TEST_USER_ID)
              .status(TimesheetStatus.APPROVED) // 已审批，不能提交
              .build();

      when(timesheetRepository.listByIds(Arrays.asList(1L, 2L)))
          .thenReturn(Arrays.asList(timesheet1, timesheet2));

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> timesheetAppService.batchSubmit(Arrays.asList(1L, 2L)));
      assertThat(exception.getMessage()).contains("状态不是草稿");
    }
  }

  @Nested
  @DisplayName("审批工时测试")
  class ApproveTimesheetTests {

    @Test
    @DisplayName("应该成功审批通过")
    void approveTimesheet_shouldSuccess() {
      // Given
      Timesheet timesheet =
          Timesheet.builder()
              .id(1L)
              .timesheetNo("TS2024001")
              .status(TimesheetStatus.SUBMITTED)
              .build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);
      when(timesheetRepository.updateById(any(Timesheet.class))).thenReturn(true);
      // Mock SecurityUtils 以通过权限验证
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

      // When
      TimesheetDTO result = timesheetAppService.approveTimesheet(1L, "审批通过");

      // Then
      assertThat(result).isNotNull();
      assertThat(timesheet.getStatus()).isEqualTo(TimesheetStatus.APPROVED);
      assertThat(timesheet.getApprovedBy()).isEqualTo(TEST_USER_ID);
      assertThat(timesheet.getApprovalComment()).isEqualTo("审批通过");
      verify(timesheetRepository).updateById(timesheet);
    }

    @Test
    @DisplayName("应该成功审批拒绝")
    void rejectTimesheet_shouldSuccess() {
      // Given
      Timesheet timesheet =
          Timesheet.builder()
              .id(1L)
              .timesheetNo("TS2024001")
              .status(TimesheetStatus.SUBMITTED)
              .build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);
      when(timesheetRepository.updateById(any(Timesheet.class))).thenReturn(true);
      // Mock SecurityUtils 以通过权限验证
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

      // When
      TimesheetDTO result = timesheetAppService.rejectTimesheet(1L, "工时不合理");

      // Then
      assertThat(result).isNotNull();
      assertThat(timesheet.getStatus()).isEqualTo(TimesheetStatus.REJECTED);
      assertThat(timesheet.getApprovalComment()).isEqualTo("工时不合理");
    }

    @Test
    @DisplayName("草稿状态不能审批")
    void approveTimesheet_shouldFail_whenDraft() {
      // Given
      Timesheet timesheet = Timesheet.builder().id(1L).status(TimesheetStatus.DRAFT).build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> timesheetAppService.approveTimesheet(1L, ""));
      assertThat(exception.getMessage()).contains("已提交");
    }
  }

  @Nested
  @DisplayName("查询工时测试")
  class QueryTimesheetTests {

    @Test
    @DisplayName("应该成功分页查询工时")
    void listTimesheets_shouldSuccess() {
      // Given
      TimesheetQueryDTO query = new TimesheetQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Timesheet timesheet =
          Timesheet.builder()
              .id(1L)
              .timesheetNo("TS2024001")
              .matterId(TEST_MATTER_ID)
              .userId(TEST_USER_ID)
              .hours(BigDecimal.valueOf(8))
              .build();

      Page<Timesheet> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(timesheet));
      page.setTotal(1L);

      when(timesheetMapper.selectTimesheetPage(
              ArgumentMatchers.<Page<Timesheet>>any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              isNull()))
          .thenReturn(page);

      // When
      PageResult<TimesheetDTO> result = timesheetAppService.listTimesheets(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("应该成功获取工时详情")
    void getTimesheetById_shouldSuccess() {
      // Given
      Timesheet timesheet =
          Timesheet.builder().id(1L).timesheetNo("TS2024001").hours(BigDecimal.valueOf(8)).build();

      when(timesheetRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(timesheet);

      // When
      TimesheetDTO result = timesheetAppService.getTimesheetById(1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTimesheetNo()).isEqualTo("TS2024001");
      assertThat(result.getHours()).isEqualByComparingTo(BigDecimal.valueOf(8));
    }
  }
}
