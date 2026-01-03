package com.lawfirm.application.system.command;

import lombok.Data;

/**
 * 更新配置命令
 */
@Data
public class UpdateConfigCommand {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String configType;
    private String description;
}
