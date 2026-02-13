package com.archivesystem.repository;

import com.archivesystem.entity.Archive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案Mapper.
 */
@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {

    /**
     * 分页查询档案.
     */
    IPage<Archive> selectArchivePage(
            Page<Archive> page,
            @Param("archiveNo") String archiveNo,
            @Param("archiveName") String archiveName,
            @Param("archiveType") String archiveType,
            @Param("category") String category,
            @Param("sourceType") String sourceType,
            @Param("status") String status,
            @Param("locationId") Long locationId,
            @Param("keyword") String keyword
    );

    /**
     * 根据来源ID和来源类型查询.
     */
    @Select("SELECT * FROM archive WHERE source_id = #{sourceId} AND source_type = #{sourceType} AND deleted = false")
    Archive selectBySourceIdAndType(@Param("sourceId") String sourceId, @Param("sourceType") String sourceType);

    /**
     * 查询即将到期的档案.
     */
    @Select("SELECT * FROM archive WHERE retention_expire_date <= #{deadline} AND status = 'STORED' AND deleted = false")
    List<Archive> selectExpiringArchives(@Param("deadline") LocalDate deadline);

    /**
     * 按位置查询档案.
     */
    @Select("SELECT * FROM archive WHERE location_id = #{locationId} AND deleted = false ORDER BY box_no")
    List<Archive> selectByLocationId(@Param("locationId") Long locationId);

    /**
     * 统计各状态档案数量.
     */
    @Select("SELECT status, COUNT(*) as count FROM archive WHERE deleted = false GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 统计各来源类型档案数量.
     */
    @Select("SELECT source_type, COUNT(*) as count FROM archive WHERE deleted = false GROUP BY source_type")
    List<java.util.Map<String, Object>> countBySourceType();
}
