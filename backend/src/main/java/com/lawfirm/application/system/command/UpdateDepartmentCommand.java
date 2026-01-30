package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 更新部门命令. */
@Data
public class UpdateDepartmentCommand {

  /** ID. */
  @NotNull(message = "部门ID不能为空")
  private Long id;

  /** 部门名称. */
  private String name;

  /** 上级部门ID. */
  private Long parentId;

  /** 排序号. */
  private Integer sortOrder;

  /** 负责人ID. */
  private Long leaderId;

  /** 状态. */
  private String status;
}
