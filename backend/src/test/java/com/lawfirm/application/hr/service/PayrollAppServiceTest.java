package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.*;
import com.lawfirm.application.hr.dto.PayrollItemDTO;
import com.lawfirm.application.hr.dto.PayrollSheetDTO;
import com.lawfirm.application.hr.dto.PayrollSheetQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.constant.CommissionStatus;
import com.lawfirm.common.constant.EmployeeStatus;
import com.lawfirm.common.constant.PayrollStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.hr.entity.*;
import com.lawfirm.domain.hr.repository.*;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.external.excel.ExcelImportExportService;
import com.lawfirm.infrastructure.persistence.mapper.PayrollSheetMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PayrollAppService 单元测试
 * 测试薪资管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollAppService 薪资服务测试")
class PayrollAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_SHEET_ID = 100L;
    private static final Long TEST_ITEM_ID = 200L;
    private static final Long TEST_EMPLOYEE_ID = 300L;

    @Mock
    private PayrollSheetRepository payrollSheetRepository;

    @Mock
    private PayrollSheetMapper payrollSheetMapper;

    @Mock
    private PayrollItemRepository payrollItemRepository;

    @Mock
    private NotificationAppService notificationAppService;

    @Mock
    private PayrollIncomeRepository payrollIncomeRepository;

    @Mock
    private PayrollDeductionRepository payrollDeductionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    @Qualifier("hrContractRepository")
    private com.lawfirm.domain.hr.repository.ContractRepository hrContractRepository;

    @Mock
    @Qualifier("financeContractRepository")
    private ContractRepository financeContractRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExcelImportExportService excelImportExportService;

    @InjectMocks
    private PayrollAppService payrollAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("FINANCE", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("创建工资表测试")
    class CreatePayrollSheetTests {

        @Test
        @DisplayName("应该成功创建工资表")
        void createPayrollSheet_shouldSuccess() {
            // Given
            CreatePayrollSheetCommand command = new CreatePayrollSheetCommand();
            command.setPayrollYear(2024);
            command.setPayrollMonth(1);

            when(payrollSheetRepository.findByYearAndMonth(2024, 1)).thenReturn(Optional.empty());
            when(payrollSheetRepository.save(any(PayrollSheet.class))).thenAnswer(invocation -> {
                PayrollSheet sheet = invocation.getArgument(0);
                sheet.setId(TEST_SHEET_ID);
                sheet.setPayrollNo("GZ202401001");
                return true;
            });
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<Employee> queryWrapper = 
                    mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
            when(employeeRepository.lambdaQuery()).thenReturn(queryWrapper);
            when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
            when(queryWrapper.list()).thenReturn(Collections.emptyList());
            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenAnswer(invocation -> {
                PayrollSheet sheet = new PayrollSheet();
                sheet.setId(TEST_SHEET_ID);
                sheet.setPayrollNo("GZ202401001");
                sheet.setPayrollYear(2024);
                sheet.setPayrollMonth(1);
                sheet.setTotalEmployees(0);
                sheet.setStatus(PayrollStatus.DRAFT);
                return sheet;
            });
            when(payrollSheetRepository.updateById(any(PayrollSheet.class))).thenReturn(true);

            // When
            PayrollSheetDTO result = payrollAppService.createPayrollSheet(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPayrollYear()).isEqualTo(2024);
            assertThat(result.getPayrollMonth()).isEqualTo(1);
            assertThat(result.getStatus()).isEqualTo(PayrollStatus.DRAFT);
            verify(payrollSheetRepository).save(any(PayrollSheet.class));
        }

        @Test
        @DisplayName("该年月的工资表已存在应该失败")
        void createPayrollSheet_shouldFail_whenExists() {
            // Given
            CreatePayrollSheetCommand command = new CreatePayrollSheetCommand();
            command.setPayrollYear(2024);
            command.setPayrollMonth(1);

            PayrollSheet existing = PayrollSheet.builder()
                    .id(999L)
                    .payrollYear(2024)
                    .payrollMonth(1)
                    .build();

            when(payrollSheetRepository.findByYearAndMonth(2024, 1)).thenReturn(Optional.of(existing));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> payrollAppService.createPayrollSheet(command));
            assertThat(exception.getMessage()).contains("该年月的工资表已存在");
        }

        @Test
        @DisplayName("非财务角色不能创建工资表")
        void createPayrollSheet_shouldFail_whenNoPermission() {
            // Given
            CreatePayrollSheetCommand command = new CreatePayrollSheetCommand();
            command.setPayrollYear(2024);
            command.setPayrollMonth(1);

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER")); // 普通用户

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> payrollAppService.createPayrollSheet(command));
            assertThat(exception.getMessage()).contains("只有财务角色");
        }
    }

    @Nested
    @DisplayName("查询工资表测试")
    class QueryPayrollSheetTests {

        @Test
        @DisplayName("应该成功分页查询工资表")
        void listPayrollSheets_shouldSuccess() {
            // Given
            PayrollSheetQueryDTO query = new PayrollSheetQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .payrollNo("GZ202401001")
                    .payrollYear(2024)
                    .payrollMonth(1)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "DIRECTOR", "FINANCE")).thenReturn(true);

            @SuppressWarnings("unchecked")
            Page<PayrollSheet> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(sheet));
            page.setTotal(1L);

            when(payrollSheetMapper.selectPayrollSheetPage(any(Page.class), any(), any(), any(), any()))
                    .thenReturn(page);

            // When
            PageResult<PayrollSheetDTO> result = payrollAppService.listPayrollSheets(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getPayrollNo()).isEqualTo("GZ202401001");
        }

        @Test
        @DisplayName("应该成功获取工资表详情")
        void getPayrollSheetById_shouldSuccess() {
            // Given
            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .payrollNo("GZ202401001")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "DIRECTOR", "FINANCE")).thenReturn(true);

            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);
            when(payrollItemRepository.findByPayrollSheetId(TEST_SHEET_ID)).thenReturn(Collections.emptyList());

            // When
            PayrollSheetDTO result = payrollAppService.getPayrollSheetById(TEST_SHEET_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPayrollNo()).isEqualTo("GZ202401001");
        }
    }

    @Nested
    @DisplayName("工资确认测试")
    class ConfirmPayrollTests {

        @Test
        @DisplayName("应该成功确认工资")
        void confirmPayroll_shouldSuccess() {
            // Given
            ConfirmPayrollCommand command = new ConfirmPayrollCommand();
            command.setPayrollItemId(TEST_ITEM_ID);
            command.setConfirmStatus(PayrollStatus.ITEM_CONFIRMED);

            PayrollItem item = PayrollItem.builder()
                    .id(TEST_ITEM_ID)
                    .payrollSheetId(TEST_SHEET_ID)
                    .userId(TEST_USER_ID)
                    .confirmStatus(PayrollStatus.ITEM_PENDING)
                    .build();

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .status(PayrollStatus.PENDING_CONFIRM)
                    .confirmedCount(0)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.getUserId()).thenReturn(TEST_USER_ID);
            when(payrollItemRepository.getByIdOrThrow(eq(TEST_ITEM_ID), anyString())).thenReturn(item);
            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);
            when(payrollItemRepository.updateById(any(PayrollItem.class))).thenReturn(true);
            when(payrollSheetRepository.updateById(any(PayrollSheet.class))).thenReturn(true);

            // When
            payrollAppService.confirmPayrollItem(command);

            // Then
            assertThat(item.getConfirmStatus()).isEqualTo(PayrollStatus.ITEM_CONFIRMED);
            assertThat(item.getConfirmedAt()).isNotNull();
            verify(payrollItemRepository).updateById(item);
        }

        @Test
        @DisplayName("只能确认自己的工资")
        void confirmPayroll_shouldFail_whenNotOwner() {
            // Given
            ConfirmPayrollCommand command = new ConfirmPayrollCommand();
            command.setPayrollItemId(TEST_ITEM_ID);

            PayrollItem item = PayrollItem.builder()
                    .id(TEST_ITEM_ID)
                    .userId(999L) // 其他用户
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.getUserId()).thenReturn(TEST_USER_ID);
            when(payrollItemRepository.getByIdOrThrow(eq(TEST_ITEM_ID), anyString())).thenReturn(item);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> payrollAppService.confirmPayrollItem(command));
            assertThat(exception.getMessage()).contains("只能确认自己的工资");
        }
    }

    @Nested
    @DisplayName("工资审批测试")
    class ApprovePayrollTests {

        @Test
        @DisplayName("应该成功审批工资表")
        void approvePayroll_shouldSuccess() {
            // Given
            ApprovePayrollCommand command = new ApprovePayrollCommand();
            command.setPayrollSheetId(TEST_SHEET_ID);
            command.setApprovalStatus(PayrollStatus.APPROVED);
            command.setApprovalComment("同意");

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .payrollNo("GZ202401001")
                    .status(PayrollStatus.PENDING_APPROVAL)
                    .approverId(TEST_USER_ID)  // Set to current user ID
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "DIRECTOR")).thenReturn(true);
            securityUtilsMock.when(() -> SecurityUtils.getUserId()).thenReturn(TEST_USER_ID);

            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);
            when(payrollSheetRepository.updateById(any(PayrollSheet.class))).thenReturn(true);

            // When
            payrollAppService.approvePayrollSheet(command);

            // Then
            assertThat(sheet.getStatus()).isEqualTo(PayrollStatus.APPROVED);
            assertThat(sheet.getApproverId()).isEqualTo(TEST_USER_ID);
            verify(payrollSheetRepository).updateById(sheet);
        }

        @Test
        @DisplayName("非待审批状态不能审批")
        void approvePayroll_shouldFail_whenNotPending() {
            // Given
            ApprovePayrollCommand command = new ApprovePayrollCommand();
            command.setPayrollSheetId(TEST_SHEET_ID);

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .status(PayrollStatus.APPROVED)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "DIRECTOR")).thenReturn(true);

            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> payrollAppService.approvePayrollSheet(command));
            assertThat(exception.getMessage()).contains("工资表状态不允许审批");
        }
    }

    @Nested
    @DisplayName("发放工资测试")
    class IssuePayrollTests {

        @Test
        @DisplayName("应该成功发放工资")
        void issuePayroll_shouldSuccess() {
            // Given
            IssuePayrollCommand command = new IssuePayrollCommand();
            command.setPayrollSheetId(TEST_SHEET_ID);
            command.setPaymentMethod("BANK");

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .payrollNo("GZ202401001")
                    .status(PayrollStatus.APPROVED)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "FINANCE")).thenReturn(true);

            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);
            when(payrollSheetRepository.updateById(any(PayrollSheet.class))).thenReturn(true);

            // When
            payrollAppService.issuePayroll(command);

            // Then
            assertThat(sheet.getStatus()).isEqualTo(PayrollStatus.ISSUED);
            assertThat(sheet.getIssuedAt()).isNotNull();
            assertThat(sheet.getIssuedBy()).isEqualTo(TEST_USER_ID);
            verify(payrollSheetRepository).updateById(sheet);
        }

        @Test
        @DisplayName("已发放的工资表不能重复发放")
        void issuePayroll_shouldFail_whenAlreadyIssued() {
            // Given
            IssuePayrollCommand command = new IssuePayrollCommand();
            command.setPayrollSheetId(TEST_SHEET_ID);

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .status(PayrollStatus.ISSUED)
                    .issuedAt(LocalDateTime.now())
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "FINANCE")).thenReturn(true);

            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> payrollAppService.issuePayroll(command));
            assertThat(exception.getMessage()).contains("已发放");
        }

        @Test
        @DisplayName("只有已审批的工资表才能发放")
        void issuePayroll_shouldFail_whenNotApproved() {
            // Given
            IssuePayrollCommand command = new IssuePayrollCommand();
            command.setPayrollSheetId(TEST_SHEET_ID);

            PayrollSheet sheet = PayrollSheet.builder()
                    .id(TEST_SHEET_ID)
                    .status(PayrollStatus.PENDING_APPROVAL)
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("ADMIN", "FINANCE")).thenReturn(true);

            when(payrollSheetRepository.getByIdOrThrow(eq(TEST_SHEET_ID), anyString())).thenReturn(sheet);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> payrollAppService.issuePayroll(command));
            assertThat(exception.getMessage()).contains("只有已审批通过");
        }
    }
}
