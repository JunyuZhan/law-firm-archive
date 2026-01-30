package com.lawfirm.application.clientservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 客户文件DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientFileDTO {

  /** ID */
  private Long id;

  /** 项目ID */
  private Long matterId;

  /** 项目名称 */
  private String matterName;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 文件名 */
  private String fileName;

  /** 原始文件名 */
  private String originalFileName;

  /** 文件大小 */
  private Long fileSize;

  /** 文件类型 */
  private String fileType;

  /** 文件分类 */
  private String fileCategory;

  /** 文件分类名称 */
  private String fileCategoryName;

  /** 描述 */
  private String description;

  /** 外部文件ID */
  private String externalFileId;

  /** 外部文件URL */
  private String externalFileUrl;

  /** 上传人 */
  private String uploadedBy;

  /** 上传时间 */
  private LocalDateTime uploadedAt;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 本地文档ID */
  private Long localDocumentId;

  /** 目标卷宗ID */
  private Long targetDossierId;

  /** 目标卷宗名称 */
  private String targetDossierName;

  /** 同步时间 */
  private LocalDateTime syncedAt;

  /** 同步人ID */
  private Long syncedBy;

  /** 同步人姓名 */
  private String syncedByName;

  /** 错误信息 */
  private String errorMessage;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
