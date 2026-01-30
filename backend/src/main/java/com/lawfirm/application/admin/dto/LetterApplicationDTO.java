package com.lawfirm.application.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 出函申请DTO */
@Data
public class LetterApplicationDTO {
  /** 申请ID */
  private Long id;

  /** 申请编号 */
  private String applicationNo;

  /** 模板ID */
  private Long templateId;

  /** 模板名称 */
  private String templateName;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 案件编号 */
  private String matterNo;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 申请人ID */
  private Long applicantId;

  /** 申请人名称 */
  private String applicantName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 函件类型 */
  private String letterType;

  /** 函件类型名称 */
  private String letterTypeName;

  /** 目标单位 */
  private String targetUnit;

  /** 目标联系人 */
  private String targetContact;

  /** 目标电话 */
  private String targetPhone;

  /** 目标地址 */
  private String targetAddress;

  /** 用途 */
  private String purpose;

  /** 律师ID列表 */
  private String lawyerIds;

  /** 律师名称列表 */
  private String lawyerNames;

  /** 函件内容 */
  private String content;

  /** 份数 */
  private Integer copies;

  /** 预计日期 */
  private LocalDate expectedDate;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审批人ID */
  private Long approvedBy;

  /** 审批人名称 */
  private String approverName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 审批意见 */
  private String approvalComment;

  /** 打印人ID */
  private Long printedBy;

  /** 打印人名称 */
  private String printerName;

  /** 打印时间 */
  private LocalDateTime printedAt;

  /** 接收人ID */
  private Long receivedBy;

  /** 接收人名称 */
  private String receiverName;

  /** 接收时间 */
  private LocalDateTime receivedAt;

  /** 备注 */
  private String remark;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
