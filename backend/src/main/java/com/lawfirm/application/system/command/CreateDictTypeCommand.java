package com.lawfirm.application.system.command;

import lombok.Data;

/**
 * 创建字典类型命令
 */
@Data
public class CreateDictTypeCommand {
    private String name;
    private String code;
    private String description;
}
