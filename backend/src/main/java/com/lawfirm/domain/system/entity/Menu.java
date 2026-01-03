package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 菜单实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {

    /** 父菜单ID */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 重定向地址 */
    private String redirect;

    /** 图标 */
    private String icon;

    /** 类型 */
    private String menuType;

    /** 权限标识 */
    private String permission;

    /** 排序 */
    private Integer sortOrder;

    /** 是否可见 */
    private Boolean visible;

    /** 状态 */
    private String status;

    /** 是否外链 */
    private Boolean isExternal;

    /** 是否缓存 */
    private Boolean isCache;

    public static final String TYPE_DIRECTORY = "DIRECTORY";
    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_BUTTON = "BUTTON";

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";
}
