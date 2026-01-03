package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始计时命令（M3-044）
 */
@Data
public class StartTimerCommand {

    /**
     * 案件ID
     */
    @NotNull(message = "案件ID不能为空")
    private Long matterId;

    /**
     * 工作类型
     */
    private String workType;

    /**
     * 工作内容
     */
    private String workContent;

    /**
     * 是否计费
     */
    private Boolean billable;
}

