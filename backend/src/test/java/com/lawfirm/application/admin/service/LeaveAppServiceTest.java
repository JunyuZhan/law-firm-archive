package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.ApplyLeaveCommand;
import com.lawfirm.application.admin.command.ApproveLeaveCommand;
import com.lawfirm.application.admin.dto.LeaveApplicationDTO;
import com.lawfirm.application.admin.dto.LeaveApplicationQueryDTO;
import com.lawfirm.application.admin.dto.LeaveBalanceDTO;
import com.lawfirm.application.admin.dto.LeaveTypeDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.LeaveApplication;
import com.lawfirm.domain.admin.entity.LeaveBalance;
import com.lawfirm.domain.admin.entity.LeaveType;
import com.lawfirm.domain.admin.repository.LeaveApplicationRepository;
import com.lawfirm.domain.admin.repository.LeaveBalanceRepository;
import com.lawfirm.domain.admin.repository.LeaveTypeRepository;
import com.lawfirm.infrastructure.persistence.mapper.LeaveApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.LeaveBalanceMapper;
import com.lawfirm.infrastructure.persistence.mapper.LeaveTypeMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LeaveAppService 单元测试
 * 测试请假管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveAppService 请假服务测试")
class LeaveAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_APPROVER_ID = 2L;
    private static final Long TEST_APPLICATION_ID = 100L;
    private static final Long TEST_LEAVE_TYPE_ID = 200L;
    private static final Long OTHER_USER_ID = 999L;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveTypeMapper leaveTypeMapper;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private LeaveApplicationMapper leaveApplicationMapper;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveBalanceMapper leaveBalanceMapper;

    @InjectMocks
    private LeaveAppService leaveAppService;

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
    @DisplayName("查询请假类型测试")
    class QueryLeaveTypeTests {

        @Test
        @DisplayName("应该成功获取所有启用的请假类型")
        void listLeaveTypes_shouldSuccess() {
            // Given
            LeaveType type = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .name("年假")
                    .code("ANNUAL")
                    .enabled(true)
                    .build();

            when(leaveTypeMapper.selectEnabledTypes()).thenReturn(Collections.singletonList(type));

            // When
            List<LeaveTypeDTO> result = leaveAppService.listLeaveTypes();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("年假");
        }
    }

    @Nested
    @DisplayName("查询请假申请测试")
    class QueryApplicationTests {

        @Test
        @DisplayName("应该成功分页查询请假申请")
        void listApplications_shouldSuccess() {
            // Given
            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("LV2024001")
                    .userId(TEST_USER_ID)
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            LeaveType type = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .name("年假")
                    .build();

            Page<LeaveApplication> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(application));
            page.setTotal(1L);

            when(leaveApplicationMapper.selectApplicationPage(any(Page.class), any(), any(), any(), any(), any()))
                    .thenReturn(page);
            when(leaveTypeRepository.listByIds(anyList())).thenReturn(Collections.singletonList(type));

            LeaveApplicationQueryDTO query = new LeaveApplicationQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<LeaveApplicationDTO> result = leaveAppService.listApplications(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }

    }

    @Nested
    @DisplayName("提交请假申请测试")
    class ApplyLeaveTests {

        @Test
        @DisplayName("应该成功提交请假申请")
        void applyLeave_shouldSuccess() {
            // Given
            ApplyLeaveCommand command = new ApplyLeaveCommand();
            command.setLeaveTypeId(TEST_LEAVE_TYPE_ID);
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(3));
            command.setDuration(BigDecimal.valueOf(2));
            command.setReason("个人原因");

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .name("年假")
                    .enabled(true)
                    .annualLimit(BigDecimal.valueOf(10))
                    .build();

            LeaveBalance balance = LeaveBalance.builder()
                    .userId(TEST_USER_ID)
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .remainingDays(BigDecimal.valueOf(5))
                    .build();

            when(leaveTypeRepository.getByIdOrThrow(eq(TEST_LEAVE_TYPE_ID), anyString())).thenReturn(leaveType);
            when(leaveApplicationMapper.countOverlapping(eq(TEST_USER_ID), any(), any())).thenReturn(0);
            when(leaveBalanceMapper.selectByUserTypeYear(eq(TEST_USER_ID), eq(TEST_LEAVE_TYPE_ID), anyInt()))
                    .thenReturn(balance);
            when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> {
                LeaveApplication app = invocation.getArgument(0);
                app.setId(TEST_APPLICATION_ID);
                app.setApplicationNo("LV2024001");
                return true;
            });

            // When
            LeaveApplicationDTO result = leaveAppService.applyLeave(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(LeaveApplication.STATUS_PENDING);
            verify(leaveApplicationRepository).save(any(LeaveApplication.class));
        }

        @Test
        @DisplayName("开始时间晚于结束时间应该失败")
        void applyLeave_shouldFail_whenStartTimeAfterEndTime() {
            // Given
            ApplyLeaveCommand command = new ApplyLeaveCommand();
            command.setLeaveTypeId(TEST_LEAVE_TYPE_ID);
            command.setStartTime(LocalDateTime.now().plusDays(3));
            command.setEndTime(LocalDateTime.now().plusDays(1)); // 早于开始时间

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .enabled(true)
                    .build();

            when(leaveTypeRepository.getByIdOrThrow(eq(TEST_LEAVE_TYPE_ID), anyString())).thenReturn(leaveType);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.applyLeave(command));
            assertThat(exception.getMessage()).contains("开始时间不能晚于结束时间");
        }

        @Test
        @DisplayName("开始时间早于当前时间应该失败")
        void applyLeave_shouldFail_whenStartTimeBeforeNow() {
            // Given
            ApplyLeaveCommand command = new ApplyLeaveCommand();
            command.setLeaveTypeId(TEST_LEAVE_TYPE_ID);
            command.setStartTime(LocalDateTime.now().minusDays(1)); // 过去的时间
            command.setEndTime(LocalDateTime.now().plusDays(1));

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .enabled(true)
                    .build();

            when(leaveTypeRepository.getByIdOrThrow(eq(TEST_LEAVE_TYPE_ID), anyString())).thenReturn(leaveType);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.applyLeave(command));
            assertThat(exception.getMessage()).contains("开始时间不能早于当前时间");
        }

        @Test
        @DisplayName("时间段重叠应该失败")
        void applyLeave_shouldFail_whenTimeOverlapping() {
            // Given
            ApplyLeaveCommand command = new ApplyLeaveCommand();
            command.setLeaveTypeId(TEST_LEAVE_TYPE_ID);
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(3));

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .enabled(true)
                    .build();

            when(leaveTypeRepository.getByIdOrThrow(eq(TEST_LEAVE_TYPE_ID), anyString())).thenReturn(leaveType);
            when(leaveApplicationMapper.countOverlapping(eq(TEST_USER_ID), any(), any())).thenReturn(1); // 有重叠

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.applyLeave(command));
            assertThat(exception.getMessage()).contains("该时间段已有请假申请");
        }

        @Test
        @DisplayName("假期余额不足应该失败")
        void applyLeave_shouldFail_whenBalanceInsufficient() {
            // Given
            ApplyLeaveCommand command = new ApplyLeaveCommand();
            command.setLeaveTypeId(TEST_LEAVE_TYPE_ID);
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(3));
            command.setDuration(BigDecimal.valueOf(5)); // 需要5天

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .enabled(true)
                    .annualLimit(BigDecimal.valueOf(10))
                    .build();

            LeaveBalance balance = LeaveBalance.builder()
                    .remainingDays(BigDecimal.valueOf(2)) // 只剩2天
                    .build();

            when(leaveTypeRepository.getByIdOrThrow(eq(TEST_LEAVE_TYPE_ID), anyString())).thenReturn(leaveType);
            when(leaveApplicationMapper.countOverlapping(eq(TEST_USER_ID), any(), any())).thenReturn(0);
            when(leaveBalanceMapper.selectByUserTypeYear(eq(TEST_USER_ID), eq(TEST_LEAVE_TYPE_ID), anyInt()))
                    .thenReturn(balance);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.applyLeave(command));
            assertThat(exception.getMessage()).contains("假期余额不足");
        }

        @Test
        @DisplayName("已禁用的请假类型应该失败")
        void applyLeave_shouldFail_whenTypeDisabled() {
            // Given
            ApplyLeaveCommand command = new ApplyLeaveCommand();
            command.setLeaveTypeId(TEST_LEAVE_TYPE_ID);

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .enabled(false) // 已禁用
                    .build();

            when(leaveTypeRepository.getByIdOrThrow(eq(TEST_LEAVE_TYPE_ID), anyString())).thenReturn(leaveType);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.applyLeave(command));
            assertThat(exception.getMessage()).contains("该请假类型已禁用");
        }
    }

    @Nested
    @DisplayName("审批请假申请测试")
    class ApproveLeaveTests {

        @Test
        @DisplayName("应该成功审批通过请假申请")
        void approveLeave_shouldSuccess_whenApproved() {
            // Given
            securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);

            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("LV2024001")
                    .userId(TEST_USER_ID)
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .duration(BigDecimal.valueOf(2))
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .annualLimit(BigDecimal.valueOf(10))
                    .build();

            ApproveLeaveCommand command = new ApproveLeaveCommand();
            command.setApplicationId(TEST_APPLICATION_ID);
            command.setApproved(true);
            command.setComment("同意");

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(leaveTypeRepository.getById(TEST_LEAVE_TYPE_ID)).thenReturn(leaveType);
            when(leaveBalanceMapper.deductBalance(eq(TEST_USER_ID), eq(TEST_LEAVE_TYPE_ID), anyInt(), any()))
                    .thenReturn(1);
            when(leaveApplicationRepository.updateById(any(LeaveApplication.class))).thenReturn(true);

            // When
            LeaveApplicationDTO result = leaveAppService.approveLeave(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(application.getStatus()).isEqualTo(LeaveApplication.STATUS_APPROVED);
            assertThat(application.getApproverId()).isEqualTo(TEST_APPROVER_ID);
            assertThat(application.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该成功审批拒绝请假申请")
        void approveLeave_shouldSuccess_whenRejected() {
            // Given
            securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);

            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("LV2024001")
                    .userId(TEST_USER_ID)
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            ApproveLeaveCommand command = new ApproveLeaveCommand();
            command.setApplicationId(TEST_APPLICATION_ID);
            command.setApproved(false);
            command.setComment("不同意");

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(leaveApplicationRepository.updateById(any(LeaveApplication.class))).thenReturn(true);
            when(leaveTypeRepository.getById(TEST_LEAVE_TYPE_ID)).thenReturn(null);

            // When
            LeaveApplicationDTO result = leaveAppService.approveLeave(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(application.getStatus()).isEqualTo(LeaveApplication.STATUS_REJECTED);
        }

        @Test
        @DisplayName("已处理的申请不能重复审批")
        void approveLeave_shouldFail_whenAlreadyProcessed() {
            // Given
            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .status(LeaveApplication.STATUS_APPROVED) // 已审批
                    .build();

            ApproveLeaveCommand command = new ApproveLeaveCommand();
            command.setApplicationId(TEST_APPLICATION_ID);
            command.setApproved(true);

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.approveLeave(command));
            assertThat(exception.getMessage()).contains("该申请已处理");
        }

        @Test
        @DisplayName("余额扣减失败应该失败")
        void approveLeave_shouldFail_whenBalanceDeductFailed() {
            // Given
            securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_APPROVER_ID);

            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .userId(TEST_USER_ID)
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .duration(BigDecimal.valueOf(2))
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            LeaveType leaveType = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .annualLimit(BigDecimal.valueOf(10))
                    .build();

            ApproveLeaveCommand command = new ApproveLeaveCommand();
            command.setApplicationId(TEST_APPLICATION_ID);
            command.setApproved(true);

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(leaveTypeRepository.getById(TEST_LEAVE_TYPE_ID)).thenReturn(leaveType);
            when(leaveBalanceMapper.deductBalance(eq(TEST_USER_ID), eq(TEST_LEAVE_TYPE_ID), anyInt(), any()))
                    .thenReturn(0); // 扣减失败

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.approveLeave(command));
            assertThat(exception.getMessage()).contains("扣减假期余额失败");
        }
    }

    @Nested
    @DisplayName("取消请假申请测试")
    class CancelApplicationTests {

        @Test
        @DisplayName("应该成功取消自己的请假申请")
        void cancelApplication_shouldSuccess() {
            // Given
            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("LV2024001")
                    .userId(TEST_USER_ID)
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(leaveApplicationRepository.updateById(any(LeaveApplication.class))).thenReturn(true);

            // When
            leaveAppService.cancelApplication(TEST_APPLICATION_ID);

            // Then
            assertThat(application.getStatus()).isEqualTo(LeaveApplication.STATUS_CANCELLED);
            verify(leaveApplicationRepository).updateById(application);
        }

        @Test
        @DisplayName("不能取消他人的申请")
        void cancelApplication_shouldFail_whenNotOwner() {
            // Given
            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .userId(OTHER_USER_ID) // 其他用户
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.cancelApplication(TEST_APPLICATION_ID));
            assertThat(exception.getMessage()).contains("只能取消自己的申请");
        }

        @Test
        @DisplayName("只能取消待审批的申请")
        void cancelApplication_shouldFail_whenNotPending() {
            // Given
            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .userId(TEST_USER_ID)
                    .status(LeaveApplication.STATUS_APPROVED) // 已审批
                    .build();

            when(leaveApplicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveAppService.cancelApplication(TEST_APPLICATION_ID));
            assertThat(exception.getMessage()).contains("只能取消待审批的申请");
        }
    }

    @Nested
    @DisplayName("假期余额测试")
    class LeaveBalanceTests {

        @Test
        @DisplayName("应该成功获取用户假期余额")
        void getUserBalance_shouldSuccess() {
            // Given
            LeaveBalance balance = LeaveBalance.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .year(2024)
                    .totalDays(BigDecimal.valueOf(10))
                    .usedDays(BigDecimal.valueOf(3))
                    .remainingDays(BigDecimal.valueOf(7))
                    .build();

            LeaveType type = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .name("年假")
                    .build();

            when(leaveBalanceMapper.selectByUserAndYear(eq(TEST_USER_ID), eq(2024)))
                    .thenReturn(Collections.singletonList(balance));
            when(leaveTypeRepository.listByIds(anyList())).thenReturn(Collections.singletonList(type));

            // When
            List<LeaveBalanceDTO> result = leaveAppService.getUserBalance(TEST_USER_ID, 2024);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(7));
        }

        @Test
        @DisplayName("应该使用当前用户和年份作为默认值")
        void getUserBalance_shouldUseDefaults() {
            // Given
            when(leaveBalanceMapper.selectByUserAndYear(eq(TEST_USER_ID), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            List<LeaveBalanceDTO> result = leaveAppService.getUserBalance(null, null);

            // Then
            assertThat(result).isEmpty();
            verify(leaveBalanceMapper).selectByUserAndYear(eq(TEST_USER_ID), eq(LocalDate.now().getYear()));
        }
    }

    @Nested
    @DisplayName("初始化假期余额测试")
    class InitBalanceTests {

        @Test
        @DisplayName("应该成功初始化用户年度假期余额")
        void initUserBalance_shouldSuccess() {
            // Given
            LeaveType type1 = LeaveType.builder()
                    .id(1L)
                    .name("年假")
                    .annualLimit(BigDecimal.valueOf(10))
                    .enabled(true)
                    .build();
            LeaveType type2 = LeaveType.builder()
                    .id(2L)
                    .name("病假")
                    .annualLimit(BigDecimal.valueOf(5))
                    .enabled(true)
                    .build();

            when(leaveTypeMapper.selectEnabledTypes()).thenReturn(Arrays.asList(type1, type2));
            when(leaveBalanceMapper.selectByUserAndYear(TEST_USER_ID, 2024)).thenReturn(Collections.emptyList());
            when(leaveBalanceRepository.saveBatch(anyList())).thenReturn(true);

            // When
            leaveAppService.initUserBalance(TEST_USER_ID, 2024);

            // Then
            verify(leaveBalanceRepository).saveBatch(anyList());
        }

        @Test
        @DisplayName("已有余额的类型不应该重复创建")
        void initUserBalance_shouldSkipExisting() {
            // Given
            LeaveType type = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .name("年假")
                    .annualLimit(BigDecimal.valueOf(10))
                    .enabled(true)
                    .build();

            LeaveBalance existingBalance = LeaveBalance.builder()
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .build();

            when(leaveTypeMapper.selectEnabledTypes()).thenReturn(Collections.singletonList(type));
            when(leaveBalanceMapper.selectByUserAndYear(TEST_USER_ID, 2024))
                    .thenReturn(Collections.singletonList(existingBalance));

            // When
            leaveAppService.initUserBalance(TEST_USER_ID, 2024);

            // Then
            verify(leaveBalanceRepository, never()).saveBatch(anyList());
        }
    }

    @Nested
    @DisplayName("待审批列表测试")
    class PendingApplicationsTests {

        @Test
        @DisplayName("应该成功获取待审批列表")
        void getPendingApplications_shouldSuccess() {
            // Given
            LeaveApplication application = LeaveApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("LV2024001")
                    .leaveTypeId(TEST_LEAVE_TYPE_ID)
                    .status(LeaveApplication.STATUS_PENDING)
                    .build();

            LeaveType type = LeaveType.builder()
                    .id(TEST_LEAVE_TYPE_ID)
                    .name("年假")
                    .build();

            when(leaveApplicationMapper.selectPendingApplications()).thenReturn(Collections.singletonList(application));
            when(leaveTypeRepository.listByIds(anyList())).thenReturn(Collections.singletonList(type));

            // When
            List<LeaveApplicationDTO> result = leaveAppService.getPendingApplications();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApplicationNo()).isEqualTo("LV2024001");
        }
    }
}
