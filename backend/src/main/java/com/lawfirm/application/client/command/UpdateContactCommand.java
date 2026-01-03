package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新联系人命令
 */
@Data
public class UpdateContactCommand {

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    private String position;
    private String department;
    private String mobilePhone;
    private String officePhone;
    private String email;
    private String wechat;
    private Boolean isPrimary;
    private String relationshipNote;
}

