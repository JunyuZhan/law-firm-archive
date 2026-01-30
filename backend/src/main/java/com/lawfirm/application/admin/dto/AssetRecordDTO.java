package com.lawfirm.application.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 资产操作记录DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRecordDTO {

  /** 记录ID */
  private Long id;

  /** 资产ID */
  private Long assetId;

  /** 资产编号 */
  private String assetNo;

  /** 资产名称 */
  private String assetName;

  /** 记录类型 */
  private String recordType;

  /** 记录类型名称 */
  private String recordTypeName;

  /** 操作人ID */
  private Long operatorId;

  /** 操作人名称 */
  private String operatorName;

  /** 原使用人ID */
  private Long fromUserId;

  /** 原使用人名称 */
  private String fromUserName;

  /** 新使用人ID */
  private Long toUserId;

  /** 新使用人名称 */
  private String toUserName;

  /** 操作日期 */
  private LocalDate operateDate;

  /** 预计归还日期 */
  private LocalDate expectedReturnDate;

  /** 实际归还日期 */
  private LocalDate actualReturnDate;

  /** 原因 */
  private String reason;

  /** 维护费用 */
  private BigDecimal maintenanceCost;

  /** 审批状态 */
  private String approvalStatus;

  /** 审批人ID */
  private Long approverId;

  /** 审批人名称 */
  private String approverName;

  /** 审批意见 */
  private String approvalComment;

  /** 备注 */
  private String remarks;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
