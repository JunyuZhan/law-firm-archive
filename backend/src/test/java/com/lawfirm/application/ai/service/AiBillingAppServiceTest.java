package com.lawfirm.application.ai.service;

import com.lawfirm.application.ai.command.SalaryDeductionLinkCommand;
import com.lawfirm.application.ai.dto.AiMonthlyBillDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.ai.entity.AiMonthlyBill;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiMonthlyBillRepository;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AiBillingAppService 单元测试
 * 测试AI账单服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiBillingAppService AI账单服务测试")
class AiBillingAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_BILL_ID = 100L;

    @Mock
    private AiMonthlyBillRepository billRepository;

    @Mock
    private AiUsageLogRepository usageLogRepository;

    @Mock
    private AiUserQuotaRepository quotaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AiBillingAppService aiBillingAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
        securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("查询账单测试")
    class QueryBillTests {

        @Test
        @DisplayName("管理员应该成功获取月度账单列表")
        void getMonthlyBills_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .billYear(2024)
                    .billMonth(1)
                    .userId(TEST_USER_ID)
                    .userName("测试用户")
                    .totalCalls(100)
                    .totalTokens(50000L)
                    .totalCost(new BigDecimal("10.00"))
                    .userCost(new BigDecimal("5.00"))
                    .deductionStatus("PENDING")
                    .build();

            when(billRepository.findByPeriod(anyInt(), anyInt())).thenReturn(List.of(bill));
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).realName("测试用户").build());

            // When
            List<AiMonthlyBillDTO> result = aiBillingAppService.getMonthlyBills(2024, 1);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBillYear()).isEqualTo(2024);
        }

        @Test
        @DisplayName("非管理员不能获取月度账单列表")
        void getMonthlyBills_shouldFail_forNonAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any())).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> aiBillingAppService.getMonthlyBills(2024, 1));
        }

        @Test
        @DisplayName("财务人员可以获取月度账单列表")
        void getMonthlyBills_shouldSuccess_forFinance() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")).thenReturn(true);

            when(billRepository.findByPeriod(anyInt(), anyInt())).thenReturn(List.of());
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).build());

            // When
            List<AiMonthlyBillDTO> result = aiBillingAppService.getMonthlyBills(2024, 1);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("应该成功获取用户自己的账单")
        void getUserBill_shouldSuccess_forOwnBill() {
            // Given
            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .billYear(2024)
                    .billMonth(1)
                    .userId(TEST_USER_ID)
                    .userName("测试用户")
                    .build();

            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(bill);
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).build());

            // When
            AiMonthlyBillDTO result = aiBillingAppService.getUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("账单不存在时应返回null")
        void getUserBill_shouldReturnNull_whenNotFound() {
            // Given
            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(null);

            // When
            AiMonthlyBillDTO result = aiBillingAppService.getUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("用户不能查看他人账单")
        void getUserBill_shouldFail_forOtherUser() {
            // Given
            Long otherUserId = 999L;
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any())).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> aiBillingAppService.getUserBill(otherUserId, 2024, 1));
        }

        @Test
        @DisplayName("管理员可以查看任何人的账单")
        void getUserBill_shouldSuccess_forAdmin() {
            // Given
            Long otherUserId = 999L;
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .userId(otherUserId)
                    .build();

            when(billRepository.findByUserAndPeriod(otherUserId, 2024, 1)).thenReturn(bill);
            when(userRepository.findById(any())).thenReturn(User.builder().id(otherUserId).build());

            // When
            AiMonthlyBillDTO result = aiBillingAppService.getUserBill(otherUserId, 2024, 1);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("生成账单测试")
    class GenerateBillTests {

        @Test
        @DisplayName("管理员应该成功生成月度账单")
        void generateMonthlyBills_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            when(usageLogRepository.getActiveUsersInPeriod(any(), any()))
                    .thenReturn(List.of(Map.of("user_id", TEST_USER_ID)));

            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(User.builder().id(TEST_USER_ID).realName("测试用户").departmentId(1L).build());

            when(usageLogRepository.getSummary(anyLong(), any(), any()))
                    .thenReturn(Map.of(
                            "total_calls", 10,
                            "total_tokens", 5000L,
                            "prompt_tokens", 3000L,
                            "completion_tokens", 2000L,
                            "total_cost", new BigDecimal("10.00")));

            when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);
            when(billRepository.findByUserAndPeriod(anyLong(), anyInt(), anyInt())).thenReturn(null);
            doAnswer(invocation -> {
                AiMonthlyBill bill = invocation.getArgument(0);
                bill.setId(TEST_BILL_ID);
                return null;
            }).when(billRepository).save(any(AiMonthlyBill.class));

            // When
            int result = aiBillingAppService.generateMonthlyBills(2024, 1);

            // Then
            assertThat(result).isEqualTo(1);
            verify(billRepository, atLeastOnce()).save(any(AiMonthlyBill.class));
        }

        @Test
        @DisplayName("非管理员不能生成月度账单")
        void generateMonthlyBills_shouldFail_forNonAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any())).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> aiBillingAppService.generateMonthlyBills(2024, 1));
        }

        @Test
        @DisplayName("财务人员可以生成月度账单")
        void generateMonthlyBills_shouldSuccess_forFinance() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("FINANCE")).thenReturn(true);

            when(usageLogRepository.getActiveUsersInPeriod(any(), any())).thenReturn(List.of());
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).build());

            // When
            int result = aiBillingAppService.generateMonthlyBills(2024, 1);

            // Then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("应该成功生成单个用户账单")
        void generateUserBill_shouldSuccess() {
            // Given
            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(null);

            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(User.builder().id(TEST_USER_ID).realName("测试用户").departmentId(1L).build());

            when(usageLogRepository.getSummary(anyLong(), any(), any()))
                    .thenReturn(Map.of(
                            "total_calls", 10,
                            "total_tokens", 5000L,
                            "prompt_tokens", 3000L,
                            "completion_tokens", 2000L,
                            "total_cost", new BigDecimal("10.00")));

            when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);
            doAnswer(invocation -> {
                AiMonthlyBill bill = invocation.getArgument(0);
                bill.setId(TEST_BILL_ID);
                return null;
            }).when(billRepository).save(any(AiMonthlyBill.class));

            // When
            AiMonthlyBillDTO result = aiBillingAppService.generateUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalCalls()).isEqualTo(10);
        }

        @Test
        @DisplayName("账单已存在时应返回已有账单")
        void generateUserBill_shouldReturnExisting_whenExists() {
            // Given
            AiMonthlyBill existingBill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .billYear(2024)
                    .billMonth(1)
                    .userId(TEST_USER_ID)
                    .build();

            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(existingBill);

            // When
            AiMonthlyBillDTO result = aiBillingAppService.generateUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_BILL_ID);
            verify(billRepository, never()).save(any());
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void generateUserBill_shouldFail_whenUserNotFound() {
            // Given
            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(null);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(null);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> aiBillingAppService.generateUserBill(TEST_USER_ID, 2024, 1));
        }
    }

    @Nested
    @DisplayName("账单操作测试")
    class BillOperationTests {

        @Test
        @DisplayName("管理员应该成功标记账单已扣减")
        void markDeducted_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("PENDING")
                    .userCost(new BigDecimal("10.00"))
                    .build();

            when(billRepository.getByIdOrThrow(eq(TEST_BILL_ID), anyString())).thenReturn(bill);
            doNothing().when(billRepository).updateById(any(AiMonthlyBill.class));

            // When
            aiBillingAppService.markDeducted(TEST_BILL_ID, "已扣款");

            // Then
            assertThat(bill.getDeductionStatus()).isEqualTo("DEDUCTED");
            assertThat(bill.getDeductionAmount()).isEqualTo(new BigDecimal("10.00"));
            verify(billRepository).updateById(bill);
        }

        @Test
        @DisplayName("非管理员不能标记账单已扣减")
        void markDeducted_shouldFail_forNonAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(any())).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> aiBillingAppService.markDeducted(TEST_BILL_ID, "备注"));
        }

        @Test
        @DisplayName("已处理的账单不能重复操作")
        void markDeducted_shouldFail_whenAlreadyProcessed() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("DEDUCTED") // 已处理
                    .build();

            when(billRepository.getByIdOrThrow(eq(TEST_BILL_ID), anyString())).thenReturn(bill);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> aiBillingAppService.markDeducted(TEST_BILL_ID, "备注"));
        }

        @Test
        @DisplayName("管理员应该成功减免账单")
        void waiveBill_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("PENDING")
                    .userCost(new BigDecimal("10.00"))
                    .build();

            when(billRepository.getByIdOrThrow(eq(TEST_BILL_ID), anyString())).thenReturn(bill);
            doNothing().when(billRepository).updateById(any(AiMonthlyBill.class));

            // When
            aiBillingAppService.waiveBill(TEST_BILL_ID, "费用减免");

            // Then
            assertThat(bill.getDeductionStatus()).isEqualTo("WAIVED");
            assertThat(bill.getDeductionAmount()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("主任可以减免账单")
        void waiveBill_shouldSuccess_forDirector() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("PENDING")
                    .build();

            when(billRepository.getByIdOrThrow(eq(TEST_BILL_ID), anyString())).thenReturn(bill);
            doNothing().when(billRepository).updateById(any(AiMonthlyBill.class));

            // When
            aiBillingAppService.waiveBill(TEST_BILL_ID, "原因");

            // Then
            assertThat(bill.getDeductionStatus()).isEqualTo("WAIVED");
        }

        @Test
        @DisplayName("普通财务不能减免账单")
        void waiveBill_shouldFail_forFinanceOnly() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("FINANCE")).thenReturn(true);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> aiBillingAppService.waiveBill(TEST_BILL_ID, "原因"));
        }
    }

    @Nested
    @DisplayName("关联工资扣减测试")
    class LinkSalaryTests {

        @Test
        @DisplayName("管理员应该成功关联工资扣减")
        void linkToSalaryDeduction_shouldSuccess_forAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("PENDING")
                    .userCost(new BigDecimal("10.00"))
                    .build();

            when(billRepository.getByIdOrThrow(eq(TEST_BILL_ID), anyString())).thenReturn(bill);
            doNothing().when(billRepository).updateById(any(AiMonthlyBill.class));

            SalaryDeductionLinkCommand command = new SalaryDeductionLinkCommand();
            command.setSalarySheetId(500L);
            command.setBillIds(List.of(TEST_BILL_ID));
            command.setRemark("关联工资");

            // When
            aiBillingAppService.linkToSalaryDeduction(command);

            // Then
            assertThat(bill.getSalaryDeductionId()).isEqualTo(500L);
            assertThat(bill.getDeductionStatus()).isEqualTo("DEDUCTED");
        }

        @Test
        @DisplayName("财务人员可以关联工资扣减")
        void linkToSalaryDeduction_shouldSuccess_forFinance() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole("FINANCE")).thenReturn(true);

            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("PENDING")
                    .build();

            when(billRepository.getByIdOrThrow(eq(TEST_BILL_ID), anyString())).thenReturn(bill);
            doNothing().when(billRepository).updateById(any(AiMonthlyBill.class));

            SalaryDeductionLinkCommand command = new SalaryDeductionLinkCommand();
            command.setSalarySheetId(500L);
            command.setBillIds(List.of(TEST_BILL_ID));

            // When
            aiBillingAppService.linkToSalaryDeduction(command);

            // Then
            assertThat(bill.getSalaryDeductionId()).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("状态映射测试")
    class StatusMappingTests {

        @Test
        @DisplayName("应该正确映射待扣减状态")
        void deductionStatus_shouldMapPending() {
            // Given
            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("PENDING")
                    .build();

            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(bill);
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).build());

            // When
            AiMonthlyBillDTO result = aiBillingAppService.getUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result.getDeductionStatusName()).isEqualTo("待扣减");
        }

        @Test
        @DisplayName("应该正确映射已扣减状态")
        void deductionStatus_shouldMapDeducted() {
            // Given
            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("DEDUCTED")
                    .build();

            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(bill);
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).build());

            // When
            AiMonthlyBillDTO result = aiBillingAppService.getUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result.getDeductionStatusName()).isEqualTo("已扣减");
        }

        @Test
        @DisplayName("应该正确映射已减免状态")
        void deductionStatus_shouldMapWaived() {
            // Given
            AiMonthlyBill bill = AiMonthlyBill.builder()
                    .id(TEST_BILL_ID)
                    .deductionStatus("WAIVED")
                    .build();

            when(billRepository.findByUserAndPeriod(TEST_USER_ID, 2024, 1)).thenReturn(bill);
            when(userRepository.findById(any())).thenReturn(User.builder().id(TEST_USER_ID).build());

            // When
            AiMonthlyBillDTO result = aiBillingAppService.getUserBill(TEST_USER_ID, 2024, 1);

            // Then
            assertThat(result.getDeductionStatusName()).isEqualTo("已减免");
        }
    }
}
