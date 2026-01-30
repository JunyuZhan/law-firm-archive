package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

/** 创建发票命令 */
@Data
public class CreateInvoiceCommand {

  /** 收费记录ID */
  private Long feeId;

  /** 合同ID */
  private Long contractId;

  /** 客户ID */
  @NotNull(message = "客户不能为空")
  private Long clientId;

  /** 发票类型 */
  @NotBlank(message = "发票类型不能为空")
  private String invoiceType;

  /** 发票抬头 */
  @NotBlank(message = "发票抬头不能为空")
  private String title;

  /** 税号 */
  private String taxNo;

  /** 开票金额 */
  @NotNull(message = "开票金额不能为空")
  @Positive(message = "开票金额必须大于0")
  private BigDecimal amount;

  /** 税率 */
  private BigDecimal taxRate;

  /** 金额是否含税 true: amount 为含税价（价税合计） false: amount 为不含税价（默认） */
  private Boolean taxIncluded;

  /** 发票内容 */
  private String content;

  /** 备注 */
  private String remark;
}
