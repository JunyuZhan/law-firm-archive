package com.archivesystem.repository;

import com.archivesystem.entity.ArchiveMetadata;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案元数据Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface ArchiveMetadataMapper extends BaseMapper<ArchiveMetadata> {

    /**
     * 根据档案ID查询元数据列表.
     */
    @Select("SELECT * FROM arc_metadata WHERE archive_id = #{archiveId} ORDER BY sort_order, id")
    List<ArchiveMetadata> selectByArchiveId(@Param("archiveId") Long archiveId);
}
