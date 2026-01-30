package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 角色菜单关联实体. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_menu")
public class RoleMenu {

  /** 主键ID. */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 角色ID. */
  private Long roleId;

  /** 菜单ID. */
  private Long menuId;
}
