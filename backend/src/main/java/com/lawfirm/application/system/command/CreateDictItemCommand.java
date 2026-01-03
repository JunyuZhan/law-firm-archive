package com.lawfirm.application.system.command;

import lombok.Data;

/**
 * 创建字典项命令
 */
@Data
public class CreateDictItemCommand {
    private Long dictTypeId;
    private String label;
    private String value;
    private String description;
    private Integer sortOrder;
    private String cssClass;
}
