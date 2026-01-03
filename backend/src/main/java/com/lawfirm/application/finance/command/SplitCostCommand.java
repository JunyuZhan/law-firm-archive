package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成本分摊命令
 */
@Data
public class SplitCostCommand {

    /**
     * 费用ID（公共费用）
     */
    @NotNull(message = "费用ID不能为空")
    private Long expenseId;

    /**
     * 分摊方式：EQUAL-平均分摊, RATIO-按比例, MANUAL-手动指定
     */
    @NotNull(message = "分摊方式不能为空")
    private String splitMethod;

    /**
     * 要分摊到的项目ID列表
     */
    @NotEmpty(message = "项目列表不能为空")
    private List<Long> matterIds;

    /**
     * 手动指定分摊金额（当splitMethod为MANUAL时使用）
     * key: matterId, value: 分摊金额
     */
    private Map<Long, BigDecimal> manualAmounts;

    /**
     * 按比例分摊的比例（当splitMethod为RATIO时使用）
     * key: matterId, value: 分摊比例（如0.25表示25%）
     */
    private Map<Long, BigDecimal> ratios;

    /**
     * 备注
     */
    private String remark;
}

