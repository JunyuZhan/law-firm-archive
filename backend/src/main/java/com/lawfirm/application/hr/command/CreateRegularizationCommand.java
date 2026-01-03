package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建转正申请命令
 */
@Data
public class CreateRegularizationCommand {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private LocalDate expectedRegularDate;
    private String selfEvaluation;
}

