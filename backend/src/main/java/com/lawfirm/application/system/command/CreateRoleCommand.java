package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/** 创建角色命令. */
@Data
public class CreateRoleCommand {

  /** 角色编码. */
  @NotBlank(message = "角色编码不能为空")
  private String roleCode;

  /** 角色名称. */
  @NotBlank(message = "角色名称不能为空")
  private String roleName;

  /** 描述. */
  private String description;

  /** 数据范围. */
  private String dataScope = "SELF";

  /** 排序. */
  private Integer sortOrder = 0;

  /** 菜单ID列表. */
  private List<Long> menuIds;
}
