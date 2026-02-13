package com.archivesystem.repository;

import com.archivesystem.entity.DigitalFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 电子文件Mapper接口.
 */
@Mapper
public interface DigitalFileMapper extends BaseMapper<DigitalFile> {

    /**
     * 根据档案ID查询文件列表.
     */
    @Select("SELECT * FROM arc_digital_file WHERE archive_id = #{archiveId} AND deleted = false ORDER BY sort_order, created_at")
    List<DigitalFile> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据哈希值查询.
     */
    @Select("SELECT * FROM arc_digital_file WHERE hash_value = #{hashValue} AND deleted = false LIMIT 1")
    DigitalFile selectByHashValue(@Param("hashValue") String hashValue);

    /**
     * 统计档案的文件数量和总大小.
     */
    @Select("SELECT COUNT(*) as count, COALESCE(SUM(file_size), 0) as total_size FROM arc_digital_file WHERE archive_id = #{archiveId} AND deleted = false")
    java.util.Map<String, Object> countByArchiveId(@Param("archiveId") Long archiveId);
}
