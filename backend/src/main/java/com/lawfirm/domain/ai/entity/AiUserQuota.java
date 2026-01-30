package com.lawfirm.domain.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户AI使用配额实体 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUserQuota {

  /** 主键ID */
  private Long id;

  /** 用户ID */
  private Long userId;

  /** 月度Token配额（NULL表示无限制） */
  private Long monthlyTokenQuota;

  /** 月度费用配额（元，NULL表示无限制） */
  private BigDecimal monthlyCostQuota;

  /** 当月已用Token */
  private Long currentMonthTokens;

  /** 当月已产生费用 */
  private BigDecimal currentMonthCost;

  /** 配额重置日期 */
  private LocalDate quotaResetDate;

  /** 个人自定义承担比例（覆盖默认配置） */
  private Integer customChargeRatio;

  /** 是否免计费（如管理员） */
  private Boolean exemptBilling;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;

  /** 创建人ID */
  private Long createdBy;

  /** 更新人ID */
  private Long updatedBy;

  /** 删除标记 */
  private Boolean deleted;

  /**
   * 检查是否超出Token配额
   *
   * @return 如果超出配额返回true，否则返回false
   */
  public boolean isTokenQuotaExceeded() {
    if (monthlyTokenQuota == null || monthlyTokenQuota <= 0) {
      return false; // 无限制
    }
    return currentMonthTokens != null && currentMonthTokens >= monthlyTokenQuota;
  }

  /**
   * 检查是否超出费用配额
   *
   * @return 如果超出配额返回true，否则返回false
   */
  public boolean isCostQuotaExceeded() {
    if (monthlyCostQuota == null || monthlyCostQuota.compareTo(BigDecimal.ZERO) <= 0) {
      return false; // 无限制
    }
    return currentMonthCost != null && currentMonthCost.compareTo(monthlyCostQuota) >= 0;
  }

  /**
   * 检查是否需要重置配额（新月份）
   *
   * @param today 当前日期
   * @return 如果需要重置返回true，否则返回false
   */
  public boolean needsReset(final LocalDate today) {
    if (quotaResetDate == null) {
      return true;
    }
    LocalDate currentMonthStart = today.withDayOfMonth(1);
    return quotaResetDate.isBefore(currentMonthStart);
  }

  /**
   * 重置月度使用量
   *
   * @param today 当前日期
   */
  public void resetMonthlyUsage(final LocalDate today) {
    this.currentMonthTokens = 0L;
    this.currentMonthCost = BigDecimal.ZERO;
    this.quotaResetDate = today.withDayOfMonth(1);
  }

  /**
   * 累加使用量
   *
   * @param tokens Token数量
   * @param cost 费用
   */
  public void addUsage(final int tokens, final BigDecimal cost) {
    if (this.currentMonthTokens == null) {
      this.currentMonthTokens = 0L;
    }
    if (this.currentMonthCost == null) {
      this.currentMonthCost = BigDecimal.ZERO;
    }
    this.currentMonthTokens += tokens;
    this.currentMonthCost = this.currentMonthCost.add(cost);
  }
}
