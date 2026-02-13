package com.lawfirm.application.client.command;

import lombok.Data;

/** 更新客户标签命令 */
@Data
public class UpdateClientTagCommand {

  /** 标签ID */
  private Long id;

  /** 标签名称 */
  private String tagName;

  /** 标签颜色 */
  private String tagColor;

  /** 描述 */
  private String description;

  /** 排序 */
  private Integer sortOrder;
}
