package com.lawfirm.application.finance.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 审批费用报销命令
 */
@Data
public class ApproveExpenseCommand {

    @NotNull(message = "费用ID不能为空")
    private Long expenseId;

    @NotBlank(message = "审批结果不能为空")
    private String action;  // APPROVE-通过, REJECT-驳回

    private String comment;  // 审批意见
}

