package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 发票 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceDTO extends BaseDTO {

  /** 发票编号 */
  private String invoiceNo;

  /** 收费记录ID */
  private Long feeId;

  /** 合同ID */
  private Long contractId;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 发票类型 */
  private String invoiceType;

  /** 发票类型名称 */
  private String invoiceTypeName;

  /** 发票抬头 */
  private String title;

  /** 税号 */
  private String taxNo;

  /** 发票金额 */
  private BigDecimal amount;

  /** 税率 */
  private BigDecimal taxRate;

  /** 税额 */
  private BigDecimal taxAmount;

  /** 发票内容 */
  private String content;

  /** 开票日期 */
  private LocalDate invoiceDate;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 申请人ID */
  private Long applicantId;

  /** 申请人名称 */
  private String applicantName;

  /** 开票人ID */
  private Long issuerId;

  /** 开票人名称 */
  private String issuerName;

  /** 文件URL */
  private String fileUrl;

  /** 备注 */
  private String remark;
}
