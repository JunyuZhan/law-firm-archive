package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 更新工资明细命令. */
@Data
public class UpdatePayrollItemCommand {

  /** 工资明细ID. */
  @NotNull(message = "工资明细ID不能为空")
  private Long payrollItemId;

  /** 应发工资（可手动设置，考虑预支、欠款等因素）. */
  private BigDecimal grossAmount;

  /** 确认截止时间（超过此时间未确认将自动确认） 如果为空，使用默认值（每月27日24时）. */
  private LocalDateTime confirmDeadline;

  /** 收入项列表. */
  private List<PayrollIncomeItem> incomes;

  /** 扣减项列表. */
  private List<PayrollDeductionItem> deductions;

  /** 备注. */
  private String remark;

  /** 收入项. */
  @Data
  public static class PayrollIncomeItem {
    /** ID（如果存在则更新，否则新增）. */
    private Long id;

    /** 收入类型. */
    private String incomeType;

    /** 金额. */
    private BigDecimal amount;

    /** 备注. */
    private String remark;

    /** 来源类型. */
    private String sourceType;

    /** 来源ID. */
    private Long sourceId;
  }

  /** 扣减项. */
  @Data
  public static class PayrollDeductionItem {
    /** ID（如果存在则更新，否则新增）. */
    private Long id;

    /** 扣减类型. */
    private String deductionType;

    /** 金额. */
    private BigDecimal amount;

    /** 备注. */
    private String remark;

    /** 来源类型. */
    private String sourceType;
  }
}
