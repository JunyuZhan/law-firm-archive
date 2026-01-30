package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文章评论DTO（M10-022） */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleCommentDTO extends BaseDTO {
  /** 文章ID */
  private Long articleId;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 父评论ID */
  private Long parentId;

  /** 评论内容 */
  private String content;

  /** 点赞数 */
  private Integer likeCount;

  /** 回复列表 */
  private List<ArticleCommentDTO> replies;
}
