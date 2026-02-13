package com.lawfirm.application.evidence.command;

import java.util.List;
import lombok.Data;

/** 创建证据清单命令. */
@Data
public class CreateEvidenceListCommand {

  /** 项目ID */
  private Long matterId;

  /** 清单名称 */
  private String name;

  /** 清单类型 */
  private String listType;

  /** 证据ID列表 */
  private List<Long> evidenceIds;
}
