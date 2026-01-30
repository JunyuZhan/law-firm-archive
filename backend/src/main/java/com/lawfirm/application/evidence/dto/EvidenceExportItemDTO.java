package com.lawfirm.application.evidence.dto;

import lombok.Data;

/** 证据清单导出项 DTO */
@Data
public class EvidenceExportItemDTO {

  /** 证据ID */
  private Long id;

  /** 证据名称 */
  private String name;

  /** 证据类型 */
  private String evidenceType;

  /** 证据类型名称 */
  private String evidenceTypeName;

  /** 证明目的 */
  private String provePurpose;

  /** 起始页码 */
  private Integer pageStart;

  /** 结束页码 */
  private Integer pageEnd;

  /** 来源 */
  private String source;

  /** 是否原件 */
  private Boolean isOriginal;

  /** 原件份数 */
  private Integer originalCount;

  /** 复印件份数 */
  private Integer copyCount;

  /** 序号（在清单中的顺序） */
  private Integer listOrder;
}
