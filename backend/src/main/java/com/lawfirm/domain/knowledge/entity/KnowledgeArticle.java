package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 经验文章实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_article")
public class KnowledgeArticle extends BaseEntity {

    /** 文章标题 */
    private String title;

    /** 分类 */
    private String category;

    /** 文章内容 */
    private String content;

    /** 摘要 */
    private String summary;

    /** 作者ID */
    private Long authorId;

    /** 状态: DRAFT草稿/PUBLISHED已发布/ARCHIVED已归档 */
    private String status;

    /** 标签 */
    private String tags;

    /** 浏览次数 */
    private Integer viewCount;

    /** 点赞次数 */
    private Integer likeCount;

    /** 评论次数 */
    private Integer commentCount;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    // 状态常量
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
}
