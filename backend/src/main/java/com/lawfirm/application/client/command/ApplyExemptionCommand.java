package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 申请利益冲突豁免命令 */
@Data
public class ApplyExemptionCommand {

  /** 利冲检查ID */
  @NotNull(message = "利冲检查ID不能为空")
  private Long conflictCheckId;

  /** 豁免理由 */
  @NotBlank(message = "豁免理由不能为空")
  private String exemptionReason;

  /** 豁免说明（详细说明为什么可以豁免） */
  private String exemptionDescription;
}
