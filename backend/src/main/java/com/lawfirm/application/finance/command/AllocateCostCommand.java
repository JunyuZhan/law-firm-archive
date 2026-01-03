package com.lawfirm.application.finance.command;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 成本归集命令
 */
@Data
public class AllocateCostCommand {

    @NotNull(message = "项目ID不能为空")
    private Long matterId;

    @NotNull(message = "费用ID列表不能为空")
    private List<Long> expenseIds;  // 要归集的费用ID列表
}

