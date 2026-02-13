package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 经验文章DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeArticleDTO extends BaseDTO {

  /** ID */
  private Long id;

  /** 标题 */
  private String title;

  /** 分类 */
  private String category;

  /** 内容 */
  private String content;

  /** 摘要 */
  private String summary;

  /** 作者ID */
  private Long authorId;

  /** 作者姓名 */
  private String authorName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 标签 */
  private String tags;

  /** 浏览次数 */
  private Integer viewCount;

  /** 点赞次数 */
  private Integer likeCount;

  /** 评论数量 */
  private Integer commentCount;

  /** 发布时间 */
  private LocalDateTime publishedAt;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
