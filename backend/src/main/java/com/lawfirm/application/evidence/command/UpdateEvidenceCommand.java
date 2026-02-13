package com.lawfirm.application.evidence.command;

import lombok.Data;

/** 更新证据命令. */
@Data
public class UpdateEvidenceCommand {

  /** 证据名称 */
  private String name;

  /** 证据类型 */
  private String evidenceType;

  /** 证据来源 */
  private String source;

  /** 证据分组 */
  private String groupName;

  /** 证明目的 */
  private String provePurpose;

  /** 描述 */
  private String description;

  /** 是否原件 */
  private Boolean isOriginal;

  /** 原件份数 */
  private Integer originalCount;

  /** 复印件份数 */
  private Integer copyCount;

  /** 起始页码 */
  private Integer pageStart;

  /** 结束页码 */
  private Integer pageEnd;
}
