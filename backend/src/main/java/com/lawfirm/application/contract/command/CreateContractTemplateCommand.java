package com.lawfirm.application.contract.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建合同模板命令
 */
@Data
public class CreateContractTemplateCommand {

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotBlank(message = "合同类型不能为空")
    private String contractType;

    private String feeType;

    private String content;

    private String clauses;

    private String description;
}
