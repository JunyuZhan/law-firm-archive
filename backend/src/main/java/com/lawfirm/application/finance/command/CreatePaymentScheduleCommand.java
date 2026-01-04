package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建付款计划命令
 */
@Data
public class CreatePaymentScheduleCommand {

    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotBlank(message = "阶段名称不能为空")
    private String phaseName;

    @NotNull(message = "付款金额不能为空")
    private BigDecimal amount;

    /**
     * 比例（风险代理时使用）
     */
    private BigDecimal percentage;

    /**
     * 计划收款日期
     */
    private LocalDate plannedDate;

    /**
     * 备注
     */
    private String remark;
}
