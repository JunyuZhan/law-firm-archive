package com.archivesystem.repository;

import com.archivesystem.entity.ArchiveSource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案来源配置Mapper.
 */
@Mapper
public interface ArchiveSourceMapper extends BaseMapper<ArchiveSource> {

    /**
     * 根据来源编码查询.
     */
    @Select("SELECT * FROM archive_source WHERE source_code = #{code} AND deleted = false")
    ArchiveSource selectByCode(@Param("code") String code);

    /**
     * 查询所有启用的来源.
     */
    @Select("SELECT * FROM archive_source WHERE enabled = true AND deleted = false ORDER BY source_code")
    List<ArchiveSource> selectEnabledSources();

    /**
     * 根据来源类型查询.
     */
    @Select("SELECT * FROM archive_source WHERE source_type = #{type} AND enabled = true AND deleted = false")
    List<ArchiveSource> selectByType(@Param("type") String type);
}
