package com.lawfirm.application.system.command;

import lombok.Data;

/** 创建菜单命令. */
@Data
public class CreateMenuCommand {
  /** 父菜单ID. */
  private Long parentId;

  /** 菜单名称. */
  private String name;

  /** 路由路径. */
  private String path;

  /** 组件路径. */
  private String component;

  /** 重定向路径. */
  private String redirect;

  /** 图标. */
  private String icon;

  /** 菜单类型. */
  private String menuType;

  /** 权限标识. */
  private String permission;

  /** 排序号. */
  private Integer sortOrder;

  /** 是否可见. */
  private Boolean visible;

  /** 是否外部链接. */
  private Boolean isExternal;

  /** 是否缓存. */
  private Boolean isCache;
}
