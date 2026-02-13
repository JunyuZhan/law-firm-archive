package com.archivesystem.repository;

import com.archivesystem.entity.ArchiveLocation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 档案位置Mapper.
 */
@Mapper
public interface ArchiveLocationMapper extends BaseMapper<ArchiveLocation> {

    /**
     * 根据位置编码查询.
     */
    @Select("SELECT * FROM archive_location WHERE location_code = #{code} AND deleted = false")
    ArchiveLocation selectByCode(@Param("code") String code);

    /**
     * 查询可用位置.
     */
    @Select("SELECT * FROM archive_location WHERE status = 'AVAILABLE' AND used_capacity < total_capacity AND deleted = false ORDER BY location_code")
    List<ArchiveLocation> selectAvailableLocations();

    /**
     * 增加已用容量.
     */
    @Update("UPDATE archive_location SET used_capacity = used_capacity + 1 WHERE id = #{id} AND used_capacity < total_capacity")
    int incrementUsedCapacity(@Param("id") Long id);

    /**
     * 减少已用容量.
     */
    @Update("UPDATE archive_location SET used_capacity = used_capacity - 1 WHERE id = #{id} AND used_capacity > 0")
    int decrementUsedCapacity(@Param("id") Long id);
}
