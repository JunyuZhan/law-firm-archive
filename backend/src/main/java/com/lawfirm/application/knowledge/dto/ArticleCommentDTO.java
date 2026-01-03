package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文章评论DTO（M10-022）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleCommentDTO extends BaseDTO {
    private Long articleId;
    private Long userId;
    private String userName;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private List<ArticleCommentDTO> replies; // 回复列表
}

