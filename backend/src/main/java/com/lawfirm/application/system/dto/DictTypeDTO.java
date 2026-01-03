package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 数据字典类型DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeDTO extends BaseDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String status;
    private Boolean isSystem;
    private List<DictItemDTO> items;
}
