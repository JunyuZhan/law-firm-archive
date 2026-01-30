package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建付款计划命令 */
@Data
public class CreatePaymentScheduleCommand {

  /** 合同ID */
  @NotNull(message = "合同ID不能为空")
  private Long contractId;

  /** 阶段名称 */
  @NotBlank(message = "阶段名称不能为空")
  private String phaseName;

  /** 付款金额 */
  @NotNull(message = "付款金额不能为空")
  private BigDecimal amount;

  /** 比例（风险代理时使用） */
  private BigDecimal percentage;

  /** 计划收款日期 */
  private LocalDate plannedDate;

  /** 备注 */
  private String remark;
}
