package com.archivesystem.repository;

import com.archivesystem.entity.ArchiveFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案文件Mapper.
 */
@Mapper
public interface ArchiveFileMapper extends BaseMapper<ArchiveFile> {

    /**
     * 根据档案ID查询文件列表.
     */
    @Select("SELECT * FROM archive_file WHERE archive_id = #{archiveId} AND deleted = false ORDER BY sort_order, id")
    List<ArchiveFile> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据MD5查询文件（用于去重）.
     */
    @Select("SELECT * FROM archive_file WHERE file_md5 = #{md5} AND deleted = false LIMIT 1")
    ArchiveFile selectByMd5(@Param("md5") String md5);

    /**
     * 统计档案的文件数量.
     */
    @Select("SELECT COUNT(*) FROM archive_file WHERE archive_id = #{archiveId} AND deleted = false")
    int countByArchiveId(@Param("archiveId") Long archiveId);
}
