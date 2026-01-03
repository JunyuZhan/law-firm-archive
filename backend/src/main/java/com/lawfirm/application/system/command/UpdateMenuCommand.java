package com.lawfirm.application.system.command;

import lombok.Data;

/**
 * 更新菜单命令
 */
@Data
public class UpdateMenuCommand {
    private Long id;
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
    private String status;
    private Boolean isExternal;
    private Boolean isCache;
}
