package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建收费记录命令
 */
@Data
public class CreateFeeCommand {

    private Long contractId;

    private Long matterId;

    @NotNull(message = "客户不能为空")
    private Long clientId;

    @NotBlank(message = "收费类型不能为空")
    private String feeType;

    @NotBlank(message = "收费项目名称不能为空")
    private String feeName;

    @NotNull(message = "应收金额不能为空")
    @Positive(message = "应收金额必须大于0")
    private BigDecimal amount;

    private String currency;

    private LocalDate plannedDate;

    private Long responsibleId;

    private String remark;
}

