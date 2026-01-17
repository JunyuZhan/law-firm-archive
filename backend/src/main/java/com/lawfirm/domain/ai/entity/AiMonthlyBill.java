package com.lawfirm.domain.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI月度账单实体
 * 按月汇总用户AI使用费用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMonthlyBill {

    private Long id;

    // 账单周期
    private Integer billYear;
    private Integer billMonth;

    // 用户信息
    private Long userId;
    private String userName;
    private Long departmentId;
    private String departmentName;

    // 使用统计
    private Integer totalCalls;
    private Long totalTokens;
    private Long promptTokens;
    private Long completionTokens;

    // 费用明细
    private BigDecimal totalCost;      // API总费用
    private BigDecimal userCost;       // 用户应付费用
    private Integer chargeRatio;       // 当月收费比例（快照）

    // 扣减状态
    private String deductionStatus;    // PENDING/DEDUCTED/WAIVED
    private BigDecimal deductionAmount;// 实际扣减金额
    private LocalDateTime deductedAt;
    private Long deductedBy;
    private String deductionRemark;

    // 关联工资记录
    private Long payrollDeductionId;

    /**
     * 获取工资扣减ID（别名）
     */
    public Long getSalaryDeductionId() {
        return payrollDeductionId;
    }

    /**
     * 设置工资扣减ID（别名）
     */
    public void setSalaryDeductionId(Long salaryDeductionId) {
        this.payrollDeductionId = salaryDeductionId;
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Boolean deleted;

    /**
     * 扣减状态枚举
     */
    public static class DeductionStatus {
        public static final String PENDING = "PENDING";
        public static final String DEDUCTED = "DEDUCTED";
        public static final String WAIVED = "WAIVED";
    }

    /**
     * 是否待扣减
     */
    public boolean isPending() {
        return DeductionStatus.PENDING.equals(deductionStatus);
    }

    /**
     * 是否已扣减
     */
    public boolean isDeducted() {
        return DeductionStatus.DEDUCTED.equals(deductionStatus);
    }

    /**
     * 是否已减免
     */
    public boolean isWaived() {
        return DeductionStatus.WAIVED.equals(deductionStatus);
    }

    /**
     * 获取账单周期描述
     */
    public String getPeriodDesc() {
        return String.format("%d年%d月", billYear, billMonth);
    }
}
