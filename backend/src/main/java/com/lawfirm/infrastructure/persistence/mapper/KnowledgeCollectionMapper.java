package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.KnowledgeCollection;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识收藏Mapper
 */
@Mapper
public interface KnowledgeCollectionMapper extends BaseMapper<KnowledgeCollection> {

    /**
     * 查询用户收藏
     */
    @Select("SELECT * FROM knowledge_collection WHERE user_id = #{userId} AND target_type = #{targetType} AND deleted = false ORDER BY created_at DESC")
    List<KnowledgeCollection> selectByUserAndType(@Param("userId") Long userId, @Param("targetType") String targetType);

    /**
     * 检查是否已收藏
     */
    @Select("SELECT COUNT(*) FROM knowledge_collection WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId} AND deleted = false")
    int countByUserAndTarget(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    /**
     * 取消收藏
     */
    @Delete("DELETE FROM knowledge_collection WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int deleteByUserAndTarget(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);
    
    /**
     * 批量查询用户收藏（用于避免N+1查询）
     */
    @Select("<script>" +
            "SELECT * FROM knowledge_collection WHERE user_id = #{userId} AND target_type = #{targetType} " +
            "AND target_id IN " +
            "<foreach collection='targetIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted = false" +
            "</script>")
    List<KnowledgeCollection> selectBatchByUserAndTargets(@Param("userId") Long userId, 
                                                          @Param("targetType") String targetType, 
                                                          @Param("targetIds") List<Long> targetIds);
}
