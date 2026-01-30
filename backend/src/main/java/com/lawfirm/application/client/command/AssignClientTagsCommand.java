package com.lawfirm.application.client.command;

import java.util.List;
import lombok.Data;

/** 为客户分配标签命令 */
@Data
public class AssignClientTagsCommand {

  /** 客户ID */
  private Long clientId;

  /** 标签ID列表 */
  private List<Long> tagIds;
}
