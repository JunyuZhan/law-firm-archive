package com.archivesystem.repository;

import com.archivesystem.entity.OperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志Mapper接口.
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 根据档案ID查询操作日志.
     */
    @Select("SELECT * FROM arc_operation_log WHERE archive_id = #{archiveId} ORDER BY operated_at DESC")
    List<OperationLog> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据对象类型和对象ID查询.
     */
    @Select("SELECT * FROM arc_operation_log WHERE object_type = #{objectType} AND object_id = #{objectId} ORDER BY operated_at DESC")
    List<OperationLog> selectByObject(@Param("objectType") String objectType, @Param("objectId") String objectId);
}
