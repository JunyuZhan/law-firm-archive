package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新付款计划命令. */
@Data
public class UpdatePaymentScheduleCommand {

  /** 付款计划ID. */
  @NotNull(message = "付款计划ID不能为空")
  private Long id;

  /** 阶段名称. */
  private String phaseName;

  /** 付款金额. */
  private BigDecimal amount;

  /** 比例（风险代理时使用）. */
  private BigDecimal percentage;

  /** 计划收款日期. */
  private LocalDate plannedDate;

  /** 实际收款日期. */
  private LocalDate actualDate;

  /** 状态. */
  private String status;

  /** 备注. */
  private String remark;
}
