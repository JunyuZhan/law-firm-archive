package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 案例分类DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseCategoryDTO extends BaseDTO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private String description;
    private List<CaseCategoryDTO> children;
}
