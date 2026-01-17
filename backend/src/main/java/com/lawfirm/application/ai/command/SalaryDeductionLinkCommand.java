package com.lawfirm.application.ai.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 关联工资扣减命令
 */
@Data
public class SalaryDeductionLinkCommand {

    /**
     * 账单ID列表
     */
    @NotNull(message = "账单ID不能为空")
    private List<Long> billIds;

    /**
     * 工资单ID
     */
    @NotNull(message = "工资单ID不能为空")
    private Long salarySheetId;

    /**
     * 备注
     */
    private String remark;
}
