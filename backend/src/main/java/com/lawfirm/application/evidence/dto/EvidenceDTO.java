package com.lawfirm.application.evidence.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 证据DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvidenceDTO extends BaseDTO {

  /** ID */
  private Long id;

  /** 证据编号 */
  private String evidenceNo;

  /** 项目ID */
  private Long matterId;

  /** 项目名称 */
  private String matterName;

  /** 证据名称 */
  private String name;

  /** 证据类型 */
  private String evidenceType;

  /** 证据类型名称 */
  private String evidenceTypeName;

  /** 来源 */
  private String source;

  /** 分组名称 */
  private String groupName;

  /** 排序 */
  private Integer sortOrder;

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

  /** 页码范围 */
  private String pageRange;

  /** 文件URL */
  private String fileUrl;

  /** 文件名 */
  private String fileName;

  /** 文件大小 */
  private Long fileSize;

  /** 文件大小显示 */
  private String fileSizeDisplay;

  /** 文件类型：image, pdf, word, excel, video, audio, other */
  private String fileType;

  /** 缩略图URL（仅图片文件） */
  private String thumbnailUrl;

  /** 质证状态 */
  private String crossExamStatus;

  /** 质证状态名称 */
  private String crossExamStatusName;

  /** 状态 */
  private String status;

  /** 质证记录 */
  private List<EvidenceCrossExamDTO> crossExams;

  /** 创建人ID */
  private Long createdBy;

  /** 创建人姓名 */
  private String createdByName;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
