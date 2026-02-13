package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 菜单DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuDTO extends BaseDTO {
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

  /** 重定向路径. */
  private String redirect;

  /** 图标. */
  private String icon;

  /** 菜单类型. */
  private String menuType;

  /** 菜单类型名称. */
  private String menuTypeName;

  /** 权限标识. */
  private String permission;

  /** 排序. */
  private Integer sortOrder;

  /** 是否可见. */
  private Boolean visible;

  /** 状态. */
  private String status;

  /** 是否外链. */
  private Boolean isExternal;

  /** 是否缓存. */
  private Boolean isCache;

  /** 子菜单. */
  private List<MenuDTO> children;
}
