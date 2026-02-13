package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建收费记录命令 */
@Data
public class CreateFeeCommand {

  /** 合同ID */
  private Long contractId;

  /** 案件ID */
  private Long matterId;

  /** 客户ID */
  @NotNull(message = "客户不能为空")
  private Long clientId;

  /** 收费类型 */
  @NotBlank(message = "收费类型不能为空")
  private String feeType;

  /** 收费项目名称 */
  @NotBlank(message = "收费项目名称不能为空")
  private String feeName;

  /** 应收金额 */
  @NotNull(message = "应收金额不能为空")
  @Positive(message = "应收金额必须大于0")
  private BigDecimal amount;

  /** 币种 */
  private String currency;

  /** 计划日期 */
  private LocalDate plannedDate;

  /** 负责人ID */
  private Long responsibleId;

  /** 备注 */
  private String remark;
}
