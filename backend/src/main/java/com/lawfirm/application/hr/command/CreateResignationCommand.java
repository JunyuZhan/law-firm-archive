package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建离职申请命令
 */
@Data
public class CreateResignationCommand {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "离职类型不能为空")
    private String resignationType;

    @NotNull(message = "离职日期不能为空")
    private LocalDate resignationDate;

    @NotNull(message = "最后工作日不能为空")
    private LocalDate lastWorkDate;

    private String reason;
    private Long handoverPersonId;
    private String handoverNote;
}

