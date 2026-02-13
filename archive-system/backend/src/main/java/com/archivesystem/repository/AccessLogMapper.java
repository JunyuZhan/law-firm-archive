package com.archivesystem.repository;

import com.archivesystem.entity.AccessLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问日志Mapper接口.
 */
@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    /**
     * 根据档案ID查询访问日志.
     */
    @Select("SELECT * FROM arc_access_log WHERE archive_id = #{archiveId} ORDER BY accessed_at DESC")
    List<AccessLog> selectByArchiveId(@Param("archiveId") Long archiveId);
}
