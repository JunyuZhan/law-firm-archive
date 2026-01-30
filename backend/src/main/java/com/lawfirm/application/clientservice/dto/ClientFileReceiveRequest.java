package com.lawfirm.application.clientservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/** 接收客户文件请求（客服系统调用） */
@Data
public class ClientFileReceiveRequest {

  /** 项目ID */
  @NotNull(message = "项目ID不能为空")
  private Long matterId;

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long clientId;

  /** 客户姓名 */
  private String clientName;

  /** 文件名 */
  @NotBlank(message = "文件名不能为空")
  private String fileName;

  /** 文件大小（字节） */
  private Long fileSize;

  /** 文件类型（MIME类型） */
  private String fileType;

  /** 文件类别：EVIDENCE, CONTRACT, ID_CARD, OTHER */
  private String fileCategory;

  /** 文件描述 */
  private String description;

  /** 客服系统中的文件ID（用于后续删除回调） */
  @NotBlank(message = "外部文件ID不能为空")
  private String externalFileId;

  /** 客服系统中的文件下载URL */
  @NotBlank(message = "文件下载URL不能为空")
  private String externalFileUrl;

  /** 上传人 */
  private String uploadedBy;

  /** 上传时间 */
  private LocalDateTime uploadedAt;
}
