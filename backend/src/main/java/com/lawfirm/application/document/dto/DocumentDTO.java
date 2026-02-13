package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 文档编号. */
  private String docNo;

  /** 文档标题. */
  private String title;

  /** 分类ID. */
  private Long categoryId;

  /** 分类名称. */
  private String categoryName;

  /** 案件ID. */
  private Long matterId;

  /** 案件名称. */
  private String matterName;

  /** 文件名. */
  private String fileName;

  /** 文件路径. */
  private String filePath;

  /** 文件大小（字节）. */
  private Long fileSize;

  /** 文件大小显示. */
  private String fileSizeDisplay;

  /** 文件类型. */
  private String fileType;

  /** MIME类型. */
  private String mimeType;

  /** 版本号. */
  private Integer version;

  /** 是否最新版本. */
  private Boolean isLatest;

  /** 父文档ID. */
  private Long parentDocId;

  /** 安全级别. */
  private String securityLevel;

  /** 安全级别名称. */
  private String securityLevelName;

  /** 阶段. */
  private String stage;

  /** 标签列表. */
  private List<String> tags;

  /** 描述. */
  private String description;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 创建人ID. */
  private Long createdBy;

  /** 创建人名称. */
  private String createdByName;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;

  /** 卷宗目录项ID. */
  private Long dossierItemId;

  /** 文件夹路径. */
  private String folderPath;

  /** 显示排序顺序. */
  private Integer displayOrder;

  /** 缩略图URL. */
  private String thumbnailUrl;

  /** 文档来源类型: SYSTEM_GENERATED, SYSTEM_LINKED, USER_UPLOADED, SIGNED_VERSION. */
  private String sourceType;

  /** 文档来源类型名称. */
  private String sourceTypeName;

  /** 来源数据ID. */
  private Long sourceId;

  /** 来源模块. */
  private String sourceModule;
}
