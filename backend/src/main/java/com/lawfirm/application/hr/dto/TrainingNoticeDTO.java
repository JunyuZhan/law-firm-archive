package com.lawfirm.application.hr.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 培训通知 DTO（简化版） */
@Data
public class TrainingNoticeDTO {

  /** 通知ID */
  private Long id;

  /** 通知标题 */
  private String title;

  /** 通知内容 */
  private String content;

  /** 附件列表 */
  private List<AttachmentDTO> attachments;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 发布时间 */
  private LocalDateTime publishedAt;

  /** 创建时间 */
  private LocalDateTime createdAt;

  // ===== 完成情况统计 =====

  /** 已完成人数 */
  private Integer completedCount;

  /** 总人数 */
  private Integer totalCount;

  /** 当前用户是否已完成 */
  private Boolean myCompleted;

  /** 当前用户的证书URL */
  private String myCertificateUrl;

  /** 附件DTO */
  @Data
  public static class AttachmentDTO {
    /** 文件名 */
    private String fileName;

    /** 文件URL */
    private String fileUrl;
  }
}
