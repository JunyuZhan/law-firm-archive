package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 法规实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("law_regulation")
public class LawRegulation extends BaseEntity {

  /** 法规标题. */
  private String title;

  /** 分类ID. */
  private Long categoryId;

  /** 文号. */
  private String docNumber;

  /** 发布机关. */
  private String issuingAuthority;

  /** 发布日期. */
  private LocalDate issueDate;

  /** 生效日期. */
  private LocalDate effectiveDate;

  /** 失效日期. */
  private LocalDate expiryDate;

  /** 状态: EFFECTIVE有效/AMENDED已修订/REPEALED已废止. */
  private String status;

  /** 法规内容. */
  private String content;

  /** 摘要. */
  private String summary;

  /** 关键词. */
  private String keywords;

  /** 来源. */
  private String source;

  /** 附件URL. */
  private String attachmentUrl;

  /** 浏览次数. */
  private Integer viewCount;

  /** 收藏次数. */
  private Integer collectCount;

  // 状态常量
  /** 状态：有效. */
  public static final String STATUS_EFFECTIVE = "EFFECTIVE";

  /** 状态：已修订. */
  public static final String STATUS_AMENDED = "AMENDED";

  /** 状态：已废止. */
  public static final String STATUS_REPEALED = "REPEALED";
}
