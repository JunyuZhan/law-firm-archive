package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 采购申请实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("admin_purchase_request")
public class PurchaseRequest extends BaseEntity {

  /** 申请单号 */
  private String requestNo;

  /** 申请标题 */
  private String title;

  /** 申请人ID */
  private Long applicantId;

  /** 申请部门ID */
  private Long departmentId;

  /** 采购类型：OFFICE-办公用品, IT-IT设备, FURNITURE-家具, SERVICE-服务, OTHER-其他 */
  private String purchaseType;

  /** 预计总金额 */
  private BigDecimal estimatedAmount;

  /** 实际总金额 */
  private BigDecimal actualAmount;

  /** 期望到货日期 */
  private LocalDate expectedDate;

  /** 采购原因 */
  private String reason;

  /**
   * 状态：DRAFT-草稿, PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, PURCHASING-采购中, COMPLETED-已完成,
   * CANCELLED-已取消
   */
  private String status;

  /** 审批人ID */
  private Long approverId;

  /** 审批时间 */
  private LocalDate approvalDate;

  /** 审批意见 */
  private String approvalComment;

  /** 供应商ID */
  private Long supplierId;

  /** 备注 */
  private String remarks;
}
