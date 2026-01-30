package com.lawfirm.application.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.lawfirm.application.ai.dto.AiUsageLogDTO;
import com.lawfirm.application.ai.dto.AiUsageQueryDTO;
import com.lawfirm.application.ai.dto.AiUsageSummaryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.ai.entity.AiUserQuota;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import com.lawfirm.domain.ai.repository.AiUserQuotaRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** AiUsageAppService 单元测试 测试AI使用量服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiUsageAppService AI使用量服务测试")
class AiUsageAppServiceTest {

  private static final Long TEST_USER_ID = 1L;

  @Mock private AiUsageLogRepository usageLogRepository;

  @Mock private AiUserQuotaRepository quotaRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private AiUsageAppService aiUsageAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
    securityUtilsMock
        .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
        .thenReturn(false);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("查询使用记录测试")
  class QueryUsageTests {

    @Test
    @DisplayName("应该成功查询我的使用记录")
    void getMyUsageLogs_shouldSuccess() {
      // Given
      AiUsageQueryDTO query = new AiUsageQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      PageResult<AiUsageLogDTO> expectedResult = PageResult.of(List.of(), 0L, 1, 10);
      when(usageLogRepository.queryPage(any(AiUsageQueryDTO.class))).thenReturn(expectedResult);

      // When
      PageResult<AiUsageLogDTO> result = aiUsageAppService.getMyUsageLogs(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(query.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("应该成功获取我的使用统计")
    void getMyUsageSummary_shouldSuccess() {
      // Given
      YearMonth month = YearMonth.of(2024, 1);
      Map<String, Object> stats =
          Map.of(
              "total_calls",
              100,
              "total_tokens",
              50000L,
              "prompt_tokens",
              30000L,
              "completion_tokens",
              20000L,
              "total_cost",
              new BigDecimal("10.00"),
              "user_cost",
              new BigDecimal("5.00"),
              "company_cost",
              new BigDecimal("5.00"),
              "charge_ratio",
              50);

      AiUserQuota quota =
          AiUserQuota.builder()
              .userId(TEST_USER_ID)
              .monthlyTokenQuota(100000L)
              .monthlyCostQuota(new BigDecimal("50.00"))
              .build();

      User user = User.builder().id(TEST_USER_ID).realName("测试用户").departmentId(1L).build();

      when(usageLogRepository.getSummary(anyLong(), any(), any())).thenReturn(stats);
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);
      when(userRepository.findById(TEST_USER_ID)).thenReturn(user);

      // When
      AiUsageSummaryDTO result = aiUsageAppService.getMyUsageSummary(month);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
      assertThat(result.getTotalCalls()).isEqualTo(100);
      assertThat(result.getTotalTokens()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("应该成功获取用户使用统计")
    void getUsageSummary_shouldSuccess() {
      // Given
      YearMonth month = YearMonth.of(2024, 1);
      Map<String, Object> stats =
          Map.of(
              "total_calls",
              50,
              "total_tokens",
              25000L,
              "prompt_tokens",
              15000L,
              "completion_tokens",
              10000L,
              "total_cost",
              new BigDecimal("5.00"),
              "user_cost",
              new BigDecimal("2.50"),
              "company_cost",
              new BigDecimal("2.50"),
              "charge_ratio",
              50);

      when(usageLogRepository.getSummary(anyLong(), any(), any())).thenReturn(stats);
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);
      when(userRepository.findById(TEST_USER_ID))
          .thenReturn(User.builder().id(TEST_USER_ID).realName("用户").build());

      // When
      AiUsageSummaryDTO result = aiUsageAppService.getUsageSummary(TEST_USER_ID, month);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTotalCalls()).isEqualTo(50);
    }

    @Test
    @DisplayName("应该按模型分组统计使用量")
    void getUsageByModel_shouldSuccess() {
      // Given
      YearMonth month = YearMonth.of(2024, 1);
      when(usageLogRepository.getUsageByModel(anyLong(), any(), any()))
          .thenReturn(List.of(Map.of("model", "gpt-4", "tokens", 10000L)));

      // When
      List<Map<String, Object>> result = aiUsageAppService.getUsageByModel(TEST_USER_ID, month);

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应该获取使用趋势")
    void getUsageTrend_shouldSuccess() {
      // Given
      YearMonth month = YearMonth.of(2024, 1);
      when(usageLogRepository.getUsageTrend(anyLong(), any(), any()))
          .thenReturn(List.of(Map.of("date", "2024-01-01", "tokens", 1000L)));

      // When
      List<Map<String, Object>> result = aiUsageAppService.getUsageTrend(TEST_USER_ID, month);

      // Then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("管理员查询测试")
  class AdminQueryTests {

    @Test
    @DisplayName("管理员应该成功获取全员统计")
    void getAllUsersSummary_shouldSuccess_forAdmin() {
      // Given
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

      when(usageLogRepository.getAllUsersSummary(any(), any()))
          .thenReturn(List.of(new AiUsageSummaryDTO()));

      // When
      List<AiUsageSummaryDTO> result = aiUsageAppService.getAllUsersSummary(YearMonth.now());

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("非管理员不能获取全员统计")
    void getAllUsersSummary_shouldFail_forNonAdmin() {
      // Given
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(false);

      // When & Then
      assertThrows(
          BusinessException.class, () -> aiUsageAppService.getAllUsersSummary(YearMonth.now()));
    }

    @Test
    @DisplayName("财务人员可以获取全员统计")
    void getAllUsersSummary_shouldSuccess_forFinance() {
      // Given
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR"))
          .thenReturn(true);

      when(usageLogRepository.getAllUsersSummary(any(), any())).thenReturn(List.of());

      // When
      List<AiUsageSummaryDTO> result = aiUsageAppService.getAllUsersSummary(YearMonth.now());

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("管理员应该成功获取部门统计")
    void getDepartmentSummary_shouldSuccess_forAdmin() {
      // Given
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);

      when(usageLogRepository.getDepartmentSummary(any(), any()))
          .thenReturn(List.of(Map.of("department", "技术部", "tokens", 50000L)));

      // When
      List<Map<String, Object>> result = aiUsageAppService.getDepartmentSummary(YearMonth.now());

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("非管理员不能获取部门统计")
    void getDepartmentSummary_shouldFail_forNonAdmin() {
      // Given
      securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
      securityUtilsMock
          .when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
          .thenReturn(false);

      // When & Then
      assertThrows(
          BusinessException.class, () -> aiUsageAppService.getDepartmentSummary(YearMonth.now()));
    }
  }

  @Nested
  @DisplayName("配额检查测试")
  class QuotaTests {

    @Test
    @DisplayName("应该成功检查用户配额")
    void checkQuota_shouldReturnTrue_whenWithinQuota() {
      // Given
      AiUserQuota quota =
          AiUserQuota.builder()
              .userId(TEST_USER_ID)
              .monthlyTokenQuota(100000L)
              .currentMonthTokens(50000L)
              .monthlyCostQuota(new BigDecimal("50.00"))
              .currentMonthCost(new BigDecimal("20.00"))
              .build();

      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);

      // When
      boolean result = aiUsageAppService.checkQuota(TEST_USER_ID);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Token超配额时应该返回false")
    void checkQuota_shouldReturnFalse_whenTokenExceeded() {
      // Given
      AiUserQuota quota =
          AiUserQuota.builder()
              .userId(TEST_USER_ID)
              .monthlyTokenQuota(100000L)
              .currentMonthTokens(100000L) // 已达上限
              .build();

      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);

      // When
      boolean result = aiUsageAppService.checkQuota(TEST_USER_ID);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("费用超配额时应该返回false")
    void checkQuota_shouldReturnFalse_whenCostExceeded() {
      // Given
      AiUserQuota quota =
          AiUserQuota.builder()
              .userId(TEST_USER_ID)
              .monthlyCostQuota(new BigDecimal("50.00"))
              .currentMonthCost(new BigDecimal("50.00")) // 已达上限
              .build();

      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);

      // When
      boolean result = aiUsageAppService.checkQuota(TEST_USER_ID);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("无配额限制时应返回true")
    void checkQuota_shouldReturnTrue_whenNoQuota() {
      // Given
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);

      // When
      boolean result = aiUsageAppService.checkQuota(TEST_USER_ID);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("应该成功获取用户配额")
    void getUserQuota_shouldSuccess() {
      // Given
      AiUserQuota quota =
          AiUserQuota.builder().userId(TEST_USER_ID).monthlyTokenQuota(100000L).build();

      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);

      // When
      AiUserQuota result = aiUsageAppService.getUserQuota(TEST_USER_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("用户无配额时应返回默认配额")
    void getUserQuota_shouldReturnDefault_whenNoQuota() {
      // Given
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);

      // When
      AiUserQuota result = aiUsageAppService.getUserQuota(TEST_USER_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
    }
  }

  @Nested
  @DisplayName("计算方法测试")
  class CalculationTests {

    @Test
    @DisplayName("应正确计算平均每次调用Token数")
    void calculateAvgTokensPerCall_shouldCalculateCorrectly() {
      // Given
      Map<String, Object> stats = Map.of("total_calls", 10, "total_tokens", 50000L);

      when(usageLogRepository.getSummary(anyLong(), any(), any())).thenReturn(stats);
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);
      when(userRepository.findById(TEST_USER_ID))
          .thenReturn(User.builder().id(TEST_USER_ID).build());

      // When
      AiUsageSummaryDTO result = aiUsageAppService.getUsageSummary(TEST_USER_ID, YearMonth.now());

      // Then
      assertThat(result.getAvgTokensPerCall()).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("应正确计算平均每次调用费用")
    void calculateAvgCostPerCall_shouldCalculateCorrectly() {
      // Given
      Map<String, Object> stats =
          Map.of(
              "total_calls", 10,
              "total_cost", new BigDecimal("10.00"),
              "user_cost", new BigDecimal("5.00"));

      when(usageLogRepository.getSummary(anyLong(), any(), any())).thenReturn(stats);
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(null);
      when(userRepository.findById(TEST_USER_ID))
          .thenReturn(User.builder().id(TEST_USER_ID).build());

      // When
      AiUsageSummaryDTO result = aiUsageAppService.getUsageSummary(TEST_USER_ID, YearMonth.now());

      // Then
      assertThat(result.getAvgCostPerCall()).isEqualTo(new BigDecimal("0.5000"));
    }

    @Test
    @DisplayName("应正确计算Token使用百分比")
    void calculateTokenUsagePercent_shouldCalculateCorrectly() {
      // Given
      Map<String, Object> stats = Map.of("total_tokens", 50000L);

      AiUserQuota quota =
          AiUserQuota.builder().userId(TEST_USER_ID).monthlyTokenQuota(100000L).build();

      when(usageLogRepository.getSummary(anyLong(), any(), any())).thenReturn(stats);
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);
      when(userRepository.findById(TEST_USER_ID))
          .thenReturn(User.builder().id(TEST_USER_ID).build());

      // When
      AiUsageSummaryDTO result = aiUsageAppService.getUsageSummary(TEST_USER_ID, YearMonth.now());

      // Then
      assertThat(result.getTokenUsagePercent()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("应正确计算费用使用百分比")
    void calculateCostUsagePercent_shouldCalculateCorrectly() {
      // Given
      Map<String, Object> stats = Map.of("user_cost", new BigDecimal("25.00"));

      AiUserQuota quota =
          AiUserQuota.builder()
              .userId(TEST_USER_ID)
              .monthlyCostQuota(new BigDecimal("50.00"))
              .build();

      when(usageLogRepository.getSummary(anyLong(), any(), any())).thenReturn(stats);
      when(quotaRepository.findByUserId(TEST_USER_ID)).thenReturn(quota);
      when(userRepository.findById(TEST_USER_ID))
          .thenReturn(User.builder().id(TEST_USER_ID).build());

      // When
      AiUsageSummaryDTO result = aiUsageAppService.getUsageSummary(TEST_USER_ID, YearMonth.now());

      // Then
      assertThat(result.getCostUsagePercent()).isEqualTo(50.0);
    }
  }
}
