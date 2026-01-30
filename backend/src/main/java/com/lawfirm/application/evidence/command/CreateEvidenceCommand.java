package com.lawfirm.application.evidence.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建证据命令. */
@Data
public class CreateEvidenceCommand {

  /** 案件ID */
  @NotNull(message = "案件ID不能为空")
  private Long matterId;

  /** 证据名称 */
  @NotBlank(message = "证据名称不能为空")
  private String name;

  /** 证据类型 */
  @NotBlank(message = "证据类型不能为空")
  private String evidenceType;

  /** 证据来源 */
  private String source;

  /** 证据分组 */
  private String groupName;

  /** 证明目的 */
  private String provePurpose;

  /** 描述 */
  private String description;

  /** 是否原件 */
  private Boolean isOriginal;

  /** 原件份数 */
  private Integer originalCount;

  /** 复印件份数 */
  private Integer copyCount;

  /** 起始页码 */
  private Integer pageStart;

  /** 结束页码 */
  private Integer pageEnd;

  /** 文件URL */
  private String fileUrl;

  /** 文件名 */
  private String fileName;

  /** 文件大小 */
  private Long fileSize;

  /** 文件类型分类（image/pdf/word/excel/ppt/audio/video/other） */
  private String fileType;

  /** 缩略图URL（仅图片文件） */
  private String thumbnailUrl;

  /** 关联卷宗文件ID，引用 doc_document.id 当设置此字段时，文件信息从卷宗文件获取 */
  private Long documentId;
}
