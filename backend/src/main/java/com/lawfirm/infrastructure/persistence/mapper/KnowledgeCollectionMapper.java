package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.KnowledgeCollection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 知识收藏Mapper */
@Mapper
public interface KnowledgeCollectionMapper extends BaseMapper<KnowledgeCollection> {

  /**
   * 查询用户收藏.
   *
   * @param userId 用户ID
   * @param targetType 目标类型
   * @return 知识收藏列表
   */
  @Select(
      "SELECT * FROM knowledge_collection WHERE user_id = #{userId} "
          + "AND target_type = #{targetType} AND deleted = false "
          + "ORDER BY created_at DESC")
  List<KnowledgeCollection> selectByUserAndType(
      @Param("userId") Long userId, @Param("targetType") String targetType);

  /**
   * 检查是否已收藏.
   *
   * @param userId 用户ID
   * @param targetType 目标类型
   * @param targetId 目标ID
   * @return 收藏数量
   */
  @Select(
      "SELECT COUNT(*) FROM knowledge_collection WHERE user_id = #{userId} "
          + "AND target_type = #{targetType} AND target_id = #{targetId} "
          + "AND deleted = false")
  int countByUserAndTarget(
      @Param("userId") Long userId,
      @Param("targetType") String targetType,
      @Param("targetId") Long targetId);

  /**
   * 取消收藏.
   *
   * @param userId 用户ID
   * @param targetType 目标类型
   * @param targetId 目标ID
   * @return 删除数量
   */
  @Delete(
      "DELETE FROM knowledge_collection WHERE user_id = #{userId} "
          + "AND target_type = #{targetType} AND target_id = #{targetId}")
  int deleteByUserAndTarget(
      @Param("userId") Long userId,
      @Param("targetType") String targetType,
      @Param("targetId") Long targetId);

  /**
   * 批量查询用户收藏（用于避免N+1查询）.
   *
   * @param userId 用户ID
   * @param targetType 目标类型
   * @param targetIds 目标ID列表
   * @return 知识收藏列表
   */
  @Select(
      "<script>"
          + "SELECT * FROM knowledge_collection WHERE user_id = #{userId} AND target_type = #{targetType} "
          + "AND target_id IN "
          + "<foreach collection='targetIds' item='id' open='(' separator=',' close=')'>"
          + "#{id}"
          + "</foreach>"
          + " AND deleted = false"
          + "</script>")
  List<KnowledgeCollection> selectBatchByUserAndTargets(
      @Param("userId") Long userId,
      @Param("targetType") String targetType,
      @Param("targetIds") List<Long> targetIds);
}
