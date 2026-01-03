package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建工时记录命令
 */
@Data
public class CreateTimesheetCommand {

    /**
     * 案件ID
     */
    @NotNull(message = "案件ID不能为空")
    private Long matterId;

    /**
     * 工作日期
     */
    @NotNull(message = "工作日期不能为空")
    private LocalDate workDate;

    /**
     * 工时（小时）
     */
    @NotNull(message = "工时不能为空")
    @Positive(message = "工时必须大于0")
    private BigDecimal hours;

    /**
     * 工作类型
     */
    private String workType;

    /**
     * 工作内容
     */
    @NotBlank(message = "工作内容不能为空")
    private String workContent;

    /**
     * 是否计费
     */
    private Boolean billable;

    /**
     * 小时费率（可选，不填则使用默认费率）
     */
    private BigDecimal hourlyRate;
}
