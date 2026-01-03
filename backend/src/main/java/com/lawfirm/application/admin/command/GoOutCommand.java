package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外出登记命令（M8-005）
 */
@Data
public class GoOutCommand {
    /**
     * 外出日期
     */
    @NotNull(message = "外出日期不能为空")
    private LocalDateTime outTime;

    /**
     * 预计返回时间
     */
    private LocalDateTime expectedReturnTime;

    /**
     * 外出地点
     */
    private String location;

    /**
     * 外出事由
     */
    @NotNull(message = "外出事由不能为空")
    private String reason;

    /**
     * 同行人员
     */
    private String companions;
}

