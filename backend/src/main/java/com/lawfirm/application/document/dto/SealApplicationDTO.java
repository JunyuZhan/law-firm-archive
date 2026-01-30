package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用印申请DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealApplicationDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 申请编号. */
  private String applicationNo;

  /** 申请人ID. */
  private Long applicantId;

  /** 申请人姓名. */
  private String applicantName;

  /** 部门ID. */
  private Long departmentId;

  /** 部门名称. */
  private String departmentName;

  /** 印章ID. */
  private Long sealId;

  /** 印章名称. */
  private String sealName;

  /** 印章类型. */
  private String sealType;

  /** 案件ID. */
  private Long matterId;

  /** 案件名称. */
  private String matterName;

  /** 文档名称. */
  private String documentName;

  /** 文档类型. */
  private String documentType;

  /** 份数. */
  private Integer copies;

  /** 用印用途. */
  private String usePurpose;

  /** 预计用印日期. */
  private LocalDate expectedUseDate;

  /** 实际用印日期. */
  private LocalDate actualUseDate;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 审批人ID. */
  private Long approvedBy;

  /** 审批人姓名. */
  private String approvedByName;

  /** 审批时间. */
  private LocalDateTime approvedAt;

  /** 审批意见. */
  private String approvalComment;

  /** 使用人ID. */
  private Long usedBy;

  /** 使用人姓名. */
  private String usedByName;

  /** 使用时间. */
  private LocalDateTime usedAt;

  /** 使用备注. */
  private String useRemark;

  /** 附件文件URL（向后兼容字段）. */
  private String attachmentUrl;

  /** MinIO桶名称，默认law-firm. */
  private String bucketName;

  /** 存储路径：seal/M_{matterId}/{YYYY-MM}/用印附件/. */
  private String storagePath;

  /** 物理文件名：{YYYYMMDD}_{UUID}_{documentName}.{ext}. */
  private String physicalName;

  /** 文件Hash值（SHA-256），用于去重和校验. */
  private String fileHash;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
