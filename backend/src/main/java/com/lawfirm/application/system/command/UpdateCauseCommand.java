package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 更新案由/罪名命令 */
@Data
public class UpdateCauseCommand {

  /** 案由代码（不可修改） */
  @NotBlank(message = "案由代码不能为空")
  private String code;

  /** 案由名称 */
  @NotBlank(message = "案由名称不能为空")
  private String name;

  /** 所属大类代码 */
  private String categoryCode;

  /** 所属大类名称 */
  private String categoryName;

  /** 父级案由代码（用于二级案由） */
  private String parentCode;

  /** 层级: 1=一级案由, 2=二级案由 */
  private Integer level;

  /** 排序号 */
  private Integer sortOrder;

  /** 是否启用 */
  private Boolean isActive;
}
