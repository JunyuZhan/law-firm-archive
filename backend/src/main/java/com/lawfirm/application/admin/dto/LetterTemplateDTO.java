package com.lawfirm.application.admin.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 出函模板DTO */
@Data
public class LetterTemplateDTO {
  /** 模板ID */
  private Long id;

  /** 模板编号 */
  private String templateNo;

  /** 模板名称 */
  private String name;

  /** 函件类型 */
  private String letterType;

  /** 函件类型名称 */
  private String letterTypeName;

  /** 模板内容 */
  private String content;

  /** 描述 */
  private String description;

  /** 状态 */
  private String status;

  /** 排序 */
  private Integer sortOrder;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
