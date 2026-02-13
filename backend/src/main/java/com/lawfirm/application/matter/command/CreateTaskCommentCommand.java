package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 创建任务评论命令 */
@Data
public class CreateTaskCommentCommand {

  /** 任务ID. */
  @NotNull(message = "任务ID不能为空")
  private Long taskId;

  /** 评论内容. */
  @NotBlank(message = "评论内容不能为空")
  private String content;

  /** 附件列表（文件URL）. */
  private List<String> attachments;

  /** 提醒的用户ID列表. */
  private List<Long> mentionedUserIds;
}
