package com.lawfirm.application.system.command;

import lombok.Data;

/**
 * 创建菜单命令
 */
@Data
public class CreateMenuCommand {
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String redirect;
    private String icon;
    private String menuType;
    private String permission;
    private Integer sortOrder;
    private Boolean visible;
    private Boolean isExternal;
    private Boolean isCache;
}
