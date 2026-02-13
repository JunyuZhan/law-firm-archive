package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 提交审批命令. */
@Data
public class SubmitApprovalCommand {

  /** 工资表ID */
  @NotNull(message = "工资表ID不能为空")
  private Long payrollSheetId;

  /** 审批人ID（主任或合伙人）. */
  @NotNull(message = "审批人不能为空")
  private Long approverId;
}
