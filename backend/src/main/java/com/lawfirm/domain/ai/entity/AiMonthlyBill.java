package com.lawfirm.domain.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI月度账单实体 按月汇总用户AI使用费用 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMonthlyBill {

  /** 主键ID */
  private Long id;

  // 账单周期
  /** 账单年份 */
  private Integer billYear;

  /** 账单月份 */
  private Integer billMonth;

  // 用户信息
  /** 用户ID */
  private Long userId;

  /** 用户姓名 */
  private String userName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  // 使用统计
  /** 总调用次数 */
  private Integer totalCalls;

  /** 总Token数 */
  private Long totalTokens;

  /** 输入Token数 */
  private Long promptTokens;

  /** 输出Token数 */
  private Long completionTokens;

  // 费用明细
  /** API总费用 */
  private BigDecimal totalCost; // API总费用

  /** 用户应付费用 */
  private BigDecimal userCost; // 用户应付费用

  /** 当月收费比例（快照） */
  private Integer chargeRatio; // 当月收费比例（快照）

  // 扣减状态
  /** 扣减状态：PENDING/DEDUCTED/WAIVED */
  private String deductionStatus; // PENDING/DEDUCTED/WAIVED

  /** 实际扣减金额 */
  private BigDecimal deductionAmount; // 实际扣减金额

  /** 扣减时间 */
  private LocalDateTime deductedAt;

  /** 扣减人ID */
  private Long deductedBy;

  /** 扣减备注 */
  private String deductionRemark;

  // 关联工资记录
  /** 工资扣减记录ID */
  private Long payrollDeductionId;

  /**
   * 获取工资扣减ID（别名）。
   *
   * @return 工资扣减ID
   */
  public Long getSalaryDeductionId() {
    return payrollDeductionId;
  }

  /**
   * 设置工资扣减ID（别名）。
   *
   * @param salaryDeductionId 工资扣减ID
   */
  public void setSalaryDeductionId(final Long salaryDeductionId) {
    this.payrollDeductionId = salaryDeductionId;
  }

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

  /** 扣减状态枚举 */
  public static class DeductionStatus {
    /** 待扣减状态 */
    public static final String PENDING = "PENDING";

    /** 已扣减状态 */
    public static final String DEDUCTED = "DEDUCTED";

    /** 已减免状态 */
    public static final String WAIVED = "WAIVED";
  }

  /**
   * 是否待扣减。
   *
   * @return 如果状态为待扣减返回true，否则返回false
   */
  public boolean isPending() {
    return DeductionStatus.PENDING.equals(deductionStatus);
  }

  /**
   * 是否已扣减。
   *
   * @return 如果状态为已扣减返回true，否则返回false
   */
  public boolean isDeducted() {
    return DeductionStatus.DEDUCTED.equals(deductionStatus);
  }

  /**
   * 是否已减免。
   *
   * @return 如果状态为已减免返回true，否则返回false
   */
  public boolean isWaived() {
    return DeductionStatus.WAIVED.equals(deductionStatus);
  }

  /**
   * 获取账单周期描述。
   *
   * @return 账单周期描述（格式：YYYY年MM月）
   */
  public String getPeriodDesc() {
    return String.format("%d年%d月", billYear, billMonth);
  }
}
