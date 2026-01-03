package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.archive.entity.ArchiveOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案操作日志 Mapper
 */
@Mapper
public interface ArchiveOperationLogMapper extends BaseMapper<ArchiveOperationLog> {

    /**
     * 查询档案的操作日志
     */
    @Select("SELECT * FROM archive_operation_log WHERE archive_id = #{archiveId} ORDER BY operated_at DESC")
    List<ArchiveOperationLog> selectByArchiveId(@Param("archiveId") Long archiveId);
}

