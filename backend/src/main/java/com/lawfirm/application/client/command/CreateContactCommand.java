package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建联系人命令
 */
@Data
public class CreateContactCommand {

    @NotNull(message = "客户ID不能为空")
    private Long clientId;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    private String position;
    private String department;
    private String mobilePhone;
    private String officePhone;
    private String email;
    private String wechat;
    private Boolean isPrimary = false;
    private String relationshipNote;
}

