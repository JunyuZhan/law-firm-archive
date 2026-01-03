package com.lawfirm.application.finance.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建费用报销命令
 */
@Data
public class CreateExpenseCommand {

    private Long matterId;  // 可选，公共费用可为空

    @NotBlank(message = "费用类型不能为空")
    private String expenseType;  // TRAVEL, MEAL, ACCOMMODATION, TRANSPORT, MATERIAL, OTHER

    @NotBlank(message = "费用分类不能为空")
    private String expenseCategory;  // CASE_COST, OFFICE_COST, OTHER

    @NotNull(message = "费用发生日期不能为空")
    private LocalDate expenseDate;

    @NotNull(message = "费用金额不能为空")
    @Positive(message = "费用金额必须大于0")
    private BigDecimal amount;

    private String currency;  // 默认CNY

    @NotBlank(message = "费用说明不能为空")
    private String description;

    private String vendorName;
    private String invoiceNo;
    private String invoiceUrl;
    private String remark;
}

