package com.lawfirm.application.client.command;

import lombok.Data;

/**
 * 更新客户标签命令
 */
@Data
public class UpdateClientTagCommand {

    private Long id;
    private String tagName;
    private String tagColor;
    private String description;
    private Integer sortOrder;
}

