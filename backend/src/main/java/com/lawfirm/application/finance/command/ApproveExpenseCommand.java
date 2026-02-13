package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 审批费用报销命令 */
@Data
public class ApproveExpenseCommand {

  /** 费用ID */
  @NotNull(message = "费用ID不能为空")
  private Long expenseId;

  /** 审批结果（APPROVE-通过, REJECT-驳回） */
  @NotBlank(message = "审批结果不能为空")
  private String action;

  /** 审批意见 */
  private String comment;
}
