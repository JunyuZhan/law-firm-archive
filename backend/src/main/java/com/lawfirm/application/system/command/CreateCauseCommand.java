package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建案由/罪名命令 */
@Data
public class CreateCauseCommand {

  /** 案由代码 */
  @NotBlank(message = "案由代码不能为空")
  private String code;

  /** 案由名称 */
  @NotBlank(message = "案由名称不能为空")
  private String name;

  /** 类型: CIVIL-民事, CRIMINAL-刑事, ADMIN-行政 */
  @NotBlank(message = "案由类型不能为空")
  private String causeType;

  /** 所属大类代码 */
  private String categoryCode;

  /** 所属大类名称 */
  private String categoryName;

  /** 父级案由代码（用于二级案由） */
  private String parentCode;

  /** 层级: 1=一级案由, 2=二级案由 */
  @NotNull(message = "层级不能为空")
  private Integer level;

  /** 排序号 */
  private Integer sortOrder;

  /** 是否启用 */
  private Boolean isActive;
}
