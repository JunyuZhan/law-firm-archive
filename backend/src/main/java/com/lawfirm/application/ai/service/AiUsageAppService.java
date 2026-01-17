package com.lawfirm.application.ai.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * AI使用量应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageAppService {

    private final AiUsageLogRepository usageLogRepository;
    private final AiUserQuotaRepository quotaRepository;
    private final UserRepository userRepository;

    /**
     * 查询我的使用记录
     */
    public PageResult<AiUsageLogDTO> getMyUsageLogs(AiUsageQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        query.setUserId(userId);
        return usageLogRepository.queryPage(query);
    }

    /**
     * 获取我的使用统计
     */
    public AiUsageSummaryDTO getMyUsageSummary(YearMonth month) {
        Long userId = SecurityUtils.getUserId();
        return getUsageSummary(userId, month);
    }

    /**
     * 获取用户使用统计
     */
    public AiUsageSummaryDTO getUsageSummary(Long userId, YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // 查询统计数据
        Map<String, Object> stats = usageLogRepository.getSummary(userId, startDate, endDate);

        // 查询配额信息
        AiUserQuota quota = quotaRepository.findByUserId(userId);

        // 获取用户信息
        User user = userRepository.findById(userId);

        // 处理空统计数据情况
        int totalCalls = 0;
        long totalTokens = 0L;
        long promptTokens = 0L;
        long completionTokens = 0L;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal userCost = BigDecimal.ZERO;
        BigDecimal companyCost = BigDecimal.ZERO;
        int chargeRatio = 100;

        if (stats != null) {
            totalCalls = stats.get("total_calls") != null ? ((Number) stats.get("total_calls")).intValue() : 0;
            totalTokens = stats.get("total_tokens") != null ? ((Number) stats.get("total_tokens")).longValue() : 0L;
            promptTokens = stats.get("prompt_tokens") != null ? ((Number) stats.get("prompt_tokens")).longValue() : 0L;
            completionTokens = stats.get("completion_tokens") != null ? ((Number) stats.get("completion_tokens")).longValue() : 0L;
            totalCost = stats.get("total_cost") != null ? (BigDecimal) stats.get("total_cost") : BigDecimal.ZERO;
            userCost = stats.get("user_cost") != null ? (BigDecimal) stats.get("user_cost") : BigDecimal.ZERO;
            companyCost = stats.get("company_cost") != null ? (BigDecimal) stats.get("company_cost") : BigDecimal.ZERO;
            chargeRatio = stats.get("charge_ratio") != null ? ((Number) stats.get("charge_ratio")).intValue() : 100;
        }

        return AiUsageSummaryDTO.builder()
                .userId(userId)
                .userName(user != null ? user.getRealName() : null)
                .departmentId(user != null ? user.getDepartmentId() : null)
                .month(month.toString())
                .totalCalls(totalCalls)
                .totalTokens(totalTokens)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalCost(totalCost)
                .userCost(userCost)
                .companyCost(companyCost)
                .chargeRatio(chargeRatio)
                // 配额信息
                .monthlyTokenQuota(quota != null ? quota.getMonthlyTokenQuota() : null)
                .monthlyCostQuota(quota != null ? quota.getMonthlyCostQuota() : null)
                .tokenUsagePercent(calculateTokenUsagePercent(totalTokens, quota != null ? quota.getMonthlyTokenQuota() : null))
                .costUsagePercent(calculateCostUsagePercent(userCost, quota != null ? quota.getMonthlyCostQuota() : null))
                .avgTokensPerCall(calculateAvgTokensPerCall(totalTokens, totalCalls))
                .avgCostPerCall(calculateAvgCostPerCall(userCost, totalCalls))
                .build();
    }

    /**
     * 获取按模型分组的使用统计
     */
    public List<Map<String, Object>> getUsageByModel(Long userId, YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return usageLogRepository.getUsageByModel(userId, startDate, endDate);
    }

    /**
     * 获取使用趋势（按日统计）
     */
    public List<Map<String, Object>> getUsageTrend(Long userId, YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return usageLogRepository.getUsageTrend(userId, startDate, endDate);
    }

    /**
     * 管理员-获取全员统计
     */
    public List<AiUsageSummaryDTO> getAllUsersSummary(YearMonth month) {
        // 检查权限：只有管理员、财务、主任可以查看
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")) {
            throw new BusinessException("无权查看全员AI使用统计");
        }

        if (month == null) {
            month = YearMonth.now();
        }

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return usageLogRepository.getAllUsersSummary(startDate, endDate);
    }

    /**
     * 管理员-获取部门统计
     */
    public List<Map<String, Object>> getDepartmentSummary(YearMonth month) {
        // 检查权限
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasAnyRole("FINANCE", "DIRECTOR")) {
            throw new BusinessException("无权查看部门AI使用统计");
        }

        if (month == null) {
            month = YearMonth.now();
        }

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return usageLogRepository.getDepartmentSummary(startDate, endDate);
    }

    /**
     * 检查用户是否超配额
     */
    public boolean checkQuota(Long userId) {
        AiUserQuota quota = quotaRepository.findByUserId(userId);
        if (quota == null) {
            return true; // 无配额限制
        }

        // 检查Token配额
        if (quota.getMonthlyTokenQuota() != null &&
                quota.getCurrentMonthTokens() >= quota.getMonthlyTokenQuota()) {
            return false;
        }

        // 检查费用配额
        if (quota.getMonthlyCostQuota() != null &&
                quota.getCurrentMonthCost().compareTo(quota.getMonthlyCostQuota()) >= 0) {
            return false;
        }

        return true;
    }

    /**
     * 获取用户当前配额使用情况
     */
    public AiUserQuota getUserQuota(Long userId) {
        AiUserQuota quota = quotaRepository.findByUserId(userId);
        if (quota == null) {
            // 返回默认配额
            quota = new AiUserQuota();
            quota.setUserId(userId);
        }
        return quota;
    }

    // ========== 私有方法 ==========

    /**
     * 计算Token使用百分比
     */
    private Double calculateTokenUsagePercent(Long used, Long quota) {
        if (quota == null || quota == 0) {
            return null;
        }
        return used * 100.0 / quota;
    }

    /**
     * 计算费用使用百分比
     */
    private Double calculateCostUsagePercent(BigDecimal used, BigDecimal quota) {
        if (quota == null || quota.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return used.multiply(BigDecimal.valueOf(100))
                .divide(quota, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 计算平均每次调用Token数
     */
    private Double calculateAvgTokensPerCall(Long totalTokens, Integer totalCalls) {
        if (totalCalls == null || totalCalls == 0) {
            return 0.0;
        }
        return totalTokens * 1.0 / totalCalls;
    }

    /**
     * 计算平均每次调用费用
     */
    private BigDecimal calculateAvgCostPerCall(BigDecimal totalCost, Integer totalCalls) {
        if (totalCalls == null || totalCalls == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(BigDecimal.valueOf(totalCalls), 4, RoundingMode.HALF_UP);
    }
}
