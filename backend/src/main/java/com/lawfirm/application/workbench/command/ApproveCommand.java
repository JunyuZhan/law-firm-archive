package com.lawfirm.application.workbench.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批命令
 */
@Data
public class ApproveCommand {
    /**
     * 审批记录ID
     */
    @NotNull(message = "审批ID不能为空")
    private Long approvalId;

    /**
     * 审批结果（APPROVED-通过, REJECTED-拒绝）
     */
    @NotBlank(message = "审批结果不能为空")
    private String result;

    /**
     * 审批意见
     * 拒绝时必填
     */
    private String comment;
}

