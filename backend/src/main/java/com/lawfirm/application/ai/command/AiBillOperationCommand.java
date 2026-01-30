package com.lawfirm.application.ai.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 账单操作命令（扣减/减免） */
@Data
public class AiBillOperationCommand {

  /** 操作类型：DEDUCT-扣减, WAIVE-减免 */
  @NotBlank(message = "操作类型不能为空")
  private String operation;

  /** 备注/原因 */
  @NotBlank(message = "备注不能为空")
  private String remark;
}
