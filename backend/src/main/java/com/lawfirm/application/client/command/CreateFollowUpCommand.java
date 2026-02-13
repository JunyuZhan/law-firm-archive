package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/** 创建跟进记录命令 */
@Data
public class CreateFollowUpCommand {

  /** 案源ID */
  @NotNull(message = "案源ID不能为空")
  private Long leadId;

  /** 跟进方式（PHONE, EMAIL, VISIT, MEETING, OTHER） */
  @NotBlank(message = "跟进方式不能为空")
  private String followType;

  /** 跟进内容 */
  @NotBlank(message = "跟进内容不能为空")
  private String followContent;

  /** 跟进结果（POSITIVE, NEUTRAL, NEGATIVE） */
  private String followResult;

  /** 下次跟进时间 */
  private LocalDateTime nextFollowTime;

  /** 下次跟进计划 */
  private String nextFollowPlan;
}
