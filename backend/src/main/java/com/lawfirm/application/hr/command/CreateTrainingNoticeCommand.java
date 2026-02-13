package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/** 创建培训通知命令 */
@Data
public class CreateTrainingNoticeCommand {

  /** 通知标题 */
  @NotBlank(message = "通知标题不能为空")
  private String title;

  /** 通知内容 */
  private String content;

  /** 附件列表 */
  private List<AttachmentCommand> attachments;

  /** 附件命令 */
  @Data
  public static class AttachmentCommand {
    /** 文件名 */
    private String fileName;

    /** 文件URL */
    private String fileUrl;
  }
}
