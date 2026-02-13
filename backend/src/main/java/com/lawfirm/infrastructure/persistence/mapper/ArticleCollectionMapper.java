package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.ArticleCollection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 文章收藏Mapper（M10-023） */
@Mapper
public interface ArticleCollectionMapper extends BaseMapper<ArticleCollection> {

  /**
   * 查询用户是否已收藏文章.
   *
   * @param userId 用户ID
   * @param articleId 文章ID
   * @return 收藏数量
   */
  @Select(
      "SELECT COUNT(*) FROM article_collection WHERE user_id = #{userId} "
          + "AND article_id = #{articleId} AND deleted = false")
  int countByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

  /**
   * 查询用户收藏的所有文章ID.
   *
   * @param userId 用户ID
   * @return 文章ID列表
   */
  @Select(
      "SELECT article_id FROM article_collection WHERE user_id = #{userId} "
          + "AND deleted = false ORDER BY created_at DESC")
  List<Long> selectArticleIdsByUserId(@Param("userId") Long userId);
}
