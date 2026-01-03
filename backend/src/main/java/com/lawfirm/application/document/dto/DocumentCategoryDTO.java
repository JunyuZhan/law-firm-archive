package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文档分类DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentCategoryDTO extends BaseDTO {

    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private String description;
    
    /**
     * 子分类（树形结构用）
     */
    private List<DocumentCategoryDTO> children;
}
