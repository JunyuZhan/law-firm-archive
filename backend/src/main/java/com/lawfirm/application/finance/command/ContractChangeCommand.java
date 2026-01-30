package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 合同变更申请命令 用于已审批通过的合同申请变更. */
@Data
public class ContractChangeCommand {

  /** 合同ID. */
  @NotNull(message = "合同ID不能为空")
  private Long contractId;

  /** 变更原因. */
  @NotNull(message = "变更原因不能为空")
  private String changeReason;

  /** 变更内容说明. */
  private String changeDescription;

  /** 变更后的合同名称. */
  private String name;

  /** 变更后的合同类型. */
  private String contractType;

  /** 变更后的客户ID. */
  private Long clientId;

  /** 变更后的案件ID. */
  private Long matterId;

  /** 变更后的收费方式. */
  private String feeType;

  /** 变更后的合同金额. */
  private BigDecimal totalAmount;

  /** 变更后的币种. */
  private String currency;

  /** 变更后的签约日期. */
  private LocalDate signDate;

  /** 变更后的生效日期. */
  private LocalDate effectiveDate;

  /** 变更后的到期日期. */
  private LocalDate expiryDate;

  /** 变更后的签约人ID. */
  private Long signerId;

  /** 变更后的部门ID. */
  private Long departmentId;

  /** 变更后的付款条款. */
  private String paymentTerms;

  /** 变更后的文件URL. */
  private String fileUrl;

  /** 变更后的备注. */
  private String remark;
}
