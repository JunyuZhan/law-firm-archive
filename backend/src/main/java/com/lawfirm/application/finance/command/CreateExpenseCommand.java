package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建费用报销命令 */
@Data
public class CreateExpenseCommand {

  /** 案件ID（可选，公共费用可为空） */
  private Long matterId;

  /** 费用类型（TRAVEL, MEAL, ACCOMMODATION, TRANSPORT, MATERIAL, OTHER） */
  @NotBlank(message = "费用类型不能为空")
  private String expenseType;

  /** 费用分类（CASE_COST, OFFICE_COST, OTHER） */
  @NotBlank(message = "费用分类不能为空")
  private String expenseCategory;

  /** 费用发生日期 */
  @NotNull(message = "费用发生日期不能为空")
  private LocalDate expenseDate;

  /** 费用金额 */
  @NotNull(message = "费用金额不能为空")
  @Positive(message = "费用金额必须大于0")
  private BigDecimal amount;

  /** 币种（默认CNY） */
  private String currency;

  /** 费用说明 */
  @NotBlank(message = "费用说明不能为空")
  private String description;

  /** 供应商名称 */
  private String vendorName;

  /** 发票号 */
  private String invoiceNo;

  /** 发票URL */
  private String invoiceUrl;

  /** 备注 */
  private String remark;
}
