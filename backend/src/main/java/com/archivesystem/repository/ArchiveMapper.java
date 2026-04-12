package com.archivesystem.repository;

import com.archivesystem.entity.Archive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 档案Mapper接口.
 * @author junyuzhan
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

    /**
     * 按档案类型统计.
     */
    @Select("SELECT archive_type AS type, COUNT(*) AS count FROM arc_archive WHERE deleted = false GROUP BY archive_type")
    List<Map<String, Object>> countByArchiveType();

    /**
     * 按保管期限统计.
     */
    @Select("SELECT retention_period AS period, COUNT(*) AS count FROM arc_archive WHERE deleted = false GROUP BY retention_period")
    List<Map<String, Object>> countByRetentionPeriod();

    /**
     * 按状态统计.
     */
    @Select("SELECT status AS status, COUNT(*) AS count FROM arc_archive WHERE deleted = false GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 按月份统计指定年份创建量.
     */
    @Select("""
            SELECT EXTRACT(MONTH FROM created_at) AS month, COUNT(*) AS count
            FROM arc_archive
            WHERE deleted = false
              AND created_at >= MAKE_TIMESTAMP(#{year}, 1, 1, 0, 0, 0)
              AND created_at < MAKE_TIMESTAMP(#{year} + 1, 1, 1, 0, 0, 0)
            GROUP BY EXTRACT(MONTH FROM created_at)
            ORDER BY month
            """)
    List<Map<String, Object>> countByMonth(@Param("year") int year);
}
