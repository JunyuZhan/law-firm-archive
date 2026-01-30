package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 法规DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class LawRegulationDTO extends BaseDTO {

  /** ID */
  private Long id;

  /** 标题 */
  private String title;

  /** 分类ID */
  private Long categoryId;

  /** 分类名称 */
  private String categoryName;

  /** 文号 */
  private String docNumber;

  /** 发布机关 */
  private String issuingAuthority;

  /** 发布日期 */
  private LocalDate issueDate;

  /** 生效日期 */
  private LocalDate effectiveDate;

  /** 失效日期 */
  private LocalDate expiryDate;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 内容 */
  private String content;

  /** 摘要 */
  private String summary;

  /** 关键词 */
  private String keywords;

  /** 来源 */
  private String source;

  /** 附件URL */
  private String attachmentUrl;

  /** 浏览次数 */
  private Integer viewCount;

  /** 收藏次数 */
  private Integer collectCount;

  /** 是否已收藏 */
  private Boolean collected;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
