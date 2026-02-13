package com.lawfirm.application.client.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 客户标签DTO */
@Data
public class ClientTagDTO {

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

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
