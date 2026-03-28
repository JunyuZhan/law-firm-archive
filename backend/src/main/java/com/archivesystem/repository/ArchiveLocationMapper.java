package com.archivesystem.repository;

import com.archivesystem.entity.ArchiveLocation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 存放位置Mapper接口.
 */
@Mapper
public interface ArchiveLocationMapper extends BaseMapper<ArchiveLocation> {

    /**
     * 根据位置编码查询.
     */
    @Select("SELECT * FROM archive_location WHERE location_code = #{code} AND deleted = false")
    ArchiveLocation selectByCode(@Param("code") String code);

    /**
     * 根据库房查询.
     */
    @Select("SELECT * FROM archive_location WHERE room_name = #{roomName} AND deleted = false ORDER BY location_code")
    List<ArchiveLocation> selectByRoom(@Param("roomName") String roomName);

    /**
     * 查询可用位置.
     */
    @Select("SELECT * FROM archive_location WHERE status = 'AVAILABLE' AND deleted = false ORDER BY location_code")
    List<ArchiveLocation> selectAvailable();

    /**
     * 获取所有库房列表.
     */
    @Select("SELECT DISTINCT room_name FROM archive_location WHERE room_name IS NOT NULL AND deleted = false ORDER BY room_name")
    List<String> selectRoomNames();
}
