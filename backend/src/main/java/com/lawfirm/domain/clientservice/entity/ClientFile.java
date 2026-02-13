package com.lawfirm.domain.clientservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 客户上传文件实体 存储客服系统推送过来的客户上传文件元数据 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("openapi_client_file")
public class ClientFile extends BaseEntity {

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 客户姓名 */
  private String clientName;

  /** 文件名 */
  private String fileName;

  /** 原始文件名（客户上传时的文件名） */
  private String originalFileName;

  /** 文件大小（字节） */
  private Long fileSize;

  /** 文件类型（MIME类型） */
  private String fileType;

  /** 文件类别 EVIDENCE - 证据材料 CONTRACT - 合同文件 ID_CARD - 身份证件 OTHER - 其他 */
  private String fileCategory;

  /** 文件描述 */
  private String description;

  /** 客服系统中的文件ID */
  private String externalFileId;

  /** 客服系统中的文件下载URL */
  private String externalFileUrl;

  /** 上传人（客户姓名或客服姓名） */
  private String uploadedBy;

  /** 上传时间（客服系统的时间） */
  private LocalDateTime uploadedAt;

  /** 状态 PENDING - 待同步 SYNCED - 已同步 DELETED - 已删除（客服系统已删除） FAILED - 同步失败 */
  private String status;

  /** 同步后的本地文档ID */
  private Long localDocumentId;

  /** 同步到的卷宗目录ID */
  private Long targetDossierId;

  /** 同步时间 */
  private LocalDateTime syncedAt;

  /** 同步操作人 */
  private Long syncedBy;

  /** 错误信息 */
  private String errorMessage;

  // ========== 状态常量 ==========
  /** 状态：待同步 */
  public static final String STATUS_PENDING = "PENDING";

  /** 状态：已同步 */
  public static final String STATUS_SYNCED = "SYNCED";

  /** 状态：已删除 */
  public static final String STATUS_DELETED = "DELETED";

  /** 状态：同步失败 */
  public static final String STATUS_FAILED = "FAILED";

  // ========== 文件类别常量 ==========
  /** 类别：证据材料 */
  public static final String CATEGORY_EVIDENCE = "EVIDENCE";

  /** 类别：合同文件 */
  public static final String CATEGORY_CONTRACT = "CONTRACT";

  /** 类别：身份证件 */
  public static final String CATEGORY_ID_CARD = "ID_CARD";

  /** 类别：其他 */
  public static final String CATEGORY_OTHER = "OTHER";
}
