package com.lawfirm.application.system.command;

import lombok.Data;

/** 更新菜单命令. */
@Data
public class UpdateMenuCommand {
  /** ID. */
  private Long id;

  /** 父菜单ID. */
  private Long parentId;

  /** 菜单名称. */
  private String name;

  /** 路由路径. */
  private String path;

  /** 组件路径. */
  private String component;

  /** 跳转路径. */
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

  /** 状态. */
  private String status;

  /** 是否外链. */
  private Boolean isExternal;

  /** 是否缓存. */
  private Boolean isCache;
}
