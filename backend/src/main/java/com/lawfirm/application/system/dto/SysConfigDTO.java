package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigDTO extends BaseDTO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String configType;
    private String description;
    private Boolean isSystem;
}
