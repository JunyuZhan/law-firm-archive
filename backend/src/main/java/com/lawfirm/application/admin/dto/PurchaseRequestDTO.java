package com.lawfirm.application.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 采购申请DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDTO {

  /** 申请ID */
  private Long id;

  /** 申请编号 */
  private String requestNo;

  /** 标题 */
  private String title;

  /** 申请人ID */
  private Long applicantId;

  /** 申请人名称 */
  private String applicantName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 采购类型 */
  private String purchaseType;

  /** 采购类型名称 */
  private String purchaseTypeName;

  /** 预计金额 */
  private BigDecimal estimatedAmount;

  /** 实际金额 */
  private BigDecimal actualAmount;

  /** 预计日期 */
  private LocalDate expectedDate;

  /** 申请原因 */
  private String reason;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审批人ID */
  private Long approverId;

  /** 审批人名称 */
  private String approverName;

  /** 审批日期 */
  private LocalDate approvalDate;

  /** 审批意见 */
  private String approvalComment;

  /** 供应商ID */
  private Long supplierId;

  /** 供应商名称 */
  private String supplierName;

  /** 备注 */
  private String remarks;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;

  /** 采购明细列表 */
  private List<PurchaseItemDTO> items;
}
