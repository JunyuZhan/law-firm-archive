package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 确认工资表命令
 */
@Data
public class ConfirmPayrollCommand {

    @NotNull(message = "工资明细ID不能为空")
    private Long payrollItemId;

    /**
     * 确认状态：CONFIRMED-已确认, REJECTED-已拒绝
     */
    @NotNull(message = "确认状态不能为空")
    private String confirmStatus;

    /**
     * 确认意见（拒绝时必填）
     */
    private String confirmComment;
}

