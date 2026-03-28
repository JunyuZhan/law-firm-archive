package com.archivesystem.repository;

import com.archivesystem.entity.AppraisalRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 鉴定记录Mapper接口.
 */
@Mapper
public interface AppraisalRecordMapper extends BaseMapper<AppraisalRecord> {

    /**
     * 根据档案ID查询鉴定记录.
     */
    @Select("SELECT * FROM arc_appraisal_record WHERE archive_id = #{archiveId} AND deleted = false ORDER BY created_at DESC")
    List<AppraisalRecord> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 查询待审批列表.
     */
    @Select("SELECT * FROM arc_appraisal_record WHERE status = 'PENDING' AND deleted = false ORDER BY created_at")
    List<AppraisalRecord> selectPendingList();

    /**
     * 根据类型查询鉴定记录.
     */
    @Select("SELECT * FROM arc_appraisal_record WHERE appraisal_type = #{type} AND deleted = false ORDER BY created_at DESC")
    List<AppraisalRecord> selectByType(@Param("type") String type);
}
