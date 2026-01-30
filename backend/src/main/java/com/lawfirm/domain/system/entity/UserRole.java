package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户角色关联实体. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
public class UserRole {

  /** 主键ID. */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户ID. */
  private Long userId;

  /** 角色ID. */
  private Long roleId;
}
