package com.archivesystem.repository;

import com.archivesystem.entity.DestructionRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 销毁记录Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface DestructionRecordMapper extends BaseMapper<DestructionRecord> {

    /**
     * 根据档案ID查询销毁记录.
     */
    @Select("SELECT * FROM arc_destruction_record WHERE archive_id = #{archiveId} ORDER BY created_at DESC")
    List<DestructionRecord> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据批次号查询.
     */
    @Select("SELECT * FROM arc_destruction_record WHERE destruction_batch_no = #{batchNo}")
    List<DestructionRecord> selectByBatchNo(@Param("batchNo") String batchNo);

    /**
     * 查询待审批列表.
     */
    @Select("SELECT * FROM arc_destruction_record WHERE status = 'PENDING' ORDER BY created_at")
    List<DestructionRecord> selectPendingList();

    /**
     * 查询已审批待执行列表.
     */
    @Select("SELECT * FROM arc_destruction_record WHERE status = 'APPROVED' ORDER BY approved_at")
    List<DestructionRecord> selectApprovedList();
}
