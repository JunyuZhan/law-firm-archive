package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新工资明细命令
 */
@Data
public class UpdatePayrollItemCommand {

    @NotNull(message = "工资明细ID不能为空")
    private Long payrollItemId;

    /**
     * 应发工资（可手动设置，考虑预支、欠款等因素）
     */
    private BigDecimal grossAmount;

    /**
     * 确认截止时间（超过此时间未确认将自动确认）
     * 如果为空，使用默认值（每月27日24时）
     */
    private LocalDateTime confirmDeadline;

    /**
     * 收入项列表
     */
    private List<PayrollIncomeItem> incomes;

    /**
     * 扣减项列表
     */
    private List<PayrollDeductionItem> deductions;

    /**
     * 备注
     */
    private String remark;

    /**
     * 收入项
     */
    @Data
    public static class PayrollIncomeItem {
        private Long id; // 如果存在则更新，否则新增
        private String incomeType;
        private BigDecimal amount;
        private String remark;
        private String sourceType;
        private Long sourceId;
    }

    /**
     * 扣减项
     */
    @Data
    public static class PayrollDeductionItem {
        private Long id; // 如果存在则更新，否则新增
        private String deductionType;
        private BigDecimal amount;
        private String remark;
        private String sourceType;
    }
}

