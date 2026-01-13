package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建期限提醒命令
 */
@Data
@Builder
public class CreateDeadlineCommand {

    @NotNull(message = "项目ID不能为空")
    private Long matterId;

    @NotBlank(message = "期限类型不能为空")
    private String deadlineType;

    @NotBlank(message = "期限名称不能为空")
    private String deadlineName;

    private LocalDate baseDate; // 可选，默认使用当前日期

    @NotNull(message = "期限日期不能为空")
    private LocalDate deadlineDate;

    private Integer reminderDays; // 默认7天
    private String description;
}

