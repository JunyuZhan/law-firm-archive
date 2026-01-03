package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建客户标签命令
 */
@Data
public class CreateClientTagCommand {

    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    private String tagColor;

    private String description;

    private Integer sortOrder;
}

