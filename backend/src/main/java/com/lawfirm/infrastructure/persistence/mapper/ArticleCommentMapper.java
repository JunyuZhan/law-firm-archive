package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文章评论Mapper（M10-022）
 */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    /**
     * 查询文章的所有评论（按时间倒序）
     */
    @Select("SELECT * FROM article_comment WHERE article_id = #{articleId} AND deleted = false ORDER BY created_at DESC")
    List<ArticleComment> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询评论的回复
     */
    @Select("SELECT * FROM article_comment WHERE parent_id = #{parentId} AND deleted = false ORDER BY created_at ASC")
    List<ArticleComment> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 增加点赞数
     */
    @Update("UPDATE article_comment SET like_count = like_count + 1 WHERE id = #{id}")
    void incrementLikeCount(@Param("id") Long id);

    /**
     * 增加文章评论数
     */
    @Update("UPDATE knowledge_article SET comment_count = comment_count + 1 WHERE id = #{articleId}")
    void incrementCommentCount(@Param("articleId") Long articleId);

    /**
     * 减少文章评论数
     */
    @Update("UPDATE knowledge_article SET comment_count = comment_count - 1 WHERE id = #{articleId}")
    void decrementCommentCount(@Param("articleId") Long articleId);
}

