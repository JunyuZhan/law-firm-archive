package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典项DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictItemDTO extends BaseDTO {
    private Long id;
    private Long dictTypeId;
    private String label;
    private String value;
    private String description;
    private Integer sortOrder;
    private String status;
    private String cssClass;
}
