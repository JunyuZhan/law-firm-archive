package com.lawfirm.application.contract.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建合同模板命令 */
@Data
public class CreateContractTemplateCommand {

  /** 模板名称 */
  @NotBlank(message = "模板名称不能为空")
  private String name;

  /** 合同类型 */
  @NotBlank(message = "合同类型不能为空")
  private String contractType;

  /** 收费方式 */
  private String feeType;

  /** 模板内容 */
  private String content;

  /** 条款 */
  private String clauses;

  /** 描述 */
  private String description;
}
