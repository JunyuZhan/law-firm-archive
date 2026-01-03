package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 经验文章DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeArticleDTO extends BaseDTO {
    private Long id;
    private String title;
    private String category;
    private String content;
    private String summary;
    private Long authorId;
    private String authorName;
    private String status;
    private String statusName;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
