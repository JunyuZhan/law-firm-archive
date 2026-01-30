package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 归还档案命令. */
@Data
public class ReturnArchiveCommand {

  /** 借阅ID */
  @NotNull(message = "借阅ID不能为空")
  private Long borrowId;

  /** 归还状态 */
  @NotBlank(message = "归还状态不能为空")
  private String returnCondition;

  /** 归还备注 */
  private String returnRemarks;
}
