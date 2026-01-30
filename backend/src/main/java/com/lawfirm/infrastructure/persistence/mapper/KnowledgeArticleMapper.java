package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.knowledge.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 经验文章Mapper */
@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

  /**
   * 分页查询文章.
   *
   * @param page 分页对象
   * @param authorId 作者ID
   * @param status 状态
   * @param category 分类
   * @param keyword 关键词
   * @return 文章分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM knowledge_article WHERE deleted = false "
          + "<if test='authorId != null'> AND author_id = #{authorId} </if>"
          + "<if test='status != null'> AND status = #{status} </if>"
          + "<if test='category != null'> AND category = #{category} </if>"
          + "<if test='keyword != null'> AND (title LIKE CONCAT('%',#{keyword},'%') "
          + "OR tags LIKE CONCAT('%',#{keyword},'%')) </if>"
          + "ORDER BY published_at DESC NULLS LAST, created_at DESC"
          + "</script>")
  IPage<KnowledgeArticle> selectArticlePage(
      Page<KnowledgeArticle> page,
      @Param("authorId") Long authorId,
      @Param("status") String status,
      @Param("category") String category,
      @Param("keyword") String keyword);

  /**
   * 增加浏览次数.
   *
   * @param id 文章ID
   * @return 更新行数
   */
  @Update("UPDATE knowledge_article SET view_count = view_count + 1 WHERE id = #{id}")
  int incrementViewCount(@Param("id") Long id);

  /**
   * 增加点赞次数.
   *
   * @param id 文章ID
   * @return 更新行数
   */
  @Update("UPDATE knowledge_article SET like_count = like_count + 1 WHERE id = #{id}")
  int incrementLikeCount(@Param("id") Long id);
}
