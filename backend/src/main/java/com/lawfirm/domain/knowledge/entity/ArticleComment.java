package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 文章评论实体（M10-022） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("article_comment")
public class ArticleComment extends BaseEntity {

  /** 文章ID */
  private Long articleId;

  /** 评论人ID */
  private Long userId;

  /** 父评论ID（用于回复） */
  private Long parentId;

  /** 评论内容 */
  private String content;

  /** 点赞数 */
  private Integer likeCount;
}
