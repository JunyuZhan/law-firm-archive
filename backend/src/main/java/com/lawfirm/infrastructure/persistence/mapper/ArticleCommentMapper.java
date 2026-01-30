package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.ArticleComment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 文章评论Mapper（M10-022） */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

  /**
   * 查询文章的所有评论（按时间倒序）.
   *
   * @param articleId 文章ID
   * @return 评论列表
   */
  @Select(
      "SELECT * FROM article_comment WHERE article_id = #{articleId} AND deleted = false ORDER BY created_at DESC")
  List<ArticleComment> selectByArticleId(@Param("articleId") Long articleId);

  /**
   * 查询评论的回复.
   *
   * @param parentId 父评论ID
   * @return 回复列表
   */
  @Select(
      "SELECT * FROM article_comment WHERE parent_id = #{parentId} AND deleted = false ORDER BY created_at ASC")
  List<ArticleComment> selectByParentId(@Param("parentId") Long parentId);

  /**
   * 增加点赞数.
   *
   * @param id 评论ID
   */
  @Update("UPDATE article_comment SET like_count = like_count + 1 WHERE id = #{id}")
  void incrementLikeCount(@Param("id") Long id);

  /**
   * 增加文章评论数.
   *
   * @param articleId 文章ID
   */
  @Update("UPDATE knowledge_article SET comment_count = comment_count + 1 WHERE id = #{articleId}")
  void incrementCommentCount(@Param("articleId") Long articleId);

  /**
   * 减少文章评论数.
   *
   * @param articleId 文章ID
   */
  @Update("UPDATE knowledge_article SET comment_count = comment_count - 1 WHERE id = #{articleId}")
  void decrementCommentCount(@Param("articleId") Long articleId);
}
