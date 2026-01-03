package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuDTO extends BaseDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String redirect;
    private String icon;
    private String menuType;
    private String menuTypeName;
    private String permission;
    private Integer sortOrder;
    private Boolean visible;
    private String status;
    private Boolean isExternal;
    private Boolean isCache;
    
    /** 子菜单 */
    private List<MenuDTO> children;
}
