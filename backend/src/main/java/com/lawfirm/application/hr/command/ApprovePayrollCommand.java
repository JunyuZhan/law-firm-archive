package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 审批工资表命令. */
@Data
public class ApprovePayrollCommand {

  /** 工资表ID */
  @NotNull(message = "工资表ID不能为空")
  private Long payrollSheetId;

  /** 审批状态：APPROVED-通过, REJECTED-拒绝. */
  @NotNull(message = "审批状态不能为空")
  private String approvalStatus;

  /** 审批意见. */
  private String approvalComment;
}
