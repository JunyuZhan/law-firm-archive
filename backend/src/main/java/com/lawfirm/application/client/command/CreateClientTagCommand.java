package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建客户标签命令 */
@Data
public class CreateClientTagCommand {

  /** 标签名称 */
  @NotBlank(message = "标签名称不能为空")
  private String tagName;

  /** 标签颜色 */
  private String tagColor;

  /** 描述 */
  private String description;

  /** 排序 */
  private Integer sortOrder;
}
