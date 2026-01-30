package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 更新角色命令. */
@Data
public class UpdateRoleCommand {

  /** 角色ID. */
  @NotNull(message = "角色ID不能为空")
  private Long id;

  /** 角色名称. */
  private String roleName;

  /** 描述. */
  private String description;

  /** 数据权限范围. */
  private String dataScope;

  /** 排序号. */
  private Integer sortOrder;

  /** 菜单ID列表. */
  private List<Long> menuIds;
}
