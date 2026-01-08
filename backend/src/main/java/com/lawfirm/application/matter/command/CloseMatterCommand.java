package com.lawfirm.application.matter.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 项目结案命令
 */
@Data
public class CloseMatterCommand {

    // matterId 由 Controller 从 URL 路径中设置，不需要在请求体中验证
    private Long matterId;

    @NotNull(message = "结案日期不能为空")
    private LocalDate closingDate;

    @NotBlank(message = "结案原因不能为空")
    private String closingReason;  // 结案原因：WIN-胜诉, LOSE-败诉, SETTLEMENT-和解, WITHDRAWAL-撤诉, COMPLETED-项目完成, OTHER-其他

    private String outcome;  // 判决/调解结果描述

    private String summary;  // 结案总结

    private String remark;  // 备注
    
    private Long approverId;  // 指定审批人ID
}

