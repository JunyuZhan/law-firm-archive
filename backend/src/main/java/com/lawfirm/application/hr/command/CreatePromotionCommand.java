package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 创建晋升申请命令 */
@Data
public class CreatePromotionCommand {

  /** 目标职级ID */
  @NotNull(message = "目标职级不能为空")
  private Long targetLevelId;

  /** 申请理由 */
  private String applyReason;

  /** 工作业绩 */
  private String achievements;

  /** 自我评价 */
  private String selfEvaluation;

  /** 附件列表 */
  private List<String> attachments;
}
