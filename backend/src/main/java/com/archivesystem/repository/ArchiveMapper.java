package com.archivesystem.repository;

import com.archivesystem.entity.Archive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案Mapper接口.
 */
@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {

    /**
     * 根据档案号查询.
     */
    @Select("SELECT * FROM arc_archive WHERE archive_no = #{archiveNo} AND deleted = false")
    Archive selectByArchiveNo(@Param("archiveNo") String archiveNo);

    /**
     * 根据来源ID查询.
     */
    @Select("SELECT * FROM arc_archive WHERE source_type = #{sourceType} AND source_id = #{sourceId} AND deleted = false")
    Archive selectBySourceId(@Param("sourceType") String sourceType, @Param("sourceId") String sourceId);

    /**
     * 根据分类ID查询档案列表.
     */
    @Select("SELECT * FROM arc_archive WHERE category_id = #{categoryId} AND deleted = false ORDER BY created_at DESC")
    List<Archive> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 根据全宗ID查询档案列表.
     */
    @Select("SELECT * FROM arc_archive WHERE fonds_id = #{fondsId} AND deleted = false ORDER BY created_at DESC")
    List<Archive> selectByFondsId(@Param("fondsId") Long fondsId);

    /**
     * 分页查询用于索引重建.
     */
    @Select("SELECT * FROM arc_archive WHERE deleted = false ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<Archive> selectPageForIndex(@Param("offset") int offset, @Param("limit") int limit);
}
