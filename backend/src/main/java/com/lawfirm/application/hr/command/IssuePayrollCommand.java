package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发放工资命令
 */
@Data
public class IssuePayrollCommand {

    @NotNull(message = "工资表ID不能为空")
    private Long payrollSheetId;

    /**
     * 发放方式：BANK_TRANSFER-银行转账, CASH-现金, OTHER-其他
     */
    @NotNull(message = "发放方式不能为空")
    private String paymentMethod;

    /**
     * 发放凭证URL
     */
    private String paymentVoucherUrl;

    /**
     * 备注
     */
    private String remark;
}

