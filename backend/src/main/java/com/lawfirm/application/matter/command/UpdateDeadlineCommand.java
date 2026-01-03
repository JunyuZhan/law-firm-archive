package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新期限提醒命令
 */
@Data
public class UpdateDeadlineCommand {

    @NotNull(message = "期限ID不能为空")
    private Long id;

    private String deadlineName;
    private LocalDate baseDate;
    private LocalDate deadlineDate;
    private Integer reminderDays;
    private String status;
    private String description;
}

